package com.emia.archives.service;

import com.emia.archives.dto.response.*;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.enums.CycleType;
import com.emia.archives.enums.StagiaireStatus;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.PromotionRepository;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public List<KpiData> getKpis(String schoolId, String rang, String nationality) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        long totalPromotions = promotions.size();

        long totalStagiaires;
        double avgMoyenne;
        double bestScore;
        String trendText;

        boolean hasNat = nationality != null && !nationality.isBlank();

        if (rang != null && !rang.isBlank()) {
            Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
            if (promotion != null) {
                totalStagiaires = hasNat
                    ? stagiaireRepository.countByPromotionIdAndObservations(promotion.getId(), nationality)
                    : stagiaireRepository.countByPromotionId(promotion.getId());
                avgMoyenne = hasNat
                    ? stagiaireRepository.averageMoyenneByPromotionIdAndObservations(promotion.getId(), nationality).doubleValue()
                    : stagiaireRepository.averageMoyenneByPromotionId(promotion.getId()).doubleValue();
                bestScore = hasNat
                    ? stagiaireRepository.maxMoyenneByPromotionIdAndObservations(promotion.getId(), nationality).doubleValue()
                    : stagiaireRepository.maxMoyenneByPromotionId(promotion.getId()).doubleValue();
                trendText = "↑ " + (promotion.getNom() != null ? promotion.getNom() : "");
            } else {
                totalStagiaires = 0;
                avgMoyenne = 0;
                bestScore = 0;
                trendText = "";
            }
        } else {
            totalStagiaires = hasNat
                ? stagiaireRepository.countBySchoolIdAndObservations(schoolId, nationality)
                : stagiaireRepository.countBySchoolId(schoolId);
            avgMoyenne = hasNat
                ? stagiaireRepository.averageMoyenneBySchoolIdAndObservations(schoolId, nationality).doubleValue()
                : stagiaireRepository.averageMoyenneBySchoolId(schoolId).doubleValue();
            bestScore = hasNat
                ? stagiaireRepository.maxMoyenneBySchoolIdAndObservations(schoolId, nationality).doubleValue()
                : stagiaireRepository.maxMoyenneBySchoolId(schoolId).doubleValue();
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
    public List<ChartDataPoint> getEffectifsEvolution(String schoolId, String nationality) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        promotions.sort((a, b) -> Integer.compare(toRangInt(b.getRang()), toRangInt(a.getRang())));
        boolean hasNat = nationality != null && !nationality.isBlank();
        return promotions.stream()
                .map(p -> {
                    int count = hasNat
                        ? (int) stagiaireRepository.countByPromotionIdAndObservations(p.getId(), nationality)
                        : (p.getStagiaireCount() != null ? p.getStagiaireCount() : 0);
                    return ChartDataPoint.builder()
                        .label(p.getRang())
                        .value(count)
                        .color("#2563eb")
                        .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getMoyennesEvolution(String schoolId, String nationality) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        promotions.sort((a, b) -> Integer.compare(toRangInt(b.getRang()), toRangInt(a.getRang())));
        boolean hasNat = nationality != null && !nationality.isBlank();
        return promotions.stream()
                .map(p -> {
                    double avg = hasNat
                        ? stagiaireRepository.averageMoyenneByPromotionIdAndObservations(p.getId(), nationality).doubleValue()
                        : (p.getMajorScore() != null ? p.getMajorScore() : 0);
                    return ChartDataPoint.builder()
                        .label(p.getRang())
                        .value(avg)
                        .color("#D4AF37")
                        .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DonutSegment> getGradeDistribution(String schoolId, String rang, String nationality) {
        Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
        if (promotion == null) return List.of();

        List<Stagiaire> stagiaires = stagiaireRepository.findByPromotionIdOrderByRangAsc(promotion.getId());

        boolean hasNat = nationality != null && !nationality.isBlank();
        if (hasNat) {
            stagiaires = stagiaires.stream()
                .filter(s -> nationality.equals(s.getObservations()))
                .toList();
        }

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
    public List<PodiumEntry> getTop5(String schoolId, String rang, String nationality) {
        Promotion promotion = findPromotionByRangFlexible(schoolId, rang);
        if (promotion == null) return List.of();

        boolean hasNat = nationality != null && !nationality.isBlank();
        List<Stagiaire> top5 = hasNat
            ? stagiaireRepository.findTop5ByPromotionIdAndObservations(promotion.getId(), nationality)
            : stagiaireRepository.findTop5ByPromotionId(promotion.getId());
        List<PodiumEntry> podium = new ArrayList<>();
        for (int i = 0; i < top5.size(); i++) {
            podium.add(mapper.toPodiumEntry(i + 1, top5.get(i)));
        }
        return podium;
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getNationalityDistribution(String schoolId) {
        List<Stagiaire> allStagiaires = stagiaireRepository.findBySchoolIdAndPromotionIsNotNull(schoolId);

        java.util.Map<String, Integer> countryCounts = new java.util.LinkedHashMap<>();
        for (Stagiaire s : allStagiaires) {
            String country = com.emia.archives.util.CountryResolver.resolve(s.getObservations());
            if (country != null) {
                countryCounts.merge(country, 1, Integer::sum);
            }
        }

        String[] palette = {
            "#2563eb", "#D4AF37", "#2E7D32", "#C62828", "#6A1B9A",
            "#00838F", "#E65100", "#4E342E", "#37474F", "#827717"
        };

        List<java.util.Map.Entry<String, Integer>> sorted = new java.util.ArrayList<>(countryCounts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<ChartDataPoint> points = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            java.util.Map.Entry<String, Integer> entry = sorted.get(i);
            points.add(ChartDataPoint.builder()
                    .label(entry.getKey())
                    .value(entry.getValue())
                    .color(palette[i % palette.length])
                    .build());
        }
        return points;
    }

    @Transactional(readOnly = true)
    public CycleStatsResponse getCycleStats(String schoolId) {
        return CycleStatsResponse.builder()
                .readaptation(buildCycleBlock(schoolId, CycleType.READAPTATION))
                .adaptation(buildCycleBlock(schoolId, CycleType.ADAPTATION))
                .build();
    }

    private CycleStatsResponse.CycleStatsBlock buildCycleBlock(String schoolId, CycleType type) {
        List<Stagiaire> stagiaires = stagiaireRepository.findBySchoolIdAndCycleType(schoolId, type);

        List<Stagiaire> sortedDesc = stagiaires.stream()
                .filter(s -> s.getMoyenne() != null)
                .sorted(Comparator.comparing(Stagiaire::getMoyenne).reversed())
                .toList();

        BigDecimal average = BigDecimal.ZERO;
        BigDecimal best = BigDecimal.ZERO;
        BigDecimal worst = BigDecimal.ZERO;
        if (!sortedDesc.isEmpty()) {
            average = sortedDesc.stream()
                    .map(Stagiaire::getMoyenne)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(sortedDesc.size()), 2, RoundingMode.HALF_UP);
            best = sortedDesc.get(0).getMoyenne();
            worst = sortedDesc.get(sortedDesc.size() - 1).getMoyenne();
        }

        long count = stagiaires.size();
        BigDecimal SIXTEEN = BigDecimal.valueOf(16);
        BigDecimal FOURTEEN = BigDecimal.valueOf(14);
        BigDecimal TWELVE = BigDecimal.valueOf(12);
        BigDecimal TEN = BigDecimal.valueOf(10);

        long excellent = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(SIXTEEN) >= 0).count();
        long bien = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TWELVE) >= 0 && s.getMoyenne().compareTo(SIXTEEN) < 0).count();
        long moyen = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) >= 0 && s.getMoyenne().compareTo(TWELVE) < 0).count();
        long faible = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) < 0).count();

        List<DonutSegment> gradeDistribution = List.of(
                createSegment("Excellent (16-20)", excellent, count, "#2E7D32", 100, 0),
                createSegment("Bien (12-16)", bien, count, "#1565C0", 100, excellent * 100 / (double) count),
                createSegment("Moyen (10-12)", moyen, count, "#F9A825", 100, (excellent + bien) * 100 / (double) count),
                createSegment("Faible (<10)", faible, count, "#C62828", 100, (excellent + bien + moyen) * 100 / (double) count)
        );

        List<PodiumEntry> topList = new ArrayList<>();
        for (int i = 0; i < Math.min(sortedDesc.size(), 5); i++) {
            topList.add(mapper.toPodiumEntry(i + 1, sortedDesc.get(i)));
        }

        List<Stagiaire> belowTwelve = stagiaires.stream()
                .filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TWELVE) < 0)
                .sorted(Comparator.comparing(Stagiaire::getMoyenne))
                .toList();
        List<PodiumEntry> lowList = new ArrayList<>();
        for (int i = 0; i < Math.min(belowTwelve.size(), 10); i++) {
            lowList.add(mapper.toPodiumEntry(i + 1, belowTwelve.get(i)));
        }

        long mentionTresBien = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(SIXTEEN) >= 0).count();
        long mentionBien = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(FOURTEEN) >= 0 && s.getMoyenne().compareTo(SIXTEEN) < 0).count();
        long mentionAssezBien = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TWELVE) >= 0 && s.getMoyenne().compareTo(FOURTEEN) < 0).count();
        long mentionPassable = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) >= 0 && s.getMoyenne().compareTo(TWELVE) < 0).count();
        long mentionInsuffisant = stagiaires.stream().filter(s -> s.getMoyenne() != null && s.getMoyenne().compareTo(TEN) < 0).count();

        List<ChartDataPoint> mentions = List.of(
                ChartDataPoint.builder().label("Très Bien (≥16)").value(mentionTresBien).color("#2E7D32").build(),
                ChartDataPoint.builder().label("Bien (14-16)").value(mentionBien).color("#1565C0").build(),
                ChartDataPoint.builder().label("Assez Bien (12-14)").value(mentionAssezBien).color("#7B1FA2").build(),
                ChartDataPoint.builder().label("Passable (10-12)").value(mentionPassable).color("#F9A825").build(),
                ChartDataPoint.builder().label("Insuffisant (<10)").value(mentionInsuffisant).color("#C62828").build()
        );

        long deceased = stagiaires.stream().filter(s -> s.getEstDecede() != null && s.getEstDecede()).count();

        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (StagiaireStatus status : StagiaireStatus.values()) {
            statusCount.put(status.name(), stagiaires.stream().filter(s -> s.getStatus() == status).count());
        }

        return CycleStatsResponse.CycleStatsBlock.builder()
                .total(count)
                .average(average)
                .best(best)
                .worst(worst)
                .gradeDistribution(gradeDistribution)
                .topList(topList)
                .lowList(lowList)
                .mentions(mentions)
                .deceased(deceased)
                .statusCount(statusCount)
                .build();
    }

    private Promotion findPromotionByRangFlexible(String schoolId, String rang) {
        Optional<Promotion> exact = promotionRepository.findFirstBySchoolIdAndRang(schoolId, rang);
        if (exact.isPresent()) return exact.get();

        Matcher matcher = Pattern.compile("(\\d+)").matcher(rang);
        if (matcher.find()) {
            String num = matcher.group(1);
            Optional<Promotion> byNum = promotionRepository.findBySchoolIdAndRangStartingWith(schoolId, num);
            if (byNum.isPresent()) return byNum.get();
        }

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

    private int toRangInt(String rang) {
        Matcher m = Pattern.compile("(\\d+)").matcher(rang);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
