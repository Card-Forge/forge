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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import forge.card.mana.ManaCost;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.event.*;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.CollectionSuppliers;
import forge.util.collect.FCollectionView;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

import org.apache.commons.lang3.time.StopWatch;

import java.util.*;


/**
 * <p>
 * Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PhaseHandler.java 13001 2012-01-08 12:25:25Z Sloth $
 */
public class PhaseHandler implements java.io.Serializable {
    private static final long serialVersionUID = 5207222278370963197L;

    // Start turn at 0, since we start even before first untap
    private PhaseType phase = null;
    private int turn = 0;

    private final transient Stack<ExtraTurn> extraTurns = new Stack<ExtraTurn>();
    private final transient Map<PhaseType, Stack<PhaseType>> extraPhases = new HashMap<PhaseType, Stack<PhaseType>>();

    private int nUpkeepsThisTurn = 0;
    private int nUpkeepsThisGame = 0;
    private int nCombatsThisTurn = 0;
    private boolean bPreventCombatDamageThisTurn  = false;
    private int planarDiceRolledthisTurn = 0;

    private transient Player playerTurn = null;

    // priority player

    private transient Player pPlayerPriority = null;
    private transient Player pFirstPriority = null;
    private transient Combat combat = null;
    private boolean bRepeatCleanup = false;

    private transient Player playerDeclaresBlockers = null;
    private transient Player playerDeclaresAttackers = null;

    /** The need to next phase. */
    private boolean givePriorityToPlayer = false;

    private final transient Game game;

    public PhaseHandler(final Game game0) {
        game = game0;
    }

    public final PhaseType getPhase() {
        return phase;
    }
    private final void setPhase(final PhaseType phase0) {
        if (phase == phase0) { return; }
        phase = phase0;
        game.updatePhaseForView();
    }

    public final int getTurn() {
        return turn;
    }

    public final boolean isPlayerTurn(final Player player) {
        return player.equals(playerTurn);
    }

    public final Player getPlayerTurn() {
        return playerTurn;
    }
    private final void setPlayerTurn(final Player playerTurn0) {
        if (playerTurn == playerTurn0) { return; }
        playerTurn = playerTurn0;
        game.updatePlayerTurnForView();
        setPriority(playerTurn);
    }

    public final Player getPriorityPlayer() {
        return pPlayerPriority;
    }
    public final void setPriority(final Player p) {
        pFirstPriority = p;
        pPlayerPriority = p;
    }
    public final void resetPriority() {
        setPriority(playerTurn);
    }

    public final boolean inCombat() { return combat != null; }
    public final Combat getCombat() { return combat; }

