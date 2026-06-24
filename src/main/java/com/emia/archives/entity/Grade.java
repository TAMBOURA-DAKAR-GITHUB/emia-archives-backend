package com.emia.archives.entity;

import com.emia.archives.enums.GradeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grades")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private GradeEnum code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 20)
    private String abbreviation;

    @Column(name = "grade_order", nullable = false)
    private Integer gradeOrder;
}
