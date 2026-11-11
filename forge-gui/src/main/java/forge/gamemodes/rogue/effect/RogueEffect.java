package forge.gamemodes.rogue.effect;

import org.apache.commons.lang3.StringUtils;

import forge.deck.CardPool;
import forge.gamemodes.rogue.*;
import forge.game.player.RegisteredPlayer;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Interface for effects that trigger at specific points during a Rogue Commander run.
 * All methods are no-ops by default - override only the triggers relevant to each effect.
 * All state is read from RogueRun (Aetherworks, event traits, descension are snapshotted there).
 */
public interface RogueEffect {

    String TRAIT_GAIN_DESCRIPTION = "Gain the {{Trait}} %s.";
    String ITEM_GAIN_DESCRIPTION = "Gain the {{Item}} %s.";
    String ACCEPT = "Accept";
    String INSUFFICIENT_GOLD = "You don't have enough Gold.";
    String INSUFFICIENT_MAX_LIFE = "You don't have enough Max. Life.";

    /**
     * Lifecycle type for effects.
     * ONESHOT: fire-and-forget, applied once immediately, never stored or dispatched.
     * PERMANENT: stored in run, dispatched at trigger points, persists for the whole run (=Trait)
     * CONSUME: stored in run, dispatched at trigger points, removed after all charges consumed
     */
    enum EffectType { ONESHOT, PERMANENT, CONSUME }

    default EffectType getEffectType() { return EffectType.PERMANENT; }

    /** Unique identifier for serialization and lookup. Override in concrete types. */
    default String getId() { return ""; }

    /** Display name for UI. Override in concrete types. */
    default String getDisplayName() { return ""; }

    /** Raw description for preview-aware UI. Override in concrete types. */
    default String getRawDescription() { return ""; }

    /** Optional representative card reference used for UI and runtime behavior. */
    default String getEffectCardReference() { return null; }

    /** Cards this effect provides outside the deck but should still block singleton deck rewards. */
    default List<String> getDuplicateProtectedCardReferences() { return List.of(); }

    /** Optional ranked card reference for effects whose representative card varies by rank. */
    default String getEffectCardReferenceForRank(int rank) {
        String effectCardReference = getEffectCardReference();
        if (StringUtils.isBlank(effectCardReference) || rank <= 0) {
            return effectCardReference;
        }
        return effectCardReference + " " + rank;
    }

    /** Paper card associated with an effect, resolved from the optional reference token. */
    default PaperCard getEffectCard() {
        String effectCardReference = getEffectCardReference();
        if (StringUtils.isBlank(effectCardReference)) {
            return null;
        }

        CardReference effectCardReferenceParts = TextHelper.parseCardReference(effectCardReference);
        if (effectCardReferenceParts.cardName().isEmpty()) {
            return null;
        }
        return RogueConfig.getCard(effectCardReferenceParts.cardName(),
            effectCardReferenceParts.setCode(), effectCardReferenceParts.artIndex());
    }

    /** Human-facing display name derived from an effect card reference when needed. */
    default String getEffectCardDisplayName() {
        return TextHelper.extractEffectCardDisplayNameFromReference(getEffectCardReference());
    }

    /** Resolves a `%s` placeholder in description text to the formatted effect-card mention, if available. */
    default String formatEffectCardDescription(String description) {
        if (description == null || !description.contains("%s")) {
            return description;
        }

        String effectCardDisplayName = getEffectCardDisplayName();
        if (StringUtils.isBlank(effectCardDisplayName)) {
            return description;
        }
        return String.format(description, "**" + effectCardDisplayName + "**");
    }

    /** Description for tooltips and display text. */
    default String getDescription() { return TextHelper.stripPreviewMarkers(getRawDescription()); }

    /** Description variant for ranked effects with current rank highlighted. */
    default String getDescriptionWithAllRanks(int currentRank, int upgradeLevel) {
        return getDescription();
    }

