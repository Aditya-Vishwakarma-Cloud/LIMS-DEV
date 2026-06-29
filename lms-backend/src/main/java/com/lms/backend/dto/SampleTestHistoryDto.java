package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleTestHistoryDto {
    private UUID id;
    private UUID sampleTestId;
    private String changeType;
    private UUID oldTechnicianId;
    private String oldTechnicianName;
    private UUID newTechnicianId;
    private String newTechnicianName;
    private LocalDate oldDueDate;
    private LocalDate newDueDate;
    private String oldStatus;
    private String newStatus;
    private UUID changedById;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;
}
