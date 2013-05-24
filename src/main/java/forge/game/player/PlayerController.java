package forge.game.player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.Card;
import forge.GameEntity;
import forge.card.mana.Mana;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.control.input.InputAutoPassPriority;
import forge.deck.Deck;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    protected final GameState game;
    
    private PhaseType autoPassUntil = null;
    private final Input autoPassPriorityInput;
    protected final Player player;

    public PlayerController(GameState game0, Player p) {
        game = game0;
        player = p;
        autoPassPriorityInput = new InputAutoPassPriority(player);
    }
    
    public abstract Input getDefaultInput();
    public abstract Input getBlockInput();
    public abstract Input getCleanupInput();
    public final Input getAutoPassPriorityInput() { return autoPassPriorityInput; }

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

    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        return false; // human has it's overload
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void passPriority() {
        PhaseHandler handler = game.getPhaseHandler();
    
        if ( handler.getPriorityPlayer() == player )
            game.getPhaseHandler().passPriority();
    }

    // Triggers preliminary choice: ask, decline or play
    private Map<Integer, Boolean> triggersAlwaysAccept = new HashMap<Integer, Boolean>();

    public final  boolean shouldAlwaysAcceptTrigger(Integer trigger) { return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger)); }
    public final boolean shouldAlwaysDeclineTrigger(Integer trigger) { return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger)); }

    public final void setShouldAlwaysAcceptTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, true); }
    public final void setShouldAlwaysDeclineTrigger(Integer trigger) { triggersAlwaysAccept.put(trigger, false); }
    public final void setShouldAlwaysAskTrigger(Integer trigger) { triggersAlwaysAccept.remove(trigger); }

    // End of Triggers preliminary choice


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
    public abstract void playSpellAbilityForFree(SpellAbility copySA);

   
    
    public abstract Deck sideboard(final Deck deck, GameType gameType);


    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender);

    public abstract Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero);
    public abstract List<Card> choosePermanentsToSacrifice(List<Card> validTargets, String validMessage, int amount, SpellAbility sa, boolean destroy, boolean isOptional);
    public abstract Target chooseTargets(SpellAbility ability);

    public Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title) { return chooseSingleCardForEffect(sourceList, sa, title, false); }
    public abstract Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title, boolean isOptional);
    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message);
    public abstract boolean getWillPlayOnFirstTurn(String message);
    public abstract boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message);

    public abstract List<Card> orderBlockers(Card attacker, List<Card> blockers);
    public abstract List<Card> orderAttackers(Card blocker, List<Card> attackers);

    /** Shows the card to this player*/
    public abstract void reveal(String string, Collection<Card> cards, ZoneType zone, Player owner);
    public abstract ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN);
    public abstract boolean willPutCardOnTop(Card c);
    public abstract List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone);
    
    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract List<Card> chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, List<Card> validCards, int min, int max);
    public abstract Card chooseCardToDredge(List<Card> dredgers);

    public abstract void playMiracle(SpellAbility miracle, Card card);
    public abstract void playMadness(SpellAbility madness);
    public abstract List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave);
    public abstract List<Card> chooseCardsToDiscardUnlessType(int min, List<Card> hand, String param, SpellAbility sa);
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);
    
    public abstract String chooseSomeType(String kindOfType, String aiLogic, List<String> validTypes, List<String> invalidTypes);
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question);
    public abstract List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer);
}
