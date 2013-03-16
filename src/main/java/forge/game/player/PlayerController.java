package forge.game.player;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.Card;
import forge.GameEntity;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
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


    public PlayerController(GameState game0) {
        game = game0;
    }
    public abstract Input getDefaultInput();
    public abstract Input getBlockInput();
    public abstract Input getCleanupInput();


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
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public abstract SpellAbility getAbilityToPlay(List<SpellAbility> abilities);

    /**
     * TODO: Write javadoc for this method.
     */
    public void passPriority() {
        PhaseHandler handler = game.getPhaseHandler();

        if ( handler.getPriorityPlayer() == getPlayer() )
            game.getPhaseHandler().passPriority();
    }

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    public abstract void playFromSuspend(Card c);
    public abstract boolean playCascade(Card cascadedCard, Card sourceCard);
    public abstract void playSpellAbilityForFree(SpellAbility copySA);
    /**
     * @return the player
     */
    protected abstract Player getPlayer();
    
    
    public abstract Deck sideboard(final Deck deck, GameType gameType);


    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender);

    public abstract String announceRequirements(SpellAbility ability, String announce);

    public abstract List<Card> choosePermanentsToSacrifice(List<Card> validTargets, int amount, SpellAbility sa, boolean destroy, boolean isOptional);

    public Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title) { return chooseSingleCardForEffect(sourceList, sa, title, false); }
    public abstract Card chooseSingleCardForEffect(List<Card> sourceList, SpellAbility sa, String title, boolean isOptional);
    public abstract boolean confirmAction(SpellAbility sa, String mode, String message);
    public abstract boolean getWillPlayOnFirstTurn(String message);
    public abstract boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message);

    public abstract List<Card> orderBlockers(Card attacker, List<Card> blockers);
    public abstract List<Card> orderAttackers(Card blocker, List<Card> attackers);

    /** Shows the card to this player*/
    public abstract void reveal(String string, List<Card> cards, ZoneType zone, Player owner);
    public abstract ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN);
    public abstract boolean willPutCardOnTop(Card c);
    
    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract List<Card> chooseCardsToDiscardFrom(Player p, SpellAbility sa, List<Card> validCards, int min);
}
