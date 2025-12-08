package forge.gamemodes.rogue;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Rogue Deck configuration: a Commander with a starting deck
 * and reward pool for progressive deck building during runs.
 */
public class RogueDeckData {

    private String name;                    // Display name (e.g., "Aegar, the Freezing Flame")
    private String commanderCardName;       // Commander card name
    private Deck startDeck;                 // 40-45 card starting deck (includes commander)
    private CardPool rewardPool;            // ~120 card pool for rewards during run
    private String description;             // Flavor text for UI
    private String themeDescription;        // Theme/archetype (e.g., "Instants/Sorceries matter")

    // Constructors
    public RogueDeckData() {
        this.rewardPool = new CardPool();
    }

    public RogueDeckData(String name, String commanderCardName) {
        this.name = name;
        this.commanderCardName = commanderCardName;
        this.rewardPool = new CardPool();
    }

    // Factory method to create a deep copy of the start deck
    public Deck createStartingDeck() {
        if (startDeck == null) {
            return null;
        }
        // Create a deep copy to avoid modifying the original
        return new Deck(startDeck);
    }

    /**
     * Draw random cards from the reward pool for post-match rewards.
     * @param count Number of cards to draw
     * @return List of random cards (may be less than count if pool is small)
     */
    public List<PaperCard> drawRewardOptions(int count) {
        if (rewardPool == null || rewardPool.isEmpty()) {
            return new ArrayList<>();
        }

        List<PaperCard> allCards = rewardPool.toFlatList();
        if (allCards.size() <= count) {
            return new ArrayList<>(allCards);
        }

        // Shuffle and take first 'count' cards
        List<PaperCard> result = new ArrayList<>(allCards);
        java.util.Collections.shuffle(result);
        return result.subList(0, Math.min(count, result.size()));
    }

    /**
     * Remove cards from the reward pool (used when cards are selected as rewards).
     * @param cards Cards to remove
     */
    public void removeFromRewardPool(Iterable<PaperCard> cards) {
        if (rewardPool != null) {
            for (PaperCard card : cards) {
                rewardPool.remove(card);
            }
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommanderCardName() {
        return commanderCardName;
    }

    public void setCommanderCardName(String commanderCardName) {
        this.commanderCardName = commanderCardName;
    }

    public Deck getStartDeck() {
        return startDeck;
    }

    public void setStartDeck(Deck startDeck) {
        this.startDeck = startDeck;
    }

    public CardPool getRewardPool() {
        return rewardPool;
    }

    public void setRewardPool(CardPool rewardPool) {
        this.rewardPool = rewardPool;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThemeDescription() {
        return themeDescription;
    }

    public void setThemeDescription(String themeDescription) {
        this.themeDescription = themeDescription;
    }

    @Override
    public String toString() {
        return name + " (" + commanderCardName + ")";
    }
}
