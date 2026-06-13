package com.lms.backend.config;

import com.lms.backend.entity.AccountStatus;
import com.lms.backend.entity.Role;
import com.lms.backend.entity.RoleName;
import com.lms.backend.entity.User;
import com.lms.backend.repository.RoleRepository;
import com.lms.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing LIMS roles and default admin user...");
        
        // Seed Roles
        for (RoleName name : RoleName.values()) {
            Optional<Role> existingRole = roleRepository.findByName(name);
            if (existingRole.isEmpty()) {
                Role role = Role.builder()
                        .name(name)
                        .description("System role for " + name.name().substring(5))
                        .build();
                roleRepository.save(role);
                log.info("Seeded role: {}", name);
            }
        }

        // Seed default SUPER_ADMIN user
        String adminEmail = "admin@wemurz.com";
        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);
        if (existingAdmin.isEmpty()) {
            Set<Role> adminRoles = new HashSet<>();
            roleRepository.findByName(RoleName.ROLE_SUPER_ADMIN).ifPresent(adminRoles::add);
            roleRepository.findByName(RoleName.ROLE_ADMIN).ifPresent(adminRoles::add);

            User admin = User.builder()
                    .name("LIMS Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@WeMurz25"))
                    .status(AccountStatus.ACTIVE)
                    .deleted(false)
                    .roles(adminRoles)
                    .build();

            userRepository.save(admin);
            log.info("Seeded default super admin user with email: {}", adminEmail);
        }
    }
}
