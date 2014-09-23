package forge.game.player;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardShields;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    public static enum ManaPaymentPurpose {
        DeclareAttacker,
        DeclareBlocker,
        Echo,
        Multikicker,
        Replicate,
        CumulativeUpkeep;
    }

    public static enum BinaryChoiceType {
        HeadsOrTails, // coin
        TapOrUntap,
        PlayOrDraw,
        OddsOrEvens,
        UntapOrLeaveTapped,
        UntapTimeVault,
        LeftOrRight,
    }

    protected final Game game;

    private PhaseType autoPassUntilPhase = null;
    protected final Player player;
    protected final LobbyPlayer lobbyPlayer;

    public PlayerController(Game game0, Player p, LobbyPlayer lp) {
        game = game0;
        player = p;
        lobbyPlayer = lp;
    }

    /**
     * Automatically pass priority until reaching the Cleanup phase of the
     * current turn.
     */
    public void autoPassUntilEndOfTurn() {
        autoPassUntilPhase = PhaseType.CLEANUP;
    }
    
    protected PhaseType getAutoPassUntilPhase() {
        return autoPassUntilPhase;
    }

    public void autoPassCancel() {
        autoPassUntilPhase = null;
    }

    public boolean mayAutoPass() {
        if (autoPassUntilPhase == null) { return false; }

        PhaseType currentPhase = game.getPhaseHandler().getPhase();
        if (currentPhase.isBefore(autoPassUntilPhase)) {
            if (currentPhase == PhaseType.COMBAT_DECLARE_BLOCKERS && game.getPhaseHandler().isPlayerTurn(player)) {
                if (!game.getCombat().getAllBlockers().isEmpty()) {
                    return false; //prevent auto-passing on Declare Blockers phase if it's your turn and your opponent blocks a creature you attacked with
                }
            }
            return true;
        }
        return false;
    }

    public boolean isAI() {
        return false;
    }

    // Abilities to auto-yield to
    private final Set<String> autoYields = Sets.newHashSet();
    public Iterable<String> getAutoYields() {
        return autoYields;
    }
    public boolean shouldAutoYield(final String key) {
        return autoYields.contains(key);
    }
    public void setShouldAutoYield(final String key, final boolean autoYield) {
        if (autoYield) {
            autoYields.add(key);
        }
        else {
            autoYields.remove(key);
        }
    }

    // Triggers preliminary choice: ask, decline or play
    private Map<Integer, Boolean> triggersAlwaysAccept = new HashMap<Integer, Boolean>();

    public final boolean shouldAlwaysAcceptTrigger(Integer trigger) { return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger)); }
    public final boolean shouldAlwaysDeclineTrigger(Integer trigger) { return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger)); }
    public final boolean shouldAlwaysAskTrigger(Integer trigger) { return !triggersAlwaysAccept.containsKey(trigger); }

    public final void setShouldAlwaysAcceptTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, true); }
    public final void setShouldAlwaysDeclineTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, false); }
    public final void setShouldAlwaysAskTrigger(Integer trigger) { triggersAlwaysAccept.remove(trigger); }

    // End of Triggers preliminary choice

    public Game getGame() { return game; }
    public Player getPlayer() { return player; }
    public LobbyPlayer getLobbyPlayer() { return lobbyPlayer; }

    public final SpellAbility getAbilityToPlay(List<SpellAbility> abilities) { return getAbilityToPlay(abilities, null); }
    public abstract SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent);

    //public abstract void playFromSuspend(Card c);
    public abstract void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets);
    public abstract void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets);

    public abstract List<PaperCard> sideboard(final Deck deck, GameType gameType);
    public abstract List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses);

    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender, boolean overrideOrder);

    public abstract Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero);
    public abstract List<Card> choosePermanentsToSacrifice(SpellAbility sa, int min, int max, List<Card> validTargets, String message);
    public abstract List<Card> choosePermanentsToDestroy(SpellAbility sa, int min, int max, List<Card> validTargets, String message);
    public abstract TargetChoices chooseNewTargetsFor(SpellAbility ability);
    public abstract boolean chooseTargetsFor(SpellAbility currentAbility); // this is bad a function for it assigns targets to sa inside its body 

    // Specify a target of a spell (Spellskite)
    public abstract Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets);

    // Q: why is there min/max and optional at once? A: This is to handle cases like 'choose 3 to 5 cards or none at all'  
    public abstract List<Card> chooseCardsForEffect(List<Card> sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional);
    
    public final <T extends GameEntity> T chooseSingleEntityForEffect(Collection<T> sourceList, SpellAbility sa, String title) { return chooseSingleEntityForEffect(sourceList, sa, title, false, null); }
    public final <T extends GameEntity> T chooseSingleEntityForEffect(Collection<T> sourceList, SpellAbility sa, String title, boolean isOptional) { return chooseSingleEntityForEffect(sourceList, sa, title, isOptional, null); } 
    public abstract <T extends GameEntity> T chooseSingleEntityForEffect(Collection<T> sourceList, SpellAbility sa, String title, boolean isOptional, Player relatedPlayer);
    public abstract SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title);

    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message);
    public abstract boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner);
    public abstract boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message);
    public abstract boolean confirmTrigger(SpellAbility sa, Trigger regtrig, Map<String, String> triggerParams, boolean isMandatory);
    public abstract Player chooseStartingPlayer(boolean isFirstGame);

    public abstract List<Card> orderBlockers(Card attacker, List<Card> blockers);
    /**
     * Add a card to a pre-existing blocking order.
     * @param attacker the attacking creature.
     * @param blocker the new blocker.
     * @param oldBlockers the creatures already blocking the attacker (in order).
     * @return The new order of creatures blocking the attacker.
     */
    public abstract List<Card> orderBlocker(final Card attacker, final Card blocker, final List<Card> oldBlockers);
    public abstract List<Card> orderAttackers(Card blocker, List<Card> attackers);

    /** Shows the card to this player*/
    public final void reveal(Collection<Card> cards, ZoneType zone, Player owner) {
        reveal(cards, zone, owner, null);
    }
    public abstract void reveal(Collection<Card> cards, ZoneType zone, Player owner, String messagePrefix);
    /** Shows message to player to reveal chosen cardName, creatureType, number etc. AI must analyze API to understand what that is */
    public abstract void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value);
    public abstract ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN);
    public abstract boolean willPutCardOnTop(Card c);
    public abstract List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone);

    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract List<Card> chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, List<Card> validCards, int min, int max);

    public abstract void playMiracle(SpellAbility miracle, Card card);
    public abstract List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave);
    public abstract List<Card> chooseCardsToRevealFromHand(int min, int max, List<Card> valid);
    public abstract List<Card> chooseCardsToDiscardUnlessType(int min, List<Card> hand, String param, SpellAbility sa);
    public abstract List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand);
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);

    public abstract String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes, boolean isOptional);
    public final String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes) {
        return chooseSomeType(kindOfType, sa, validTypes, invalidTypes, false);
    }

    public abstract Object vote(SpellAbility sa, String prompt, List<Object> options, ArrayListMultimap<Object, Player> votes);
    public abstract Pair<CounterType,String> chooseAndRemoveOrPutCounter(Card cardWithCounter);
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question);
    public abstract List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer);

    public abstract void declareAttackers(Player attacker, Combat combat);
    public abstract void declareBlockers(Player defender, Combat combat);
    public abstract SpellAbility chooseSpellAbilityToPlay();
    public abstract void playChosenSpellAbility(SpellAbility sa);

    public abstract List<Card> chooseCardsToDiscardToMaximumHandSize(int numDiscard);
    public abstract boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose);

    public abstract int chooseNumber(SpellAbility sa, String title, int min, int max);
    public abstract int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer);

    public final boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice) { return chooseBinary(sa, question, kindOfChoice, null); }
    public abstract boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChioce);
    public abstract boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call);
    public abstract Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap);

    public abstract List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num);

    public abstract byte chooseColor(String message, SpellAbility sa, ColorSet colors);
    public abstract byte chooseColorAllowColorless(String message, Card c, ColorSet colors);

    public abstract PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name);
    public abstract List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options);
    public abstract CounterType chooseCounterType(Collection<CounterType> options, SpellAbility sa, String prompt);

    public abstract boolean confirmPayment(CostPart costPart, String string);
    public abstract ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers, HashMap<String, Object> runParams);
    public abstract String chooseProtectionType(String string, SpellAbility sa, List<String> choices);
	public abstract CardShields chooseRegenerationShield(Card c);
	
    // these 4 need some refining.
    public abstract boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, List<Player> allPayers);
    public abstract void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs);
    public abstract void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory);

    public abstract boolean playSaFromPlayEffect(SpellAbility tgtSA);
    public abstract Map<GameEntity, CounterType> chooseProliferation();
    public abstract boolean chooseCardsPile(SpellAbility sa, List<Card> pile1,List<Card> pile2, boolean faceUp);

    public abstract void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards);

    // These 2 are for AI
    public List<Card> cheatShuffle(List<Card> list) { return list; }
    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) { return null; }

    public abstract void resetAtEndOfTurn(); // currently used by the AI to perform card memory cleanup

    public final boolean payManaCost(CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedAbility) {
        return payManaCost(costPartMana.getManaCostFor(sa), costPartMana, sa, prompt, isActivatedAbility);
    }
    public abstract boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedAbility);

    public abstract Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost, List<Card> untappedCreats);

    public abstract String chooseCardName(SpellAbility sa, Predicate<PaperCard> cpp, String valid, String message);

    // better to have this odd method than those if playerType comparison in ChangeZone  
    public abstract Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, List<Card> fetchList, String selectPrompt, boolean isOptional, Player decider);

    public boolean isGuiPlayer() {
        return false;
    }

    public boolean canPlayUnlimitedLands() {
        return false;
    }
}