/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.phase;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.event.EndOfTurnEvent;
import forge.game.event.ManaBurnEvent;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyObservable;


/**
 * <p>
 * Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PhaseHandler.java 13001 2012-01-08 12:25:25Z Sloth $
 */
public class PhaseHandler extends MyObservable implements java.io.Serializable {

    /** Constant <code>serialVersionUID=5207222278370963197L</code>. */
    private static final long serialVersionUID = 5207222278370963197L;

    private PhaseType phase = PhaseType.MULLIGAN;
    private int turn = 0;
    // Start turn at 0, so first untap step will turn it to 1

    private final transient Stack<ExtraTurn> extraTurns = new Stack<ExtraTurn>();

    private int extraCombats = 0;

    private int nCombatsThisTurn = 0;
    private boolean bPreventCombatDamageThisTurn  = false;

    private Player playerTurn = null;

    // priority player

    private Player pPlayerPriority = null;
    private Player pFirstPriority = null;
    private boolean bPhaseEffects = true;
    private boolean bCombat = false;
    private boolean bRepeat = false;

    /** The need to next phase. */
    private boolean isPlayerPriorityAllowed = false;

    private final transient GameState game;

    public PhaseHandler(final GameState game0) {

        game = game0;
    }

    /**
     * <p>
     * isPlayerTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public final boolean isPlayerTurn(final Player player) {
        return this.playerTurn.equals(player);
    }

    /**
     * <p>
     * Setter for the field <code>playerTurn</code>.
     * </p>
     * 
     * @param s
     *            a {@link forge.game.player.Player} object.
     */
    public final void setPlayerTurn(final Player s) {
        this.playerTurn = s;
        this.setPriority(s);
    }

    /**
     * <p>
     * Getter for the field <code>playerTurn</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getPlayerTurn() {
        return this.playerTurn;
    }

    // priority player

    /**
     * <p>
     * getPriorityPlayer.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getPriorityPlayer() {
        return this.pPlayerPriority;
    }


    /**
     * <p>
     * getFirstPriority.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    private final Player getFirstPriority() {
        return this.pFirstPriority;
    }

    /**
     * <p>
     * setPriority.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     */
    public final void setPriority(final Player p) {
        if (game.getStack() != null) {
            game.getStack().chooseOrderOfSimultaneousStackEntryAll();
        }

        this.pFirstPriority = p;
        this.pPlayerPriority = p;
    }

    /**
     * <p>
     * resetPriority.
     * </p>
     */
    public final void resetPriority() {
        this.setPriority(this.playerTurn);
    }

    /**
     * <p>
     * doPhaseEffects.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasPhaseEffects() {
        return this.bPhaseEffects;
    }

    /**
     * <p>
     * setPhaseEffects.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    private final void setPhaseEffects(final boolean b) {
        this.bPhaseEffects = b;
    }

    /**
     * <p>
     * inCombat.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean inCombat() {
        return this.bCombat;
    }

    /**
     * <p>
     * setCombat.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCombat(final boolean b) {
        this.bCombat = b;
    }

    /**
     * <p>
     * repeatPhase.
     * </p>
     */
    public final void repeatPhase() {
        this.bRepeat = true;
    }

