package com.lms.backend.service.impl;

import com.lms.backend.dto.ProjectDto;
import com.lms.backend.entity.Customer;
import com.lms.backend.entity.Project;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.CustomerRepository;
import com.lms.backend.repository.ProjectRepository;
import com.lms.backend.service.ProjectService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ProjectDto createProject(ProjectDto projectDto) {
        Customer customer = customerRepository.findById(projectDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + projectDto.getCustomerId()));

        String projectCode = generateProjectCode();

        Project project = Project.builder()
                .projectCode(projectCode)
                .projectNumber(projectDto.getProjectNumber())
                .projectName(projectDto.getProjectName())
                .siteName(projectDto.getSiteName())
                .engineer(projectDto.getEngineer())
                .consultant(projectDto.getConsultant())
                .contractor(projectDto.getContractor())
                .location(projectDto.getLocation())
                .expectedCompletion(projectDto.getExpectedCompletion())
                .status(projectDto.getStatus() != null ? projectDto.getStatus() : "Active")
                .customer(customer)
                .deleted(false)
                .build();

        Project saved = projectRepository.save(project);
        logAudit("CREATE_PROJECT", "Created project: " + saved.getProjectCode() + " (" + saved.getProjectName() + ")");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(UUID id, ProjectDto projectDto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        Customer customer = customerRepository.findById(projectDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + projectDto.getCustomerId()));

        project.setProjectNumber(projectDto.getProjectNumber());
        project.setProjectName(projectDto.getProjectName());
        project.setSiteName(projectDto.getSiteName());
        project.setEngineer(projectDto.getEngineer());
        project.setConsultant(projectDto.getConsultant());
        project.setContractor(projectDto.getContractor());
        project.setLocation(projectDto.getLocation());
        project.setExpectedCompletion(projectDto.getExpectedCompletion());
        project.setStatus(projectDto.getStatus());
        project.setCustomer(customer);

        Project saved = projectRepository.save(project);
        logAudit("UPDATE_PROJECT", "Updated project: " + saved.getProjectCode());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapToDto(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByCustomerId(UUID customerId) {
        return projectRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        
        String username = getCurrentUsername();
        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        project.setDeletedBy(username);
        
        projectRepository.save(project);
        logAudit("DELETE_PROJECT", "Soft deleted project: " + project.getProjectCode());
    }

    private synchronized String generateProjectCode() {
        Optional<Project> lastProject = projectRepository.findFirstByProjectCodeStartingWithOrderByProjectCodeDesc("PROJ-");
        if (lastProject.isPresent()) {
            String code = lastProject.get().getProjectCode();
            try {
                int num = Integer.parseInt(code.substring(5));
                return String.format("PROJ-%04d", num + 1);
            } catch (Exception e) {
                // Fallback to parsing issues
            }
        }
        return "PROJ-0001";
    }

    private void logAudit(String action, String details) {
        auditLogService.log(action, getCurrentUsername(), "localhost", details);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
    }

    private ProjectDto mapToDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .projectNumber(p.getProjectNumber())
                .projectName(p.getProjectName())
                .siteName(p.getSiteName())
                .engineer(p.getEngineer())
                .consultant(p.getConsultant())
                .contractor(p.getContractor())
                .location(p.getLocation())
                .expectedCompletion(p.getExpectedCompletion())
                .status(p.getStatus())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getCustomerName())
                .build();
    }
}
