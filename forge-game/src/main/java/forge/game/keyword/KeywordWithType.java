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
                type = details.split(":")[1];
                // type lists defined by rules should not be changed by TextChange in reminder text 
                if (type.equalsIgnoreCase("Outlaw")) {
                    type = "Assassin, Mercenary, Pirate, Rogue, and/or Warlock";
                } else if (type.equalsIgnoreCase("historic permanent")) {
                    type = "artifact, legendary, and/or Saga permanent";
                }
                break;
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
