package com.emia.archives.controller;

import com.emia.archives.dto.PromotionDto;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.dto.response.DashboardStats;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.StagiaireRepository;
import com.emia.archives.service.ExcelImportService;
import com.emia.archives.service.FileStorageService;
import com.emia.archives.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;
    private final ExcelImportService excelImportService;
    private final FileStorageService fileStorageService;
    private final StagiaireRepository stagiaireRepository;
    private final EntityMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getPromotions(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getPromotionsBySchool(schoolId)));
    }

    @GetMapping("/{rang}")
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotion(@PathVariable String schoolId,
                                                                   @PathVariable String rang) {
        Promotion promotion = promotionService.getPromotionEntityBySchoolAndRangNumber(schoolId, rang);
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(promotion)));
    }

    @GetMapping("/{rang}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DashboardStats>> getStats(@PathVariable String schoolId,
                                                                @PathVariable String rang) {
        try {
            Promotion promotion = promotionService.getPromotionEntityBySchoolAndRangNumber(schoolId, rang);
            int total = (int) stagiaireRepository.countByPromotionId(promotion.getId());

            List<Stagiaire> top = stagiaireRepository.findTop5ByPromotionId(promotion.getId());
            String majorName = !top.isEmpty() ? top.get(0).getNomComplet() : promotion.getMajorName();
            Double bestAverage = !top.isEmpty() && top.get(0).getMoyenne() != null
                    ? top.get(0).getMoyenne().doubleValue()
                    : (promotion.getMajorScore() != null ? promotion.getMajorScore() : 0.0);

            DashboardStats stats = DashboardStats.builder()
                    .totalStagiaires(total)
                    .majorName(majorName)
                    .bestAverage(bestAverage)
                    .promotionRang(promotion.getRang())
                    .build();
            return ResponseEntity.ok(ApiResponse.ok(stats));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error computing stats for school={} rang={}: {}", schoolId, rang, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> importPromotions(
            @PathVariable String schoolId, @RequestParam("file") MultipartFile file) throws Exception {
        List<PromotionDto> result = excelImportService.importPromotions(schoolId, file);
        return ResponseEntity.ok(ApiResponse.ok(result, result.size() + " promotion(s) importée(s)"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(@PathVariable String schoolId,
                                                                       @RequestBody PromotionDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(
                promotionService.createPromotion(schoolId, dto), "Promotion créée avec succès"));
    }

    @PutMapping("/{rang}")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(@PathVariable String schoolId,
                                                                      @PathVariable String rang,
                                                                      @RequestBody PromotionDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(
                promotionService.updatePromotion(schoolId, rang, dto), "Promotion modifiée avec succès"));
    }

    @DeleteMapping("/{rang}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable String schoolId,
                                                              @PathVariable String rang) {
        promotionService.deletePromotion(schoolId, rang);
        return ResponseEntity.ok(ApiResponse.ok(null, "Promotion supprimée avec succès"));
    }

    @PostMapping("/{rang}/photo/{role}")
    public ResponseEntity<ApiResponse<PromotionDto>> uploadLeaderPhoto(
            @PathVariable String schoolId,
            @PathVariable String rang,
            @PathVariable String role,
            @RequestParam("file") MultipartFile file) {
        Promotion promotion = promotionService.getPromotionEntityBySchoolAndRangNumber(schoolId, rang);

        String prefix = "promo-" + rang.replaceAll("\\s+", "-") + "-" + role;
        String filename = fileStorageService.saveFileGeneric(prefix, file);

        String oldPhoto = "emir".equals(role) ? promotion.getEmirPhotoUrl() : promotion.getChefTondjonPhotoUrl();

        if ("emir".equals(role)) {
            promotion.setEmirPhotoUrl(filename);
        } else if ("chef".equals(role)) {
            promotion.setChefTondjonPhotoUrl(filename);
        }

        promotion = promotionService.save(promotion);

        if (oldPhoto != null) {
            fileStorageService.deleteFile(oldPhoto);
        }

        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(promotion), "Photo téléchargée avec succès"));
    }
}
