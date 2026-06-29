package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleTestDto {
    private UUID id;
    private UUID sampleId;
    private String sampleCode;
    private UUID testDefinitionId;
    private String testName;
    private String testCode;
    private String unit;
    private String specification;
    private String specOperator;
    private String specValue;
    private String valueType;
    private String method;
    private boolean isMandatory;
    private UUID technicianId;
    private String technicianName;
    private UUID assignedById;
    private String assignedByName;
    private LocalDate assignedDate;
    private LocalDate scheduledDate;
    private LocalDate dueDate;
    private int sequenceNumber;
    private String status;
    private String remarks;
}
