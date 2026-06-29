package com.lms.backend.repository;

import com.lms.backend.entity.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SampleRepository extends JpaRepository<Sample, UUID> {
    List<Sample> findByWorkOrderId(UUID workOrderId);
    Optional<Sample> findFirstBySampleIdStartingWithOrderBySampleIdDesc(String prefix);
    Long countByWorkOrderCustomerId(UUID customerId);
    List<Sample> findByStatusInAndDeletedFalse(List<com.lms.backend.entity.SampleStatus> statuses);

    @Query("SELECT COUNT(s) FROM Sample s WHERE s.createdAt >= :startOfDay AND s.deleted = false")
    long countByCreatedAtAfter(@org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay);

    @Query("SELECT COUNT(s) FROM Sample s WHERE s.status = :status AND s.deleted = false")
    long countByStatusAndDeletedFalse(@org.springframework.data.repository.query.Param("status") com.lms.backend.entity.SampleStatus status);

    @Query("SELECT COUNT(s) FROM Sample s WHERE s.status IN :statuses AND s.deleted = false")
    long countByStatusInAndDeletedFalse(@org.springframework.data.repository.query.Param("statuses") java.util.Collection<com.lms.backend.entity.SampleStatus> statuses);

    @Query("SELECT COUNT(s) FROM Sample s WHERE s.status IN :statuses AND s.updatedAt >= :startOfDay AND s.deleted = false")
    long countByStatusInAndUpdatedAtAfterAndDeletedFalse(
        @org.springframework.data.repository.query.Param("statuses") java.util.Collection<com.lms.backend.entity.SampleStatus> statuses,
        @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay
    );

    @Query("SELECT s.material.materialCode, COUNT(s) FROM Sample s WHERE s.deleted = false GROUP BY s.material.materialCode")
    List<Object[]> countSamplesByMaterial();

    long countByWorkOrderCustomerIdAndStatusInAndDeletedFalse(UUID customerId, java.util.Collection<com.lms.backend.entity.SampleStatus> statuses);

    List<Sample> findByWorkOrderCustomerIdAndDeletedFalseOrderByCreatedAtDesc(UUID customerId);
}

