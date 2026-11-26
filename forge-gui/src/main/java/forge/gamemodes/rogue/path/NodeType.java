package forge.gamemodes.rogue.path;

import forge.util.Localizer;

public enum NodeType {
    PLANEBOUND, SANCTUM, BAZAAR, EVENT, CHEST;

    /** What a side node offers, as its map tooltip says. Null for a plane. */
    public String sideTooltip() {
        return switch (this) {
            case PLANEBOUND -> null;
            case SANCTUM -> Localizer.getInstance().getMessage("lblRogueSanctumTooltip");
            case BAZAAR -> Localizer.getInstance().getMessage("lblRogueBazaarTooltip");
            case EVENT -> Localizer.getInstance().getMessage("lblRogueEventTooltip");
            case CHEST -> Localizer.getInstance().getMessage("lblRogueChestTooltip");
        };
    }
}
