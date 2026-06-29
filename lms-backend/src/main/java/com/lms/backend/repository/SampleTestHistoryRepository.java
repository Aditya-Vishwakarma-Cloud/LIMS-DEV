package com.lms.backend.repository;

import com.lms.backend.entity.SampleTestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SampleTestHistoryRepository extends JpaRepository<SampleTestHistory, UUID> {
    List<SampleTestHistory> findBySampleTestIdOrderByChangedAtDesc(UUID sampleTestId);
}
