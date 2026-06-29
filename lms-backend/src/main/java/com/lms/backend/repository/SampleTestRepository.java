package com.lms.backend.repository;

import com.lms.backend.entity.SampleTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SampleTestRepository extends JpaRepository<SampleTest, UUID> {
    List<SampleTest> findBySampleIdOrderBySequenceNumberAsc(UUID sampleId);
    
    List<SampleTest> findByTechnicianId(UUID technicianId);

    @Query("SELECT COUNT(st) FROM SampleTest st WHERE st.technician.id = :techId AND st.status = :status AND st.deleted = false")
    long countByTechnicianIdAndStatus(@Param("techId") UUID techId, @Param("status") String status);

    @Query("SELECT COUNT(st) FROM SampleTest st WHERE st.dueDate < :today AND st.status NOT IN (com.lms.backend.entity.SampleTestStatus.COMPLETED, com.lms.backend.entity.SampleTestStatus.VERIFIED) AND st.deleted = false")
    long countOverdueTests(@Param("today") java.time.LocalDate today);

    @Query("SELECT COUNT(st) FROM SampleTest st WHERE st.status IN (com.lms.backend.entity.SampleTestStatus.ASSIGNED, com.lms.backend.entity.SampleTestStatus.IN_PROGRESS) AND st.deleted = false")
    long countActiveTests();

    @Query("SELECT COUNT(st) FROM SampleTest st WHERE st.status IN (com.lms.backend.entity.SampleTestStatus.COMPLETED, com.lms.backend.entity.SampleTestStatus.VERIFIED) AND st.deleted = false")
    long countCompletedTests();

    @Query("SELECT COUNT(st) FROM SampleTest st WHERE st.status IN (com.lms.backend.entity.SampleTestStatus.COMPLETED, com.lms.backend.entity.SampleTestStatus.VERIFIED) AND st.updatedAt >= :startOfDay AND st.deleted = false")
    long countCompletedTestsToday(@Param("startOfDay") java.time.LocalDateTime startOfDay);

    @Query("SELECT st.technician.id, st.status, COUNT(st) FROM SampleTest st WHERE st.technician IS NOT NULL AND st.deleted = false GROUP BY st.technician.id, st.status")
    List<Object[]> getTechnicianWorkloadStats();
}

