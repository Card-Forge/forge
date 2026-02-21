package forge.gui.card;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordAction;
import forge.game.keyword.KeywordInterface;

/**
 * Platform-neutral utility for building keyword info (names + reminder text)
 * from card data. Returns raw strings with {W}-style mana symbols — callers
 * are responsible for platform-specific symbol rendering (e.g. FSkin on desktop).
 */
public final class KeywordInfoUtil {

    private KeywordInfoUtil() { }

    /** A keyword name paired with its reminder text. Both are raw strings. */
    public static class KeywordData {
        public final String name;
        public final String reminderText;

        public KeywordData(final String name, final String reminderText) {
            this.name = name;
            this.reminderText = reminderText;
        }
    }

    /**
     * Parse a comma-separated keyword key string into keyword data entries.
     * @param keywordKey the comma-separated keyword string from CardStateView
     * @param addedNames tracks already-added keyword names (lowercase) to deduplicate
     * @return list of keyword data with raw name and reminder text
     */
    public static List<KeywordData> buildKeywords(final String keywordKey,
                                                   final Set<String> addedNames) {
        final String[] tokens = keywordKey.split(",");
        final Set<Keyword> seen = new LinkedHashSet<>();
        final List<KeywordData> result = new ArrayList<>();

        for (final String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                final KeywordInterface inst = Keyword.getInstance(token);
                final Keyword kw = inst.getKeyword();
                if (kw == Keyword.UNDEFINED || kw == Keyword.ENCHANT) {
                    continue;
                }
                if (!seen.add(kw)) {
                    continue; // deduplicate
                }
                String reminderText;
                try {
                    reminderText = inst.getReminderText();
                } catch (Exception ex) {
                    reminderText = "";
                }
                final String name = colorNamesToSymbols(inst.getTitle());
                result.add(new KeywordData(name, reminderText));
                addedNames.add(kw.toString().toLowerCase());
            } catch (Exception e) {
                // Skip malformed keyword tokens
            }
        }
        return result;
    }

    /**
     * Scan oracle text for keyword actions that aren't already shown as keyword
     * abilities, and append them to the keyword list.
     */
    public static void addKeywordActions(final List<KeywordData> result,
                                          final String oracleText,
                                          final Set<String> existingNames,
                                          final String cardName) {
        if (oracleText == null || oracleText.isEmpty()) {
            return;
        }
        // Strip card name to avoid false positives (e.g., "Boseiju, Who Endures")
        String lowerText = oracleText.toLowerCase();
        if (!cardName.isEmpty()) {
            lowerText = lowerText.replace(cardName.toLowerCase(), "");
        }
        // Collect matches with their position in the oracle text, then sort by position
        // so keyword actions appear top-to-bottom in card text order
        final List<int[]> pendingIndices = new ArrayList<>(); // [oraclePos, enumOrdinal]
        for (final KeywordAction action : KeywordAction.values()) {
            if (action.basic) {
                continue;
            }
            final String name = action.getDisplayName();
            if (existingNames.contains(name.toLowerCase())) {
                continue;
            }
            // Match whole word (case-insensitive): "goad", "goads", "goaded"
            final String lowerName = name.toLowerCase();
            // Try base form first, then -ies conjugation for -y verbs (scry → scries)
            String matchTerm = lowerName;
            if (!lowerText.contains(matchTerm) && lowerName.endsWith("y")) {
                matchTerm = lowerName.substring(0, lowerName.length() - 1) + "ies";
            }
            if (lowerText.contains(matchTerm)) {
                // Verify word boundaries: start must not be preceded by a letter,
                // and end must be followed by a verb suffix (s/d/ed/es/ing) or
                // non-letter — prevents "planeswalk" matching "planeswalker"
                int idx = lowerText.indexOf(matchTerm);
                while (idx >= 0) {
                    final boolean startOk = idx == 0
                            || !Character.isLetter(lowerText.charAt(idx - 1));
                    final int endIdx = idx + matchTerm.length();
                    final boolean endOk = endIdx >= lowerText.length()
                            || !Character.isLetter(lowerText.charAt(endIdx))
                            || isVerbSuffix(lowerText, endIdx);
                    if (startOk && endOk) {
                        pendingIndices.add(new int[]{idx, action.ordinal()});
                        existingNames.add(lowerName);
                        break;
                    }
                    idx = lowerText.indexOf(matchTerm, idx + 1);
                }
            }
        }
        pendingIndices.sort((a, b) -> Integer.compare(a[0], b[0]));
        final KeywordAction[] allActions = KeywordAction.values();
        for (final int[] pair : pendingIndices) {
            final KeywordAction action = allActions[pair[1]];
            final String lowerName = action.getDisplayName().toLowerCase();
            String displayName = action.getDisplayName();
            String reminder = action.getReminderText();
            if (reminder.contains("N")) {
                int pos = pair[0] + lowerName.length();
                // Skip inflected suffix letters (s, ed, ing, etc.)
                while (pos < lowerText.length()
                        && Character.isLetter(lowerText.charAt(pos))) {
                    pos++;
                }
                // Skip whitespace to reach the number
                if (pos < lowerText.length()
                        && lowerText.charAt(pos) == ' ') {
                    pos++;
                    final boolean isDigit = Character.isDigit(lowerText.charAt(pos));
                    final String resolved = parseNumber(lowerText, pos);
                    if (resolved != null) {
                        reminder = reminder.replace("N", resolved);
                        // Only add number to heading when the card text uses a digit
                        // (e.g. "scry 2") — spelled-out numbers ("mill three") stay generic
                        if (isDigit) {
                            displayName = displayName + " " + resolved;
                        }
                        if ("1".equals(resolved)) {
                            reminder = reminder.replace("counters", "counter");
                        }
                    } else {
                        // Can't resolve number (e.g. "mill half their library")
                        // — drop reminder text rather than showing literal "N"
                        reminder = "";
                    }
                }
            }
            result.add(new KeywordData(displayName, reminder));
        }
    }

    /**
     * Cross-check CardStateView boolean keyword flags against already-parsed
     * keywords. Adds any keywords whose flag is true but were missed by
     * keywordKey string parsing.
     */
    public static void addMissingKeywordsFromFlags(final List<KeywordData> result,
                                                    final CardStateView state,
                                                    final Set<String> addedNames) {
        if (state == null) {
            return;
        }
        addFlagKeyword(result, addedNames, state.hasFlying(), Keyword.FLYING);
        addFlagKeyword(result, addedNames, state.hasFirstStrike(), Keyword.FIRST_STRIKE);
        addFlagKeyword(result, addedNames, state.hasDoubleStrike(), Keyword.DOUBLE_STRIKE);
        addFlagKeyword(result, addedNames, state.hasDeathtouch(), Keyword.DEATHTOUCH);
        addFlagKeyword(result, addedNames, state.hasDefender(), Keyword.DEFENDER);
        addFlagKeyword(result, addedNames, state.hasFear(), Keyword.FEAR);
        addFlagKeyword(result, addedNames, state.hasHaste(), Keyword.HASTE);
        addFlagKeyword(result, addedNames, state.hasHexproof(), Keyword.HEXPROOF);
        addFlagKeyword(result, addedNames, state.hasIndestructible(), Keyword.INDESTRUCTIBLE);
        addFlagKeyword(result, addedNames, state.hasIntimidate(), Keyword.INTIMIDATE);
        addFlagKeyword(result, addedNames, state.hasLifelink(), Keyword.LIFELINK);
        addFlagKeyword(result, addedNames, state.hasMenace(), Keyword.MENACE);
        addFlagKeyword(result, addedNames, state.hasReach(), Keyword.REACH);
        addFlagKeyword(result, addedNames, state.hasShadow(), Keyword.SHADOW);
        addFlagKeyword(result, addedNames, state.hasShroud(), Keyword.SHROUD);
        addFlagKeyword(result, addedNames, state.hasTrample(), Keyword.TRAMPLE);
        addFlagKeyword(result, addedNames, state.hasVigilance(), Keyword.VIGILANCE);
        addFlagKeyword(result, addedNames, state.hasInfect(), Keyword.INFECT);
        addFlagKeyword(result, addedNames, state.hasWither(), Keyword.WITHER);
        addFlagKeyword(result, addedNames, state.hasHorsemanship(), Keyword.HORSEMANSHIP);
    }

    /**
     * Sort keywords so they appear in the same order they are mentioned in the
     * oracle text, rather than alphabetical or parse order.
     */
    public static void sortByOracleText(final List<KeywordData> keywords,
                                         final String oracleText) {
        if (oracleText == null || oracleText.isEmpty() || keywords.size() <= 1) {
            return;
        }
        final String lowerText = oracleText.toLowerCase();
        keywords.sort((a, b) -> {
            final int posA = findKeywordPosition(lowerText, a.name);
            final int posB = findKeywordPosition(lowerText, b.name);
            return Integer.compare(posA, posB);
        });
    }

    // --- Private helpers ---

    private static void addFlagKeyword(final List<KeywordData> result,
                                        final Set<String> addedNames,
                                        final boolean hasFlag, final Keyword kw) {
        if (!hasFlag) {
            return;
        }
        if (addedNames.contains(kw.toString().toLowerCase())) {
            return;
        }
        addedNames.add(kw.toString().toLowerCase());
        result.add(new KeywordData(kw.toString(), kw.getReminderText()));
    }

    private static int findKeywordPosition(final String lowerOracleText,
                                            final String keywordName) {
        // Strip markup (e.g. {W} symbols) to get plain text for matching
        final String plain = keywordName.replaceAll("\\{[^}]+}", "").trim().toLowerCase();
        int pos = lowerOracleText.indexOf(plain);
        if (pos >= 0) {
            return pos;
        }
        // Try first word only (e.g. "bestow" from "Bestow {2}{W}{U}{B}{R}{G}")
        final int space = plain.indexOf(' ');
        if (space > 0) {
            pos = lowerOracleText.indexOf(plain.substring(0, space));
            if (pos >= 0) {
                return pos;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Replace standalone MTG color names with mana symbols for display. */
    private static String colorNamesToSymbols(final String text) {
        return text
                .replace("white", "{W}").replace("White", "{W}")
                .replace("blue", "{U}").replace("Blue", "{U}")
                .replace("black", "{B}").replace("Black", "{B}")
                .replace("red", "{R}").replace("Red", "{R}")
                .replace("green", "{G}").replace("Green", "{G}");
    }

    /** Parse a digit or number word at the given position. Returns the digit string or null. */
    private static String parseNumber(final String text, final int pos) {
        // Try digits first: "3", "10"
        int numEnd = pos;
        while (numEnd < text.length() && Character.isDigit(text.charAt(numEnd))) {
            numEnd++;
        }
        if (numEnd > pos) {
            return text.substring(pos, numEnd);
        }
        // Try number words: "one" through "twenty"
        final String[] words = {
            "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
            "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
        };
        for (int i = 0; i < words.length; i++) {
            if (text.startsWith(words[i], pos)) {
                final int end = pos + words[i].length();
                if (end >= text.length() || !Character.isLetter(text.charAt(end))) {
                    return String.valueOf(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Check if the text at {@code pos} starts with a common English verb suffix
     * (s, d, ed, es, ing) followed by a non-letter. This allows matching
     * "goads"/"goaded"/"goading" but rejects "planeswalker" (suffix "er").
     */
    private static boolean isVerbSuffix(final String text, final int pos) {
        final String[] suffixes = {"ing", "ed", "es", "s", "d"};
        for (final String suffix : suffixes) {
            if (text.startsWith(suffix, pos)) {
                final int end = pos + suffix.length();
                if (end >= text.length() || !Character.isLetter(text.charAt(end))) {
                    return true;
                }
            }
        }
        return false;
    }
}
