package com.emia.archives.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DashboardStats {

    private int totalStagiaires;
    private String majorName;
    private String majorGrade;
    private double bestAverage;
    private String promotionRang;
}
