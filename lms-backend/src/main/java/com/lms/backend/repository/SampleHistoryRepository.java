package com.lms.backend.repository;

import com.lms.backend.entity.SampleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SampleHistoryRepository extends JpaRepository<SampleHistory, UUID> {
    List<SampleHistory> findBySampleIdOrderByChangedAtDesc(UUID sampleId);
}
