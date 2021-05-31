package forge.game.keyword;

import forge.card.CardType;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    private String type;

    @Override
    protected void parse(String details) {
        if (CardType.isACardType(details)) {
            type = details.toLowerCase();
        } else if (details.contains(":")) {
            type = details.split(":")[0];
            if (this.toString().startsWith("Affinity")) {
                type = details.split(":")[1];
            }
        } else {
            type = details;
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, type);
    }
}
