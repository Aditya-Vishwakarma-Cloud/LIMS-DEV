package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.ProjectDto;
import com.lms.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(@Valid @RequestBody ProjectDto dto) {
        ProjectDto created = projectService.createProject(dto);
        return new ResponseEntity<>(ApiResponse.success(created, "Project created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_EDIT')")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectDto dto) {
        ProjectDto updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Project updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<ApiResponse<ProjectDto>> getProjectById(@PathVariable UUID id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project, "Project retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAllProjects() {
        List<ProjectDto> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success(projects, "Projects retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjectsByCustomerId(@PathVariable UUID customerId) {
        List<ProjectDto> projects = projectService.getProjectsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(projects, "Projects for customer retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Project deleted successfully"));
    }
}