    /**
     * <p>
     * handleBeginPhase.
     * </p>
     */
    public final void handleBeginPhase() {
        if (null == playerTurn) {
            return;
        }

        this.setPhaseEffects(false);
        // Handle effects that happen at the beginning of phases
        game.getAction().checkStateEffects();


        switch(this.getPhase()) {
            case UNTAP:
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                PhaseUtil.handleUntap(game);
                break;

            case UPKEEP:
                if (this.getPlayerTurn().hasKeyword("Skip your upkeep step.")) {
                    // Slowtrips all say "on the next turn's upkeep" if there is no
                    // upkeep next turn, the trigger will never occur.
                    for (Player p : game.getPlayers()) {
                        p.clearSlowtripList();
                    }
                    this.setPlayerMayHavePriority(false);
                } else {
                    game.getUpkeep().executeUntil(this.getPlayerTurn());
                    game.getUpkeep().executeAt();
                }
                break;

            case DRAW:
                if (getTurn() == 1 || PhaseUtil.skipDraw(this.getPlayerTurn())) {
                    this.setPlayerMayHavePriority(false);
                } else {
                    this.getPlayerTurn().drawCards(1, true);
                }
                break;

            case COMBAT_BEGIN:
                //PhaseUtil.verifyCombat();
                if (playerTurn.isSkippingCombat()) {
                    this.setPlayerMayHavePriority(false);
                }
                break;

            case COMBAT_DECLARE_ATTACKERS:
                if (playerTurn.isSkippingCombat()) {
                    this.setPlayerMayHavePriority(false);
                    playerTurn.removeKeyword("Skip your next combat phase.");
                }
                break;

            case COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY:
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareAttackers(game.getCombat());
                    CombatUtil.showCombat();
                } else {
                    this.setPlayerMayHavePriority(false);
                }
                break;
            // we can skip AfterBlockers and AfterAttackers if necessary
            case COMBAT_DECLARE_BLOCKERS:
                if (this.inCombat()) {
                    game.getCombat().verifyCreaturesInPlay();
                    CombatUtil.showCombat();
                } else {
                    this.setPlayerMayHavePriority(false);
                }
                break;

            case COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY:
                // After declare blockers are finished being declared mark them
                // blocked and trigger blocking things
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareBlockers(game);
                    CombatUtil.showCombat();
                } else {
                    this.setPlayerMayHavePriority(false);
                }
                break;

            case COMBAT_FIRST_STRIKE_DAMAGE:
                if (!this.inCombat()) {
                    this.setPlayerMayHavePriority(false);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    // no first strikers, skip this step
                    if (!game.getCombat().assignCombatDamage(true)) {
                        this.setPlayerMayHavePriority(false);
                    } else {
                        game.getCombat().dealAssignedDamage();
                        game.getAction().checkStateEffects();
                        CombatUtil.showCombat();
                    }
                }
                break;

            case COMBAT_DAMAGE:
                if (!this.inCombat()) {
                    this.setPlayerMayHavePriority(false);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    if (!game.getCombat().assignCombatDamage(false)) {
                        this.setPlayerMayHavePriority(false);
                    } else {
                        game.getCombat().dealAssignedDamage();
                        game.getAction().checkStateEffects();
                        CombatUtil.showCombat();
                    }
                }
                break;

            case COMBAT_END:
                // End Combat always happens
                game.getEndOfCombat().executeUntil();
                game.getEndOfCombat().executeAt();
                CombatUtil.showCombat();
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                break;

            case MAIN2:
                CombatUtil.showCombat();
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                break;

            case END_OF_TURN:
                game.getEndOfTurn().executeAt();
                break;

