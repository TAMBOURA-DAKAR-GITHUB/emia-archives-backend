package com.emia.archives.entity;

import com.emia.archives.enums.CycleType;
import com.emia.archives.enums.GradeEnum;
import com.emia.archives.enums.StagiaireStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stagiaires")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Stagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(length = 50, unique = true)
    private String matricule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(precision = 4, scale = 2)
    private BigDecimal moyenne;

    @Column
    private Integer rang;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private GradeEnum grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", length = 20)
    private CycleType cycleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StagiaireStatus status;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "informations_supplementaires", columnDefinition = "TEXT")
    private String informationsSupplementaires;

    @Column(name = "est_decede")
    private Boolean estDecede;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = StagiaireStatus.EN_COURS;
        if (estDecede == null) estDecede = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
