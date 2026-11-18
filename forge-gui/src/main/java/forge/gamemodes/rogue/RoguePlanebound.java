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
    private final int minRow;

    public RoguePlanebound(String planeName, String planeboundName, String deckPath,
                           int avatarIndex, RoguePlaneboundType type, int minRow) {
        this.planeName = planeName;
        this.planeboundName = planeboundName;
        this.deckPath = deckPath;
        this.avatarIndex = avatarIndex;
        this.type = type;
        this.minRow = minRow;
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

    /** Minimum one-based Planebound row; zero means unrestricted. */
    public int minRow() {
        return minRow;
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
