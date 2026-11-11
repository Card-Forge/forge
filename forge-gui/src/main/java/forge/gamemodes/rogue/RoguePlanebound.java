package forge.gamemodes.rogue;

/**
 * Data for a Planebound encounter. Represents a plane with its associated Planebound
 * commander and deck.
 *
 * Note: This is a regular class instead of a record to support XStream serialization.
 */
public class RoguePlanebound {
    private final String planeName;
    private final String planeboundName;
    private final String deckPath;
    private final int avatarIndex;
    private final RoguePlaneboundType type;

    public RoguePlanebound(String planeName, String planeboundName, String deckPath,
                           int avatarIndex, RoguePlaneboundType type) {
        this.planeName = planeName;
        this.planeboundName = planeboundName;
        this.deckPath = deckPath;
        this.avatarIndex = avatarIndex;
        this.type = type;
    }

    public String planeName() {
        return planeName;
    }

    public String planeboundName() {
        return planeboundName;
    }

    public String deckPath() {
        return deckPath;
    }

    public int avatarIndex() {
        return avatarIndex;
    }

    public RoguePlaneboundType type() {
        return type;
    }

    @Override
    public String toString() {
        return planeName + " (" + planeboundName + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoguePlanebound other)) return false;
        return deckPath.equals(other.deckPath);
    }

    @Override
    public int hashCode() {
        return deckPath.hashCode();
    }
}
