package com.emia.archives.service;

import com.emia.archives.dto.PromotionDto;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.School;
import com.emia.archives.exception.BadRequestException;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.PromotionRepository;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final StagiaireRepository stagiaireRepository;
    private final SchoolService schoolService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotionsBySchool(String schoolId) {
        schoolService.getSchoolEntityById(schoolId);
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        promotions.sort((a, b) -> Integer.compare(toRangInt(b.getRang()), toRangInt(a.getRang())));
        return promotions.stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionDto getPromotionBySchoolAndRang(String schoolId, String rang) {
        Promotion promotion = promotionRepository.findFirstBySchoolIdAndRang(schoolId, rang)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "rang", rang));
        return mapper.toDto(promotion);
    }

    @Transactional(readOnly = true)
    public Promotion getPromotionEntityBySchoolAndRang(String schoolId, String rang) {
        return promotionRepository.findFirstBySchoolIdAndRang(schoolId, rang)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "rang", rang));
    }

    @Transactional(readOnly = true)
    public Promotion getPromotionEntityBySchoolAndRangNumber(String schoolId, String rang) {
        // First try exact match
        Optional<Promotion> exact = promotionRepository.findFirstBySchoolIdAndRang(schoolId, rang);
        if (exact.isPresent()) return exact.get();

        // Then try matching by the numeric part
        Matcher matcher = Pattern.compile("(\\d+)").matcher(rang);
        if (matcher.find()) {
            String num = matcher.group(1);
            Optional<Promotion> byNum = promotionRepository.findBySchoolIdAndRangStartingWith(schoolId, num);
            if (byNum.isPresent()) return byNum.get();
        }

        throw new ResourceNotFoundException("Promotion", "rang", rang);
    }

    @Transactional(readOnly = true)
    public Promotion getPromotionEntityById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
    }

    @Transactional(readOnly = true)
    public PromotionDto getActivePromotion(String schoolId) {
        List<Promotion> promotions = promotionRepository.findBySchoolIdOrderByAnneeEntreeDesc(schoolId);
        Promotion promotion = promotions.stream()
                .max(Comparator.comparingInt(p -> toRangInt(p.getRang())))
                .orElseThrow(() -> new ResourceNotFoundException("Aucune promotion trouvée pour l'école " + schoolId));
        return mapper.toDto(promotion);
    }

    @Transactional
    public PromotionDto createPromotion(String schoolId, PromotionDto dto) {
        School school = schoolService.getSchoolEntityById(schoolId);

        if (promotionRepository.findFirstBySchoolIdAndRang(schoolId, dto.getRang()).isPresent()) {
            throw new BadRequestException("La '" + dto.getRang() + "' existe déjà dans cette école");
        }

        Promotion promotion = Promotion.builder()
                .school(school)
                .rang(dto.getRang())
                .nom(dto.getNom())
                .anneeEntree(dto.getAnneeEntree())
                .anneeSortie(dto.getAnneeSortie())
                .cycleSpecial(dto.isCycleSpecial())
                .cycleSpecialInfo(dto.getCycleSpecialInfo())
                .stagiaireCount(0)
                .majorName(dto.getMajorName())
                .majorScore(dto.getMajorScore())
                .build();

        promotion = promotionRepository.save(promotion);
        return mapper.toDto(promotion);
    }

    @Transactional
    public PromotionDto updatePromotion(String schoolId, String rang, PromotionDto dto) {
        Promotion promotion = getPromotionEntityBySchoolAndRangNumber(schoolId, rang);

        if (!rang.equals(dto.getRang()) &&
                promotionRepository.findFirstBySchoolIdAndRang(schoolId, dto.getRang()).isPresent()) {
            throw new BadRequestException("La '" + dto.getRang() + "' existe déjà dans cette école");
        }

        promotion.setRang(dto.getRang());
        promotion.setNom(dto.getNom());
        promotion.setAnneeEntree(dto.getAnneeEntree());
        promotion.setAnneeSortie(dto.getAnneeSortie());
        promotion.setCycleSpecial(dto.isCycleSpecial());
        promotion.setCycleSpecialInfo(dto.getCycleSpecialInfo());
        promotion.setMajorName(dto.getMajorName());
        promotion.setMajorScore(dto.getMajorScore());

        promotion = promotionRepository.save(promotion);
        return mapper.toDto(promotion);
    }

    @Transactional
    public void deletePromotion(String schoolId, String rang) {
        Promotion promotion = getPromotionEntityBySchoolAndRangNumber(schoolId, rang);

        long stagiaireCount = stagiaireRepository.countByPromotionId(promotion.getId());
        if (stagiaireCount > 0) {
            throw new BadRequestException(
                    "Impossible de supprimer la '" + rang + "' car elle contient " + stagiaireCount + " stagiaire(s)");
        }

        promotionRepository.delete(promotion);
    }

    private int toRangInt(String rang) {
        Matcher m = Pattern.compile("(\\d+)").matcher(rang);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
