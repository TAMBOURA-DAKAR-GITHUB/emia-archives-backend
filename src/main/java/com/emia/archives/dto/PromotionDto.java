package com.emia.archives.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PromotionDto {

    private Long id;
    private String rang;
    private String nom;
    private LocalDate anneeEntree;
    private LocalDate anneeSortie;
    private boolean cycleSpecial;
    private String cycleSpecialInfo;
    private int stagiaireCount;
    private double majorScore;
    private String majorName;
    private String emir;
    private String chefTondjon;
    private String emirPhotoUrl;
    private String chefTondjonPhotoUrl;
    private String schoolId;
}
