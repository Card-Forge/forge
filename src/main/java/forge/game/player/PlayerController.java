package forge.game.player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.GameEntity;
import forge.ITargetable;
import forge.card.cost.Cost;
import forge.card.mana.Mana;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.TargetChoices;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    public static enum ManaPaymentPurpose {
        DeclareAttacker,
        DeclareBlocker,
        Recover,
        Echo,
        Multikicker,
        Replicate, 
        CumulativeUpkeep;
    }
    
    protected final Game game;
    
    private PhaseType autoPassUntil = null;
    protected final Player player;
    protected final LobbyPlayer lobbyPlayer;

    public PlayerController(Game game0, Player p, LobbyPlayer lp) {
        game = game0;
        player = p;
        lobbyPlayer = lp;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param cleanup
     */
    public void autoPassTo(PhaseType cleanup) {
        autoPassUntil = cleanup;
    }
    public void autoPassCancel() {
        autoPassUntil = null;
    }

    public boolean mayAutoPass(PhaseType phase) {
        return phase.isBefore(autoPassUntil);
    }


    // Triggers preliminary choice: ask, decline or play
    private Map<Integer, Boolean> triggersAlwaysAccept = new HashMap<Integer, Boolean>();

    public final  boolean shouldAlwaysAcceptTrigger(Integer trigger) { return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger)); }
    public final boolean shouldAlwaysDeclineTrigger(Integer trigger) { return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger)); }

    public final void setShouldAlwaysAcceptTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, true); }
    public final void setShouldAlwaysDeclineTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, false); }
    public final void setShouldAlwaysAskTrigger(Integer trigger) { triggersAlwaysAccept.remove(trigger); }

    // End of Triggers preliminary choice

    public LobbyPlayer getLobbyPlayer() { return lobbyPlayer; }
    
    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public abstract SpellAbility getAbilityToPlay(List<SpellAbility> abilities);

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    //public abstract void playFromSuspend(Card c);
    public abstract boolean playCascade(Card cascadedCard, Card sourceCard);
    public abstract void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets);
    public abstract void playSpellAbilityNoStack(Player player, SpellAbility effectSA);

    public abstract Deck sideboard(final Deck deck, GameType gameType);


    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender, boolean overrideOrder);

    public abstract Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero);
    public abstract List<Card> choosePermanentsToSacrifice(SpellAbility sa, int min, int max, List<Card> validTargets, String message);
    public abstract List<Card> choosePermanentsToDestroy(SpellAbility sa, int min, int max, List<Card> validTargets, String message);
    public abstract TargetChoices chooseNewTargetsFor(SpellAbility ability);

    // Specify a target of a spell (Spellskite)
    public abstract Pair<SpellAbilityStackInstance, ITargetable> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, ITargetable>> allTargets);

    public Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title) { return chooseSingleCardForEffect(sourceList, sa, title, false); }
    public abstract Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title, boolean isOptional);
    public abstract Player chooseSinglePlayerForEffect(List<Player> options, SpellAbility sa, String title);
    public abstract SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title);

    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message);
    public abstract boolean getWillPlayOnFirstTurn(boolean isFirstGame);
    public abstract boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message);

    public abstract List<Card> orderBlockers(Card attacker, List<Card> blockers);
    public abstract List<Card> orderAttackers(Card blocker, List<Card> attackers);

    /** Shows the card to this player*/
    public abstract void reveal(String string, Collection<Card> cards, ZoneType zone, Player owner);
    /** Shows message to player to reveal chosen cardName, creatureType, number etc. AI must analyze API to understand what that is */
    public abstract void notifyOfValue(SpellAbility saSource, ITargetable realtedTarget, String value);
    public abstract ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN);
    public abstract boolean willPutCardOnTop(Card c);
    public abstract List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone);
    
    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract List<Card> chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, List<Card> validCards, int min, int max);
    public abstract Card chooseCardToDredge(List<Card> dredgers);

    public abstract void playMiracle(SpellAbility miracle, Card card);
    public abstract void playMadness(SpellAbility madness);
    public abstract List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave);
    public abstract List<Card> chooseCardsToRevealFromHand(int min, int max, List<Card> valid);
    public abstract List<Card> chooseCardsToDiscardUnlessType(int min, List<Card> hand, String param, SpellAbility sa);
    public abstract List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand);
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);
    
    public abstract String chooseSomeType(String kindOfType, String aiLogic, List<String> validTypes, List<String> invalidTypes);
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question);
    public abstract List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer);

    public abstract void declareAttackers(Player attacker, Combat combat);
    public abstract void declareBlockers(Player defender, Combat combat);
    public abstract void takePriority();
    
    public abstract List<Card> chooseCardsToDiscardToMaximumHandSize(int numDiscard);
    public abstract boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose);

    public abstract int chooseNumber(SpellAbility sa, String title, int min, int max);

    public abstract boolean chooseBinary(SpellAbility sa, String question, boolean isCoin);
    public abstract boolean chooseFilpResult(SpellAbility sa, Player flipper, boolean[] results, boolean call);
    public abstract Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap);

}
