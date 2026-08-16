package com.emia.archives.service;

import com.emia.archives.dto.BulletinData;
import com.emia.archives.entity.Bulletin;
import com.emia.archives.entity.Promotion;
import com.emia.archives.entity.Stagiaire;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.repository.BulletinRepository;
import com.emia.archives.repository.StagiaireRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.extend.FSSupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulletinService {

    private final StagiaireRepository stagiaireRepository;
    private final BulletinRepository bulletinRepository;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_STRING = new TypeReference<>() {};

    @Transactional
    public BulletinData saveBulletin(String schoolId, Long stagiaireId, BulletinData data) throws JsonProcessingException {
        Stagiaire stagiaire = findStagiaire(schoolId, stagiaireId);
        Bulletin bulletin = bulletinRepository.findByStagiaireId(stagiaireId).orElseGet(Bulletin::new);
        bulletin.setStagiaire(stagiaire);
        bulletin.setPays(val(data.pays()));
        bulletin.setNotes(writeJson(data.notes()));
        bulletin.setCoefficients(writeJson(data.coefficients()));
        bulletin.setObservations(writeJson(data.observations()));
        bulletin.setMoyenne1(val(data.moyenne1()));
        bulletin.setMoyenne2(val(data.moyenne2()));
        bulletin.setMoyenne3(val(data.moyenne3()));
        bulletin.setClassementRang(val(data.classementRang()));
        bulletin.setClassementTotal(val(data.classementTotal()));
        bulletin.setAppreciation(val(data.appreciation()));
        bulletinRepository.save(bulletin);
        return data;
    }

    @Transactional(readOnly = true)
    public BulletinData getBulletin(String schoolId, Long stagiaireId) {
        findStagiaire(schoolId, stagiaireId);
        return bulletinRepository.findByStagiaireId(stagiaireId)
                .map(b -> new BulletinData(
                        b.getPays(),
                        readJson(b.getNotes()),
                        readJson(b.getCoefficients()),
                        readJson(b.getObservations()),
                        b.getMoyenne1(),
                        b.getMoyenne2(),
                        b.getMoyenne3(),
                        b.getClassementRang(),
                        b.getClassementTotal(),
                        b.getAppreciation()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public byte[] generateBulletin(String schoolId, Long stagiaireId, BulletinData data) throws Exception {
        Stagiaire stagiaire = findStagiaire(schoolId, stagiaireId);

        BulletinData d = data != null ? data : new BulletinData(null, null, null, null, null, null, null, null, null, null);

        String html = TEMPLATE;
        html = html.replace("{{LOGO}}", logoDataUri());
        html = html.replace("{{NOM}}", escape(stagiaire.getNomComplet() != null ? stagiaire.getNomComplet().trim() : ""));
        html = html.replace("{{ANNEE}}", schoolYear(stagiaire.getPromotion()));
        html = html.replace("{{PAYS}}", escape(val(d.pays())));
        for (int i = 1; i <= 12; i++) {
            html = html.replace("{{N" + i + "}}", escape(val(row(d.notes(), i))));
            html = html.replace("{{C" + i + "}}", escape(coefi(row(d.coefficients(), i), i)));
            html = html.replace("{{O" + i + "}}", escape(val(row(d.observations(), i))));
        }
        html = html.replace("{{M1}}", escape(val(d.moyenne1())));
        html = html.replace("{{M2}}", escape(val(d.moyenne2())));
        html = html.replace("{{M3}}", escape(val(d.moyenne3())));
        html = html.replace("{{MFC}}", moyenneFinCycle(d));
        html = html.replace("{{CLR}}", escape(val(d.classementRang())));
        html = html.replace("{{CLT}}", escape(val(d.classementTotal())));
        String appreciation = escape(val(d.appreciation())).replace("\n", "<br/>");
        html = html.replace("{{APPR}}", appreciation);
        return renderPdf(html);
    }

    private static String moyenneFinCycle(BulletinData d) {
        double a = parse(val(d.moyenne1()));
        double b = parse(val(d.moyenne2()));
        double c = parse(val(d.moyenne3()));
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            return "";
        }
        double resultat = (((a + b) / 2) + (c * 2)) / 3;
        return String.format("%.2f", resultat).replace('.', ',');
    }

    private static double parse(String value) {
        if (value == null || value.isBlank()) return Double.NaN;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String row(List<String> list, int index) {
        if (list == null || list.size() < index || list.get(index - 1) == null) {
            return "";
        }
        return list.get(index - 1).trim();
    }

    private static String val(String value) {
        return value != null ? value.trim() : "";
    }

    private static final String[] DEFAULT_COEFFICIENTS = {"10", "10", "4", "5", "8", "4", "4", "3", "4", "3", "2", "3"};

    private static String coefi(String value, int index) {
        if (value == null || value.isBlank()) {
            if (index >= 1 && index <= DEFAULT_COEFFICIENTS.length) {
                return DEFAULT_COEFFICIENTS[index - 1];
            }
            return "";
        }
        return value.trim();
    }

    private Stagiaire findStagiaire(String schoolId, Long stagiaireId) {
        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", "id", stagiaireId));
        if (!stagiaire.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Stagiaire", "schoolId", schoolId);
        }
        return stagiaire;
    }

    private String writeJson(List<String> values) throws JsonProcessingException {
        if (values == null) {
            return "[]";
        }
        return objectMapper.writeValueAsString(values);
    }

    private List<String> readJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_STRING);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static byte[] renderPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(fontSupplier("fonts/LiberationSerif-Regular.ttf"), "Liberation Serif", 400, FontStyle.NORMAL, true);
            builder.useFont(fontSupplier("fonts/LiberationSerif-Bold.ttf"), "Liberation Serif", 700, FontStyle.NORMAL, true);
            builder.useFont(fontSupplier("fonts/LiberationSerif-Italic.ttf"), "Liberation Serif", 400, FontStyle.ITALIC, true);
            builder.useFont(fontSupplier("fonts/LiberationSerif-BoldItalic.ttf"), "Liberation Serif", 700, FontStyle.ITALIC, true);
            builder.withHtmlContent(html, "src/main/resources/");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private static FSSupplier<InputStream> fontSupplier(String path) {
        return () -> {
            try {
                return new ClassPathResource(path).getInputStream();
            } catch (Exception e) {
                throw new RuntimeException("Police introuvable: " + path, e);
            }
        };
    }

    private static String schoolYear(Promotion promotion) {
        if (promotion != null) {
            Integer deb = promotion.getAnneeEntree() != null ? promotion.getAnneeEntree().getYear() : null;
            Integer fin = promotion.getAnneeSortie() != null ? promotion.getAnneeSortie().getYear() : null;
            if (deb != null && fin != null) {
                return deb + "-" + fin;
            }
            if (fin != null) {
                return (fin - 1) + "-" + fin;
            }
            if (deb != null) {
                return deb + "-" + (deb + 1);
            }
        }
        YearMonth now = YearMonth.now();
        int y = now.getYear();
        if (now.getMonthValue() >= 8) {
            return y + "-" + (y + 1);
        }
        return (y - 1) + "-" + y;
    }

    private static String logoDataUri() throws Exception {
        BufferedImage src;
        try (InputStream in = new ClassPathResource("static/images/logo_emia.png").getInputStream()) {
            src = ImageIO.read(in);
        }
        int targetH = 140;
        int targetW = (int) Math.round(src.getWidth() * (targetH / (double) src.getHeight()));
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaled, "png", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static final String TEMPLATE = """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8"/>
<title>Fiche de Notation - EMIA</title>
<style>
  @page {
    size: A4;
    margin: 12mm 14mm;
  }

  html, body {
    margin: 0;
    padding: 0;
    font-family: "Liberation Serif", "Times New Roman", Times, serif;
    color: #000;
  }

  .page {
    background: #ffffff;
    width: 100%;
  }

  .blank {
    display: block;
    min-height: 9px;
  }

  /* ===================== EN-TETE ===================== */
  table.letterhead {
    width: 100%;
    border-collapse: collapse;
    font-size: 9.5pt;
  }

  table.letterhead td {
    vertical-align: top;
    padding: 0;
  }

  table.letterhead td.left {
    width: 38%;
    text-align: left;
    white-space: nowrap;
  }

  table.letterhead td.center {
    width: 22%;
    text-align: center;
    padding-top: 2px;
  }

  table.letterhead td.right {
    width: 40%;
    text-align: center;
  }

  table.letterhead img {
    width: 64px;
    height: 50px;
  }

  table.letterhead td.right .devise {
    font-style: italic;
    font-size: 9pt;
  }

  table.letterhead td.left .devise {
    font-style: normal;
    font-weight: bold;
    text-align: center;
  }

  table.letterhead td.right .etoiles {
    font-weight: bold;
    margin-top: 2px;
  }

  /* ===================== TITRE ===================== */
  .title-block {
    text-align: center;
    margin-top: 12px;
  }

  .title-block .main-title {
    font-size: 15pt;
    font-weight: bold;
    letter-spacing: 0.5px;
  }

  .title-block .sub-title {
    font-size: 11pt;
    font-weight: bold;
    font-style: italic;
    margin-top: 2px;
  }

  /* ===================== INFOS CANDIDAT ===================== */
  table.infos {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
    font-size: 12pt;
    font-weight: bold;
  }

  table.infos td {
    vertical-align: baseline;
    padding: 0;
  }

  table.infos td.grade {
    text-align: right;
    white-space: nowrap;
  }

  .infos2 {
    margin-top: 6px;
    font-size: 12pt;
    font-weight: bold;
  }

  .line-input {
    display: inline-block;
    font-weight: bold;
    font-size: 12pt;
    border-bottom: 1px solid #000;
    padding: 0 2px 2px 4px;
    min-width: 120px;
  }

  td.nom-cell .line-input {
    width: 100%;
  }

  .infos2 .line-input {
    min-width: 320px;
  }

  /* ===================== TABLEAU DE NOTES ===================== */
  table.notes {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
    font-size: 10.5pt;
  }

  table.notes th, table.notes td {
    border: 1px solid #000;
    padding: 2px 4px;
  }

  table.notes th {
    font-size: 11pt;
    font-weight: bold;
    text-align: center;
    vertical-align: middle;
    background: #f2f2f2;
  }

  table.notes th.th-num { width: 5%; }
  table.notes th.th-matiere { width: 37%; }
  table.notes th.th-note { width: 14%; }
  table.notes th.th-coef { width: 12%; }
  table.notes th.th-obs { width: 32%; }

  table.notes td.col-num {
    text-align: center;
    width: 5%;
  }

  table.notes td.col-matiere {
    width: 37%;
  }

  table.notes td.col-note {
    width: 14%;
    text-align: center;
  }

  table.notes td.col-coef {
    width: 12%;
    text-align: center;
  }

  table.notes td.col-obs {
    width: 32%;
  }

  /* ===================== MOYENNES ===================== */
  .moyennes {
    margin-top: 10px;
    font-size: 12pt;
    font-weight: bold;
  }

  table.lignes {
    width: 100%;
    border-collapse: collapse;
    font-size: 12pt;
    font-weight: bold;
  }

  table.lignes td {
    padding: 0;
    vertical-align: baseline;
    white-space: nowrap;
  }

  table.lignes td.fill {
    border-bottom: 1px solid #000;
  }

  table.lignes td.moy-cell {
    width: 70px;
    text-align: center;
    border-bottom: 1px solid #000;
    min-height: 14px;
  }

  table.lignes tr {
    height: 19px;
  }

  table.lignes .sep {
    width: 12px;
    text-align: center;
  }

  .appreciation-title {
    text-align: center;
    font-weight: bold;
    font-size: 12pt;
    margin-top: 2px;
  }

  .lined {
    min-height: 17px;
  }

  /* ===================== SIGNATURES ===================== */
  table.signatures {
    width: 100%;
    border-collapse: collapse;
    margin-top: 66px;
    font-size: 12pt;
    font-weight: bold;
  }

  table.signatures td {
    padding: 0;
    vertical-align: top;
  }

  table.signatures td.directeur {
    width: 46%;
    text-align: left;
  }

  table.signatures td.commandant {
    width: 54%;
    text-align: center;
  }

  .sig-space {
    height: 58px;
  }

  .sig-name {
    text-decoration: underline;
    font-size: 11pt;
  }
</style>
</head>
<body>
<div class="page">

  <!-- EN-TETE -->
  <table class="letterhead">
    <tr>
      <td class="left">
        <div>DIRECTION DES ECOLES MILITAIRES</div>
        <div>CENTRE D'INSTRUCTION BOUBACAR SADA SY</div>
        <div class="devise">*******</div>
        <div>ECOLE MILITAIRE INTERARMES</div>
      </td>
      <td class="center">
        <img src="{{LOGO}}" alt="Logo EMIA" width="64" height="50"/>
      </td>
      <td class="right">
        <div>REPUBLIQUE DU MALI</div>
        <div class="devise">UN PEUPLE - UN BUT - UNE FOI</div>
        <div class="etoiles">*******</div>
      </td>
    </tr>
  </table>

  <!-- TITRE -->
  <div class="title-block">
    <div class="main-title">FICHE DE NOTATION</div>
    <div class="sub-title">(Année scolaire {{ANNEE}})</div>
  </div>

  <!-- INFOS CANDIDAT -->
  <table class="infos">
    <tr>
      <td class="nom-cell">NOM &amp; PRENOMS :
        <span class="line-input">{{NOM}}</span>
      </td>
      <td class="grade">GRADE : EOA</td>
    </tr>
  </table>
  <div class="infos2">
    PAYS :&#160;<span class="line-input">{{PAYS}}</span>
  </div>

  <!-- TABLEAU DE NOTES -->
  <table class="notes">
    <thead>
      <tr>
        <th class="th-num">N°</th>
        <th class="th-matiere">Matières</th>
        <th class="th-note">Note examen</th>
        <th class="th-coef">Coefficient</th>
        <th class="th-obs">Observations</th>
      </tr>
    </thead>
    <tbody>
      <tr><td class="col-num">1</td><td class="col-matiere">Tactique</td><td class="col-note">{{N1}}</td><td class="col-coef">{{C1}}</td><td class="col-obs">{{O1}}</td></tr>
      <tr><td class="col-num">2</td><td class="col-matiere">Sport</td><td class="col-note">{{N2}}</td><td class="col-coef">{{C2}}</td><td class="col-obs">{{O2}}</td></tr>
      <tr><td class="col-num">3</td><td class="col-matiere">Tir toutes armes</td><td class="col-note">{{N3}}</td><td class="col-coef">{{C3}}</td><td class="col-obs">{{O3}}</td></tr>
      <tr><td class="col-num">4</td><td class="col-matiere">Armement</td><td class="col-note">{{N4}}</td><td class="col-coef">{{C4}}</td><td class="col-obs">{{O4}}</td></tr>
      <tr><td class="col-num">5</td><td class="col-matiere">Topographie</td><td class="col-note">{{N5}}</td><td class="col-coef">{{C5}}</td><td class="col-obs">{{O5}}</td></tr>
      <tr><td class="col-num">6</td><td class="col-matiere">Instruction sur le Tir</td><td class="col-note">{{N6}}</td><td class="col-coef">{{C6}}</td><td class="col-obs">{{O6}}</td></tr>
      <tr><td class="col-num">7</td><td class="col-matiere">Règlement du Service dans l'Armée</td><td class="col-note">{{N7}}</td><td class="col-coef">{{C7}}</td><td class="col-obs">{{O7}}</td></tr>
      <tr><td class="col-num">8</td><td class="col-matiere">Génie</td><td class="col-note">{{N8}}</td><td class="col-coef">{{C8}}</td><td class="col-obs">{{O8}}</td></tr>
      <tr><td class="col-num">9</td><td class="col-matiere">Hygiène et Secourisme</td><td class="col-note">{{N9}}</td><td class="col-coef">{{C9}}</td><td class="col-obs">{{O9}}</td></tr>
      <tr><td class="col-num">10</td><td class="col-matiere">Ordre Serré</td><td class="col-note">{{N10}}</td><td class="col-coef">{{C10}}</td><td class="col-obs">{{O10}}</td></tr>
      <tr><td class="col-num">11</td><td class="col-matiere">Service en Campagne</td><td class="col-note">{{N11}}</td><td class="col-coef">{{C11}}</td><td class="col-obs">{{O11}}</td></tr>
      <tr><td class="col-num">12</td><td class="col-matiere">Transmission</td><td class="col-note">{{N12}}</td><td class="col-coef">{{C12}}</td><td class="col-obs">{{O12}}</td></tr>
    </tbody>
  </table>

  <!-- MOYENNES -->
  <div class="moyennes">
    <table class="lignes">
      <tr>
        <td>Moyenne 1ère Année :</td>
        <td class="moy-cell">{{M1}}</td>
        <td class="fill"></td>
      </tr>
      <tr>
        <td>Moyenne 2ème Année :</td>
        <td class="moy-cell">{{M2}}</td>
        <td class="fill"></td>
      </tr>
      <tr>
        <td>Moyenne 3ème Année :</td>
        <td class="moy-cell">{{M3}}</td>
        <td class="fill"></td>
      </tr>
      <tr>
        <td>Moyenne Fin de Cycle :</td>
        <td class="moy-cell">{{MFC}}</td>
        <td class="fill"></td>
      </tr>
      <tr>
        <td>Classement :</td>
        <td class="moy-cell">{{CLR}}</td>
        <td class="sep">/</td>
        <td class="moy-cell">{{CLT}}</td>
      </tr>
    </table>

    <div class="appreciation-title">Appréciation :</div>
    <div class="lined">{{APPR}}</div>
    <div class="lined">&#160;</div>
    <div class="lined">&#160;</div>
  </div>

  <!-- SIGNATURES -->
  <table class="signatures">
    <tr>
      <td class="directeur">
        <div>Le Directeur des Etudes</div>
        <div class="sig-space">&#160;</div>
        <div class="sig-name">Commandant Mamadou Modibo COULIBALY</div>
      </td>
      <td class="commandant">
        <div>Le Commandant de l'Ecole Militaire Interarmes</div>
        <div class="sig-space">&#160;</div>
        <div class="sig-name">Colonel Dramane DEMBELE</div>
      </td>
    </tr>
  </table>

</div>
</body>
</html>
""";
}
