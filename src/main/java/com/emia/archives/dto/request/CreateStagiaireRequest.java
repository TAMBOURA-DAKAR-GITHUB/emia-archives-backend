package com.emia.archives.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateStagiaireRequest {

    @NotBlank(message = "Le nom complet est requis")
    @Size(min = 2, message = "Le nom doit contenir au moins 2 caractères")
    private String nomComplet;

    private String matricule;

    private String promotion;

    @NotBlank(message = "L'école est requise")
    private String schoolId;

    @DecimalMin(value = "0.0", message = "La moyenne ne peut pas être négative")
    @DecimalMax(value = "20.0", message = "La moyenne ne peut pas dépasser 20")
    private BigDecimal moyenne;

    private LocalDate dateNaissance;

    @Size(max = 200, message = "Le lieu de naissance ne doit pas dépasser 200 caractères")
    private String lieuNaissance;

    private Integer rang;

    private String grade;
    private String cycleType;
    private String status;
    private String observations;
    private String informationsSupplementaires;
    private Boolean estDecede;
    private String photoUrl;
}
