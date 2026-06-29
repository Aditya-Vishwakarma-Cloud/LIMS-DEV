package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.TestResultDto;
import com.lms.backend.service.TestResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-results")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TestResultController {

    private final TestResultService testResultService;

    @GetMapping("/sample-test/{sampleTestId}")
    @PreAuthorize("hasAnyAuthority('RESULT_DRAFT', 'RESULT_SUBMIT', 'RESULT_REVIEW', 'RESULT_APPROVE')")
    public ResponseEntity<ApiResponse<TestResultDto>> getResultBySampleTestId(@PathVariable UUID sampleTestId) {
        TestResultDto dto = testResultService.getResultBySampleTestId(sampleTestId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Result retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RESULT_DRAFT', 'RESULT_SUBMIT', 'RESULT_REVIEW', 'RESULT_APPROVE')")
    public ResponseEntity<ApiResponse<TestResultDto>> getResultById(@PathVariable UUID id) {
        TestResultDto dto = testResultService.getResultById(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Result retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESULT_DRAFT')")
    public ResponseEntity<ApiResponse<TestResultDto>> saveDraftInitial(@RequestBody TestResultDto dto) {
        TestResultDto created = testResultService.saveDraft(dto.getSampleTestId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Draft result saved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RESULT_DRAFT')")
    public ResponseEntity<ApiResponse<TestResultDto>> updateDraft(@PathVariable UUID id, @RequestBody TestResultDto dto) {
        TestResultDto updated = testResultService.saveDraft(dto.getSampleTestId(), dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Draft result updated successfully"));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('RESULT_SUBMIT')")
    public ResponseEntity<ApiResponse<TestResultDto>> submitResult(@PathVariable UUID id) {
        TestResultDto submitted = testResultService.submitResult(id);
        return ResponseEntity.ok(ApiResponse.success(submitted, "Test result submitted successfully"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('RESULT_REJECT')")
    public ResponseEntity<ApiResponse<TestResultDto>> rejectResult(@PathVariable UUID id, @RequestParam(value = "remarks", required = false) String remarks) {
        TestResultDto rejected = testResultService.rejectResult(id, remarks);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Test result rejected for correction"));
    }

    @GetMapping("/pending-review")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    public ResponseEntity<ApiResponse<List<TestResultDto>>> getPendingReviewResults() {
        List<TestResultDto> pending = testResultService.getPendingReviewResults();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending review results retrieved successfully"));
    }

    @GetMapping("/technician/{id}")
    @PreAuthorize("hasAuthority('RESULT_DRAFT')")
    public ResponseEntity<ApiResponse<List<TestResultDto>>> getTechnicianResults(@PathVariable UUID id) {
        List<TestResultDto> results = testResultService.getTechnicianResults(id);
        return ResponseEntity.ok(ApiResponse.success(results, "Technician results retrieved successfully"));
    }

    @GetMapping("/technician/{id}/metrics")
    @PreAuthorize("hasAuthority('RESULT_DRAFT')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTechnicianMetrics(@PathVariable UUID id) {
        Map<String, Long> metrics = testResultService.getTechnicianMetrics(id);
        return ResponseEntity.ok(ApiResponse.success(metrics, "Technician metrics dashboard counters retrieved successfully"));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    public ResponseEntity<ApiResponse<TestResultDto>> verifyResult(@PathVariable UUID id, @RequestParam(value = "remarks", required = false) String remarks) {
        TestResultDto verified = testResultService.verifyResult(id, remarks);
        return ResponseEntity.ok(ApiResponse.success(verified, "Test result verified successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RESULT_APPROVE')")
    public ResponseEntity<ApiResponse<TestResultDto>> approveResult(@PathVariable UUID id, @RequestParam(value = "remarks", required = false) String remarks) {
        TestResultDto approved = testResultService.approveResult(id, remarks);
        return ResponseEntity.ok(ApiResponse.success(approved, "Test result approved successfully"));
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAuthority('RESULT_APPROVE')")
    public ResponseEntity<ApiResponse<List<TestResultDto>>> getPendingApprovalResults() {
        List<TestResultDto> pending = testResultService.getPendingApprovalResults();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending approval results retrieved successfully"));
    }
}
