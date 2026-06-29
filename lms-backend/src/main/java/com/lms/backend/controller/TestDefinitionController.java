package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.TestDefinitionDto;
import com.lms.backend.service.TestDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-definitions")
@RequiredArgsConstructor
public class TestDefinitionController {

    private final TestDefinitionService testDefinitionService;

    @PostMapping
    @PreAuthorize("hasAuthority('MASTER_CREATE')")
    public ResponseEntity<ApiResponse<TestDefinitionDto>> createTestDefinition(@Valid @RequestBody TestDefinitionDto dto) {
        TestDefinitionDto created = testDefinitionService.createTestDefinition(dto);
        return new ResponseEntity<>(ApiResponse.success(created, "Test definition created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_EDIT')")
    public ResponseEntity<ApiResponse<TestDefinitionDto>> updateTestDefinition(@PathVariable UUID id, @Valid @RequestBody TestDefinitionDto dto) {
        TestDefinitionDto updated = testDefinitionService.updateTestDefinition(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Test definition updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ResponseEntity<ApiResponse<TestDefinitionDto>> getTestDefinitionById(@PathVariable UUID id) {
        TestDefinitionDto test = testDefinitionService.getTestDefinitionById(id);
        return ResponseEntity.ok(ApiResponse.success(test, "Test definition retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<TestDefinitionDto>>> getAllTestDefinitions() {
        List<TestDefinitionDto> tests = testDefinitionService.getAllTestDefinitions();
        return ResponseEntity.ok(ApiResponse.success(tests, "Test definitions retrieved successfully"));
    }

    @GetMapping("/material/{materialId}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<TestDefinitionDto>>> getTestDefinitionsByMaterialId(@PathVariable UUID materialId) {
        List<TestDefinitionDto> tests = testDefinitionService.getTestDefinitionsByMaterialId(materialId);
        return ResponseEntity.ok(ApiResponse.success(tests, "Test definitions for material retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteTestDefinition(@PathVariable UUID id) {
        testDefinitionService.deleteTestDefinition(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Test definition deleted successfully"));
    }
}
