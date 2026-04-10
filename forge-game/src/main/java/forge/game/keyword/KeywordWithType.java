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
        switch (getKeyword()) {
            case LANDWALK:
                // "Swampwalk", "Islandwalk", etc. instead of "Landwalk Swamp"
                return Character.toUpperCase(descType.charAt(0))
                        + descType.substring(1) + "walk";
            case AFFINITY:
                // "Affinity for artifacts" instead of "Affinity artifact".
                // Some types are already plural (e.g. "Plains"), so skip
                // pluralisation if descType already ends in "s".
                final String affinityPlural = descType.endsWith("s")
                        ? descType : Lang.getPlural(descType);
                return "Affinity for " + affinityPlural;
            default:
                return this.getKeyword() + " " + descType;
        }
    }

    @Override
    protected void parse(String details) {
        String k[];
        if (details.contains(":")) {
            k = details.split(":");
            type = k[0];
            descType = k[1];
        } else {
            MagicColor.Color color = MagicColor.Color.fromName(details);
            if (color != null) {
                type = "Card." + StringUtils.capitalize(color.getName());
                descType = color.getName();
            } else {
                type = details;
                descType = Lang.getInstance().buildValidDesc(Arrays.asList(type.split(",")), false);
            }
        }

        reminderType = descType;
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, reminderType);
    }
}
