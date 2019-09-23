package forge.card;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Charsets;
import forge.properties.ForgeConstants;
import forge.util.LineReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CardTranslation {

    private static Map <String, String> translatednames;
    private static Map <String, String> translatedtypes;
    private static Map <String, String> translatedoracles;
    private static String languageSelected;

    private static void readTranslationFile(String language) {
        String filename = "cardnames-" + language + ".txt";

        try (LineReader translationFile = new LineReader(new FileInputStream(ForgeConstants.LANG_DIR + filename), Charsets.UTF_8)) {
            for (String line : translationFile.readLines()) {
                String[] matches = line.split("\\|");
                if (matches.length >= 2) {
                    translatednames.put(matches[0], matches[1]);
                }
                if (matches.length >= 3) {
                    translatedtypes.put(matches[0], matches[2]);
                }
                if (matches.length >= 4) {
                    translatedoracles.put(matches[0], matches[3].replace("\\n", "\n\n"));
                }
            }
        } catch (IOException e) {
            Log.error("Error reading translation file: cardnames-" + language + ".txt");
        }
    }

    public static String getTranslatedName(String name) {
        if (needsTranslation()) {
            String tname = translatednames.get(name);
            return tname == null ? name : tname;
        }

        return name;
    }

    public static String getTranslatedType(String name, String originaltype) {
        if (needsTranslation()) {
            String ttype = translatedtypes.get(name);
            return ttype == null ? originaltype : ttype;
        }

        return originaltype;
    }

    public static String getTranslatedOracle(String name) {
        if (needsTranslation()) {
            String toracle = translatedoracles.get(name);
            return toracle == null ? "" : toracle;
        }

        return "";
    }

    public static HashMap<String, String> getTranslationTexts(String cardname, String altcardname) {
        HashMap<String, String> translations = new HashMap<String, String>();
        translations.put("name", getTranslatedName(cardname));
        translations.put("oracle", getTranslatedOracle(cardname));
        translations.put("altname", getTranslatedName(altcardname));
        translations.put("altoracle", getTranslatedOracle(altcardname));
        return translations;
    }

    private static boolean needsTranslation() {
        return !languageSelected.equals("en-US");
    }

    public static void preloadTranslation(String language) {
        languageSelected = language;
        
        if (needsTranslation()) {
            translatednames = new HashMap<>();
            translatedtypes = new HashMap<>();
            translatedoracles = new HashMap<>();
            readTranslationFile(languageSelected);
        }
    }
}