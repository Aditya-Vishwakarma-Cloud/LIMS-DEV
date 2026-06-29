package com.lms.backend.service;

import com.lms.backend.dto.TestDefinitionDto;
import java.util.List;
import java.util.UUID;

public interface TestDefinitionService {
    TestDefinitionDto createTestDefinition(TestDefinitionDto dto);
    TestDefinitionDto updateTestDefinition(UUID id, TestDefinitionDto dto);
    TestDefinitionDto getTestDefinitionById(UUID id);
    List<TestDefinitionDto> getAllTestDefinitions();
    List<TestDefinitionDto> getTestDefinitionsByMaterialId(UUID materialId);
    void deleteTestDefinition(UUID id);
}
