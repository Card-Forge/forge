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
package forge.game.trigger;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.*;

import forge.game.CardTraitBase;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityDisableTriggers;
import forge.game.staticability.StaticAbilityPanharmonicon;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

public class TriggerHandler {
    private final Set<TriggerType> suppressedModes = Collections.synchronizedSet(EnumSet.noneOf(TriggerType.class));
    private boolean allSuppressed = false;
    private final List<Trigger> activeTriggers = Collections.synchronizedList(new ArrayList<>());

    private final List<Trigger> delayedTriggers = Collections.synchronizedList(new ArrayList<>());
    private final List<Trigger> thisTurnDelayedTriggers = Collections.synchronizedList(new ArrayList<>());
    private final ListMultimap<Player, Trigger> playerDefinedDelayedTriggers = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private final List<TriggerWaiting> waitingTriggers = Collections.synchronizedList(new ArrayList<>());
    private final Game game;

    public TriggerHandler(final Game gameState) {
        game = gameState;
    }

    public final void registerDelayedTrigger(final Trigger trig) {
        delayedTriggers.add(trig);
    }

    public final void clearDelayedTrigger() {
        delayedTriggers.clear();
    }

    public final void registerThisTurnDelayedTrigger(final Trigger trig) {
        thisTurnDelayedTriggers.add(trig);
        delayedTriggers.add(trig);
    }

    public final void clearThisTurnDelayedTrigger() {
        delayedTriggers.removeAll(thisTurnDelayedTriggers);
        thisTurnDelayedTriggers.clear();
    }

    public final void clearDelayedTrigger(final Card card) {
        final List<Trigger> deltrigs = new ArrayList<>(delayedTriggers);

        for (final Trigger trigger : deltrigs) {
            if (trigger.getHostCard().equals(card)) {
                delayedTriggers.remove(trigger);
            }
        }
    }

    public final void registerPlayerDefinedDelayedTrigger(final Player player, final Trigger trig) {
        playerDefinedDelayedTriggers.put(player, trig);
    }

    public final void clearPlayerDefinedDelayedTrigger() {
        playerDefinedDelayedTriggers.clear();
    }

    public final void handlePlayerDefinedDelTriggers(final Player player) {
        final List<Trigger> playerTriggers = playerDefinedDelayedTriggers.removeAll(player);
        playerTriggers.stream().filter(CardTraitPredicates.hasParam("ThisTurn")).forEach(thisTurnDelayedTriggers::add);
        delayedTriggers.addAll(playerTriggers);
    }

    public final void suppressMode(final TriggerType mode) {
        suppressedModes.add(mode);
    }

    public final void setSuppressAllTriggers(final boolean suppress) {
        allSuppressed = suppress;
    }

    public final void clearSuppression(final TriggerType mode) {
        suppressedModes.remove(mode);
    }
    public boolean isTriggerSuppressed(final TriggerType mode) {
        return allSuppressed || suppressedModes.contains(mode);
    }

    public static Trigger parseTrigger(final String trigParse, final Card host, final boolean intrinsic) {
        return parseTrigger(trigParse, host, intrinsic, host.getCurrentState());
    }

    public static Trigger parseTrigger(final String trigParse, final Card host, final boolean intrinsic, final IHasSVars sVarHolder) {
        try {
            final Map<String, String> mapParams = TriggerHandler.parseParams(trigParse);
            return TriggerHandler.parseTrigger(mapParams, host, intrinsic, sVarHolder);
        } catch (Exception e) {
            String msg = "TriggerHandler:parseTrigger failed to parse";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", host.getName());
            bread.setData("Trigger", trigParse);
            Sentry.addBreadcrumb(bread);

            //rethrow
            throw new RuntimeException("Error in Trigger for Card: " + host.getName(), e);
        }
    }

    public static Trigger parseTrigger(final Map<String, String> mapParams, final Card host, final boolean intrinsic, final IHasSVars sVarHolder) {
        Trigger result;

        try {
            final TriggerType type = TriggerType.smartValueOf(mapParams.get("Mode"));
            result = type.createTrigger(mapParams, host, intrinsic);
            if (sVarHolder != null) {
                result.ensureAbility(sVarHolder);

                if (sVarHolder instanceof CardState) {
                    result.setCardState((CardState)sVarHolder);
                } else if (sVarHolder instanceof CardTraitBase) {
                    result.setCardState(((CardTraitBase)sVarHolder).getCardState());
                }
            }
        } catch (Exception e) {
            String msg = "TriggerHandler:parseTrigger failed to parse";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", host.getName());
            bread.setData("Params", mapParams.toString());
            Sentry.addBreadcrumb(bread);

            //rethrow
            throw new RuntimeException("Error in Trigger for Card: " + host.getName(), e);
        }

        return result;
    }

