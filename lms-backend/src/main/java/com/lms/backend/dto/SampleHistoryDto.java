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
public class SampleHistoryDto {
    private UUID id;
    private UUID sampleId;
    private String sampleCode;
    private String oldStatus;
    private String newStatus;
    private UUID changedById;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;
}