    private void advanceToNextPhase() {
        PhaseType oldPhase = phase;

        if (bRepeatCleanup) { // for when Cleanup needs to repeat itself
            bRepeatCleanup = false;
        }
        else {
            // If the phase that's ending has a stack of additional phases
            // Take the LIFO one and move to that instead of the normal one
            if (extraPhases.containsKey(phase)) {
                PhaseType nextPhase = extraPhases.get(phase).pop();
                // If no more additional phases are available, remove it from the map
                // and let the next add, reput the key
                if (extraPhases.get(phase).isEmpty()) {
                    extraPhases.remove(phase);
                }
                setPhase(nextPhase);
            }
            else {
                setPhase(PhaseType.getNext(phase));
            }
        }

        game.getStack().clearUndoStack(); //can't undo action from previous phase

        String phaseType = oldPhase == phase ? "Repeat" : phase == PhaseType.getNext(oldPhase) ? "" : "Additional";

        if (phase == PhaseType.UNTAP) {
            turn++;
            game.updateTurnForView();
            game.fireEvent(new GameEventTurnBegan(playerTurn, turn));

            // Tokens starting game in play should suffer from Sum. Sickness
            final CardCollectionView list = playerTurn.getCardsIncludePhasingIn(ZoneType.Battlefield);
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

        game.fireEvent(new GameEventTurnPhase(playerTurn, phase, phaseType));
    }

    private boolean isSkippingPhase(final PhaseType phase) {
        // TODO: Refactor this method to replacement effect
        switch (phase) {
            case UNTAP:
                if (playerTurn.hasKeyword("Skip your next untap step.")) {
                    playerTurn.removeKeyword("Skip your next untap step.");
                    return true;
                }
                return playerTurn.hasKeyword("Skip the untap step of this turn.") || playerTurn.hasKeyword("Skip your untap step.");

            case UPKEEP:
                return playerTurn.hasKeyword("Skip your upkeep step.");

            case DRAW:
                return playerTurn.isSkippingDraw() || turn == 1 && game.getPlayers().size() == 2;

            case MAIN1:
            case MAIN2:
                return playerTurn.isSkippingMain();

            case COMBAT_BEGIN:
            case COMBAT_DECLARE_ATTACKERS:
                return playerTurn.isSkippingCombat();

            case COMBAT_DECLARE_BLOCKERS:
            case COMBAT_FIRST_STRIKE_DAMAGE:
            case COMBAT_DAMAGE:
                return !inCombat();

            default:
                return false;
        }
    }

    private final void onPhaseBegin() {
        boolean skipped = false;

        game.getTriggerHandler().resetActiveTriggers();
        if (isSkippingPhase(phase)) {
            skipped = true;
            givePriorityToPlayer = false;
            if (phase == PhaseType.COMBAT_DECLARE_ATTACKERS) {
                playerTurn.removeKeyword("Skip your next combat phase.");
            }
        }
        else  {
            // Perform turn-based actions
            switch (phase) {
                case UNTAP:
                    givePriorityToPlayer = false;
                    game.getUntap().executeUntil(playerTurn);
                    game.getUntap().executeAt();
                    break;

                case UPKEEP:
                    nUpkeepsThisTurn++;
                    nUpkeepsThisGame++;
                    game.getUpkeep().executeUntil(playerTurn);
                    game.getUpkeep().executeAt();
                    break;

                case DRAW:
                    playerTurn.drawCard();
                    break;

                case MAIN1:
                    if (playerTurn.isArchenemy() && isPreCombatMain()) {
                        playerTurn.setSchemeInMotion();
                    }
                    break;

                case COMBAT_BEGIN:
                    //PhaseUtil.verifyCombat();
                    break;

                case COMBAT_DECLARE_ATTACKERS:
                    if (!playerTurn.hasLost()) {
                        combat = new Combat(playerTurn);
                        game.getStack().freezeStack();
                        declareAttackersTurnBasedAction();
                        game.getStack().unfreezeStack();

                        if (combat != null && combat.getAttackers().isEmpty()
                                && !game.getTriggerHandler().hasDelayedTriggers()) {
                            endCombat();
                        }
                    }

                    givePriorityToPlayer = inCombat();
                    break;

                case COMBAT_DECLARE_BLOCKERS:
                    combat.removeAbsentCombatants();
                    game.getStack().freezeStack();
                    declareBlockersTurnBasedAction();
                    game.getStack().unfreezeStack();
                    break;

                case COMBAT_FIRST_STRIKE_DAMAGE:
                    if (combat.removeAbsentCombatants()) {
                        game.updateCombatForView();
                    }

                    // no first strikers, skip this step
                    if (!combat.assignCombatDamage(true)) {
                        givePriorityToPlayer = false;
                    }
                    else {
                        combat.dealAssignedDamage();
                    }
                    break;

                case COMBAT_DAMAGE:
                    if (combat.removeAbsentCombatants()) {
                        game.updateCombatForView();
                    }

                    if (!combat.assignCombatDamage(false)) {
                        givePriorityToPlayer = false;
                    }
                    else {
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
                    if (playerTurn.getController().isAI()) {
                        playerTurn.getController().resetAtEndOfTurn();
                    }
                    game.getEndOfTurn().executeAt();
                    break;

                case CLEANUP:
                    // Rule 514.1
                    final int handSize = playerTurn.getZone(ZoneType.Hand).size();
                    final int max = playerTurn.getMaxHandSize();
                    int numDiscard = playerTurn.isUnlimitedHandSize() || handSize <= max || handSize == 0 ? 0 : handSize - max;

                    if (numDiscard > 0) {
                        for (Card c : playerTurn.getController().chooseCardsToDiscardToMaximumHandSize(numDiscard)){
                            playerTurn.discard(c, null);
                        }
                    }

                    // Rule 514.2
                    // Reset Damage received map
                    for (final Card c : game.getCardsIncludePhasingIn(ZoneType.Battlefield)) {
                        c.onCleanupPhase(playerTurn);
                    }

                    game.getEndOfCombat().executeUntil(); //Repeat here in case Time Stop et. al. ends combat early
                    game.getEndOfTurn().executeUntil();

                    for (Player player : game.getPlayers()) {
                        player.onCleanupPhase();
                        player.getController().autoPassCancel(); // autopass won't wrap to next turn
                    }
                    playerTurn.removeKeyword("Skip all combat phases of this turn.");
                    game.getCleanup().executeUntil(getNextTurn());
                    nUpkeepsThisTurn = 0;

                    // Rule 514.3
                    givePriorityToPlayer = false;

                    // Rule 514.3a - state-based actions
                    game.getAction().checkStateEffects(true);
                    break;

                default:
                    break;
            }
        }

        if (!skipped) {
            // Run triggers if phase isn't being skipped
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Phase", phase.nameForScripts);
            runParams.put("Player", playerTurn);
            game.getTriggerHandler().runTrigger(TriggerType.Phase, runParams, false);
        }

        // This line fixes Combat Damage triggers not going off when they should
        game.getStack().unfreezeStack();

        // Rule 514.3a
        if (phase == PhaseType.CLEANUP && (!game.getStack().isEmpty() || game.getStack().hasSimultaneousStackEntries())) {
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
            int burn = p.getManaPool().clearPool(true).size();
            
            boolean manaBurns = game.getRules().hasManaBurn();
            if (manaBurns) {
                p.loseLife(burn);
            }
            // Play the Mana Burn sound
            if (burn > 0) {
                game.fireEvent(new GameEventManaBurn(burn, manaBurns));
            }
        }

        switch (phase) {
            case UNTAP:
                nCombatsThisTurn = 0;
                break;

            case UPKEEP:
                for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
                    c.getDamageHistory().setNotAttackedSinceLastUpkeepOf(playerTurn);
                    c.getDamageHistory().setNotBlockedSinceLastUpkeepOf(playerTurn);
                    c.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(playerTurn);
                    if (playerTurn.equals(c.getController())) {
                        c.setCameUnderControlSinceLastUpkeep(false);
                    }
                }
                game.getUpkeep().executeUntilEndOfPhase(playerTurn);
                game.getUpkeep().registerUntilEndCommand(playerTurn);
                break;

            case COMBAT_END:
                GameEventCombatEnded eventEndCombat = null;
                if (combat != null) {
                    List<Card> attackers = combat.getAttackers();
                    List<Card> blockers = combat.getAllBlockers();
                    eventEndCombat = new GameEventCombatEnded(attackers, blockers);
                }
                endCombat();
                playerTurn.resetAttackedThisCombat();

                if (eventEndCombat != null) {
                    game.fireEvent(eventEndCombat);
                }
                break;

            case CLEANUP:
                bPreventCombatDamageThisTurn = false;
                if (!bRepeatCleanup) {
                    setPlayerTurn(handleNextTurn());
                    // "Trigger" for begin turn to get around a phase skipping
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Player", playerTurn);
                    game.getTriggerHandler().runTrigger(TriggerType.TurnBegin, runParams, false);
                }
                planarDiceRolledthisTurn = 0;
                // Play the End Turn sound
                game.fireEvent(new GameEventTurnEnded());
                break;
            default: // no action
        }
    }

    private void declareAttackersTurnBasedAction() {
        final Player whoDeclares = playerDeclaresAttackers == null || playerDeclaresAttackers.hasLost()
                ? playerTurn
                : playerDeclaresAttackers;

        if (CombatUtil.canAttack(playerTurn)) {
            boolean success = false;
            do {
                if (game.isGameOver()) { // they just like to close window at any moment
                    return;
                }

                whoDeclares.getController().declareAttackers(playerTurn, combat);
                combat.removeAbsentCombatants();

                success = CombatUtil.validateAttackers(combat);
                if (!success) {
                    whoDeclares.getController().notifyOfValue(null, null, "Attack declaration invalid");
                    continue;
                }

                for (final Card attacker : combat.getAttackers()) {
                    final boolean shouldTapForAttack = !attacker.hasKeyword("Vigilance") && !attacker.hasKeyword("Attacking doesn't cause CARDNAME to tap.");
                    if (shouldTapForAttack) {
                        // set tapped to true without firing triggers because it may affect propaganda costs
                        attacker.setTapped(true);
                    }

                    final boolean canAttack = CombatUtil.checkPropagandaEffects(game, attacker, combat);
                    attacker.setTapped(false);

                    if (canAttack) {
                        if (shouldTapForAttack) {
                            attacker.tap();
                        }
                    } else {
                        combat.removeFromCombat(attacker);
                        success = CombatUtil.validateAttackers(combat);
                        if (!success) {
                            break;
                        }
                    }
                }

            } while (!success);
        }

        if (game.isGameOver()) { // they just like to close window at any moment
            return;
        }

        nCombatsThisTurn++;

        // Prepare and fire event 'attackers declared'
        Multimap<GameEntity, Card> attackersMap = ArrayListMultimap.create();
        for (GameEntity ge : combat.getDefenders()) {
            attackersMap.putAll(ge, combat.getAttackersOf(ge));
        }
        game.fireEvent(new GameEventAttackersDeclared(playerTurn, attackersMap));

        // This Exalted handler should be converted to script
        if (combat.getAttackers().size() == 1) {
            final Player attackingPlayer = combat.getAttackingPlayer();
            final Card attacker = combat.getAttackers().get(0);
            for (Card card : attackingPlayer.getCardsIn(ZoneType.Battlefield)) {
                int exaltedMagnitude = card.getAmountOfKeyword("Exalted");

                for (int i = 0; i < exaltedMagnitude; i++) {
                    String abScript = String.format("AB$ Pump | Cost$ 0 | Defined$ CardUID_%d | NumAtt$ +1 | NumDef$ +1 | StackDescription$ Exalted for attacker {c:CardUID_%d} (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn).", attacker.getId(), attacker.getId());
                    SpellAbility ability = AbilityFactory.getAbility(abScript, card);
                    ability.setActivatingPlayer(card.getController());
                    ability.setDescription(ability.getStackDescription());
                    ability.setTrigger(true);

                    game.getStack().addSimultaneousStackEntry(ability);
                }
            }
        }

        // fire AttackersDeclared trigger
        if (!combat.getAttackers().isEmpty()) {
            List<GameEntity> attackedTarget = new ArrayList<GameEntity>();
            for (final Card c : combat.getAttackers()) {
                attackedTarget.add(combat.getDefenderByAttacker(c));
            }
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Attackers", combat.getAttackers());
            runParams.put("AttackingPlayer", combat.getAttackingPlayer());
            runParams.put("AttackedTarget", attackedTarget);
            game.getTriggerHandler().runTrigger(TriggerType.AttackersDeclared, runParams, false);
        }

        for (final Card c : combat.getAttackers()) {
            CombatUtil.checkDeclaredAttacker(game, c, combat);
        }

        game.getTriggerHandler().resetActiveTriggers();
        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());
    }

