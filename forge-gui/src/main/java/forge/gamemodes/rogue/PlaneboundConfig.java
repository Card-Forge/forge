package forge.gamemodes.rogue;

/**
 * Configuration data for a Planebound encounter. Represents a plane with its associated Planebound
 * commander and deck.
 */
public record PlaneboundConfig(String planeName, String planeboundName, String deckPath,
                               int avatarIndex) {

    @Override
    public String toString() {
        return planeName + " (" + planeboundName + ")";
    }
}
