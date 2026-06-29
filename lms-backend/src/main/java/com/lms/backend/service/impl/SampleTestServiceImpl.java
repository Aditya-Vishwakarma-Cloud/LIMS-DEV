package com.lms.backend.service.impl;

import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleTestDto;
import com.lms.backend.dto.SampleTestAssignmentDto;
import com.lms.backend.dto.SampleTestHistoryDto;
import com.lms.backend.entity.*;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.*;
import com.lms.backend.service.SampleTestService;
import com.lms.backend.service.AuditLogService;
import com.lms.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleTestServiceImpl implements SampleTestService {

    private final SampleRepository sampleRepository;
    private final SampleTestRepository sampleTestRepository;
    private final SampleTestHistoryRepository sampleTestHistoryRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final TestResultRepository testResultRepository;
    private final UserRepository userRepository;
    private final SampleHistoryRepository sampleHistoryRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public List<SampleDto> getPendingSamples() {
        List<SampleStatus> statuses = List.of(SampleStatus.RECEIVED, SampleStatus.PARTIALLY_ASSIGNED);
        return sampleRepository.findByStatusInAndDeletedFalse(statuses).stream()
                .map(this::mapSampleToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTestDto> getSampleTestsBySampleId(UUID sampleId) {
        return sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sampleId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SampleTestDto> assignTests(UUID sampleId, SampleTestAssignmentDto dto) {
        Sample sample = sampleRepository.findById(sampleId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + sampleId));

        if (sample.getStatus() != SampleStatus.RECEIVED && sample.getStatus() != SampleStatus.PARTIALLY_ASSIGNED) {
            throw new IllegalArgumentException("Sample must be in RECEIVED or PARTIALLY_ASSIGNED status for test assignment. Current status: " + sample.getStatus());
        }

        if (dto.getAssignments() == null || dto.getAssignments().isEmpty()) {
            throw new IllegalArgumentException("At least one test assignment specification must be provided.");
        }

        // Fetch all active test definitions for this material
        List<TestDefinition> activeMaterialTests = testDefinitionRepository.findByMaterialId(sample.getMaterial().getId())
                .stream()
                .filter(TestDefinition::isActive)
                .collect(Collectors.toList());

        List<UUID> assignedDefIds = dto.getAssignments().stream()
                .map(SampleTestAssignmentDto.SingleTestAssignment::getTestDefinitionId)
                .collect(Collectors.toList());

        // Get currently saved assignments in database
        List<SampleTest> existingTests = sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sampleId);
        List<UUID> existingDefIds = existingTests.stream()
                .map(st -> st.getTestDefinition().getId())
                .collect(Collectors.toList());

        // Check if mode is INITIAL but tests already exist
        if ("INITIAL".equalsIgnoreCase(dto.getMode()) && !existingTests.isEmpty()) {
            throw new IllegalArgumentException("Sample tests are already assigned. Use ADDITIONAL or REASSIGN modes instead.");
        }

        // Check mandatory tests constraint
        List<TestDefinition> mandatoryTests = activeMaterialTests.stream()
                .filter(TestDefinition::isMandatory)
                .collect(Collectors.toList());

        for (TestDefinition mandatory : mandatoryTests) {
            boolean isAssignedInRequest = assignedDefIds.contains(mandatory.getId());
            boolean isAlreadyInDb = existingDefIds.contains(mandatory.getId());
            if (!isAssignedInRequest && !isAlreadyInDb) {
                throw new IllegalArgumentException("Mandatory test definition: " + mandatory.getTestName() + " must be assigned.");
            }
        }

        User currentUser = getCurrentUser();
        List<SampleTest> savedTests = new ArrayList<>();

        if ("REASSIGN".equalsIgnoreCase(dto.getMode())) {
            // Process reassignments
            for (SampleTestAssignmentDto.SingleTestAssignment assignItem : dto.getAssignments()) {
                SampleTest existing = existingTests.stream()
                        .filter(st -> st.getTestDefinition().getId().equals(assignItem.getTestDefinitionId()))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    User oldTech = existing.getTechnician();
                    User newTech = null;
                    if (assignItem.getTechnicianId() != null) {
                        newTech = userRepository.findById(assignItem.getTechnicianId())
                                .filter(u -> !u.isDeleted())
                                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + assignItem.getTechnicianId()));
                    }

                    LocalDate oldDueDate = existing.getDueDate();
                    LocalDate newDueDate = assignItem.getDueDate();

                    existing.setTechnician(newTech);
                    existing.setScheduledDate(assignItem.getScheduledDate());
                    existing.setDueDate(newDueDate);
                    existing.setSequenceNumber(assignItem.getSequenceNumber() > 0 ? assignItem.getSequenceNumber() : existing.getSequenceNumber());
                    if (assignItem.getRemarks() != null) {
                        existing.setRemarks(assignItem.getRemarks());
                    }

                    if (newTech != null && existing.getStatus() == SampleTestStatus.PENDING) {
                        existing.setStatus(SampleTestStatus.ASSIGNED);
                    }

                    SampleTest saved = sampleTestRepository.save(existing);
                    savedTests.add(saved);

                    // History tracking
                    boolean techChanged = !Objects.equals(oldTech, newTech);
                    boolean dueDateChanged = !Objects.equals(oldDueDate, newDueDate);

                    if (techChanged || dueDateChanged) {
                        String changeType = techChanged ? "TECHNICIAN_CHANGE" : "DUE_DATE_CHANGE";
                        if (techChanged && dueDateChanged) {
                            changeType = "REASSIGN";
                        }
                        logTestHistory(saved, changeType, oldTech, newTech, oldDueDate, newDueDate, saved.getStatus(), saved.getStatus(), assignItem.getRemarks());
                    }

                    // Notification if reassigned to new tech
                    if (techChanged && newTech != null) {
                        sendTechnicianNotification(newTech, saved);
                    }
                }
            }
        } else {
            // INITIAL or ADDITIONAL: Register new tests
            for (SampleTestAssignmentDto.SingleTestAssignment assignItem : dto.getAssignments()) {
                // If ADDITIONAL, skip if already exists in DB
                if ("ADDITIONAL".equalsIgnoreCase(dto.getMode()) && existingDefIds.contains(assignItem.getTestDefinitionId())) {
                    continue;
                }

                TestDefinition testDef = testDefinitionRepository.findById(assignItem.getTestDefinitionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Test definition not found with id: " + assignItem.getTestDefinitionId()));

                User tech = null;
                if (assignItem.getTechnicianId() != null) {
                    tech = userRepository.findById(assignItem.getTechnicianId())
                            .filter(u -> !u.isDeleted())
                            .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + assignItem.getTechnicianId()));
                }

                SampleTestStatus initStatus = tech != null ? SampleTestStatus.ASSIGNED : SampleTestStatus.PENDING;

                SampleTest sampleTest = SampleTest.builder()
                        .sample(sample)
                        .testDefinition(testDef)
                        .technician(tech)
                        .assignedBy(currentUser)
                        .assignedDate(LocalDate.now())
                        .scheduledDate(assignItem.getScheduledDate())
                        .dueDate(assignItem.getDueDate())
                        .sequenceNumber(assignItem.getSequenceNumber() > 0 ? assignItem.getSequenceNumber() : 1)
                        .status(initStatus)
                        .remarks(assignItem.getRemarks())
                        .deleted(false)
                        .build();

                SampleTest saved = sampleTestRepository.save(sampleTest);
                savedTests.add(saved);

                // Initial audit trail
                logTestHistory(saved, "INITIAL_ASSIGN", null, tech, null, assignItem.getDueDate(), null, initStatus, assignItem.getRemarks());

                // Auto seed empty Test Result
                TestResult result = TestResult.builder()
                        .sampleTest(saved)
                        .status(ResultStatus.DRAFT)
                        .deleted(false)
                        .build();
                testResultRepository.save(result);

                // Notify tech
                if (tech != null) {
                    sendTechnicianNotification(tech, saved);
                }
            }
        }

        // Determine Sample Status: PARTIALLY_ASSIGNED vs FULLY_ASSIGNED
        long totalActiveMaterialTests = activeMaterialTests.size();
        List<SampleTest> currentAllTests = sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sampleId);
        long assignedTestsCount = currentAllTests.size();

        SampleStatus oldSampleStatus = sample.getStatus();
        SampleStatus newSampleStatus = (assignedTestsCount >= totalActiveMaterialTests) ? SampleStatus.FULLY_ASSIGNED : SampleStatus.PARTIALLY_ASSIGNED;

        sample.setStatus(newSampleStatus);
        sampleRepository.save(sample);

        logSampleHistory(sample, oldSampleStatus, newSampleStatus, "Assigned " + savedTests.size() + " test(s) to sample via " + dto.getMode() + " mode.");
        logAudit("ASSIGN_TESTS", "Assigned tests to sample: " + sample.getSampleId() + " (Mode: " + dto.getMode() + ")");

        return currentAllTests.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SampleTestDto updateSampleTest(UUID id, SampleTestDto dto) {
        SampleTest sampleTest = sampleTestRepository.findById(id)
                .filter(st -> !st.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sample test assignment not found with id: " + id));

        User oldTech = sampleTest.getTechnician();
        User newTech = null;
        if (dto.getTechnicianId() != null) {
            newTech = userRepository.findById(dto.getTechnicianId())
                    .filter(u -> !u.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + dto.getTechnicianId()));
        }

        LocalDate oldDueDate = sampleTest.getDueDate();
        LocalDate newDueDate = dto.getDueDate();

        SampleTestStatus oldStatus = sampleTest.getStatus();
        SampleTestStatus newStatus = dto.getStatus() != null ? SampleTestStatus.valueOf(dto.getStatus().toUpperCase()) : oldStatus;

        sampleTest.setTechnician(newTech);
        sampleTest.setScheduledDate(dto.getScheduledDate());
        sampleTest.setDueDate(newDueDate);
        sampleTest.setStatus(newStatus);
        if (dto.getRemarks() != null) {
            sampleTest.setRemarks(dto.getRemarks());
        }
        if (dto.getSequenceNumber() > 0) {
            sampleTest.setSequenceNumber(dto.getSequenceNumber());
        }

        SampleTest saved = sampleTestRepository.save(sampleTest);
        logAudit("UPDATE_SAMPLE_TEST", "Updated test assignment for " + saved.getSample().getSampleId() + " - Test: " + saved.getTestDefinition().getTestName());

        // History track
        logTestHistory(saved, "STATUS_CHANGE", oldTech, newTech, oldDueDate, newDueDate, oldStatus, newStatus, dto.getRemarks());

        if (newTech != null && !Objects.equals(oldTech, newTech)) {
            sendTechnicianNotification(newTech, saved);
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTestDto> getSampleTestsByTechnicianId(UUID technicianId) {
        return sampleTestRepository.findByTechnicianId(technicianId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SampleTestDto getSampleTestById(UUID id) {
        SampleTest st = sampleTestRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sample test not found with id: " + id));
        return mapToDto(st);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTestHistoryDto> getSampleTestHistory(UUID sampleTestId) {
        return sampleTestHistoryRepository.findBySampleTestIdOrderByChangedAtDesc(sampleTestId).stream()
                .map(this::mapHistoryToDto)
                .collect(Collectors.toList());
    }

    private void sendTechnicianNotification(User tech, SampleTest test) {
        try {
            publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                this,
                "TEST_ASSIGNED",
                "New Test Assigned: " + test.getTestDefinition().getTestName(),
                String.format("You have been assigned to perform laboratory test %s for Sample ID %s. Target Due Date: %s.",
                    test.getTestDefinition().getTestName(), test.getSample().getSampleId(), test.getDueDate() != null ? test.getDueDate() : "Not specified"),
                com.lms.backend.entity.NotificationPriority.HIGH,
                null,
                tech.getEmail()
            ));
        } catch (Exception e) {
            log.error("Failed to publish technician assignment event: {}", tech.getEmail(), e);
        }
    }

    private void logAudit(String action, String details) {
        auditLogService.log(action, getCurrentUsername(), "localhost", details);
    }

    private void logSampleHistory(Sample sample, SampleStatus oldStatus, SampleStatus newStatus, String remarks) {
        User user = getCurrentUser();
        SampleHistory history = SampleHistory.builder()
                .sample(sample)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(user)
                .changedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();
        sampleHistoryRepository.save(history);
    }

    private void logTestHistory(SampleTest test, String changeType, User oldTech, User newTech,
                                LocalDate oldDueDate, LocalDate newDueDate,
                                SampleTestStatus oldStatus, SampleTestStatus newStatus, String remarks) {
        User user = getCurrentUser();
        SampleTestHistory history = SampleTestHistory.builder()
                .sampleTest(test)
                .changeType(changeType)
                .oldTechnician(oldTech)
                .newTechnician(newTech)
                .oldDueDate(oldDueDate)
                .newDueDate(newDueDate)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(user != null ? user : test.getAssignedBy())
                .changedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();
        sampleTestHistoryRepository.save(history);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByEmail(username).orElse(null);
    }

    private SampleDto mapSampleToDto(Sample s) {
        return SampleDto.builder()
                .id(s.getId())
                .sampleId(s.getSampleId())
                .workOrderId(s.getWorkOrder().getId())
                .workOrderNumber(s.getWorkOrder().getWorkOrderNumber())
                .customerName(s.getWorkOrder().getCustomer().getCustomerName())
                .projectName(s.getWorkOrder().getProject().getProjectName())
                .materialId(s.getMaterial().getId())
                .materialName(s.getMaterial().getMaterialName())
                .materialCode(s.getMaterial().getMaterialCode())
                .quantity(s.getQuantity())
                .unit(s.getUnit())
                .collectionDate(s.getCollectionDate())
                .collectionLocation(s.getCollectionLocation())
                .status(s.getStatus().name())
                .priority(s.getPriority())
                .remarks(s.getRemarks())
                .build();
    }

    private SampleTestDto mapToDto(SampleTest st) {
        return SampleTestDto.builder()
                .id(st.getId())
                .sampleId(st.getSample().getId())
                .sampleCode(st.getSample().getSampleId())
                .testDefinitionId(st.getTestDefinition().getId())
                .testName(st.getTestDefinition().getTestName())
                .testCode(st.getTestDefinition().getTestCode())
                .unit(st.getTestDefinition().getUnit())
                .specification(st.getTestDefinition().getSpecification())
                .specOperator(st.getTestDefinition().getSpecOperator())
                .specValue(st.getTestDefinition().getSpecValue())
                .valueType(st.getTestDefinition().getValueType())
                .method(st.getTestDefinition().getMethod())
                .isMandatory(st.getTestDefinition().isMandatory())
                .technicianId(st.getTechnician() != null ? st.getTechnician().getId() : null)
                .technicianName(st.getTechnician() != null ? st.getTechnician().getName() : null)
                .assignedById(st.getAssignedBy() != null ? st.getAssignedBy().getId() : null)
                .assignedByName(st.getAssignedBy() != null ? st.getAssignedBy().getName() : null)
                .assignedDate(st.getAssignedDate())
                .scheduledDate(st.getScheduledDate())
                .dueDate(st.getDueDate())
                .sequenceNumber(st.getSequenceNumber())
                .status(st.getStatus().name())
                .remarks(st.getRemarks())
                .build();
    }

    private SampleTestHistoryDto mapHistoryToDto(SampleTestHistory h) {
        return SampleTestHistoryDto.builder()
                .id(h.getId())
                .sampleTestId(h.getSampleTest().getId())
                .changeType(h.getChangeType())
                .oldTechnicianId(h.getOldTechnician() != null ? h.getOldTechnician().getId() : null)
                .oldTechnicianName(h.getOldTechnician() != null ? h.getOldTechnician().getName() : null)
                .newTechnicianId(h.getNewTechnician() != null ? h.getNewTechnician().getId() : null)
                .newTechnicianName(h.getNewTechnician() != null ? h.getNewTechnician().getName() : null)
                .oldDueDate(h.getOldDueDate())
                .newDueDate(h.getNewDueDate())
                .oldStatus(h.getOldStatus() != null ? h.getOldStatus().name() : null)
                .newStatus(h.getNewStatus().name())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : "System")
                .changedAt(h.getChangedAt())
                .remarks(h.getRemarks())
                .build();
    }
}
