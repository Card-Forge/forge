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
import java.util.Observer;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyObservable;
import forge.util.closures.Predicate;

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

    private int phaseIndex;
    private int turn;

    // Please use getX, setX, and incrementX methods instead of directly
    // accessing the following:
    /** Constant <code>GameBegins=0</code>. */
    private static int gameBegins = 0;

    private final Stack<ExtraTurn> extraTurns = new Stack<ExtraTurn>();

    private int extraCombats;

    private int nCombatsThisTurn;
    private boolean bPreventCombatDamageThisTurn;

    private Player playerTurn = AllZone.getHumanPlayer();

    private Player skipToTurn = AllZone.getHumanPlayer();
    private PhaseType skipToPhase = PhaseType.CLEANUP;
    private boolean autoPass = false;

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
        return this.playerTurn.isPlayer(player);
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

    private Player pPlayerPriority = AllZone.getHumanPlayer();

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

    private Player pFirstPriority = AllZone.getHumanPlayer();

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
        if (AllZone.getStack() != null) {
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
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

    private boolean bPhaseEffects = true;

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

    private boolean bSkipPhase = true;

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

    private boolean bCombat = false;

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

    private boolean bRepeat = false;

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
     * Constructor for PhaseHandler.
     * </p>
     */
    public PhaseHandler() {
        this.reset();
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        this.turn = 1;
        this.playerTurn = AllZone.getHumanPlayer();
        this.resetPriority();
        this.bPhaseEffects = true;
        this.needToNextPhase = false;
        PhaseHandler.setGameBegins(0);
        this.phaseIndex = 0;
        this.extraTurns.clear();
        this.nCombatsThisTurn = 0;
        this.extraCombats = 0;
        this.bPreventCombatDamageThisTurn = false;
        this.bCombat = false;
        this.bRepeat = false;
        this.autoPass = false;
        this.updateObservers();
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
        Singletons.getModel().getGameAction().checkStateEffects();

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
                    AllZone.getCombat().verifyCreaturesInPlay();

                    // no first strikers, skip this step
                    if (!AllZone.getCombat().assignCombatDamage(true)) {
                        this.setNeedToNextPhase(true);
                    } else {
                        Combat.dealAssignedDamage();
                        Singletons.getModel().getGameAction().checkStateEffects();
                        CombatUtil.showCombat();
                    }
                }
                break;

            case COMBAT_DAMAGE:
                if (!this.inCombat()) {
                    this.setNeedToNextPhase(true);
                } else {
                    AllZone.getCombat().verifyCreaturesInPlay();

                    if (!AllZone.getCombat().assignCombatDamage(false)) {
                        this.setNeedToNextPhase(true);
                    } else {
                        Combat.dealAssignedDamage();
                        Singletons.getModel().getGameAction().checkStateEffects();
                        CombatUtil.showCombat();
                    }
                }
                break;

            case COMBAT_END:
                // End Combat always happens
                AllZone.getEndOfCombat().executeUntil();
                AllZone.getEndOfCombat().executeAt();
                CombatUtil.showCombat();
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                break;

            case MAIN2:
                CombatUtil.showCombat();
                //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                break;

            case END_OF_TURN:
                AllZone.getEndOfTurn().executeAt();
                break;

            case CLEANUP:
                // Reset Damage received map
                final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
                for (final Card c : list) {
                    c.resetPreventNextDamage();
                    c.resetReceivedDamageFromThisTurn();
                    c.resetDealtDamageToThisTurn();
                    c.resetDealtDamageToPlayerThisTurn();
                    c.getDamageHistory().setDealtDmgToHumanThisTurn(false);
                    c.getDamageHistory().setDealtDmgToComputerThisTurn(false);
                    c.getDamageHistory().setDealtCombatDmgToHumanThisTurn(false);
                    c.getDamageHistory().setDealtCombatDmgToComputerThisTurn(false);
                    c.setRegeneratedThisTurn(0);
                    c.clearMustBlockCards();
                    if (this.isPlayerTurn(AllZone.getComputerPlayer())) {
                        c.getDamageHistory().setCreatureAttackedLastComputerTurn(c.getDamageHistory().getCreatureAttackedThisTurn());
                    }
                    if (this.isPlayerTurn(AllZone.getHumanPlayer())) {
                        c.getDamageHistory().setCreatureAttackedLastHumanTurn(c.getDamageHistory().getCreatureAttackedThisTurn());
                    }
                    c.getDamageHistory().setCreatureAttackedThisTurn(false);
                    c.getDamageHistory().setCreatureBlockedThisTurn(false);
                    c.getDamageHistory().setCreatureGotBlockedThisTurn(false);
                    c.clearBlockedByThisTurn();
                    c.clearBlockedThisTurn();
                }

                AllZone.getEndOfTurn().executeUntil();
                final CardList cHand = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
                final CardList hHand = AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand);
                for (final Card c : cHand) {
                    c.setDrawnThisTurn(false);
                }
                for (final Card c : hHand) {
                    c.setDrawnThisTurn(false);
                }
                for (Player player : AllZone.getPlayersInGame()) {
                    player.resetPreventNextDamage();
                    player.resetNumDrawnThisTurn();
                    player.setAttackedWithCreatureThisTurn(false);
                    player.setNumLandsPlayed(0);
                    player.clearAssignedDamage();
                    player.resetAttackersDeclaredThisTurn();
                }
                this.getPlayerTurn().removeKeyword("Skip all combat phases of this turn.");
                Singletons.getModel().getGameState().getCleanup().executeUntilTurn(this.getNextTurn());
                break;

            default:
                break;
          }

        if (!this.isNeedToNextPhase()) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", phase.Name);
            runParams.put("Player", turn);
            AllZone.getTriggerHandler().runTrigger(TriggerType.Phase, runParams);

        }

        // This line fixes Combat Damage triggers not going off when they should
        AllZone.getStack().unfreezeStack();

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
        if (AllZone.getStack().size() != 0) {
            Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
        this.bPhaseEffects = true;
        if (!AllZoneUtil.isCardInPlay("Upwelling")) {
            for (Player p : AllZone.getPlayersInGame()) {
                int burn = p.getManaPool().clearPool();
                if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN)) {
                    p.loseLife(burn, null);
                }
            }
        }

        if (this.getPhase() == PhaseType.COMBAT_DECLARE_ATTACKERS) {
            AllZone.getStack().unfreezeStack();
            this.nCombatsThisTurn++;
        } else if (this.getPhase() == PhaseType.UNTAP) {
            this.nCombatsThisTurn = 0;
        }

        if (this.getPhase() == PhaseType.COMBAT_END) {
            //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
            AllZone.getCombat().reset();
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
            AllZone.getStack().unfreezeStack();
        }

        if (this.is(PhaseType.COMBAT_END) && (this.extraCombats > 0)) {
            // TODO: ExtraCombat needs to be changed for other spell/abilities
            // that give extra combat
            // can do it like ExtraTurn stack ExtraPhases

            final Player player = this.getPlayerTurn();
            final Player opp = player.getOpponent();

            this.bCombat = true;
            this.extraCombats--;
            AllZone.getCombat().reset();
            AllZone.getCombat().setAttackingPlayer(player);
            AllZone.getCombat().setDefendingPlayer(opp);
            this.phaseIndex = PhaseType.COMBAT_DECLARE_ATTACKERS.Index;
        } else {
            if (!this.bRepeat) { // for when Cleanup needs to repeat itself
                this.phaseIndex++;
                this.phaseIndex %= PhaseType.values().length;
            } else {
                this.bRepeat = false;
            }
        }

        AllZone.getGameLog().add("Phase", this.getPlayerTurn() + " " + this.getPhase().Name, 6);

        // **** Anything BELOW Here is actually in the next phase. Maybe move
        // this to handleBeginPhase
        if (this.getPhase() == PhaseType.UNTAP) {
            this.turn++;
            AllZone.getGameLog().add("Turn", "Turn " + this.turn + " (" + this.getPlayerTurn() + ")", 0);
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

        AllZone.getStack().setCardsCastLastTurn();
        AllZone.getStack().clearCardsCastThisTurn();
        AllZone.resetZoneMoveTracking();
        AllZone.getComputerPlayer().resetProwl();
        AllZone.getHumanPlayer().resetProwl();
        AllZone.getComputerPlayer().setLifeLostThisTurn(0);
        AllZone.getHumanPlayer().setLifeLostThisTurn(0);
        for (Player player : AllZone.getPlayersInGame()) {
            player.removeKeyword("At the beginning of this turn's end step, you lose the game.");
            player.removeKeyword("Skip the untap step of this turn.");
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
        CardList vaults = turn.getCardsIn(ZoneType.Battlefield, "Time Vault");
        vaults = vaults.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return c.isTapped();
            }
        });

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
        return this.getPhase() == phase && this.getPlayerTurn().isPlayer(player);
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
    public final boolean isNextTurn(final Player pl) {
        final Player next = this.getNextTurn();
        return (pl.equals(next));
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
        CardList list = player.getCardsIn(ZoneType.Battlefield);

        list = list.getType("Creature");

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
            AllZone.getInputControl().resetInput();
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
        } else {
            if (AllZone.getStack().size() == 0) {
                // end phase
                this.needToNextPhase = true;
                this.pPlayerPriority = this.getPlayerTurn(); // this needs to be
                                                             // set early
                // as we exit the phase
            } else {
                if (!AllZone.getStack().hasSimultaneousStackEntries()) {
                    AllZone.getStack().resolveStack();
                }
            }
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
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
            AllZone.getInputControl().resetInput();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void addObserver(final Observer o) {
        super.addObserver(o);
    }

    /** The need to next phase. */
    private boolean needToNextPhase = false;

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

    // This should only be true four times! that is for the initial nextPhases
    // in MyObservable
    /** The need to next phase init. */
    private int needToNextPhaseInit = 0;

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

    /**
     * <p>
     * canCastSorcery.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canCastSorcery(final Player player) {
        PhaseHandler now = Singletons.getModel().getGameState().getPhaseHandler();
        return now.isPlayerTurn(player) && now.getPhase().isMain() && AllZone.getStack().size() == 0;
    }

    /**
     * <p>
     * couldCastSorcery.
     * for conditions the stack must only have the sa being checked
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param sa
     *            a {@link forge.game.player.SpellAbility} object.
     * @return a boolean .
     */
    public static boolean couldCastSorcery(final Player player, final SpellAbility sa) {
        PhaseHandler now = Singletons.getModel().getGameState().getPhaseHandler();
        final Card source = sa.getRootSpellAbility().getSourceCard();
        boolean onlyThis = true;
        if (AllZone.getStack().size() != 0) {
            for (final Card card : AllZoneUtil.getCardsIn(ZoneType.Stack)) {
                if (card != source) {
                    onlyThis = false;
                    //System.out.println("StackCard: " + card + " vs SourceCard: " + source);
                }
            }
        }
        //System.out.println("now.isPlayerTurn(player) - " + now.isPlayerTurn(player));
        //System.out.println("now.getPhase().isMain() - " + now.getPhase().isMain());
        //System.out.println("onlyThis - " + onlyThis);
        return now.isPlayerTurn(player) && now.getPhase().isMain() && onlyThis;
    }



    /**
     * <p>
     * setGameBegins.
     * </p>
     * 
     * @param gameBegins
     *            a int.
     */
    public static void setGameBegins(final int gameBegins) {
        PhaseHandler.gameBegins = gameBegins;
    }

    /**
     * <p>
     * getGameBegins.
     * </p>
     * 
     * @return a int.
     */
    public static int getGameBegins() {
        return PhaseHandler.gameBegins;
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
