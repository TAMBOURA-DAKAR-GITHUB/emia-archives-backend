package com.emia.archives.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KpiData {

    private String icon;
    private String label;
    private String value;
    private String trend;
    private boolean trendDown;
}
