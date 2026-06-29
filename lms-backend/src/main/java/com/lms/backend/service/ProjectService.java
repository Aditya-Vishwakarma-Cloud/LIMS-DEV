package com.lms.backend.service;

import com.lms.backend.dto.ProjectDto;
import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ProjectDto createProject(ProjectDto projectDto);
    ProjectDto updateProject(UUID id, ProjectDto projectDto);
    ProjectDto getProjectById(UUID id);
    List<ProjectDto> getAllProjects();
    List<ProjectDto> getProjectsByCustomerId(UUID customerId);
    void deleteProject(UUID id);
}
