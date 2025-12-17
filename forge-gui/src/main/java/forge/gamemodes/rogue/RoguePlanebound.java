package forge.gamemodes.rogue;

/**
 * Data for a Planebound encounter. Represents a plane with its associated Planebound
 * commander and deck.
 */
public record RoguePlanebound(String planeName, String planeboundName, String deckPath,
                              int avatarIndex, RoguePlaneboundType type) {

    @Override
    public String toString() {
        return planeName + " (" + planeboundName + ")";
    }
}
