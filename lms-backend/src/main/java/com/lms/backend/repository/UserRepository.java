package com.lms.backend.repository;

import com.lms.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND u.deleted = false")
    List<User> searchUsers(@Param("query") String query);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.status = :status AND u.deleted = false")
    List<User> findByRoleAndStatusAndNotDeleted(
            @Param("roleName") String roleName,
            @Param("status") com.lms.backend.entity.AccountStatus status
    );
}
