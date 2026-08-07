package com.emia.archives.service;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.StagiaireDto;
import com.emia.archives.entity.School;
import com.emia.archives.enums.PersonnelCategorie;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final StagiaireService stagiaireService;
    private final PersonnelService personnelService;
    private final SchoolService schoolService;

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
        School school = schoolService.getSchoolEntityById(schoolId);

        String sheetName = switch (categorie) {
            case COM_EMIA -> "COM EMIA";
            case DE -> "DE";
            case IP -> "IP";
        };
        String categoryLabel = switch (categorie) {
            case COM_EMIA -> "Liste des COM EMIA";
            case DE -> "Liste des Directeurs des Études";
            case IP -> "Liste des Instructeurs Permanents";
        };
        boolean includeMatiere = categorie == PersonnelCategorie.IP;
        String[] headers = includeMatiere
                ? new String[]{"Prénoms & Noms", "Grade", "Matière Enseignée", "Date Début Service", "Date Fin Service"}
                : new String[]{"Prénoms & Noms", "Grade", "Date Début Service", "Date Fin Service"};
        int lastCol = headers.length - 1;

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createPersonnelHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle altStyle = createDataStyle(wb);
            altStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowNum = 0;

            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(22);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(school.getName() != null ? school.getName().toUpperCase() : "");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

            Row subRow = sheet.createRow(rowNum++);
            subRow.setHeightInPoints(18);
            Cell subCell = subRow.createCell(0);
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            subCell.setCellValue((school.getAcronym() != null ? school.getAcronym().toUpperCase() : "")
                    + " — " + categoryLabel + " — Édité le " + date);
            subCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastCol));

            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int idx = 0;
            for (PersonnelDto p : personnel) {
                Row row = sheet.createRow(rowNum++);
                CellStyle style = (idx++ % 2 == 0) ? dataStyle : altStyle;
                row.createCell(0).setCellValue(p.getNomComplet() != null ? p.getNomComplet() : "");
                row.createCell(1).setCellValue(p.getGrade() != null ? p.getGrade() : "");
                int c = 2;
                if (includeMatiere) row.createCell(c++).setCellValue(p.getMatiereEnseignee() != null ? p.getMatiereEnseignee() : "");
                row.createCell(c++).setCellValue(p.getDateDebutService() != null ? p.getDateDebutService().toString() : "");
                row.createCell(c).setCellValue(p.getDateFinService() != null ? p.getDateFinService().toString() : "");
                for (int col = 0; col < headers.length; col++) row.getCell(col).setCellStyle(style);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportPersonnelPdf(String schoolId, PersonnelCategorie categorie) throws Exception {
        List<PersonnelDto> personnel = personnelService.getAll(schoolId, categorie);
        School school = schoolService.getSchoolEntityById(schoolId);

        String categoryLabel = switch (categorie) {
            case COM_EMIA -> "Liste des COM EMIA";
            case DE -> "Liste des Directeurs des Études";
            case IP -> "Liste des Instructeurs Permanents";
        };
        boolean includeMatiere = categorie == PersonnelCategorie.IP;
        String schoolName = school.getName() != null ? school.getName().toUpperCase() : "";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        PdfTable table = new PdfTable(schoolName, categoryLabel, date, includeMatiere);
        int idx = 0;
        for (PersonnelDto p : personnel) {
            List<String> cells = new ArrayList<>();
            cells.add(p.getNomComplet() != null ? p.getNomComplet() : "");
            cells.add(p.getGrade() != null ? p.getGrade() : "");
            if (includeMatiere) cells.add(p.getMatiereEnseignee() != null ? p.getMatiereEnseignee() : "");
            cells.add(p.getDateDebutService() != null ? p.getDateDebutService().toString() : "");
            cells.add(p.getDateFinService() != null ? p.getDateFinService().toString() : "");
            table.addRow(cells, idx++ % 2 == 1);
        }
        return table.finish();
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createPersonnelHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
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

    private static final class PdfTable {

        private static final float MARGIN = 36f;
        private static final float HEADER_ROW_H = 24f;
        private static final float ROW_H = 20f;
        private static final PDFont FONT = PDType1Font.HELVETICA;
        private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
        private static final Color DARK_BLUE = new Color(26, 42, 74);
        private static final Color LIGHT_GREY = new Color(240, 240, 240);
        private static final Color BORDER_GREY = new Color(170, 170, 170);

        private final String schoolName;
        private final String title;
        private final String date;
        private final String[] headers;
        private final float[] widths;
        private final float pageW;
        private final float pageH;

        private PDDocument document;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        PdfTable(String schoolName, String title, String date, boolean includeMatiere) {
            this.schoolName = schoolName;
            this.title = title;
            this.date = date;
            this.headers = includeMatiere
                    ? new String[]{"Prénoms & Noms", "Grade", "Matière Enseignée", "Date Début Service", "Date Fin Service"}
                    : new String[]{"Prénoms & Noms", "Grade", "Date Début Service", "Date Fin Service"};
            this.pageW = PDRectangle.A4.getHeight();
            this.pageH = PDRectangle.A4.getWidth();
            float tableW = pageW - 2 * MARGIN;
            float[] base = includeMatiere
                    ? new float[]{300, 140, 180, 110, 110}
                    : new float[]{340, 160, 170, 170};
            float sum = 0;
            for (float w : base) sum += w;
            this.widths = new float[base.length];
            for (int i = 0; i < base.length; i++) widths[i] = base[i] * tableW / sum;
        }

        byte[] finish() throws Exception {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (cs != null) cs.close();
                document.save(out);
                return out.toByteArray();
            }
        }

        void addRow(List<String> cells, boolean alternate) throws Exception {
            if (y - ROW_H < MARGIN) newPage();
            float rowTop = y;
            if (alternate) {
                cs.setNonStrokingColor(LIGHT_GREY);
                cs.addRect(MARGIN, rowTop - ROW_H, pageW - 2 * MARGIN, ROW_H);
                cs.fill();
            }
            drawCellTexts(cells, rowTop, ROW_H, FONT, 10, Color.BLACK);
            drawRowLine(rowTop - ROW_H);
            y = rowTop - ROW_H;
        }

        private void newPage() throws Exception {
            if (cs != null) cs.close();
            if (document == null) document = new PDDocument();
            page = new PDPage(new PDRectangle(pageW, pageH));
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            y = pageH - MARGIN;
            drawSchoolHeader();
            drawHeaderRow();
        }

        private void drawSchoolHeader() throws Exception {
            cs.setStrokingColor(DARK_BLUE);
            cs.setLineWidth(1.5f);

            cs.setFont(FONT_BOLD, 16);
            cs.setNonStrokingColor(Color.BLACK);
            drawCentered(schoolName, y, FONT_BOLD, 16);
            y -= 22;

            cs.setFont(FONT_BOLD, 12);
            cs.setNonStrokingColor(DARK_BLUE);
            drawCentered(title, y, FONT_BOLD, 12);
            y -= 18;

            cs.setFont(FONT, 9);
            cs.setNonStrokingColor(Color.DARK_GRAY);
            drawCentered("Édité le " + date, y, FONT, 9);
            y -= 14;

            cs.moveTo(MARGIN, y);
            cs.lineTo(pageW - MARGIN, y);
            cs.stroke();
            y -= 12;
        }

        private void drawHeaderRow() throws Exception {
            float rowTop = y;
            cs.setNonStrokingColor(DARK_BLUE);
            cs.addRect(MARGIN, rowTop - HEADER_ROW_H, pageW - 2 * MARGIN, HEADER_ROW_H);
            cs.fill();
            drawCellTexts(List.of(headers), rowTop, HEADER_ROW_H, FONT_BOLD, 10, Color.WHITE);
            drawRowLine(rowTop - HEADER_ROW_H);
            y = rowTop - HEADER_ROW_H;
        }

        private void drawCellTexts(List<String> cells, float rowTop, float rowH, PDFont font, float size, Color color) throws Exception {
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            float x = MARGIN;
            float baseline = rowTop - (rowH - size) / 2 - 2;
            for (int i = 0; i < cells.size(); i++) {
                String text = sanitize(cells.get(i));
                if (i == 0) {
                    cs.beginText();
                    cs.newLineAtOffset(x + 4, baseline);
                    cs.showText(text);
                    cs.endText();
                } else {
                    float cw = widths[i];
                    float textW = font.getStringWidth(text) / 1000 * size;
                    float tx = x + cw / 2 - textW / 2;
                    cs.beginText();
                    cs.newLineAtOffset(tx, baseline);
                    cs.showText(text);
                    cs.endText();
                }
                x += widths[i];
            }
        }

        private void drawRowLine(float lineY) throws Exception {
            cs.setStrokingColor(BORDER_GREY);
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, lineY);
            cs.lineTo(pageW - MARGIN, lineY);
            cs.stroke();
        }

        private void drawCentered(String text, float baseline, PDFont font, float size) throws Exception {
            float textW = font.getStringWidth(text) / 1000 * size;
            cs.beginText();
            cs.newLineAtOffset(pageW / 2 - textW / 2, baseline);
            cs.showText(text);
            cs.endText();
        }

        private String sanitize(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\u2019', '\u2018' -> sb.append('\'');
                    case '\u201C', '\u201D' -> sb.append('"');
                    case '\u2013', '\u2014' -> sb.append('-');
                    default -> {
                        if (c >= 0x20 && c <= 0xFF) sb.append(c);
                    }
                }
            }
            return sb.toString();
        }
    }
}
