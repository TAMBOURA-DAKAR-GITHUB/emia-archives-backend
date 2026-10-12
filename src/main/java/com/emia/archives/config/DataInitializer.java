package com.emia.archives.config;

import com.emia.archives.entity.*;
import com.emia.archives.enums.*;
import com.emia.archives.repository.*;
import com.emia.archives.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final GradeRepository gradeRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StagiaireService stagiaireService;

    @Override
    public void run(String... args) {
        initSchool();
        initGrades();
        initAdminUser();
        initViewerUser();
        backfillNumeroEoa();
        stagiaireService.recalculateAllRangs();
    }

    private void backfillNumeroEoa() {
        List<Stagiaire> missing = stagiaireRepository.findAllByOrderByIdAsc().stream()
                .filter(s -> s.getNumeroEoa() == null)
                .toList();
        if (missing.isEmpty()) return;
        int next = stagiaireRepository.findMaxNumeroEoa() + 1;
        for (Stagiaire s : missing) {
            s.setNumeroEoa(next++);
        }
        stagiaireRepository.saveAll(missing);
        log.info("NumeroEoa backfilled for {} stagiaires", missing.size());
    }

    private void initSchool() {
        if (schoolRepository.existsById("school-cert")) return;

        School school = School.builder()
                .id("school-cert")
                .name("Ecole Militaire Inter-Arme EMIA")
                .acronym("EMIA")
                .type(SchoolType.CERTIFICAT_ETAT_MAJOR)
                .motto("S'instruire pour mieux Conseiller")
                .logoUrl("images/logo_emia.png")
                .build();
        schoolRepository.save(school);
        log.info("School created: EMIA");
    }

    private void initGrades() {
        if (gradeRepository.count() > 0) return;

        List<Grade> grades = List.of(
                createGrade(GradeEnum.LIEUTENANT, "Lieutenant", "Lt", 1),
                createGrade(GradeEnum.CAPITAINE, "Capitaine", "Cpt", 2),
                createGrade(GradeEnum.COMMANDANT, "Commandant", "Cdt", 3),
                createGrade(GradeEnum.LIEUTENANT_COLONEL, "Lieutenant-Colonel", "Lt-Col", 4),
                createGrade(GradeEnum.COLONEL, "Colonel", "Col", 5),
                createGrade(GradeEnum.COLONEL_MAJOR, "Colonel-Major", "Col-Maj", 6),
                createGrade(GradeEnum.GENERAL_DE_BRIGADE, "Général de brigade", "Gén. Bde", 7),
                createGrade(GradeEnum.GENERAL_DE_DIVISION, "Général de division", "Gén. Div", 8),
                createGrade(GradeEnum.GENERAL_DE_CORPS_D_ARMEE, "Général de corps d'armée", "Gén. CA", 9),
                createGrade(GradeEnum.GENERAL_D_ARMEE, "Général d'armée", "Gén. A", 10)
        );
        gradeRepository.saveAll(grades);
        log.info("Grades created: {} grades", grades.size());
    }

    private Grade createGrade(GradeEnum code, String label, String abbreviation, int order) {
        return Grade.builder()
                .code(code)
                .label(label)
                .abbreviation(abbreviation)
                .gradeOrder(order)
                .build();
    }

    private void initAdminUser() {
        User admin = userRepository.findByUsername("emia")
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElse(null);
        if (admin == null) {
            // L'admin existe sous un autre nom ("dem" ou "admin") → migré vers "emia"
            admin = userRepository.findByUsername("dem")
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .orElse(userRepository.findByUsername("admin").orElse(null));
            if (admin != null) {
                // Si "emia" est pris par un autre compte (viewer), on le déplace temporairement
                userRepository.findByUsername("emia").ifPresent(other -> {
                    other.setUsername("emia-tmp");
                    userRepository.save(other);
                });
                admin.setUsername("emia");
            } else {
                admin = User.builder()
                        .username("emia")
                        .firstName("Administrateur")
                        .lastName("Système")
                        .email("ecolemilitaireinterarme@emia.mil.ml")
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build();
            }
        }
        admin.setPassword("Emia@12345");
        userRepository.save(admin);
        log.info("Admin user ready: emia");
    }

    private void initViewerUser() {
        User viewer = userRepository.findByUsername("dem")
                .filter(u -> u.getRole() == Role.VIEWER)
                .orElse(null);
        if (viewer == null) {
            // Le viewer existe sous un autre nom ("emia", "consultation", "emia-tmp") → migré vers "dem"
            viewer = userRepository.findByUsername("emia")
                    .filter(u -> u.getRole() == Role.VIEWER)
                    .orElse(userRepository.findByUsername("consultation")
                            .filter(u -> u.getRole() == Role.VIEWER)
                            .orElse(userRepository.findByUsername("emia-tmp")
                                    .filter(u -> u.getRole() == Role.VIEWER)
                                    .orElse(null)));
            if (viewer != null) {
                // Si "dem" est déjà pris, on le déplace temporairement
                userRepository.findByUsername("dem").ifPresent(other -> {
                    other.setUsername("dem-tmp");
                    userRepository.save(other);
                });
                viewer.setUsername("dem");
            } else {
                viewer = User.builder()
                        .username("dem")
                        .firstName("DEM")
                        .lastName("Consultation")
                        .email("ecolemilitaireinterarme@emia.mil.ml")
                        .role(Role.VIEWER)
                        .enabled(true)
                        .build();
            }
        }
        viewer.setPassword("Dem@12345");
        userRepository.save(viewer);
        log.info("Viewer user ready: dem");
    }
}
