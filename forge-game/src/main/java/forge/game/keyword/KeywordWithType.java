package forge.game.keyword;

import forge.card.CardType;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    protected String type = null;
    protected String descType = null;
    protected String reminderType = null;

    public String getValidType() { return type; }
    public String getTypeDescription() { return descType; }

    @Override
    protected void parse(String details) {
        String k[];
        if (details.contains(":")) {
            switch (getKeyword()) {
            case AFFINITY:
            case BANDSWITH:
            case ENCHANT:
            case HEXPROOF:
            case LANDWALK:
                k = details.split(":");
                type = k[0];
                descType = k[1];
                break;
            default:
                k = details.split(":");
                type = k[1];
                descType = k[0];
            }
        } else {
            descType = type = details;
        }

        if (CardType.isACardType(descType) || "Permanent".equals(descType) || "Player".equals(descType) || "Opponent".equals(descType)) {
            descType = descType.toLowerCase();
        } else if (descType.equalsIgnoreCase("Outlaw")) {
            reminderType = "Assassin, Mercenary, Pirate, Rogue, and/or Warlock";
        } else if (type.equalsIgnoreCase("historic permanent")) {
            reminderType = "artifact, legendary, and/or Saga permanent";
        }
        if (reminderType == null) {
            reminderType = type;
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, reminderType);
    }
}
