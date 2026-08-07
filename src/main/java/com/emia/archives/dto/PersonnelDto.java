package com.emia.archives.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PersonnelDto {

    private Long id;
    private String nomComplet;
    private LocalDate dateDebutService;
    private LocalDate dateFinService;
    private String grade;
    private String categorie;
    private String matiereEnseignee;
    private String schoolId;
}
