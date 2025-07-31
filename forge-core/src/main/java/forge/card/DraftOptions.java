package forge.card;

public class DraftOptions {
    public enum DoublePick {
        NEVER,
        FIRST_PICK, // only first pick each pack
        ALWAYS // each time you receive a pack, you can pick two cards
    };
    public enum DeckType {
        Normal, // Standard deck, usually 40 cards
        Commander // Special deck type for Commander format. Important for selection/construction
    }

    private DoublePick doublePick = DoublePick.NEVER;
    private final int maxPodSize; // Usually 8, but could be smaller for cubes. I guess it could be larger too
    private final int recommendedPodSize; // Usually 8, but is 4 for new double pick
    private final int maxMatchPlayers; // Usually 2, but 4 for things like Commander or Conspiracy
    private final DeckType deckType; // Normal or Commander
    private final String freeCommander;

    public DraftOptions(String doublePickOption, int maxPodSize, int recommendedPodSize, int maxMatchPlayers, String deckType, String freeCommander) {
        this.maxPodSize = maxPodSize;
        this.recommendedPodSize = recommendedPodSize;
        this.maxMatchPlayers = maxMatchPlayers;
        this.deckType = DeckType.valueOf(deckType);
        this.freeCommander = freeCommander;
        if (doublePickOption != null) {
            switch (doublePickOption.toLowerCase()) {
                case "firstpick":
                    doublePick = DoublePick.FIRST_PICK;
                    break;
                case "always":
                    doublePick = DoublePick.ALWAYS;
                    break;
            }
        }

    }
    public int getMaxPodSize() {
        return maxPodSize;
    }
    public int getRecommendedPodSize() {
        return recommendedPodSize;
    }
    public DoublePick getDoublePick() {
        return doublePick;
    }
    public int getMaxMatchPlayers() {
        return maxMatchPlayers;
    }
    public DeckType getDeckType() {
        return deckType;
    }
    public String getFreeCommander() {
        return freeCommander;
    }
}
