package forge.game.keyword;

import forge.card.CardType;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    protected String type;

    @Override
    protected void parse(String details) {
        if (CardType.isACardType(details)) {
            type = details.toLowerCase();
        } else if (details.contains(":")) {
            String[] detailSpl = details.split(":");
            switch (getKeyword()) {
            case AFFINITY:
                type = (detailSpl.length > 2) ? detailSpl[2] : detailSpl[1];
                break;
            case HEXPROOF:
            case LANDWALK:
                type = detailSpl[1];
                break;
            default:
                type = detailSpl[0];
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
