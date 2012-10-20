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
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.player.PlayerType;
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

    private int phaseIndex = 0;
    private int turn = 1;

    private final Stack<ExtraTurn> extraTurns = new Stack<ExtraTurn>();

    private int extraCombats = 0;

    private int nCombatsThisTurn = 0;
    private boolean bPreventCombatDamageThisTurn  = false;

    private Player playerTurn = null;

    private Player skipToTurn = null;
    private PhaseType skipToPhase = PhaseType.CLEANUP;
    private boolean autoPass = false;

    // priority player
    
    private Player pPlayerPriority = null;
    private Player pFirstPriority = null;
    private boolean bPhaseEffects = true;
    private boolean bSkipPhase = true;
    private boolean bCombat = false;
    private boolean bRepeat = false;

    /** The need to next phase. */
    private boolean needToNextPhase = false;

    // This should only be true four times! that is for the initial nextPhases
    // in MyObservable
    /** The need to next phase init. */
    private int needToNextPhaseInit = 0;

    private final GameState game;
    
    public PhaseHandler(final GameState game0)
    {
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
     * setPriorityPlayer.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     */
    public final void setPriorityPlayer(final Player p) {
        this.pPlayerPriority = p;
    }

    /**
     * <p>
     * getFirstPriority.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getFirstPriority() {
        return this.pFirstPriority;
    }

    /**
     * <p>
     * setFirstPriority.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     */
    public final void setFirstPriority(final Player p) {
        this.pFirstPriority = p;
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
    public final boolean doPhaseEffects() {
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
    public final void setPhaseEffects(final boolean b) {
        this.bPhaseEffects = b;
    }

    /**
     * <p>
     * doSkipPhase.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean doSkipPhase() {
        return this.bSkipPhase;
    }

    /**
     * <p>
     * setSkipPhase.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setSkipPhase(final boolean b) {
        this.bSkipPhase = b;
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
        this.setPhaseEffects(false);
        // Handle effects that happen at the beginning of phases
        final PhaseType phase = this.getPhase();
        final Player turn = this.getPlayerTurn();
        this.setSkipPhase(true);
        game.getAction().checkStateEffects();

        if (this.isAutoPassedPhase(turn, phase)) {
            this.setAutoPass(false);
        }

        switch(phase) {
            case UNTAP:
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                PhaseUtil.handleUntap();
                break;

            case UPKEEP:
                PhaseUtil.handleUpkeep();
                break;

            case DRAW:
                PhaseUtil.handleDraw();
                break;

            case COMBAT_BEGIN:
                //PhaseUtil.verifyCombat();
                PhaseUtil.handleCombatBegin();
                break;

            case COMBAT_DECLARE_ATTACKERS:
                PhaseUtil.handleCombatDeclareAttackers();
                break;

            case COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY:
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareAttackers();
                    CombatUtil.showCombat();
                } else {
                    this.setNeedToNextPhase(true);
                }
                break;
            // we can skip AfterBlockers and AfterAttackers if necessary
            case COMBAT_DECLARE_BLOCKERS:
                if (this.inCombat()) {
                    PhaseUtil.verifyCombat();
                    CombatUtil.showCombat();
                } else {
                    this.setNeedToNextPhase(true);
                }
                break;

            case COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY:
                // After declare blockers are finished being declared mark them
                // blocked and trigger blocking things
                if (this.inCombat()) {
                    PhaseUtil.handleDeclareBlockers();
                    CombatUtil.showCombat();
                } else {
                    this.setNeedToNextPhase(true);
                }
                break;

            case COMBAT_FIRST_STRIKE_DAMAGE:
                if (!this.inCombat()) {
                    this.setNeedToNextPhase(true);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    // no first strikers, skip this step
                    if (!game.getCombat().assignCombatDamage(true)) {
                        this.setNeedToNextPhase(true);
                    } else {
                        Combat.dealAssignedDamage();
                        game.getAction().checkStateEffects();
                        CombatUtil.showCombat();
                    }
                }
                break;

            case COMBAT_DAMAGE:
                if (!this.inCombat()) {
                    this.setNeedToNextPhase(true);
                } else {
                    game.getCombat().verifyCreaturesInPlay();

                    if (!game.getCombat().assignCombatDamage(false)) {
                        this.setNeedToNextPhase(true);
                    } else {
                        Combat.dealAssignedDamage();
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
                final List<Card> list = game.getCardsIn(ZoneType.Battlefield);
                for (final Card c : list) {
                    c.resetPreventNextDamage();
                    c.resetReceivedDamageFromThisTurn();
                    c.resetDealtDamageToThisTurn();
                    c.resetDealtDamageToPlayerThisTurn();
                    c.getDamageHistory().newTurn();
                    c.setRegeneratedThisTurn(0);
                    c.clearMustBlockCards();
                    c.getDamageHistory().setCreatureAttackedLastTurnOf(playerTurn, c.getDamageHistory().getCreatureAttackedThisTurn());
                    c.getDamageHistory().setCreatureAttackedThisTurn(false);
                    c.getDamageHistory().setCreatureBlockedThisTurn(false);
                    c.getDamageHistory().setCreatureGotBlockedThisTurn(false);
                    c.clearBlockedByThisTurn();
                    c.clearBlockedThisTurn();
                }

                game.getEndOfTurn().executeUntil();

                for (Player player : game.getPlayers()) {
                    for (Card c : player.getCardsIn(ZoneType.Hand))
                        c.setDrawnThisTurn(false);

                    player.resetPreventNextDamage();
                    player.resetNumDrawnThisTurn();
                    player.setAttackedWithCreatureThisTurn(false);
                    player.setNumLandsPlayed(0);
                    player.clearAssignedDamage();
                    player.resetAttackersDeclaredThisTurn();
                }
                this.getPlayerTurn().removeKeyword("Skip all combat phases of this turn.");
                game.getCleanup().executeUntilTurn(this.getNextTurn());
                break;

            default:
                break;
          }

        if (!this.isNeedToNextPhase()) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", phase.Name);
            runParams.put("Player", turn);
            game.getTriggerHandler().runTrigger(TriggerType.Phase, runParams);

        }

        // This line fixes Combat Damage triggers not going off when they should
        game.getStack().unfreezeStack();

        // UNTAP
        if (phase != PhaseType.UNTAP) {
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
        this.needToNextPhase = false;

        // If the Stack isn't empty why is nextPhase being called?
        if (game.getStack().size() != 0) {
            Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
        this.bPhaseEffects = true;
        if (!game.isCardInPlay("Upwelling")) {
            for (Player p : game.getPlayers()) {
                int burn = p.getManaPool().clearPool();
                if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN)) {
                    p.loseLife(burn, null);
                }
            }
        }

        if (this.getPhase() == PhaseType.COMBAT_DECLARE_ATTACKERS) {
            game.getStack().unfreezeStack();
            this.nCombatsThisTurn++;
        } else if (this.getPhase() == PhaseType.UNTAP) {
            this.nCombatsThisTurn = 0;
        }

        if (this.getPhase() == PhaseType.COMBAT_END) {
            //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
            game.getCombat().reset();
            this.resetAttackedThisCombat(this.getPlayerTurn());
            this.bCombat = false;
        }

        if (this.phaseIndex == PhaseType.CLEANUP.Index) {
            this.bPreventCombatDamageThisTurn = false;
            if (!this.bRepeat) {
                this.setPlayerTurn(this.handleNextTurn());
            }
        }

        if (this.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            game.getStack().unfreezeStack();
        }

        if (this.is(PhaseType.COMBAT_END) && (this.extraCombats > 0)) {
            // TODO: ExtraCombat needs to be changed for other spell/abilities
            // that give extra combat
            // can do it like ExtraTurn stack ExtraPhases

            final Player player = this.getPlayerTurn();
            final Player opp = player.getOpponent();

            this.bCombat = true;
            this.extraCombats--;
            game.getCombat().reset();
            game.getCombat().setAttackingPlayer(player);
            game.getCombat().setDefendingPlayer(opp);
            this.phaseIndex = PhaseType.COMBAT_DECLARE_ATTACKERS.Index;
        } else {
            if (!this.bRepeat) { // for when Cleanup needs to repeat itself
                this.phaseIndex++;
                this.phaseIndex %= PhaseType.values().length;
            } else {
                this.bRepeat = false;
            }
        }

        game.getGameLog().add("Phase", this.getPlayerTurn() + " " + this.getPhase().Name, 6);

        // **** Anything BELOW Here is actually in the next phase. Maybe move
        // this to handleBeginPhase
        if (this.getPhase() == PhaseType.UNTAP) {
            this.turn++;
            game.getGameLog().add("Turn", "Turn " + this.turn + " (" + this.getPlayerTurn() + ")", 0);
        }

        PhaseUtil.visuallyActivatePhase(this.getPhase());

        // When consecutively skipping phases (like in combat) this section
        // pushes through that block
        this.updateObservers();

        if (this.isNeedToNextPhase()) {
            this.setNeedToNextPhase(false);
            this.nextPhase();
        }
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
        for( Player p : game.getPlayers() )
        {
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
        Player nextTurn = this.getPlayerTurn().getOpponent();
        if (!this.extraTurns.isEmpty()) {
            ExtraTurn extraTurn = this.extraTurns.pop();
            nextTurn = extraTurn.getPlayer();
            if (skipTurnTimeVault(nextTurn)) {
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
        if (skipTurnTimeVault(nextTurn)) {
            this.setPlayerTurn(nextTurn);
            return getNextActivePlayer();
        }
        return nextTurn;
    }

    /**
     * <p>
     * skipTurnTimeVault.
     * </p>
     * 
     * @param turn
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.game.player.Player} object.
     */
    private boolean skipTurnTimeVault(Player turn) {
        // time vault:
        List<Card> vaults = turn.getCardsIn(ZoneType.Battlefield, "Time Vault");
        vaults = CardLists.filter(vaults, CardPredicates.Presets.TAPPED);

        if (vaults.size() > 0) {
            final Card crd = vaults.get(0);

            if (turn.isHuman()) {
                if (GameActionUtil.showYesNoDialog(crd, "Untap " + crd + "?")) {
                    crd.untap();
                    return true;
                }
            } else {
                // TODO Should AI skip his turn for time vault?
            }
        }
        return false;
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
    public final synchronized boolean is(final PhaseType phase) {
        return this.getPhase() == phase;
    }

    /**
     * <p>
     * getPhase.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final PhaseType getPhase() {
        return PhaseType.getByIndex(this.phaseIndex);
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
            return this.getPlayerTurn().getOpponent();
        }

        return this.extraTurns.peek().getPlayer();
    }

    /**
     * <p>
     * isNextTurn.
     * </p>
     * 
     * @param pl
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public final boolean isNextTurn(final PlayerType pt) {
        final Player next = this.getNextTurn();
        return next.getType() == pt;
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
            this.extraTurns.push(new ExtraTurn(this.getPlayerTurn().getOpponent()));
        }

        return this.extraTurns.push(new ExtraTurn(player));
    }

    /**
     * <p>
     * skipTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void skipTurn(final Player player) {
        // skipping turn without having extras is equivalent to giving your
        // opponent an extra turn
        boolean skipped = false;
        for (ExtraTurn turn : this.extraTurns) {
            if (turn.getPlayer().equals(player)) {
                this.extraTurns.remove(turn);
                skipped = true;
                break;
            }
        }
        if (!skipped) {
            this.addExtraTurn(player.getOpponent());
        }
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
        List<Card> list = player.getCardsIn(ZoneType.Battlefield);

        list = CardLists.filter(list, CardPredicates.Presets.CREATURES);

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
        final Player actingPlayer = this.getPriorityPlayer();
        final Player firstAction = this.getFirstPriority();

        // actingPlayer is the player who may act
        // the firstAction is the player who gained Priority First in this segment
        // of Priority

        if (firstAction.equals(actingPlayer)) {
            // pass the priority to other player
            this.setPriorityPlayer(actingPlayer.getOpponent());
            Singletons.getModel().getMatch().getInput().resetInput();
            game.getStack().chooseOrderOfSimultaneousStackEntryAll();
        } else {
            if (game.getStack().size() == 0) {
                // end phase
                this.needToNextPhase = true;
                this.pPlayerPriority = this.getPlayerTurn(); // this needs to be
                                                             // set early
                // as we exit the phase
            } else {
                if (!game.getStack().hasSimultaneousStackEntries()) {
                    game.getStack().resolveStack();
                }
            }
            game.getStack().chooseOrderOfSimultaneousStackEntryAll();
        }
    }

    public void setAutoPass(boolean bAutoPass) {
        this.autoPass = bAutoPass;
    }

    public boolean getAutoPass() {
        return this.autoPass;
    }

    public boolean isAutoPassedPhase(Player skipToTurn, PhaseType skipToPhase) {
        return this.skipToTurn == skipToTurn && this.skipToPhase == skipToPhase;
    }

    public void autoPassToCleanup() {
        this.autoPassTo(playerTurn, PhaseType.CLEANUP);
    }

    public void autoPassTo(Player skipToTurn, PhaseType skipToPhase) {
        this.skipToTurn = skipToTurn;
        this.skipToPhase = skipToPhase;
        this.autoPass = true;
        this.bSkipPhase = true;

        if (this.getPriorityPlayer().isHuman()) {
            // TODO This doesn't work quite 100% but pretty close
            this.passPriority();
            Singletons.getModel().getMatch().getInput().resetInput();
        }
    }

    /**
     * <p>
     * Setter for the field <code>needToNextPhase</code>.
     * </p>
     * 
     * @param needToNextPhase
     *            a boolean.
     */
    public final void setNeedToNextPhase(final boolean needToNextPhase) {
        this.needToNextPhase = needToNextPhase;
    }

    /**
     * <p>
     * isNeedToNextPhase.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isNeedToNextPhase() {
        return this.needToNextPhase;
    }

    /**
     * <p>
     * isNeedToNextPhaseInit.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isNeedToNextPhaseInit() {
        this.needToNextPhaseInit++;
        if (this.needToNextPhaseInit <= 4) {
            return true;
        }
        return false;
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
    public final void setDevPhaseState(final PhaseType phase) {
        this.phaseIndex = phase.Index;
    }

    /**
     * Sets the phase state.
     *
     * @param phaseID the new phase state
     */
    public final void setPhaseState(final PhaseType phaseID) {
        this.phaseIndex = phaseID.Index;
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
