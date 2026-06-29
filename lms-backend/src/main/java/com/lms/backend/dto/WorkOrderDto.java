package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDto {
    private UUID id;
    private String workOrderNumber;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    private String customerName;

    @NotNull(message = "Project ID is required")
    private UUID projectId;
    private String projectName;

    @NotNull(message = "Received Date is required")
    private LocalDate receivedDate;
    
    private LocalDate dueDate;
    private String priority;
    
    private UUID requestedById;
    private String requestedByName;
    
    private String remarks;
    private String status;
}
