package com.lms.backend.service;

import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleTestDto;
import com.lms.backend.dto.SampleTestAssignmentDto;
import com.lms.backend.dto.SampleTestHistoryDto;

import java.util.List;
import java.util.UUID;

public interface SampleTestService {
    List<SampleDto> getPendingSamples();
    List<SampleTestDto> getSampleTestsBySampleId(UUID sampleId);
    List<SampleTestDto> assignTests(UUID sampleId, SampleTestAssignmentDto dto);
    SampleTestDto updateSampleTest(UUID id, SampleTestDto dto);
    List<SampleTestDto> getSampleTestsByTechnicianId(UUID technicianId);
    SampleTestDto getSampleTestById(UUID id);
    List<SampleTestHistoryDto> getSampleTestHistory(UUID sampleTestId);
}
