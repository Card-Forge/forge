package forge.game.keyword;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import forge.card.MagicColor;
import forge.util.Lang;

public class KeywordWithType extends KeywordInstance<KeywordWithType> implements KeywordWithTypeInterface {
    protected String type = null;
    protected String descType = null;
    protected String reminderType = null;

    @Override
    public String getValidType() { return type; }
    @Override
    public String getTypeDescription() { return descType; }

    @Override
    public String getTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getKeyword()).append(" ").append(descType);
        return sb.toString();
    }

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
            MagicColor.Color color = MagicColor.Color.fromName(details);
            if (color != MagicColor.Color.COLORLESS) {
                type = "Card." + StringUtils.capitalize(color.getName());
                descType = color.getName();
            } else {
                descType = type = details;
                boolean multiple = switch(getKeyword()) {
                    case AFFINITY -> true;
                    default -> false;
                };
                descType = Lang.getInstance().buildValidDesc(Arrays.asList(type.split(",")), multiple);
            }
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
