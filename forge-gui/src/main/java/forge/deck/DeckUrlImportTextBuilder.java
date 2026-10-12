package forge.deck;

import java.util.Locale;
import java.util.regex.Pattern;

final class DeckUrlImportTextBuilder {
    private static final Pattern RECOGNIZED_COLLECTOR_NUMBER = Pattern.compile(DeckRecognizer.REX_COLL_NUMBER);

    private final StringBuilder text = new StringBuilder();
    private DeckSection currentSection;

    void add(final DeckSection section, final int quantity, final String cardName,
            final String setCode, final String collectorNumber) {
        if (currentSection != section) {
            text.append(section.name()).append('\n');
            currentSection = section;
        }
        text.append(quantity).append(' ').append(cardName);
        if (setCode != null) {
            text.append(" [").append(setCode.toUpperCase(Locale.ROOT)).append(']');
            if (collectorNumber != null && RECOGNIZED_COLLECTOR_NUMBER.matcher(collectorNumber).matches()) {
                text.append(' ').append(collectorNumber);
            }
        }
        text.append('\n');
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
