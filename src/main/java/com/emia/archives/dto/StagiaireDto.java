package com.emia.archives.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StagiaireDto {

    private Long id;
    private String nomComplet;
    private String matricule;
    private Integer numeroEoa;
    private String promotion;
    private String schoolId;
    private BigDecimal moyenne;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private Integer rang;
    private String grade;
    private String cycleType;
    private String status;
    private String observations;
    private String informationsSupplementaires;
    private boolean estDecede;
    private String photoUrl;
}
