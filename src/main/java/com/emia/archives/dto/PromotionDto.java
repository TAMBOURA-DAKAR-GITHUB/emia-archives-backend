package com.emia.archives.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PromotionDto {

    private Long id;
    private String rang;
    private String nom;
    private int anneeEntree;
    private int anneeSortie;
    private boolean cycleSpecial;
    private String cycleSpecialInfo;
    private int stagiaireCount;
    private double majorScore;
    private String majorName;
    private String schoolId;
}
