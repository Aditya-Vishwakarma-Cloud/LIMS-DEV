package com.lms.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Project extends BaseEntity {

    @Column(name = "project_code", nullable = false, unique = true, length = 50)
    private String projectCode;

    @Column(name = "project_number", length = 100)
    private String projectNumber;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "site_name", length = 200)
    private String siteName;

    @Column(name = "engineer", length = 100)
    private String engineer;

    @Column(name = "consultant", length = 100)
    private String consultant;

    @Column(name = "contractor", length = 100)
    private String contractor;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "expected_completion")
    private LocalDate expectedCompletion;

    @Column(name = "status", length = 20)
    private String status; // Active / Inactive

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}
