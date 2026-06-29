package com.lms.backend.service.impl;

import com.lms.backend.dto.UserCreateRequest;
import com.lms.backend.dto.UserUpdateRequest;
import com.lms.backend.dto.UserResponse;
import com.lms.backend.dto.PasswordUpdateRequest;
import com.lms.backend.entity.AccountStatus;
import com.lms.backend.entity.Role;
import com.lms.backend.entity.User;
import com.lms.backend.exception.EmailAlreadyExistsException;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.RoleRepository;
import com.lms.backend.repository.UserRepository;
import com.lms.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return getAllUsers();
        }
        return userRepository.searchUsers(query).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        Role role = null;
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleNameStr = request.getRoles().iterator().next();
            role = roleRepository.findByName(roleNameStr).orElse(null);
        }
        if (role == null) {
            role = roleRepository.findByName("ROLE_CLIENT_VIEWER").orElse(null);
        }

        AccountStatus status = AccountStatus.ACTIVE;
        try {
            status = AccountStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid account status requested: {}", request.getStatus());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(status)
                .deleted(false)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user: {}", savedUser.getEmail());
        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setName(request.getName());

        try {
            user.setStatus(AccountStatus.valueOf(request.getStatus()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid account status requested: {}", request.getStatus());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleNameStr = request.getRoles().iterator().next();
            roleRepository.findByName(roleNameStr).ifPresent(user::setRole);
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {}", updatedUser.getEmail());
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Soft delete
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Soft-deleted user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void updatePassword(UUID id, PasswordUpdateRequest request) {
        User user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        log.info("Updated password for user: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveTechnicians() {
        return userRepository.findByRoleAndStatusAndNotDeleted(
                "ROLE_TECHNICIAN",
                AccountStatus.ACTIVE
        ).stream()
        .map(this::mapToUserResponse)
        .collect(Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(user.getRole() != null ? java.util.Set.of(user.getRole().getName()) : java.util.Collections.emptySet())
                .build();
    }
}
