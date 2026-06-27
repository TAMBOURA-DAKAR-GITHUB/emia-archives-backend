package com.emia.archives.service;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.enums.PersonnelCategorie;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final StagiaireService stagiaireService;
    private final PersonnelService personnelService;

    public byte[] exportStagiairesExcel(String schoolId, String rangPromotion) throws IOException {
        List<StagiaireDto> stagiaires = stagiaireService.getStagiairesByPromotion(schoolId, rangPromotion);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Stagiaires");
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"Rang", "Prénoms & Noms", "Matricule", "Moyenne", "Grade", "Cycle", "Statut",
                    "Informations Supplémentaires", "Observations", "Décédé"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (StagiaireDto s : stagiaires) {
                Row row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue(s.getRang() != null ? s.getRang().doubleValue() : (double) rowNum);
                rowNum++;
                row.createCell(1).setCellValue(s.getNomComplet() != null ? s.getNomComplet() : "");
                row.createCell(2).setCellValue(s.getMatricule() != null ? s.getMatricule() : "");
                row.createCell(3).setCellValue(s.getMoyenne() != null ? s.getMoyenne().doubleValue() : 0);
                row.createCell(4).setCellValue(s.getGrade() != null ? s.getGrade() : "");
                row.createCell(5).setCellValue(s.getCycleType() != null ? s.getCycleType() : "");
                row.createCell(6).setCellValue(s.getStatus() != null ? s.getStatus() : "");
                row.createCell(7).setCellValue(s.getInformationsSupplementaires() != null ? s.getInformationsSupplementaires() : "");
                row.createCell(8).setCellValue(s.getObservations() != null ? s.getObservations() : "");
                row.createCell(9).setCellValue(s.isEstDecede() ? "Oui" : "Non");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportPersonnelExcel(String schoolId, PersonnelCategorie categorie) throws IOException {
        List<PersonnelDto> personnel = personnelService.getAll(schoolId, categorie);

        try (Workbook wb = new XSSFWorkbook()) {
            String sheetName = categorie == PersonnelCategorie.COM_EMIA ? "COM EMIA" : "DE";
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"Prénoms & Noms", "Grade", "Date Début Service", "Date Fin Service"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (PersonnelDto p : personnel) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getNomComplet() != null ? p.getNomComplet() : "");
                row.createCell(1).setCellValue(p.getGrade() != null ? p.getGrade() : "");
                row.createCell(2).setCellValue(p.getDateDebutService() != null ? p.getDateDebutService().toString() : "");
                row.createCell(3).setCellValue(p.getDateFinService() != null ? p.getDateFinService().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
