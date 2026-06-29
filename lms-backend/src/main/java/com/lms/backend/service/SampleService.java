package com.lms.backend.service;

import com.lms.backend.dto.SampleDto;
import com.lms.backend.dto.SampleHistoryDto;
import java.util.List;
import java.util.UUID;

public interface SampleService {
    SampleDto createSample(SampleDto sampleDto);
    SampleDto updateSample(UUID id, SampleDto sampleDto);
    SampleDto getSampleById(UUID id);
    List<SampleDto> getAllSamples();
    List<SampleDto> getSamplesByWorkOrderId(UUID workOrderId);
    SampleDto receiveSample(UUID id, SampleDto receiveDetails);
    SampleDto updateSampleStatus(UUID id, String status, String remarks);
    List<SampleHistoryDto> getSampleHistory(UUID sampleId);
    void deleteSample(UUID id);
}
