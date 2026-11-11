package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import forge.deck.DeckFormat;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.CardPool;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.path.RoguePath;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.item.PaperCard;
import forge.util.MyRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Main container for a Rogue Commander run state.
 * Tracks deck evolution, life persistence, path progress, and match history.
 */
public class RogueRun {

    @XStreamOmitField
    private String name;  // Set based on filename on load

    private int saveVersion = RogueSaveMigration.currentVersion();

    // Run Configuration
    private RogueDeck selectedRogueDeck;        // Selected Rogue Deck identifier
    private String timestamp;                   // Creation timestamp
    private List<RogueRunEffect> activeCustomRunModifications;

    // Run State
    private Deck currentDeck;                   // Player's evolving deck (starts as Start Deck copy)
    private int currentLife;                    // Persistent life total (starts at 20)
    private int maxLife;                        // Persistent life cap (default: 20)
    private int currentGold;                    // Currency (for future Bazaar support)
    private RoguePath path;                     // The generated path
    private int currentNodeIndex;               // Current position on path
    private RogueRunState runState;             // Current state of the run

    // Descension
    private int descensionLevel;                // 0 = no descension; XStream defaults int to 0 for old saves

    // Endless Mode (0 = original Trail and default for old saves)
    private int endlessLevel;

    // Aether effects (Aetherworks) (snapshotted from RogueMetaProgress at run creation)
    private List<RogueRunEffect> activeAetherEffects;

    // NPC effects (gained from NPC encounters at run start)
    private List<RogueRunEffect> activeNPCEffects;

    // Event effects (gained from event nodes during the run)
    private List<RogueRunEffect> activeEventEffects;

    // Chest effects (gained from chest nodes during the run)
    private List<RogueRunEffect> activeChestEffects;

    // Wounds (permanent debuffs gained from events, descension, etc.)
    private List<RogueRunEffect> activeWounds;

    // Wrathful effects (consume debuffs from wrathful planebounds)
    private List<RogueRunEffect> activeWrathful;

    // Cursed effects (consume debuffs from cursed planebounds)
    private List<RogueRunEffect> activeCursed;

    // Match History
    private int matchesWon;                     // Win counter
    private int matchesLost;                    // Loss counter
    private int removalCredits;                 // Credits for removing cards from deck (from rewards and Sanctum)
    private RogueRunTimer runTimer;             // Active play time for this run

    /** Type of card carried between matches in the command zone. */
    public enum CarryCardType { ITEM, FELLOW, SCROLL}

    /** A card the player carries between matches (castable from command zone).
     *  sourceId links to the effect that granted it (null if purchased/rewarded). */
    public static class CarryCard {
        private String cardName;
        private String setCode;
        private Integer artIndex;
        private CarryCardType type;
        private String sourceId;

        public CarryCard() {} // XStream

        public CarryCard(String cardName, CarryCardType type, String sourceId) {
            this(cardName, null, null, type, sourceId);
        }

        public CarryCard(String cardName, String setCode, Integer artIndex, CarryCardType type, String sourceId) {
            this.cardName = cardName;
            this.setCode = setCode;
            this.artIndex = artIndex;
            this.type = type;
            this.sourceId = sourceId;
        }

        public String cardName() { return cardName; }
        public String setCode() { return setCode; }
        public Integer artIndex() { return artIndex; }
        public CarryCardType type() { return type; }
        public String sourceId() { return sourceId; }

        public PaperCard toPaperCard() {
            return RogueConfig.getCard(cardName, setCode, artIndex);
        }
    }

    // Carry cards (items/fellows/scrolls that persist across matches in the command zone)
    private List<CarryCard> carryCards;

    // Transient (runtime only, not serialized)
    @XStreamOmitField
    private transient HostedMatch hostedMatch = null;
    @XStreamOmitField
    private transient Integer lastMatchRawLife = null;

    // Constructors
    public RogueRun() {
        this.initializeMaxLife(20);
        this.setCurrentGold(0);
        this.setCurrentNodeIndex(0);
        this.setRemovalCredits(0);
        this.setRunState(RogueRunState.STARTED);
        this.runTimer = new RogueRunTimer();
        this.matchesWon = 0;
        this.matchesLost = 0;
        stamp();
    }

