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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.lang.time.StopWatch;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.FThreads;
import forge.GameEntity;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameAge;
import forge.game.Game;
import forge.game.GameType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnEnded;
import forge.game.event.GameEventGameRestarted;
import forge.game.event.GameEventManaBurn;
import forge.game.event.GameEventTurnPhase;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.properties.ForgePreferences.FPref;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;


/**
 * <p>
 * Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PhaseHandler.java 13001 2012-01-08 12:25:25Z Sloth $
 */
public class PhaseHandler implements java.io.Serializable {

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
    private Combat combat = null;
    private boolean bRepeatCleanup = false;
    
    private Player playerDeclaresBlockers = null;
    private Player playerDeclaresAttackers = null;

    /** The need to next phase. */
    private boolean givePriorityToPlayer = false;

    private final transient Game game;

    public PhaseHandler(final Game game0) {

        game = game0;
    }


    public final boolean isPlayerTurn(final Player player) {
        return player.equals(this.playerTurn);
    }

    private final void setPlayerTurn(final Player s) {
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
        game.getStack().chooseOrderOfSimultaneousStackEntryAll();

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
    public final boolean inCombat() { return combat != null; }
    public final Combat getCombat() { return this.combat; }

    private void advanceToNextPhase() {
        PhaseType oldPhase = phase;
        
        if (this.bRepeatCleanup) { // for when Cleanup needs to repeat itself
            this.bRepeatCleanup = false;
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
            } else {
                this.phase = PhaseType.getNext(phase);
            }
        }
        
        String phaseType = oldPhase == phase ? "Repeat" : phase == PhaseType.getNext(oldPhase) ? "" : "Additional";

        if (this.phase == PhaseType.UNTAP) {
            this.turn++;
            game.fireEvent(new GameEventTurnBegan(playerTurn, turn));

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
        }

        game.fireEvent(new GameEventTurnPhase(this.getPlayerTurn(), this.getPhase(), phaseType));
    }

    private boolean isSkippingPhase(PhaseType phase) {
        switch(phase) {
            case UNTAP:
                if (playerTurn.hasKeyword("Skip your next untap step.")) {
                    playerTurn.removeKeyword("Skip your next untap step.");
                    return true;
                }
                return playerTurn.hasKeyword("Skip the untap step of this turn.") || playerTurn.hasKeyword("Skip your untap step.");
        
            case UPKEEP: 
                return getPlayerTurn().hasKeyword("Skip your upkeep step.");
    
            case DRAW: 
                return getPlayerTurn().isSkippingDraw() || getTurn() == 1 && game.getPlayers().size() == 2;
    
            case COMBAT_BEGIN:
            case COMBAT_DECLARE_ATTACKERS:
                return playerTurn.isSkippingCombat();
    
            case COMBAT_DECLARE_BLOCKERS:
            case COMBAT_FIRST_STRIKE_DAMAGE:
            case COMBAT_DAMAGE:
                return !this.inCombat();
            
            default: 
                return false;
        }
        
    }


