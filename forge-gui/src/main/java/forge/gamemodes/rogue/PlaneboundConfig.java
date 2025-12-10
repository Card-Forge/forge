package forge.gamemodes.rogue;

/**
 * Configuration data for a Planebound encounter.
 * Represents a plane with its associated Planebound commander and deck.
 */
public class PlaneboundConfig {
    private final String planeName;
    private final String planeboundName;
    private final String deckPath;

    /**
     * Create a new Planebound configuration.
     * @param planeName Name of the plane (e.g., "Bloodhill Bastion")
     * @param planeboundName Name of the Planebound commander (e.g., "Lyzolda, the Blood Witch")
     * @param deckPath Relative path to the Planebound deck file (e.g., "rogue/planebounds/lyzolda.dck")
     */
    public PlaneboundConfig(String planeName, String planeboundName, String deckPath) {
        this.planeName = planeName;
        this.planeboundName = planeboundName;
        this.deckPath = deckPath;
    }

    public String getPlaneName() {
        return planeName;
    }

    public String getPlaneboundName() {
        return planeboundName;
    }

    public String getDeckPath() {
        return deckPath;
    }

    @Override
    public String toString() {
        return planeName + " (" + planeboundName + ")";
    }
}
