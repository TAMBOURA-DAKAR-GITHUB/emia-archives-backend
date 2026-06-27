package com.emia.archives.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StagiaireDto {

    private Long id;
    private String nomComplet;
    private String matricule;
    private String promotion;
    private String schoolId;
    private BigDecimal moyenne;
    private Integer rang;
    private String grade;
    private String cycleType;
    private String status;
    private String observations;
    private String informationsSupplementaires;
    private boolean estDecede;
    private String photoUrl;
}
