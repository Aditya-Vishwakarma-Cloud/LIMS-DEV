package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultDto {
    private UUID id;
    private UUID sampleTestId;
    private UUID sampleId;
    private String sampleCode;
    private UUID testDefinitionId;
    private String testName;
    private String observations; // JSON format
    private String calculations;  // JSON format
    private String finalResult;
    private String unit;
    private String specOperator;
    private String specValue;
    private String passFail;
    private String attachments;  // JSON array format
    private int version;
    private String remarks;
    private String status;
    private UUID testedById;
    private String testedByName;
    private LocalDateTime testingStartedAt;
    private LocalDateTime testingCompletedAt;
    private LocalDateTime submittedAt;
}
