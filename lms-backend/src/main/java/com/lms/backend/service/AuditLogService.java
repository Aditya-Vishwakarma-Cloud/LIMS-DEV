package com.lms.backend.service;

import com.lms.backend.entity.AuditLog;
import com.lms.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String username, String ipAddress, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .username(username)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
