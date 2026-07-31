package com.emia.archives.mapper;

import com.emia.archives.dto.*;
import com.emia.archives.dto.request.CreatePersonnelRequest;
import com.emia.archives.dto.response.DashboardStats;
import com.emia.archives.dto.response.PodiumEntry;
import com.emia.archives.entity.*;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }

    public SchoolDto toDto(School school) {
        if (school == null) return null;
        return SchoolDto.builder()
                .id(school.getId())
                .name(school.getName())
                .acronym(school.getAcronym())
                .type(school.getType().name())
                .motto(school.getMotto())
                .logoUrl(school.getLogoUrl())
                .build();
    }

    public PromotionDto toDto(Promotion promotion) {
        if (promotion == null) return null;
        return PromotionDto.builder()
                .id(promotion.getId())
                .rang(promotion.getRang())
                .nom(promotion.getNom())
                .anneeEntree(promotion.getAnneeEntree())
                .anneeSortie(promotion.getAnneeSortie())
                .cycleSpecial(promotion.getCycleSpecial() != null && promotion.getCycleSpecial())
                .cycleSpecialInfo(promotion.getCycleSpecialInfo())
                .stagiaireCount(promotion.getStagiaireCount() != null ? promotion.getStagiaireCount() : 0)
                .majorScore(promotion.getMajorScore() != null ? promotion.getMajorScore() : 0.0)
                .majorName(promotion.getMajorName())
                .emir(promotion.getEmir())
                .chefTondjon(promotion.getChefTondjon())
                .emirPhotoUrl(promotion.getEmirPhotoUrl())
                .chefTondjonPhotoUrl(promotion.getChefTondjonPhotoUrl())
                .schoolId(promotion.getSchool().getId())
                .build();
    }

    public StagiaireDto toDto(Stagiaire stagiaire) {
        if (stagiaire == null) return null;
        return StagiaireDto.builder()
                .id(stagiaire.getId())
                .nomComplet(stagiaire.getNomComplet())
                .matricule(stagiaire.getMatricule())
                .promotion(stagiaire.getPromotion().getRang())
                .schoolId(stagiaire.getSchool().getId())
                .moyenne(stagiaire.getMoyenne())
                .rang(stagiaire.getRang())
                .grade(stagiaire.getGrade() != null ? stagiaire.getGrade().name() : null)
                .cycleType(stagiaire.getCycleType() != null ? stagiaire.getCycleType().name() : null)
                .status(stagiaire.getStatus().name())
                .observations(stagiaire.getObservations())
                .informationsSupplementaires(stagiaire.getInformationsSupplementaires())
                .estDecede(stagiaire.getEstDecede() != null && stagiaire.getEstDecede())
                .photoUrl(stagiaire.getPhotoUrl())
                .build();
    }

    public GradeDto toDto(Grade grade) {
        if (grade == null) return null;
        return GradeDto.builder()
                .id(grade.getId())
                .code(grade.getCode().name())
                .label(grade.getLabel())
                .abbreviation(grade.getAbbreviation())
                .order(grade.getGradeOrder())
                .build();
    }

    public DashboardStats toDashboardStats(Promotion promotion, int total, String majorGrade) {
        return DashboardStats.builder()
                .totalStagiaires(total)
                .majorName(promotion.getMajorName())
                .majorGrade(majorGrade)
                .bestAverage(promotion.getMajorScore() != null ? promotion.getMajorScore() : 0.0)
                .promotionRang(promotion.getRang())
                .build();
    }

    public PodiumEntry toPodiumEntry(int rank, Stagiaire stagiaire) {
        return PodiumEntry.builder()
                .rank(rank)
                .name(stagiaire.getNomComplet())
                .grade(stagiaire.getGrade() != null ? stagiaire.getGrade().name() : null)
                .matricule(stagiaire.getMatricule())
                .average(stagiaire.getMoyenne())
                .build();
    }

    public PersonnelDto toDto(Personnel personnel) {
        if (personnel == null) return null;
        return PersonnelDto.builder()
                .id(personnel.getId())
                .nomComplet(personnel.getNomComplet())
                .dateDebutService(personnel.getDateDebutService())
                .dateFinService(personnel.getDateFinService())
                .grade(personnel.getGrade() != null ? personnel.getGrade().name() : null)
                .categorie(personnel.getCategorie().name())
                .schoolId(personnel.getSchool().getId())
                .build();
    }

    public Personnel toEntity(CreatePersonnelRequest request, School school, com.emia.archives.enums.PersonnelCategorie categorie) {
        Personnel.PersonnelBuilder builder = Personnel.builder()
                .nomComplet(request.getNomComplet())
                .dateDebutService(request.getDateDebutService())
                .dateFinService(request.getDateFinService())
                .categorie(categorie)
                .school(school);

        if (request.getGrade() != null) {
            try {
                builder.grade(com.emia.archives.enums.GradeEnum.valueOf(request.getGrade()));
            } catch (IllegalArgumentException e) {
                builder.grade(null);
            }
        }

        return builder.build();
    }
}
