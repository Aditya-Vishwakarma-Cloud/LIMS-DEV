package com.lms.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sample_test_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTestHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_test_id", nullable = false)
    private SampleTest sampleTest;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // e.g. INITIAL_ASSIGN, REASSIGN, TECHNICIAN_CHANGE, DUE_DATE_CHANGE, STATUS_CHANGE

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "old_technician_id")
    private User oldTechnician;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "new_technician_id")
    private User newTechnician;

    @Column(name = "old_due_date")
    private LocalDate oldDueDate;

    @Column(name = "new_due_date")
    private LocalDate newDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private SampleTestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private SampleTestStatus newStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
