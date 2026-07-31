package com.emia.archives.util;

import java.util.HashMap;
import java.util.Map;

public class CountryResolver {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        put("sénégal", "Sénégal");
        put("senegal", "Sénégal");
        put("république du senegal", "Sénégal");
        put("republique du senegal", "Sénégal");

        put("niger", "Niger");

        put("guinée", "Guinée");
        put("guinee", "Guinée");
        put("guinea", "Guinée");
        put("république de guinee", "Guinée");
        put("republique de guinee", "Guinée");
        put("république de guinée", "Guinée");

        put("burkina faso", "Burkina Faso");
        put("burkina-faso", "Burkina Faso");
        put("burkina fasso", "Burkina Faso");
        put("burkina-fasso", "Burkina Faso");
        put("bf", "Burkina Faso");

        put("togo", "Togo");

        put("bénin", "Bénin");
        put("benin", "Bénin");

        put("gabon", "Gabon");

        put("cameroun", "Cameroun");

        put("mali", "Mali");

        put("mauritanie", "Mauritanie");

        put("côte d'ivoire", "Côte d'Ivoire");
        put("cote d'ivoire", "Côte d'Ivoire");
        put("côte d’ivoire", "Côte d'Ivoire");
        put("cote d’ivoire", "Côte d'Ivoire");
        put("cote divoire", "Côte d'Ivoire");

        put("tchad", "Tchad");

        put("rca", "Centrafrique");
        put("centrafrique", "Centrafrique");
        put("république centrafricaine", "Centrafrique");

        put("congo brazza", "Congo");
        put("congo-brazza", "Congo");
        put("congo", "Congo");

        put("ouganda", "Ouganda");
        put("emia ouganda", "Ouganda");
    }

    private static void put(String alias, String country) {
        ALIASES.put(alias.toLowerCase().trim(), country);
    }

    public static String resolve(String observations) {
        if (observations == null || observations.isBlank()) return null;

        String trimmed = observations.trim();
        String lower = trimmed.toLowerCase();

        String exact = ALIASES.get(lower);
        if (exact != null) return exact;

        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }
}
