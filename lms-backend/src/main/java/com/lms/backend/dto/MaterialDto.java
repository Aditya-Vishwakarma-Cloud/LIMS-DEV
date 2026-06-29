package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDto {
    private UUID id;

    @NotBlank(message = "Material Code is required")
    private String materialCode;

    @NotBlank(message = "Material Name is required")
    private String materialName;

    @NotBlank(message = "Sample Prefix is required")
    private String samplePrefix;

    private String description;
    private boolean active;
}
