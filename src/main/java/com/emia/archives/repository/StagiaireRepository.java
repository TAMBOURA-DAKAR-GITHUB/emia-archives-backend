package com.emia.archives.repository;

import com.emia.archives.entity.Stagiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {

    List<Stagiaire> findByPromotionIdOrderByRangAsc(Long promotionId);

    List<Stagiaire> findBySchoolId(String schoolId);

    List<Stagiaire> findByPromotionSchoolIdAndPromotionRangOrderByRangAsc(String schoolId, String rang);

    Optional<Stagiaire> findByMatricule(String matricule);

    long countBySchoolId(String schoolId);

    long countByPromotionId(Long promotionId);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId")
    BigDecimal averageMoyenneBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId")
    BigDecimal maxMoyenneBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId")
    BigDecimal averageMoyenneByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId")
    BigDecimal maxMoyenneByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT s FROM Stagiaire s WHERE s.promotion.id = :promotionId ORDER BY s.moyenne DESC LIMIT 5")
    List<Stagiaire> findTop5ByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT COUNT(s) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.status = 'ABANDONNE'")
    long countAbandonnesBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT s FROM Stagiaire s WHERE s.school.id = :schoolId AND s.promotion.rang = :rang ORDER BY s.rang ASC")
    List<Stagiaire> findBySchoolIdAndPromotionRangOrderByRangAsc(@Param("schoolId") String schoolId, @Param("rang") String rang);

    @Query("SELECT s FROM Stagiaire s WHERE s.school.id = :schoolId AND (LOWER(s.nomComplet) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.matricule) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Stagiaire> searchBySchoolId(@Param("schoolId") String schoolId, @Param("q") String q);
}
