package com.emia.archives.dto.response;

import com.emia.archives.dto.PersonnelDto;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MemberPromotionDetail {

    private PersonnelDto member;
    private List<PromotionDetail> promotions;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class PromotionDetail {
        private String rang;
        private String nom;
        private int anneeEntree;
        private int anneeSortie;
        private int stagiaireCount;
        private String majorName;
    }
}