    private static Map<String, String> parseParams(final String trigParse) {
        if (trigParse.length() == 0) {
            throw new RuntimeException("TriggerFactory : registerTrigger -- trigParse too short");
        }

        return FileSection.parseToMap(trigParse, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    public void collectTriggerForWaiting() {
        for (final TriggerWaiting wt : waitingTriggers) {
            if (wt.getTriggers() != null)
                continue;

            // TODO we don't seem to handle Static ones from this,
            // so they shouldn't be checked for performance in the first place
            wt.setTriggers(getActiveTrigger(wt.getMode(), wt.getParams()));
        }
    }

    public final void resetActiveTriggers() {
        resetActiveTriggers(true, null);
    }
    public final void resetActiveTriggers(boolean collect, CardCollectionView lastStateBattlefield) {
        if (collect) {
            collectTriggerForWaiting();
        }
        activeTriggers.clear();
        game.forEachCardInGame(c -> {
            for (final Trigger t : c.getTriggers()) {
                if (c.isInPlay() && lastStateBattlefield != null && !lastStateBattlefield.contains(c) && looksBackInTime(t)) {
                    continue;
                }
                if (isTriggerActive(t)) {
                    activeTriggers.add(t);
                }
            }
            return true;
        });
    }

    public final void clearActiveTriggers(final Card c, Zone zoneFrom) {
        final List<Trigger> toBeRemoved = Lists.newArrayList();

        for (Trigger t : activeTriggers) {
            // Clear if no ZoneFrom, or not coming from the TriggerZone
            if (c.getId() == t.getHostCard().getId()) {
                if (!c.getTriggers().contains(t) || !t.zonesCheck(zoneFrom))
                    toBeRemoved.add(t);
            }
        }

        activeTriggers.removeAll(toBeRemoved);
    }

    public final void registerActiveTrigger(final Card c, final boolean onlyExtrinsic) {
        for (final Trigger t : c.getTriggers()) {
            if (!onlyExtrinsic || c.isCloned() || !t.isIntrinsic() || TriggerType.Always.equals(t.getMode())) {
                registerOneTrigger(t);
            }
        }
    }

    public final void registerActiveLTBTrigger(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (looksBackInTime(t)) {
                registerOneTrigger(t);
            }
        }
    }

    private boolean looksBackInTime(Trigger t) {
        return TriggerType.Exploited.equals(t.getMode()) ||
                TriggerType.Destroyed.equals(t.getMode()) ||
                TriggerType.Sacrificed.equals(t.getMode()) || TriggerType.SacrificedOnce.equals(t.getMode()) ||
                ((TriggerType.ChangesZone.equals(t.getMode()) || TriggerType.ChangesZoneAll.equals(t.getMode()))
                        && (StringUtils.contains(t.getParam("Origin"), "Battlefield") ||
                        StringUtils.containsAny(t.getParam("Destination"), "Library", "Hand")));
    }

    public final boolean registerOneTrigger(final Trigger t) {
        if (isTriggerActive(t)) {
            activeTriggers.add(t);
            return true;
        }
        return false;
    }

    public final void runTrigger(final TriggerType mode, final Map<AbilityKey, Object> runParams, boolean holdTrigger) {
        if (isTriggerSuppressed(mode)) {
            return;
        }

        // too many waiting triggers might cause OutOfMemory exception
        // such high amount usually happens from looping on one type:
        // e.g. Heroes' Bane counters ability
        // we can just run further triggers directly, side effects are highly unlikely
        // (could also make this depend on Runtime.getRuntime().freeMemory()
        // - but probably overkill)
        boolean canWait = waitingTriggers.size() < 9999;
        if (mode == TriggerType.Always) {
            runStateTrigger(runParams);
        } else if (canWait && (game.getStack().isFrozen() || holdTrigger) && mode != TriggerType.TapsForMana && mode != TriggerType.ManaAdded) {
            waitingTriggers.add(new TriggerWaiting(mode, runParams));
        } else {
            runWaitingTrigger(new TriggerWaiting(mode, runParams));
        }
    }

    private void runStateTrigger(final Map<AbilityKey, Object> runParams) {
        for (final Trigger t: Lists.newArrayList(activeTriggers)) {
            if (canRunTrigger(t, TriggerType.Always, runParams)) {
                runSingleTrigger(t, runParams);
            }
        }
    }

    public final boolean runWaitingTriggers() {
        if (waitingTriggers.isEmpty()) {
            return false;
        }
        final List<TriggerWaiting> waiting = new ArrayList<>(waitingTriggers);
        waitingTriggers.clear();

        boolean haveWaiting = false;
        for (final TriggerWaiting wt : waiting) {
            haveWaiting |= runWaitingTrigger(wt);
        }

        return haveWaiting;
    }

    private boolean runWaitingTrigger(final TriggerWaiting wt) {
        final Player playerAP = game.getPhaseHandler().getPlayerTurn();
        if (playerAP == null) {
            // This should only happen outside of games, so it's safe to abort.
            return false;
        }

        final TriggerType mode = wt.getMode();
        final Map<AbilityKey, Object> runParams = wt.getParams();
        // Copy triggers here, so things can be modified just in case
        final List<Trigger> delayedTriggersWorkingCopy = new ArrayList<>(delayedTriggers);
        boolean checkStatics = false;

        // Static ones should happen first
        for (final Trigger t : Lists.newArrayList(activeTriggers)) {
            if (t.isStatic() && canRunTrigger(t, mode, runParams)) {
                int trigAmt = 1 + StaticAbilityPanharmonicon.handlePanharmonicon(game, t, runParams);
                for (int i = 0; i < trigAmt; ++i) {
                    runSingleTrigger(t, runParams);
                }
                checkStatics = true;
            }
        }

        if (runParams.containsKey(AbilityKey.Destination)) {
            // Check static abilities when a card enters the battlefield
            if (runParams.get(AbilityKey.Destination) instanceof String) {
                final String type = (String) runParams.get(AbilityKey.Destination);
                checkStatics |= type.equals("Battlefield");
            } else {
                final ZoneType zone = (ZoneType) runParams.get(AbilityKey.Destination);
                if (zone != null) {
                    checkStatics |= zone.equals(ZoneType.Battlefield);
                }
            }
        }

        final boolean wasCollected = wt.getTriggers() != null;
        final Iterable<Trigger> triggers = wasCollected ? wt.getTriggers() : activeTriggers;

        // the trigger will be ordered later in MagicStack
        for (final Trigger t : triggers) {
            if (!t.isStatic() && (wasCollected || canRunTrigger(t, mode, runParams))) {
                if (wasCollected && !t.checkActivationLimit()) {
                    continue;
                }
                int trigAmt = 1 + StaticAbilityPanharmonicon.handlePanharmonicon(game, t, runParams);
                for (int i = 0; i < trigAmt; ++i) {
                    runSingleTrigger(t, runParams, wt.getController(t));
                }
                checkStatics = true;
            }
        }

        for (final Trigger deltrig : delayedTriggersWorkingCopy) {
            if (isTriggerActive(deltrig) && canRunTrigger(deltrig, mode, runParams)) {
                delayedTriggers.remove(deltrig);
                runSingleTrigger(deltrig, runParams);
            }
        }

        return checkStatics;
    }

    public void clearWaitingTriggers() {
        waitingTriggers.clear();
    }

    private boolean isTriggerActive(final Trigger regtrig) {
        if (!regtrig.phasesCheck(game)) {
            return false; // It's not the right phase to go off.
        }

        if (regtrig.isSuppressed()) {
            return false; // Trigger removed by effect
        }

        if (TriggerType.Always.equals(regtrig.getMode()) && game.getStack().hasStateTrigger(regtrig.getId())) {
            return false; // State triggers that are already on the stack
            // don't trigger again.
        }

        // do not check delayed
        if (regtrig.getSpawningAbility() == null && !regtrig.zonesCheck(game.getZoneOf(regtrig.getHostCard()))) {
            return false; // Host card isn't where it needs to be.
        }

        for (Trigger t : this.activeTriggers) {
            // If an ID that matches this ID is already active, don't add it
            if (regtrig.getId() == t.getId()) {
                return false;
            }
        }

        return true;
    }

    private boolean canRunTrigger(final Trigger regtrig, final TriggerType mode, final Map<AbilityKey, Object> runParams) {
        if (regtrig.getMode() != mode) {
            return false; // Not the right mode.
        }

        if (regtrig.isSuppressed()) {
            return false; // Trigger removed by effect
        }

        /* this trigger can only be activated once per turn, verify it hasn't already run */
        if (!regtrig.checkActivationLimit()) {
            return false;
        }

        if (!regtrig.requirementsCheck(game)) {
            return false; // Conditions aren't right.
        }

        if (!regtrig.meetsRequirementsOnTriggeredObjects(game, runParams)) {
            return false; // Conditions aren't right.
        }

        if (!regtrig.performTest(runParams)) {
            return false; // Test failed.
        }

        if (TriggerType.Always.equals(regtrig.getMode()) && game.getStack().hasStateTrigger(regtrig.getId())) {
            return false; // State triggers that are already on the stack
            // don't trigger again.
        }

        // check if any static abilities are disabling the trigger (Torpor Orb and the like)
        if (!regtrig.isStatic() && StaticAbilityDisableTriggers.disabled(game, regtrig, runParams)) {
            return false;
        }

        return true;
    }

    private void runSingleTrigger(final Trigger regtrig, final Map<AbilityKey, Object> runParams) {
        runSingleTrigger(regtrig, runParams, null);
    }
    private void runSingleTrigger(final Trigger regtrig, final Map<AbilityKey, Object> runParams, Player controller) {
        if (controller == null) {
            controller = regtrig.getHostCard().getController();
        }
        // If the runParams contains MergedCards, it is called from GameAction.changeZone()
        if (runParams.get(AbilityKey.MergedCards) != null) {
            // Check if the trigger cares the origin is from battlefield
            Card original = (Card) runParams.get(AbilityKey.Card);
            CardCollection mergedCards = (CardCollection) runParams.get(AbilityKey.MergedCards);
            mergedCards.set(mergedCards.indexOf(original), original);
            Map<AbilityKey, Object> newParams = AbilityKey.newMap(runParams);
            if ("Battlefield".equals(regtrig.getParam("Origin"))) {
                // If yes, only trigger once
                newParams.put(AbilityKey.Card, mergedCards);
                runSingleTriggerInternal(regtrig, newParams, controller);
            } else {
                // Else, trigger for each merged components
                for (final Card c : mergedCards) {
                    newParams.put(AbilityKey.Card, c);
                    runSingleTriggerInternal(regtrig, newParams, controller);
                }
            }
        } else {
            runSingleTriggerInternal(regtrig, runParams, controller);
        }
    }

    // Checks if the conditions are right for a single trigger to go off, and
    // runs it if so.
    // Return true if the trigger went off, false otherwise.
    private void runSingleTriggerInternal(final Trigger regtrig, final Map<AbilityKey, Object> runParams, Player controller) {
        // All tests passed, execute ability.

        adjustUndoStack(regtrig, runParams);

        Card host = regtrig.getHostCard();
        SpellAbility sa = regtrig.getOverridingAbility();
        if (sa == null) {
            if (!regtrig.hasParam("Execute")) {
                sa = new SpellAbility.EmptySa(host);
            } else {
                String name = regtrig.getParam("Execute");
                if (!host.getCurrentState().hasSVar(name)) {
                    System.err.println("Warning: tried to run a trigger for card " + host + " referencing a SVar " + name + " not present on the current state " + host.getCurrentState() + ". Aborting trigger execution to prevent a crash.");
                    return;
                }

                sa = AbilityFactory.getAbility(host, name);
                // need to set as Overriding Ability so it can be copied better
                regtrig.setOverridingAbility(sa);
            }
            sa.setActivatingPlayer(controller);

            if (regtrig.isIntrinsic()) {
                sa.setIntrinsic(true);
                sa.changeText();
            }
        } else {
            if (regtrig.getSpawningAbility() != null) {
                controller = regtrig.getSpawningAbility().getActivatingPlayer();
            }
            // need to copy the SA because of TriggeringObjects
            sa = sa.copy(host, controller, false, true);
        }

        sa.setTrigger(regtrig);
        regtrig.setTriggeringObjects(sa, runParams);

        if (regtrig.hasParam("TriggerController")) {
            Player p = AbilityUtils.getDefinedPlayers(host, regtrig.getParam("TriggerController"), sa).get(0);
            sa.setActivatingPlayer(p);
        }

        if (!sa.getActivatingPlayer().isInGame()) {
            return;
        }

        sa.setStackDescription(sa.toString());

        Player decider = null;
        boolean isMandatory = false;
        if (regtrig.hasParam("OptionalDecider")) {
            sa.setOptionalTrigger(true);
            decider = AbilityUtils.getDefinedPlayers(host, regtrig.getParam("OptionalDecider"), sa).get(0);
        }
        else if (sa instanceof AbilitySub || !sa.hasParam("Cost") || (sa.getPayCosts() != null && sa.getPayCosts().isMandatory()) || sa.getParam("Cost").equals("0")) {
            isMandatory = true;
        } else { // triggers with a cost can't be mandatory
            sa.setOptionalTrigger(true);
            decider = sa.getActivatingPlayer();
        }

        final WrappedAbility wrapperAbility = new WrappedAbility(regtrig, sa, decider);
        //wrapperAbility.setDescription(wrapperAbility.getStackDescription());
        //wrapperAbility.setDescription(wrapperAbility.toUnsuppressedString());

        if (regtrig.isStatic()) {
            if (wrapperAbility.getActivatingPlayer().getController().playTrigger(host, wrapperAbility, isMandatory)) {
                final Map<AbilityKey, Object> staticParams = AbilityKey.mapFromCard(host);
                staticParams.put(AbilityKey.SpellAbility, sa);
                game.getTriggerHandler().runTrigger(TriggerType.AbilityResolves, staticParams, false);
            }
        } else {
            game.getStack().addSimultaneousStackEntry(wrapperAbility);
            game.getTriggerHandler().runTrigger(TriggerType.AbilityTriggered, TriggerAbilityTriggered.getRunParams(regtrig, wrapperAbility, runParams), false);
        }

        regtrig.triggerRun();

        boolean removeBoon = host.isBoon();
        if (regtrig.hasParam("BoonAmount")) {
            int x = AbilityUtils.calculateAmount(host, regtrig.getParam("BoonAmount"), wrapperAbility);
            int y = host.getAbilityActivatedThisGame(regtrig.getOverridingAbility());
            if (y < x) removeBoon = false;
        }
        if (regtrig.hasParam("OneOff") && host.isImmutable() || removeBoon) {
            host.getController().getZone(ZoneType.Command).remove(host);
        }
    }

    private void adjustUndoStack(Trigger regtrig, Map<AbilityKey, Object> runParams) {
        if (regtrig instanceof TriggerTapsForMana || regtrig instanceof TriggerManaAdded) {
            final SpellAbility abMana = (SpellAbility) runParams.get(AbilityKey.AbilityMana);
            if (null != abMana && null != abMana.getManaPart()) {
                abMana.setUndoable(false);
            }
        }
        else if (regtrig instanceof TriggerSpellAbilityCastOrCopy || regtrig instanceof TriggerAbilityResolves) {
            final SpellAbility abMana = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
            if (null != abMana && null != abMana.getManaPart()) {
                abMana.setUndoable(false);
            }
        }
        else if (regtrig instanceof TriggerTaps || regtrig instanceof TriggerUntaps) {
            final Card c = (Card) runParams.get(AbilityKey.Card);
            for (SpellAbility sa : game.getStack().filterUndoStackByHost(c)) {
                sa.setUndoable(false);
            }
        }  
        else if (regtrig instanceof TriggerTapAll) {
            final Iterable<Card> cards = (Iterable<Card>) runParams.get(AbilityKey.Cards);
            for (Card c : cards) {
                for (SpellAbility sa : game.getStack().filterUndoStackByHost(c)) {
                    sa.setUndoable(false);
                }
            }
        }
        else if (regtrig instanceof TriggerUntapAll) {
            final Map<Player, CardCollection> map = (Map<Player, CardCollection>) runParams.get(AbilityKey.Map);
            for (Card c : Iterables.concat(map.values())) {
                for (SpellAbility sa : game.getStack().filterUndoStackByHost(c)) {
                    sa.setUndoable(false);
                }
            }
        } 
    }

    public List<Trigger> getActiveTrigger(final TriggerType mode, final Map<AbilityKey, Object> runParams) {
        List<Trigger> trigger = Lists.newArrayList();
        for (final Trigger t : activeTriggers) {
            if (canRunTrigger(t, mode, runParams)) {
                trigger.add(t);
            }
        }
        return trigger;
    }

    public void onPlayerLost(Player p) {
        List<Trigger> lost = new ArrayList<>(delayedTriggers);
        for (Trigger t : lost) {
            // CR 800.4d trigger controller lost game
            if (p.equals(t.getSpawningAbility().getActivatingPlayer())) {
                delayedTriggers.remove(t);
            }
        }
        // run all ChangesZone
        runWaitingTriggers();
    }
}
