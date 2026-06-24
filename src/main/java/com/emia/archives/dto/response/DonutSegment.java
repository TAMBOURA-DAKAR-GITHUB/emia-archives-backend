package com.emia.archives.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DonutSegment {

    private String label;
    private double percentage;
    private String color;
    private String dashArray;
    private String dashOffset;
    private int count;
}
