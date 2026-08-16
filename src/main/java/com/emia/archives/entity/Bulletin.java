package com.emia.archives.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulletins",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stagiaire_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Bulletin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @Column(length = 100)
    private String pays;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String coefficients;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(length = 20)
    private String moyenne1;

    @Column(length = 20)
    private String moyenne2;

    @Column(length = 20)
    private String moyenne3;

    @Column(name = "classement_rang", length = 20)
    private String classementRang;

    @Column(name = "classement_total", length = 20)
    private String classementTotal;

    @Column(columnDefinition = "TEXT")
    private String appreciation;

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