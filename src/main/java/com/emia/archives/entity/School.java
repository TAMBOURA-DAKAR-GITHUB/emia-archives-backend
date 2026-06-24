package com.emia.archives.entity;

import com.emia.archives.enums.SchoolType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "schools")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class School {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String acronym;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SchoolType type;

    @Column(length = 500)
    private String motto;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

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