    private void declareBlockersTurnBasedAction() {
        Player p = playerTurn;

        do {
            p = game.getNextPlayerAfter(p);
            // Apply Odric's effect here
            Player whoDeclaresBlockers = playerDeclaresBlockers == null || playerDeclaresBlockers.hasLost() ? p : playerDeclaresBlockers;
            if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.attackerChoosesBlockers)) {
                whoDeclaresBlockers = combat.getAttackingPlayer();
            }
            if (combat.isPlayerAttacked(p)) {
                if (CombatUtil.canBlock(p, combat)) {
                    whoDeclaresBlockers.getController().declareBlockers(p, combat);
                }
            }
            else { continue; }

            if (game.isGameOver()) { // they just like to close window at any moment
                return;
            }

            // Handles removing cards like Mogg Flunkies from combat if group block
            // didn't occur
            for (Card blocker : CardLists.filterControlledBy(combat.getAllBlockers(), p)) {
                final List<Card> attackers = new ArrayList<Card>(combat.getAttackersBlockedBy(blocker));
                for (Card attacker : attackers) {
                    boolean hasPaid = payRequiredBlockCosts(game, blocker, attacker);

                    if (!hasPaid) {
                        combat.removeBlockAssignment(attacker, blocker);
                    }
                }
            }

            // We may need to do multiple iterations removing blockers, since removing one may invalidate
            // others. The loop below is structured so that if no blockers were removed, no extra passes
            // are needed.
            boolean reachedSteadyState;
            do {
                reachedSteadyState = true;
                List<Card> remainingBlockers = CardLists.filterControlledBy(combat.getAllBlockers(), p);
                for (Card c : remainingBlockers) {
                    boolean removeBlocker = false;
                    if (remainingBlockers.size() < 2 && c.hasKeyword("CARDNAME can't attack or block alone.")) {
                        removeBlocker = true;
                    } else if (remainingBlockers.size() < 3 && c.hasKeyword("CARDNAME can't block unless at least two other creatures block.")) {
                        removeBlocker = true;
                    } else if (c.hasKeyword("CARDNAME can't block unless a creature with greater power also blocks.")) {
                        removeBlocker = true;
                        int power = c.getNetPower();
                        // Note: This is O(n^2), but there shouldn't generally be many creatures with the above keyword.
                        for (Card c2 : remainingBlockers) {
                            if (c2.getNetPower() > power) {
                                removeBlocker = false;
                                break;
                            }
                        }
                    }
                    if (removeBlocker) {
                        combat.undoBlockingAssignment(c);
                        reachedSteadyState = false;
                    }
                }
            } while (!reachedSteadyState);

