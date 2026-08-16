package com.emia.archives.config;

import com.emia.archives.entity.*;
import com.emia.archives.enums.*;
import com.emia.archives.repository.*;
import com.emia.archives.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final GradeRepository gradeRepository;
    private final PromotionRepository promotionRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StagiaireService stagiaireService;

    @Override
    public void run(String... args) {
        initSchool();
        initGrades();
        initAdminUser();
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
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .username("admin")
                    .firstName("Administrateur")
                    .lastName("Système")
                    .email("admin@eemk.mil.ml")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
        }
        admin.setPassword("1234");
        userRepository.save(admin);}
}
