package com.emia.archives.repository;

import com.emia.archives.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findBySchoolIdOrderByAnneeEntreeDesc(String schoolId);

    Optional<Promotion> findFirstBySchoolIdAndRang(String schoolId, String rang);

    Optional<Promotion> findTopBySchoolIdOrderByAnneeEntreeDesc(String schoolId);

    @Query("SELECT p FROM Promotion p WHERE p.school.id = :schoolId AND p.rang LIKE CONCAT(:num, '%') ORDER BY p.anneeEntree DESC LIMIT 1")
    Optional<Promotion> findBySchoolIdAndRangStartingWith(@Param("schoolId") String schoolId, @Param("num") String num);

    @Query("SELECT p FROM Promotion p WHERE p.school.id = :schoolId AND p.anneeEntree <= :anneeFin AND p.anneeSortie >= :anneeDebut ORDER BY p.anneeEntree DESC")
    List<Promotion> findBySchoolIdAndAnneeOverlap(@Param("schoolId") String schoolId,
                                                   @Param("anneeDebut") Integer anneeDebut,
                                                   @Param("anneeFin") Integer anneeFin);
}
