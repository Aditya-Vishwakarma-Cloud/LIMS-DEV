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
public class ContactPersonDto {
    private UUID id;
    
    @NotBlank(message = "Contact Person Name is required")
    private String name;
    
    private String designation;
    private String phone;
    private String email;
    private UUID customerId;
}
