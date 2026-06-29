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

@Entity
@Table(name = "test_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "test_code", length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "specification", length = 255)
    private String specification;

    @Column(name = "spec_operator", length = 50)
    private String specOperator; // e.g. >=, <=, BETWEEN, RANGE, EQUAL, NONE

    @Column(name = "spec_value", length = 255)
    private String specValue;

    @Column(name = "value_type", length = 50)
    private String valueType; // e.g. NUMERIC, TEXT, TIME, SIEVE_ANALYSIS

    @Column(name = "method", length = 255)
    private String method;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private boolean isMandatory = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
