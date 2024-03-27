package forge.game.keyword;

import forge.card.CardType;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    protected String type;

    @Override
    protected void parse(String details) {
        if (CardType.isACardType(details)) {
            type = details.toLowerCase();
        } else if (details.contains(":")) {
            switch (getKeyword()) {
            case AFFINITY:
            case HEXPROOF:
            case LANDWALK:
                type = details.split(":")[1];
                break;
            default:
                type = details.split(":")[0];
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
