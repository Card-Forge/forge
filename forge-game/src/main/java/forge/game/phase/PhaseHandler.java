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

import java.util.*;

import com.google.common.collect.*;
import org.apache.commons.lang3.time.StopWatch;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityKey;
import forge.game.ability.effects.AddTurnEffect;
import forge.game.ability.effects.SkipPhaseEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.CostEnlist;
import forge.game.cost.CostExert;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventCombatEnded;
import forge.game.event.GameEventGameRestarted;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.event.GameEventTokenStateUpdate;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnEnded;
import forge.game.event.GameEventTurnPhase;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.CollectionSuppliers;
import forge.util.TextUtil;
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
    private static final long serialVersionUID = 5207222278370963197L;

    // Start turn at 0, since we start even before first untap
    private PhaseType phase = null;
    private int turn = 0;

    private final transient Stack<ExtraTurn> extraTurns = new Stack<>();
    private final transient Map<PhaseType, Stack<ExtraPhase>> extraPhases = Maps.newEnumMap(PhaseType.class);

    private int nUpkeepsThisTurn = 0;
    private int nUpkeepsThisGame = 0;
    private int nCombatsThisTurn = 0;
    private int nMain2sThisTurn = 0;
    private int planarDiceSpecialActionThisTurn = 0;

    private transient Player playerTurn = null;
    private transient Player playerPreviousTurn = null;

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
    public final void setPlayerTurn(final Player playerTurn0) {
        if (playerTurn == playerTurn0) { return; }
        playerTurn = playerTurn0;
        game.updatePlayerTurnForView();
        setPriority(playerTurn);
    }

    public final Player getPreviousPlayerTurn() {
        return playerPreviousTurn;
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
        boolean isTopsy = playerTurn.getAmountOfKeyword("The phases of your turn are reversed.") % 2 == 1;
        boolean turnEnded = false;

        game.getStack().clearUndoStack(); //can't undo action from previous phase

        if (bRepeatCleanup) { // for when Cleanup needs to repeat itself
            bRepeatCleanup = false;
        } else {
            // If the phase that's ending has a stack of additional phases
            // Take the LIFO one and move to that instead of the normal one
            ExtraPhase extraPhase = null;
            if (extraPhases.containsKey(phase)) {
                extraPhase = extraPhases.get(phase).pop();
                PhaseType nextPhase = extraPhase.getPhase();
                // If no more additional phases are available, remove it from the map
                // and let the next add, reput the key
                if (extraPhases.get(phase).isEmpty()) {
                    extraPhases.remove(phase);
                }
                setPhase(nextPhase);
            } else {
                turnEnded = PhaseType.isLast(phase, isTopsy);
                setPhase(PhaseType.getNext(phase, isTopsy));
            }

            if (turnEnded) {
                turn++;
                extraPhases.clear();
                game.updateTurnForView();
                game.fireEvent(new GameEventTurnBegan(playerTurn, turn));

                // Tokens starting game in play should suffer from Sum. Sickness
                for (final Card c : playerTurn.getCardsIn(ZoneType.Battlefield, false)) {
                    if (playerTurn.getTurn() > 0 || !c.isStartsGameInPlay()) {
                        c.setSickness(false);
                    }
                }
                playerTurn.incrementTurn();

                game.getAction().resetActivationsPerTurn();

                final int lands = CardLists.count(playerTurn.getLandsInPlay(), Presets.UNTAPPED);
                playerTurn.setNumPowerSurgeLands(lands);
            }
            //update tokens
            game.fireEvent(new GameEventTokenStateUpdate(playerTurn.getTokensInPlay()));

            // Replacement effects
            final Map<AbilityKey, Object> repRunParams = AbilityKey.mapFromAffected(playerTurn);
            repRunParams.put(AbilityKey.Phase, phase.nameForScripts);
            ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.BeginPhase, repRunParams);
            if (repres != ReplacementResult.NotReplaced) {
                // Currently there is no effect to skip entire beginning phase
                // If in the future that kind of effect is added, need to handle it too.
                // Handle skipping of entire combat phase
                if (phase == PhaseType.COMBAT_BEGIN) {
                    setPhase(PhaseType.COMBAT_END);
                }
                advanceToNextPhase();
                return;
            }

            if (extraPhase != null) {
                for (Trigger deltrig : extraPhase.getDelayedTriggers()) {
                    game.getTriggerHandler().registerThisTurnDelayedTrigger(deltrig);
                }
            }
        }

        String phaseType = oldPhase == phase ? "Repeat" : phase == PhaseType.getNext(oldPhase, isTopsy) ? "" : "Additional";
        game.fireEvent(new GameEventTurnPhase(playerTurn, phase, phaseType));
    }

    private boolean isSkippingPhase(final PhaseType phase) {
        switch (phase) {
            case DRAW:
                return turn == 1 && game.getPlayers().size() == 2;

            case COMBAT_BEGIN:
            case COMBAT_DECLARE_ATTACKERS:
                return playerTurn.isSkippingCombat();

            case COMBAT_DECLARE_BLOCKERS:
                if (inCombat() && combat.getAttackers().isEmpty()) {
                    endCombat();
                }
                //$FALL-THROUGH$
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
        } else  {
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
                    for (Player p : game.getPlayers()) {
                        p.resetNumDrawnThisDrawStep();
                    }
                    playerTurn.drawCard();
                    for (Player p : game.getPlayers()) {
                        if (p.isOpponentOf(playerTurn) &&
                                p.hasKeyword("You draw a card during each opponent's draw step.")) {
                            p.drawCard();
                        }
                    }
                    break;

                case MAIN1:
                    {
                        if (playerTurn.isArchenemy()) {
                            playerTurn.setSchemeInMotion();
                        }
                        GameEntityCounterTable table = new GameEntityCounterTable();
                        // all Saga get Lore counter at the begin of pre combat
                        for (Card c : playerTurn.getCardsIn(ZoneType.Battlefield)) {
                            if (c.getType().hasSubtype("Saga")) {
                                c.addCounter(CounterEnumType.LORE, 1, playerTurn, table);
                            }
                        }
                        table.replaceCounterEffect(game, null, false);
                    }
                    break;

                case COMBAT_BEGIN:
                    nCombatsThisTurn++;
                    combat = new Combat(playerTurn);
                    //PhaseUtil.verifyCombat();
                    break;

                case COMBAT_DECLARE_ATTACKERS:
                    combat.initConstraints();
                    game.getStack().freezeStack();
                    declareAttackersTurnBasedAction();
                    game.getStack().unfreezeStack();

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
                    } else {
                        combat.dealAssignedDamage();
                    }
                    break;

                case COMBAT_DAMAGE:
                    if (combat.removeAbsentCombatants()) {
                        game.updateCombatForView();
                    }

                    if (!combat.assignCombatDamage(false)) {
                        givePriorityToPlayer = false;
                    } else {
                        combat.dealAssignedDamage();
                    }
                    break;

                case COMBAT_END:
                    // End Combat always happens
                    for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                        c.onEndOfCombat(playerTurn);
                    }
                    game.getEndOfCombat().executeAt();

                    //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                    break;

                case MAIN2:
                    //SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
                    break;

                case END_OF_TURN:
                    game.getEndOfTurn().executeUntil(playerTurn);
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
                        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                        moveParams.put(AbilityKey.LastStateBattlefield, game.getLastStateBattlefield());
                        moveParams.put(AbilityKey.LastStateGraveyard, game.getLastStateGraveyard());

                        final CardZoneTable table = new CardZoneTable();
                        final CardCollection discarded = new CardCollection();
                        boolean firstDiscarded = playerTurn.getNumDiscardedThisTurn() == 0;
                        for (Card c : playerTurn.getController().chooseCardsToDiscardToMaximumHandSize(numDiscard)) {
                            if (playerTurn.discard(c, null, false, table, moveParams) != null) {
                                discarded.add(c);
                            }
                        }
                        table.triggerChangesZoneAll(game, null);

                        if (!discarded.isEmpty()) {
                            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(playerTurn);
                            runParams.put(AbilityKey.Cards, discarded);
                            runParams.put(AbilityKey.Cause, null);
                            runParams.put(AbilityKey.FirstTime, firstDiscarded);
                            game.getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);
                        }
                    }

                    // Rule 514.2
                    // Reset Damage received map
                    for (final Card c : game.getCardsIncludePhasingIn(ZoneType.Battlefield)) {
                        c.onCleanupPhase(playerTurn);
                    }

                    game.getEndOfTurn().executeUntil();
                    game.getEndOfTurn().executeUntilEndOfPhase(playerTurn);
                    game.getEndOfTurn().registerUntilEndCommand(playerTurn);
                    game.getEndOfCombat().registerUntilEndCommand(playerTurn);

                    for (Player player : game.getPlayers()) {
                        player.getController().autoPassCancel(); // autopass won't wrap to next turn
                    }

                    nUpkeepsThisTurn = 0;
                    nCombatsThisTurn = 0;
                    nMain2sThisTurn = 0;
                    game.getStack().resetMaxDistinctSources();

                    // Rule 514.3
                    givePriorityToPlayer = false;

                    // Rule 514.3a - state-based actions
                    if (game.getAction().checkStateEffects(true)) {
                        bRepeatCleanup = true;
                        givePriorityToPlayer = true;
                    }
                    break;

                default:
                    break;
            }
        }

        if (!skipped) {
            // Run triggers if phase isn't being skipped
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(playerTurn);
            runParams.put(AbilityKey.Phase, phase.nameForScripts);
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

        final Map<Player, Integer> lossMap = Maps.newHashMap();
        for (Player p : game.getPlayers()) {
            int burn = p.getManaPool().clearPool(true).size();

            if (p.getManaPool().hasBurn()) {
                final int lost = p.loseLife(burn, false, true);
                if (lost > 0) {
                    lossMap.put(p, lost);
                }
            }
        }
        if (!lossMap.isEmpty()) { // Run triggers if any player actually lost life
            final Map<AbilityKey, Object> runLifeLostParams = AbilityKey.mapFromPIMap(lossMap);
            game.getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runLifeLostParams, false);
        }

        switch (phase) {
            case UPKEEP:
                for (Card c : game.getCardsIncludePhasingIn(ZoneType.Battlefield)) {
                    c.getDamageHistory().setNotAttackedSinceLastUpkeepOf(playerTurn);
                    c.getDamageHistory().setNotBlockedSinceLastUpkeepOf(playerTurn);
                    c.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(playerTurn);
                    if (playerTurn.equals(c.getController()) && c.getTurnInZone() < game.getPhaseHandler().getTurn()) {
                        c.setCameUnderControlSinceLastUpkeep(false);
                    }
                }
                game.getUpkeep().executeUntilEndOfPhase(playerTurn);
                game.getUpkeep().registerUntilEndCommand(playerTurn);
                break;

            case COMBAT_END:
                GameEventCombatEnded eventEndCombat = null;
                if (inCombat()) {
                    List<Card> attackers = combat.getAttackers();
                    List<Card> blockers = combat.getAllBlockers();
                    eventEndCombat = new GameEventCombatEnded(attackers, blockers);
                }
                endCombat();

                if (eventEndCombat != null) {
                    game.fireEvent(eventEndCombat);
                }
                break;

            case MAIN2:
                nMain2sThisTurn++;
                break;

            case CLEANUP:
                if (!bRepeatCleanup) {
                    // only call onCleanupPhase when Cleanup is not repeated
                    game.onCleanupPhase();
                    // set previous player
                    playerPreviousTurn = this.getPlayerTurn();
                    setPlayerTurn(handleNextTurn());

                    // start effects for next turn (do this first for ControlPlayer)
                    game.getCleanup().executeUntil();
                    // done this after check state effects, so it only has effect next check
                    game.getCleanup().executeUntil(playerTurn);

                    // "Trigger" for begin turn to get around a phase skipping
                    final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(playerTurn);
                    game.getTriggerHandler().runTrigger(TriggerType.TurnBegin, runParams, false);
                }
                planarDiceSpecialActionThisTurn = 0;
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

                final CardCollection untapFromCancel = new CardCollection();
                // do a full loop first so attackers can't be used to pay for Propaganda
                for (final Card attacker : combat.getAttackers()) {
                    if (!attacker.attackVigilance()) {
                        // set tapped to true without firing triggers because it may affect propaganda costs
                        attacker.setTapped(true);
                        untapFromCancel.add(attacker);
                    }
                }

                // CR 508.1g
                List<Card> possibleExerters = CombatUtil.getOptionalAttackCostCreatures(combat.getAttackers(), CostExert.class);
                if (!possibleExerters.isEmpty()) {
                    possibleExerters = whoDeclares.getController().exertAttackers(possibleExerters);
                }

                List<Card> possibleEnlisters = CombatUtil.getOptionalAttackCostCreatures(combat.getAttackers(), CostEnlist.class);
                if (!possibleEnlisters.isEmpty()) {
                    // TODO might want to skip if can't be paid
                    possibleEnlisters = whoDeclares.getController().enlistAttackers(possibleEnlisters);
                    possibleExerters.addAll(possibleEnlisters);
                }

                for (final Card attacker : combat.getAttackers()) {
                    // TODO currently doesn't refund previous attackers (can really only happen if you cancel paying for a creature with an attack requirement that could be satisfied without a tax)
                    final boolean canAttack = CombatUtil.checkPropagandaEffects(game, attacker, combat, possibleExerters);

                    if (!canAttack) {
                        combat.removeFromCombat(attacker);
                        if (untapFromCancel.contains(attacker)) {
                            attacker.setTapped(false);
                        }
                        success = CombatUtil.validateAttackers(combat);
                        if (!success) {
                            for (Card c : untapFromCancel) {
                                c.setTapped(false);
                            }
                            // might have been sacrificed while paying
                            combat.removeAbsentCombatants();
                            combat.initConstraints();
                            break;
                        }
                    }
                }

            } while (!success);

            for (final Card attacker : combat.getAttackers()) {
                if (!attacker.attackVigilance()) {
                    attacker.setTapped(false);
                    attacker.tap(true, true, null, null);
                }
            }
        }

        if (game.isGameOver()) { // they just like to close window at any moment
            return;
        }

        // Reset all active Triggers
        game.getTriggerHandler().resetActiveTriggers();

        // Prepare and fire event 'attackers declared'
        Multimap<GameEntity, Card> attackersMap = ArrayListMultimap.create();
        for (GameEntity ge : combat.getDefenders()) {
            attackersMap.putAll(ge, combat.getAttackersOf(ge));
        }
        game.fireEvent(new GameEventAttackersDeclared(playerTurn, attackersMap));

        // fire AttackersDeclared trigger
        if (!combat.getAttackers().isEmpty()) {
            List<GameEntity> attackedTarget = new ArrayList<>();
            for (GameEntity ge : combat.getDefenders()) {
                if (!combat.getAttackersOf(ge).isEmpty()) {
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Attackers, combat.getAttackersOf(ge));
                    runParams.put(AbilityKey.AttackingPlayer, combat.getAttackingPlayer());
                    runParams.put(AbilityKey.AttackedTarget, Collections.singletonList(ge));
                    attackedTarget.add(ge);
                    game.getTriggerHandler().runTrigger(TriggerType.AttackersDeclaredOneTarget, runParams, false);
                }
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Attackers, combat.getAttackers());
            runParams.put(AbilityKey.AttackingPlayer, combat.getAttackingPlayer());
            runParams.put(AbilityKey.AttackedTarget, attackedTarget);
            game.getTriggerHandler().runTrigger(TriggerType.AttackersDeclared, runParams, false);
        }

        playerTurn.clearAttackedPlayersMyCombat();
        for (final Card c : combat.getAttackers()) {
            CombatUtil.checkDeclaredAttacker(game, c, combat, true);
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
                    // Replacement effects (for Camouflage)
                    final Map<AbilityKey, Object> repRunParams = AbilityKey.mapFromAffected(p);
                    repRunParams.put(AbilityKey.Player, whoDeclaresBlockers);
                    ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.DeclareBlocker, repRunParams);
                    if (repres == ReplacementResult.NotReplaced) {
                        // If not replaced, run normal declare blockers
                        whoDeclaresBlockers.getController().declareBlockers(p, combat);
                    }
                }
            }
            else { continue; }

            if (game.isGameOver()) { // they just like to close window at any moment
                return;
            }

            // Handles removing cards like Mogg Flunkies from combat if group block didn't occur
            for (Card blocker : CardLists.filterControlledBy(combat.getAllBlockers(), p)) {
                final List<Card> attackers = combat.getAttackersBlockedBy(blocker);
                for (Card attacker : attackers) {
                    boolean hasPaid = CombatUtil.payRequiredBlockCosts(game, blocker, attacker);

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
                    boolean cantBlockAlone = c.hasKeyword("CARDNAME can't attack or block alone.") || c.hasKeyword("CARDNAME can't block alone.");
                    if (remainingBlockers.size() < 2 && cantBlockAlone) {
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
            Map<GameEntity, MapOfLists<Card, Card>> blockers = Maps.newHashMap();
            for (GameEntity ge : combat.getDefendersControlledBy(p)) {
                MapOfLists<Card, Card> protectThisDefender = new HashMapOfLists<>(CollectionSuppliers.arrayLists());
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

        combat.fireTriggersForUnblockedAttackers(game);

        final List<Card> declaredBlockers = combat.getAllBlockers();
        if (!declaredBlockers.isEmpty()) {
            final List<Card> blockedAttackers = Lists.newArrayList();
            for (final Card blocker : declaredBlockers) {
                for (final Card blockedAttacker : combat.getAttackersBlockedBy(blocker)) {
                    if (!blockedAttackers.contains(blockedAttacker)) {
                        blockedAttackers.add(blockedAttacker);
                    }
                }
            }
            // fire blockers declared trigger
            final Map<AbilityKey, Object> bdRunParams = AbilityKey.newMap();
            bdRunParams.put(AbilityKey.Blockers, declaredBlockers);
            bdRunParams.put(AbilityKey.Attackers, blockedAttackers);
            game.getTriggerHandler().runTrigger(TriggerType.BlockersDeclared, bdRunParams, false);
        }

        for (final Card c1 : combat.getAllBlockers()) {
            if (c1.getDamageHistory().getCreatureBlockedThisCombat()) {
                continue;
            }

            if (!c1.getDamageHistory().getCreatureBlockedThisCombat()) {
                // Run triggers
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Blocker, c1);
                runParams.put(AbilityKey.Attackers, combat.getAttackersBlockedBy(c1));
                game.getTriggerHandler().runTrigger(TriggerType.Blocks, runParams, false);
            }

            c1.getDamageHistory().setCreatureBlockedThisCombat(true);
            c1.getDamageHistory().clearNotBlockedSinceLastUpkeepOf();
        }

        List<Card> blocked = Lists.newArrayList();

        for (final Card a : combat.getAttackers()) {
            if (combat.isBlocked(a)) {
                a.getDamageHistory().clearNotBeenBlockedSinceLastUpkeepOf();
            }

            final List<Card> blockers = combat.getBlockers(a);
            if (blockers.isEmpty()) {
                continue;
            }

            blocked.add(a);

            // Run triggers
            {
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Attacker, a);
                runParams.put(AbilityKey.Blockers, blockers);
                runParams.put(AbilityKey.Defender, combat.getDefenderByAttacker(a));
                runParams.put(AbilityKey.DefendingPlayer, combat.getDefenderPlayerByAttacker(a));
                game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);
            }

            // Run this trigger once for each blocker
            for (final Card b : blockers) {
                b.addBlockedThisTurn(CardUtil.getLKICopy(a));
                a.addBlockedByThisTurn(CardUtil.getLKICopy(b));

            	final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Attacker, a);
                runParams.put(AbilityKey.Blocker, b);
            	game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedByCreature, runParams, false);
            }

            a.getDamageHistory().setCreatureGotBlockedThisCombat(true);
        }

        if (!blocked.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Attackers, blocked);
            game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedOnce, runParams, false);
        }

        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());
    }

    public void restart() {
        extraPhases.clear();
        extraTurns.clear();
        turn = 0;
    }

    private Player handleNextTurn() {
        game.getStack().onNextTurn();

        game.getTriggerHandler().clearThisTurnDelayedTrigger();

        Player next = getNextActivePlayer();
        while (next.hasLost()) {
            next = getNextActivePlayer();
        }

        game.getTriggerHandler().handlePlayerDefinedDelTriggers(next);

        game.setMonarchBeginTurn(game.getMonarch());

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
        // The bottom of the extra turn stack is the normal turn
        boolean isExtraTurn = !extraTurns.isEmpty();

        // update ExtraTurn Count
        nextPlayer.setExtraTurnCount(getExtraTurnForPlayer(nextPlayer));

        // Replacement effects
        final Map<AbilityKey, Object> repRunParams = AbilityKey.mapFromAffected(nextPlayer);
        repRunParams.put(AbilityKey.ExtraTurn, isExtraTurn);
        ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.BeginTurn, repRunParams);
        if (repres != ReplacementResult.NotReplaced) {
            if (extraTurn == null) {
                setPlayerTurn(nextPlayer);
            }
            return getNextActivePlayer();
        }

        nextPlayer.setExtraTurn(isExtraTurn);
        if (extraTurn != null) {
            for (Trigger deltrig : extraTurn.getDelayedTriggers()) {
                game.getTriggerHandler().registerThisTurnDelayedTrigger(deltrig);
            }
            if (extraTurn.isSkipUntap()) {
                SkipPhaseEffect.createSkipPhaseEffect(extraTurn.getSkipUntapSA(), nextPlayer, null, null, "Untap");
            }
            if (extraTurn.isCantSetSchemesInMotion()) {
                AddTurnEffect.createCantSetSchemesInMotionEffect(extraTurn.getCantSetSchemesInMotionSA());
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
        Player previous = null;
        // use a stack to handle extra turns, make sure the bottom of the stack
        // restores original turn order
        if (extraTurns.isEmpty()) {
            extraTurns.push(new ExtraTurn(game.getNextPlayerAfter(playerTurn)));
        } else {
            previous = extraTurns.peek().getPlayer();
        }

        ExtraTurn result = extraTurns.push(new ExtraTurn(player));
        // update Extra Turn for all players
        for (final Player p : game.getPlayers()) {
            p.setExtraTurnCount(getExtraTurnForPlayer(p));
        }

        // get all players where the view should be updated
        List<Player> toUpdate = Lists.newArrayList(player);
        if (previous != null) {
            toUpdate.add(previous);
        }

        // fireEvent to update the Details
        game.fireEvent(new GameEventPlayerStatsChanged(toUpdate, false));

        return result;
    }

    /**
     * Add an extra phase between afterPhase and nextPhase
     * @param afterPhase The phase to add extra phase after
     * @param extraPhaseList The list of extra phase(s) to be added
     * @param nextPhase The original next phase following afterPhase, after extra phase the flow will return to this phase
     * @return returns the added ExtraPhase object
     */
    public final ExtraPhase addExtraPhase(final PhaseType afterPhase, final List<PhaseType> extraPhaseList, PhaseType nextPhase) {
        // 500.8. Some effects can add phases to a turn. They do this by adding the phases directly after the specified phase.
        // If multiple extra phases are created after the same phase, the most recently created phase will occur first.
        for (int i = 0; i < extraPhaseList.size(); i++) {
            PhaseType extra = extraPhaseList.get(i);
            if (!extraPhases.containsKey(extra)) {
                extraPhases.put(extra, new Stack<>());
            }
            if (i < extraPhaseList.size() - 1 ) {
                extraPhases.get(extra).push(new ExtraPhase(extraPhaseList.get(i + 1)));
            } else {
                if (extraPhases.containsKey(afterPhase) && !extraPhases.get(afterPhase).isEmpty()) {
                    // Extra phase(s) was inserted already, link to the first step of inserted extra phase(s)
                    extraPhases.get(extra).push(extraPhases.get(afterPhase).pop());
                } else {
                    extraPhases.get(extra).push(new ExtraPhase(nextPhase));
                }
            }
        }
        if (!extraPhases.containsKey(afterPhase)) {
            extraPhases.put(afterPhase, new Stack<>());
        }
        return extraPhases.get(afterPhase).push(new ExtraPhase(extraPhaseList.get(0)));
    }

    public final boolean isFirstCombat() {
        return nCombatsThisTurn == 1;
    }
    public final int getNumCombat() {
        return nCombatsThisTurn;
    }

    public final boolean isFirstUpkeep() {
        return is(PhaseType.UPKEEP) && nUpkeepsThisTurn == 0;
    }

    public final boolean isFirstUpkeepThisGame() {
        return is(PhaseType.UPKEEP) && nUpkeepsThisGame == 0;
    }

    public final boolean isPreCombatMain() {
        // 505.1a. Only the first main phase of the turn is a precombat main phase.
        return is(PhaseType.MAIN1);
    }

    public final boolean beforeFirstPostCombatMainEnd() {
        return nMain2sThisTurn == 0;
    }

    private final static boolean DEBUG_PHASES = false;

    public void startFirstTurn(Player goesFirst) {
        startFirstTurn(goesFirst, null);
    }
    public void startFirstTurn(Player goesFirst, Runnable startGameHook) {
        StopWatch sw = new StopWatch();

        if (phase != null) {
            throw new IllegalStateException("Turns already started, call this only once per game");
        }

        setPlayerTurn(goesFirst);
        advanceToNextPhase();
        onPhaseBegin();

        // don't even offer priority, because it's untap of 1st turn now
        givePriorityToPlayer = false;

        if (startGameHook != null) {
            startGameHook.run();
            givePriorityToPlayer = true;
        }

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
                    if (checkStateBasedEffects()) {
                        // state-based effects check could lead to game over
                        return;
                    }

                    chosenSa = pPlayerPriority.getController().chooseSpellAbilityToPlay();

                    // this needs to come after chosenSa so it sees you conceding on own turn
                    if (playerTurn.hasLost() && pPlayerPriority.equals(playerTurn) && pFirstPriority.equals(playerTurn)) {
                        // If the active player has lost, and they have priority, set the next player to have priority
                        System.out.println("Active player is no longer in the game...");
                        pPlayerPriority = game.getNextPlayerAfter(getPriorityPlayer());
                        pFirstPriority = pPlayerPriority;
                    }

                    if (chosenSa == null) {
                        break; // that means 'I pass'
                    }
                    if (DEBUG_PHASES) {
                        System.out.print("... " + pPlayerPriority + " plays " + chosenSa);
                    }
                    for (SpellAbility sa : chosenSa) {
                        Card saHost = sa.getHostCard();
                        final Zone originZone = saHost.getZone();

                        if (pPlayerPriority.getController().playChosenSpellAbility(sa)) {
                            pFirstPriority = pPlayerPriority; // all opponents have to pass before stack is allowed to resolve
                        }

                        saHost = game.getCardState(saHost);
                        final Zone currentZone = saHost.getZone();

                        // Need to check if Zone did change
                        if (currentZone != null && originZone != null && !currentZone.equals(originZone) && (sa.isSpell() || sa instanceof LandAbility)) {
                            // currently there can be only one Spell put on the Stack at once, or Land Abilities be played
                            final CardZoneTable triggerList = new CardZoneTable();
                            triggerList.put(originZone.getZoneType(), currentZone.getZoneType(), saHost);
                            triggerList.triggerChangesZoneAll(game, sa);
                        }
                    }
                    game.copyLastState();
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
            else if (DEBUG_PHASES) {
                System.out.print(" >> (no priority given to " + getPriorityPlayer() + ")\n");
            }

            // actingPlayer is the player who may act
            // the firstAction is the player who gained Priority First in this segment
            // of Priority
            Player nextPlayer = game.getNextPlayerAfter(getPriorityPlayer());

            if (game.isGameOver() || nextPlayer == null) { return; } // conceded?

            if (DEBUG_PHASES) {
                System.out.println(TextUtil.concatWithSpace(playerTurn.toString(),TextUtil.addSuffix(phase.toString(),":"), pPlayerPriority.toString(),"is active, previous was", nextPlayer.toString()));
            }
            if (pFirstPriority == nextPlayer) {
                if (game.getStack().isEmpty()) {
                    if (playerTurn.hasLost()) {
                        setPriority(game.getNextPlayerAfter(playerTurn));
                    } else {
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
            } else {
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

            // update Priority for all players
            for (final Player p : game.getPlayers()) {
                if (getPriorityPlayer() == p)
                    p.setHasPriority(true);
                else
                    p.setHasPriority(false);
            }
        }
    }

    private boolean checkStateBasedEffects() {
        final Set<Card> allAffectedCards = new HashSet<>();
        do {
            // Rule 704.3  Whenever a player would get priority, the game checks ... for state-based actions,
            game.getAction().checkStateEffects(false, allAffectedCards);
            if (game.isGameOver()) {
                return true; // state-based effects check could lead to game over
            }
        } while (game.getStack().addAllTriggeredAbilitiesToStack()); //loop so long as something was added to stack

        if (!allAffectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(allAffectedCards));
            allAffectedCards.clear();
        }
        return false;
    }

    public final boolean devAdvanceToPhase(PhaseType targetPhase) {
        return devAdvanceToPhase(targetPhase, null);
    }
    public final boolean devAdvanceToPhase(PhaseType targetPhase, Runnable resolver) {
        boolean isTopsy = playerTurn.getAmountOfKeyword("The phases of your turn are reversed.") % 2 == 1;
        while (phase.isBefore(targetPhase, isTopsy)) {
            if (checkStateBasedEffects()) {
                return false;
            }
            if (resolver != null) {
                resolver.run();
            }
            onPhaseEnd();
            advanceToNextPhase();
            onPhaseBegin();
        }
        checkStateBasedEffects();
        return true;
    }

    // this is a hack for the setup game state mode, do not use outside of devSetupGameState code
    // as it avoids calling any of the phase effects that may be necessary in a less enforced context
    public final void devModeSet(final PhaseType phase0, final Player player0, boolean endCombat, int cturn) {
        if (phase0 != null) {
            setPhase(phase0);
        }
        if (player0 != null) {
            setPlayerTurn(player0);
        }
        turn = cturn;

        game.fireEvent(new GameEventTurnPhase(playerTurn, phase, "dev"));
        if (endCombat) {
            endCombat(); // not-null can be created only when declare attackers phase begins
        }
    }
    public final void devModeSet(final PhaseType phase0, final Player player0) {
        devModeSet(phase0, player0, true, 1);
    }

    public final void devModeSet(final PhaseType phase0, final Player player0, int cturn) {
        devModeSet(phase0, player0, true, cturn);
    }

    public final void devModeSet(final PhaseType phase0, final Player player0, boolean endCombat) {
        devModeSet(phase0, player0, endCombat, 0);
    }

    public final void endCombatPhaseByEffect() {
        game.getAction().checkStateEffects(true);
        setPhase(PhaseType.COMBAT_END);
        advanceToNextPhase();
    }

    public final void endTurnByEffect() {
        extraPhases.clear();
        setPhase(PhaseType.CLEANUP);
        onPhaseBegin();
    }

    public int getPlanarDiceSpecialActionThisTurn() {
        return planarDiceSpecialActionThisTurn;
    }
    public void incPlanarDiceSpecialActionThisTurn() {
        planarDiceSpecialActionThisTurn++;
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
        for (Player player : game.getPlayers()) {
            player.resetCombatantsThisCombat();
        }
        game.getEndOfCombat().executeUntil();
        game.getEndOfCombat().executeUntilEndOfPhase(playerTurn);
        if (inCombat()) {
            combat.endCombat();
            combat = null;
        }
        game.updateCombatForView();
    }

    public void setCombat(Combat combat) {
        this.combat = combat;
    }

    /**
     * returns the continuous extra turn count
     * @param p
     * @return int
     */
    public int getExtraTurnForPlayer(final Player p) {
        if (this.extraTurns.isEmpty() || this.extraTurns.size() < 2) {
            return 0;
        }

        int count = 0;
        // skip the first element
        for (final ExtraTurn et : extraTurns.subList(1, extraTurns.size())) {
            if (!et.getPlayer().equals(p)) {
                break;
            }
            count += 1;
        }
        return count;
    }
}
