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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityPanharmonicon;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import forge.util.Visitor;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

public class TriggerHandler {
    private final List<TriggerType> suppressedModes = Collections.synchronizedList(new ArrayList<>());
    private final List<Trigger> activeTriggers = Collections.synchronizedList(new ArrayList<>());

    private final List<Trigger> delayedTriggers = Collections.synchronizedList(new ArrayList<>());
    private final List<Trigger> thisTurnDelayedTriggers = Collections.synchronizedList(new ArrayList<>());
    private final ListMultimap<Player, Trigger> playerDefinedDelayedTriggers = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private final List<TriggerWaiting> waitingTriggers = Collections.synchronizedList(new ArrayList<>());
    private final Game game;

    public TriggerHandler(final Game gameState) {
        game = gameState;
    }

    public final boolean hasDelayedTriggers() {
        return !delayedTriggers.isEmpty();
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

    public final void handlePlayerDefinedDelTriggers(final Player player) {
        delayedTriggers.addAll(playerDefinedDelayedTriggers.removeAll(player));
    }

    public final void suppressMode(final TriggerType mode) {
        suppressedModes.add(mode);
    }

    public final void setSuppressAllTriggers(final boolean suppress) {
        for (TriggerType t : TriggerType.values()) {
            if (suppress) {
                suppressMode(t);
            } else {
                clearSuppression(t);
            }
        }
    }

    public final void clearSuppression(final TriggerType mode) {
        suppressedModes.remove(mode);
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
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", host.getName()).withData("Trigger", trigParse).build()
            );
            //rethrow
            throw new RuntimeException("Error in Trigger for Card: " + host.getName(), e);
        }
    }

    public static Trigger parseTrigger(final Map<String, String> mapParams, final Card host, final boolean intrinsic, final IHasSVars sVarHolder) {
        Trigger ret = null;

        try {
            final TriggerType type = TriggerType.smartValueOf(mapParams.get("Mode"));
            ret = type.createTrigger(mapParams, host, intrinsic);
            if (sVarHolder != null) {
                ret.ensureAbility(sVarHolder);

                if (sVarHolder instanceof CardState) {
                    ret.setCardState((CardState)sVarHolder);
                } else if (sVarHolder instanceof CardTraitBase) {
                    ret.setCardState(((CardTraitBase)sVarHolder).getCardState());
                }
            }
        } catch (Exception e) {
            String msg = "TriggerHandler:parseTrigger failed to parse";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", host.getName()).withData("Params", mapParams.toString()).build()
            );
            //rethrow
            throw new RuntimeException("Error in Trigger for Card: " + host.getName(), e);
        }

        return ret;
    }

    private static Map<String, String> parseParams(final String trigParse) {
        if (trigParse.length() == 0) {
            throw new RuntimeException("TriggerFactory : registerTrigger -- trigParse too short");
        }

        return FileSection.parseToMap(trigParse, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    private void collectTriggerForWaiting() {
        for (final TriggerWaiting wt : waitingTriggers) {
            if (wt.getTriggers() != null)
                continue;

            wt.setTriggers(getActiveTrigger(wt.getMode(), wt.getParams()));
        }
    }

    private void buildActiveTrigger() {
        activeTriggers.clear();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(Card c) {
                for (final Trigger t : c.getTriggers()) {
                    if (isTriggerActive(t)) {
                        activeTriggers.add(t);
                    }
                }
                return true;
            }
        });
    }

    public final void resetActiveTriggers() {
        resetActiveTriggers(true);
    }

    public final void resetActiveTriggers(boolean collect) {
        if (collect) {
            collectTriggerForWaiting();
        }
        buildActiveTrigger();
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
            if (
                    TriggerType.Exploited.equals(t.getMode()) ||
                    TriggerType.Sacrificed.equals(t.getMode()) ||
                    TriggerType.Destroyed.equals(t.getMode()) ||
                    (TriggerType.ChangesZone.equals(t.getMode()) && "Battlefield".equals(t.getParam("Origin")))) { // TODO needs additional logic in case origin=Any
                registerOneTrigger(t);
            }
        }
    }

    public final boolean registerOneTrigger(final Trigger t) {
        if (isTriggerActive(t)) {
            activeTriggers.add(t);
            return true;
        }
        return false;
    }

    public final void runTrigger(final TriggerType mode, final Map<AbilityKey, Object> runParams, boolean holdTrigger) {
        if (suppressedModes.contains(mode)) {
            return;
        }

        if (mode == TriggerType.Always) {
            runStateTrigger(runParams);
        } else if (game.getStack().isFrozen() || holdTrigger) {
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
        final List<TriggerWaiting> waiting = new ArrayList<>(waitingTriggers);
        waitingTriggers.clear();
        if (waiting.isEmpty()) {
            return false;
        }

        boolean haveWaiting = false;
        for (final TriggerWaiting wt : waiting) {
            haveWaiting |= runWaitingTrigger(wt);
        }

        return haveWaiting;
    }

    private boolean runWaitingTrigger(final TriggerWaiting wt) {
        final TriggerType mode = wt.getMode();
        final Map<AbilityKey, Object> runParams = wt.getParams();

        final Player playerAP = game.getPhaseHandler().getPlayerTurn();
        if (playerAP == null) {
            // This should only happen outside of games, so it's safe to abort.
            return false;
        }

        // Copy triggers here, so things can be modified just in case
        final List<Trigger> delayedTriggersWorkingCopy = new ArrayList<>(delayedTriggers);

        boolean checkStatics = false;

        // Static triggers
        for (final Trigger t : Lists.newArrayList(activeTriggers)) {
            if (t.isStatic() && canRunTrigger(t, mode, runParams)) {
                runSingleTrigger(t, runParams);

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

        // AP
        checkStatics |= runNonStaticTriggersForPlayer(playerAP, wt, delayedTriggersWorkingCopy);

        // NAPs
        for (final Player nap : game.getNonactivePlayers()) {
            checkStatics |= runNonStaticTriggersForPlayer(nap, wt, delayedTriggersWorkingCopy);
        }
        return checkStatics;
    }

    public void clearWaitingTriggers() {
        waitingTriggers.clear();
    }

    public void resetTurnTriggerState()    {
        for(final Trigger t : activeTriggers) {
            t.resetTurnState();
        }
        for (final Trigger t : delayedTriggers) {
            t.resetTurnState();
        }
    }

    private boolean runNonStaticTriggersForPlayer(final Player player, final TriggerWaiting wt, final List<Trigger> delayedTriggersWorkingCopy ) {

        final TriggerType mode = wt.getMode();
        final Map<AbilityKey, Object> runParams = wt.getParams();
        final List<Trigger> triggers = wt.getTriggers() != null ? wt.getTriggers() : activeTriggers;

        boolean checkStatics = false;

        for (final Trigger t : triggers) {
            if (!t.isStatic() && t.getHostCard().getController().equals(player) && canRunTrigger(t, mode, runParams)) {
                int x = 1 + StaticAbilityPanharmonicon.handlePanharmonicon(game, t, runParams);

                for (int i = 0; i < x; ++i) {
                    runSingleTrigger(t, runParams);
                }
                checkStatics = true;
            }
        }

        for (final Trigger deltrig : delayedTriggersWorkingCopy) {
            if (deltrig.getHostCard().getController().equals(player)) {
                if (isTriggerActive(deltrig) && canRunTrigger(deltrig, mode, runParams)) {
                    runSingleTrigger(deltrig, runParams);
                    delayedTriggers.remove(deltrig);
                }
            }
        }
        return checkStatics;
    }

    private boolean isTriggerActive(final Trigger regtrig) {
        if (!regtrig.phasesCheck(game)) {
            return false; // It's not the right phase to go off.
        }

        if (regtrig.getHostCard().isFaceDown() && regtrig.isIntrinsic()) {
            return false; // Morphed cards only have pumped triggers go off.
        }
        if (TriggerType.Always.equals(regtrig.getMode())) {
            if (game.getStack().hasStateTrigger(regtrig.getId())) {
                return false; // State triggers that are already on the stack
                // don't trigger again.
            }
        }

        if (regtrig.isSuppressed()) {
            return false; // Trigger removed by effect
        }

        if (!regtrig.zonesCheck(game.getZoneOf(regtrig.getHostCard()))) {
            return false; // Host card isn't where it needs to be.
        }

        for (Trigger t : this.activeTriggers) {
            // If an ID that matches this ID is already active, don't add it
            if (regtrig.getId() == t.getId()) {
                return false;
            }
        }

        // Check if a trigger with the same ID is already in activeTriggers
        return true;
    }

    private boolean canRunTrigger(final Trigger regtrig, final TriggerType mode, final Map<AbilityKey, Object> runParams) {
        if (regtrig.getMode() != mode) {
            return false; // Not the right mode.
        }

        /* this trigger can only be activated once per turn, verify it hasn't already run */
        if (regtrig.hasParam("ActivationLimit")) {
            if (regtrig.getActivationsThisTurn() >= Integer.parseInt(regtrig.getParam("ActivationLimit"))) {
                return false;
            }
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
        if (regtrig.isSuppressed()) {
            return false; // Trigger removed by effect
        }

        if (TriggerType.Always.equals(regtrig.getMode())) {
            if (game.getStack().hasStateTrigger(regtrig.getId())) {
                return false; // State triggers that are already on the stack
                // don't trigger again.
            }
        }

        // Torpor Orb check
        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)
                && !regtrig.isStatic() && mode.equals(TriggerType.ChangesZone)) {
            if (runParams.get(AbilityKey.Destination) instanceof String) {
                final String dest = (String) runParams.get(AbilityKey.Destination);
                if (dest.equals("Battlefield") && runParams.get(AbilityKey.Card) instanceof Card) {
                    final Card card = (Card) runParams.get(AbilityKey.Card);
                    if (card.isCreature()) {
                        return false;
                    }
                }
            }
        } // Torpor Orb check

        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureDyingTriggers)
                && !regtrig.isStatic() && mode.equals(TriggerType.ChangesZone)) {
            if (runParams.get(AbilityKey.Destination) instanceof String && runParams.get(AbilityKey.Origin) instanceof String) {
                final String dest = (String) runParams.get(AbilityKey.Destination);
                final String origin = (String) runParams.get(AbilityKey.Origin);
                if (dest.equals("Graveyard") && origin.equals("Battlefield") && runParams.get(AbilityKey.Card) instanceof Card) {
                    final Card card = (Card) runParams.get(AbilityKey.Card);
                    if (card.isCreature()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void runSingleTrigger(final Trigger regtrig, final Map<AbilityKey, Object> runParams) {
        // If the runParams contains MergedCards, it is called from GameAction.changeZone()
        if (runParams.get(AbilityKey.MergedCards) != null) {
            // Check if the trigger cares the origin is from battlefield
            Card original = (Card) runParams.get(AbilityKey.Card);
            CardCollection mergedCards = (CardCollection) runParams.get(AbilityKey.MergedCards);
            mergedCards.set(mergedCards.indexOf(original), original);
            Map<AbilityKey, Object> newParams = AbilityKey.mapFromCard(original);
            newParams.putAll(runParams);
            if ("Battlefield".equals(regtrig.getParam("Origin"))) {
                // If yes, only trigger once
                newParams.put(AbilityKey.Card, mergedCards);
                runSingleTriggerInternal(regtrig, newParams);
            } else {
                // Else, trigger for each merged components
                for (final Card c : mergedCards) {
                    newParams.put(AbilityKey.Card, c);
                    runSingleTriggerInternal(regtrig, newParams);
                }
            }
        } else {
            runSingleTriggerInternal(regtrig, runParams);
        }
    }

    // Checks if the conditions are right for a single trigger to go off, and
    // runs it if so.
    // Return true if the trigger went off, false otherwise.
    private void runSingleTriggerInternal(final Trigger regtrig, final Map<AbilityKey, Object> runParams) {

        // All tests passed, execute ability.
        if (regtrig instanceof TriggerTapsForMana) {
            final SpellAbility abMana = (SpellAbility) runParams.get(AbilityKey.AbilityMana);
            if (null != abMana && null != abMana.getManaPart()) {
                abMana.setUndoable(false);
            }
        }

        SpellAbility sa = null;
        Card host = regtrig.getHostCard();

        sa = regtrig.getOverridingAbility();
        if (sa == null) {
            if (!regtrig.hasParam("Execute")) {
                sa = new SpellAbility.EmptySa(host);
            }
            else {
                String name = regtrig.getParam("Execute");
                if (!host.getCurrentState().hasSVar(name)) {
                    System.err.println("Warning: tried to run a trigger for card " + host + " referencing a SVar " + name + " not present on the current state " + host.getCurrentState() + ". Aborting trigger execution to prevent a crash.");
                    return;
                }

                sa = AbilityFactory.getAbility(host, name);
                // need to set as Overriding Abiltiy so it can be copied better
                regtrig.setOverridingAbility(sa);
            }
            sa.setActivatingPlayer(host.getController());

            if (regtrig.isIntrinsic()) {
                sa.setIntrinsic(true);
                sa.changeText();
            }
        } else {
            // need to copy the SA because of TriggeringObjects
            sa = sa.copy(host, host.getController(), false);
        }

        sa.setLastStateBattlefield(game.getLastStateBattlefield());
        sa.setLastStateGraveyard(game.getLastStateGraveyard());

        sa.setTrigger(regtrig);
        sa.setSourceTrigger(regtrig.getId());
        regtrig.setTriggeringObjects(sa, runParams);
        TriggerAbilityTriggered.addTriggeringObject(regtrig, sa, runParams);
        sa.setTriggerRemembered(regtrig.getTriggerRemembered());

        if (regtrig.hasParam("TriggerController")) {
            Player p = AbilityUtils.getDefinedPlayers(regtrig.getHostCard(), regtrig.getParam("TriggerController"), sa).get(0);
            sa.setActivatingPlayer(p);
        }

        if (regtrig.hasParam("RememberController")) {
            host.addRemembered(sa.getActivatingPlayer());
        }

        if (regtrig.hasParam("RememberTriggeringCard")) {
            Card triggeredCard = ((Card) sa.getTriggeringObject(AbilityKey.Card));
            host.addRemembered(triggeredCard);
        }

        if (regtrig.hasParam("RememberTriggeringCards")) {
            CardCollection triggeringCards = ((CardCollection) sa.getTriggeringObject(AbilityKey.Cards));
            for (Card c : triggeringCards) {
                host.addRemembered(c);
            }
        }

        if (regtrig.hasParam("RememberKey")) {
            host.addRemembered(runParams.get(AbilityKey.fromString(regtrig.getParam("RememberKey"))));
        }

        sa.setStackDescription(sa.toString());
        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            if (!CharmEffect.makeChoices(sa)) {
                // 603.3c If no mode is chosen, the ability is removed from the stack.
                return;
            }
        }

        Player decider = null;
        boolean isMandatory = false;
        if (regtrig.hasParam("OptionalDecider")) {
            sa.setOptionalTrigger(true);
            decider = AbilityUtils.getDefinedPlayers(host, regtrig.getParam("OptionalDecider"), sa).get(0);
        }
        else if (sa instanceof AbilitySub || !sa.hasParam("Cost") || sa.getParam("Cost").equals("0")) {
            isMandatory = true;
        }
        else { // triggers with a cost can't be mandatory
            sa.setOptionalTrigger(true);
            decider = sa.getActivatingPlayer();
        }

        final WrappedAbility wrapperAbility = new WrappedAbility(regtrig, sa, decider);
        //wrapperAbility.setDescription(wrapperAbility.getStackDescription());
        //wrapperAbility.setDescription(wrapperAbility.toUnsuppressedString());

        wrapperAbility.setLastStateBattlefield(game.getLastStateBattlefield());
        if (regtrig.isStatic()) {
            wrapperAbility.getActivatingPlayer().getController().playTrigger(host, wrapperAbility, isMandatory);
        }
        else {
            game.getStack().addSimultaneousStackEntry(wrapperAbility);
        }

        regtrig.triggerRun();

        if (regtrig.hasParam("OneOff")) {
            if (regtrig.getHostCard().isImmutable()) {
                Player p = regtrig.getHostCard().getController();
                p.getZone(ZoneType.Command).remove(regtrig.getHostCard());
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
            if (t.getHostCard().getOwner().equals(p)) {
                delayedTriggers.remove(t);
            }
        }
        // run all ChangesZone
        runWaitingTriggers();
    }
}
