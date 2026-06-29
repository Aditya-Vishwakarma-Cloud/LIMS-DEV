package com.lms.backend.service;

import com.lms.backend.dto.UserCreateRequest;
import com.lms.backend.dto.UserUpdateRequest;
import com.lms.backend.dto.UserResponse;
import com.lms.backend.dto.PasswordUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserResponse> getAllUsers();
    List<UserResponse> searchUsers(String query);
    UserResponse getUserById(UUID id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(UUID id, UserUpdateRequest request);
    void deleteUser(UUID id);
    void updatePassword(UUID id, PasswordUpdateRequest request);
    List<UserResponse> getActiveTechnicians();
}
