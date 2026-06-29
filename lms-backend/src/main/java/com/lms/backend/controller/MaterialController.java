package com.lms.backend.controller;

import com.lms.backend.common.ApiResponse;
import com.lms.backend.dto.MaterialDto;
import com.lms.backend.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    @PreAuthorize("hasAuthority('MASTER_CREATE')")
    public ResponseEntity<ApiResponse<MaterialDto>> createMaterial(@Valid @RequestBody MaterialDto dto) {
        MaterialDto created = materialService.createMaterial(dto);
        return new ResponseEntity<>(ApiResponse.success(created, "Material created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_EDIT')")
    public ResponseEntity<ApiResponse<MaterialDto>> updateMaterial(@PathVariable UUID id, @Valid @RequestBody MaterialDto dto) {
        MaterialDto updated = materialService.updateMaterial(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Material updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ResponseEntity<ApiResponse<MaterialDto>> getMaterialById(@PathVariable UUID id) {
        MaterialDto material = materialService.getMaterialById(id);
        return ResponseEntity.ok(ApiResponse.success(material, "Material retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<MaterialDto>>> getAllMaterials() {
        List<MaterialDto> materials = materialService.getAllMaterials();
        return ResponseEntity.ok(ApiResponse.success(materials, "Materials retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable UUID id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Material deleted successfully"));
    }
}