    private final void onPhaseBegin() {
        boolean skipped = false;
        
        if ( isSkippingPhase(phase) ) {
            skipped = true;
            givePriorityToPlayer = false;
            if( phase == PhaseType.COMBAT_DECLARE_ATTACKERS  )
                playerTurn.removeKeyword("Skip your next combat phase.");
        } else  { 
            // Perform turn-based actions 
            switch(this.getPhase()) {
                case UNTAP:
                    givePriorityToPlayer = false;
                    game.getUntap().executeUntil(playerTurn);
                    game.getUntap().executeAt();
                    break;
    
                case UPKEEP:
                    this.nUpkeepsThisTurn++;
                    game.getUpkeep().executeUntil(this.getPlayerTurn());
                    game.getUpkeep().executeAt();
                    break;
    
                case DRAW:
                    this.getPlayerTurn().drawCard();
                    break;
    
                case MAIN1:
                    if (this.getPlayerTurn().isArchenemy()) {
                        this.getPlayerTurn().setSchemeInMotion();
                    }
                    break;
    
                case COMBAT_BEGIN:
                    //PhaseUtil.verifyCombat();
                    break;
    
                case COMBAT_DECLARE_ATTACKERS:
                    combat = new Combat(playerTurn);
                    game.getStack().freezeStack();
                    declareAttackersTurnBasedAction();
                    game.getStack().unfreezeStack();
                    
                    if (combat != null && combat.getAttackers().isEmpty() )
                        combat = null;

                    givePriorityToPlayer = inCombat();
                    this.nCombatsThisTurn++;
                    
                    break;
                    
                case COMBAT_DECLARE_BLOCKERS:
                    combat.removeAbsentCombatants();
                    game.getStack().freezeStack();
                    declareBlockersTurnBasedAction();
                    game.getStack().unfreezeStack();
                    break;
    
                case COMBAT_FIRST_STRIKE_DAMAGE:
                    combat.removeAbsentCombatants();
    
                    // no first strikers, skip this step
                    if (!combat.assignCombatDamage(true)) {
                        this.givePriorityToPlayer = false;
                    } else {
                        combat.dealAssignedDamage();
                    }
                    break;
    
                case COMBAT_DAMAGE:
                    combat.removeAbsentCombatants();
    
                    if (!combat.assignCombatDamage(false)) {
                        this.givePriorityToPlayer = false;
                    } else {
                        combat.dealAssignedDamage();
                    }
                    break;
    
                case COMBAT_END:
                    // End Combat always happens
                    game.getEndOfCombat().executeUntil();
                    game.getEndOfCombat().executeAt();
    
                    //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                    break;
    
                case MAIN2:
                    //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                    break;
    
                case END_OF_TURN:
                    game.getEndOfTurn().executeAt();
                    break;
    
                case CLEANUP:
                    // Rule 514.1
                    final int handSize = playerTurn.getZone(ZoneType.Hand).size();
                    final int max = playerTurn.getMaxHandSize();
                    int numDiscard = playerTurn.isUnlimitedHandSize() || handSize <= max || handSize == 0 ? 0 : handSize - max; 

                    if ( numDiscard > 0 ) {
                        for(Card c : playerTurn.getController().chooseCardsToDiscardToMaximumHandSize(numDiscard)){ 
                            playerTurn.discard(c, null);
                        }
                    }

                    // Rule 514.2
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
                    
                    // Rule 514.3
                    givePriorityToPlayer = false;
                    
                    // Rule 514.3a - state-based actions
                    game.getAction().checkStateEffects();
                    break;
                    
                default:
                    break;
            }
        }
    
    
        if (!skipped) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", this.getPhase().nameForScripts);
            runParams.put("Player", this.getPlayerTurn());
            game.getTriggerHandler().runTrigger(TriggerType.Phase, runParams, false);
        }
    
        // This line fixes Combat Damage triggers not going off when they should
        game.getStack().unfreezeStack();
        
