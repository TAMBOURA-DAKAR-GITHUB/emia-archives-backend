package com.emia.archives.service;

import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.dto.request.CreateStagiaireRequest;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.School;
import com.emia.archives.entity.Stagiaire;
import java.math.BigDecimal;
import com.emia.archives.enums.CycleType;
import com.emia.archives.enums.GradeEnum;
import com.emia.archives.enums.StagiaireStatus;
import com.emia.archives.exception.BadRequestException;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final PromotionService promotionService;
    private final SchoolService schoolService;
    private final EntityMapper mapper;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<StagiaireDto> getAllStagiaires() {
        return stagiaireRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StagiaireDto> getStagiairesByPromotion(String schoolId, String rang) {
        schoolService.getSchoolEntityById(schoolId);
        return stagiaireRepository.findBySchoolIdAndPromotionRangOrderByRangAsc(schoolId, rang).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public StagiaireDto getStagiaireById(Long id) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", id));
        return mapper.toDto(stagiaire);
    }

    @Transactional(readOnly = true)
    public List<StagiaireDto> getStagiairesWithoutPromotion(String schoolId) {
        schoolService.getSchoolEntityById(schoolId);
        return stagiaireRepository.findBySchoolIdAndPromotionIsNull(schoolId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public StagiaireDto createStagiaire(String schoolId, CreateStagiaireRequest request) {
        School school = schoolService.getSchoolEntityById(schoolId);

        Promotion promotion = null;
        if (!isCycleStagiaire(request.getCycleType())) {
            if (request.getPromotion() == null || request.getPromotion().isBlank()) {
                throw new BadRequestException("La promotion est requise");
            }
            promotion = promotionService.getPromotionEntityBySchoolAndRangNumber(schoolId, request.getPromotion());
        }

        if (request.getMatricule() != null && stagiaireRepository.findByMatricule(request.getMatricule()).isPresent()) {
            throw new BadRequestException("Un stagiaire avec le matricule " + request.getMatricule() + " existe déjà");
        }

        Stagiaire stagiaire = Stagiaire.builder()
                .nomComplet(request.getNomComplet())
                .matricule(request.getMatricule())
                .numeroEoa(nextNumeroEoa())
                .promotion(promotion)
                .school(school)
                .moyenne(request.getMoyenne())
                .dateNaissance(request.getDateNaissance())
                .lieuNaissance(request.getLieuNaissance())
                .rang(null)
                .grade(request.getGrade() != null ? GradeEnum.valueOf(request.getGrade()) : null)
                .cycleType(request.getCycleType() != null ? CycleType.valueOf(request.getCycleType()) : null)
                .status(request.getStatus() != null ? StagiaireStatus.valueOf(request.getStatus()) : StagiaireStatus.EN_COURS)
                .observations(request.getObservations())
                .informationsSupplementaires(request.getInformationsSupplementaires())
                .estDecede(request.getEstDecede() != null && request.getEstDecede())
                .photoUrl(request.getPhotoUrl())
                .build();

        stagiaire = stagiaireRepository.save(stagiaire);
        if (promotion != null) {
            recalculateRangs(promotion.getId());
            updatePromotionStats(promotion);
        }
        return mapper.toDto(stagiaire);
    }

    @Transactional
    public StagiaireDto updateStagiaire(Long id, CreateStagiaireRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", id));

        Promotion oldPromotion = stagiaire.getPromotion();

        stagiaire.setNomComplet(request.getNomComplet());
        stagiaire.setMatricule(request.getMatricule());
        stagiaire.setMoyenne(request.getMoyenne());
        stagiaire.setDateNaissance(request.getDateNaissance());
        stagiaire.setLieuNaissance(request.getLieuNaissance());
        stagiaire.setRang(null);
        stagiaire.setGrade(request.getGrade() != null ? GradeEnum.valueOf(request.getGrade()) : null);
        stagiaire.setCycleType(request.getCycleType() != null ? CycleType.valueOf(request.getCycleType()) : null);
        stagiaire.setStatus(request.getStatus() != null ? StagiaireStatus.valueOf(request.getStatus()) : StagiaireStatus.EN_COURS);
        stagiaire.setObservations(request.getObservations());
        stagiaire.setInformationsSupplementaires(request.getInformationsSupplementaires());
        stagiaire.setEstDecede(request.getEstDecede() != null && request.getEstDecede());
        stagiaire.setPhotoUrl(request.getPhotoUrl() != null ? request.getPhotoUrl() : stagiaire.getPhotoUrl());

        if (isCycleStagiaire(request.getCycleType())) {
            stagiaire.setPromotion(null);
        } else {
            if (request.getPromotion() == null || request.getPromotion().isBlank()) {
                throw new BadRequestException("La promotion est requise");
            }
            Promotion newPromotion = promotionService.getPromotionEntityBySchoolAndRangNumber(
                    request.getSchoolId(), request.getPromotion());
            if (oldPromotion == null || !oldPromotion.getId().equals(newPromotion.getId())) {
                stagiaire.setPromotion(newPromotion);
            }
        }

        stagiaire = stagiaireRepository.save(stagiaire);
        if (oldPromotion != null) {
            recalculateRangs(oldPromotion.getId());
            updatePromotionStats(oldPromotion);
        }
        Promotion newPromotion = stagiaire.getPromotion();
        if (newPromotion != null && (oldPromotion == null || !oldPromotion.getId().equals(newPromotion.getId()))) {
            recalculateRangs(newPromotion.getId());
            updatePromotionStats(newPromotion);
        }
        return mapper.toDto(stagiaire);
    }

    private boolean isCycleStagiaire(String cycleType) {
        if (cycleType == null) return false;
        try {
            return CycleType.READAPTATION == CycleType.valueOf(cycleType)
                    || CycleType.ADAPTATION == CycleType.valueOf(cycleType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional
    public StagiaireDto uploadPhoto(Long id, MultipartFile file) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", id));

        String oldPhoto = stagiaire.getPhotoUrl();
        String newFilename = fileStorageService.saveFile(id, file);
        stagiaire.setPhotoUrl(newFilename);
        stagiaire = stagiaireRepository.save(stagiaire);

        if (oldPhoto != null) {
            fileStorageService.deleteFile(oldPhoto);
        }

        return mapper.toDto(stagiaire);
    }

    @Transactional
    public void deleteStagiaire(Long id) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", id));
        if (stagiaire.getPhotoUrl() != null) {
            fileStorageService.deleteFile(stagiaire.getPhotoUrl());
        }
        Promotion promotion = stagiaire.getPromotion();
        stagiaireRepository.delete(stagiaire);
        if (promotion != null) {
            recalculateRangs(promotion.getId());
            updatePromotionStats(promotion);
        }
    }

    private int nextNumeroEoa() {
        return stagiaireRepository.findMaxNumeroEoa() + 1;
    }

    private void updatePromotionStats(Promotion promotion) {
        long count = stagiaireRepository.countByPromotionId(promotion.getId());
        promotion.setStagiaireCount((int) count);

        boolean hasSpecial = stagiaireRepository.findByPromotionIdOrderByRangAsc(promotion.getId())
                .stream().anyMatch(s -> s.getCycleType() == CycleType.SPECIAL);
        promotion.setCycleSpecial(hasSpecial);

        Optional<Stagiaire> major = stagiaireRepository.findTop5ByPromotionId(promotion.getId())
                .stream().findFirst();
        if (major.isPresent()) {
            promotion.setMajorName(major.get().getNomComplet());
            BigDecimal moy = major.get().getMoyenne();
            promotion.setMajorScore(moy != null ? moy.doubleValue() : null);
        }
    }

    public List<Stagiaire> getTop5ByPromotion(Long promotionId) {
        return stagiaireRepository.findTop5ByPromotionId(promotionId);
    }

    @Transactional
    public void recalculateRangs(Long promotionId) {
        List<Stagiaire> stagiaires = stagiaireRepository.findAllByPromotionIdOrderByMoyenneDesc(promotionId);

        int currentRank = 0;
        BigDecimal prevMoyenne = null;

        for (int i = 0; i < stagiaires.size(); i++) {
            Stagiaire s = stagiaires.get(i);
            if (s.getMoyenne() == null) {
                s.setRang(null);
            } else if (prevMoyenne != null && s.getMoyenne().compareTo(prevMoyenne) == 0) {
                s.setRang(currentRank);
            } else {
                currentRank = i + 1;
                s.setRang(currentRank);
            }
            prevMoyenne = s.getMoyenne();
        }
        stagiaireRepository.saveAll(stagiaires);
    }
}
