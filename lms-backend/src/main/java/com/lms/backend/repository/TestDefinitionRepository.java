package com.lms.backend.repository;

import com.lms.backend.entity.TestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestDefinitionRepository extends JpaRepository<TestDefinition, UUID> {
    List<TestDefinition> findByMaterialId(UUID materialId);
}
