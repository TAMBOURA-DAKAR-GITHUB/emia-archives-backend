package com.emia.archives.controller;

import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.dto.request.CreateStagiaireRequest;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.service.ExcelImportService;
import com.emia.archives.service.StagiaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StagiaireController {

    private final StagiaireService stagiaireService;
    private final ExcelImportService excelImportService;

    @GetMapping("/stagiaires")
    public ResponseEntity<ApiResponse<List<StagiaireDto>>> getAllStagiaires() {
        return ResponseEntity.ok(ApiResponse.ok(stagiaireService.getAllStagiaires()));
    }

    @GetMapping("/stagiaires/{id}")
    public ResponseEntity<ApiResponse<StagiaireDto>> getStagiaireById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(stagiaireService.getStagiaireById(id)));
    }

    @GetMapping("/schools/{schoolId}/promotions/{rang}/stagiaires")
    public ResponseEntity<ApiResponse<List<StagiaireDto>>> getStagiairesByPromotion(
            @PathVariable String schoolId, @PathVariable String rang) {
        return ResponseEntity.ok(ApiResponse.ok(stagiaireService.getStagiairesByPromotion(schoolId, rang)));
    }

    @PostMapping("/stagiaires")
    public ResponseEntity<ApiResponse<StagiaireDto>> createStagiaire(
            @Valid @RequestBody CreateStagiaireRequest request) {
        StagiaireDto dto = stagiaireService.createStagiaire(request.getSchoolId(), request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Stagiaire créé avec succès"));
    }

    @PutMapping("/stagiaires/{id}")
    public ResponseEntity<ApiResponse<StagiaireDto>> updateStagiaire(
            @PathVariable Long id, @Valid @RequestBody CreateStagiaireRequest request) {
        StagiaireDto dto = stagiaireService.updateStagiaire(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Stagiaire mis à jour avec succès"));
    }

    @DeleteMapping("/stagiaires/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStagiaire(@PathVariable Long id) {
        stagiaireService.deleteStagiaire(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Stagiaire supprimé avec succès"));
    }

    @PostMapping("/schools/{schoolId}/stagiaires/import")
    public ResponseEntity<ApiResponse<List<StagiaireDto>>> importStagiaires(
            @PathVariable String schoolId, @RequestParam("file") MultipartFile file) throws Exception {
        List<StagiaireDto> result = excelImportService.importStagiaires(schoolId, file);
        return ResponseEntity.ok(ApiResponse.ok(result, result.size() + " stagiaires importés"));
    }
}