            case CLEANUP:
                // Reset Damage received map
                for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                    c.onCleanupPhase(playerTurn);
                }

                game.getEndOfTurn().executeUntil();

                for (Player player : game.getPlayers()) {
                    player.onCleanupPhase();
                    player.getController().autoPassCancel(); // autopass won't wrap to next turn
                }
                this.getPlayerTurn().removeKeyword("Skip all combat phases of this turn.");
                game.getCleanup().executeUntilTurn(this.getNextTurn());
                break;

            default:
                break;
          }

        if (this.mayPlayerHavePriority()) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", this.getPhase().Name);
            runParams.put("Player", this.getPlayerTurn());
            game.getTriggerHandler().runTrigger(TriggerType.Phase, runParams);

        }

        // This line fixes Combat Damage triggers not going off when they should
        game.getStack().unfreezeStack();

        // UNTAP
        if (this.getPhase() != PhaseType.UNTAP) {
            // during untap
            this.resetPriority();
        }
    }

    /**
     * Checks if is prevent combat damage this turn.
     * 
     * @return true, if is prevent combat damage this turn
     */
    public final boolean isPreventCombatDamageThisTurn() {
        return this.bPreventCombatDamageThisTurn;
    }

    /**
     * <p>
     * nextPhase.
     * </p>
     */
    public final void nextPhase() {

        this.setPlayerMayHavePriority(true); //  PlayerPriorityAllowed = false;

        // If the Stack isn't empty why is nextPhase being called?
        if (game.getStack().size() != 0) {
            Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
        setPhaseEffects(true);

        for (Player p : game.getPlayers()) {
            int burn = p.getManaPool().clearPool(true);
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN)) {
                p.loseLife(burn);

                // Play the Mana Burn sound
                Singletons.getModel().getGame().getEvents().post(new ManaBurnEvent());
            }
            p.updateObservers();
        }

        switch (this.phase) {
            case UNTAP:
                this.nCombatsThisTurn = 0;
                break;

            case COMBAT_DECLARE_ATTACKERS:
                game.getStack().unfreezeStack();
                this.nCombatsThisTurn++;
                break;

            case COMBAT_DECLARE_BLOCKERS:
                game.getStack().unfreezeStack();
                break;

            case COMBAT_END:
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                game.getCombat().reset();
                this.resetAttackedThisCombat(this.getPlayerTurn());
                this.bCombat = false;

                // TODO: ExtraCombat needs to be changed for other spell/abilities
                // that give extra combat can do it like ExtraTurn stack ExtraPhases
                if (this.extraCombats > 0) {
                    final Player player = this.getPlayerTurn();

                    this.bCombat = true;
                    this.extraCombats--;
                    game.getCombat().reset();
                    game.getCombat().setAttackingPlayer(player);
                    this.phase = PhaseType.COMBAT_BEGIN;
                }
                break;

            case CLEANUP:
                this.bPreventCombatDamageThisTurn = false;
                if (!this.bRepeat) {
                    this.setPlayerTurn(this.handleNextTurn());
                }
                // Play the End Turn sound
                Singletons.getModel().getGame().getEvents().post(new EndOfTurnEvent());
                break;
            default: // no action
        }

        if (this.bRepeat) { // for when Cleanup needs to repeat itself
            this.bRepeat = false;
        } else {
            this.phase = phase.getNextPhase();
        }

        game.getGameLog().add("Phase", this.getPlayerTurn() + " " + this.getPhase().Name, 6);

        // **** Anything BELOW Here is actually in the next phase. Maybe move
        // this to handleBeginPhase
        if (this.phase == PhaseType.UNTAP) {
            this.turn++;
            game.getGameLog().add("Turn", "Turn " + this.turn + " (" + this.getPlayerTurn() + ")", 0);
        }

        PhaseUtil.visuallyActivatePhase(this.getPlayerTurn(), this.getPhase());

        // When consecutively skipping phases (like in combat) this section
        // pushes through that block
        this.updateObservers();
        // it no longer does.
    }

    /**
     * <p>
     * handleNextTurn.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    private Player handleNextTurn() {

        game.getStack().setCardsCastLastTurn();
        game.getStack().clearCardsCastThisTurn();

        for (final Player p1 : game.getPlayers()) {
            for (final ZoneType z : Player.ALL_ZONES) {
                p1.getZone(z).resetCardsAddedThisTurn();
            }
        }
        for (Player p : game.getPlayers()) {

            p.resetProwl();
            p.setLifeLostThisTurn(0);

            p.removeKeyword("At the beginning of this turn's end step, you lose the game.");
            p.removeKeyword("Skip the untap step of this turn.");
        }

        return getNextActivePlayer();
    }

    /**
     * <p>
     * getNextActivePlayer.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    private Player getNextActivePlayer() {
        Player nextTurn = game.getNextPlayerAfter(this.getPlayerTurn());
        if (!this.extraTurns.isEmpty()) {
            ExtraTurn extraTurn = this.extraTurns.pop();
            nextTurn = extraTurn.getPlayer();
            if (nextTurn.skipTurnTimeVault()) {
                return getNextActivePlayer();
            }
            if (extraTurn.isLoseAtEndStep()) {
                nextTurn.addKeyword("At the beginning of this turn's end step, you lose the game.");
            }
            if (extraTurn.isSkipUntap()) {
                nextTurn.addKeyword("Skip the untap step of this turn.");
            }
            return nextTurn;
        }
        if (nextTurn.skipTurnTimeVault()) {
            this.setPlayerTurn(nextTurn);
            return getNextActivePlayer();
        }
        return nextTurn;
    }

    /**
     * <p>
     * is.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public final synchronized boolean is(final PhaseType phase, final Player player) {
        return this.getPhase() == phase && this.getPlayerTurn().equals(player);
    }

    /**
     * <p>
     * is.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final synchronized boolean is(final PhaseType phase0) {
        return this.getPhase() == phase0;
    }

    /**
     * <p>
     * getPhase.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final PhaseType getPhase() {
        return phase;
    }

    /**
     * <p>
     * Getter for the field <code>turn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getTurn() {
        return this.turn;
    }

    /**
     * <p>
     * getNextTurn.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getNextTurn() {
        if (this.extraTurns.isEmpty()) {
            return game.getNextPlayerAfter(this.getPlayerTurn());
        }

        return this.extraTurns.peek().getPlayer();
    }


    /**
     * <p>
     * addExtraTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final ExtraTurn addExtraTurn(final Player player) {
        // use a stack to handle extra turns, make sure the bottom of the stack
        // restores original turn order
        if (this.extraTurns.isEmpty()) {
            this.extraTurns.push(new ExtraTurn(game.getNextPlayerAfter(this.getPlayerTurn())));
        }

        return this.extraTurns.push(new ExtraTurn(player));
    }


    /**
     * <p>
     * addExtraCombat.
     * </p>
     */
    public final void addExtraCombat() {
        // Extra combats can only happen
        this.extraCombats++;
    }

    /**
     * <p>
     * isFirstCombat.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFirstCombat() {
        return (this.nCombatsThisTurn == 1);
    }

    /**
     * <p>
     * resetAttackedThisCombat.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void resetAttackedThisCombat(final Player player) {
        // resets the status of attacked/blocked this phase
        List<Card> list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.getDamageHistory().getCreatureAttackedThisCombat()) {
                c.getDamageHistory().setCreatureAttackedThisCombat(false);
            }
            if (c.getDamageHistory().getCreatureBlockedThisCombat()) {
                c.getDamageHistory().setCreatureBlockedThisCombat(false);
            }

            if (c.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                c.getDamageHistory().setCreatureGotBlockedThisCombat(false);
            }
        }
    }

    /**
     * <p>
     * passPriority.
     * </p>
     */
    public final void passPriority() {
        // stop game if it's outcome is clear
        if (game.isGameOver()) {
            return;
        }

        final Player actingPlayer = this.getPriorityPlayer();
        final Player firstAction = this.getFirstPriority();

        // actingPlayer is the player who may act
        // the firstAction is the player who gained Priority First in this segment
        // of Priority

        Player nextPlayer = game.getNextPlayerAfter(actingPlayer);
        if (firstAction.equals(nextPlayer)) {
            if (game.getStack().isEmpty()) {
                this.setPriority(this.getPlayerTurn()); // this needs to be set early as we exit the phase
                // end phase
                setPlayerMayHavePriority(true);
                nextPhase();
                return;
            } else if (!game.getStack().hasSimultaneousStackEntries()) {
                game.getStack().resolveStack();
            }
        } else {
            // pass the priority to other player
            this.pPlayerPriority = nextPlayer;
            Singletons.getModel().getMatch().getInput().resetInput();

        }
        game.getStack().chooseOrderOfSimultaneousStackEntryAll();
    }

    /**
     * <p>
     * Setter for the field <code>needToNextPhase</code>.
     * </p>
     * 
     * @param needToNextPhase
     *            a boolean.
     */
    public final void setPlayerMayHavePriority(final boolean mayHavePriority) {
        this.isPlayerPriorityAllowed = mayHavePriority;
    }

    /**
     * <p>
     * isNeedToNextPhase.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean mayPlayerHavePriority() {
        return this.isPlayerPriorityAllowed;
    }

    // this is a hack for the setup game state mode, do not use outside of
    // devSetupGameState code
    // as it avoids calling any of the phase effects that may be necessary in a
    // less enforced context
    /**
     * <p>
     * setDevPhaseState.
     * </p>
     * 
     * @param phase
     *            a {@link java.forge.game.phase.PhaseType} object.
     */
    public final void setDevPhaseState(final PhaseType phase0) {
        this.phase = phase0;
    }

    /**
     * Sets the phase state.
     *
     * @param phaseID the new phase state
     */
    public final void setPhaseState(final PhaseType phase0) {
        this.phase = phase0;
        this.handleBeginPhase();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param b
     *            a boolean
     */
    public final void setPreventCombatDamageThisTurn(final boolean b) {
        this.bPreventCombatDamageThisTurn = true;
    }
}
