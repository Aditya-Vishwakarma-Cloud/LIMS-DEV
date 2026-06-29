package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleTestAssignmentDto {

    @NotNull(message = "Assignment mode is required")
    private String mode; // INITIAL, ADDITIONAL, REASSIGN

    private List<SingleTestAssignment> assignments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleTestAssignment {
        @NotNull(message = "Test Definition ID is required")
        private UUID testDefinitionId;

        private UUID technicianId;
        private LocalDate scheduledDate;
        private LocalDate dueDate;
        private int sequenceNumber;
        private String remarks;
    }
}
