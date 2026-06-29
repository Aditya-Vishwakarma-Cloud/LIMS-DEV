package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDefinitionDto {
    private UUID id;

    @NotNull(message = "Material ID is required")
    private UUID materialId;
    
    private String materialName;
    private String materialCode;

    @NotBlank(message = "Test Name is required")
    private String testName;

    private String testCode;
    private String unit;
    private String specification;
    private String specOperator;
    private String specValue;
    private String valueType;
    private String method;
    private boolean isMandatory;
    private boolean active;
}
