package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleHistoryDto;
import com.lms.backend.service.SampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SAMPLE_CREATE')")
    public ResponseEntity<ApiResponse<SampleDto>> createSample(@Valid @RequestBody SampleDto dto) {
        SampleDto created = sampleService.createSample(dto);
        return new ResponseEntity<>(ApiResponse.success(created, "Sample registered successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SAMPLE_EDIT')")
    public ResponseEntity<ApiResponse<SampleDto>> updateSample(@PathVariable UUID id, @Valid @RequestBody SampleDto dto) {
        SampleDto updated = sampleService.updateSample(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Sample updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<SampleDto>> getSampleById(@PathVariable UUID id) {
        SampleDto sample = sampleService.getSampleById(id);
        return ResponseEntity.ok(ApiResponse.success(sample, "Sample retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleDto>>> getAllSamples() {
        List<SampleDto> samples = sampleService.getAllSamples();
        return ResponseEntity.ok(ApiResponse.success(samples, "Samples retrieved successfully"));
    }

    @GetMapping("/work-order/{workOrderId}")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleDto>>> getSamplesByWorkOrderId(@PathVariable UUID workOrderId) {
        List<SampleDto> samples = sampleService.getSamplesByWorkOrderId(workOrderId);
        return ResponseEntity.ok(ApiResponse.success(samples, "Samples for work order retrieved successfully"));
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('SAMPLE_RECEIVE')")
    public ResponseEntity<ApiResponse<SampleDto>> receiveSample(@PathVariable UUID id, @RequestBody SampleDto receiveDetails) {
        SampleDto updated = sampleService.receiveSample(id, receiveDetails);
        return ResponseEntity.ok(ApiResponse.success(updated, "Sample physically received successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SAMPLE_EDIT')")
    public ResponseEntity<ApiResponse<SampleDto>> updateSampleStatus(
            @PathVariable UUID id,
            @RequestParam("status") String status,
            @RequestParam(value = "remarks", required = false) String remarks) {
        SampleDto updated = sampleService.updateSampleStatus(id, status, remarks != null ? remarks : "");
        return ResponseEntity.ok(ApiResponse.success(updated, "Sample status updated successfully"));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    public ResponseEntity<ApiResponse<List<SampleHistoryDto>>> getSampleHistory(@PathVariable UUID id) {
        List<SampleHistoryDto> history = sampleService.getSampleHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history, "Sample history timeline retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SAMPLE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteSample(@PathVariable UUID id) {
        sampleService.deleteSample(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Sample deleted successfully"));
    }
}
