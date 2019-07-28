package forge.card;

import com.esotericsoftware.minlog.Log;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.LineReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class CardTranslation {

    public static Map <String, String> translatednames;
    public static Map <String, String> translatedtypes;
    public static Map <String, String> translatedoracles;
    public static Map <String, String> translatedflavors;

    private static String removeDiacritics(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replace("ñ", "ny");
        return text;
    }

    private static void readTranslationFile(String language) {
        String filename = "cardnames-" + language + ".txt";

        try (LineReader translationFile = new LineReader(new FileInputStream(ForgeConstants.LANG_DIR + filename))) {

            for (String line : translationFile.readLines()) {
                String[] matches = line.split("#");

                if(matches.length >= 2) {
                    translatednames.put(matches[0], removeDiacritics(matches[1]));
                }

                if(matches.length >= 3) {
                    translatedtypes.put(matches[0], removeDiacritics(matches[2]));
                }

                if(matches.length >= 4) {
                    translatedoracles.put(matches[0], removeDiacritics(matches[3]).replace("\\n", "\n\n"));
                }

                if(matches.length >= 5) {
                    translatedflavors.put(matches[0], removeDiacritics(matches[4]));
                }
            }
        } catch (IOException e) {
            Log.error("Error reading translated file. Language: " + language);
        }
    }

    public static boolean needsTranslation() {
        return !ForgePreferences.FPref.UI_LANGUAGE.toString().equals("en-US");
    }

    public static void preloadTranslation(String language) {
        if (needsTranslation()) {
            translatednames = new HashMap<>();
            translatedtypes = new HashMap<>();
            translatedoracles = new HashMap<>();
            translatedflavors = new HashMap<>();
            readTranslationFile(ForgePreferences.FPref.UI_LANGUAGE.toString());
        }
    }
}