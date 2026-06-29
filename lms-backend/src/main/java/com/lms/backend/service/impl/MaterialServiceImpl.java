package com.lms.backend.service.impl;

import com.lms.backend.dto.MaterialDto;
import com.lms.backend.entity.Material;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.MaterialRepository;
import com.lms.backend.service.MaterialService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MaterialDto createMaterial(MaterialDto dto) {
        if (materialRepository.findByMaterialCode(dto.getMaterialCode()).isPresent()) {
            throw new IllegalArgumentException("Material code already exists: " + dto.getMaterialCode());
        }
        Material material = Material.builder()
                .materialCode(dto.getMaterialCode().toUpperCase())
                .materialName(dto.getMaterialName())
                .samplePrefix(dto.getSamplePrefix().toUpperCase())
                .description(dto.getDescription())
                .active(dto.isActive())
                .build();
        Material saved = materialRepository.save(material);
        logAudit("CREATE_MATERIAL", "Created material " + saved.getMaterialCode());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public MaterialDto updateMaterial(UUID id, MaterialDto dto) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));

        material.setMaterialName(dto.getMaterialName());
        material.setSamplePrefix(dto.getSamplePrefix().toUpperCase());
        material.setDescription(dto.getDescription());
        material.setActive(dto.isActive());

        Material saved = materialRepository.save(material);
        logAudit("UPDATE_MATERIAL", "Updated material " + saved.getMaterialCode());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDto getMaterialById(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));
        return mapToDto(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getAllMaterials() {
        return materialRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMaterial(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));
        materialRepository.delete(material);
        logAudit("DELETE_MATERIAL", "Deleted material " + material.getMaterialCode());
    }

    private void logAudit(String action, String details) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(action, username, "localhost", details);
    }

    private MaterialDto mapToDto(Material material) {
        return MaterialDto.builder()
                .id(material.getId())
                .materialCode(material.getMaterialCode())
                .materialName(material.getMaterialName())
                .samplePrefix(material.getSamplePrefix())
                .description(material.getDescription())
                .active(material.isActive())
                .build();
    }
}