            // Player is done declaring blockers - redraw UI at this point

            // map: defender => (many) attacker => (many) blocker
            Map<GameEntity, MapOfLists<Card, Card>> blockers = new HashMap<GameEntity, MapOfLists<Card,Card>>();
            for (GameEntity ge : combat.getDefendersControlledBy(p)) {
                MapOfLists<Card, Card> protectThisDefender = new HashMapOfLists<Card, Card>(CollectionSuppliers.<Card>arrayLists());
                for (Card att : combat.getAttackersOf(ge)) {
                    protectThisDefender.addAll(att, combat.getBlockers(att));
                }
                blockers.put(ge, protectThisDefender);
            }
            game.fireEvent(new GameEventBlockersDeclared(p, blockers));
        } while (p != playerTurn);

        combat.orderBlockersForDamageAssignment(); // 509.2
        combat.orderAttackersForDamageAssignment(); // 509.3

        combat.removeAbsentCombatants();

        combat.fireTriggersForUnblockedAttackers();

        final List<Card> declaredBlockers = combat.getAllBlockers();
        if (!declaredBlockers.isEmpty()) {
            final List<Card> blockedAttackers = new ArrayList<Card>();
            for (final Card blocker : declaredBlockers) {
                for (final Card blockedAttacker : combat.getAttackersBlockedBy(blocker)) {
                    if (!blockedAttackers.contains(blockedAttacker)) {
                        blockedAttackers.add(blockedAttacker);
                    }
                }
            }
            // fire blockers declared trigger
            final HashMap<String, Object> bdRunParams = new HashMap<String, Object>();
            bdRunParams.put("Blockers", declaredBlockers);
            bdRunParams.put("Attackers", blockedAttackers);
            game.getTriggerHandler().runTrigger(TriggerType.BlockersDeclared, bdRunParams, false);
        }

        for (final Card c1 : combat.getAllBlockers()) {
            if (c1.getDamageHistory().getCreatureBlockedThisCombat()) {
                continue;
            }

            if (!c1.getDamageHistory().getCreatureBlockedThisCombat()) {
                for (final SpellAbility ab : CardFactoryUtil.getBushidoEffects(c1)) {
                    game.getStack().add(ab);
                }
                // Run triggers
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Blocker", c1);
                runParams.put("Attackers", combat.getAttackersBlockedBy(c1));
                game.getTriggerHandler().runTrigger(TriggerType.Blocks, runParams, false);
            }

            c1.getDamageHistory().setCreatureBlockedThisCombat(true);
            c1.getDamageHistory().clearNotBlockedSinceLastUpkeepOf();
        }

        for (final Card a : combat.getAttackers()) {
            if (combat.isBlocked(a)) {
                a.getDamageHistory().clearNotBeenBlockedSinceLastUpkeepOf();
            }

            final List<Card> blockers = combat.getBlockers(a);
            if (blockers.isEmpty()) {
                continue;
            }

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Attacker", a);
            runParams.put("Blockers", blockers);
            runParams.put("NumBlockers", blockers.size());
            game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);
            
            // Run this trigger once for each blocker
            for (final Card b : blockers) {
            	final HashMap<String, Object> runParams2 = new HashMap<String, Object>();
            	runParams2.put("Attacker", a);
            	runParams2.put("Blocker", b);
            	game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedByCreature, runParams2, false);
            }

            if (!a.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                // Bushido
                for (final SpellAbility ab : CardFactoryUtil.getBushidoEffects(a)) {
                    game.getStack().add(ab);
                }

                // Rampage
                CombatUtil.handleRampage(game, a, blockers);
            }

            CombatUtil.handleFlankingKeyword(game, a, blockers);

            a.getDamageHistory().setCreatureGotBlockedThisCombat(true);
        }

        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());
    }

    private static boolean payRequiredBlockCosts(Game game, Card blocker, Card attacker) {
        Cost blockCost = new Cost(ManaCost.ZERO, true);
        // Sort abilities to apply them in proper order
        boolean noCost = true;
        List<ZoneType> checkZones = ZoneType.listValueOf("Battlefield,Command");
        for (Card card : game.getCardsIn(checkZones)) {
            final FCollectionView<StaticAbility> staticAbilities = card.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                Cost c1 = stAb.getBlockCost(blocker, attacker);
                if (c1 != null) {
                    blockCost.add(c1);
                    noCost = false;
                }
            }
        }
        SpellAbility fakeSA = new SpellAbility.EmptySa(blocker, blocker.getController());
        return noCost || blocker.getController().getController().payManaOptional(blocker, blockCost, fakeSA, "Pay cost to declare " + blocker + " a blocker. ", ManaPaymentPurpose.DeclareBlocker);
    }

    public final boolean isPreventCombatDamageThisTurn() {
        return bPreventCombatDamageThisTurn;
    }

    private Player handleNextTurn() {
        game.getStack().onNextTurn();
        // reset mustAttackEntity
        playerTurn.setMustAttackEntity(null);

        for (final Player p1 : game.getPlayers()) {
            for (final ZoneType z : Player.ALL_ZONES) {
                p1.getZone(z).resetCardsAddedThisTurn();
            }
        }
        for (Player p : game.getPlayers()) {
            p.resetProwl();
            p.resetSpellsCastThisTurn();
            p.setLifeLostLastTurn(p.getLifeLostThisTurn());
            p.setLifeLostThisTurn(0);
            p.setLifeGainedThisTurn(0);
            p.setLibrarySearched(0);
            p.setNumManaConversion(0);

            p.removeKeyword("Skip the untap step of this turn.");
            p.removeKeyword("Schemes can't be set in motion this turn.");
        }

        game.getTriggerHandler().clearThisTurnDelayedTrigger();
        game.getTriggerHandler().resetTurnTriggerState();

        Player next = getNextActivePlayer();

        game.getTriggerHandler().handlePlayerDefinedDelTriggers(next);

        if (game.getRules().hasAppliedVariant(GameType.Planechase)) {
            for (Card p :game.getActivePlanes()) {
                if (p != null) {
                    p.setController(next, 0);
                    game.getAction().controllerChangeZoneCorrection(p);
                }
            }
        }
        return next;
    }

    private Player getNextActivePlayer() {
        ExtraTurn extraTurn = !extraTurns.isEmpty() ? extraTurns.pop() : null;
        Player nextPlayer = extraTurn != null ? extraTurn.getPlayer() : game.getNextPlayerAfter(playerTurn);
        
        if (extraTurn != null) {
            // The bottom of the extra turn stack is the normal turn
            nextPlayer.setExtraTurn(!extraTurns.isEmpty());
            if (nextPlayer.hasKeyword("If you would begin an extra turn, skip that turn instead.")) {
                return getNextActivePlayer();
            }
            for (Trigger deltrig : extraTurn.getDelayedTriggers()) {
                game.getTriggerHandler().registerThisTurnDelayedTrigger(deltrig);
            }
        }
        else {
            nextPlayer.setExtraTurn(false);
        }

        if (nextPlayer.hasKeyword("Skip your next turn.")) {
            nextPlayer.removeKeyword("Skip your next turn.", false);
            if (extraTurn == null) { 
                setPlayerTurn(nextPlayer);
            }
            return getNextActivePlayer();
        }

        // TODO: This shouldn't filter by Time Vault, just in case Time Vault doesn't have it's normal ability.
        CardCollection vaults = CardLists.filter(nextPlayer.getCardsIn(ZoneType.Battlefield, "Time Vault"), Presets.TAPPED);
        if (!vaults.isEmpty()) {
            Card crd = vaults.getFirst();
            SpellAbility fakeSA = new SpellAbility.EmptySa(crd, nextPlayer);
            boolean untapTimeVault = nextPlayer.getController().chooseBinary(fakeSA, "Skip a turn to untap a Time Vault?", BinaryChoiceType.UntapTimeVault, false);
            if (untapTimeVault) {
                if (vaults.size() > 1) {
                    Card c = nextPlayer.getController().chooseSingleEntityForEffect(vaults, fakeSA, "Which Time Vault do you want to Untap?");
                    if (c != null)
                        crd = c;
                }
                crd.untap();
                if (extraTurn == null) {
                    setPlayerTurn(nextPlayer);
                }
                return getNextActivePlayer();
            }
        }
        
        if (extraTurn != null) {
            if (extraTurn.isSkipUntap()) {
                nextPlayer.addKeyword("Skip the untap step of this turn.");
            }
            if (extraTurn.isCantSetSchemesInMotion()) {
                nextPlayer.addKeyword("Schemes can't be set in motion this turn.");
            }
        }
        return nextPlayer;
    }

    public final synchronized boolean is(final PhaseType phase0, final Player player0) {
        return phase == phase0 && playerTurn.equals(player0);
    }
    public final synchronized boolean is(final PhaseType phase0) {
        return phase == phase0;
    }

    public final Player getNextTurn() {
        if (extraTurns.isEmpty()) {
            return game.getNextPlayerAfter(playerTurn);
        }
        return extraTurns.peek().getPlayer();
    }

    public final ExtraTurn addExtraTurn(final Player player) {
        // use a stack to handle extra turns, make sure the bottom of the stack
        // restores original turn order
        if (extraTurns.isEmpty()) {
            extraTurns.push(new ExtraTurn(game.getNextPlayerAfter(playerTurn)));
        }
        return extraTurns.push(new ExtraTurn(player));
    }

    public final void addExtraPhase(final PhaseType afterPhase, final PhaseType extraPhase) {
        // 500.8. Some effects can add phases to a turn. They do this by adding the phases directly after the specified phase.
        // If multiple extra phases are created after the same phase, the most recently created phase will occur first.
        if (!extraPhases.containsKey(afterPhase)) {
            extraPhases.put(afterPhase, new Stack<PhaseType>());
        }
        extraPhases.get(afterPhase).push(extraPhase);
    }

    public final boolean isFirstCombat() {
        return (nCombatsThisTurn == 1);
    }

    public final boolean isFirstUpkeep() {
        return (nUpkeepsThisTurn == 0);
    }

    public final boolean isFirstUpkeepThisGame() {
        return (nUpkeepsThisGame == 0);
    }

    public final boolean isPreCombatMain() {
        return (nCombatsThisTurn == 0);
    }

    private final static boolean DEBUG_PHASES = false;

    public void startFirstTurn(Player goesFirst) {
        StopWatch sw = new StopWatch();

        if (phase != null) {
            throw new IllegalStateException("Turns already started, call this only once per game");
        }

        setPlayerTurn(goesFirst);
        advanceToNextPhase();
        onPhaseBegin();

        // don't even offer priority, because it's untap of 1st turn now
        givePriorityToPlayer = false;

        final Set<Card> allAffectedCards = new HashSet<Card>();

        // MAIN GAME LOOP
        while (!game.isGameOver()) {
            if (givePriorityToPlayer) {
                if (DEBUG_PHASES) {
                    sw.start();
                }

                game.fireEvent(new GameEventPlayerPriority(playerTurn, phase, getPriorityPlayer()));
                List<SpellAbility> chosenSa = null;

                int loopCount = 0;
                do {
                    do {
                        // Rule 704.3  Whenever a player would get priority, the game checks ... for state-based actions,
                        game.getAction().checkStateEffects(false, allAffectedCards);
                        if (game.isGameOver()) {
                            return; // state-based effects check could lead to game over
                        }
                    } while (game.getStack().addAllTriggeredAbilitiesToStack()); //loop so long as something was added to stack

                    if (!allAffectedCards.isEmpty()) {
                        game.fireEvent(new GameEventCardStatsChanged(allAffectedCards));
                        allAffectedCards.clear();
                    }

                    if (playerTurn.hasLost() && pPlayerPriority.equals(playerTurn) && pFirstPriority.equals(playerTurn)) {
                        // If the active player has lost, and they have priority, set the next player to have priority
                        System.out.println("Active player is no longer in the game...");
                        pPlayerPriority = game.getNextPlayerAfter(getPriorityPlayer());
                        pFirstPriority = pPlayerPriority;
                    }

                    chosenSa = pPlayerPriority.getController().chooseSpellAbilityToPlay();
                    if (chosenSa == null) {
                        break; // that means 'I pass'
                    }
                    if (DEBUG_PHASES) {
                        System.out.print("... " + pPlayerPriority + " plays " + chosenSa);
                    }
                    pFirstPriority = pPlayerPriority; // all opponents have to pass before stack is allowed to resolve
                    for (SpellAbility sa : chosenSa) {
                        pPlayerPriority.getController().playChosenSpellAbility(sa);
                    }
                    loopCount++;
                } while (loopCount < 999 || !pPlayerPriority.getController().isAI());

                if (loopCount >= 999 && pPlayerPriority.getController().isAI()) {
                    System.out.print("AI looped too much with: " + chosenSa);
                }

                if (DEBUG_PHASES) {
                    sw.stop();
                    System.out.print("... passed in " + sw.getTime()/1000f + " s\n");
                    System.out.println("\t\tStack: " + game.getStack());
                    sw.reset();
                }
            }
            else if (DEBUG_PHASES){
                System.out.print(" >> (no priority given to " + getPriorityPlayer() + ")\n");
            }

            // actingPlayer is the player who may act
            // the firstAction is the player who gained Priority First in this segment
            // of Priority
            Player nextPlayer = game.getNextPlayerAfter(getPriorityPlayer());

            if (game.isGameOver() || nextPlayer == null) { return; } // conceded?

            if (DEBUG_PHASES) {
                System.out.println(String.format("%s %s: %s is active, previous was %s", playerTurn, phase, pPlayerPriority, nextPlayer));
            }
            if (pFirstPriority == nextPlayer) {
                if (game.getStack().isEmpty()) {
                    if (playerTurn.hasLost()) {
                        setPriority(game.getNextPlayerAfter(playerTurn));
                    }
                    else {
                        setPriority(playerTurn);
                    }

                    // end phase
                    givePriorityToPlayer = true;
                    onPhaseEnd();
                    advanceToNextPhase();
                    onPhaseBegin();
                }
                else if (!game.getStack().hasSimultaneousStackEntries()) {
                    game.getStack().resolveStack();
                }
            }
            else {
                // pass the priority to other player
                pPlayerPriority = nextPlayer;
            }

            // If ever the karn's ultimate resolved
            if (game.getAge() == GameStage.RestartedByKarn) {
                setPhase(null);
                game.updatePhaseForView();
                game.fireEvent(new GameEventGameRestarted(playerTurn));
                return;
            }
        }
    }

    // this is a hack for the setup game state mode, do not use outside of devSetupGameState code
    // as it avoids calling any of the phase effects that may be necessary in a less enforced context
    public final void devModeSet(final PhaseType phase0, final Player player0) {
        if (phase0 != null) {
            setPhase(phase0);
        }
        if (player0 != null) {
            setPlayerTurn(player0);
        }

        game.fireEvent(new GameEventTurnPhase(playerTurn, phase, ""));
        endCombat(); // not-null can be created only when declare attackers phase begins
    }

    public final void endTurnByEffect() {
        endCombat();
        extraPhases.clear();
        setPhase(PhaseType.CLEANUP);
        onPhaseBegin();
    }

    public final void setPreventCombatDamageThisTurn() {
        bPreventCombatDamageThisTurn = true;
    }

    public int getPlanarDiceRolledthisTurn() {
        return planarDiceRolledthisTurn;
    }
    public void incPlanarDiceRolledthisTurn() {
        planarDiceRolledthisTurn++;
    }

    public String debugPrintState(boolean hasPriority) {
        return String.format("%s's %s [%sP] %s", playerTurn, phase.nameForUi, hasPriority ? "+" : "-", getPriorityPlayer());
    }

    // just to avoid exposing variable to outer classes
    public void onStackResolved() {
        givePriorityToPlayer = true;
    }

    public final void setPlayerDeclaresAttackers(Player player) {
        playerDeclaresAttackers = player;
    }

    public final void setPlayerDeclaresBlockers(Player player) {
        playerDeclaresBlockers = player;
    }

    public void endCombat() {
        if (combat != null) {
            combat.endCombat();
            combat = null;
        }
        game.updateCombatForView();
    }
}
