package com.lms.backend.service.impl;

import com.lms.backend.dto.TestDefinitionDto;
import com.lms.backend.entity.Material;
import com.lms.backend.entity.TestDefinition;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.MaterialRepository;
import com.lms.backend.repository.TestDefinitionRepository;
import com.lms.backend.service.TestDefinitionService;
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
public class TestDefinitionServiceImpl implements TestDefinitionService {

    private final TestDefinitionRepository testDefinitionRepository;
    private final MaterialRepository materialRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public TestDefinitionDto createTestDefinition(TestDefinitionDto dto) {
        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + dto.getMaterialId()));

        TestDefinition test = TestDefinition.builder()
                .material(material)
                .testName(dto.getTestName())
                .testCode(dto.getTestCode())
                .unit(dto.getUnit())
                .specification(dto.getSpecification())
                .specOperator(dto.getSpecOperator())
                .specValue(dto.getSpecValue())
                .valueType(dto.getValueType())
                .method(dto.getMethod())
                .isMandatory(dto.isMandatory())
                .active(dto.isActive())
                .build();
        TestDefinition saved = testDefinitionRepository.save(test);
        logAudit("CREATE_TEST_DEFINITION", "Created test definition " + saved.getTestName() + " for " + material.getMaterialCode());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TestDefinitionDto updateTestDefinition(UUID id, TestDefinitionDto dto) {
        TestDefinition test = testDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test definition not found with id: " + id));

        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + dto.getMaterialId()));

        test.setMaterial(material);
        test.setTestName(dto.getTestName());
        test.setTestCode(dto.getTestCode());
        test.setUnit(dto.getUnit());
        test.setSpecification(dto.getSpecification());
        test.setSpecOperator(dto.getSpecOperator());
        test.setSpecValue(dto.getSpecValue());
        test.setValueType(dto.getValueType());
        test.setMethod(dto.getMethod());
        test.setMandatory(dto.isMandatory());
        test.setActive(dto.isActive());

        TestDefinition saved = testDefinitionRepository.save(test);
        logAudit("UPDATE_TEST_DEFINITION", "Updated test definition " + saved.getTestName());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TestDefinitionDto getTestDefinitionById(UUID id) {
        TestDefinition test = testDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test definition not found with id: " + id));
        return mapToDto(test);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDefinitionDto> getAllTestDefinitions() {
        return testDefinitionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDefinitionDto> getTestDefinitionsByMaterialId(UUID materialId) {
        return testDefinitionRepository.findByMaterialId(materialId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTestDefinition(UUID id) {
        TestDefinition test = testDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test definition not found with id: " + id));
        testDefinitionRepository.delete(test);
        logAudit("DELETE_TEST_DEFINITION", "Deleted test definition " + test.getTestName());
    }

    private void logAudit(String action, String details) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(action, username, "localhost", details);
    }

    private TestDefinitionDto mapToDto(TestDefinition test) {
        return TestDefinitionDto.builder()
                .id(test.getId())
                .materialId(test.getMaterial().getId())
                .materialName(test.getMaterial().getMaterialName())
                .materialCode(test.getMaterial().getMaterialCode())
                .testName(test.getTestName())
                .testCode(test.getTestCode())
                .unit(test.getUnit())
                .specification(test.getSpecification())
                .specOperator(test.getSpecOperator())
                .specValue(test.getSpecValue())
                .valueType(test.getValueType())
                .method(test.getMethod())
                .isMandatory(test.isMandatory())
                .active(test.isActive())
                .build();
    }
}
