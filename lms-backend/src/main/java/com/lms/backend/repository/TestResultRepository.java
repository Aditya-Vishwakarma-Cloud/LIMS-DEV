package com.lms.backend.repository;

import com.lms.backend.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    Optional<TestResult> findBySampleTestId(UUID sampleTestId);
    java.util.List<TestResult> findByStatus(com.lms.backend.entity.ResultStatus status);
    java.util.List<TestResult> findByStatusIn(java.util.List<com.lms.backend.entity.ResultStatus> statuses);
    java.util.List<TestResult> findBySampleTestTechnicianId(UUID technicianId);
    long countByStatus(com.lms.backend.entity.ResultStatus status);
    long countByStatusIn(java.util.Collection<com.lms.backend.entity.ResultStatus> statuses);
}

