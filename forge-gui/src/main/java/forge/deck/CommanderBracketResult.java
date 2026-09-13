package forge.deck;

import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

record CommanderBracketResult(String deckHash, int bracket, String bracketName, String bracketDescription, String bracketReason,
                              String bracketNarrative, String confidence, String confidenceReason, String estimatedWinTurn,
                              int totalGameChangers, int fastManaCount, int tutorCount, int comboCount, int cardsFound, int totalCards) {
    private static final Localizer localizer = Localizer.getInstance();

    static CommanderBracketResult fromResponse(final String deckHash, final String response) throws IOException {
        final Object parsed = JsonUtil.parse(response);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Unexpected CommanderBracket response.");
        }

        final Map<?, ?> bracketAnalysis = asMap(root.get("bracket_analysis"));
        final Map<?, ?> deckStats = asMap(root.get("deck_stats"));
        final int bracket = coerceBracket(root, bracketAnalysis);
        if (bracket < 1 || bracket > 5) {
            throw new IOException("CommanderBracket response did not contain a valid bracket.");
        }

        return new CommanderBracketResult(deckHash, bracket,
                sanitizeBracketLabel(firstString(root, bracketAnalysis, "bracket_name"), bracket),
                sanitizeBracketLabel(firstString(root, bracketAnalysis, "bracket_description"), bracket),
                firstString(root, bracketAnalysis, "bracket_reason"), firstString(root, bracketAnalysis, "bracket_narrative"),
                firstString(root, bracketAnalysis, "bracket_confidence"),
                sanitizeBracketLabel(firstString(root, bracketAnalysis, "confidence_reason"), bracket),
                firstString(root, bracketAnalysis, "estimated_win_turn"), intValue(bracketAnalysis.get("total_game_changers")),
                intValue(bracketAnalysis.get("fast_mana_count")), intValue(bracketAnalysis.get("tutor_count")), countCombos(root),
                intValue(deckStats.get("cards_found")), intValue(deckStats.get("total_cards")));
    }

    static CommanderBracketResult fromCachedBracket(final String deckHash, final int bracket) {
        return new CommanderBracketResult(deckHash, bracket, "", "", "", "", "", "", "", 0, 0, 0, 0, 0, 0);
    }

    void appendExplanation(final StringBuilder sb) {
        appendEstimate(sb);
        if (StringUtils.isNotBlank(bracketName)) {
            sb.append(" - ").append(bracketName);
        }
        sb.append("\n");
        appendLine(sb, localizer.getMessage("lblCommanderBracketDescriptionLabel"), bracketDescription);
        appendLine(sb, localizer.getMessage("lblCommanderBracketReasonLabel"), bracketReason);
        appendLine(sb, localizer.getMessage("lblCommanderBracketNarrativeLabel"), bracketNarrative);
        appendLine(sb, localizer.getMessage("lblCommanderBracketEstimatedWinTurnLabel"), estimatedWinTurn);
        appendLine(sb, localizer.getMessage("lblCommanderBracketConfidenceLabel"), confidence);
        appendLine(sb, localizer.getMessage("lblCommanderBracketConfidenceReasonLabel"), confidenceReason);
        if (cardsFound > 0 || totalCards > 0) {
            sb.append(localizer.getMessage("lblCommanderBracketCardsFound", cardsFound, totalCards)).append("\n");
        }
        if (hasSignalDetails()) {
            sb.append(localizer.getMessage("lblCommanderBracketSignals", totalGameChangers, fastManaCount, tutorCount, comboCount)).append("\n\n");
        }
        sb.append(localizer.getMessage("lblCommanderBracketAttribution"));
    }

    void appendEstimate(final StringBuilder sb) {
        sb.append(localizer.getMessage("lblCommanderBracketAppEstimate", bracket));
    }

    boolean hasDetails() {
        return StringUtils.isNotBlank(bracketName) || StringUtils.isNotBlank(bracketDescription) || StringUtils.isNotBlank(bracketReason)
                || StringUtils.isNotBlank(bracketNarrative) || StringUtils.isNotBlank(confidence) || StringUtils.isNotBlank(confidenceReason)
                || StringUtils.isNotBlank(estimatedWinTurn) || cardsFound > 0 || totalCards > 0 || hasSignalDetails();
    }

    private boolean hasSignalDetails() {
        return totalGameChangers > 0 || fastManaCount > 0 || tutorCount > 0 || comboCount > 0;
    }

    private static void appendLine(final StringBuilder sb, final String label, final String value) {
        if (StringUtils.isNotBlank(value)) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    private static Map<?, ?> asMap(final Object value) {
        return value instanceof Map<?, ?> map ? map : Collections.emptyMap();
    }

    private static String firstString(final Map<?, ?> root, final Map<?, ?> nested, final String key) {
        final String fromRoot = stringValue(root.get(key));
        return StringUtils.isNotBlank(fromRoot) ? fromRoot : stringValue(nested.get(key));
    }

    private static int firstInt(final Map<?, ?> values, final String... keys) {
        for (final String key : keys) {
            final int value = intValue(values.get(key));
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static int coerceBracket(final Map<?, ?> root, final Map<?, ?> bracketAnalysis) {
        // The public endpoint has returned both nested and top-level names; retain compatibility with those response shapes.
        final int nestedEstimate = firstInt(bracketAnalysis, "final_bracket", "bracket", "overall_bracket", "estimated_bracket");
        if (nestedEstimate > 0) {
            return nestedEstimate;
        }
        final int rootEstimate = firstInt(root, "final_bracket", "bracket", "overall_bracket", "estimated_bracket");
        if (rootEstimate > 0) {
            return rootEstimate;
        }
        final int rootDeckBracket = intValue(root.get("deck_bracket"));
        if (rootDeckBracket > 0) {
            return rootDeckBracket;
        }
        return intValue(bracketAnalysis.get("deck_bracket"));
    }

    private static String sanitizeBracketLabel(final String value, final int bracket) {
        // Explanatory labels have occasionally described a different bracket than the numeric model result.
        final int labeledBracket = findLabeledBracket(value);
        return labeledBracket > 0 && labeledBracket != bracket ? "" : value;
    }

    private static int findLabeledBracket(final String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        final String lowerValue = value.toLowerCase(Locale.ROOT);
        for (int i = 1; i <= 5; i++) {
            if (lowerValue.contains("bracket " + i)) {
                return i;
            }
        }
        return 0;
    }

    private static int countCombos(final Map<?, ?> root) {
        final Object combos = asMap(asMap(root.get("ipom_analysis")).get("combos")).get("detected_combos");
        return combos instanceof List<?> list ? list.size() : 0;
    }

    private static String stringValue(final Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        return String.valueOf(value);
    }

    private static int intValue(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            }
            catch (final NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
