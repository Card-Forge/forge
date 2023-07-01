package forge.gamemodes.limited;

import forge.util.Localizer;

public enum LimitedPoolType {
    Full(Localizer.getInstance().getMessage("lblLimitedPoolFull")),
    Block(Localizer.getInstance().getMessage("lblLimitedBlock")),
    Prerelease(Localizer.getInstance().getMessage("lblLimitedPrerelease"), false),
    FantasyBlock(Localizer.getInstance().getMessage("lblLimitedFantasy")),
    Custom(Localizer.getInstance().getMessage("lblLimitedCustom")),
    Chaos(Localizer.getInstance().getMessage("lblLimitedChaos"));

    private final String displayName;
    private final boolean draftable;

    LimitedPoolType(String name) {
        this(name, true);
    }

    LimitedPoolType(String name, boolean draftable) {
        this.draftable = draftable;
        displayName = name;
    }

    public static LimitedPoolType[] values(boolean draftable) {
        if (!draftable) {
            return values();
        }

        int n = 0;
        for (LimitedPoolType lpt : values()) {
            if (lpt.draftable) {
                n++;
            }
        }
        LimitedPoolType[] draftableFormats = new LimitedPoolType[n];
        n = 0;
        for (LimitedPoolType lpt : values()) {
            if (lpt.draftable) {
                draftableFormats[n] = lpt;
                n++;
            }
        }
        return draftableFormats;
    }

    @Override
    public String toString() {
        return displayName;
    }
}