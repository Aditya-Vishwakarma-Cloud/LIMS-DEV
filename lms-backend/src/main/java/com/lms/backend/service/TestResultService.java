package com.lms.backend.service;

import com.lms.backend.dto.TestResultDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TestResultService {
    TestResultDto saveDraft(UUID sampleTestId, TestResultDto dto);
    TestResultDto submitResult(UUID id);
    TestResultDto getResultBySampleTestId(UUID sampleTestId);
    TestResultDto getResultById(UUID id);
    List<TestResultDto> getPendingReviewResults();
    List<TestResultDto> getTechnicianResults(UUID technicianId);
    TestResultDto rejectResult(UUID id, String remarks);
    Map<String, Long> getTechnicianMetrics(UUID technicianId);
    TestResultDto verifyResult(UUID id, String remarks);
    TestResultDto approveResult(UUID id, String remarks);
    List<TestResultDto> getPendingApprovalResults();
}
