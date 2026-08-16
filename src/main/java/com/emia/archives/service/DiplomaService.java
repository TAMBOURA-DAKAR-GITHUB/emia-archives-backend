package com.emia.archives.service;

import com.emia.archives.entity.Stagiaire;
import com.emia.archives.enums.CycleType;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DiplomaService {

    private static final String TEMPLATE_ORDINAIRE = "static/images/ordinaire.png";
    private static final String TEMPLATE_SPECIAL = "static/images/cycleSpecial.png";

    private static final int TEMPLATE_REF_WIDTH = 19844;
    private static final int TEMPLATE_REF_HEIGHT = 14034;

    private static final String FONT_FAMILY = "Arial";
    private static final Color TEXT_COLOR = new Color(0x16, 0x16, 0x16);
    private static final Color RED_COLOR = new Color(0xFF, 0x00, 0x00);
    private static final int NAME_OFFSET_X = 50;
    private static final float FONT_SIZE_RATIO = 1.28f;

    private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    private final StagiaireRepository stagiaireRepository;

    private final Map<String, TemplateLayout> layoutCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public byte[] generateDiploma(String schoolId, Long stagiaireId) throws Exception {
        BufferedImage image = generateDiplomaImage(schoolId, stagiaireId);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfBuilder.addPage(document, image);
            image.flush();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateDiplomasPdf(String schoolId, List<Long> stagiaireIds) throws Exception {
        if (stagiaireIds == null || stagiaireIds.isEmpty()) {
            throw new IllegalArgumentException("Aucun stagiaire sélectionné");
        }
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Long id : stagiaireIds) {
                BufferedImage image = generateDiplomaImage(schoolId, id);
                PdfBuilder.addPage(document, image);
                image.flush();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private BufferedImage generateDiplomaImage(String schoolId, Long stagiaireId) throws Exception {
        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", stagiaireId));
        if (!stagiaire.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Stagiaire", "schoolId", schoolId);
        }

        TemplateLayout layout = layoutFor(stagiaire.getCycleType());
        return layout.render(stagiaire.getNomComplet(), stagiaire.getNumeroEoa(),
                promotionText(stagiaire.getPromotion()),
                mentionText(stagiaire.getMoyenne()), birthText(stagiaire.getDateNaissance(), stagiaire.getLieuNaissance()),
                periodText(stagiaire.getPromotion()));
    }
    private static String periodText(com.emia.archives.entity.Promotion promotion) {
        if (promotion == null) return null;
        LocalDate debut = promotion.getAnneeEntree();
        LocalDate fin = promotion.getAnneeSortie();
        if (debut == null && fin == null) return null;
        String d = debut != null ? debut.format(FRENCH_DATE) : null;
        String f = fin != null ? fin.format(FRENCH_DATE) : null;
        if (d != null && f != null) return d + "\n au " + f;
        return d != null ? d : f;
    }

    private static String mentionText(BigDecimal moyenne) {
        if (moyenne == null) return null;
        if (moyenne.compareTo(new BigDecimal("16")) >= 0) return "\"TRÈS BIEN\"";
        if (moyenne.compareTo(new BigDecimal("14")) >= 0) return "\"BIEN\"";
        if (moyenne.compareTo(new BigDecimal("12")) >= 0) return "\"ASSEZ BIEN\"";
        if (moyenne.compareTo(new BigDecimal("10")) >= 0) return "\"PASSABLE\"";
        return null;
    }

    private static String birthText(LocalDate dateNaissance, String lieuNaissance) {
        StringBuilder sb = new StringBuilder();
        if (dateNaissance != null) {
            sb.append(dateNaissance.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (lieuNaissance != null && !lieuNaissance.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append("à ").append(lieuNaissance.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String promotionText(com.emia.archives.entity.Promotion promotion) {
        if (promotion == null) return "";
        String rang = promotion.getRang();
        if (rang != null && !rang.isBlank()) {
            java.util.regex.Matcher m = Pattern.compile("\\d+").matcher(rang);
            if (m.find()) return ordinal(m.group());
        }
        if (promotion.getNom() != null && !promotion.getNom().isBlank()) {
            return promotion.getNom().trim();
        }
        return rang == null ? "" : rang.trim();
    }

    private static String ordinal(String number) {
        int n = Integer.parseInt(number);
        if (n % 10 == 1 && n % 100 != 11) return n + "ère";
        return n + "ème";
    }

    private TemplateLayout layoutFor(CycleType cycleType) {
        String key = cycleType == CycleType.SPECIAL ? "special" : "ordinaire";
        return layoutCache.computeIfAbsent(key, this::buildLayout);
    }

    private TemplateLayout buildLayout(String key) {
        BufferedImage full = loadTemplate(key);
        int fullW = full.getWidth();
        int fullH = full.getHeight();

        TextSlot name = defaultNameSlot(key, fullW, fullH);
        TextSlot numero = defaultNumeroSlot(key, fullW, fullH);
        TextSlot promotion = defaultPromotionSlot(key, fullW, fullH);
        TextSlot mention = defaultMentionSlot(key, fullW, fullH);
        TextSlot birth = defaultBirthSlot(key, fullW, fullH);
        TextSlot period = defaultPeriodSlot(key, fullW, fullH);
        TextSlot period2 = defaultPeriod2Slot(key, fullW, fullH);

        BufferedImage render = downscaleIfNeeded(full);
        double sx = render.getWidth() / (double) fullW;
        double sy = render.getHeight() / (double) fullH;
        name = name.scaled(sx, sy);
        if (numero != null) {
            numero = numero.scaled(sx, sy);
            numero = new TextSlot(numero.align, numero.left - 20, numero.top, numero.right - 20, numero.bottom,
                    numero.eraseLeft, numero.eraseTop, numero.eraseRight, numero.eraseBottom,
                    numero.bold, numero.maxFontSize);
            if ("special".equals(key)) {
                numero = new TextSlot(numero.align, numero.left, numero.top + 5, numero.right, numero.bottom + 5,
                        numero.eraseLeft, numero.eraseTop + 5, numero.eraseRight, numero.eraseBottom + 5,
                        numero.bold, numero.maxFontSize);
            }
        }
        if (promotion != null) {
            promotion = promotion.scaled(sx, sy);
            promotion = new TextSlot(promotion.align, promotion.left + 40, promotion.top - 10, promotion.right, promotion.bottom - 10,
                    promotion.eraseLeft + 40, promotion.eraseTop - 10, promotion.eraseRight, promotion.eraseBottom - 10,
                    promotion.bold, promotion.maxFontSize);
        }
        mention = mention.scaled(sx, sy);
        birth = birth.scaled(sx, sy);
        birth = new TextSlot(birth.align, birth.left - 70, birth.top + 0, birth.right, birth.bottom + 15,
                birth.eraseLeft - 70, birth.eraseTop + 0, birth.eraseRight, birth.eraseBottom + 15,
                birth.bold, birth.maxFontSize - 4);
        if ("special".equals(key)) {
            birth = new TextSlot(birth.align, birth.left + 10, birth.top, birth.right, birth.bottom,
                    birth.eraseLeft + 10, birth.eraseTop, birth.eraseRight, birth.eraseBottom,
                    birth.bold, birth.maxFontSize + 5);
        } else {
            birth = new TextSlot(birth.align, birth.left, birth.top - 5, birth.right, birth.bottom - 5,
                    birth.eraseLeft, birth.eraseTop - 5, birth.eraseRight, birth.eraseBottom - 5,
                    birth.bold, birth.maxFontSize);
        }
        if (period != null) {
            period = period.scaled(sx, sy);
            period = new TextSlot(period.align, period.left + 20, period.top - 6, period.right, period.bottom - 6,
                    period.eraseLeft + 20, period.eraseTop - 6, period.eraseRight, period.eraseBottom - 6,
                    period.bold, period.maxFontSize);
            if ("special".equals(key)) {
                period = new TextSlot(period.align, period.left, period.top + 8, period.right, period.bottom + 8,
                        period.eraseLeft, period.eraseTop + 8, period.eraseRight, period.eraseBottom + 8,
                        period.bold, period.maxFontSize);
            }
        }
        if (period2 != null) {
            period2 = period2.scaled(sx, sy);
            period2 = new TextSlot(period2.align, period2.left, period2.top + 30, period2.right, period2.bottom + 30,
                    period2.eraseLeft, period2.eraseTop + 30, period2.eraseRight, period2.eraseBottom + 30,
                    period2.bold, period2.maxFontSize);
        }
        return new TemplateLayout(render, name, numero, promotion, mention, birth, period, period2);
    }

    private static int labelFontSize(int lineHeight) {
        return Math.max(12, Math.round(lineHeight * FONT_SIZE_RATIO));
    }

    private TextSlot defaultNameSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        int off = scale(NAME_OFFSET_X, sx);
        if ("special".equals(key)) {
            return TextSlot.left(scale(10100, sx) + off, scale(6960, sy), scale(16500, sx) + off, scale(7250, sy),
                    scale(10097, sx) + off, scale(6957, sy), scale(16503, sx) + off, scale(7253, sy), true,
                    labelFontSize(scale(7250, sy) - scale(6960, sy)));
        }
        return TextSlot.left(scale(9777, sx) + off, scale(6628, sy), scale(16500, sx) + off, scale(6959, sy),
                scale(9774, sx) + off, scale(6625, sy), scale(16503, sx) + off, scale(6962, sy), true,
                labelFontSize(scale(6959, sy) - scale(6628, sy)));
    }

    private TextSlot defaultPromotionSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return null;
        }
        return TextSlot.left(scale(9400, sx), scale(6100, sy), scale(10744, sx), scale(6527, sy),
                scale(9400, sx), scale(6097, sy), scale(10620, sx), scale(6530, sy), false,
                2 * labelFontSize(scale(6527, sy) - scale(6100, sy)));
    }

    private TextSlot defaultNumeroSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return TextSlot.left(scale(7346, sx), scale(6315, sy), scale(8328, sx), scale(6741, sy),
                    scale(7343, sx), scale(6312, sy), scale(8331, sx), scale(6744, sy), false,
                    labelFontSize(scale(6741, sy) - scale(6315, sy)));
        }
        return TextSlot.left(scale(7184, sx), scale(6040, sy), scale(8142, sx), scale(6467, sy),
                scale(7181, sx), scale(6037, sy), scale(8145, sx), scale(6470, sy), false,
                labelFontSize(scale(6467, sy) - scale(6040, sy)));
    }

    private TextSlot defaultMentionSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return TextSlot.left(scale(13500, sx), scale(7475, sy), scale(17850, sx), scale(7725, sy),
                    scale(13497, sx), scale(7472, sy), scale(17853, sx), scale(7728, sy), false,
                    labelFontSize(scale(7725, sy) - scale(7475, sy)));
        }
        return TextSlot.left(scale(12570, sx), scale(7155, sy), scale(18450, sx), scale(7446, sy),
                scale(12567, sx), scale(7152, sy), scale(18453, sx), scale(7449, sy), false,
                labelFontSize(scale(7446, sy) - scale(7155, sy)));
    }

    private TextSlot defaultBirthSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return TextSlot.left(scale(6250, sx), scale(7441, sy), scale(11450, sx), scale(7729, sy),
                    scale(6247, sx), scale(7438, sy), scale(11453, sx), scale(7732, sy), false,
                    labelFontSize(scale(7729, sy) - scale(7441, sy)));
        }
        return TextSlot.left(scale(6000, sx), scale(7129, sy), scale(11200, sx), scale(7415, sy),
                scale(5997, sx), scale(7126, sy), scale(11203, sx), scale(7418, sy), false,
                labelFontSize(scale(7415, sy) - scale(7129, sy)));
    }

    private TextSlot defaultPeriodSlot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return TextSlot.left(scale(12650, sx), scale(7938, sy), scale(19750, sx), scale(8226, sy),
                    scale(12647, sx), scale(7935, sy), scale(19753, sx), scale(8229, sy), false,
                    labelFontSize(scale(8226, sy) - scale(7938, sy)));
        }
        return TextSlot.left(scale(12470, sx), scale(7682, sy), scale(19750, sx), scale(7970, sy),
                scale(12467, sx), scale(7679, sy), scale(19753, sx), scale(7973, sy), false,
                labelFontSize(scale(7970, sy) - scale(7682, sy)));
    }

    private TextSlot defaultPeriod2Slot(String key, int width, int height) {
        double sx = width / (double) TEMPLATE_REF_WIDTH;
        double sy = height / (double) TEMPLATE_REF_HEIGHT;
        if ("special".equals(key)) {
            return TextSlot.center(scale(4000, sx), scale(8360, sy), scale(16000, sx), scale(8648, sy),
                    scale(3997, sx), scale(8357, sy), scale(16003, sx), scale(8651, sy), false,
                    labelFontSize(scale(8648, sy) - scale(8360, sy)));
        }
        return TextSlot.center(scale(4000, sx), scale(7980, sy), scale(16000, sx), scale(8268, sy),
                scale(3997, sx), scale(7977, sy), scale(16003, sx), scale(8271, sy), false,
                labelFontSize(scale(8268, sy) - scale(7980, sy)));
    }

    private static int scale(int value, double factor) {
        return (int) Math.round(value * factor);
    }

    private static final int MAX_TEMPLATE_DIMENSION = 3508;

    private BufferedImage loadTemplate(String key) {
        String path = "special".equals(key) ? TEMPLATE_SPECIAL : TEMPLATE_ORDINAIRE;
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IllegalStateException("Template illisible: " + path);
            return image;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger le template " + path, e);
        }
    }

    private static BufferedImage downscaleIfNeeded(BufferedImage image) {
        if (image.getWidth() <= MAX_TEMPLATE_DIMENSION && image.getHeight() <= MAX_TEMPLATE_DIMENSION) {
            return image;
        }
        double scale = Math.min(MAX_TEMPLATE_DIMENSION / (double) image.getWidth(),
                MAX_TEMPLATE_DIMENSION / (double) image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        image.flush();
        return scaled;
    }


    private enum Align { LEFT, CENTER, RIGHT }

    private static final class TextSlot {
        private final Align align;
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;
        private final int eraseLeft;
        private final int eraseTop;
        private final int eraseRight;
        private final int eraseBottom;
        private final boolean bold;
        private final int maxFontSize;

        private TextSlot(Align align, int left, int top, int right, int bottom,
                         int eraseLeft, int eraseTop, int eraseRight, int eraseBottom,
                         boolean bold, int maxFontSize) {
            this.align = align;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.eraseLeft = eraseLeft;
            this.eraseTop = eraseTop;
            this.eraseRight = eraseRight;
            this.eraseBottom = eraseBottom;
            this.bold = bold;
            this.maxFontSize = maxFontSize;
        }

        static TextSlot left(int left, int top, int right, int bottom,
                             int eraseLeft, int eraseTop, int eraseRight, int eraseBottom, boolean bold) {
            return new TextSlot(Align.LEFT, left, top, right, bottom,
                    eraseLeft, eraseTop, eraseRight, eraseBottom, bold, bold ? 20 : 17);
        }

        static TextSlot left(int left, int top, int right, int bottom,
                             int eraseLeft, int eraseTop, int eraseRight, int eraseBottom,
                             boolean bold, int maxFontSize) {
            return new TextSlot(Align.LEFT, left, top, right, bottom,
                    eraseLeft, eraseTop, eraseRight, eraseBottom, bold, maxFontSize);
        }

        static TextSlot right(int left, int top, int right, int bottom,
                              int eraseLeft, int eraseTop, int eraseRight, int eraseBottom, boolean bold) {
            return new TextSlot(Align.RIGHT, left, top, right, bottom,
                    eraseLeft, eraseTop, eraseRight, eraseBottom, bold, bold ? 20 : 17);
        }

        static TextSlot right(int left, int top, int right, int bottom,
                              int eraseLeft, int eraseTop, int eraseRight, int eraseBottom,
                              boolean bold, int maxFontSize) {
            return new TextSlot(Align.RIGHT, left, top, right, bottom,
                    eraseLeft, eraseTop, eraseRight, eraseBottom, bold, maxFontSize);
        }

        static TextSlot center(int left, int top, int right, int bottom,
                               int eraseLeft, int eraseTop, int eraseRight, int eraseBottom,
                               boolean bold, int maxFontSize) {
            return new TextSlot(Align.CENTER, left, top, right, bottom,
                    eraseLeft, eraseTop, eraseRight, eraseBottom, bold, maxFontSize);
        }

        TextSlot scaled(double sx, double sy) {
            return new TextSlot(align,
                    (int) Math.round(left * sx), (int) Math.round(top * sy),
                    (int) Math.round(right * sx), (int) Math.round(bottom * sy),
                    (int) Math.round(eraseLeft * sx), (int) Math.round(eraseTop * sy),
                    (int) Math.round(eraseRight * sx), (int) Math.round(eraseBottom * sy),
                    bold, Math.max(10, (int) Math.round(maxFontSize * Math.min(sx, sy))));
        }
    }

    private static final class TemplateLayout {
        private final BufferedImage template;
        private final TextSlot name;
        private final TextSlot numero;
        private final TextSlot promotion;
        private final TextSlot mention;
        private final TextSlot birth;
        private final TextSlot period;
        private final TextSlot period2;

        private TemplateLayout(BufferedImage template, TextSlot name, TextSlot numero, TextSlot promotion, TextSlot mention, TextSlot birth, TextSlot period, TextSlot period2) {
            this.template = template;
            this.name = name;
            this.numero = numero;
            this.promotion = promotion;
            this.mention = mention;
            this.birth = birth;
            this.period = period;
            this.period2 = period2;
        }

        private BufferedImage render(String nomComplet, Integer numeroEoa, String promotionText, String mentionText, String birthText, String periodText) {
            BufferedImage copy = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(template, 0, 0, null);

            if (nomComplet != null && !nomComplet.isBlank()) {
                drawSlot(g, nomComplet.trim(), name, TEXT_COLOR);
            }
            int numeroFontSize = 100;
            if (numeroEoa != null && numero != null) {
                numeroFontSize = fitFont(g, String.valueOf(numeroEoa), numero, Font.BOLD | Font.ITALIC).getSize();
                drawSlot(g, String.valueOf(numeroEoa), numero, RED_COLOR, Font.BOLD | Font.ITALIC);
            }
            if (birthText != null && !birthText.isBlank()) {
                drawSlot(g, birthText.trim(), birth, RED_COLOR);
            }
            if (promotionText != null && !promotionText.isBlank() && promotion != null) {
                eraseRegion(copy, promotion.eraseLeft, promotion.eraseTop, promotion.eraseRight, promotion.eraseBottom);
                drawPromotion(g, promotionText.trim(), promotion, numeroFontSize);
            }
            if (mentionText != null && !mentionText.isBlank()) {
                drawSlot(g, mentionText.trim(), mention, TEXT_COLOR);
            }
            if (period != null && periodText != null && !periodText.isBlank()) {
                int idx = periodText.indexOf(" au ");
                if (idx > 0) {
                    drawSlot(g, periodText.substring(0, idx).trim(), period, TEXT_COLOR);
                    if (period2 != null) {
                        drawSlot(g, periodText.substring(idx + 1).trim(), period2, TEXT_COLOR);
                    }
                } else {
                    drawSlot(g, periodText.trim(), period, TEXT_COLOR);
                }
            }
            g.dispose();
            return copy;
        }

        private void drawSlot(Graphics2D g, String text, TextSlot slot, Color color) {
            drawSlot(g, text, slot, color, Font.BOLD | Font.ITALIC);
        }

        private void drawSlot(Graphics2D g, String text, TextSlot slot, Color color, int style) {
            Font font = fitFont(g, text, slot, style);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics(font);
            int lineHeight = slot.bottom - slot.top;
            int textHeight = fm.getHeight();
            int baseline = slot.top + (lineHeight - textHeight) / 2 + fm.getAscent();
            int x;
            switch (slot.align) {
                case CENTER -> x = (slot.left + slot.right - fm.stringWidth(text)) / 2;
                case RIGHT -> x = slot.right - fm.stringWidth(text);
                default -> x = slot.left;
            }
            g.setColor(color);
            g.drawString(text, x, baseline);
        }

        private Font fitFont(Graphics2D g, String text, TextSlot slot) {
            return fitFont(g, text, slot, Font.BOLD | Font.ITALIC);
        }

        private Font fitFont(Graphics2D g, String text, TextSlot slot, int style) {
            for (int size = slot.maxFontSize; size >= 10; size--) {
                Font font = new Font(FONT_FAMILY, style, size);
                FontMetrics fm = g.getFontMetrics(font);
                if (fm.stringWidth(text) <= (slot.right - slot.left)) return font;
            }
            return new Font(FONT_FAMILY, style, 10);
        }

        private void drawPromotion(Graphics2D g, String text, TextSlot slot, int maxFontSize) {
            java.util.regex.Matcher m = Pattern.compile("^(\\d+)(\\D.*)$").matcher(text);
            if (!m.matches()) {
                drawSlot(g, text, slot, RED_COLOR);
                return;
            }   
            String number = m.group(1);
            String suffix = m.group(2);

            Font font = fitFont(g, number, slot);
            if (maxFontSize > 0 && font.getSize() > maxFontSize) {
                font = font.deriveFont((float) maxFontSize);
            }
            Font supFont = font.deriveFont((float) (Math.max(4, Math.min(font.getSize() / 2, 5)) + 40));

            g.setFont(font);
            FontMetrics fm = g.getFontMetrics(font);
            FontMetrics supFm = g.getFontMetrics(supFont);

            int lineHeight = slot.bottom - slot.top;
            int baseline = slot.top + (lineHeight - fm.getHeight()) / 2 + fm.getAscent();
            int mainWidth = fm.stringWidth(number);
            int supWidth = supFm.stringWidth(suffix);
            int gap = Math.max(1, mainWidth / 60);
            int total = mainWidth + gap + supWidth;
            while (font.getSize() > 10 && total > (slot.right - slot.left)) {
                font = font.deriveFont(font.getSize() - 1f);
                supFont = font.deriveFont((float) (Math.max(4, Math.min(font.getSize() / 2, 5)) + 40));
                g.setFont(font);
                fm = g.getFontMetrics(font);
                supFm = g.getFontMetrics(supFont);
                baseline = slot.top + (lineHeight - fm.getHeight()) / 2 + fm.getAscent();
                mainWidth = fm.stringWidth(number);
                supWidth = supFm.stringWidth(suffix);
                gap = Math.max(1, mainWidth / 60);
                total = mainWidth + gap + supWidth;
            }
            int supBaseline = baseline - fm.getAscent() + supFm.getAscent();
            int mainX;
            switch (slot.align) {
                case CENTER -> mainX = (slot.left + slot.right - total) / 2;
                case RIGHT -> mainX = slot.right - total;
                default -> mainX = slot.left;
            }
            int supX = mainX + mainWidth + gap;

            g.setColor(RED_COLOR);
            g.drawString(number, mainX, baseline);
            g.setFont(supFont);
            g.drawString(suffix, supX, supBaseline);
        }

        private static void eraseRegion(BufferedImage img, int left, int top, int right, int bottom) {
            int x0 = Math.max(0, left);
            int y0 = Math.max(0, top);
            int x1 = Math.min(img.getWidth() - 1, right - 1);
            int y1 = Math.min(img.getHeight() - 1, bottom - 1);
            for (int y = y0; y <= y1; y++) {
                int x = x0;
                while (x <= x1) {
                    if (!isInk(img.getRGB(x, y))) {
                        x++;
                        continue;
                    }
                    int s = x;
                    while (x <= x1 && isInk(img.getRGB(x, y))) x++;
                    int e = x - 1;
                    int bgL = findBackgroundLeft(img, s - 1, y);
                    int bgR = findBackgroundRight(img, e + 1, y);
                    if (bgL != -1 || bgR != -1) {
                        if (bgL == -1) bgL = bgR;
                        if (bgR == -1) bgR = bgL;
                        int len = e - s;
                        for (int px = s; px <= e; px++) {
                            double t = len > 0 ? (double) (px - s) / len : 0.5;
                            img.setRGB(px, y, lerp(bgL, bgR, t));
                        }
                    } else {
                        for (int px = s; px <= e; px++) {
                            int up = findBackgroundColumnUp(img, px, y - 1, y0);
                            int down = findBackgroundColumnDown(img, px, y + 1, y1);
                            int bg = up != -1 ? up : down;
                            if (bg != -1) img.setRGB(px, y, bg);
                        }
                    }
                }
            }
        }

        private static boolean isInk(int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return r - g > 40 && r - b > 40;
        }

        private static int findBackgroundLeft(BufferedImage img, int x, int y) {
            for (; x >= 0; x--) {
                if (!isInk(img.getRGB(x, y))) return img.getRGB(x, y);
            }
            return -1;
        }

        private static int findBackgroundRight(BufferedImage img, int x, int y) {
            for (; x < img.getWidth(); x++) {
                if (!isInk(img.getRGB(x, y))) return img.getRGB(x, y);
            }
            return -1;
        }

        private static int findBackgroundColumnUp(BufferedImage img, int x, int y, int minY) {
            for (; y >= minY; y--) {
                if (!isInk(img.getRGB(x, y))) return img.getRGB(x, y);
            }
            return -1;
        }

        private static int findBackgroundColumnDown(BufferedImage img, int x, int y, int maxY) {
            for (; y <= maxY; y++) {
                if (!isInk(img.getRGB(x, y))) return img.getRGB(x, y);
            }
            return -1;
        }

        private static int lerp(int a, int b, double t) {
            int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
            int r = (int) Math.round(ar + (br - ar) * t);
            int g = (int) Math.round(ag + (bg - ag) * t);
            int bl = (int) Math.round(ab + (bb - ab) * t);
            return (0xFF << 24) | (r << 16) | (g << 8) | bl;
        }
    }


    private static final class PdfBuilder {

        private static final float MARGIN = 0f;

        private PdfBuilder() {
        }

        private static void addPage(PDDocument document, BufferedImage image) throws Exception {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float maxW = pageW - 2 * MARGIN;
            float maxH = pageH - 2 * MARGIN;
            float scale = Math.min(maxW / image.getWidth(), maxH / image.getHeight());
            float w = image.getWidth() * scale;
            float h = image.getHeight() * scale;
            float x = (pageW - w) / 2;
            float y = (pageH - h) / 2;

            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D gg = rgb.createGraphics();
            gg.setColor(Color.WHITE);
            gg.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            gg.drawImage(image, 0, 0, null);
            gg.dispose();
            PDImageXObject xobject = LosslessFactory.createFromImage(document, rgb);
            rgb.flush();

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.drawImage(xobject, x, y, w, h);
            }
            document.addPage(page);
        }
    }
}
