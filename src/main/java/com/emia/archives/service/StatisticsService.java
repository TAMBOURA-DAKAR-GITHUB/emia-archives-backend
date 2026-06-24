package com.emia.archives.service;

import com.emia.archives.dto.response.*;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.PromotionRepository;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StagiaireRepository stagiaireRepository;
    private final PromotionRepository promotionRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<KpiData> getKpis(String schoolId, String rang) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        long totalPromotions = promotions.size();

        long totalStagiaires;
        double avgMoyenne;
        double bestScore;
        String trendText;

        if (rang != null && !rang.isBlank()) {
            Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
            if (promotion != null) {
                totalStagiaires = stagiaireRepository.countByPromotionId(promotion.getId());
                avgMoyenne = stagiaireRepository.averageMoyenneByPromotionId(promotion.getId()).doubleValue();
                bestScore = stagiaireRepository.maxMoyenneByPromotionId(promotion.getId()).doubleValue();
                trendText = "↑ " + (promotion.getNom() != null ? promotion.getNom() : "");
            } else {
                totalStagiaires = 0;
                avgMoyenne = 0;
                bestScore = 0;
                trendText = "";
            }
        } else {
            totalStagiaires = stagiaireRepository.countBySchoolId(schoolId);
            avgMoyenne = stagiaireRepository.averageMoyenneBySchoolId(schoolId).doubleValue();
            bestScore = stagiaireRepository.maxMoyenneBySchoolId(schoolId).doubleValue();
            trendText = "↑ " + (!promotions.isEmpty() && promotions.get(0).getNom() != null ? promotions.get(0).getNom() : "");
        }

        List<KpiData> kpis = new ArrayList<>();
        kpis.add(KpiData.builder().icon("🎓").label("Total EOA")
                .value(String.format("%,d", totalStagiaires)).trend(trendText).build());
        kpis.add(KpiData.builder().icon("📅").label("Promotions enregistrées")
                .value(String.valueOf(totalPromotions)).trend("↑ depuis 2018").build());
        kpis.add(KpiData.builder().icon("📈").label("Moyenne générale")
                .value(String.format("%.2f", avgMoyenne)).trend("↑ amélioration continue").build());
        kpis.add(KpiData.builder().icon("🏆").label("Meilleur score")
                .value(String.format("%.2f", bestScore)).trend(trendText).build());
        return kpis;
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getEffectifsEvolution(String schoolId) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        java.util.Collections.reverse(promotions);
        return promotions.stream()
                .map(p -> ChartDataPoint.builder()
                        .label(p.getRang())
                        .value(p.getStagiaireCount() != null ? p.getStagiaireCount() : 0)
                        .color("#2563eb")
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getMoyennesEvolution(String schoolId) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        java.util.Collections.reverse(promotions);
        return promotions.stream()
                .map(p -> ChartDataPoint.builder()
                        .label(p.getRang())
                        .value(p.getMajorScore() != null ? p.getMajorScore() : 0)
                        .color("#D4AF37")
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DonutSegment> getGradeDistribution(String schoolId, String rang) {
        Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
        if (promotion == null) return List.of();

        List<Stagiaire> stagiaires = stagiaireRepository.findByPromotionIdOrderByRangAsc(promotion.getId());
        long count = stagiaires.size();
        if (count == 0) return List.of();

        BigDecimal SIXTEEN = BigDecimal.valueOf(16);
        BigDecimal TWELVE = BigDecimal.valueOf(12);
        BigDecimal TEN = BigDecimal.valueOf(10);

        long excellent = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(SIXTEEN) >= 0).count();
        long bien = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TWELVE) >= 0 && s.getMoyenne().compareTo(SIXTEEN) < 0).count();
        long moyen = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) >= 0 && s.getMoyenne().compareTo(TWELVE) < 0).count();
        long faible = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) < 0).count();

        return List.of(
                createSegment("Excellent (16-20)", excellent, count, "#2E7D32", 100, 0),
                createSegment("Bien (12-16)", bien, count, "#1565C0", 100, excellent * 100 / (double) count),
                createSegment("Moyen (10-12)", moyen, count, "#F9A825", 100, (excellent + bien) * 100 / (double) count),
                createSegment("Faible (<10)", faible, count, "#C62828", 100, (excellent + bien + moyen) * 100 / (double) count)
        );
    }

    private DonutSegment createSegment(String label, long count, long total, String color,
                                        double circumference, double offsetPct) {
        double percentage = total > 0 ? (count * 100.0 / total) : 0;
        return DonutSegment.builder()
                .label(label)
                .percentage(Math.round(percentage * 10.0) / 10.0)
                .color(color)
                .dashArray(String.valueOf(circumference))
                .dashOffset(String.valueOf(circumference * offsetPct / 100.0))
                .count((int) count)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PodiumEntry> getTop5(String schoolId, String rang) {
        Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
        if (promotion == null) return List.of();

        List<Stagiaire> top5 = stagiaireRepository.findTop5ByPromotionId(promotion.getId());
        List<PodiumEntry> podium = new ArrayList<>();
        for (int i = 0; i < top5.size(); i++) {
            podium.add(mapper.toPodiumEntry(i + 1, top5.get(i)));
        }
        return podium;
    }

    private Promotion findPromotionByRangFlexible(String schoolId, String rang) {
        // First try exact match
        Optional<Promotion> exact = promotionRepository.findFirstBySchoolIdAndRang(schoolId, rang);
        if (exact.isPresent()) return exact.get();

        // Then try matching by the numeric part
        Matcher matcher = Pattern.compile("(\\d+)").matcher(rang);
        if (matcher.find()) {
            String num = matcher.group(1);
            Optional<Promotion> byNum = promotionRepository.findBySchoolIdAndRangStartingWith(schoolId, num);
            if (byNum.isPresent()) return byNum.get();
        }

        // Fallback: iterate all promotions and find one whose rang contains the same numeric value
        List<Promotion> all = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        Matcher inputMatcher = Pattern.compile("(\\d+)").matcher(rang);
        if (inputMatcher.find()) {
            String inputNum = inputMatcher.group(1);
            for (Promotion p : all) {
                Matcher pMatcher = Pattern.compile("(\\d+)").matcher(p.getRang());
                if (pMatcher.find() && pMatcher.group(1).equals(inputNum)) {
                    return p;
                }
            }
        }
        return null;
    }
}
