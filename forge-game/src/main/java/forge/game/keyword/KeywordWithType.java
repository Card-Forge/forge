package forge.game.keyword;

import java.util.Arrays;

import forge.util.Lang;

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
            boolean multiple = switch(getKeyword()) {
                case AFFINITY -> true;
                default -> false;
            };
            descType = Lang.getInstance().buildValidDesc(Arrays.asList(type.split(",")), multiple);
        }

        if (descType.equalsIgnoreCase("Outlaw")) {
            reminderType = "Assassin, Mercenary, Pirate, Rogue, and/or Warlock";
        } else if (type.equalsIgnoreCase("historic permanent")) {
            reminderType = "artifact, legendary, and/or Saga permanent";
        } else {
            reminderType = descType;
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, reminderType);
    }
}