    public RogueRun(RogueDeck selectedRogueDeck) {
        this();
        this.selectedRogueDeck = selectedRogueDeck;
        // Create deep copy of start deck
        this.currentDeck = new Deck(selectedRogueDeck.getStartDeck());
    }

    // Timestamp management
    public void stamp() {
        final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy, H:m");
        timestamp = dateFormat.format(new Date());
    }

    // Path navigation
    public void nextNode() {
        if (currentNodeIndex >= path.getNodeCount() - 1) {
            return; // Already at last node
        }

        RoguePathNode currentNode = path.getNode(currentNodeIndex);
        if (currentNode == null) {
            currentNodeIndex++;
            return;
        }

        // Get reachable nodes in next row from current completed node
        List<Integer> reachableIndices = path.getReachableNodeIndices(currentNodeIndex);

        if (!reachableIndices.isEmpty()) {
            // Set to first reachable node in next row
            currentNodeIndex = reachableIndices.get(0);
        } else {
            // No reachable nodes (end of path or error), just increment
            currentNodeIndex++;
        }
    }

    public RoguePathNode getCurrentNode() {
        return path.getNode(currentNodeIndex);
    }

    public boolean isRunComplete() {
        return currentNodeIndex >= path.getNodeCount() - 1 &&
               (getCurrentNode() == null || getCurrentNode().isCompleted());
    }

    public boolean isRunFailed() {
        return runState == RogueRunState.LOST || currentLife <= 0;
    }

    public void setRunFailed(boolean failed) {
        if (failed) {
            this.runState = RogueRunState.LOST;
        }
    }

    public boolean isRunWon() {
        return runState == RogueRunState.WON;
    }

    public void setRunWon(boolean won) {
        if (won) {
            this.runState = RogueRunState.WON;
        }
    }

    public RogueRunState getRunState() {
        return runState;
    }

    public void setRunState(RogueRunState state) {
        this.runState = state;
    }

    /**
     * Called after deserialization to ensure state is initialized.
     * Handles backward compatibility with old save files.
     * Is used by framework even though looks unused.
     */
    private Object readResolve() {
        // Initialize state if it's null (old save files)
        if (runState == null) {
            if (currentLife <= 0) {
                runState = RogueRunState.LOST;
            } else if (isRunComplete()) {
                runState = RogueRunState.WON;
            } else {
                runState = RogueRunState.STARTED;
            }
        }
        getRunTimer().stop();
        clampCurrentLifeToMax();
        return this;
    }

    // Match result tracking
    public void recordMatchResult(boolean won) {
        if (won) {
            matchesWon++;
            // Mark current node as completed
            if (getCurrentNode() != null) {
                getCurrentNode().setCompleted(true);
            }
        } else {
            matchesLost++;
        }
    }

    public List<PaperCard> getAllCardsForActiveCommander(Predicate<PaperCard> filter) {
        List<PaperCard> candidateCards = RogueConfig.getRandomCards(filter);
        List<PaperCard> commanderLegalCards = filterCardsByCommanderColorIdentity(candidateCards);
        return filterDuplicateCards(commanderLegalCards);
    }

    public List<PaperCard> getGamechangerCardsForActiveCommander() {
        List<PaperCard> candidateCards = RogueConfig.getGamechangerCards();
        List<PaperCard> commanderLegalCards = filterCardsByCommanderColorIdentity(candidateCards);
        return filterDuplicateCards(commanderLegalCards);
    }

    public List<PaperCard> getBanlistCardsForActiveCommander() {
        List<PaperCard> candidateCards = RogueConfig.getCommanderBanlistCards();
        List<PaperCard> commanderLegalCards = filterCardsByCommanderColorIdentity(candidateCards);
        return filterDuplicateCards(commanderLegalCards);
    }

