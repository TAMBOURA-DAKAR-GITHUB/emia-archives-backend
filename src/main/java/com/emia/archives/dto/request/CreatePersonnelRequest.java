package com.emia.archives.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreatePersonnelRequest {

    private String nomComplet;
    private LocalDate dateDebutService;
    private LocalDate dateFinService;
    private String grade;
    private String categorie;
    private String schoolId;
}
