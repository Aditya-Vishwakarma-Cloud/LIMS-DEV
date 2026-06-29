package com.lms.backend.service.impl;

import com.lms.backend.dto.TestResultDto;
import com.lms.backend.entity.*;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.*;
import com.lms.backend.service.TestResultService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultServiceImpl implements TestResultService {

    private final TestResultRepository testResultRepository;
    private final SampleTestRepository sampleTestRepository;
    private final SampleRepository sampleRepository;
    private final UserRepository userRepository;
    private final SampleHistoryRepository sampleHistoryRepository;
    private final SampleTestHistoryRepository sampleTestHistoryRepository;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public TestResultDto saveDraft(UUID sampleTestId, TestResultDto dto) {
        SampleTest sampleTest = sampleTestRepository.findById(sampleTestId)
                .filter(st -> !st.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sample test not found with id: " + sampleTestId));

        // Rule 1: Validate allowed statuses for result entry
        if (sampleTest.getStatus() == SampleTestStatus.COMPLETED || sampleTest.getStatus() == SampleTestStatus.VERIFIED) {
            throw new IllegalArgumentException("Cannot save draft. Test is already completed or verified.");
        }

        User currentUser = getCurrentUser();

        // Rule 2: Assert technician lock (unless manager/admin)
        if (sampleTest.getTechnician() != null && !sampleTest.getTechnician().getId().equals(currentUser.getId())) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : "";
            boolean isManagerOrAdmin = "ROLE_ADMIN".equals(roleName) || "ROLE_SUPER_ADMIN".equals(roleName) || "ROLE_LAB_MANAGER".equals(roleName);
            if (!isManagerOrAdmin) {
                throw new IllegalArgumentException("You are not the assigned technician for this test.");
            }
        }

        // Fetch or initialize TestResult
        TestResult result = testResultRepository.findBySampleTestId(sampleTestId)
                .orElse(null);

        if (result == null) {
            result = TestResult.builder()
                    .sampleTest(sampleTest)
                    .status(ResultStatus.DRAFT)
                    .version(1)
                    .deleted(false)
                    .build();
        } else {
            // If already submitted and not rejected, technician cannot modify
            if (result.getStatus() != ResultStatus.DRAFT && result.getStatus() != ResultStatus.REJECTED) {
                throw new IllegalArgumentException("Result is currently locked in status: " + result.getStatus());
            }
        }

        result.setObservations(dto.getObservations());
        result.setCalculations(dto.getCalculations());
        result.setFinalResult(dto.getFinalResult());
        result.setRemarks(dto.getRemarks());
        result.setAttachments(dto.getAttachments());
        result.setTestedBy(currentUser);

        if (result.getTestingStartedAt() == null) {
            result.setTestingStartedAt(LocalDateTime.now());
        }

        // Auto evaluate Pass/Fail dynamically
        TestDefinition def = sampleTest.getTestDefinition();
        result.setUnit(def.getUnit());
        result.setSpecOperator(def.getSpecOperator());
        result.setSpecValue(def.getSpecValue());
        
        PassFailStatus pf = evaluatePassFail(def.getSpecOperator(), def.getSpecValue(), dto.getFinalResult());
        result.setPassFail(pf);

        TestResult savedResult = testResultRepository.save(result);

        // Update SampleTest status to IN_PROGRESS if not already
        if (sampleTest.getStatus() == SampleTestStatus.ASSIGNED || sampleTest.getStatus() == SampleTestStatus.PENDING) {
            SampleTestStatus oldStatus = sampleTest.getStatus();
            sampleTest.setStatus(SampleTestStatus.IN_PROGRESS);
            sampleTestRepository.save(sampleTest);
            logTestHistory(sampleTest, "STATUS_CHANGE", sampleTest.getTechnician(), sampleTest.getTechnician(),
                    sampleTest.getDueDate(), sampleTest.getDueDate(), oldStatus, SampleTestStatus.IN_PROGRESS, "Started testing operations");
        }

        // Update parent Sample status to TESTING if registered or received
        Sample sample = sampleTest.getSample();
        if (sample.getStatus() != SampleStatus.TESTING && sample.getStatus() != SampleStatus.TESTING_COMPLETE) {
            SampleStatus oldStatus = sample.getStatus();
            sample.setStatus(SampleStatus.TESTING);
            sampleRepository.save(sample);
            logSampleHistory(sample, oldStatus, SampleStatus.TESTING, "Started laboratory testing sequence.");
        }

        logAudit("SAVE_RESULT_DRAFT", "Saved result draft for " + sample.getSampleId() + " - Test: " + def.getTestName());

        return mapToDto(savedResult);
    }

    @Override
    @Transactional
    public TestResultDto submitResult(UUID id) {
        TestResult result = testResultRepository.findById(id)
                .filter(tr -> !tr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Test result not found with id: " + id));

        if (result.getStatus() != ResultStatus.DRAFT && result.getStatus() != ResultStatus.REJECTED) {
            throw new IllegalArgumentException("Result can only be submitted from DRAFT or REJECTED status. Current status: " + result.getStatus());
        }

        SampleTest sampleTest = result.getSampleTest();
        TestDefinition def = sampleTest.getTestDefinition();

        result.setStatus(ResultStatus.SUBMITTED);
        result.setSubmittedAt(LocalDateTime.now());
        if (result.getTestingCompletedAt() == null) {
            result.setTestingCompletedAt(LocalDateTime.now());
        }

        // Perform final Pass/Fail evaluation
        PassFailStatus pf = evaluatePassFail(result.getSpecOperator(), result.getSpecValue(), result.getFinalResult());
        result.setPassFail(pf);

        TestResult savedResult = testResultRepository.save(result);

        // Transition SampleTest status to COMPLETED
        SampleTestStatus oldStatus = sampleTest.getStatus();
        sampleTest.setStatus(SampleTestStatus.COMPLETED);
        sampleTestRepository.save(sampleTest);
        logTestHistory(sampleTest, "STATUS_CHANGE", sampleTest.getTechnician(), sampleTest.getTechnician(),
                sampleTest.getDueDate(), sampleTest.getDueDate(), oldStatus, SampleTestStatus.COMPLETED, "Submitted final testing values");

        // Cascade verification for parent Sample
        Sample sample = sampleTest.getSample();
        List<SampleTest> allTests = sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sample.getId());
        
        boolean allCompleted = allTests.stream()
                .allMatch(st -> st.getStatus() == SampleTestStatus.COMPLETED || st.getStatus() == SampleTestStatus.VERIFIED);

        if (allCompleted) {
            SampleStatus oldSampleStatus = sample.getStatus();
            sample.setStatus(SampleStatus.TESTING_COMPLETE);
            sampleRepository.save(sample);
            logSampleHistory(sample, oldSampleStatus, SampleStatus.TESTING_COMPLETE, "All assigned lab tests submitted. Transitioning to review cycle.");
        }

        logAudit("SUBMIT_RESULT", "Submitted testing result for sample: " + sample.getSampleId() + " - Test: " + def.getTestName() + " (PassFail: " + pf + ")");

        publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
            this,
            "RESULT_SUBMITTED",
            "Result Awaiting Review: " + sample.getSampleId(),
            String.format("Technician has submitted the results for test %s on Sample %s. Ready for QA review.", def.getTestName(), sample.getSampleId()),
            com.lms.backend.entity.NotificationPriority.MEDIUM,
            "ROLE_QUALITY_ENGINEER",
            null
        ));

        return mapToDto(savedResult);
    }

    @Override
    @Transactional
    public TestResultDto rejectResult(UUID id, String remarks) {
        TestResult result = testResultRepository.findById(id)
                .filter(tr -> !tr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Test result not found with id: " + id));

        if (result.getStatus() != ResultStatus.SUBMITTED && result.getStatus() != ResultStatus.UNDER_REVIEW && result.getStatus() != ResultStatus.VERIFIED) {
            throw new IllegalArgumentException("Only submitted or verified results can be rejected.");
        }

        SampleTest sampleTest = result.getSampleTest();

        result.setStatus(ResultStatus.REJECTED);
        result.setRemarks(remarks != null ? remarks : "Rejected by Quality Manager");
        result.setVersion(result.getVersion() + 1);
        TestResult savedResult = testResultRepository.save(result);

        // Move SampleTest back to IN_PROGRESS
        SampleTestStatus oldStatus = sampleTest.getStatus();
        sampleTest.setStatus(SampleTestStatus.IN_PROGRESS);
        sampleTestRepository.save(sampleTest);
        logTestHistory(sampleTest, "STATUS_CHANGE", sampleTest.getTechnician(), sampleTest.getTechnician(),
                sampleTest.getDueDate(), sampleTest.getDueDate(), oldStatus, SampleTestStatus.IN_PROGRESS, "Result rejected: " + remarks);

        // Revert parent Sample status to TESTING if it was TESTING_COMPLETE
        Sample sample = sampleTest.getSample();
        if (sample.getStatus() == SampleStatus.TESTING_COMPLETE) {
            SampleStatus oldSampleStatus = sample.getStatus();
            sample.setStatus(SampleStatus.TESTING);
            sampleRepository.save(sample);
            logSampleHistory(sample, oldSampleStatus, SampleStatus.TESTING, "Result rejected for test " + sampleTest.getTestDefinition().getTestName() + ". Reverting status to Testing.");
        }

        logAudit("REJECT_RESULT", "Rejected testing result for sample: " + sample.getSampleId());

        if (sampleTest.getTechnician() != null) {
            publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                this,
                "RESULT_REJECTED",
                "Result Rejected: " + sampleTest.getTestDefinition().getTestName(),
                String.format("Your submitted test result for %s on Sample ID %s has been rejected. Reason: %s. Please re-enter observations.",
                    sampleTest.getTestDefinition().getTestName(), sample.getSampleId(), remarks),
                com.lms.backend.entity.NotificationPriority.HIGH,
                null,
                sampleTest.getTechnician().getEmail()
            ));
        }

        return mapToDto(savedResult);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultDto getResultBySampleTestId(UUID sampleTestId) {
        TestResult result = testResultRepository.findBySampleTestId(sampleTestId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for sample test id: " + sampleTestId));
        return mapToDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultDto getResultById(UUID id) {
        TestResult result = testResultRepository.findById(id)
                .filter(tr -> !tr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
        return mapToDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultDto> getPendingReviewResults() {
        return testResultRepository.findByStatusIn(List.of(ResultStatus.SUBMITTED, ResultStatus.UNDER_REVIEW)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestResultDto verifyResult(UUID id, String remarks) {
        TestResult result = testResultRepository.findById(id)
                .filter(tr -> !tr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Test result not found with id: " + id));

        if (result.getStatus() != ResultStatus.SUBMITTED && result.getStatus() != ResultStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Only submitted results can be verified. Current status: " + result.getStatus());
        }

        SampleTest sampleTest = result.getSampleTest();

        result.setStatus(ResultStatus.VERIFIED);
        if (remarks != null) {
            result.setRemarks(remarks);
        }
        TestResult savedResult = testResultRepository.save(result);

        // Transition SampleTest status to VERIFIED
        SampleTestStatus oldStatus = sampleTest.getStatus();
        sampleTest.setStatus(SampleTestStatus.VERIFIED);
        sampleTestRepository.save(sampleTest);
        logTestHistory(sampleTest, "STATUS_CHANGE", sampleTest.getTechnician(), sampleTest.getTechnician(),
                sampleTest.getDueDate(), sampleTest.getDueDate(), oldStatus, SampleTestStatus.VERIFIED, "Verified by QA/QE: " + remarks);

        // Transition Sample status
        Sample sample = sampleTest.getSample();
        
        List<SampleTest> allTests = sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sample.getId());
        boolean allVerified = allTests.stream()
                .allMatch(st -> st.getStatus() == SampleTestStatus.VERIFIED);
                
        if (allVerified) {
            SampleStatus oldSampleStatus = sample.getStatus();
            sample.setStatus(SampleStatus.REVIEW);
            sampleRepository.save(sample);
            logSampleHistory(sample, oldSampleStatus, SampleStatus.REVIEW, "All assigned lab tests verified. Ready for final approval sign-off.");
        }

        logAudit("VERIFY_RESULT", "Verified testing result for sample: " + sample.getSampleId());

        return mapToDto(savedResult);
    }

    @Override
    @Transactional
    public TestResultDto approveResult(UUID id, String remarks) {
        TestResult result = testResultRepository.findById(id)
                .filter(tr -> !tr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Test result not found with id: " + id));

        if (result.getStatus() != ResultStatus.VERIFIED) {
            throw new IllegalArgumentException("Only verified results can be approved. Current status: " + result.getStatus());
        }

        SampleTest sampleTest = result.getSampleTest();

        result.setStatus(ResultStatus.APPROVED);
        if (remarks != null) {
            result.setRemarks(remarks);
        }
        TestResult savedResult = testResultRepository.save(result);

        Sample sample = sampleTest.getSample();
        List<SampleTest> allTests = sampleTestRepository.findBySampleIdOrderBySequenceNumberAsc(sample.getId());
        
        boolean allApproved = true;
        for (SampleTest st : allTests) {
            TestResult tr = testResultRepository.findBySampleTestId(st.getId()).orElse(null);
            if (tr == null || tr.getStatus() != ResultStatus.APPROVED) {
                allApproved = false;
                break;
            }
        }

        if (allApproved) {
            SampleStatus oldSampleStatus = sample.getStatus();
            sample.setStatus(SampleStatus.APPROVED);
            sampleRepository.save(sample);
            logSampleHistory(sample, oldSampleStatus, SampleStatus.APPROVED, "All testing results approved. Ready for PDF report release.");

            publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                this,
                "RESULT_APPROVED",
                "Sample Approved: " + sample.getSampleId(),
                String.format("All test results for Sample %s have been approved. Ready for report generation.", sample.getSampleId()),
                com.lms.backend.entity.NotificationPriority.MEDIUM,
                "ROLE_LAB_MANAGER,ROLE_ADMIN",
                null
            ));
        }

        logAudit("APPROVE_RESULT", "Approved testing result for sample: " + sample.getSampleId());

        return mapToDto(savedResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultDto> getPendingApprovalResults() {
        return testResultRepository.findByStatus(ResultStatus.VERIFIED).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultDto> getTechnicianResults(UUID technicianId) {
        return testResultRepository.findBySampleTestTechnicianId(technicianId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getTechnicianMetrics(UUID technicianId) {
        List<SampleTest> techTests = sampleTestRepository.findByTechnicianId(technicianId);

        long assigned = 0;
        long inProgress = 0;
        long completed = 0;
        long overdue = 0;
        LocalDate today = LocalDate.now();

        for (SampleTest st : techTests) {
            if (st.isDeleted()) continue;
            
            SampleTestStatus status = st.getStatus();
            if (status == SampleTestStatus.ASSIGNED) {
                assigned++;
            } else if (status == SampleTestStatus.IN_PROGRESS || status == SampleTestStatus.ON_HOLD) {
                inProgress++;
            } else if (status == SampleTestStatus.COMPLETED || status == SampleTestStatus.VERIFIED) {
                completed++;
            }

            // Overdue check
            if (status != SampleTestStatus.COMPLETED && status != SampleTestStatus.VERIFIED) {
                if (st.getDueDate() != null && st.getDueDate().isBefore(today)) {
                    overdue++;
                }
            }
        }

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("assigned", assigned);
        metrics.put("inProgress", inProgress);
        metrics.put("completed", completed);
        metrics.put("overdue", overdue);

        return metrics;
    }

    private PassFailStatus evaluatePassFail(String specOperator, String specValue, String finalResult) {
        if (specOperator == null || specValue == null || finalResult == null || finalResult.trim().isEmpty()) {
            return PassFailStatus.NONE;
        }
        String op = specOperator.trim().toUpperCase();
        String valStr = specValue.trim();
        String resStr = finalResult.trim();

        try {
            if (">=".equals(op) || "GE".equals(op)) {
                double val = Double.parseDouble(valStr);
                double res = Double.parseDouble(resStr);
                return res >= val ? PassFailStatus.PASS : PassFailStatus.FAIL;
            } else if ("<=".equals(op) || "LE".equals(op)) {
                double val = Double.parseDouble(valStr);
                double res = Double.parseDouble(resStr);
                return res <= val ? PassFailStatus.PASS : PassFailStatus.FAIL;
            } else if (">".equals(op) || "GT".equals(op)) {
                double val = Double.parseDouble(valStr);
                double res = Double.parseDouble(resStr);
                return res > val ? PassFailStatus.PASS : PassFailStatus.FAIL;
            } else if ("<".equals(op) || "LT".equals(op)) {
                double val = Double.parseDouble(valStr);
                double res = Double.parseDouble(resStr);
                return res < val ? PassFailStatus.PASS : PassFailStatus.FAIL;
            } else if ("==".equals(op) || "EQUAL".equals(op) || "EQUALS".equals(op)) {
                try {
                    double val = Double.parseDouble(valStr);
                    double res = Double.parseDouble(resStr);
                    return res == val ? PassFailStatus.PASS : PassFailStatus.FAIL;
                } catch (NumberFormatException e) {
                    return resStr.equalsIgnoreCase(valStr) ? PassFailStatus.PASS : PassFailStatus.FAIL;
                }
            } else if ("BETWEEN".equals(op) || "RANGE".equals(op)) {
                String[] parts = valStr.split("[-_]");
                if (parts.length == 2) {
                    double low = Double.parseDouble(parts[0].trim());
                    double high = Double.parseDouble(parts[1].trim());
                    double res = Double.parseDouble(resStr);
                    return (res >= low && res <= high) ? PassFailStatus.PASS : PassFailStatus.FAIL;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric values for pass/fail check: op={}, val={}, res={}", op, valStr, resStr);
        }
        return PassFailStatus.NONE;
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
                .changedBy(user)
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

    private TestResultDto mapToDto(TestResult tr) {
        return TestResultDto.builder()
                .id(tr.getId())
                .sampleTestId(tr.getSampleTest().getId())
                .sampleId(tr.getSampleTest().getSample().getId())
                .sampleCode(tr.getSampleTest().getSample().getSampleId())
                .testDefinitionId(tr.getSampleTest().getTestDefinition().getId())
                .testName(tr.getSampleTest().getTestDefinition().getTestName())
                .observations(tr.getObservations())
                .calculations(tr.getCalculations())
                .finalResult(tr.getFinalResult())
                .unit(tr.getUnit())
                .specOperator(tr.getSpecOperator())
                .specValue(tr.getSpecValue())
                .passFail(tr.getPassFail().name())
                .attachments(tr.getAttachments())
                .version(tr.getVersion())
                .remarks(tr.getRemarks())
                .status(tr.getStatus().name())
                .testedById(tr.getTestedBy() != null ? tr.getTestedBy().getId() : null)
                .testedByName(tr.getTestedBy() != null ? tr.getTestedBy().getName() : null)
                .testingStartedAt(tr.getTestingStartedAt())
                .testingCompletedAt(tr.getTestingCompletedAt())
                .submittedAt(tr.getSubmittedAt())
                .build();
    }
}
