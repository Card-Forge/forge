package forge.gamemodes.limited;

import forge.util.Localizer;

public enum LimitedPoolType {
    Full(Localizer.getInstance().getMessage("lblLimitedPoolFull")),
    Block(Localizer.getInstance().getMessage("lblLimitedBlock")),
    Prerelease(Localizer.getInstance().getMessage("lblLimitedPrerelease")),
    FantasyBlock(Localizer.getInstance().getMessage("lblLimitedFantasy")),
    Custom(Localizer.getInstance().getMessage("lblLimitedCustom")),
    Chaos(Localizer.getInstance().getMessage("lblLimitedChaos"));
    
    private final String displayName;
    LimitedPoolType(String name) {
        displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}