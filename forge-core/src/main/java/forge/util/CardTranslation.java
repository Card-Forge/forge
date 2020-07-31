package forge.util;

import com.google.common.base.Charsets;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CardTranslation {

    private static Map <String, String> translatednames;
    private static Map <String, String> translatedtypes;
    private static Map <String, String> translatedoracles;
    private static String languageSelected = "en-US";

    private static void readTranslationFile(String language, String languagesDirectory) {
        String filename = "cardnames-" + language + ".txt";

        try (LineReader translationFile = new LineReader(new FileInputStream(languagesDirectory + filename), Charsets.UTF_8)) {
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
            System.err.println("Error reading translation file: cardnames-" + language + ".txt");
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
        HashMap<String, String> translations = new HashMap<>();
        translations.put("name", getTranslatedName(cardname));
        translations.put("oracle", getTranslatedOracle(cardname));
        translations.put("altname", getTranslatedName(altcardname));
        translations.put("altoracle", getTranslatedOracle(altcardname));
        return translations;
    }

    private static boolean needsTranslation() {
        return !languageSelected.equals("en-US");
    }

    public static void preloadTranslation(String language, String languagesDirectory) {
        languageSelected = language;
        
        if (needsTranslation()) {
            translatednames = new HashMap<>();
            translatedtypes = new HashMap<>();
            translatedoracles = new HashMap<>();
            readTranslationFile(languageSelected, languagesDirectory);
        }
    }
}