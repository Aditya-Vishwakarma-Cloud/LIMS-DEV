package com.lms.backend.repository;

import com.lms.backend.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findByCustomerId(UUID customerId);
    List<WorkOrder> findByProjectId(UUID projectId);
    Optional<WorkOrder> findFirstByWorkOrderNumberStartingWithOrderByWorkOrderNumberDesc(String prefix);
    Long countByCustomerId(UUID customerId);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.createdAt >= :startOfDay AND wo.deleted = false")
    long countByCreatedAtAfter(@org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay);
    long countByStatusAndDeletedFalse(com.lms.backend.entity.WorkOrderStatus status);
    long countByCustomerIdAndStatusAndDeletedFalse(UUID customerId, com.lms.backend.entity.WorkOrderStatus status);
}