    // Deck management
    public List<PaperCard> getSelectableDeckCards(Predicate<PaperCard> filter) {
        if (currentDeck == null) {
            return ImmutableList.of();
        }

        List<PaperCard> deckCards = new ArrayList<>(currentDeck.getMain().toFlatList());
        Set<String> commanderNames = getActiveCommanderNames();
        deckCards.removeIf(c -> commanderNames.contains(c.getName()) || c.getRules().getType().isBasicLand());

        if (filter != null) {
            deckCards.removeIf(card -> !filter.test(card));
        }
        return deckCards;
    }

    public void addCardsToDeck(List<PaperCard> cards, boolean awardRemovalCredits) {
        if (currentDeck != null && cards != null) {
            for (PaperCard card : cards) {
                currentDeck.getMain().add(card);
            }
            if (awardRemovalCredits) {
                removalCredits += cards.size();
            }
        }
    }

    public List<PaperCard> removeCardsFromDeck(Predicate<PaperCard> predicate) {
        if (currentDeck == null || predicate == null) {
            return ImmutableList.of();
        }

        List<PaperCard> deckCardsToRemove = new ArrayList<>(currentDeck.getMain().toFlatList());
        Set<String> commanderNames = getActiveCommanderNames();
        deckCardsToRemove.removeIf(card -> commanderNames.contains(card.getName()) || !predicate.test(card));
        if (deckCardsToRemove.isEmpty()) {
            return ImmutableList.of();
        }

        for (PaperCard card : deckCardsToRemove) {
            currentDeck.getMain().remove(card);
        }
        return deckCardsToRemove;
    }

    public List<PaperCard> removeRandomCardsFromDeck(int count, Predicate<PaperCard> predicate) {
        if (currentDeck == null || count <= 0) {
            return ImmutableList.of();
        }

        List<PaperCard> selectableDeckCards = getSelectableDeckCards(predicate);
        if (selectableDeckCards.isEmpty()) {
            return ImmutableList.of();
        }

        Collections.shuffle(selectableDeckCards, MyRandom.getRandom());
        List<PaperCard> removedCards = new ArrayList<>(selectableDeckCards.subList(0,
            Math.min(count, selectableDeckCards.size())));
        for (PaperCard card : removedCards) {
            currentDeck.getMain().remove(card);
        }
        return removedCards;
    }

    /**
     * Add removal credits without adding cards to deck.
     * Used by Sanctum and other sources of free card removals.
     */
    public void addRemovalCredits(int count) {
        removalCredits += count;
    }

    // Carry card management (items/fellows/scrolls in command zone)
    public void addCarryCard(PaperCard card, CarryCardType type, String sourceId) {
        if (card == null) {
            return;
        }
        if (carryCards == null) carryCards = new ArrayList<>();
        carryCards.add(new CarryCard(card.getName(), card.getEdition(), card.getArtIndex(), type, sourceId));
    }

    public void removeCarryCard(String cardName) {
        if (carryCards == null) return;
        carryCards.removeIf(c -> c.cardName().equals(cardName));
    }

    public void removeCarryCardsBySource(String sourceId) {
        if (carryCards == null || sourceId == null) return;
        carryCards.removeIf(c -> sourceId.equals(c.sourceId()));
    }

    public List<CarryCard> getCarryCards() {
        return carryCards != null ? carryCards : ImmutableList.of();
    }

    public boolean hasCarryCardOfType(CarryCardType type) {
        return getCarryCards().stream().anyMatch(c -> c.type() == type);
    }

    /**
     * Filter candidate cards to only those legal under the active run commander's color identity.
     * Uses Forge's commander-conformance predicate to stay aligned with deck legality rules.
     */
    public List<PaperCard> filterCardsByCommanderColorIdentity(List<PaperCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return ImmutableList.of();
        }

        List<PaperCard> commanders = getActiveCommanders();
        if (commanders.isEmpty()) {
            return ImmutableList.of();
        }

