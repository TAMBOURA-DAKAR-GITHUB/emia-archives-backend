package com.emia.archives.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "rang"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 50)
    private String rang;

    @Column
    private String nom;

    @Column(name = "annee_entree")
    private Integer anneeEntree;

    @Column(name = "annee_sortie")
    private Integer anneeSortie;

    @Column(name = "cycle_special")
    private Boolean cycleSpecial;

    @Column(name = "cycle_special_info", length = 500)
    private String cycleSpecialInfo;

    @Column(name = "stagiaire_count")
    private Integer stagiaireCount;

    @Column(name = "major_score")
    private Double majorScore;

    @Column(name = "major_name")
    private String majorName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (cycleSpecial == null) cycleSpecial = false;
        if (stagiaireCount == null) stagiaireCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
