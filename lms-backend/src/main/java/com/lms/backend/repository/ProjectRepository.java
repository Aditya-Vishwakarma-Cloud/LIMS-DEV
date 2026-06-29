package com.lms.backend.repository;

import com.lms.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByCustomerId(UUID customerId);
    Optional<Project> findFirstByProjectCodeStartingWithOrderByProjectCodeDesc(String prefix);
    Long countByCustomerId(UUID customerId);
}
