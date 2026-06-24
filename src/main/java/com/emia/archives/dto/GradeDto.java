package com.emia.archives.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GradeDto {

    private Long id;
    private String code;
    private String label;
    private String abbreviation;
    private int order;
}
