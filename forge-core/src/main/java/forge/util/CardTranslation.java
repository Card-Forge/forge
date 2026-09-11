package forge.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CardTranslation {

    private static Map <String, String> translatednames;
    private static Map <String, String> translatedtypes;
    private static Map <String, String> translatedoracles;
    private static Map <String, List <Pair <String, String> > > oracleMappings;
    private static Map <String, Map <String, String> > translatedCaches;
    private static Map <String, String> sharedCache;
    private static Map <String, String> translatedEffectNames;
    private static Map <String, String> translatedTokenNames;
    private static final List <String> knownEffectNames = Arrays.asList("The Ring", "The Monarch", "The Initiative", "City's Blessing", "Keyword Effects");
    private static final Pattern TRANSLATION_INVARIANT = Pattern.compile("\\{[^}]{1,6}\\}|[+-]\\d+/[+-]\\d+");
    private static String languageSelected = "en-US";

    private static void readTranslationFile(String language, String languagesDirectory) {
        String filename = "cardnames-" + language + ".txt";

        try (LineReader translationFile = new LineReader(Files.newInputStream(Paths.get(languagesDirectory + filename)), StandardCharsets.UTF_8)) {
            for (String line : translationFile.readLines()) {
                String[] matches = line.split("\\|");
                if (matches.length >= 2) {
                    if (matches[0].indexOf('$') > 0) {
                        //Functional variant, e.g. "Garbage Elemental $C"
                        String[] variantSplit = matches[0].split("\\s*\\$", 2);
                        if(variantSplit.length > 1) {
                            //Add the base name to the translated names.
                            translatednames.put(variantSplit[0], matches[1]);
                            matches[0] = variantSplit[0] + " $" + variantSplit[1]; //Standardize storage.
                        }
                    }
                    translatednames.put(matches[0], matches[1]);
                }
                if (matches.length >= 3) {
                    translatedtypes.put(matches[0], matches[2]);
                }
                if (matches.length >= 4) {
                    String toracle = matches[3];
                    // Workaround to remove additional //Level_2// and //Level_3// lines from non-English Class cards
                    toracle = toracle.replace("//Level_2//\\n", "").replace("//Level_3//\\n", "");
                    // Workaround for roll dice cards
                    toracle = toracle.replace("\\n", "\r\n\r\n").replace("VERT", "|");
                    translatedoracles.put(matches[0], toracle);
                }
            }
        } catch (IOException e) {
            if (!"en-US".equalsIgnoreCase(language))
                System.err.println("Error reading translation file: cardnames-" + language + ".txt");
        }
    }

    public static String getTranslatedName(String name) {
        if (needsTranslation()) {
            if (name.contains(" // ")) {
                int splitIndex = name.indexOf(" // ");
                String leftname = name.substring(0, splitIndex);
                String rightname = name.substring(splitIndex + 4);
                return translatednames.getOrDefault(leftname, leftname) + " // " + translatednames.getOrDefault(rightname, rightname);
            }
            try {
                if (name.endsWith(" Token")) {
                    return translateTokenName(name);
                } else if (name.startsWith("Emblem — ") || name.contains("'s Effect") || name.contains("'s Boon")) {
                    return translateEffectNames(name);
                } else if (knownEffectNames.contains(name)) {
                    return translateKnownEffectNames(name);
                } else {
                    String tname = translatednames.get(name);
                    return (tname == null || tname.isEmpty()) ? name : tname;
                }
            } catch (Exception e) {
                return name;
            }
        }
        return name;
    }

    public static String getTranslatedName(ITranslatable card) {
        return getTranslatedName(card.getUntranslatedName());
    }

    private static String translateTokenName(String name) {
        if (translatedTokenNames == null)
            translatedTokenNames = new HashMap<>();
        String ttype = translatedTokenNames.get(name);
        if (ttype == null) {
            String sub = name.replace(" Token", "");
            ttype = Localizer.getInstance().getMessageorUseDefault("lbl" + sub, "");
            if (ttype == null || ttype.isEmpty()) {
                ttype = name;
            } else {
                ttype = ttype  + " " + Localizer.getInstance().getMessage("lblToken");
            }
            translatedTokenNames.put(name, ttype);
            return ttype;
        } else {
            return ttype;
        }
    }

    private static String translateKnownEffectNames(String name) {
        if (translatedEffectNames == null)
            translatedEffectNames = new HashMap<>();
        String fname = translatedEffectNames.get(name);
        if (fname == null) {
            switch (name) {
                case "The Ring":
                    fname = Localizer.getInstance().getMessage("lblTheRing");
                    translatedEffectNames.put(name, fname);
                    return fname;
                case "The Monarch":
                    fname = Localizer.getInstance().getMessage("lblTheMonarch");
                    translatedEffectNames.put(name, fname);
                    return fname;
                case "The Initiative":
                    fname = Localizer.getInstance().getMessage("lblTheInitiative");
                    translatedEffectNames.put(name, fname);
                    return fname;
                case "City's Blessing":
                    fname = Localizer.getInstance().getMessage("lblCityBlessing");
                    translatedEffectNames.put(name, fname);
                    return fname;
                case "Keyword Effects":
                    fname = Localizer.getInstance().getMessage("lblKeywordEffects");
                    translatedEffectNames.put(name, fname);
                    return fname;
                default:
                    return name;
            }
        } else {
            return fname;
        }
    }

    private static String translateEffectNames(String name) {
        if (translatedEffectNames == null)
            translatedEffectNames = new HashMap<>();
        String fname = translatedEffectNames.get(name);
        if (fname == null) {
            String finalname = name.replaceAll("\\([^()]*\\)", "");
            if (finalname.contains(" 's Effect")) {
                finalname = finalname.replace( " 's Effect", "");
                fname = translatednames.get(finalname);
                if (fname == null || fname.isEmpty())
                    fname = finalname;
                else {
                    fname = fname + " " + Localizer.getInstance().getMessage("lblEffect");
                }
                translatedEffectNames.put(name, fname);
                return fname;
            } else if (finalname.contains("'s Effect")) {
                finalname = finalname.replace( "'s Effect", "");
                fname = translatednames.get(finalname);
                if (fname == null || fname.isEmpty())
                    fname = finalname;
                else {
                    fname = fname + " " + Localizer.getInstance().getMessage("lblEffect");
                }
                translatedEffectNames.put(name, fname);
                return fname;
            } else if (finalname.contains(" 's Boon")) {
                finalname = finalname.replace( " 's Boon", "");
                fname = translatednames.get(finalname);
                if (fname == null || fname.isEmpty())
                    fname = finalname;
                else {
                    fname = fname + " " + Localizer.getInstance().getMessage("lblBoon");
                }
                translatedEffectNames.put(name, fname);
                return fname;
            } else if (finalname.contains("'s Boon")) {
                finalname = finalname.replace( "'s Boon", "");
                fname = translatednames.get(finalname);
                if (fname == null || fname.isEmpty())
                    fname = finalname;
                else {
                    fname = fname + " " + Localizer.getInstance().getMessage("lblBoon");
                }
                translatedEffectNames.put(name, fname);
                return fname;
            } else if (finalname.startsWith("Emblem — ")) {
                String []s = finalname.split(" — ");
                try {
                    fname = translatednames.get(s[1].endsWith(" ") ? s[1].substring(0, s[1].lastIndexOf(" ")) : s[1]);
                    if (fname == null || fname.isEmpty())
                        fname = finalname;
                    else {
                        fname = fname + " " + Localizer.getInstance().getMessage("lblEmblem");
                    }
                    translatedEffectNames.put(name, fname);
                    return fname;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            return name;
        } else {
            return fname;
        }
    }

    public static String getTranslatedType(String name, String originaltype) {
        if (needsTranslation()) {
            String ttype = translatedtypes.get(name);
            return ttype == null ? originaltype : ttype;
        }

        return originaltype;
    }

    public static String getTranslatedType(ITranslatable item) {
        if (!needsTranslation())
            return item.getUntranslatedType();
        return translatedtypes.getOrDefault(item.getTranslationKey(), item.getUntranslatedType());
    }

    public static String getTranslatedOracle(String name) {
        if (needsTranslation()) {
            String toracle = translatedoracles.get(name);
            return toracle == null ? "" : toracle;
        }

        return "";
    }

    public static String getTranslatedOracle(ITranslatable card) {
        if(!needsTranslation())
            return ""; //card.getUntranslatedOracle();
        //Fallbacks and english versions of oracle texts are handled elsewhere.
        return translatedoracles.getOrDefault(card.getTranslationKey(), "");
    }

    public static HashMap<String, String> getTranslationTexts(ITranslatable card) {
        return getTranslationTexts(card, null);
    }

    public static HashMap<String, String> getTranslationTexts(ITranslatable cardMain, ITranslatable cardOther) {
        if(!needsTranslation()) return null;
        HashMap<String, String> translations = new HashMap<>();
        translations.put("name", getTranslatedName(cardMain));
        translations.put("oracle", getTranslatedOracle(cardMain));
        if(cardOther == null) {
            translations.put("altname", "");
            translations.put("altoracle", "");
        }
        else {
            translations.put("altname", getTranslatedName(cardOther));
            translations.put("altoracle", getTranslatedOracle(cardOther));
        }
        return translations;
    }

    public static String getLanguageSelected() {
        return languageSelected;
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
            oracleMappings = new HashMap<>();
            // read from the game thread and the EDT both, and computeIfAbsent on a plain
            // HashMap is not safe under that
            translatedCaches = new ConcurrentHashMap<>();
            sharedCache = new ConcurrentHashMap<>();
            readTranslationFile(languageSelected, languagesDirectory);
        }
    }

    private static String replaceCardName(String language, String name, String toracle) {
        String nickName = language.equals("en-US") ? Lang.getEnglishInstance().getNickName(name) : Lang.getInstance().getNickName(name);
        String result = TextUtil.fastReplace(toracle, name, "CARDNAME");
        if (!nickName.equals(name)) {
            result = TextUtil.fastReplace(result, nickName, "NICKNAME");
        }
        return result;
    }

    public static void buildOracleMapping(String faceName, String oracleText, String variantName) {
        String translationKey = faceName;
        if(variantName != null)
            translationKey = faceName + " $" + variantName;
        if (!needsTranslation() || oracleMappings.containsKey(translationKey)) return;
        String translatedText = getTranslatedOracle(translationKey);
        if (translatedText.isEmpty()) {
            // english card only, fall back
            return;
        }
        String translatedName = getTranslatedName(translationKey);
        List <Pair <String, String> > mapping = new ArrayList<>();
        String [] splitOracleText = oracleText.split("\\\\n");
        String [] splitTranslatedText = translatedText.split("\r\n\r\n");

        int offset = leadingLineOffset(splitOracleText, splitTranslatedText);

        for (int i = 0; i < splitOracleText.length; i++) {
            String toracle = replaceCardName("en-US", faceName, splitOracleText[i]);
            // Remove reminder text in English oracle text unless entire line is reminder text
            if (!toracle.startsWith("(")) {
                toracle = toracle.replaceAll("\\(.*\\)", "");
            }
            // A translated oracle can have fewer lines than the English one. Give the uncovered
            // lines an entry of their own anyway, with no translation - otherwise they match some
            // other line's entry and two abilities display the same text.
            int t = i - offset;
            String ttranslated = t >= 0 && t < splitTranslatedText.length
                    ? replaceCardName(languageSelected, translatedName, splitTranslatedText[t])
                    : null;
            mapping.add(Pair.of(toracle, ttranslated));
        }
        oracleMappings.put(translationKey, mapping);
    }

    /**
     * Aura oracles routinely reach us a line short, having dropped the leading "Enchant ..."
     * line, which leaves every remaining line paired with its neighbour's translation. Mana
     * symbols and stat changes survive translation unaltered, so where a line carries one they
     * say which pairing is right; where none does, the leading line is the one usually missing.
     *
     * @return how far the translated lines have slipped against the English ones, 0 or 1
     */
    private static int leadingLineOffset(String [] oracle, String [] translated) {
        if (oracle.length - translated.length != 1 || !oracle[0].startsWith("Enchant ")) {
            return 0;
        }
        int asIs = 0, slipped = 0;
        for (int i = 1; i < oracle.length; i++) {
            List <String> marks = translationInvariants(oracle[i]);
            if (marks.isEmpty()) {
                continue;
            }
            if (i < translated.length && marks.equals(translationInvariants(translated[i]))) {
                asIs++;
            }
            if (marks.equals(translationInvariants(translated[i - 1]))) {
                slipped++;
            }
        }
        return asIs > slipped ? 0 : 1;
    }

    /** The parts of an oracle line that read the same in every language. */
    private static List <String> translationInvariants(String line) {
        List <String> found = new ArrayList<>();
        Matcher m = TRANSLATION_INVARIANT.matcher(line);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    public static String translateMultipleDescriptionText(String descText, ITranslatable card) {
        if (!needsTranslation()) return descText;
        String [] splitDescText = descText.split("\n");
        String result = descText;
        for (String text : splitDescText) {
            text = text.trim();
            if (text.isEmpty()) continue;
            String translated = translateSingleDescriptionText(text, card);
            if (!text.equals(translated)) {
                result = TextUtil.fastReplace(result, text, translated);
            } else {
                // keywords maybe combined into one line, split them and try translate again
                String [] splitKeywords = text.split(", ");
                if (splitKeywords.length <= 1) continue;
                for (String keyword : splitKeywords) {
                    if (keyword.contains(" ")) continue;
                    translated = translateSingleDescriptionText(keyword, card);
                    if (!keyword.equals(translated)) {
                        result = TextUtil.fastReplace(result, keyword, translated);
                    }
                }
            }
        }
        return result;
    }

    public static String translateSingleDescriptionText(String descText, ITranslatable card) {
        if (descText == null)
            return "";
        if (!needsTranslation()) return descText;
        // the answer comes out of one card's oracle mapping, so it can only be reused for that
        // card - two cards sharing an ability need not word its translation the same way
        String key = card.getTranslationKey();
        Map <String, String> cardCache = translatedCaches.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        if (cardCache.containsKey(descText)) return cardCache.get(descText);

        List <Pair <String, String> > mapping = oracleMappings.get(key);
        if (mapping == null) {
            // no mapping of its own - a clone that took a different name, say - so fall back to
            // whatever another card already worked out for this exact text
            return sharedCache.getOrDefault(descText, descText);
        }
        String result = descText;
        if (!mapping.isEmpty()) {
            result = translateSingleIngameText(descText, mapping);
        }
        cardCache.put(descText, result);
        if (!result.equals(descText)) {
            sharedCache.putIfAbsent(descText, result);
        }
        return result;
    }

    private static String translateSingleIngameText(String descText, List <Pair <String, String> > mapping) {
        int candidateIndex = matchingEntry(descText, mapping);

        if (candidateIndex < mapping.size()) {
            String translated = mapping.get(candidateIndex).getRight();
            // an oracle line the translation does not cover carries no text of its own
            return translated == null ? descText : translated;
        }

        return descText;
    }

    private static int matchingEntry(String descText, List <Pair <String, String> > mapping) {
        String tcompare = descText.startsWith("(") ? descText : descText.replaceAll("\\(.*\\)", "");

        // Use Levenshtein Distance to find matching oracle text and replace it with translated text
        int candidateIndex = mapping.size();
        int minDistance = tcompare.length();
        for (int i = 0; i < mapping.size(); i++) {
            String toracle = mapping.get(i).getLeft();
            int threshold = Math.min(toracle.length(), tcompare.length()) / 3;
            int distance = StringUtils.getLevenshteinDistance(toracle, tcompare, threshold);
            if (distance != -1 && distance < minDistance) {
                minDistance = distance;
                candidateIndex = i;
            }
        }

        return candidateIndex;
    }

}
