package com.emia.archives.service;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.request.CreatePersonnelRequest;
import com.emia.archives.dto.response.MemberPromotionDetail;
import com.emia.archives.entity.Personnel;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.School;
import com.emia.archives.enums.PersonnelCategorie;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.PersonnelRepository;
import com.emia.archives.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final PromotionRepository promotionRepository;
    private final SchoolService schoolService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<PersonnelDto> getAll(String schoolId, PersonnelCategorie categorie) {
        schoolService.getSchoolEntityById(schoolId);
        return personnelRepository.findBySchoolIdAndCategorieOrderByNomCompletAsc(schoolId, categorie).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonnelDto getById(Long id, String schoolId) {
        Personnel personnel = personnelRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));
        return mapper.toDto(personnel);
    }

    @Transactional(readOnly = true)
    public MemberPromotionDetail getMemberWithPromotions(Long id, String schoolId) {
        Personnel personnel = personnelRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));

        LocalDate debut = personnel.getDateDebutService() != null
                ? personnel.getDateDebutService() : LocalDate.of(1900, 1, 1);
        LocalDate fin = personnel.getDateFinService() != null
                ? personnel.getDateFinService() : LocalDate.of(2100, 12, 31);

        List<Promotion> promotions = promotionRepository.findBySchoolIdAndAnneeOverlap(schoolId, debut, fin);

        List<MemberPromotionDetail.PromotionDetail> promoDetails = promotions.stream()
                .map(p -> MemberPromotionDetail.PromotionDetail.builder()
                        .rang(p.getRang())
                        .nom(p.getNom())
                        .anneeEntree(p.getAnneeEntree())
                        .anneeSortie(p.getAnneeSortie())
                        .stagiaireCount(p.getStagiaireCount() != null ? p.getStagiaireCount() : 0)
                        .majorName(p.getMajorName())
                        .build())
                .collect(Collectors.toList());

        return MemberPromotionDetail.builder()
                .member(mapper.toDto(personnel))
                .promotions(promoDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PersonnelDto> getDeComEmia(Long deId, String schoolId) {
        Personnel de = personnelRepository.findByIdAndSchoolId(deId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel DE", "id", deId));

        LocalDate debut = de.getDateDebutService() != null ? de.getDateDebutService() : LocalDate.of(1900, 1, 1);
        LocalDate fin = de.getDateFinService() != null ? de.getDateFinService() : LocalDate.of(2100, 12, 31);

        return personnelRepository.findOverlappingService(schoolId, PersonnelCategorie.COM_EMIA, debut, fin).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public PersonnelDto create(String schoolId, CreatePersonnelRequest request, PersonnelCategorie categorie) {
        School school = schoolService.getSchoolEntityById(schoolId);
        request.setCategorie(categorie.name());
        request.setSchoolId(schoolId);
        Personnel personnel = mapper.toEntity(request, school, categorie);
        personnel = personnelRepository.save(personnel);
        return mapper.toDto(personnel);
    }

    @Transactional
    public PersonnelDto update(Long id, String schoolId, CreatePersonnelRequest request) {
        Personnel personnel = personnelRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));

        personnel.setNomComplet(request.getNomComplet());
        personnel.setDateDebutService(request.getDateDebutService());
        personnel.setDateFinService(request.getDateFinService());
        personnel.setMatiereEnseignee(request.getMatiereEnseignee());

        if (request.getGrade() != null) {
            try {
                personnel.setGrade(com.emia.archives.enums.GradeEnum.valueOf(request.getGrade()));
            } catch (IllegalArgumentException e) {
                personnel.setGrade(null);
            }
        } else {
            personnel.setGrade(null);
        }

        personnel = personnelRepository.save(personnel);
        return mapper.toDto(personnel);
    }

    @Transactional
    public void delete(Long id, String schoolId) {
        Personnel personnel = personnelRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));
        personnelRepository.delete(personnel);
    }
}
