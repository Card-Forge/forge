package forge;

import java.util.HashMap;
import java.util.Observer;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;

/**
 * <p>
 * Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Phase extends MyObservable implements java.io.Serializable {

    /** Constant <code>serialVersionUID=5207222278370963197L</code>. */
    private static final long serialVersionUID = 5207222278370963197L;

    private int phaseIndex;
    private int turn;

    // Please use getX, setX, and incrementX methods instead of directly
    // accessing the following:
    /** Constant <code>GameBegins=0</code>. */
    private static int GameBegins = 0;

    private Stack<Player> extraTurns = new Stack<Player>();

    private int extraCombats;

    private int nCombatsThisTurn;
    private boolean bPreventCombatDamageThisTurn;

    private Player playerTurn = AllZone.getHumanPlayer();

    /**
     * <p>
     * isPlayerTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public final boolean isPlayerTurn(final Player player) {
        return playerTurn.isPlayer(player);
    }

    /**
     * <p>
     * Setter for the field <code>playerTurn</code>.
     * </p>
     * 
     * @param s
     *            a {@link forge.Player} object.
     */
    public final void setPlayerTurn(final Player s) {
        playerTurn = s;
    }

    /**
     * <p>
     * Getter for the field <code>playerTurn</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getPlayerTurn() {
        return playerTurn;
    }

    // priority player

    private Player pPlayerPriority = AllZone.getHumanPlayer();

    /**
     * <p>
     * getPriorityPlayer.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getPriorityPlayer() {
        return pPlayerPriority;
    }

    /**
     * <p>
     * setPriorityPlayer.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     */
    public final void setPriorityPlayer(final Player p) {
        pPlayerPriority = p;
    }

    private Player pFirstPriority = AllZone.getHumanPlayer();

    /**
     * <p>
     * getFirstPriority.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getFirstPriority() {
        return pFirstPriority;
    }

    /**
     * <p>
     * setFirstPriority.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     */
    public final void setFirstPriority(final Player p) {
        pFirstPriority = p;
    }

    /**
     * <p>
     * setPriority.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     */
    public final void setPriority(final Player p) {
        if (AllZone.getStack() != null) {
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
        }

        pFirstPriority = p;
        pPlayerPriority = p;
    }

    /**
     * <p>
     * resetPriority.
     * </p>
     */
    public final void resetPriority() {
        setPriority(playerTurn);
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
        return bPhaseEffects;
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
        bPhaseEffects = b;
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
        return bSkipPhase;
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
        bSkipPhase = b;
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
        return bCombat;
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
        bCombat = b;
    }

    private boolean bRepeat = false;

    /**
     * <p>
     * repeatPhase.
     * </p>
     */
    public final void repeatPhase() {
        bRepeat = true;
    }

    /** The phase order. */
    String[] phaseOrder = { Constant.Phase.UNTAP, Constant.Phase.UPKEEP, Constant.Phase.DRAW, Constant.Phase.MAIN1,
            Constant.Phase.COMBAT_BEGIN, Constant.Phase.Combat_Declare_Attackers,
            Constant.Phase.Combat_Declare_Attackers_InstantAbility, Constant.Phase.Combat_Declare_Blockers,
            Constant.Phase.Combat_Declare_Blockers_InstantAbility, Constant.Phase.Combat_FirstStrikeDamage,
            Constant.Phase.Combat_Damage, Constant.Phase.Combat_End, Constant.Phase.Main2, Constant.Phase.End_Of_Turn,
            Constant.Phase.Cleanup };

    /**
     * <p>
     * Constructor for Phase.
     * </p>
     */
    public Phase() {
        reset();
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        turn = 1;
        playerTurn = AllZone.getHumanPlayer();
        resetPriority();
        bPhaseEffects = true;
        needToNextPhase = false;
        setGameBegins(0);
        phaseIndex = 0;
        extraTurns.clear();
        nCombatsThisTurn = 0;
        extraCombats = 0;
        bPreventCombatDamageThisTurn = false;
        bCombat = false;
        bRepeat = false;
        this.updateObservers();
    }

    /**
     * <p>
     * turnReset.
     * </p>
     */
    public final void turnReset() {
        playerTurn.setNumLandsPlayed(0);
    }

    /**
     * <p>
     * handleBeginPhase.
     * </p>
     */
    public final void handleBeginPhase() {
        AllZone.getPhase().setPhaseEffects(false);
        // Handle effects that happen at the beginning of phases
        final String phase = AllZone.getPhase().getPhase();
        final Player turn = AllZone.getPhase().getPlayerTurn();
        AllZone.getPhase().setSkipPhase(true);
        AllZone.getGameAction().checkStateEffects();

        if (phase.equals(Constant.Phase.UNTAP)) {
            PhaseUtil.handleUntap();
        } else if (phase.equals(Constant.Phase.UPKEEP)) {
            PhaseUtil.handleUpkeep();
        } else if (phase.equals(Constant.Phase.DRAW)) {
            PhaseUtil.handleDraw();
        } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
            PhaseUtil.verifyCombat();
        } else if (phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
            if (inCombat()) {
                PhaseUtil.handleDeclareAttackers();
            } else {
                AllZone.getPhase().setNeedToNextPhase(true);
            }
        }

        // we can skip AfterBlockers and AfterAttackers if necessary
        else if (phase.equals(Constant.Phase.Combat_Declare_Blockers)) {
            if (inCombat()) {
                PhaseUtil.verifyCombat();
            } else {
                AllZone.getPhase().setNeedToNextPhase(true);
            }
        } else if (phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
            // After declare blockers are finished being declared mark them
            // blocked and trigger blocking things
            if (!inCombat()) {
                AllZone.getPhase().setNeedToNextPhase(true);
            } else {
                PhaseUtil.handleDeclareBlockers();
            }
        } else if (phase.equals(Constant.Phase.Combat_FirstStrikeDamage)) {
            if (!inCombat()) {
                AllZone.getPhase().setNeedToNextPhase(true);
            } else {
                AllZone.getCombat().verifyCreaturesInPlay();

                // no first strikers, skip this step
                if (!AllZone.getCombat().setAssignedFirstStrikeDamage()) {
                    AllZone.getPhase().setNeedToNextPhase(true);
                } else {
                    if (!isPreventCombatDamageThisTurn()) {
                        Combat.dealAssignedDamage();
                    }

                    AllZone.getGameAction().checkStateEffects();
                    CombatUtil.showCombat();
                }
            }
        } else if (phase.equals(Constant.Phase.Combat_Damage)) {
            if (!inCombat()) {
                AllZone.getPhase().setNeedToNextPhase(true);
            } else {
                AllZone.getCombat().verifyCreaturesInPlay();

                AllZone.getCombat().setAssignedDamage();

                if (!isPreventCombatDamageThisTurn()) {
                    Combat.dealAssignedDamage();
                }

                AllZone.getGameAction().checkStateEffects();
                CombatUtil.showCombat();
            }
        } else if (phase.equals(Constant.Phase.Combat_End)) {
            // End Combat always happens
            AllZone.getEndOfCombat().executeUntil();
            AllZone.getEndOfCombat().executeAt();
        } else if (phase.equals(Constant.Phase.End_Of_Turn)) {
            AllZone.getEndOfTurn().executeAt();
        } else if (phase.equals(Constant.Phase.Cleanup)) {
            AllZone.getPhase().getPlayerTurn().setAssignedDamage(0);

            // Reset Damage received map
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            for (Card c : list) {
                c.resetPreventNextDamage();
                c.resetReceivedDamageFromThisTurn();
                c.resetDealtDamageToThisTurn();
                c.setDealtDmgToHumanThisTurn(false);
                c.setDealtDmgToComputerThisTurn(false);
            }
            AllZone.getHumanPlayer().resetPreventNextDamage();
            AllZone.getComputerPlayer().resetPreventNextDamage();

            AllZone.getEndOfTurn().executeUntil();
            CardList cHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
            CardList hHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
            for (Card c : cHand)
                c.setDrawnThisTurn(false);
            for (Card c : hHand)
                c.setDrawnThisTurn(false);
            AllZone.getHumanPlayer().resetNumDrawnThisTurn();
            AllZone.getComputerPlayer().resetNumDrawnThisTurn();
        }

        if (!AllZone.getPhase().isNeedToNextPhase()) {
            // Run triggers if phase isn't being skipped
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", phase);
            runParams.put("Player", turn);
            AllZone.getTriggerHandler().runTrigger("Phase", runParams);

        }

        // This line fixes Combat Damage triggers not going off when they should
        AllZone.getStack().unfreezeStack();

        if (!phase.equals(Constant.Phase.UNTAP)) {
            // during untap
            resetPriority();
        }
    }

    /**
     * Checks if is prevent combat damage this turn.
     * 
     * @return true, if is prevent combat damage this turn
     */
    public final boolean isPreventCombatDamageThisTurn() {
        return bPreventCombatDamageThisTurn;
    }

    /**
     * <p>
     * nextPhase.
     * </p>
     */
    public final void nextPhase() {
        // experimental, add executeCardStateEffects() here:
        for (String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }

        needToNextPhase = false;

        // If the Stack isn't empty why is nextPhase being called?
        if (AllZone.getStack().size() != 0) {
            Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
        this.bPhaseEffects = true;
        if (!AllZoneUtil.isCardInPlay("Upwelling")) {
            AllZone.getHumanPlayer().getManaPool().clearPool();
            AllZone.getComputerPlayer().getManaPool().clearPool();
        }

        if (getPhase().equals(Constant.Phase.Combat_Declare_Attackers)) {
            AllZone.getStack().unfreezeStack();
            nCombatsThisTurn++;
        } else if (getPhase().equals(Constant.Phase.UNTAP)) {
            nCombatsThisTurn = 0;
        }

        if (getPhase().equals(Constant.Phase.Combat_End)) {
            AllZone.getCombat().reset();
            AllZone.getDisplay().showCombat("");
            resetAttackedThisCombat(getPlayerTurn());
            this.bCombat = false;
        }

        if (phaseOrder[phaseIndex].equals(Constant.Phase.Cleanup)) {
            bPreventCombatDamageThisTurn = false;
            if (!bRepeat) {
                AllZone.getPhase().setPlayerTurn(handleNextTurn());
            }
        }

        if (is(Constant.Phase.Combat_Declare_Blockers)) {
            AllZone.getStack().unfreezeStack();
        }

        if (is(Constant.Phase.Combat_End) && extraCombats > 0) {
            // TODO: ExtraCombat needs to be changed for other spell/abilities
            // that give extra combat
            // can do it like ExtraTurn stack ExtraPhases

            Player player = getPlayerTurn();
            Player opp = player.getOpponent();

            bCombat = true;
            extraCombats--;
            AllZone.getCombat().reset();
            AllZone.getCombat().setAttackingPlayer(player);
            AllZone.getCombat().setDefendingPlayer(opp);
            phaseIndex = findIndex(Constant.Phase.Combat_Declare_Attackers);
        } else {
            if (!bRepeat) { // for when Cleanup needs to repeat itself
                phaseIndex++;
                phaseIndex %= phaseOrder.length;
            } else {
                bRepeat = false;
            }
        }

        // **** Anything BELOW Here is actually in the next phase. Maybe move
        // this to handleBeginPhase
        if (getPhase().equals(Constant.Phase.UNTAP)) {
            turn++;
        }

        // When consecutively skipping phases (like in combat) this section
        // pushes through that block
        this.updateObservers();
        if (AllZone.getPhase() != null && AllZone.getPhase().isNeedToNextPhase()) {
            AllZone.getPhase().setNeedToNextPhase(false);
            AllZone.getPhase().nextPhase();
        }
    }

    /**
     * <p>
     * handleNextTurn.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    private Player handleNextTurn() {
        Player nextTurn = extraTurns.isEmpty() ? getPlayerTurn().getOpponent() : extraTurns.pop();

        AllZone.getStack().setCardsCastLastTurn();
        AllZone.getStack().clearCardsCastThisTurn();
        AllZone.resetZoneMoveTracking();
        AllZone.getComputerPlayer().resetProwl();
        AllZone.getHumanPlayer().resetProwl();

        return skipTurnTimeVault(nextTurn);
    }

    /**
     * <p>
     * skipTurnTimeVault.
     * </p>
     * 
     * @param turn
     *            a {@link forge.Player} object.
     * @return a {@link forge.Player} object.
     */
    private Player skipTurnTimeVault(Player turn) {
        // time vault:
        CardList vaults = turn.getCardsIn(Zone.Battlefield, "Time Vault");
        vaults = vaults.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.isTapped();
            }
        });

        if (vaults.size() > 0) {
            final Card crd = vaults.get(0);

            if (turn.isHuman()) {
                if (GameActionUtil.showYesNoDialog(crd, "Untap " + crd + "?")) {
                    crd.untap();
                    turn = extraTurns.isEmpty() ? turn.getOpponent() : extraTurns.pop();
                }
            } else {
                // TODO Should AI skip his turn for time vault?
            }
        }
        return turn;
    }

    /**
     * <p>
     * is.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public final synchronized boolean is(final String phase, final Player player) {
        return getPhase().equals(phase) && getPlayerTurn().isPlayer(player);
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
    public final synchronized boolean is(final String phase) {
        return (getPhase().equals(phase));
    }

    /**
     * <p>
     * isAfter.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isAfter(final String phase) {
        return phaseIndex > findIndex(phase);
    }

    /**
     * <p>
     * isBefore.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isBefore(final String phase) {
        return phaseIndex < findIndex(phase);
    }

    /**
     * <p>
     * findIndex.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    private int findIndex(final String phase) {
        for (int i = 0; i < phaseOrder.length; i++) {
            if (phase.equals(phaseOrder[i])) {
                return i;
            }
        }
        throw new RuntimeException("Phase : findIndex() invalid argument, phase = " + phase);
    }

    /**
     * <p>
     * getPhase.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getPhase() {
        return phaseOrder[phaseIndex];
    }

    /**
     * <p>
     * Getter for the field <code>turn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getTurn() {
        return turn;
    }

    /**
     * <p>
     * getNextTurn.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getNextTurn() {
        if (extraTurns.isEmpty()) {
            return getPlayerTurn().getOpponent();
        }

        return extraTurns.peek();
    }

    /**
     * <p>
     * isNextTurn.
     * </p>
     * 
     * @param pl
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public final boolean isNextTurn(final Player pl) {
        Player next = getNextTurn();
        return (pl.equals(next));
    }

    /**
     * <p>
     * addExtraTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void addExtraTurn(final Player player) {
        // use a stack to handle extra turns, make sure the bottom of the stack
        // restores original turn order
        if (extraTurns.isEmpty()) {
            extraTurns.push(getPlayerTurn().getOpponent());
        }

        extraTurns.push(player);
    }

    /**
     * <p>
     * skipTurn.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void skipTurn(final Player player) {
        // skipping turn without having extras is equivalent to giving your
        // opponent an extra turn
        if (extraTurns.isEmpty()) {
            addExtraTurn(player.getOpponent());
        } else {
            int pos = extraTurns.lastIndexOf(player);
            if (pos == -1) {
                addExtraTurn(player.getOpponent());
            } else {
                extraTurns.remove(pos);
            }
        }
    }

    /**
     * <p>
     * addExtraCombat.
     * </p>
     */
    public final void addExtraCombat() {
        // Extra combats can only happen
        extraCombats++;
    }

    /**
     * <p>
     * isFirstCombat.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFirstCombat() {
        return (nCombatsThisTurn == 1);
    }

    /**
     * <p>
     * resetAttackedThisCombat.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void resetAttackedThisCombat(final Player player) {
        // resets the status of attacked/blocked this phase
        CardList list = player.getCardsIn(Zone.Battlefield);

        list = list.getType("Creature");

        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (c.getCreatureAttackedThisCombat()) {
                c.setCreatureAttackedThisCombat(false);
            }
            if (c.getCreatureBlockedThisCombat()) {
                c.setCreatureBlockedThisCombat(false);
            }

            if (c.getCreatureGotBlockedThisCombat()) {
                c.setCreatureGotBlockedThisCombat(false);
            }
        }
    }

    /**
     * <p>
     * passPriority.
     * </p>
     */
    public final void passPriority() {
        Player actingPlayer = getPriorityPlayer();
        Player lastToAct = getFirstPriority();

        // actingPlayer is the player who may act
        // the lastToAct is the player who gained Priority First in this segment
        // of Priority

        if (lastToAct.equals(actingPlayer)) {
            // pass the priority to other player
            setPriorityPlayer(actingPlayer.getOpponent());
            AllZone.getInputControl().resetInput();
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
        } else {
            if (AllZone.getStack().size() == 0) {
                // end phase
                needToNextPhase = true;
                pPlayerPriority = getPlayerTurn(); // this needs to be set early
                                                   // as we exit the phase
            } else {
                if (!AllZone.getStack().hasSimultaneousStackEntries()) {
                    AllZone.getStack().resolveStack();
                }
            }
            AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void addObserver(final Observer o) {
        super.deleteObservers();
        super.addObserver(o);
    }

    /** The need to next phase. */
    boolean needToNextPhase = false;

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
    int needToNextPhaseInit = 0;

    /**
     * <p>
     * isNeedToNextPhaseInit.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isNeedToNextPhaseInit() {
        needToNextPhaseInit++;
        if (needToNextPhaseInit <= 4) {
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
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean canCastSorcery(final Player player) {
        return AllZone.getPhase().isPlayerTurn(player)
                && (AllZone.getPhase().getPhase().equals(Constant.Phase.Main2) || AllZone.getPhase().getPhase()
                        .equals(Constant.Phase.MAIN1)) && AllZone.getStack().size() == 0;
    }

    /**
     * <p>
     * buildActivateString.
     * </p>
     * 
     * @param startPhase
     *            a {@link java.lang.String} object.
     * @param endPhase
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String buildActivateString(final String startPhase, final String endPhase) {
        StringBuilder sb = new StringBuilder();

        boolean add = false;
        for (int i = 0; i < phaseOrder.length; i++) {
            if (phaseOrder[i].equals(startPhase)) {
                add = true;
            }

            if (add) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(phaseOrder[i]);
            }

            if (phaseOrder[i].equals(endPhase)) {
                add = false;
            }
        }

        return sb.toString();
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
        GameBegins = gameBegins;
    }

    /**
     * <p>
     * getGameBegins.
     * </p>
     * 
     * @return a int.
     */
    public static int getGameBegins() {
        return GameBegins;
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
     * @param phaseID
     *            a {@link java.lang.String} object.
     */
    public final void setDevPhaseState(final String phaseID) {
        this.phaseIndex = findIndex(phaseID);
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param b
     *            a boolean
     */
    public final void setPreventCombatDamageThisTurn(final boolean b) {
        bPreventCombatDamageThisTurn = true;
    }
}
