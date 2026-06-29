package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleDto {
    private UUID id;
    private String sampleId;

    @NotNull(message = "Work Order ID is required")
    private UUID workOrderId;
    private String workOrderNumber;
    private String customerName;
    private String projectName;

    @NotNull(message = "Material ID is required")
    private UUID materialId;
    private String materialName;
    private String materialCode;

    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;
    
    private String unit;
    
    private LocalDate collectionDate;
    private String collectionLocation;
    
    private UUID collectedById;
    private String collectedByName;
    
    private LocalDate receivedDate;
    private LocalTime receivedTime;
    
    private UUID receivedById;
    private String receivedByName;
    
    private String condition;
    private String status;
    private String priority;
    private String remarks;
}
