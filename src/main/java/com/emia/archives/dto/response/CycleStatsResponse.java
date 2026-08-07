package com.emia.archives.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CycleStatsResponse {

    private CycleStatsBlock readaptation;
    private CycleStatsBlock adaptation;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class CycleStatsBlock {
        private long total;
        private BigDecimal average;
        private BigDecimal best;
        private BigDecimal worst;
        private List<DonutSegment> gradeDistribution;
        private List<PodiumEntry> topList;
        private List<PodiumEntry> lowList;
        private List<ChartDataPoint> mentions;
        private long deceased;
        private Map<String, Long> statusCount;
    }
}
