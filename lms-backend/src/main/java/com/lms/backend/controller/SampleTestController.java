package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleTestDto;
import com.lms.backend.dto.SampleTestAssignmentDto;
import com.lms.backend.dto.SampleTestHistoryDto;
import com.lms.backend.service.SampleTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SampleTestController {

    private final SampleTestService sampleTestService;

    @GetMapping("/sample-tests/pending")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleDto>>> getPendingSamples() {
        List<SampleDto> samples = sampleTestService.getPendingSamples();
        return ResponseEntity.ok(ApiResponse.success(samples, "Pending samples retrieved successfully"));
    }

    @GetMapping("/samples/{sampleId}/tests")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleTestDto>>> getSampleTestsBySampleId(@PathVariable UUID sampleId) {
        List<SampleTestDto> tests = sampleTestService.getSampleTestsBySampleId(sampleId);
        return ResponseEntity.ok(ApiResponse.success(tests, "Sample test assignments retrieved successfully"));
    }

    @PostMapping("/samples/{sampleId}/assign-tests")
    @PreAuthorize("hasAuthority('TEST_ASSIGN')")
    public ResponseEntity<ApiResponse<List<SampleTestDto>>> assignTests(
            @PathVariable UUID sampleId,
            @Valid @RequestBody SampleTestAssignmentDto dto
    ) {
        List<SampleTestDto> assigned = sampleTestService.assignTests(sampleId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assigned, "Tests assigned successfully to sample"));
    }

    @PutMapping("/sample-tests/{id}")
    @PreAuthorize("hasAuthority('TEST_ASSIGN')")
    public ResponseEntity<ApiResponse<SampleTestDto>> updateSampleTest(
            @PathVariable UUID id,
            @Valid @RequestBody SampleTestDto dto
    ) {
        SampleTestDto updated = sampleTestService.updateSampleTest(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Sample test assignment updated successfully"));
    }

    @GetMapping("/sample-tests/{id}")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<SampleTestDto>> getSampleTestById(@PathVariable UUID id) {
        SampleTestDto st = sampleTestService.getSampleTestById(id);
        return ResponseEntity.ok(ApiResponse.success(st, "Sample test assignment retrieved successfully"));
    }

    @GetMapping("/sample-tests/technician/{technicianId}")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleTestDto>>> getSampleTestsByTechnicianId(@PathVariable UUID technicianId) {
        List<SampleTestDto> tests = sampleTestService.getSampleTestsByTechnicianId(technicianId);
        return ResponseEntity.ok(ApiResponse.success(tests, "Technician assignments retrieved successfully"));
    }

    @GetMapping("/sample-tests/{id}/history")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleTestHistoryDto>>> getSampleTestHistory(@PathVariable UUID id) {
        List<SampleTestHistoryDto> history = sampleTestService.getSampleTestHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history, "Sample test history timeline retrieved successfully"));
    }
}