        Predicate<PaperCard> predicate =
            DeckFormat.RogueCommander.isLegalCardForCommanderPredicate(commanders);
        List<PaperCard> filtered = new ArrayList<>();
        for (PaperCard card : cards) {
            if (predicate.test(card)) {
                filtered.add(card);
            }
        }
        return filtered;
    }

    public boolean canAddCardToDeck(PaperCard card) {
        if (card == null) {
            return false;
        }
        boolean fulfillsColorIdentity = !filterCardsByCommanderColorIdentity(ImmutableList.of(card)).isEmpty();

        if (fulfillsColorIdentity) {
            return !filterDuplicateCards(ImmutableList.of(card)).isEmpty();
        } else {
            return false;
        }
    }

    public List<PaperCard> filterAddableCardsToDeck(Collection<PaperCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return ImmutableList.of();
        }

        List<PaperCard> filtered = filterCardsByCommanderColorIdentity(new ArrayList<>(cards));
        return filterDuplicateCards(filtered);
    }

    public boolean canAddCardAsCarryCard(PaperCard card) {
        if (card == null) {
            return false;
        }

        if (filterCardsByCommanderColorIdentity(ImmutableList.of(card)).isEmpty()) {
            return false;
        }

        String normalizedName = card.getRules().getNormalizedName();
        return !getActiveCarryCardNames().contains(normalizedName)
            && !getExistingCardCounts().containsKey(normalizedName);
    }

    public List<PaperCard> filterDuplicateCards(Collection<PaperCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return ImmutableList.of();
        }

        Map<String, Integer> existingCardCounts = getExistingCardCounts();
        Set<String> activeCarryCardNames = getActiveCarryCardNames();

        List<PaperCard> filtered = new ArrayList<>();
        for (PaperCard card : cards) {
            String normalizedName = card.getRules().getNormalizedName();
            if (!activeCarryCardNames.contains(normalizedName)
                && canAddCardByCardCountRules(card, existingCardCounts)) {
                filtered.add(card);
                if (!DeckFormat.canHaveAnyNumberOf(card)) {
                    existingCardCounts.put(normalizedName, existingCardCounts.getOrDefault(normalizedName, 0) + 1);
                }
            }
        }
        return filtered;
    }

    public Predicate<PaperCard> getNotAlreadyInDeckPredicate() {
        Set<String> activeCarryCardNames = getActiveCarryCardNames();
        if (currentDeck == null) {
            return card -> !activeCarryCardNames.contains(card.getRules().getNormalizedName());
        }

        Map<String, Integer> existingCardCounts = getExistingCardCounts();
        return card -> !activeCarryCardNames.contains(card.getRules().getNormalizedName())
            && canAddCardByCardCountRules(card, existingCardCounts);
    }

    public int getCommanderColorIdentityMask() {
        int colorIdentityMask = 0;
        for (PaperCard commander : getActiveCommanders()) {
            colorIdentityMask |= commander.getRules().getColorIdentity().getColor();
        }
        return colorIdentityMask;
    }

    public String getCurrentCommanderName() {
        List<PaperCard> commanders = getActiveCommanders();
        if (!commanders.isEmpty()) {
            return commanders.get(0).getName();
        }
        return selectedRogueDeck != null ? selectedRogueDeck.getCommanderCardName() : "";
    }

    private List<PaperCard> getActiveCommanders() {
        if (currentDeck != null) {
            List<PaperCard> commanders = currentDeck.getCommanders();
            if (!commanders.isEmpty()) {
                return commanders;
            }
        }
        if (selectedRogueDeck != null && selectedRogueDeck.getStartDeck() != null) {
            List<PaperCard> commanders = selectedRogueDeck.getStartDeck().getCommanders();
            if (!commanders.isEmpty()) {
                return commanders;
            }
        }
        return ImmutableList.of();
    }

    private Set<String> getActiveCommanderNames() {
        Set<String> commanderNames = new HashSet<>();
        for (PaperCard commander : getActiveCommanders()) {
            commanderNames.add(commander.getName());
        }
        return commanderNames;
    }

    private Set<String> getActiveCarryCardNames() {
        Set<String> carryCardNames = new HashSet<>();
        for (CarryCard carryCard : getCarryCards()) {
            PaperCard card = carryCard.toPaperCard();
            if (card != null) {
                carryCardNames.add(card.getRules().getNormalizedName());
            }
        }
        return carryCardNames;
    }

    private Map<String, Integer> getExistingCardCounts() {
        Map<String, Integer> existingCardCounts = new HashMap<>();
        addExistingCardCounts(existingCardCounts, currentDeck.get(DeckSection.Main));
        addExistingCardCounts(existingCardCounts, currentDeck.get(DeckSection.Commander));
        addDuplicateProtectedCardCounts(existingCardCounts);
        return existingCardCounts;
    }

    private static boolean canAddCardByCardCountRules(PaperCard card, Map<String, Integer> existingCardCounts) {
        String normalizedName = card.getRules().getNormalizedName();
        int existingCopies = existingCardCounts.getOrDefault(normalizedName, 0);
        Integer deckLimit = DeckFormat.canHaveSpecificNumberInDeck(card);
        return DeckFormat.canHaveAnyNumberOf(card)
            || deckLimit != null && deckLimit > 0 && existingCopies < deckLimit
            || deckLimit == null && existingCopies == 0;
    }

    private static void addExistingCardCounts(Map<String, Integer> existingCardCounts, CardPool cardPool) {
        if (cardPool == null) {
            return;
        }
        for (Map.Entry<PaperCard, Integer> entry : cardPool) {
            String normalizedName = entry.getKey().getRules().getNormalizedName();
            existingCardCounts.merge(normalizedName, entry.getValue(), Integer::sum);
        }
    }

    private void addDuplicateProtectedCardCounts(Map<String, Integer> existingCardCounts) {
        for (RogueEffect effect : RogueEffectComposite.getAllEffects(this)) {
            for (String cardReferenceText : effect.getDuplicateProtectedCardReferences()) {
                CardReference cardReference = TextHelper.parseCardReference(cardReferenceText);
                if (cardReference.cardName().isEmpty()) {
                    continue;
                }

                PaperCard card = RogueConfig.getCard(cardReference.cardName(),
                    cardReference.setCode(), cardReference.artIndex());
                String normalizedName = card.getRules().getNormalizedName();
                existingCardCounts.merge(normalizedName, 1, Integer::sum);
            }
        }
    }

    // Life management
    public void gainLifeUpToMax(int amount) {
        GainLifeContext ctx = new GainLifeContext(amount);
        RogueEffectComposite.INSTANCE.onBeforeGainLife(ctx, this);
        this.setCurrentLife(Math.min(currentLife + Math.max(0, ctx.amount), maxLife));
    }

    public void loseLife(int amount) {
        this.setCurrentLife(Math.max(currentLife - amount, 0));
    }

    public void clampCurrentLifeToMax() {
        if (maxLife > 0) {
            currentLife = Math.min(currentLife, maxLife);
        }
    }

    public void persistMatchLife(int life) {
        lastMatchRawLife = life;
        currentLife = life;
        clampCurrentLifeToMax();
    }

    public int getLastMatchRawLife() {
        return lastMatchRawLife != null ? lastMatchRawLife : currentLife;
    }

    // Match hosting (transient)
    public void setHostedMatch(HostedMatch match) {
        this.hostedMatch = match;
    }

    public HostedMatch getHostedMatch() {
        return hostedMatch;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        if (name != null && name.startsWith(RogueIO.PREFIX_LOCKED)) {
            return name.substring(RogueIO.PREFIX_LOCKED.length());
        }
        return name;
    }

    public RogueDeck getSelectedRogueDeck() {
        return selectedRogueDeck;
    }

    public boolean isCustomRun() {
        return activeCustomRunModifications != null && !activeCustomRunModifications.isEmpty();
    }

    private boolean hasCustomRunModification(CustomRunModification modification) {
        return activeCustomRunModifications != null && activeCustomRunModifications.stream()
            .anyMatch(effect -> effect.getId().equals(modification.getId()));
    }

    public boolean recordsCuratedCommanderRewards() {
        return getActiveCustomRunModifications().stream().allMatch(effect ->
            effect instanceof CustomRunModification modification
                && modification.recordsCuratedCommanderRewards());
    }

    public List<RogueEffect> getActiveCustomRunModifications() {
        return mapEffects(activeCustomRunModifications, CustomRunModification::fromId);
    }

    public void addCustomRunModification(CustomRunModification modification) {
        if (!hasCustomRunModification(modification)) {
            activeCustomRunModifications = addEffect(activeCustomRunModifications, modification);
        }
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Deck getCurrentDeck() {
        return currentDeck;
    }

    public void setCurrentDeck(Deck currentDeck) {
        this.currentDeck = currentDeck;
    }

    public int getCurrentLife() {
        return currentLife;
    }

    public void setCurrentLife(int currentLife) {
        this.currentLife = currentLife;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void initializeMaxLife(int maxLife) {
        this.maxLife = maxLife;
        this.setCurrentLife(this.maxLife);
    }

    public void addMaxLife(int amount) {
        this.setMaxLife(maxLife + amount);
    }

    public void loseMaxLife(int amount) {
        this.setMaxLife(Math.max(maxLife - amount, 0));
    }

    public void setMaxLife(int maxLife) {
        int oldMaxLife = this.maxLife;
        if (oldMaxLife <= 0) {
            this.maxLife = maxLife;
            // No valid old cap exists, so preserve raw life instead of granting a heal.
            this.currentLife = Math.min(this.currentLife, maxLife);
            return;
        }

        float currentLifeRatio = this.currentLife / (float) oldMaxLife;
        this.maxLife = maxLife;
        this.currentLife = Math.round(currentLifeRatio * maxLife);
        clampCurrentLifeToMax();
    }

    public int getCurrentGold() {
        return currentGold;
    }

    public void setCurrentGold(int currentGold) {
        this.currentGold = currentGold;
    }

    public void addGold(int amount) {
        this.setCurrentGold(this.getCurrentGold() + amount);
    }

    public boolean hasEnoughGold(int amount) {
        return getCurrentGold() >= amount;
    }

    public void spendGold(int amount) {
        this.setCurrentGold(Math.max(getCurrentGold() - amount, 0));
    }

    public RoguePath getPath() {
        return path;
    }

    public void setPath(RoguePath path) {
        this.path = path;
    }

    public int getCurrentNodeIndex() {
        return currentNodeIndex;
    }

    public void setCurrentNodeIndex(int currentNodeIndex) {
        this.currentNodeIndex = currentNodeIndex;
    }

    public int getRemovalCredits() {
        return removalCredits;
    }

    public void setRemovalCredits(int removalCredits) {
        this.removalCredits = removalCredits;
    }

    public RogueRunTimer getRunTimer() {
        if (runTimer == null) {
            runTimer = new RogueRunTimer();
        }
        return runTimer;
    }

    public long getRunTimeMillis() {
        return getRunTimer().getElapsedMillis();
    }

    public int getDescensionLevel() {
        return descensionLevel;
    }

    public void setDescensionLevel(int level) {
        this.descensionLevel = level;
    }

    public int getEndlessLevel() {
        return endlessLevel;
    }

    public void setEndlessLevel(int level) {
        this.endlessLevel = level;
    }

    // Aetherwork management
    public void snapshotAetherEffects(RogueMetaProgress progress) {
        activeAetherEffects = new ArrayList<>();
        for (AetherEffect aetherEffect : progress.getActiveAetherEffects()) {
            int rank = progress.getAetherEffectRank(aetherEffect);
            int charges = aetherEffect.getChargesForRank(rank);
            activeAetherEffects.add(new RogueRunEffect(aetherEffect.getId(), rank, charges));
        }
    }

    void capAetherEffectRank(AetherEffect aetherEffect, int maxRank) {
        if (activeAetherEffects == null) return;
        for (int i = 0; i < activeAetherEffects.size(); i++) {
            RogueRunEffect effect = activeAetherEffects.get(i);
            if (aetherEffect.getId().equals(effect.getId()) && effect.getRank() > maxRank) {
                activeAetherEffects.set(i, new RogueRunEffect(effect.getId(), maxRank, effect.getCharges()));
            }
        }
    }

    void removeAetherEffect(String id) {
        if (activeAetherEffects != null) {
            activeAetherEffects.removeIf(effect -> id.equals(effect.getId()));
        }
    }

    int getSaveVersion() {
        return saveVersion;
    }

    void setSaveVersion(int version) {
        saveVersion = version;
    }

    // Generic effect list helpers
    private List<RogueRunEffect> ensureList(List<RogueRunEffect> list) {
        return list != null ? list : new ArrayList<>();
    }

    private List<RogueEffect> mapEffects(List<RogueRunEffect> list, Function<String, ? extends RogueEffect> lookup) {
        List<RogueEffect> result = new ArrayList<>();
        for (RogueRunEffect rb : ensureList(list)) {
            RogueEffect e = lookup.apply(rb.getId());
            if (e != null) result.add(e);
        }
        return result;
    }

    public List<RogueEffect> getActiveAetherEffects() { return mapEffects(activeAetherEffects, AetherEffect::fromId); }
    public List<RogueEffect> getActiveEventEffects() { return mapEffects(activeEventEffects, EventEffect::fromId); }
    public List<RogueEffect> getActiveChestEffects() { return mapEffects(activeChestEffects, ChestEffect::fromId); }
    public List<RogueEffect> getActiveWoundEffects()     { return mapEffects(activeWounds, WoundEffect::fromId); }
    public List<RogueEffect> getActiveWrathfulEffects()   { return mapEffects(activeWrathful, WrathfulEffect::fromId); }
    public List<RogueEffect> getActiveCursedEffects()    { return mapEffects(activeCursed, CursedEffect::fromId); }
    public List<RogueEffect> getActiveNPCEffects() { return mapEffects(activeNPCEffects, NPCEffect::fromId); }

    public void addEventEffect(EventEffect eventEffect)   { activeEventEffects = addEffect(
        activeEventEffects, eventEffect); }
    public void addChestEffect(ChestEffect chestEffect)    { activeChestEffects = addEffect(
        activeChestEffects, chestEffect); }
    public void addWound(WoundEffect woundEffect)           { activeWounds = addEffect(activeWounds,
        woundEffect); }
    public void addWrathful(WrathfulEffect wrathfulEffect)  { activeWrathful = addEffect(activeWrathful,
        wrathfulEffect); }
    public void addCursed(CursedEffect cursedEffect)        { activeCursed = addEffect(activeCursed,
        cursedEffect); }
    public void addNPCEffect(NPCEffect npcEffect)           { activeNPCEffects = addEffect(activeNPCEffects,
        npcEffect); }

    private List<RogueRunEffect> addEffect(List<RogueRunEffect> list, RogueEffect effect) {
        if (list == null) list = new ArrayList<>();
        list.add(new RogueRunEffect(effect.getId(), 0, effect.getChargesForRank(0)));
        return list;
    }

    public void clearWounds() {
        if (activeWounds != null) activeWounds.clear();
    }

    // effect queries
    public int getRunEffectRank(String id) {
        RogueRunEffect rb = findRunEffect(id);
        return rb != null ? rb.getRank() : 0;
    }

    public int getRunEffectCharges(String id) {
        RogueRunEffect rb = findRunEffect(id);
        return rb != null ? Math.max(0, rb.getCharges()) : 0;
    }

    @SuppressWarnings("unchecked")
    private List<RogueRunEffect>[] allEffectLists() {
        return new List[]{activeAetherEffects, activeEventEffects, activeChestEffects,
                activeWounds, activeWrathful, activeCursed, activeNPCEffects};
    }

    // Effect consumption
    public void consumeEffect(String id) {
        for (List<RogueRunEffect> list : allEffectLists())
            consumeFromList(list, id);
    }

    private void consumeFromList(List<RogueRunEffect> list, String id) {
        if (list == null) return;
        Iterator<RogueRunEffect> it = list.iterator();
        while (it.hasNext()) {
            RogueRunEffect rb = it.next();
            if (rb.getId().equals(id) && rb.consumeCharge()) {
                it.remove();
                removeCarryCardsBySource(id);
                return;
            }
        }
    }

    private RogueRunEffect findRunEffect(String id) {
        for (List<RogueRunEffect> list : allEffectLists()) {
            if (list == null) continue;
            for (RogueRunEffect rb : list)
                if (rb.getId().equals(id)) return rb;
        }
        return null;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + matchesWon + "-" + matchesLost + ", " + currentLife + " life)";
    }
}
