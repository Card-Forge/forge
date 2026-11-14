package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared text helpers for Rogue Commander UI markup.
 */
public final class TextHelper {
    private static final String EFFECT_CARD_NAME_PREFIX = " - ";
    private static final int CARD_HIDDEN_GROUP = 1;
    private static final int CARD_TOKEN_GROUP = 2;
    private static final int KEYWORD_HIDDEN_GROUP = 1;
    private static final int KEYWORD_TOKEN_GROUP = 2;
    private static final int PREVIEW_CARD_TOKEN_GROUP = 2;
    private static final int PREVIEW_KEYWORD_TOKEN_GROUP = 4;
    private static final Pattern CARD_MARKER_PATTERN = Pattern.compile("(!)?\\[\\[(.+?)]]");
    private static final Pattern KEYWORD_MARKER_PATTERN = Pattern.compile("(!)?\\{\\{(.+?)}}");
    private static final Pattern PREVIEW_MARKER_PATTERN = Pattern.compile("(!)?\\[\\[(.+?)]]|(!)?\\{\\{(.+?)}}");

    private TextHelper() {
    }

    public static String stripPreviewMarkers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String withoutCardMarkers = stripCardPattern(text);
        String withoutKeywordMarkers = stripKeywordPattern(withoutCardMarkers);
        return normalizeStrippedPreviewText(withoutKeywordMarkers);
    }

    private static String stripCardPattern(String text) {
        Matcher matcher = CARD_MARKER_PATTERN.matcher(text);
        // Matcher overloads taking StringBuilder require Android API 34.
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(CARD_TOKEN_GROUP).trim();
            String replacement = matcher.group(CARD_HIDDEN_GROUP) == null
                ? extractCardNameFromReference(token)
                : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String stripKeywordPattern(String text) {
        Matcher matcher = KEYWORD_MARKER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(KEYWORD_TOKEN_GROUP).trim();
            String replacement = matcher.group(KEYWORD_HIDDEN_GROUP) == null ? token : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static List<PreviewReference> extractPreviewReferences(String text) {
        if (text == null || text.isEmpty()) {
            return ImmutableList.of();
        }

        LinkedHashMap<String, PreviewReference> cards = new LinkedHashMap<>();
        LinkedHashMap<String, PreviewReference> keywords = new LinkedHashMap<>();
        Matcher matcher = PREVIEW_MARKER_PATTERN.matcher(text);
        int order = 0;

        while (matcher.find()) {
            if (matcher.group(PREVIEW_CARD_TOKEN_GROUP) != null) {
                String token = matcher.group(PREVIEW_CARD_TOKEN_GROUP).trim();
                if (!token.isEmpty() && !cards.containsKey(token)) {
                    cards.put(token, new PreviewReference(PreviewReferenceType.CARD, token, order++));
                }
            } else if (matcher.group(PREVIEW_KEYWORD_TOKEN_GROUP) != null) {
                String token = matcher.group(PREVIEW_KEYWORD_TOKEN_GROUP).trim();
                if (!token.isEmpty() && !keywords.containsKey(token)) {
                    keywords.put(token, new PreviewReference(PreviewReferenceType.KEYWORD, token, order++));
                }
            }
        }

        if (cards.isEmpty() && keywords.isEmpty()) {
            return ImmutableList.of();
        }

        List<PreviewReference> references = new ArrayList<>(cards.size() + keywords.size());
        references.addAll(cards.values());
        references.addAll(keywords.values());
        return references;
    }

    public static String extractCardNameFromReference(String token) {
        if (StringUtils.isBlank(token)) {
            return "";
        }

        int separatorIndex = token.indexOf('|');
        return separatorIndex >= 0 ? token.substring(0, separatorIndex).trim() : token;
    }

    public static String extractEffectCardDisplayNameFromReference(String token) {
        String cardName = extractCardNameFromReference(token);
        if (cardName.isEmpty()) {
            return "";
        }

        int separatorIndex = cardName.indexOf(EFFECT_CARD_NAME_PREFIX);
        if (separatorIndex >= 0) {
            return cardName.substring(separatorIndex + EFFECT_CARD_NAME_PREFIX.length()).trim();
        }
        return cardName;
    }

    public static CardReference parseCardReference(String token) {
        String cardName = extractCardNameFromReference(token);
        if (cardName.isEmpty()) {
            return new CardReference("", null, null);
        }

        String[] referenceParts = token.split("\\|", 3);
        String setCode = referenceParts.length > 1 && !StringUtils.isBlank(referenceParts[1])
            ? referenceParts[1].trim()
            : null;
        Integer artIndex = null;
        if (referenceParts.length > 2 && !StringUtils.isBlank(referenceParts[2])) {
            try {
                artIndex = Integer.parseInt(referenceParts[2].trim());
            } catch (NumberFormatException ignored) {
                artIndex = null;
            }
        }

        return new CardReference(cardName, setCode, artIndex);
    }

    public static String formatHoursMinutesSeconds(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long hours = totalMinutes / 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static String normalizeStrippedPreviewText(String text) {
        return text
            .replaceAll("[ \\t]{2,}", " ")
            .replaceAll("\\s+([,.;:!?])", "$1")
            .replaceAll("\\(\\s+", "(")
            .replaceAll("\\s+\\)", ")")
            .trim();
    }
}
