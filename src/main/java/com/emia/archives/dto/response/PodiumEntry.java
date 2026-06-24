package com.emia.archives.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PodiumEntry {

    private int rank;
    private String name;
    private String grade;
    private String matricule;
    private BigDecimal average;
}
