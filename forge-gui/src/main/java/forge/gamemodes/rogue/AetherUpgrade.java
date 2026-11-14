package forge.gamemodes.rogue;

/**
 * Defines the progressive Aether Upgrades purchasable with Sparks & Echoes.
 * Upgrades must be purchased in order (1 → 2 → 3...).
 */
public enum AetherUpgrade {
    LEVEL_1(1, "Aether Upgrade 1", "Increase Aether Energy capacity by 1.", 1, 20, 1, 0),
    LEVEL_2(2, "Aether Upgrade 2", "Unlock new Aetherworks.", 2, 30,  0, 0),
    LEVEL_3(3, "Aether Upgrade 3", "Increase Aether Energy capacity by 1.", 3, 40,  1, 0),
    LEVEL_4(4, "Aether Upgrade 4", "Aetherworks gain +1 maximum rank.", 4, 50, 0, 1);

    public final int level;
    public final String name;
    public final String description;
    public final int sparkCost;
    public final int echoCost;
    public final int extraEnergy;       // How many extra active slots this upgrade adds
    public final int extraEffectRanks;  // How many extra max ranks this upgrade adds to effects

    AetherUpgrade(int level, String name, String description, int sparkCost,
                  int echoCost, int extraEnergy, int extraEffectRanks) {
        this.level = level;
        this.name = name;
        this.description = description;
        this.sparkCost = sparkCost;
        this.echoCost = echoCost;
        this.extraEnergy = extraEnergy;
        this.extraEffectRanks = extraEffectRanks;
    }

    public static AetherUpgrade forLevel(int level) {
        for (AetherUpgrade u : values()) {
            if (u.level == level) return u;
        }
        return null;
    }

    public static int getMaxLevel() {
        return values().length;
    }
}
