package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private UUID id;
    private String projectCode;
    
    private String projectNumber;

    @NotBlank(message = "Project Name is required")
    private String projectName;

    private String siteName;
    private String engineer;
    private String consultant;
    private String contractor;
    private String location;
    private LocalDate expectedCompletion;
    private String status;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    private String customerName;
}