    /** Active description for run-context displays such as the RogueMap header tooltip. */
    default String getActiveDescription(RogueRun run) {
        return getTooltipText();
    }

    /** Resolved display name for active-effect UI such as the RogueMap header. */
    default String getUIDisplayName() {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null) {
            return effectCard.getName();
        }

        String effectCardDisplayName = getEffectCardDisplayName();
        return !effectCardDisplayName.isEmpty() ? effectCardDisplayName : getDisplayName();
    }

    /** Full display text for active-effect UI such as the RogueMap header. */
    default String getUIDisplayText() {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null) {
            return effectCard.getName();
        }

        String displayName = getEffectCardDisplayName();
        if (displayName.isEmpty()) {
            displayName = getDisplayName();
        }
        return getUIDisplayPrefix(this) + displayName;
    }

    /** Tooltip text for long-lived effect displays such as the RogueMap header. */
    default String getTooltipText() {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null && effectCard.getRules() != null
            && effectCard.getRules().getOracleText() != null
            && !StringUtils.isBlank(effectCard.getRules().getOracleText())) {
            return effectCard.getRules().getOracleText();
        }

        if (!getRawDescription().contains("{{Trait}}")) {
            return getDescription();
        }

        List<PreviewReference> cardReferences = getPreviewReferences().stream()
            .filter(reference -> reference.type() == PreviewReferenceType.CARD)
            .collect(Collectors.toList());
        if (cardReferences.size() != 1) {
            return getDescription();
        }

        CardReference cardReference = TextHelper.parseCardReference(cardReferences.get(0).token());
        PaperCard card = cardReference.cardName().isEmpty() ? null
            : RogueConfig.getCard(cardReference.cardName(), cardReference.setCode(), cardReference.artIndex());
        if (card == null || card.getRules() == null || card.getRules().getOracleText() == null
            || StringUtils.isBlank(card.getRules().getOracleText())) {
            return getDescription();
        }
        return card.getRules().getOracleText();
    }

    private static String getUIDisplayPrefix(RogueEffect effect) {
        if (effect instanceof CustomRunModification) return "Run Mod: ";
        if (effect instanceof WoundEffect) return "Wound - ";
        if (effect instanceof AetherEffect) return "Aetherwork - ";
        if (effect instanceof NPCEffect) return "NPC Trait - ";
        if (effect instanceof EventEffect) return "Event Trait - ";
        if (effect instanceof ChestEffect) return "Chest Trait - ";
        if (effect instanceof DescensionLevel) return "Descension - ";
        if (effect instanceof WrathfulEffect) return "Wrathful - ";
        if (effect instanceof CursedEffect) return "Cursed - ";
        return "";
    }

    /** Preview references parsed from raw description plus the optional effect-card preview. */
    default List<PreviewReference> getPreviewReferences() {
        List<PreviewReference> references = new ArrayList<>(TextHelper.extractPreviewReferences(getRawDescription()));
        String effectCardReference = getEffectCardReference();
        if (StringUtils.isBlank(effectCardReference)) {
            return references;
        }

        String effectCardDisplayName = TextHelper.extractCardNameFromReference(effectCardReference);
        if (effectCardDisplayName.isEmpty()) {
            return references;
        }

        boolean alreadyReferenced = references.stream()
            .anyMatch(reference -> reference.type() == PreviewReferenceType.CARD
                && effectCardDisplayName.equals(TextHelper.extractCardNameFromReference(reference.token())));
        if (!alreadyReferenced) {
            references.add(0, new PreviewReference(PreviewReferenceType.CARD, effectCardReference, 0));
        }
        return references;
    }

    /** Optional shared card-pool filter for effects that repeatedly use one DB-backed card selection rule. */
    default Predicate<PaperCard> getDBCardsFilter() { return null; }

    /** Optional shared deck filter for effects that repeatedly use one deck-card selection rule. */
    default Predicate<PaperCard> getDeckCardFilter() { return card -> true; }

    /** Number of charges for CONSUME effects at the given rank. -1 = permanent (default). */
    default int getChargesForRank(int rank) { return -1; }

    /** Fired once immediately for ONESHOT effects. May also grant or mutate run state. */
    default void applyEffect(RogueRun run, EffectResultContext ctx) {}

    /** Whether this effect applies at the current run-start trigger. */
    default boolean appliesOnRunStart(RogueRun run) { return true; }

    /** Fired when a new run or Endless Trail starts. Submit any follow-up actions to handleResult. */
    default void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {}

    /** Fired after path generation. Modify nodes via run.getPath().getNodes(). */
    default void afterPathGeneration(RogueRun run) {}

    /** Fired when the path visualizer updates. Use to modify plane visibility. */
    default void onPathUpdate(PathUpdateContext ctx, RogueRun run) {}

    /** Fired after a path node reroll action is used. Consume charges or apply side effects. */
    default void onPathNodeReroll(PathUpdateContext ctx, RogueRun run) {}

    /** Fired before a Sanctum dialog is shown. Modify ctx to inject choices or other setup. */
    default void onBeforeSanctum(SanctumContext ctx, RogueRun run) {}

    /** Fired before a Bazaar dialog is shown. Modify ctx to adjust offers or pricing. */
    default void onBeforeBazaar(BazaarContext ctx, RogueRun run) {}

    /** Fired after a custom Sanctum choice is selected. */
    default void onSanctumChoice(SanctumContext.SanctumChoice choice, RogueRun run) {}

    /** Fired once per match start. Add command zone cards, adjust hand size, etc. */
    default void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {}

    /** Fired before players and variants are created for a match. */
    default void onBeforeMatchSetup(MatchSetupContext ctx, RogueRun run) {}

    /** Fired before explicit Rogue-side life gain is applied. */
    default void onBeforeGainLife(GainLifeContext ctx, RogueRun run) {}

    /** Fired after winning a match (non-final node). Heal life, etc. */
    default void onMatchWin(RogueRun run) {}

    /** Fired when the player would lose the run. */
    default void onDefeat(DefeatContext ctx, RogueRun run) {}

    /** Fired before match rewards are given after a win. Use to modify or skip them. */
    default void onBeforeRewards(MatchRewardContext ctx, RogueRun run) {}

    /** Fired for card reward node selections. */
    default void onCardReward(CardRewardContext ctx, RogueRun run) {}

    /** Fired for both card reward and bazaar selections. */
    default void onCardSelection(CardSelectionContext ctx, RogueRun run) {}

    /** Fired before run-start NPC boon choices are shown. Modify ctx to enable reroll actions. */
    default void onBeforeNpcBoons(ChoiceRerollContext ctx, RogueRun run) {}

    /** Fired before Chest loot choices are shown. Modify ctx to enable reroll actions. */
    default void onBeforeChestLoot(ChoiceRerollContext ctx, RogueRun run) {}

    /** Fired before Event choices are shown. Modify ctx to enable reroll actions. */
    default void onBeforeEvent(ChoiceRerollContext ctx, RogueRun run) {}

    /** Fired after a Rogue choice dialog is rerolled. Consume charges or apply side effects. */
    default void onChoiceReroll(ChoiceRerollContext ctx, RogueRun run) {}

    default void addEffectCardToCommandZone(RegisteredPlayer human) {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null) {
            addCardToCommandZone(effectCard, human);
        }
    }

    default void addEffectCardToBattlefield(RegisteredPlayer human) {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null) {
            addCardToBattlefield(effectCard, human);
        }
    }

    default void addEffectCardAsCarryCard(RogueRun run, EffectResultContext ctx, RogueRun.CarryCardType type) {
        PaperCard effectCard = getEffectCard();
        if (effectCard != null) {
            addCarryCard(run, ctx,  effectCard, type);
        }
    }

    default void addCarryCards(RogueRun run, EffectResultContext ctx, Predicate<PaperCard> filter,
                               int count, RogueRun.CarryCardType type, List<CardReference> cardPrintOverrides) {
        List<PaperCard> added = getAllCards(run, filter, count, cardPrintOverrides);
        for (PaperCard card : added) {
            addCarryCard(run, ctx, card, type);
        }
    }

    default void addCarryCards(RogueRun run, EffectResultContext ctx, List<String> cardReferences,
                               RogueRun.CarryCardType type) {
        if (cardReferences == null || cardReferences.isEmpty()) {
            return;
        }

        for (String cardReferenceText : cardReferences) {
            CardReference cardReference = TextHelper.parseCardReference(cardReferenceText);
            if (cardReference.cardName().isEmpty()) {
                continue;
            }

            PaperCard card = RogueConfig.getCard(cardReference.cardName(),
                cardReference.setCode(), cardReference.artIndex());
            addCarryCard(run, ctx, card, type);
        }
    }

    default boolean canAddAllCarryCards(RogueRun run, List<String> cardReferences) {
        if (cardReferences == null || cardReferences.isEmpty()) {
            return false;
        }

        for (String cardReferenceText : cardReferences) {
            CardReference cardReference = TextHelper.parseCardReference(cardReferenceText);
            if (cardReference.cardName().isEmpty()) {
                return false;
            }

            PaperCard card = RogueConfig.getCard(cardReference.cardName(),
                cardReference.setCode(), cardReference.artIndex());
            if (!run.canAddCardAsCarryCard(card)) {
                return false;
            }
        }
        return true;
    }

    default void addCarryCard(RogueRun run, EffectResultContext ctx, PaperCard card, RogueRun.CarryCardType type) {
        if (card == null || !run.canAddCardAsCarryCard(card)) {
            return;
        }
        run.addCarryCard(card, type, getId());
        ctx.addedCards.add(card);
    }

    default void addCardsToDeck(RogueRun run, EffectResultContext ctx, Predicate<PaperCard> filter,
                                Integer cardCount, Integer copyCount, List<CardReference> cardPrintOverrides) {
        List<PaperCard> added = getAllCards(run, filter, cardCount, cardPrintOverrides);
        if (added.isEmpty()) {
            return;
        }

        if (copyCount != null) {
            ArrayList<PaperCard> copies = new ArrayList<>();
            for (PaperCard card : added) {
                copies.addAll(Collections.nCopies(copyCount, card));
            }

            added = copies;
        }

        run.addCardsToDeck(added, false);
        ctx.addedCards.addAll(added);
    }

    default void addCardsToDeck(RogueRun run, EffectResultContext ctx, List<String> cardReferences) {
        if (cardReferences == null || cardReferences.isEmpty()) {
            return;
        }

        List<PaperCard> added = new ArrayList<>();
        for (String cardReferenceText : cardReferences) {
            CardReference cardReference = TextHelper.parseCardReference(cardReferenceText);
            if (cardReference.cardName().isEmpty()) {
                continue;
            }

            PaperCard card = RogueConfig.getCard(cardReference.cardName(),
                cardReference.setCode(), cardReference.artIndex());
            if (card != null) {
                added.add(card);
            }
        }

        if (added.isEmpty()) {
            return;
        }

        added = run.filterAddableCardsToDeck(added);
        if (added.isEmpty()) {
            return;
        }

        run.addCardsToDeck(added, false);
        ctx.addedCards.addAll(added);
    }

    default void addCardsFromCardRewardPool(RogueRun run, EffectResultContext ctx, int count,
                                            Predicate<PaperCard> filter) {
        RogueDeck rogueDeck = run.getSelectedRogueDeck();
        if (rogueDeck == null) {
            return;
        }

        Predicate<PaperCard> effectiveFilter = filter == null
            ? run.getNotAlreadyInDeckPredicate()
            : filter.and(run.getNotAlreadyInDeckPredicate());
        List<PaperCard> added = rogueDeck.drawRewardOptions(count, effectiveFilter);
        if (added.isEmpty()) {
            return;
        }

        run.addCardsToDeck(added, false);
        rogueDeck.removeFromCardPools(added);
        ctx.addedCards.addAll(added);
    }

    default void removeCardsFromDeck(RogueRun run, EffectResultContext ctx, Predicate<PaperCard> filter,
                                     Integer count) {
        List<PaperCard> removedCards;

        if (count == null) {
            removedCards = run.removeCardsFromDeck(filter);
        } else {
            removedCards = run.removeRandomCardsFromDeck(count, filter);
        }

        if (!removedCards.isEmpty()) {
            ctx.removedCards.addAll(removedCards);
        }
    }

    default void removeCarryCards(RogueRun run, EffectResultContext ctx, RogueRun.CarryCardType type) {
        if (!run.hasCarryCardOfType(type)) {
            return;
        }

        List<RogueRun.CarryCard> removedCarryCards = new ArrayList<>(run.getCarryCards().stream()
            .filter(card -> card.type() == type)
            .collect(Collectors.toList()));
        run.getCarryCards().removeIf(card -> card.type() == type);

        for (RogueRun.CarryCard carryCard : removedCarryCards) {
            PaperCard card = carryCard.toPaperCard();
            if (card != null) {
                ctx.removedCards.add(card);
            }
        }
    }

    default void selectCardsForDeck(RogueRun run, EffectResultContext ctx, Predicate<PaperCard> filter,
        Integer collectionCount, Integer minCount, Integer maxCount, List<CardReference> cardPrintOverrides) {

        List<PaperCard> collection = getAllCards(run, filter, null, cardPrintOverrides);
        if (collection.isEmpty()) {
            return;
        }

        setCandidateCardsFromCollection(ctx, collection, minCount, maxCount, collectionCount);
    }

    default void selectCardsForDeck(EffectResultContext ctx, List<PaperCard> collection,
        Integer minCount, Integer maxCount) {

        setCandidateCardsFromCollection(ctx, collection, minCount, maxCount, null);
    }

    default void selectCardsFromDeck(RogueRun run, EffectResultContext ctx, Predicate<PaperCard> filter,
        int removeMinCount, int removeMaxCount, List<PaperCard> replacementCards,
        EffectResultContext.CarryCardReward replacementCarryCard) {
        List<PaperCard> candidateCards = run.getSelectableDeckCards(filter);
        if (candidateCards.isEmpty()) {
            return;
        }

        ctx.candidateCards = candidateCards;
        ctx.cardSelectionMinCount = Math.min(removeMinCount, candidateCards.size());
        ctx.cardSelectionMaxCount = Math.min(removeMaxCount, candidateCards.size());
        ctx.replacementCards = replacementCards;
        ctx.replacementCarryCard = replacementCarryCard;
        if (ctx.cardSelectionMaxCount > 0) {
            ctx.trigger = EffectResultContext.ActionTriggerType.CARD_REMOVAL;
        }
    }

    default void triggerCustomBazaar(EffectResultContext ctx, String title, List<PaperCard> inventory,
                                     Map<String, Integer> priceOverrides) {
        BazaarContext bazaarContext = new BazaarContext();
        bazaarContext.title = title;
        for (PaperCard card : inventory) {
            Integer priceOverride = priceOverrides != null ? priceOverrides.get(card.getName()) : null;
            bazaarContext.inventory.add(BazaarItem.forCard(card, priceOverride));
        }

        ctx.bazaarContext = bazaarContext;
        ctx.trigger = EffectResultContext.ActionTriggerType.BAZAAR;
    }

    default void triggerCustomCardReward(EffectResultContext ctx, String title, List<PaperCard> rewardCards,
                                         int rewardCount, int maxPicks) {
        CardRewardContext cardRewardContext = new CardRewardContext(maxPicks);
        cardRewardContext.title = title;
        cardRewardContext.rewardCards.addAll(getSubsetFromCollection(new ArrayList<>(rewardCards), rewardCount));
        ctx.cardRewardContext = cardRewardContext;
        ctx.trigger = EffectResultContext.ActionTriggerType.CARD_REWARD;
    }

    default void triggerChest(EffectResultContext ctx) {
        ctx.trigger = EffectResultContext.ActionTriggerType.CHEST;
    }

    default void triggerSanctum(EffectResultContext ctx) {
        ctx.trigger = EffectResultContext.ActionTriggerType.SANCTUM;
    }

    default void triggerPlanebound(EffectResultContext ctx, RoguePlaneboundType type) {
        List<RoguePlanebound> all = RogueConfig.loadPlanebounds();
        all.removeIf(p -> p.type() != type);
        Collections.shuffle(all);
        if (all.isEmpty()) {
            return;
        }

        RoguePlanebound opponent = all.get(0);
        List<PaperCard> planes = RogueConfig.getAllPlanes().toFlatList();
        Collections.shuffle(planes);
        String randomPlaneName = planes.isEmpty() ? opponent.planeName() : planes.get(0).getName();

        ctx.planebound = new RoguePlanebound(randomPlaneName, opponent.planeboundName(),
            opponent.deckPath(), opponent.avatarIndex(), opponent.type());
        ctx.trigger = EffectResultContext.ActionTriggerType.PLANEBOUND;
    }

    default int rollD20() {
        return MyRandom.getRandom().nextInt(20) + 1;
    }

    default void gainWound(RogueRun run, EffectResultContext ctx) {
        List<WoundEffect> available = new ArrayList<>(List.of(WoundEffect.values()));
        List<RogueEffect> active = run.getActiveWoundEffects();
        available.removeIf(w -> active.stream().anyMatch(a -> a == w));
        if (available.isEmpty()) return;
        WoundEffect woundEffect = available.get(MyRandom.getRandom().nextInt(available.size()));
        run.addWound(woundEffect);
        CodexHelper.recordTraitAcquired(woundEffect);
        ctx.gainedWoundEffect = woundEffect;
    }

    default void swapDeckCards(RogueRun run, EffectResultContext ctx, List<PaperCard> collection) {
        Collections.shuffle(collection, MyRandom.getRandom());
        if (collection.size() > 20) {
            collection = collection.subList(0, 20);
        }
        int swapCount = Math.min(3, collection.size());
        if (swapCount == 0) {
            return;
        }

        removeCardsFromDeck(run, ctx, null, swapCount);
        selectCardsForDeck(ctx, collection, swapCount, swapCount);
    }

    // Override found cards with specific set / art if requested
    default List<PaperCard> applyCardPrintOverrides(List<PaperCard> cards, List<CardReference> cardPrintOverrides) {
        if (cards == null || cards.isEmpty() || cardPrintOverrides == null || cardPrintOverrides.isEmpty()) {
            return cards;
        }

        List<PaperCard> exactPrints = new ArrayList<>(cards.size());
        for (PaperCard card : cards) {
            PaperCard exactCard = card;
            for (CardReference cardReference : cardPrintOverrides) {
                if (!cardReference.matches(card)) {
                    continue;
                }

                PaperCard resolved = RogueConfig.getCard(cardReference.cardName(),
                    cardReference.setCode(), cardReference.artIndex());
                if (resolved != null) {
                    exactCard = resolved;
                }
                break;
            }
            exactPrints.add(exactCard);
        }
        return exactPrints;
    }

    static void addCardToCommandZone(String cardReference, RegisteredPlayer human) {
        CardReference cardReferenceParts = TextHelper.parseCardReference(cardReference);
        if (cardReferenceParts.cardName().isEmpty()) {
            return;
        }

        PaperCard card = RogueConfig.getCard(cardReferenceParts.cardName(),
            cardReferenceParts.setCode(), cardReferenceParts.artIndex());
        addCardToCommandZone(card, human);
    }

    static void addCardToCommandZone(PaperCard card, RegisteredPlayer human) {
        if (card != null) human.addExtraCardsInCommandZone(Collections.singletonList(card));
    }

    static void addCardToBattlefield(String cardReference, RegisteredPlayer human) {
        CardReference cardReferenceParts = TextHelper.parseCardReference(cardReference);
        if (cardReferenceParts.cardName().isEmpty()) {
            return;
        }

        PaperCard card = RogueConfig.getCard(cardReferenceParts.cardName(),
            cardReferenceParts.setCode(), cardReferenceParts.artIndex());
        addCardToBattlefield(card, human);
    }

    static void addCardToBattlefield(PaperCard card, RegisteredPlayer human) {
        if (card != null) human.addExtraCardsOnBattlefield(Collections.singletonList(card));
    }

    static void moveCardsFromDeckToBattlefield(Predicate<PaperCard> filter,
        Integer count, RegisteredPlayer player) {

        if (player == null) {
            return;
        }
        List<PaperCard> cards = new ArrayList<>();
        for (PaperCard card : player.getDeck().getMain().toFlatList()) {
            if (filter == null || filter.test(card)) {
                cards.add(card);
            }
        }
        List<PaperCard> selectedCards = getSubsetFromCollection(cards, count);
        if (selectedCards.isEmpty()) {
            return;
        }
        List<IPaperCard> toMove = new ArrayList<>(selectedCards);
        moveCardsFromDeckToBattlefield(toMove, player);
    }

    /**
     * Moves cards from the player's match deck to the starting battlefield.
     * Removes each card from the deck's main section so it doesn't also appear in the library.
     * Always use this instead of addExtraCardsOnBattlefield directly.
     */
    static void moveCardsFromDeckToBattlefield(List<IPaperCard> cards, RegisteredPlayer human) {
        CardPool main = human.getDeck().getMain();
        for (IPaperCard card : cards)
            main.remove((PaperCard) card, 1);
        human.addExtraCardsOnBattlefield(cards);
    }

    // Load all cards needed for effect
    private List<PaperCard> getAllCards(RogueRun run, Predicate<PaperCard> filter,
        Integer count, List<CardReference> cardPrintOverrides) {

        List<PaperCard> result = run.getAllCardsForActiveCommander(filter);
        if (result.isEmpty()) {
            return List.of();
        }

        List<PaperCard> selectedCards = getSubsetFromCollection(result, count);
        if (selectedCards.isEmpty()) {
            return List.of();
        }

        selectedCards = applyCardPrintOverrides(selectedCards, cardPrintOverrides);
        return selectedCards;
    }

    // Get random subset of cards if specific count was passed, otherwise return ALL cards
    private static List<PaperCard> getSubsetFromCollection(List<PaperCard> cards, Integer count) {
        if (cards.isEmpty()) {
            return List.of();
        }

        if (count == null) {
            return new ArrayList<>(cards);
        }

        if (count <= 0) {
            return List.of();
        }

        if (count == 1) {
            PaperCard selected = Aggregates.random(cards);
            return selected == null ? List.of() : new ArrayList<>(List.of(selected));
        }

        Collections.shuffle(cards, MyRandom.getRandom());
        return new ArrayList<>(cards.subList(0, Math.min(count, cards.size())));
    }

    private static void setCandidateCardsFromCollection(EffectResultContext ctx, List<PaperCard> collection,
        Integer minCount, Integer maxCount, Integer collectionCount) {

        Collections.shuffle(collection, MyRandom.getRandom());

        List<PaperCard> candidates;
        if (collectionCount != null) {
            candidates = new ArrayList<>(collection.subList(0, Math.min(collectionCount, collection.size())));
        } else {
            candidates = collection;
        }

        ctx.candidateCards = candidates;
        ctx.cardSelectionMinCount = Math.min(minCount, candidates.size());
        ctx.cardSelectionMaxCount = Math.min(maxCount, candidates.size());
        if (ctx.cardSelectionMaxCount > 0) {
            ctx.trigger = EffectResultContext.ActionTriggerType.CARD_ADDITION;
        }
    }
}
