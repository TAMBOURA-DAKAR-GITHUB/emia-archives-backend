package com.emia.archives.dto;

import java.util.List;

public record BulletinData(
        String pays,
        List<String> notes,
        List<String> coefficients,
        List<String> observations,
        String moyenne1,
        String moyenne2,
        String moyenne3,
        String classementRang,
        String classementTotal,
        String appreciation
) {
}
