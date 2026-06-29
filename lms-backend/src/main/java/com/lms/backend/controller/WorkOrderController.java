package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.WorkOrderDto;
import com.lms.backend.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    @PreAuthorize("hasAuthority('WORKORDER_CREATE')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> createWorkOrder(@Valid @RequestBody WorkOrderDto dto) {
        WorkOrderDto created = workOrderService.createWorkOrder(dto);
        return new ResponseEntity<>(ApiResponse.success(created, "Work order created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKORDER_EDIT')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> updateWorkOrder(@PathVariable UUID id, @Valid @RequestBody WorkOrderDto dto) {
        WorkOrderDto updated = workOrderService.updateWorkOrder(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Work order updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKORDER_VIEW')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> getWorkOrderById(@PathVariable UUID id) {
        WorkOrderDto wo = workOrderService.getWorkOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(wo, "Work order retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKORDER_VIEW')")
    public ResponseEntity<ApiResponse<List<WorkOrderDto>>> getAllWorkOrders() {
        List<WorkOrderDto> wos = workOrderService.getAllWorkOrders();
        return ResponseEntity.ok(ApiResponse.success(wos, "Work orders retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('WORKORDER_VIEW')")
    public ResponseEntity<ApiResponse<List<WorkOrderDto>>> getWorkOrdersByCustomerId(@PathVariable UUID customerId) {
        List<WorkOrderDto> wos = workOrderService.getWorkOrdersByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(wos, "Work orders for customer retrieved successfully"));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('WORKORDER_VIEW')")
    public ResponseEntity<ApiResponse<List<WorkOrderDto>>> getWorkOrdersByProjectId(@PathVariable UUID projectId) {
        List<WorkOrderDto> wos = workOrderService.getWorkOrdersByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(wos, "Work orders for project retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKORDER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteWorkOrder(@PathVariable UUID id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Work order deleted successfully"));
    }
}
