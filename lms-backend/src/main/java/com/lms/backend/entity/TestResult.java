package com.lms.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class TestResult extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_test_id", nullable = false, unique = true)
    private SampleTest sampleTest;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations; // JSON structure for multi-value entries (e.g. {"4.75mm": "98.5"})

    @Column(name = "calculations", columnDefinition = "TEXT")
    private String calculations; // JSON structure for calculated metrics (e.g. {"area": "12.5"})

    @Column(name = "final_result", length = 255)
    private String finalResult;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "spec_operator", length = 50)
    private String specOperator;

    @Column(name = "spec_value", length = 255)
    private String specValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_fail", nullable = false, length = 30)
    @Builder.Default
    private PassFailStatus passFail = PassFailStatus.NONE;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // JSON array of filenames/URLs (e.g. ["cert.pdf", "img.jpg"])

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tested_by_id")
    private User testedBy;

    @Column(name = "testing_started_at")
    private LocalDateTime testingStartedAt;

    @Column(name = "testing_completed_at")
    private LocalDateTime testingCompletedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ResultStatus status = ResultStatus.DRAFT;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}
