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
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;

import com.esotericsoftware.minlog.Log;

import forge.Card;
import forge.CardLists;
import forge.FThreads;
import forge.GameEventType;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.event.EndOfTurnEvent;
import forge.game.event.ManaBurnEvent;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
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

    // Start turn at 0, since we start even before first untap
    private PhaseType phase = null;
    private int turn = 0;


    private final transient Stack<ExtraTurn> extraTurns = new Stack<ExtraTurn>();
    private final transient Map<PhaseType, Stack<PhaseType>> extraPhases = new HashMap<PhaseType, Stack<PhaseType>>();

    private int nUpkeepsThisTurn = 0;
    private int nCombatsThisTurn = 0;
    private boolean bPreventCombatDamageThisTurn  = false;
    private int planarDiceRolledthisTurn = 0;

    private Player playerTurn = null;

    // priority player

    private Player pPlayerPriority = null;
    private Player pFirstPriority = null;
    private AtomicBoolean bCombat = new AtomicBoolean(false);
    private boolean bRepeatCleanup = false;

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
        return player.equals(this.playerTurn);
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
     * inCombat.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean inCombat() {
        return this.bCombat.get();
    }

    /**
     * <p>
     * setCombat.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCombat(boolean value) {
        this.bCombat.set(value);
    }

    /**
     * <p>
     * repeatPhase.
     * </p>
     */
    public final void repeatPhase() {
        this.bRepeatCleanup = true;
    }

    /**
     * <p>
     * nextPhase.
     * </p>
     */
    private final void nextPhase() {
        this.setPlayersPriorityPermission(true); //  PlayerPriorityAllowed = false;
    
        // If the Stack isn't empty why is nextPhase being called?
        if (!game.getStack().isEmpty()) {
            Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
    
        for (Player p : game.getPlayers()) {
            int burn = p.getManaPool().clearPool(true);
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN)) {
                p.loseLife(burn);
    
                // Play the Mana Burn sound
                game.getEvents().post(new ManaBurnEvent());
            }
            p.updateObservers();
        }
    
        if( phase != null ) {
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
                    game.getCombat().reset(playerTurn);
                    this.getPlayerTurn().resetAttackedThisCombat();
                    this.bCombat.set(false);
        
                    break;
        
                case CLEANUP:
                    this.bPreventCombatDamageThisTurn = false;
                    if (!this.bRepeatCleanup) {
                        this.setPlayerTurn(this.handleNextTurn());
                    }
                    this.planarDiceRolledthisTurn = 0;
                    // Play the End Turn sound
                    game.getEvents().post(new EndOfTurnEvent());
                    break;
                default: // no action
            }
        }
    
        String phaseType = "";
        if (this.bRepeatCleanup) { // for when Cleanup needs to repeat itself
            this.bRepeatCleanup = false;
            phaseType = "Repeat ";
        } else {
            // If the phase that's ending has a stack of additional phases
            // Take the LIFO one and move to that instead of the normal one
            if (this.extraPhases.containsKey(phase)) {
                PhaseType nextPhase = this.extraPhases.get(phase).pop();
                // If no more additional phases are available, remove it from the map
                // and let the next add, reput the key
                if (this.extraPhases.get(phase).isEmpty()) {
                    this.extraPhases.remove(phase);
                }
                this.phase = nextPhase;
                phaseType = "Additional ";
            } else {
                this.phase = PhaseType.getNext(phase);
            }
        }
    
        // **** Anything BELOW Here is actually in the next phase. Maybe move
        // this to handleBeginPhase
        if (this.phase == PhaseType.UNTAP) {
            this.turn++;
            game.getGameLog().add(GameEventType.TURN, "Turn " + this.turn + " (" + this.getPlayerTurn() + ")");
        }
        
        game.getGameLog().add(GameEventType.PHASE, phaseType + Lang.getPossesive(this.getPlayerTurn().getName()) + " " + this.getPhase().Name);
        PhaseUtil.visuallyActivatePhase(this.getPlayerTurn(), this.getPhase());
    }

    private final void handleBeginPhase() {
        if (null == playerTurn) {
            return;
        }
        
        // Handle effects that happen at the beginning of phases
        game.getAction().checkStateEffects();


        switch(this.getPhase()) {
            case UNTAP:
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                game.getPhaseHandler().setPlayersPriorityPermission(false);

                game.getCombat().reset(playerTurn);

                // Tokens starting game in play should suffer from Sum. Sickness
                final List<Card> list = playerTurn.getCardsIncludePhasingIn(ZoneType.Battlefield);
                for (final Card c : list) {
                    if (playerTurn.getTurn() > 0 || !c.isStartsGameInPlay()) {
                        c.setSickness(false);
                    }
                }
                playerTurn.incrementTurn();

                game.getAction().resetActivationsPerTurn();

                final List<Card> lands = CardLists.filter(playerTurn.getLandsInPlay(), Presets.UNTAPPED);
                playerTurn.setNumPowerSurgeLands(lands.size());

                // anything before this point happens regardless of whether the Untap
                // phase is skipped

                if (!PhaseUtil.isSkipUntap(playerTurn)) {
                    game.getUntap().executeUntil(playerTurn);
                    game.getUntap().executeAt();
                }

                break;

            case UPKEEP:
                if (this.getPlayerTurn().hasKeyword("Skip your upkeep step.")) {
                    // Slowtrips all say "on the next turn's upkeep" if there is no
                    // upkeep next turn, the trigger will never occur.
                    for (Player p : game.getPlayers()) {
                        p.clearSlowtripList();
                    }
                    this.setPlayersPriorityPermission(false);
                } else {
                    this.nUpkeepsThisTurn++;
                    game.getUpkeep().executeUntil(this.getPlayerTurn());
                    game.getUpkeep().executeAt();
                }
                break;

            case DRAW:
                if (getTurn() == 1 || this.getPlayerTurn().isSkippingDraw()) {
                    this.setPlayersPriorityPermission(false);
                } else {
                    this.getPlayerTurn().drawCard();
                }
                break;

            case MAIN1:
                if (this.getPlayerTurn().isArchenemy()) {
                    this.getPlayerTurn().setSchemeInMotion();
                }
                break;

            case COMBAT_BEGIN:
                //PhaseUtil.verifyCombat();
                if (playerTurn.isSkippingCombat()) {
                    this.setPlayersPriorityPermission(false);
                }
                break;

            case COMBAT_DECLARE_ATTACKERS:
                if (playerTurn.isSkippingCombat()) {
                    this.setPlayersPriorityPermission(false);
                    playerTurn.removeKeyword("Skip your next combat phase.");
                } /* else game.getInputQueue().setInput(playerTurn.getController().getAttackInput());*/
                break;

            case COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY:
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareAttackers(game);
                    CombatUtil.showCombat(game);
                } else {
                    this.setPlayersPriorityPermission(false);
                }
                break;
            // we can skip AfterBlockers and AfterAttackers if necessary
            case COMBAT_DECLARE_BLOCKERS:
                if (this.inCombat()) {
                    game.getCombat().verifyCreaturesInPlay();
                    CombatUtil.showCombat(game);
                } else {
                    this.setPlayersPriorityPermission(false);
                }
                break;

            case COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY:
                // After declare blockers are finished being declared mark them
                // blocked and trigger blocking things
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareBlockers(game);
                    CombatUtil.showCombat(game);
                } else {
                    this.setPlayersPriorityPermission(false);
                }
                break;

            case COMBAT_FIRST_STRIKE_DAMAGE:
                if (!this.inCombat()) {
                    this.setPlayersPriorityPermission(false);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    // no first strikers, skip this step
                    if (!game.getCombat().assignCombatDamage(true)) {
                        this.setPlayersPriorityPermission(false);
                    } else {
                        game.getCombat().dealAssignedDamage();
                        game.getAction().checkStateEffects();
                        CombatUtil.showCombat(game);
                    }
                }
                break;

            case COMBAT_DAMAGE:
                if (!this.inCombat()) {
                    this.setPlayersPriorityPermission(false);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    if (!game.getCombat().assignCombatDamage(false)) {
                        this.setPlayersPriorityPermission(false);
                    } else {
                        game.getCombat().dealAssignedDamage();
                        game.getAction().checkStateEffects();
                        CombatUtil.showCombat(game);
                    }
                }
                break;

            case COMBAT_END:
                // End Combat always happens
                game.getEndOfCombat().executeUntil();
                game.getEndOfCombat().executeAt();
                CombatUtil.showCombat(game);
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                break;

            case MAIN2:
                CombatUtil.showCombat(game);
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

                game.getEndOfCombat().executeUntil(); //Repeat here in case Time Stop et. al. ends combat early
                game.getEndOfTurn().executeUntil();

                for (Player player : game.getPlayers()) {
                    player.onCleanupPhase();
                    player.getController().autoPassCancel(); // autopass won't wrap to next turn
                }
                this.getPlayerTurn().removeKeyword("Skip all combat phases of this turn.");
                game.getCleanup().executeUntil(this.getNextTurn());
                this.nUpkeepsThisTurn = 0;
                break;

            default:
                break;
          }

        if (this.isPlayerPriorityAllowed()) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", this.getPhase().Name);
            runParams.put("Player", this.getPlayerTurn());
            game.getTriggerHandler().runTrigger(TriggerType.Phase, runParams, false);
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
            p.setLifeGainedThisTurn(0);

            p.removeKeyword("At the beginning of this turn's end step, you lose the game.");
            p.removeKeyword("Skip the untap step of this turn.");
            p.removeKeyword("Schemes can't be set in motion this turn.");
        }

        Player next = getNextActivePlayer();
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(next);
        SDisplayUtil.showTab(nextField);

        if (game.getType() == GameType.Planechase) {
            for(Card p :game.getActivePlanes())
            {
                if (p != null) {
                    p.setController(next, 0);
                    game.getAction().controllerChangeZoneCorrection(p);
                }
            }            
        }

        return next;
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
            if (extraTurn.isCantSetSchemesInMotion()) {
                nextTurn.addKeyword("Schemes can't be set in motion this turn.");
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
     * addExtraPhase.
     * </p>
     * 
     */
    public final void addExtraPhase(final PhaseType afterPhase, final PhaseType extraPhase) {
        // 300.7. Some effects can add phases to a turn. They do this by adding the phases directly after the specified phase. 
        // If multiple extra phases are created after the same phase, the most recently created phase will occur first.
        if (!this.extraPhases.containsKey(afterPhase)) {
            this.extraPhases.put(afterPhase, new Stack<PhaseType>());
        }
        
        this.extraPhases.get(afterPhase).push(extraPhase);
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
     * isFirstUpkeep.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFirstUpkeep() {
        return (this.nUpkeepsThisTurn == 1);
    }

    /**
     * <p>
     * passPriority.
     * </p>
     */
    public final void passPriority() {
        FThreads.assertExecutedByEdt(false);
        StopWatch sw = new StopWatch();
        
        // This is main game loop. It will hang waiting for player's input.  
        while (!game.isGameOver()) { // stop game if it's outcome is clear.
            final Player actingPlayer = this.getPriorityPlayer();
            final Player firstAction = this.getFirstPriority();
    
            // actingPlayer is the player who may act
            // the firstAction is the player who gained Priority First in this segment
            // of Priority
    
            Player nextPlayer = game.getNextPlayerAfter(actingPlayer);
    
            // System.out.println(String.format("%s %s: %s passes priority to %s", playerTurn, phase, actingPlayer, nextPlayer));
            if (firstAction.equals(nextPlayer)) {
                if (game.getStack().isEmpty()) {
                    advancePhase();
                } else if (!game.getStack().hasSimultaneousStackEntries()) {
                    game.getStack().resolveStack();
                    game.getStack().chooseOrderOfSimultaneousStackEntryAll();
                    updateObservers();
                }
            } else {
                // pass the priority to other player
                this.pPlayerPriority = nextPlayer;
                updateObservers();
            }
            
            
            // Time to handle priority to next player.
            if ( phase == PhaseType.COMBAT_DECLARE_ATTACKERS || phase == PhaseType.COMBAT_DECLARE_BLOCKERS)
                game.getStack().freezeStack();
            
            boolean givePriority = isPlayerPriorityAllowed;
            
            if ( phase == PhaseType.COMBAT_DECLARE_BLOCKERS) {
                givePriority = game.getCombat().isPlayerAttacked(pPlayerPriority);
            }
            if ( phase == PhaseType.COMBAT_DECLARE_ATTACKERS && playerTurn != pPlayerPriority )
                givePriority = false;
            
            System.out.print(FThreads.prependThreadId(debugPrintState(givePriority)));

            
            if( givePriority ) {
                sw.start();
                
                pPlayerPriority.getController().takePriority();
                
                sw.stop();
                System.out.print("... passed in " + sw.getTime()/1000f + " ms\n");
                sw.reset();
            } else {
                System.out.print(" >>\n");
            }
        }
    }
    
    public void startFirstTurn(Player goesFirst) {
        if(phase != null)
            throw new IllegalStateException("Turns already started, call this only once per game"); 
        setPlayerTurn(goesFirst);
        advancePhase();
        pPlayerPriority.getController().takePriority();
        passPriority();
    }

    private void advancePhase() { // may be called externally only from gameAction after mulligans 

        this.setPriority(this.getPlayerTurn()); // this needs to be set early as we exit the phase
        // end phase
        setPlayersPriorityPermission(true);
        nextPhase();
        // When consecutively skipping phases (like in combat) this section
        // pushes through that block
        handleBeginPhase();
        // it no longer does.
        updateObservers();
    }

    /**
     * <p>
     * Setter for the field <code>needToNextPhase</code>.
     * </p>
     * 
     * @param needToNextPhase
     *            a boolean.
     */
    public final void setPlayersPriorityPermission(final boolean mayHavePriority) {
        this.isPlayerPriorityAllowed = mayHavePriority;
    }

    /**
     * <p>
     * isNeedToNextPhase.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isPlayerPriorityAllowed() {
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

    /**
     * @return the planarDiceRolledthisTurn
     */
    public int getPlanarDiceRolledthisTurn() {
        return planarDiceRolledthisTurn;
    }


    public void incPlanarDiceRolledthisTurn() {
        this.planarDiceRolledthisTurn++;
    }
    
    public String debugPrintState(boolean hasPriority) {
        return String.format("%s's %s [%sP] %s", getPlayerTurn(), getPhase().Name, hasPriority ? "+" : "-", getPriorityPlayer());
    }


}
