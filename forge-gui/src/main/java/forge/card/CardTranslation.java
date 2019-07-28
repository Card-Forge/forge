package forge.card;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Charsets;
import forge.GuiBase;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.LineReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class CardTranslation {

    private static Map <String, String> translatednames;
    private static Map <String, String> translatedtypes;
    private static Map <String, String> translatedoracles;

    private static String removeDiacritics(String text) {
        text = text.replace("ñ", "ny");
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return text;
    }

    private static void readTranslationFile(String language) {
        String filename = "cardnames-" + language + ".txt";

        try (LineReader translationFile = new LineReader(new FileInputStream(ForgeConstants.LANG_DIR + filename), Charsets.UTF_8)) {
            for (String line : translationFile.readLines()) {
                String[] matches = line.split("#");

                if(matches.length >= 2) {
                    translatednames.put(matches[0], matches[1]);
                }

                if(matches.length >= 3) {
                    translatedtypes.put(matches[0], matches[2]);
                }

                if(matches.length >= 4) {
                    translatedoracles.put(matches[0], matches[3].replace("\\n", "\n\n"));
                }
            }
        } catch (IOException e) {
            Log.error("Error reading translated file. Language: " + language);
        }
    }

    public static String getTranslatedName(String name) {
        String tname = translatednames.get(name);

        if (tname != null) {
            if (GuiBase.getInterface().isLibgdxPort()) tname = removeDiacritics(tname);
          } else {
            tname = name;
        }

        return tname;
    }

    public static String getTranslatedType(String name, String originaltype) {
        String ttype = translatedtypes.get(name);

        if (ttype != null) {
            if (GuiBase.getInterface().isLibgdxPort()) ttype = removeDiacritics(ttype);
        } else {
            ttype = originaltype;
        }

        return ttype;
    }

    public static String getTranslatedOracle(String name, String originaloracle) {
        String toracle = translatedoracles.get(name);

        if (toracle != null) {
            if (GuiBase.getInterface().isLibgdxPort()) toracle = removeDiacritics(toracle);
        } else {
            toracle = originaloracle;
        }

        return toracle;
    }

    private static boolean needsTranslation() {
        ForgePreferences preferences = new ForgePreferences();
        return !preferences.getPref(ForgePreferences.FPref.UI_LANGUAGE).equals("en-US");
    }

    public static void preloadTranslation(String language) {
        if (needsTranslation()) {
            translatednames = new HashMap<>();
            translatedtypes = new HashMap<>();
            translatedoracles = new HashMap<>();
            readTranslationFile(language);
        }
    }
}