package com.lms.backend.service;

import com.lms.backend.dto.MaterialDto;
import java.util.List;
import java.util.UUID;

public interface MaterialService {
    MaterialDto createMaterial(MaterialDto dto);
    MaterialDto updateMaterial(UUID id, MaterialDto dto);
    MaterialDto getMaterialById(UUID id);
    List<MaterialDto> getAllMaterials();
    void deleteMaterial(UUID id);
}
