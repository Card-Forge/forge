package forge.util;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CardLanguageIndex {

    private static volatile CardLanguageIndex instance;

    private final Map<String, Map<String, Set<String>>> index;

    public static CardLanguageIndex instance() {
        CardLanguageIndex result = instance;
        if (result == null) {
            synchronized (CardLanguageIndex.class) {
                result = instance;
                if (result == null) {
                    instance = result = new CardLanguageIndex(ForgeConstants.CARD_LANGUAGES_FILE);
                }
            }
        }
        return result;
    }

    CardLanguageIndex(String path) {
        this.index = load(path);
    }

    private static Map<String, Map<String, Set<String>>> load(String path) {
        final Map<String, Map<String, Set<String>>> result = new HashMap<>();
        final File file = new File(path);
        if (!file.isFile()) {
            System.err.println("CardLanguageIndex: no language index found at " + path
                    + " - all cards will be treated as English-only.");
            return result;
        }

        for (String line : FileUtil.readFile(file)) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            final int slash = line.indexOf('/');
            final int eq = line.indexOf('=');
            if (slash <= 0 || eq <= slash) {
                continue;
            }

            final String setCode = line.substring(0, slash).toLowerCase();
            final String collector = line.substring(slash + 1, eq);
            final Set<String> langs = new HashSet<>();
            for (String lang : line.substring(eq + 1).split(",")) {
                final String trimmed = lang.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    langs.add(trimmed);
                }
            }
            if (langs.isEmpty()) {
                continue;
            }

            result.computeIfAbsent(setCode, k -> new HashMap<>()).put(collector, langs);
        }
        return result;
    }

    public boolean isAvailableInLanguage(String setCode, String collectorNumber, String langCode) {
        if (langCode == null || langCode.isEmpty() || "en".equalsIgnoreCase(langCode)) {
            return true;
        }
        final Map<String, Set<String>> collectors = index.get(setCode.toLowerCase());
        if (collectors == null) {
            return false;
        }
        final Set<String> langs = collectors.get(collectorNumber);
        return langs != null && langs.contains(langCode.toLowerCase());
    }

    public static String getPreferredCardLangCode() {
        String pref = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG);
        if (pref == null || pref.isEmpty() || "en".equalsIgnoreCase(pref)) {
            return null;
        }
        return pref;
    }

    public static String resolvePreferredLangCode(String setCode, String collectorNumber, String defaultLangCode) {
        String preferred = getPreferredCardLangCode();
        if (preferred == null || preferred.equalsIgnoreCase(defaultLangCode)) {
            return defaultLangCode;
        }
        if (instance().isAvailableInLanguage(setCode, collectorNumber, preferred)) {
            return preferred;
        }
        return defaultLangCode;
    }
}
