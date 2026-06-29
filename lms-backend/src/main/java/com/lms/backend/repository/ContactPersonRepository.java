package com.lms.backend.repository;

import com.lms.backend.entity.ContactPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactPersonRepository extends JpaRepository<ContactPerson, UUID> {
    List<ContactPerson> findByCustomerId(UUID customerId);
    java.util.Optional<ContactPerson> findByEmail(String email);
}

