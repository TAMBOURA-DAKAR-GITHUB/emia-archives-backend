package com.emia.archives.controller;

import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.dto.request.CreateStagiaireRequest;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.StagiaireRepository;
import com.emia.archives.service.ExcelImportService;
import com.emia.archives.service.ExportService;
import com.emia.archives.service.FileStorageService;
import com.emia.archives.service.StagiaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StagiaireController {

    private final StagiaireService stagiaireService;
    private final ExcelImportService excelImportService;
    private final FileStorageService fileStorageService;
    private final ExportService exportService;
    private final StagiaireRepository stagiaireRepository;
    private final EntityMapper mapper;

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

    @GetMapping("/schools/{schoolId}/stagiaires/cycles")
    public ResponseEntity<ApiResponse<List<StagiaireDto>>> getStagiairesWithoutPromotion(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(stagiaireService.getStagiairesWithoutPromotion(schoolId)));
    }

    @GetMapping("/schools/{schoolId}/stagiaires/search")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<StagiaireDto>>> searchStagiaires(
            @PathVariable String schoolId, @RequestParam String q) {
        List<Stagiaire> results = stagiaireRepository.searchBySchoolId(schoolId, q);
        return ResponseEntity.ok(ApiResponse.ok(results.stream().map(mapper::toDto).toList()));
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

    @GetMapping("/schools/{schoolId}/promotions/{rang}/stagiaires/export/excel")
    public ResponseEntity<byte[]> exportStagiairesExcel(
            @PathVariable String schoolId, @PathVariable String rang) throws Exception {
        byte[] data = exportService.exportStagiairesExcel(schoolId, rang);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stagiaires-" + rang + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/stagiaires/{id}/photo")
    public ResponseEntity<ApiResponse<StagiaireDto>> uploadPhoto(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        StagiaireDto dto = stagiaireService.uploadPhoto(id, file);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Photo téléchargée avec succès"));
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFile(filename);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = determineContentType(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }
}
