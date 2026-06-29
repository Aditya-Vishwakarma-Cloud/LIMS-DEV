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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "samples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Sample extends BaseEntity {

    @Column(name = "sample_id", nullable = false, unique = true, length = 50)
    private String sampleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "collection_date")
    private LocalDate collectionDate;

    @Column(name = "collection_location", length = 200)
    private String collectionLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_id")
    private User collectedBy;

    @Column(name = "collected_by_name", length = 100)
    private String collectedByName;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "received_time")
    private LocalTime receivedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_id")
    private User receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 30)
    private SampleCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SampleStatus status;

    @Column(name = "priority", length = 20)
    private String priority; // Low, Medium, High, Urgent

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
