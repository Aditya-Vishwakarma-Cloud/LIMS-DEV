package com.lms.backend.service.impl;

import com.lms.backend.dto.WorkOrderDto;
import com.lms.backend.entity.ContactPerson;
import com.lms.backend.entity.Customer;
import com.lms.backend.entity.Project;
import com.lms.backend.entity.WorkOrder;
import com.lms.backend.entity.WorkOrderStatus;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.ContactPersonRepository;
import com.lms.backend.repository.CustomerRepository;
import com.lms.backend.repository.ProjectRepository;
import com.lms.backend.repository.WorkOrderRepository;
import com.lms.backend.service.WorkOrderService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public WorkOrderDto createWorkOrder(WorkOrderDto workOrderDto) {
        Customer customer = customerRepository.findById(workOrderDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + workOrderDto.getCustomerId()));

        Project project = projectRepository.findById(workOrderDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + workOrderDto.getProjectId()));

        ContactPerson requestedBy = null;
        if (workOrderDto.getRequestedById() != null) {
            requestedBy = contactPersonRepository.findById(workOrderDto.getRequestedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact person not found with id: " + workOrderDto.getRequestedById()));
        }

        String woNumber = generateWorkOrderNumber();

        WorkOrder wo = WorkOrder.builder()
                .workOrderNumber(woNumber)
                .customer(customer)
                .project(project)
                .receivedDate(workOrderDto.getReceivedDate() != null ? workOrderDto.getReceivedDate() : LocalDate.now())
                .dueDate(workOrderDto.getDueDate())
                .priority(workOrderDto.getPriority() != null ? workOrderDto.getPriority() : "Medium")
                .requestedBy(requestedBy)
                .remarks(workOrderDto.getRemarks())
                .status(workOrderDto.getStatus() != null ? WorkOrderStatus.valueOf(workOrderDto.getStatus()) : WorkOrderStatus.OPEN)
                .deleted(false)
                .build();

        WorkOrder saved = workOrderRepository.save(wo);
        logAudit("CREATE_WORK_ORDER", "Created work order: " + saved.getWorkOrderNumber());
        
        publisher.publishEvent(new com.lms.backend.event.LimsNotificationEvent(
            this,
            "WORK_ORDER_CREATED",
            "New Work Order: " + saved.getWorkOrderNumber(),
            String.format("Customer: %s has created work order %s for project %s.", customer.getCustomerName(), saved.getWorkOrderNumber(), project.getProjectName()),
            com.lms.backend.entity.NotificationPriority.MEDIUM,
            "ROLE_RECEPTION,ROLE_LAB_MANAGER,ROLE_ADMIN",
            null
        ));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public WorkOrderDto updateWorkOrder(UUID id, WorkOrderDto dto) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + dto.getCustomerId()));

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + dto.getProjectId()));

        ContactPerson requestedBy = null;
        if (dto.getRequestedById() != null) {
            requestedBy = contactPersonRepository.findById(dto.getRequestedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact person not found with id: " + dto.getRequestedById()));
        }

        wo.setCustomer(customer);
        wo.setProject(project);
        wo.setReceivedDate(dto.getReceivedDate());
        wo.setDueDate(dto.getDueDate());
        wo.setPriority(dto.getPriority());
        wo.setRequestedBy(requestedBy);
        wo.setRemarks(dto.getRemarks());
        if (dto.getStatus() != null) {
            wo.setStatus(WorkOrderStatus.valueOf(dto.getStatus()));
        }

        WorkOrder saved = workOrderRepository.save(wo);
        logAudit("UPDATE_WORK_ORDER", "Updated work order: " + saved.getWorkOrderNumber());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderDto getWorkOrderById(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));
        return mapToDto(wo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderDto> getAllWorkOrders() {
        return workOrderRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderDto> getWorkOrdersByCustomerId(UUID customerId) {
        return workOrderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderDto> getWorkOrdersByProjectId(UUID projectId) {
        return workOrderRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteWorkOrder(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));

        String username = getCurrentUsername();
        wo.setDeleted(true);
        wo.setDeletedAt(LocalDateTime.now());
        wo.setDeletedBy(username);

        workOrderRepository.save(wo);
        logAudit("DELETE_WORK_ORDER", "Soft deleted work order: " + wo.getWorkOrderNumber());
    }

    private synchronized String generateWorkOrderNumber() {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("WO-%d-", currentYear);
        Optional<WorkOrder> lastWO = workOrderRepository.findFirstByWorkOrderNumberStartingWithOrderByWorkOrderNumberDesc(prefix);
        if (lastWO.isPresent()) {
            String number = lastWO.get().getWorkOrderNumber();
            try {
                int num = Integer.parseInt(number.substring(prefix.length()));
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

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
    }

    private WorkOrderDto mapToDto(WorkOrder wo) {
        return WorkOrderDto.builder()
                .id(wo.getId())
                .workOrderNumber(wo.getWorkOrderNumber())
                .customerId(wo.getCustomer().getId())
                .customerName(wo.getCustomer().getCustomerName())
                .projectId(wo.getProject().getId())
                .projectName(wo.getProject().getProjectName())
                .receivedDate(wo.getReceivedDate())
                .dueDate(wo.getDueDate())
                .priority(wo.getPriority())
                .requestedById(wo.getRequestedBy() != null ? wo.getRequestedBy().getId() : null)
                .requestedByName(wo.getRequestedBy() != null ? wo.getRequestedBy().getName() : null)
                .remarks(wo.getRemarks())
                .status(wo.getStatus().name())
                .build();
    }
}
