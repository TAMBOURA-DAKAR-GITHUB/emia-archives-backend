package com.emia.archives.service;

import com.emia.archives.dto.response.SearchResultDto;
import com.emia.archives.entity.Personnel;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.enums.PersonnelCategorie;
import com.emia.archives.repository.PersonnelRepository;
import com.emia.archives.repository.PromotionRepository;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final StagiaireRepository stagiaireRepository;
    private final PromotionRepository promotionRepository;
    private final PersonnelRepository personnelRepository;

    public List<SearchResultDto> search(String schoolId, String q) {
        List<SearchResultDto> results = new ArrayList<>();

        String query = q != null ? q.trim() : "";
        if (query.isEmpty()) return results;

        // Search stagiaires
        List<Stagiaire> stagiaires = stagiaireRepository.searchBySchoolId(schoolId, query);
        for (Stagiaire s : stagiaires) {
            results.add(SearchResultDto.builder()
                    .type("STAGIAIRE")
                    .id(String.valueOf(s.getId()))
                    .label(s.getNomComplet())
                    .subtitle("Promotion " + s.getPromotion().getRang())
                    .build());
        }

        // Search promotions
        List<Promotion> promotions = promotionRepository.searchBySchoolId(schoolId, query);
        for (Promotion p : promotions) {
            results.add(SearchResultDto.builder()
                    .type("PROMOTION")
                    .id(p.getRang())
                    .label(p.getRang() + " — " + p.getNom())
                    .subtitle(p.getAnneeEntree() != null ? p.getAnneeEntree().toString() : "")
                    .build());
        }

        // Search COM EMIA
        List<Personnel> comEmia = personnelRepository.searchBySchoolIdAndCategorie(schoolId, PersonnelCategorie.COM_EMIA, query);
        for (Personnel p : comEmia) {
            results.add(SearchResultDto.builder()
                    .type("COM_EMIA")
                    .id(String.valueOf(p.getId()))
                    .label(p.getNomComplet())
                    .subtitle(p.getGrade() != null ? p.getGrade().name() : "")
                    .build());
        }

        // Search DE
        List<Personnel> de = personnelRepository.searchBySchoolIdAndCategorie(schoolId, PersonnelCategorie.DE, query);
        for (Personnel p : de) {
            results.add(SearchResultDto.builder()
                    .type("DE")
                    .id(String.valueOf(p.getId()))
                    .label(p.getNomComplet())
                    .subtitle(p.getGrade() != null ? p.getGrade().name() : "")
                    .build());
        }

        return results;
    }
}
