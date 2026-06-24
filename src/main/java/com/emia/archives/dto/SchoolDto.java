package com.emia.archives.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SchoolDto {

    private String id;
    private String name;
    private String acronym;
    private String type;
    private String motto;
    private String logoUrl;
}
