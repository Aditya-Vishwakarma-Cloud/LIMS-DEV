package com.lms.backend.service;

import com.lms.backend.dto.WorkOrderDto;
import java.util.List;
import java.util.UUID;

public interface WorkOrderService {
    WorkOrderDto createWorkOrder(WorkOrderDto workOrderDto);
    WorkOrderDto updateWorkOrder(UUID id, WorkOrderDto workOrderDto);
    WorkOrderDto getWorkOrderById(UUID id);
    List<WorkOrderDto> getAllWorkOrders();
    List<WorkOrderDto> getWorkOrdersByCustomerId(UUID customerId);
    List<WorkOrderDto> getWorkOrdersByProjectId(UUID projectId);
    void deleteWorkOrder(UUID id);
}
