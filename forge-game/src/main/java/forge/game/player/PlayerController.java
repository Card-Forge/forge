package forge.game.player;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.GameOutcome.AnteResult;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A prototype for player controller class
 *
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    public enum BinaryChoiceType {
        HeadsOrTails, // coin
        TapOrUntap,
        PlayOrDraw,
        OddsOrEvens,
        UntapOrLeaveTapped,
        LeftOrRight,
        AddOrRemove,
        IncreaseOrDecrease
    }

    public enum FullControlFlag {
        ChooseCostOrder,
        ChooseCostReductionOrderAndVariableAmount,
        //ChooseManaPoolShard, // select shard with special properties
        NoPaymentFromManaAbility,
        NoFreeCombatCostHandling,
        AllowPaymentStartWithMissingResources,
        LayerTimestampOrder // for StaticEffect$, tokens later etc.
    }

    private Set<FullControlFlag> fullControls = EnumSet.noneOf(FullControlFlag.class);

    protected final GameView gameView;

    protected final Player player;
    protected final LobbyPlayer lobbyPlayer;

    public PlayerController(Game game0, Player p, LobbyPlayer lp) {
        gameView = game0.getView();
        player = p;
        lobbyPlayer = lp;
    }

    public boolean isAI() {
        return false;
    }

    public Game getGame() { return gameView.getGame(); }
    public Match getMatch() { return gameView.getMatch(); }
    public Player getPlayer() { return player; }
    public LobbyPlayer getLobbyPlayer() { return lobbyPlayer; }

    public void tempShowCards(final Iterable<Card> cards) { } // show cards in UI until ended
    public void endTempShowCards() { }

    public final SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities) { return getAbilityToPlay(hostCard, abilities, null); }
    public abstract SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent);

    public abstract void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets);
    public abstract void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs);
    public abstract boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory);
    public abstract boolean playSaFromPlayEffect(SpellAbility tgtSA);

    public abstract List<PaperCard> sideboard(final Deck deck, GameType gameType, String message);
    public abstract List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses);

    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder);
    public abstract Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount);
    public abstract Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different);

    public abstract CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);
    public abstract CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);

    public abstract Integer announceRequirements(SpellAbility ability, String announce);
    public abstract TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional);
    public abstract boolean chooseTargetsFor(SpellAbility currentAbility); // this is bad a function for it assigns targets to sa inside its body

    // Specify a target of a spell (Spellskite)
    public abstract Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets);

    public abstract boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested);
    public abstract Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max);

    // Q: why is there min/max and optional at once? A: This is to handle cases like 'choose 3 to 5 cards or none at all'
    public abstract CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params);
    public abstract CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional);

    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title, Map<String, Object> params) { return chooseSingleEntityForEffect(optionList, null, sa, title, false, null, params); }
    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title, boolean isOptional, Map<String, Object> params) { return chooseSingleEntityForEffect(optionList, null, sa, title, isOptional, null, params); }
    public abstract <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player relatedPlayer, Map<String, Object> params);

    public abstract <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer, Map<String, Object> params);

    public abstract List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params);

    public abstract SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params);

    public final boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return confirmAction(sa, mode, message, Lists.newArrayList(), null, params);
    }
    public final boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Card cardToShow, Map<String, Object> params) {
        return confirmAction(sa, mode, message, Lists.newArrayList(), cardToShow, params);
    }
    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params);
    public abstract boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner);
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question);
    public abstract boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, String message, String logic);
    public abstract boolean confirmTrigger(WrappedAbility sa);

    public abstract List<Card> exertAttackers(List<Card> attackers);
    public abstract List<Card> enlistAttackers(List<Card> attackers);

    public abstract void declareAttackers(Player attacker, Combat combat);
    public abstract void declareBlockers(Player defender, Combat combat);

    public abstract CardCollection orderBlockers(Card attacker, CardCollection blockers);

    /**
     * Add a card to a pre-existing blocking order.
     * @param attacker the attacking creature.
     * @param blocker the new blocker.
     * @param oldBlockers the creatures already blocking the attacker (in order).
     * @return The new order of creatures blocking the attacker.
     */
    public abstract CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers);
    public abstract CardCollection orderAttackers(Card blocker, CardCollection attackers);

    /** Shows the card to this player*/
    public final void reveal(CardCollectionView cards, ZoneType zone, Player owner) {
        reveal(cards, zone, owner, null);
    }
    public final void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix) {
        reveal(cards, zone, owner, null, true);
    }
    public abstract void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addMsgSuffix);
    public final void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        reveal(cards, zone, owner, null, true);
    }
    public abstract void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addMsgSuffix);

    /** Shows message to player to reveal chosen cardName, creatureType, number etc. AI must analyze API to understand what that is */
    public abstract void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value);

    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN);
    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN);

    public abstract boolean willPutCardOnTop(Card c);

    /**
     * Prompts the player to choose the order for cards being moved into a zone.
     * The cards will be returned in the order that they should be moved, one at a time,
     * to the given zone and position. Be aware that when moving cards to the top of a
     * deck, this will be the reverse of the order they will ultimately end up in.
     */
    public abstract CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source);

    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max);
    public abstract CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa);
    public abstract CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard);

    public abstract CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave);
    public abstract Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise);
    public abstract List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards);

    public abstract CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid);
    public abstract List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand);
    public abstract Player chooseStartingPlayer(boolean isFirstGame);
    public abstract PlayerZone chooseStartingHand(List<PlayerZone> zones);
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);

    public abstract String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional);
    public final String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes) {
        return chooseSomeType(kindOfType, sa, validTypes, false);
    }

    public abstract String chooseSector(Card assignee, String ai, List<String> sectors);
    public final String chooseSector(Card assignee, String ai) {
        final List<String> sectors = Arrays.asList("Alpha", "Beta", "Gamma");
        return chooseSector(assignee, ai, sectors);
    }

    public abstract List<Card> chooseContraptionsToCrank(List<Card> contraptions);

    public abstract int chooseSprocket(Card assignee, boolean forceDifferent);
    public final int chooseSprocket(Card assignee) {
        return chooseSprocket(assignee, false);
    }

    public abstract PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls);
    public abstract Integer chooseRollToIgnore(List<Integer> rolls);
    public abstract List<Integer> chooseDiceToReroll(List<Integer> rolls);
    public abstract Integer chooseRollToModify(List<Integer> rolls);
    public abstract RollDiceEffect.DieRollResult chooseRollToSwap(List<RollDiceEffect.DieRollResult> rolls);
    public abstract String chooseRollSwapValue(List<String> swapChoices, Integer currentResult, int power, int toughness);

    public abstract Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional);

    public abstract boolean mulliganKeepHand(Player player, int cardsToReturn);
    public abstract CardCollectionView londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn);
    public abstract boolean confirmMulliganScry(final Player p);

    public abstract List<SpellAbility> chooseSpellAbilityToPlay();
    public abstract boolean playChosenSpellAbility(SpellAbility sa);

    public abstract List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat);

    public abstract int chooseNumberForCostReduction(final SpellAbility sa, final int min, final int max);
    public abstract int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max);
    public boolean addKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt) {
        return chooseNumberForKeywordCost(sa, cost, keyword, prompt, 1) == 1;
    }

    public abstract int chooseNumber(SpellAbility sa, String title, int min, int max);
    public abstract int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer);
    public int chooseNumber(SpellAbility sa, String string, int min, int max, Map<String, Object> params) {
        return chooseNumber(sa, string, min, max);
    }

    public final boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice) { return chooseBinary(sa, question, kindOfChoice, (Boolean) null); }
    public abstract boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChoice);
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Map<String, Object> params)  { return chooseBinary(sa, question, kindOfChoice); }

    public abstract boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call);

    public abstract MagicColor.Color chooseColor(String message, SpellAbility sa, ColorSet colors);
    public abstract MagicColor.Color chooseColorAllowColorless(String message, Card c, ColorSet colors);
    public abstract List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options);

    public abstract ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name);
    public abstract ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message);
    public abstract CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params);

    public abstract boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp);

    public abstract CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params);

    public abstract String chooseKeywordForPump(List<String> options, SpellAbility sa, String prompt, Card tgtCard);

    public abstract boolean confirmPayment(CostPart costPart, String string, SpellAbility sa);
    public abstract ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers);
    public abstract StaticAbility chooseSingleStaticAbility(String prompt, List<StaticAbility> possibleReplacers);
    public abstract String chooseProtectionType(String string, SpellAbility sa, List<String> choices);

    public abstract void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards);
    public abstract void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards);

    public abstract void revealUnsupported(Map<Player, List<PaperCard>> unsupported);

    // These 2 are for AI
    public CardCollectionView cheatShuffle(CardCollectionView list) { return list; }
    public Map<DeckSection, List<? extends PaperCard>> complainCardsCantPlayWell(Deck myDeck) { return null; }

    public abstract void resetAtEndOfTurn(); // currently used by the AI to perform card memory cleanup

    public abstract List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCostValues);

    public abstract List<CostPart> orderCosts(List<CostPart> costs);

    public abstract boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers);
    public abstract boolean payCostDuringRoll(Cost cost, SpellAbility sa, FCollectionView<Player> allPayers);

    public abstract boolean payCombatCost(Card card, Cost cost, SpellAbility sa, String prompt);

    public final boolean payManaCost(CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        return payManaCost(costPartMana.getManaCostFor(sa), costPartMana, sa, prompt, matrix, effect);
    }
    public abstract boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect);

    public abstract String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message);
    public abstract String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message);

    public abstract Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message);
    // better to have this odd method than those if playerType comparison in ChangeZone
    public abstract Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider);
    public abstract List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider);

    public Set<FullControlFlag> getFullControl() {
        return fullControls;
    }
    public boolean isFullControl(FullControlFlag f) {
        return fullControls.contains(f);
    }

    public abstract void autoPassCancel();

    public abstract void awaitNextInput();
    public abstract void cancelAwaitNextInput();

    public void resetInputs() {
        // Do nothing unless overridden by a subclass
    }

    public boolean isGuiPlayer() {
        return false;
    }

    public boolean canPlayUnlimitedLands() {
        return false;
    }

    public AnteResult getAnteResult() {
        return gameView.getAnteResult(player.getView());
    }

    public boolean isOrderedZone() { return false; }
}
