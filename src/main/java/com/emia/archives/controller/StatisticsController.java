package com.emia.archives.controller;

import com.emia.archives.dto.response.*;
import com.emia.archives.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<List<KpiData>>> getKpis(
            @PathVariable String schoolId, @RequestParam(required = false) String rang) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getKpis(schoolId, rang)));
    }

    @GetMapping("/effectifs")
    public ResponseEntity<ApiResponse<List<ChartDataPoint>>> getEffectifs(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getEffectifsEvolution(schoolId)));
    }

    @GetMapping("/moyennes")
    public ResponseEntity<ApiResponse<List<ChartDataPoint>>> getMoyennes(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getMoyennesEvolution(schoolId)));
    }

    @GetMapping("/grade-distribution")
    public ResponseEntity<ApiResponse<List<DonutSegment>>> getGradeDistribution(
            @PathVariable String schoolId, @RequestParam(required = false) String rang) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getGradeDistribution(schoolId, rang)));
    }

    @GetMapping("/top5")
    public ResponseEntity<ApiResponse<List<PodiumEntry>>> getTop5(
            @PathVariable String schoolId, @RequestParam(required = false) String rang) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.getTop5(schoolId, rang)));
    }
}
