package com.emia.archives.entity;

import com.emia.archives.enums.GradeEnum;
import com.emia.archives.enums.PersonnelCategorie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personnels")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "date_debut_service")
    private LocalDate dateDebutService;

    @Column(name = "date_fin_service")
    private LocalDate dateFinService;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private GradeEnum grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 20)
    private PersonnelCategorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