        // Rule 514.3a
        if( phase == PhaseType.CLEANUP && !game.getStack().isEmpty() ) {
            bRepeatCleanup = true;
            givePriorityToPlayer = true;
        }
    }


    private void onPhaseEnd() {
        // If the Stack isn't empty why is nextPhase being called?
        if (!game.getStack().isEmpty()) {
            throw new IllegalStateException("Phase.nextPhase() is called, but Stack isn't empty.");
        }
    
        for (Player p : game.getPlayers()) {
            int burn = p.getManaPool().clearPool(true);
            boolean dealDamage = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN);
            
            if (dealDamage) {
                p.loseLife(burn);
            }
            // Play the Mana Burn sound
            if ( burn > 0 )
                game.fireEvent(new GameEventManaBurn(burn, dealDamage));
        }

        switch (this.phase) {
            case UNTAP:
                this.nCombatsThisTurn = 0;
                break;

            case COMBAT_END:
                combat = null;
                this.getPlayerTurn().resetAttackedThisCombat();
                break;
    
            case CLEANUP:
                this.bPreventCombatDamageThisTurn = false;
                if (!this.bRepeatCleanup) {
                    this.setPlayerTurn(this.handleNextTurn());
                }
                this.planarDiceRolledthisTurn = 0;
                // Play the End Turn sound
                game.fireEvent(new GameEventTurnEnded());
                break;
            default: // no action
        }
    }

    private Combat declareAttackersTurnBasedAction() {
        Player whoDeclares = playerDeclaresAttackers == null || playerDeclaresAttackers.hasLost() ? playerTurn : playerDeclaresAttackers;
        whoDeclares.getController().declareAttackers(playerTurn, combat);
        
        if ( game.isGameOver() ) // they just like to close window at any moment
            return null;

        combat.removeAbsentCombatants();
        CombatUtil.checkAttackOrBlockAlone(combat);

        // TODO move propaganda to happen as the Attacker is Declared
        for (final Card c2 : combat.getAttackers()) {
            boolean canAttack = CombatUtil.checkPropagandaEffects(game, c2, combat);
            if ( canAttack ) {
                if (!c2.hasKeyword("Vigilance")) 
                    c2.tap();
            } else {
                combat.removeFromCombat(c2);
            }
        }

        // Prepare and fire event 'attackers declared'
        MapOfLists<GameEntity, Card> attackersMap = new HashMapOfLists<GameEntity, Card>(CollectionSuppliers.<Card>arrayLists());
        for(GameEntity ge : combat.getDefenders()) attackersMap.addAll(ge, combat.getAttackersOf(ge));
        game.fireEvent(new GameEventAttackersDeclared(playerTurn, attackersMap));

        // This Exalted handler should be converted to script
        if (combat.getAttackers().size() == 1) {
            final Player attackingPlayer = combat.getAttackingPlayer();
            final Card attacker = combat.getAttackers().get(0);
            for (Card card : attackingPlayer.getCardsIn(ZoneType.Battlefield)) {
                int exaltedMagnitude = card.getKeywordAmount("Exalted");
                if (exaltedMagnitude > 0) {
                    CombatUtil.executeExaltedAbility(game, attacker, exaltedMagnitude, card);
                }
            }
        }

        // fire trigger
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attackers", combat.getAttackers());
        runParams.put("AttackingPlayer", combat.getAttackingPlayer());
        game.getTriggerHandler().runTrigger(TriggerType.AttackersDeclared, runParams, false);

        for (final Card c : combat.getAttackers()) {
            CombatUtil.checkDeclaredAttacker(game, c, combat);
        }
        return combat;
    }


    private void declareBlockersTurnBasedAction() {
        Player p = playerTurn;

        do {
            p = game.getNextPlayerAfter(p);
            // Apply Odric's effect here
            Player whoDeclaresBlockers = playerDeclaresBlockers == null || playerDeclaresBlockers.hasLost() ? p : playerDeclaresBlockers;
            if ( combat.isPlayerAttacked(p) ) {
                whoDeclaresBlockers.getController().declareBlockers(p, combat);
            }
            
            if ( game.isGameOver() ) // they just like to close window at any moment
                return;
        } while(p != playerTurn);
        
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();

        combat.removeAbsentCombatants();
        
        
        // Handles removing cards like Mogg Flunkies from combat if group block
        // didn't occur
        final List<Card> filterList = combat.getAllBlockers();
        for (Card blocker : filterList) {
            final List<Card> attackers = new ArrayList<Card>(combat.getAttackersBlockedBy(blocker));
            for (Card attacker : attackers) {
                boolean hasPaid = payRequiredBlockCosts(game, blocker, attacker);
        
                if ( !hasPaid ) {
                    combat.removeBlockAssignment(attacker, blocker);
                }
            }
        }
        for (Card c : filterList) {
            if (c.hasKeyword("CARDNAME can't attack or block alone.") && combat.isBlocking(c)) {
                if (combat.getAllBlockers().size() < 2) {
                    combat.undoBlockingAssignment(c);
                }
            }
        }
        
        combat.onBlockersDeclared();
        
        List<Card> list = combat.getAllBlockers();
        
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getDamageHistory().getCreatureBlockedThisCombat();
            }
        });
        
        CombatUtil.checkDeclareBlockers(game, list, combat);
        
        for (final Card a : combat.getAttackers()) {
            CombatUtil.checkBlockedAttackers(game, a, combat.getBlockers(a));
        }
        
        // map: defender => (many) attacker => (many) blocker  
        Map<GameEntity, MapOfLists<Card, Card>> blockers = new HashMap<GameEntity, MapOfLists<Card,Card>>();
        for(GameEntity ge : combat.getDefenders()) {
            MapOfLists<Card, Card> protectThisDefender = new HashMapOfLists<Card, Card>(CollectionSuppliers.<Card>arrayLists());
            for(Card att : combat.getAttackersOf(ge)) {
                protectThisDefender.addAll(att, combat.getBlockers(att));
            }
            blockers.put(ge, protectThisDefender);
        }
        game.fireEvent(new GameEventBlockersDeclared(blockers));
    }


    private static boolean payRequiredBlockCosts(Game game, Card blocker, Card attacker) {
        Cost blockCost = new Cost(ManaCost.ZERO, true);
        // Sort abilities to apply them in proper order
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                Cost c1 = stAb.getBlockCost(blocker, attacker);
                if ( c1 != null )
                    blockCost.add(c1);
            }
        }
        
        boolean hasPaid = blockCost.getTotalMana().isZero() && blockCost.isOnlyManaCost(); // true if needless to pay
        if (!hasPaid) { 
            hasPaid = blocker.getController().getController().payManaOptional(blocker, blockCost, null, "Pay cost to declare " + blocker + " a blocker", ManaPaymentPurpose.DeclareBlocker);
        }
        return hasPaid;
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

        game.getStack().onNextTurn();

        for (final Player p1 : game.getPlayers()) {
            for (final ZoneType z : Player.ALL_ZONES) {
                p1.getZone(z).resetCardsAddedThisTurn();
            }
        }
        for (Player p : game.getPlayers()) {

            p.resetProwl();
            p.setLifeLostThisTurn(0);
            p.setLifeGainedThisTurn(0);
            p.setLibrarySearched(0);

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

    private final static boolean DEBUG_PHASES = false;

    public void startFirstTurn(Player goesFirst) {
        FThreads.assertExecutedByEdt(false);
        StopWatch sw = new StopWatch();

        if(phase != null)
            throw new IllegalStateException("Turns already started, call this only once per game"); 

        setPlayerTurn(goesFirst);
        advanceToNextPhase();
        onPhaseBegin();

        // don't even offer priority, because it's untap of 1st turn now
        givePriorityToPlayer = false;
        
        while (!game.isGameOver()) { // loop only while is playing
    
            if( DEBUG_PHASES ) {
                System.out.println("\t\tStack: " + game.getStack());
                System.out.print(FThreads.prependThreadId(debugPrintState(givePriorityToPlayer)));
            }

            
            if( givePriorityToPlayer ) {
                if( DEBUG_PHASES )
                    sw.start();
                
                game.getAction().checkStateEffects();
                game.fireEvent(new GameEventPlayerPriority(getPlayerTurn(), getPhase(), getPriorityPlayer()));
                pPlayerPriority.getController().takePriority();
                
                if( DEBUG_PHASES ) {
                    sw.stop();
                    System.out.print("... passed in " + sw.getTime()/1000f + " s\n");
                    System.out.println("\t\tStack: " + game.getStack());
                    sw.reset(); 
                }
            } else if( DEBUG_PHASES ){
                System.out.print(" >>\n");
            }

            // actingPlayer is the player who may act
            // the firstAction is the player who gained Priority First in this segment
            // of Priority
            Player nextPlayer = game.getNextPlayerAfter(this.getPriorityPlayer());
            
            if ( game.isGameOver() || nextPlayer == null ) return; // conceded?
    
            // System.out.println(String.format("%s %s: %s passes priority to %s", playerTurn, phase, actingPlayer, nextPlayer));
            if (getFirstPriority() == nextPlayer) {
                if (game.getStack().isEmpty()) {
                    this.setPriority(this.getPlayerTurn()); // this needs to be set early as we exit the phase

                    // end phase
                    this.givePriorityToPlayer = true;
                    onPhaseEnd();
                    advanceToNextPhase();
                    onPhaseBegin();
                } else if (!game.getStack().hasSimultaneousStackEntries()) {
                    game.getStack().resolveStack();
                    game.getStack().chooseOrderOfSimultaneousStackEntryAll();
                }
            } else {
                // pass the priority to other player
                this.pPlayerPriority = nextPlayer;
            }

            // If ever the karn's ultimate resolved
            if( game.getAge() == GameAge.RestartedByKarn) {
                phase = null;
                game.fireEvent(new GameEventGameRestarted(playerTurn));
                return;
            }
        }
    }
    

    // this is a hack for the setup game state mode, do not use outside of devSetupGameState code
    // as it avoids calling any of the phase effects that may be necessary in a less enforced context
    public final void devModeSet(final PhaseType phase0, final Player player0) {
        
        if (null != phase0) this.phase = phase0;
        if (null != player0 )
            setPlayerTurn(player0);
        
        game.fireEvent(new GameEventTurnPhase(this.getPlayerTurn(), this.getPhase(), ""));
        combat = null; // not-null can be created only when declare attackers phase begins 
    }

    /**
     * Sets the phase state.
     *
     * @param phaseID the new phase state
     */
    public final void endTurnByEffect() {
        this.combat = null;
        this.phase = PhaseType.CLEANUP;
        this.onPhaseBegin();
    }


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
        return String.format("%s's %s [%sP] %s", getPlayerTurn(), getPhase().nameForUi, hasPriority ? "+" : "-", getPriorityPlayer());
    }


    // just to avoid exposing variable to oute classes
    public void onStackResolved() {
        givePriorityToPlayer = true;
    }


    public final void setPlayerDeclaresBlockers(Player player) {
        this.playerDeclaresBlockers = player;
    }
    


    public final void setPlayerDeclaresAttackers(Player player) {
        this.playerDeclaresAttackers = player;
    }


    public void endCombat() {
        combat = null;
    }
    


}
