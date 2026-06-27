package com.emia.archives.repository;

import com.emia.archives.entity.Personnel;
import com.emia.archives.enums.PersonnelCategorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    List<Personnel> findBySchoolIdAndCategorieOrderByNomCompletAsc(String schoolId, PersonnelCategorie categorie);

    Optional<Personnel> findByIdAndSchoolId(Long id, String schoolId);

    @Query("SELECT p FROM Personnel p WHERE p.school.id = :schoolId AND p.categorie = :categorie " +
           "AND p.dateDebutService <= :dateFin AND (p.dateFinService IS NULL OR p.dateFinService >= :dateDebut)")
    List<Personnel> findOverlappingService(@Param("schoolId") String schoolId,
                                           @Param("categorie") PersonnelCategorie categorie,
                                           @Param("dateDebut") LocalDate dateDebut,
                                           @Param("dateFin") LocalDate dateFin);

    @Query("SELECT p FROM Personnel p WHERE p.school.id = :schoolId AND p.categorie = :categorie AND LOWER(p.nomComplet) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Personnel> searchBySchoolIdAndCategorie(@Param("schoolId") String schoolId,
                                                  @Param("categorie") PersonnelCategorie categorie,
                                                  @Param("q") String q);
}
