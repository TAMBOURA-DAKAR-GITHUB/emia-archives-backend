package com.emia.archives.service;

import com.emia.archives.dto.PromotionDto;
import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.dto.request.CreateStagiaireRequest;
import com.emia.archives.exception.BadRequestException;
import com.emia.archives.enums.StagiaireStatus;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StagiaireService stagiaireService;
    private final PromotionService promotionService;

    public List<StagiaireDto> importStagiaires(String schoolId, MultipartFile file) throws IOException, BadRequestException {
        List<CreateStagiaireRequest> requests = parseExcelFile(file);
        if (requests.isEmpty()) {
            throw new BadRequestException("Aucune ligne valide trouvée dans le fichier. Vérifiez le format (Prénoms & Noms, Promotion, Type de Cycle, Moyenne, Décédé, Infos Supp., Observations, Date de Naissance, Lieu de Naissance).");
        }
        List<StagiaireDto> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            CreateStagiaireRequest request = requests.get(i);
            request.setSchoolId(schoolId);
            try {
                StagiaireDto dto = stagiaireService.createStagiaire(schoolId, request);
                result.add(dto);
            } catch (Exception e) {
                errors.add("Ligne " + (i + 2) + " : " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            String summary = result.isEmpty()
                    ? "Échec de l'import :\n" + String.join("\n", errors)
                    : result.size() + " stagiaire(s) importé(s) avec " + errors.size() + " erreur(s) :\n" + String.join("\n", errors);
            throw new BadRequestException(summary);
        }
        return result;
    }

    public List<PromotionDto> importPromotions(String schoolId, MultipartFile file) throws IOException {
        List<PromotionDto> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() < 1) {
                throw new BadRequestException("Le fichier est vide. Vérifiez le format (Rang, Nom, Année Entrée, Année Sortie, Cycle Spécial, Détails).");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Double rangNumber = getCellNumericValue(row.getCell(0));
                    if (rangNumber == null) {
                        errors.add("Ligne " + (i + 1) + " : rang manquant, ligne ignorée");
                        continue;
                    }

                    String rangString = rangNumber.intValue() == 1
                            ? "1ère Promotion"
                            : rangNumber.intValue() + "ième Promotion";

                    String nom = getCellStringValue(row.getCell(1));
                    if (nom == null || nom.isBlank()) {
                        nom = rangString;
                    }

                    LocalDate anneeEntree = getCellDateOrYearValue(row.getCell(2));
                    LocalDate anneeSortie = getCellDateOrYearValue(row.getCell(3));

                    String cycleSpecialText = getCellStringValue(row.getCell(4));
                    boolean cycleSpecial = "OUI".equalsIgnoreCase(cycleSpecialText) ||
                            "TRUE".equalsIgnoreCase(cycleSpecialText) || "1".equals(cycleSpecialText);
                    String cycleSpecialInfo = getCellStringValue(row.getCell(5));

                    PromotionDto dto = PromotionDto.builder()
                            .rang(rangString)
                            .nom(nom.trim())
                            .anneeEntree(anneeEntree)
                            .anneeSortie(anneeSortie)
                            .cycleSpecial(cycleSpecial)
                            .cycleSpecialInfo(cycleSpecialInfo != null ? cycleSpecialInfo.trim() : null)
                            .stagiaireCount(0)
                            .majorScore(0)
                            .build();

                    PromotionDto created = promotionService.createPromotion(schoolId, dto);
                    result.add(created);
                } catch (Exception e) {
                    errors.add("Ligne " + (i + 1) + " : " + e.getMessage());
                }
            }
        }

        if (!errors.isEmpty()) {
            String summary = result.isEmpty()
                    ? "Échec de l'import :\n" + String.join("\n", errors)
                    : result.size() + " promotion(s) importée(s) avec " + errors.size() + " erreur(s) :\n" + String.join("\n", errors);
            throw new BadRequestException(summary);
        }

        return result;
    }

    private List<CreateStagiaireRequest> parseExcelFile(MultipartFile file) throws IOException {
        List<CreateStagiaireRequest> requests = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    CreateStagiaireRequest request = new CreateStagiaireRequest();
                    request.setNomComplet(getCellStringValue(row.getCell(0)));
                    request.setPromotion(toFullRang(getCellStringValue(row.getCell(1))));
                    String cycleTypeStr = getCellStringValue(row.getCell(2));
                    request.setCycleType(cycleTypeStr != null ? cycleTypeStr.toUpperCase() : "ORDINAIRE");
                    Double cellVal = getCellNumericValue(row.getCell(3));
                    request.setMoyenne(cellVal != null ? BigDecimal.valueOf(cellVal) : null);
                    String decedeText = getCellStringValue(row.getCell(4));
                    request.setEstDecede("OUI".equalsIgnoreCase(decedeText) || "TRUE".equalsIgnoreCase(decedeText) || "1".equals(decedeText));
                    request.setInformationsSupplementaires(getCellStringValue(row.getCell(5)));
                    request.setObservations(getCellStringValue(row.getCell(6)));
                    request.setDateNaissance(getCellDateValue(row.getCell(7)));
                    request.setLieuNaissance(getCellStringValue(row.getCell(8)));
                    request.setStatus(StagiaireStatus.EN_COURS.name());

                    if (request.getNomComplet() != null && !request.getNomComplet().isBlank()) {
                        requests.add(request);
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                }
            }
        }

        return requests;
    }

    private String toFullRang(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        // Extract the numeric part from any format: "1", "1ière", "9ème", "9ième Promotion", etc.
        Matcher matcher = Pattern.compile("(\\d+)").matcher(trimmed);
        if (matcher.find()) {
            int num = Integer.parseInt(matcher.group(1));
            return num == 1 ? "1ère Promotion" : num + "ème Promotion";
        }
        return trimmed;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Double getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = getCellStringValue(cell);
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE)) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private LocalDate getCellDateOrYearValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            Double num = cell.getNumericCellValue();
            if (num != null && num >= 1900 && num <= 2100 && num == Math.floor(num)) {
                return LocalDate.of(num.intValue(), 1, 1);
            }
        }
        LocalDate parsed = getCellDateValue(cell);
        if (parsed != null) return parsed;
        Double num = getCellNumericValue(cell);
        if (num != null && num >= 1900 && num <= 2100) {
            return LocalDate.of(num.intValue(), 1, 1);
        }
        return null;
    }

    private Integer getCellIntValue(Cell cell) {
        Double val = getCellNumericValue(cell);
        return val != null ? val.intValue() : null;
    }
}
