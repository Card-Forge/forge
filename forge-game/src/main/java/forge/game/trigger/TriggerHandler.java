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

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Visitor;

import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

public class TriggerHandler {
    private final List<TriggerType> suppressedModes = Collections.synchronizedList(new ArrayList<TriggerType>());
    private final List<Trigger> activeTriggers = Collections.synchronizedList(new ArrayList<Trigger>());

    private final List<Trigger> delayedTriggers = Collections.synchronizedList(new ArrayList<Trigger>());
    private final List<Trigger> thisTurnDelayedTriggers = Collections.synchronizedList(new ArrayList<Trigger>());
    private final ListMultimap<Player, Trigger> playerDefinedDelayedTriggers = Multimaps.synchronizedListMultimap(ArrayListMultimap.<Player, Trigger>create());
    private final List<TriggerWaiting> waitingTriggers = Collections.synchronizedList(new ArrayList<TriggerWaiting>());
    private final Game game;

    public TriggerHandler(final Game gameState) {
        game = gameState;
    }

    public final void cleanUpTemporaryTriggers() {
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(Card c) {
                boolean changed = false;
                for (int i = 0; i < c.getTriggers().size(); i++) {
                    Trigger trigger = c.getTriggers().get(i);
                    if (trigger.isTemporary()) {
                        c.removeTrigger(trigger);
                        changed = true;
                        i--;
                    }
                }
                if (changed) {
                    c.updateStateForView();
                }
            }
        });
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(Card c) {
                boolean changed = false;
                for (int i = 0; i < c.getTriggers().size(); i++) {
                    if (c.getTriggers().get(i).isSuppressed()) {
                        c.getTriggers().get(i).setTemporarilySuppressed(false);
                        changed = true;
                    }
                }
                if (changed) {
                    c.updateStateForView();
                }
            }
        });
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
        final List<Trigger> deltrigs = new ArrayList<Trigger>(delayedTriggers);

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

    public final void clearSuppression(final TriggerType mode) {
        suppressedModes.remove(mode);
    }

    public static Trigger parseTrigger(final String trigParse, final Card host, final boolean intrinsic) {
        final HashMap<String, String> mapParams = TriggerHandler.parseParams(trigParse);
        return TriggerHandler.parseTrigger(mapParams, host, intrinsic);
    }

    public static Trigger parseTrigger(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        Trigger ret = null;

        final TriggerType type = TriggerType.smartValueOf(mapParams.get("Mode"));
        ret = type.createTrigger(mapParams, host, intrinsic);

        String triggerZones = mapParams.get("TriggerZones");
        if (null != triggerZones) {
            ret.setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(triggerZones)));
        }

        String triggerPhases = mapParams.get("Phase");
        if (null != triggerPhases) {
            ret.setTriggerPhases(PhaseType.parseRange(triggerPhases));
        }

        return ret;
    }

    private static HashMap<String, String> parseParams(final String trigParse) {
        final HashMap<String, String> mapParams = new HashMap<String, String>();

        if (trigParse.length() == 0) {
            throw new RuntimeException("TriggerFactory : registerTrigger -- trigParse too short");
        }

        final String[] params = trigParse.split("\\|");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }

        for (final String param : params) {
            final String[] splitParam = param.split("\\$");
            for (int i = 0; i < splitParam.length; i++) {
                splitParam[i] = splitParam[i].trim();
            }

            if (splitParam.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("TriggerFactory Parsing Error in registerTrigger() : Split length of ");
                sb.append(param).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParams.put(splitParam[0], splitParam[1]);
        }

        return mapParams;
    }

    public final void resetActiveTriggers() {
        activeTriggers.clear();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(Card c) {
                for (final Trigger t : c.getTriggers()) {
                    if (isTriggerActive(t)) {
                        activeTriggers.add(t);
                    }
                }
            }
        });
    }

    public final void clearInstrinsicActiveTriggers(final Card c, Zone zoneFrom) {
        final Iterator<Trigger> itr = activeTriggers.iterator();
        Trigger t;
        final List<Trigger> toBeRemoved = new ArrayList<Trigger>();

        while(itr.hasNext()) {
            t = itr.next();

            // Clear if no ZoneFrom, or not coming from the TriggerZone
            if (c.getId() == t.getHostCard().getId() && t.isIntrinsic()) {
                if (!t.zonesCheck(zoneFrom))
                    toBeRemoved.add(t);
            }
        }

        for (final Trigger removed : toBeRemoved) {
            // This line was not removing the correct trigger for cloned tokens
            activeTriggers.remove(removed);
        }
    }

    public final void registerActiveTrigger(final Card c, final boolean onlyExtrinsic) {
        for (final Trigger t : c.getTriggers()) {
            if (!onlyExtrinsic || c.isCloned() || !t.isIntrinsic() || t instanceof TriggerAlways) {
                if (isTriggerActive(t)) {
                    activeTriggers.add(t);
                }
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

    public final void runTrigger(final TriggerType mode, final Map<String, Object> runParams, boolean holdTrigger) {
        if (suppressedModes.contains(mode)) {
            return;
        }

        //runWaitingTrigger(new TriggerWaiting(mode, runParams));
        if (mode == TriggerType.Always) {
            runStateTrigger(runParams);
        } else if (game.getStack().isFrozen() || holdTrigger) {
            waitingTriggers.add(new TriggerWaiting(mode, runParams));
        } else {
            runWaitingTrigger(new TriggerWaiting(mode, runParams));
        }
        // Tell auto stop to stop
    }

    public final boolean runStateTrigger(Map<String, Object> runParams) {
        boolean checkStatics = false;
        // only cards in play can run state triggers

        for (final Trigger t: activeTriggers) {
            if (canRunTrigger(t, TriggerType.Always, runParams)) {
                runSingleTrigger(t, runParams);
                checkStatics = true;
            }
        }
        return checkStatics;
    }

    public final boolean runWaitingTriggers() {
        final List<TriggerWaiting> waiting = new ArrayList<TriggerWaiting>(waitingTriggers);
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

    public final boolean runWaitingTrigger(final TriggerWaiting wt) {
        final TriggerType mode = wt.getMode();
        final Map<String, Object> runParams = wt.getParams();

        final Player playerAP = game.getPhaseHandler().getPlayerTurn();
        if (playerAP == null) {
            // This should only happen outside of games, so it's safe to abort.
            return false;
        }

        // Copy triggers here, so things can be modified just in case
        final List<Trigger> delayedTriggersWorkingCopy = new ArrayList<Trigger>(delayedTriggers);

        boolean checkStatics = false;

        // Static triggers
        for (final Trigger t : Lists.newArrayList(activeTriggers)) {
            if (t.isStatic() && canRunTrigger(t, mode, runParams)) {
                runSingleTrigger(t, runParams);
                checkStatics = true;
            }
        }

        if (runParams.containsKey("Destination")) {
            // Check static abilities when a card enters the battlefield
            final String type = (String) runParams.get("Destination");
            checkStatics |= type.equals("Battlefield");
        }

        // AP 
        checkStatics |= runNonStaticTriggersForPlayer(playerAP, mode, runParams, delayedTriggersWorkingCopy);

        // NAPs
        for (final Player nap : game.getNonactivePlayers()) {
            checkStatics |= runNonStaticTriggersForPlayer(nap, mode, runParams, delayedTriggersWorkingCopy);
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

    private boolean runNonStaticTriggersForPlayer(final Player player, final TriggerType mode, 
            final Map<String, Object> runParams, final List<Trigger> delayedTriggersWorkingCopy ) {

        Card card = null;
        boolean checkStatics = false;

        for (final Trigger t : activeTriggers) {
            if (!t.isStatic() && t.getHostCard().getController().equals(player) && canRunTrigger(t, mode, runParams)) {
                if (runParams.containsKey("Card") && runParams.get("Card") instanceof Card) {
                    card = (Card) runParams.get("Card");
                    if (runParams.containsKey("Destination") && !ZoneType.Battlefield.name().equals(runParams.get("Destination"))) {
                        card = CardUtil.getLKICopy(card);
                        if (card.isCloned() || !t.isIntrinsic()) {
                            runParams.put("Card", card);
                        }
                    }
                }

                runSingleTrigger(t, runParams);
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
        if (regtrig instanceof TriggerAlways) {
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

    private boolean canRunTrigger(final Trigger regtrig, final TriggerType mode, final Map<String, Object> runParams) {
        if (regtrig.getMode() != mode) {
            return false; // Not the right mode.
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

        if (regtrig instanceof TriggerAlways) {
            if (game.getStack().hasStateTrigger(regtrig.getId())) {
                return false; // State triggers that are already on the stack
                // don't trigger again.
            }
        }

        // Torpor Orb check
        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)
                && !regtrig.isStatic() && mode.equals(TriggerType.ChangesZone)) {
            if (runParams.get("Destination") instanceof String) {
                final String dest = (String) runParams.get("Destination");
                if (dest.equals("Battlefield") && runParams.get("Card") instanceof Card) {
                    final Card card = (Card) runParams.get("Card");
                    if (card.isCreature()) {
                        return false;
                    }
                }
            }
        } // Torpor Orb check
        return true;
    }

    // Checks if the conditions are right for a single trigger to go off, and
    // runs it if so.
    // Return true if the trigger went off, false otherwise.
    private void runSingleTrigger(final Trigger regtrig, final Map<String, Object> runParams) {
        final Map<String, String> triggerParams = regtrig.getMapParams();

        regtrig.setRunParams(runParams);

        // All tests passed, execute ability.
        if (regtrig instanceof TriggerTapsForMana) {
            final SpellAbility abMana = (SpellAbility) runParams.get("AbilityMana");
            if (null != abMana && null != abMana.getManaPart()) {
                abMana.setUndoable(false);
            }
        }

        SpellAbility sa = null;
        Card host = regtrig.getHostCard();
        final Card trigCard = regtrig.getRunParams().containsKey("Card") ? (Card)regtrig.getRunParams().get("Card") : null;

        if (trigCard != null && (host.getId() == trigCard.getId())) {
            host = trigCard;
        }
        else {
        	// get CardState does not work for transformed cards
        	// also its about LKI
        	if (host.isInZone(ZoneType.Battlefield) || !host.hasAlternateState()) {
        		host = game.getCardState(host);
        	}
        }

        sa = regtrig.getOverridingAbility();
        if (sa == null) {
            if (!triggerParams.containsKey("Execute")) {
                sa = new Ability(host, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                    }
                };
            }
            else {
                sa = AbilityFactory.getAbility(host.getSVar(triggerParams.get("Execute")), host);
            }
        }

        sa.setHostCard(host);
        sa.setTrigger(true);
        sa.setSourceTrigger(regtrig.getId());
        regtrig.setTriggeringObjects(sa);
        sa.setTriggerRemembered(regtrig.getTriggerRemembered());
        if (regtrig.getStoredTriggeredObjects() != null) {
            sa.setTriggeringObjects(regtrig.getStoredTriggeredObjects());
        }
        if (sa.getActivatingPlayer() == null) { // overriding delayed trigger should have set activator
            sa.setActivatingPlayer(host.getController());
        }
        if (triggerParams.containsKey("TriggerController")) {
            Player p = AbilityUtils.getDefinedPlayers(regtrig.getHostCard(), triggerParams.get("TriggerController"), sa).get(0);
            sa.setActivatingPlayer(p);
        }

        if (triggerParams.containsKey("RememberController")) {
            host.addRemembered(sa.getActivatingPlayer());
        }

        if (regtrig.isIntrinsic()) {
            sa.setIntrinsic(true);
            sa.changeText();
        }

        sa.setStackDescription(sa.toString());
        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        Player decider = null;
        boolean mand = false;
        if (triggerParams.containsKey("OptionalDecider")) {
            sa.setOptionalTrigger(true);
            decider = AbilityUtils.getDefinedPlayers(host, triggerParams.get("OptionalDecider"), sa).get(0);
        }
        else if (sa instanceof AbilitySub || !sa.hasParam("Cost") || sa.getParam("Cost").equals("0")) {
            mand = true;
        }
        else { // triggers with a cost can't be mandatory
            sa.setOptionalTrigger(true);
            decider = sa.getActivatingPlayer();
        }

        SpellAbility ability = sa;
        while (ability != null) {
            final TargetRestrictions tgt = ability.getTargetRestrictions();

            if (tgt != null) {
                tgt.setMandatory(true);
            }
            ability = ability.getSubAbility();
        }
        final boolean isMandatory = mand;

        final WrappedAbility wrapperAbility = new WrappedAbility(regtrig, sa, decider);
        wrapperAbility.setTrigger(true);
        wrapperAbility.setMandatory(isMandatory);
        //wrapperAbility.setDescription(wrapperAbility.getStackDescription());
        wrapperAbility.setDescription(wrapperAbility.toUnsuppressedString());

        if (regtrig.isStatic()) {
            wrapperAbility.getActivatingPlayer().getController().playTrigger(host, wrapperAbility, isMandatory);
        }
        else {
            game.getStack().addSimultaneousStackEntry(wrapperAbility);
        }
        regtrig.setTriggeredSA(wrapperAbility);

        regtrig.triggerRun();

        if (triggerParams.containsKey("OneOff")) {
            if (regtrig.getHostCard().isImmutable()) {
                Player p = regtrig.getHostCard().getController();
                p.getZone(ZoneType.Command).remove(regtrig.getHostCard());
            }
            else {
                regtrig.getHostCard().removeTrigger(regtrig);
            }
        }
    }
}
