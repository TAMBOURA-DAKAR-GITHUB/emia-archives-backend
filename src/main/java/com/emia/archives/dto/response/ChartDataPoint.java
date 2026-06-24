package com.emia.archives.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChartDataPoint {

    private String label;
    private double value;
    private String color;
}
