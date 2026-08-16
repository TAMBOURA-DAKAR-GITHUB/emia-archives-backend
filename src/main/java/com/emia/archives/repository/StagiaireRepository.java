package com.emia.archives.repository;

import com.emia.archives.entity.Stagiaire;
import com.emia.archives.enums.CycleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {

    List<Stagiaire> findByPromotionIdOrderByRangAsc(Long promotionId);

    List<Stagiaire> findBySchoolId(String schoolId);

    List<Stagiaire> findBySchoolIdAndPromotionIsNull(String schoolId);

    List<Stagiaire> findBySchoolIdAndPromotionIsNotNull(String schoolId);

    List<Stagiaire> findBySchoolIdAndCycleType(String schoolId, CycleType cycleType);

    List<Stagiaire> findByPromotionSchoolIdAndPromotionRangOrderByRangAsc(String schoolId, String rang);

    Optional<Stagiaire> findByMatricule(String matricule);

    @Query("SELECT COALESCE(MAX(s.numeroEoa), 0) FROM Stagiaire s")
    Integer findMaxNumeroEoa();

    List<Stagiaire> findAllByOrderByIdAsc();

    @Query("SELECT DISTINCT s.promotion.id FROM Stagiaire s WHERE s.promotion IS NOT NULL")
    List<Long> findDistinctPromotionIds();

    @Query("SELECT COUNT(s) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.promotion IS NOT NULL")
    long countBySchoolId(@Param("schoolId") String schoolId);

    long countByPromotionId(Long promotionId);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.promotion IS NOT NULL")
    BigDecimal averageMoyenneBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.promotion IS NOT NULL")
    BigDecimal maxMoyenneBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId")
    BigDecimal averageMoyenneByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId")
    BigDecimal maxMoyenneByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT s FROM Stagiaire s WHERE s.promotion.id = :promotionId ORDER BY s.moyenne DESC LIMIT 5")
    List<Stagiaire> findTop5ByPromotionId(@Param("promotionId") Long promotionId);

    @Query("SELECT COUNT(s) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.status = 'ABANDONNE'")
    long countAbandonnesBySchoolId(@Param("schoolId") String schoolId);

    @Query("SELECT s FROM Stagiaire s WHERE s.school.id = :schoolId AND s.promotion.rang = :rang ORDER BY CASE WHEN s.rang IS NULL THEN 1 ELSE 0 END, s.rang ASC")
    List<Stagiaire> findBySchoolIdAndPromotionRangOrderByRangAsc(@Param("schoolId") String schoolId, @Param("rang") String rang);

    @Query("SELECT s FROM Stagiaire s WHERE s.school.id = :schoolId AND (LOWER(s.nomComplet) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.matricule) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Stagiaire> searchBySchoolId(@Param("schoolId") String schoolId, @Param("q") String q);

    @Query("SELECT s FROM Stagiaire s WHERE s.promotion.id = :promotionId ORDER BY CASE WHEN s.moyenne IS NULL THEN 1 ELSE 0 END, s.moyenne DESC")
    List<Stagiaire> findAllByPromotionIdOrderByMoyenneDesc(@Param("promotionId") Long promotionId);

    @Query("SELECT COUNT(s) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.observations = :observations AND s.promotion IS NOT NULL")
    long countBySchoolIdAndObservations(@Param("schoolId") String schoolId, @Param("observations") String observations);

    long countByPromotionIdAndObservations(Long promotionId, String observations);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.observations = :observations AND s.promotion IS NOT NULL")
    BigDecimal averageMoyenneBySchoolIdAndObservations(@Param("schoolId") String schoolId, @Param("observations") String observations);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.school.id = :schoolId AND s.observations = :observations AND s.promotion IS NOT NULL")
    BigDecimal maxMoyenneBySchoolIdAndObservations(@Param("schoolId") String schoolId, @Param("observations") String observations);

    @Query("SELECT COALESCE(AVG(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId AND s.observations = :observations")
    BigDecimal averageMoyenneByPromotionIdAndObservations(@Param("promotionId") Long promotionId, @Param("observations") String observations);

    @Query("SELECT COALESCE(MAX(s.moyenne), 0) FROM Stagiaire s WHERE s.promotion.id = :promotionId AND s.observations = :observations")
    BigDecimal maxMoyenneByPromotionIdAndObservations(@Param("promotionId") Long promotionId, @Param("observations") String observations);

    @Query("SELECT s FROM Stagiaire s WHERE s.promotion.id = :promotionId AND s.observations = :observations ORDER BY CASE WHEN s.moyenne IS NULL THEN 1 ELSE 0 END, s.moyenne DESC LIMIT 5")
    List<Stagiaire> findTop5ByPromotionIdAndObservations(@Param("promotionId") Long promotionId, @Param("observations") String observations);

    @Query(value = "SELECT s.observations AS nationality, COUNT(*) AS cnt FROM stagiaires s WHERE s.school_id = :schoolId AND s.observations IS NOT NULL AND s.observations != '' GROUP BY s.observations ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByNationality(@Param("schoolId") String schoolId);

    @Query(value = "SELECT s.observations AS nationality, COUNT(*) AS cnt FROM stagiaires s WHERE s.school_id = :schoolId AND s.promotion_id = :promotionId AND s.observations IS NOT NULL AND s.observations != '' GROUP BY s.observations ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByNationalityAndPromotionId(@Param("schoolId") String schoolId, @Param("promotionId") Long promotionId);

    @Query("SELECT DISTINCT s.observations FROM Stagiaire s WHERE s.school.id = :schoolId AND s.observations IS NOT NULL AND s.observations != ''")
    List<String> findDistinctObservationsBySchoolId(@Param("schoolId") String schoolId);
}
