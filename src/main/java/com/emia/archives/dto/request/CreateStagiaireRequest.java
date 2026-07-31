package com.emia.archives.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateStagiaireRequest {

    @NotBlank(message = "Le nom complet est requis")
    @Size(min = 2, message = "Le nom doit contenir au moins 2 caractères")
    private String nomComplet;

    private String matricule;

    @NotBlank(message = "La promotion est requise")
    private String promotion;

    @NotBlank(message = "L'école est requise")
    private String schoolId;

    @DecimalMin(value = "0.0", message = "La moyenne ne peut pas être négative")
    @DecimalMax(value = "20.0", message = "La moyenne ne peut pas dépasser 20")
    private BigDecimal moyenne;

    private Integer rang;

    private String grade;
    private String cycleType;
    private String status;
    private String observations;
    private String informationsSupplementaires;
    private Boolean estDecede;
    private String photoUrl;
}
