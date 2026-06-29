package com.lms.backend.service.impl;

import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleHistoryDto;
import com.lms.backend.entity.Material;
import com.lms.backend.entity.Sample;
import com.lms.backend.entity.SampleCondition;
import com.lms.backend.entity.SampleHistory;
import com.lms.backend.entity.SampleStatus;
import com.lms.backend.entity.User;
import com.lms.backend.entity.WorkOrder;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.MaterialRepository;
import com.lms.backend.repository.SampleHistoryRepository;
import com.lms.backend.repository.SampleRepository;
import com.lms.backend.repository.UserRepository;
import com.lms.backend.repository.WorkOrderRepository;
import com.lms.backend.service.SampleService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SampleServiceImpl implements SampleService {

    private final SampleRepository sampleRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final SampleHistoryRepository sampleHistoryRepository;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public SampleDto createSample(SampleDto sampleDto) {
        WorkOrder workOrder = workOrderRepository.findById(sampleDto.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + sampleDto.getWorkOrderId()));

        Material material = materialRepository.findById(sampleDto.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + sampleDto.getMaterialId()));

        User collectedBy = null;
        if (sampleDto.getCollectedById() != null) {
            collectedBy = userRepository.findById(sampleDto.getCollectedById()).orElse(null);
        }

        String sampleId = generateSampleId(material);

        Sample sample = Sample.builder()
                .sampleId(sampleId)
                .workOrder(workOrder)
                .material(material)
                .quantity(sampleDto.getQuantity())
                .unit(sampleDto.getUnit() != null ? sampleDto.getUnit() : "Nos")
                .collectionDate(sampleDto.getCollectionDate() != null ? sampleDto.getCollectionDate() : LocalDate.now())
                .collectionLocation(sampleDto.getCollectionLocation())
                .collectedBy(collectedBy)
                .collectedByName(sampleDto.getCollectedByName())
                .status(SampleStatus.REGISTERED)
                .priority(sampleDto.getPriority() != null ? sampleDto.getPriority() : "Normal")
                .remarks(sampleDto.getRemarks())
                .deleted(false)
                .build();

        Sample saved = sampleRepository.save(sample);
        logAudit("CREATE_SAMPLE", "Registered sample: " + saved.getSampleId());
        
        // Log history of registration
        logHistory(saved, null, SampleStatus.REGISTERED, "Sample Registered");

        publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
            this,
            "SAMPLE_REGISTERED",
            "New Sample Registered: " + saved.getSampleId(),
            String.format("Sample ID %s has been registered. Priority: %s.", saved.getSampleId(), saved.getPriority()),
            com.lms.backend.entity.NotificationPriority.MEDIUM,
            "ROLE_LAB_MANAGER",
            null
        ));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SampleDto updateSample(UUID id, SampleDto dto) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + id));

        WorkOrder workOrder = workOrderRepository.findById(dto.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + dto.getWorkOrderId()));

        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + dto.getMaterialId()));

        User collectedBy = null;
        if (dto.getCollectedById() != null) {
            collectedBy = userRepository.findById(dto.getCollectedById()).orElse(null);
        }

        sample.setWorkOrder(workOrder);
        sample.setMaterial(material);
        sample.setQuantity(dto.getQuantity());
        sample.setUnit(dto.getUnit());
        sample.setCollectionDate(dto.getCollectionDate());
        sample.setCollectionLocation(dto.getCollectionLocation());
        sample.setCollectedBy(collectedBy);
        sample.setCollectedByName(dto.getCollectedByName());
        sample.setPriority(dto.getPriority());
        sample.setRemarks(dto.getRemarks());

        Sample saved = sampleRepository.save(sample);
        logAudit("UPDATE_SAMPLE", "Updated sample details: " + saved.getSampleId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleDto getSampleById(UUID id) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + id));
        return mapToDto(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleDto> getAllSamples() {
        return sampleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleDto> getSamplesByWorkOrderId(UUID workOrderId) {
        return sampleRepository.findByWorkOrderId(workOrderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SampleDto receiveSample(UUID id, SampleDto receiveDetails) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + id));

        User receivedBy = null;
        if (receiveDetails.getReceivedById() != null) {
            receivedBy = userRepository.findById(receiveDetails.getReceivedById()).orElse(null);
        } else {
            receivedBy = getCurrentUser();
        }

        SampleStatus oldStatus = sample.getStatus();
        SampleStatus newStatus = SampleStatus.RECEIVED;

        sample.setReceivedDate(receiveDetails.getReceivedDate() != null ? receiveDetails.getReceivedDate() : LocalDate.now());
        sample.setReceivedTime(receiveDetails.getReceivedTime() != null ? receiveDetails.getReceivedTime() : LocalTime.now());
        sample.setReceivedBy(receivedBy);
        sample.setCondition(receiveDetails.getCondition() != null ? SampleCondition.valueOf(receiveDetails.getCondition()) : SampleCondition.GOOD);
        sample.setStatus(newStatus);

        Sample saved = sampleRepository.save(sample);
        logAudit("RECEIVE_SAMPLE", "Received sample physically: " + saved.getSampleId());
        logHistory(saved, oldStatus, newStatus, "Sample received in " + saved.getCondition() + " condition. " + (receiveDetails.getRemarks() != null ? receiveDetails.getRemarks() : ""));

        publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
            this,
            "SAMPLE_RECEIVED",
            "New Sample Received: " + saved.getSampleId(),
            String.format("Sample ID %s has been received physically at the lab in %s condition.", saved.getSampleId(), saved.getCondition()),
            com.lms.backend.entity.NotificationPriority.MEDIUM,
            "ROLE_LAB_MANAGER",
            null
        ));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SampleDto updateSampleStatus(UUID id, String status, String remarks) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + id));

        SampleStatus oldStatus = sample.getStatus();
        SampleStatus newStatus = SampleStatus.valueOf(status.toUpperCase());

        sample.setStatus(newStatus);
        Sample saved = sampleRepository.save(sample);

        logAudit("UPDATE_SAMPLE_STATUS", "Status of sample " + saved.getSampleId() + " updated to " + newStatus);
        logHistory(saved, oldStatus, newStatus, remarks);

        // Intercept statuses for notifications
        if (newStatus == SampleStatus.REPORT_GENERATED) {
            publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                this,
                "REPORT_GENERATED",
                "Report Generated: " + saved.getSampleId(),
                String.format("Final test report for Sample %s has been successfully compiled and generated.", saved.getSampleId()),
                com.lms.backend.entity.NotificationPriority.MEDIUM,
                "ROLE_LAB_MANAGER,ROLE_ADMIN",
                null
            ));
        } else if (newStatus == SampleStatus.DELIVERED) {
            String clientEmail = saved.getWorkOrder() != null && saved.getWorkOrder().getCustomer() != null 
                    ? saved.getWorkOrder().getCustomer().getEmailId() : null;

            if (clientEmail != null && !clientEmail.isEmpty()) {
                String clientName = saved.getWorkOrder().getCustomer().getContactPerson() != null 
                        ? saved.getWorkOrder().getCustomer().getContactPerson() : "Valued Customer";

                publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                    this,
                    "REPORT_RELEASED",
                    "Laboratory Test Report Ready",
                    String.format("Hello %s, your quality certification test report for Sample ID %s is now available for download. Regards, WeMurz LIMS.", clientName, saved.getSampleId()),
                    com.lms.backend.entity.NotificationPriority.MEDIUM,
                    null,
                    clientEmail
                ));

                // Also notify admin of invoice generation
                publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
                    this,
                    "INVOICE_GENERATED",
                    "Invoice Issued: " + saved.getSampleId(),
                    String.format("Tax invoice and report files released for client core sample: %s.", saved.getSampleId()),
                    com.lms.backend.entity.NotificationPriority.MEDIUM,
                    "ROLE_ADMIN",
                    null
                ));
            }
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleHistoryDto> getSampleHistory(UUID sampleId) {
        return sampleHistoryRepository.findBySampleIdOrderByChangedAtDesc(sampleId).stream()
                .map(this::mapHistoryToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSample(UUID id) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found with id: " + id));

        String username = getCurrentUsername();
        sample.setDeleted(true);
        sample.setDeletedAt(LocalDateTime.now());
        sample.setDeletedBy(username);

        sampleRepository.save(sample);
        logAudit("DELETE_SAMPLE", "Soft deleted sample: " + sample.getSampleId());
    }

    private synchronized String generateSampleId(Material material) {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("%s-%d-", material.getSamplePrefix(), currentYear);
        Optional<Sample> lastSample = sampleRepository.findFirstBySampleIdStartingWithOrderBySampleIdDesc(prefix);
        if (lastSample.isPresent()) {
            String id = lastSample.get().getSampleId();
            try {
                int num = Integer.parseInt(id.substring(prefix.length()));
                return String.format("%s%04d", prefix, num + 1);
            } catch (Exception e) {
                // Fallback
            }
        }
        return prefix + "0001";
    }

    private void logAudit(String action, String details) {
        auditLogService.log(action, getCurrentUsername(), "localhost", details);
    }

    private void logHistory(Sample sample, SampleStatus oldStatus, SampleStatus newStatus, String remarks) {
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

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByEmail(username).orElse(null);
    }

    private SampleDto mapToDto(Sample s) {
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
                .collectedById(s.getCollectedBy() != null ? s.getCollectedBy().getId() : null)
                .collectedByName(s.getCollectedByName() != null ? s.getCollectedByName() : (s.getCollectedBy() != null ? s.getCollectedBy().getName() : null))
                .receivedDate(s.getReceivedDate())
                .receivedTime(s.getReceivedTime())
                .receivedById(s.getReceivedBy() != null ? s.getReceivedBy().getId() : null)
                .receivedByName(s.getReceivedBy() != null ? s.getReceivedBy().getName() : null)
                .condition(s.getCondition() != null ? s.getCondition().name() : null)
                .status(s.getStatus().name())
                .priority(s.getPriority())
                .remarks(s.getRemarks())
                .build();
    }

    private SampleHistoryDto mapHistoryToDto(SampleHistory h) {
        return SampleHistoryDto.builder()
                .id(h.getId())
                .sampleId(h.getSample().getId())
                .sampleCode(h.getSample().getSampleId())
                .oldStatus(h.getOldStatus() != null ? h.getOldStatus().name() : null)
                .newStatus(h.getNewStatus().name())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : "System")
                .changedAt(h.getChangedAt())
                .remarks(h.getRemarks())
                .build();
    }
}
