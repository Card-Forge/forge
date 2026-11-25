package forge.gamemodes.rogue;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a Rogue Deck configuration: a Commander with a starting deck
 * and reward pool for progressive deck building during runs.
 */
public class RogueDeck {

    private String name;                    // Display name (e.g., "Aegar, the Freezing Flame")
    private String commanderCardName;       // Commander card name
    private Deck startDeck;                 // 40-45 card starting deck (includes commander)
    private CardPool rewardPool;            // ~100 card pool for rewards during run
    private CardPool discardedRewardPool;   // Shown reward cards that can return on reshuffle
    private String description;             // Flavor text for UI
    private String themeDescription;        // Theme/archetype (e.g., "Instants/Sorceries matter")
    private int avatarIndex;                // Avatar image index
    private int sleeveIndex;                // Sleeve image index
    private String landEdition;             // Land edition code for basic lands in Deck Editor (optional)
    private boolean includeColorlessBasics; // Adds Wastes support for decks that need explicit {C} sources
    private boolean experimental;
    private RogueUnlockCondition rogueUnlockCondition;  // Unlock condition

    // Constructors
    public RogueDeck() {
        this.rewardPool = new CardPool();
        this.discardedRewardPool = new CardPool();
    }

    public RogueDeck copy() {
        RogueDeck copy = new RogueDeck();
        copy.name = name;
        copy.commanderCardName = commanderCardName;
        copy.startDeck = startDeck == null ? null : new Deck(startDeck);
        copy.rewardPool.addAll(rewardPool);
        copy.discardedRewardPool.addAll(discardedRewardPool);
        copy.description = description;
        copy.themeDescription = themeDescription;
        copy.avatarIndex = avatarIndex;
        copy.sleeveIndex = sleeveIndex;
        copy.landEdition = landEdition;
        copy.includeColorlessBasics = includeColorlessBasics;
        copy.experimental = experimental;
        copy.rogueUnlockCondition = rogueUnlockCondition;
        return copy;
    }

    /**
     * Draw random cards from the reward pool for post-match rewards.
     * @param count Number of cards to draw
     * @param filter Optional predicate to filter cards (null for unfiltered)
     * @return List of random cards (may be less than count if pool is small)
     */
    public List<PaperCard> drawRewardOptions(int count, Predicate<PaperCard> filter) {
        ensureCardPools();
        if (count <= 0) return new ArrayList<>();

        // Use filtered pool if filter provided, otherwise use full pool
        CardPool poolToUse = (filter != null) ? rewardPool.getFilteredPool(filter) : rewardPool;
        if (poolToUse.countAll() < count && !discardedRewardPool.isEmpty()) {
            rewardPool.addAll(discardedRewardPool);
            discardedRewardPool.clear();
            poolToUse = (filter != null) ? rewardPool.getFilteredPool(filter) : rewardPool;
        }

        List<PaperCard> allCards = poolToUse.toFlatList();
        if (allCards.isEmpty()) {
            return new ArrayList<>();
        }
        if (allCards.size() <= count) {
            return new ArrayList<>(allCards);
        }

        // Shuffle and take first 'count' cards
        List<PaperCard> result = new ArrayList<>(allCards);
        java.util.Collections.shuffle(result);
        return new ArrayList<>(result.subList(0, Math.min(count, result.size())));
    }

    /**
     * Move shown cards out of the active reward pool so they only return after a reshuffle.
     * @param cards Cards that were shown to the player
     */
    public void discardRewardOptions(Iterable<PaperCard> cards) {
        ensureCardPools();
        for (PaperCard card : cards) {
            if (rewardPool.remove(card)) {
                discardedRewardPool.add(card);
            }
        }
    }

    /**
     * Remove cards from the reward pool / discard pool
     * (used when cards are selected as rewards or given in other ways).
     * @param cards Cards to remove
     */
    public void removeFromCardPools(Iterable<PaperCard> cards) {
        ensureCardPools();
        for (PaperCard card : cards) {
            rewardPool.remove(card);
            discardedRewardPool.remove(card);
        }
    }

    private void ensureCardPools() {
        if (rewardPool == null) {
            rewardPool = new CardPool();
        }
        if (discardedRewardPool == null) {
            discardedRewardPool = new CardPool();
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

    public void setRewardPool(CardPool rewardPool) {
        this.rewardPool = rewardPool;
        if (discardedRewardPool == null) {
            discardedRewardPool = new CardPool();
        }
    }

    public List<PaperCard> getRewardPoolCards() {
        ensureCardPools();
        return new ArrayList<>(rewardPool.toFlatList());
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

    public int getAvatarIndex() {
        return avatarIndex;
    }

    public void setAvatarIndex(int avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public int getSleeveIndex() {
        return sleeveIndex;
    }

    public void setSleeveIndex(int sleeveIndex) {
        this.sleeveIndex = sleeveIndex;
    }

    public String getLandEdition() {
        return landEdition;
    }

    public void setLandEdition(String landEdition) {
        this.landEdition = landEdition;
    }

    public boolean shouldIncludeColorlessBasics() {
        return includeColorlessBasics;
    }

    public void setIncludeColorlessBasics(boolean includeColorlessBasics) {
        this.includeColorlessBasics = includeColorlessBasics;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public void setExperimental(boolean experimental) {
        this.experimental = experimental;
    }

    public RogueUnlockCondition getUnlockCondition() {
        return rogueUnlockCondition;
    }

    public void setUnlockCondition(RogueUnlockCondition rogueUnlockCondition) {
        this.rogueUnlockCondition = rogueUnlockCondition;
    }

    /**
     * Check if this commander is unlocked and available for selection.
     * A commander is unlocked if:
     * - It has no unlock condition (always available)
     * - Its unlock condition evaluates to true
     * @return true if the commander can be selected
     */
    public boolean isUnlocked() {
        if (RogueMetaProgress.getInstance().isDevUnlockAll()) return true;
        if (rogueUnlockCondition == null) return true;
        return rogueUnlockCondition.evaluate();
    }

    /**
     * Get the description of the unlock requirement for display.
     * @return Human-readable unlock requirement, or null if always available
     */
    public String getUnlockDescription() {
        if (rogueUnlockCondition == null || (
            rogueUnlockCondition.isDefault() && !rogueUnlockCondition.isAlwaysLocked())) {
            return null;
        }
        return rogueUnlockCondition.getDescription();
    }

    @Override
    public String toString() {
        return name + " (" + commanderCardName + ")";
    }
}
