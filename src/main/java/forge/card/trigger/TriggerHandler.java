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
package forge.card.trigger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;

import forge.CardLists;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameState;
import forge.game.GlobalRuleChange;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
//import forge.util.TextUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * TriggerHandler class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */

public class TriggerHandler {
    private final ArrayList<TriggerType> suppressedModes = new ArrayList<TriggerType>();

    private final ArrayList<Trigger> delayedTriggers = new ArrayList<Trigger>();
    private final List<TriggerWaiting> waitingTriggers = new ArrayList<TriggerWaiting>();

    /**
     * Clean up temporary triggers.
     */
    public final void cleanUpTemporaryTriggers() {
        final List<Card> absolutelyAllCards = Singletons.getModel().getGame().getCardsInGame();
        for (final Card c : absolutelyAllCards) {
            for (int i = 0; i < c.getTriggers().size(); i++) {
                if (c.getTriggers().get(i).isTemporary()) {
                    c.getTriggers().remove(i);
                    i--;
                }
            }
        }
        for (final Card c : absolutelyAllCards) {
            for (int i = 0; i < c.getTriggers().size(); i++) {
                c.getTriggers().get(i).setTemporarilySuppressed(false);
            }
        }

    }

    /**
     * <p>
     * registerDelayedTrigger.
     * </p>
     * 
     * @param trig
     *            a {@link forge.card.trigger.Trigger} object.
     */
    public final void registerDelayedTrigger(final Trigger trig) {
        this.delayedTriggers.add(trig);
    }

    /**
     * <p>
     * clearDelayedTrigger.
     * </p>
     */
    public final void clearDelayedTrigger() {
        this.delayedTriggers.clear();
    }

    /**
     * <p>
     * clearDelayedTrigger.
     * </p>
     * @param card
     *            a card object.
     */
    public final void clearDelayedTrigger(Card card) {
        ArrayList<Trigger> deltrigs = new ArrayList<Trigger>(this.delayedTriggers);

        for (Trigger trigger : deltrigs) {
            if (trigger.getHostCard().equals(card)) {
                this.delayedTriggers.remove(trigger);
            }
        }
    }


    /**
     * <p>
     * suppressMode.
     * </p>
     * 
     * @param mode
     *            a {@link java.lang.String} object.
     */
    public final void suppressMode(final TriggerType mode) {
        this.suppressedModes.add(mode);
    }

    /**
     * <p>
     * clearSuppression.
     * </p>
     * 
     * @param mode
     *            a {@link java.lang.String} object.
     */
    public final void clearSuppression(final TriggerType mode) {
        this.suppressedModes.remove(mode);
    }

    /**
     * <p>
     * parseTrigger.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param trigParse
     *            a {@link java.lang.String} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public static Trigger parseTrigger(final String name, final String trigParse, final Card host,
            final boolean intrinsic) {
        final Trigger ret = TriggerHandler.parseTrigger(trigParse, host, intrinsic);
        ret.setName(name);
        return ret;
    }

    /**
     * <p>
     * parseTrigger.
     * </p>
     * 
     * @param trigParse
     *            a {@link java.lang.String} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public static Trigger parseTrigger(final String trigParse, final Card host, final boolean intrinsic) {
        final HashMap<String, String> mapParams = TriggerHandler.parseParams(trigParse);
        return TriggerHandler.parseTrigger(mapParams, host, intrinsic);
    }

    /**
     * <p>
     * parseTrigger.
     * </p>
     * 
     * @param mapParams
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
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

    /**
     * <p>
     * parseParams.
     * </p>
     * 
     * @param trigParse
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.HashMap} object.
     */
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

    /**
     * <p>
     * runTrigger.
     * </p>
     * 
     * @param mode
     *            a {@link java.lang.String} object.
     * @param runParams
     *            a {@link java.util.Map} object.
     * @param forceHeldTriggers Force certain triggers to be added the waitingTriggers if stack isnt frozen
     */
    public final void runTrigger(final TriggerType mode, final Map<String, Object> runParams, boolean holdTrigger) {
        if (this.suppressedModes.contains(mode)) {
            return;
        }

        final GameState game = Singletons.getModel().getGame();

        //runWaitingTrigger(new TriggerWaiting(mode, runParams));
        
        if (game.getStack().isFrozen() || holdTrigger) {
            waitingTriggers.add(new TriggerWaiting(mode, runParams));
        } else {
            runWaitingTrigger(new TriggerWaiting(mode, runParams), true);
        }
        // Tell auto stop to stop
    }

    public final boolean runWaitingTriggers(boolean runStaticEffects) {
        ArrayList<TriggerWaiting> waiting = new ArrayList<TriggerWaiting>(waitingTriggers);
        waitingTriggers.clear();
        if (waiting.isEmpty()) {
            return false;
        }

        boolean haveWaiting = false;
        for (TriggerWaiting wt : waiting) {
            haveWaiting |= runWaitingTrigger(wt, runStaticEffects);
        }

        return haveWaiting;
    }

    public final boolean runWaitingTrigger(TriggerWaiting wt, boolean runStaticEffects) {
        final TriggerType mode = wt.getMode();
        final Map<String, Object> runParams = wt.getParams();
        final GameState game = Singletons.getModel().getGame();

        final Player playerAP = game.getPhaseHandler().getPlayerTurn();
        if (playerAP == null) {
            // This should only happen outside of games, so it's safe to just
            // abort.
            return false;
        }

        // This is done to allow the list of triggers to be modified while
        // triggers are running.
        final ArrayList<Trigger> delayedTriggersWorkingCopy = new ArrayList<Trigger>(this.delayedTriggers);
        List<Card> allCards = game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
        allCards.addAll(game.getCardsIn(ZoneType.Stack));
        boolean checkStatics = false;

        // Static triggers
        for (final Card c : allCards) {
            ArrayList<Trigger> triggers = new ArrayList<Trigger>();
            triggers.addAll(c.getTriggers());
            for (final Trigger t : triggers) {
                if (t.isStatic()) {
                    checkStatics |= this.runSingleTrigger(t, mode, runParams);
                }
            }
        }

        if (runParams.containsKey("Destination")) {
            // Check static abilities when a card enters the battlefield
            String type = (String) runParams.get("Destination");
            checkStatics |= type.equals("Battlefield");
        }

        // AP
        allCards = playerAP.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
        allCards.addAll(CardLists.filterControlledBy(game.getCardsIn(ZoneType.Stack), playerAP));
        // add cards that move to hidden zones
        if (runParams.containsKey("Destination") && runParams.containsKey("Card")) {
            Card card = (Card) runParams.get("Card");
            if (playerAP.equals(card.getController()) && !allCards.contains(card)
                    && (game.getZoneOf(card) == null || game.getZoneOf(card).getZoneType().isHidden())) {
                allCards.add(card);
            }
        }
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (!t.isStatic()) {
                    checkStatics |= this.runSingleTrigger(t, mode, runParams);
                }
            }
        }
        for (Trigger deltrig : delayedTriggersWorkingCopy) {
            if (deltrig.getHostCard().getController().equals(playerAP)) {
                if (this.runSingleTrigger(deltrig, mode, runParams)) {
                    this.delayedTriggers.remove(deltrig);
                }
            }
        }

        // NAPs

        for (Player nap : game.getPlayers()) {

            if (nap.equals(playerAP)) {
                continue;
            }

            allCards = nap.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
            allCards.addAll(CardLists.filterControlledBy(game.getCardsIn(ZoneType.Stack), nap));
            // add cards that move to hidden zones
            if (runParams.containsKey("Destination") && runParams.containsKey("Card")) {
                Card card = (Card) runParams.get("Card");
                if (!playerAP.equals(card.getController()) && !allCards.contains(card)
                        && (game.getZoneOf(card) == null || game.getZoneOf(card).getZoneType().isHidden())) {
                    allCards.add(card);
                }
            }
            for (final Card c : allCards) {
                for (final Trigger t : c.getTriggers()) {
                    if (!t.isStatic()) {
                        checkStatics |= this.runSingleTrigger(t, mode, runParams);
                    }
                }
            }
            for (Trigger deltrig : delayedTriggersWorkingCopy) {
                if (deltrig.getHostCard().getController().equals(nap)) {
                    if (this.runSingleTrigger(deltrig, mode, runParams)) {
                        this.delayedTriggers.remove(deltrig);
                    }
                }
            }
        }

        return checkStatics;
    }

    // Checks if the conditions are right for a single trigger to go off, and
    // runs it if so.
    // Return true if the trigger went off, false otherwise.
    /**
     * <p>
     * runSingleTrigger.
     * </p>
     * 
     * @param regtrig
     *            a {@link forge.card.trigger.Trigger} object.
     * @param mode
     *            a {@link java.lang.String} object.
     * @param runParams
     *            a {@link java.util.HashMap} object.
     * @return false if trigger is not happening.
     */
    private boolean runSingleTrigger(final Trigger regtrig, final TriggerType mode, final Map<String, Object> runParams) {
        final Map<String, String> triggerParams = regtrig.getMapParams();
        final GameState game = Singletons.getModel().getGame();

        if (regtrig.getMode() != mode) {
            return false; // Not the right mode.
        }

        if (!regtrig.phasesCheck()) {
            return false; // It's not the right phase to go off.
        }

        if (!regtrig.requirementsCheck(runParams)) {
            return false; // Conditions aren't right.
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

        if (!regtrig.performTest(runParams)) {
            return false; // Test failed.
        }
        if (regtrig.isSuppressed()) {
            return false; // Trigger removed by effect
        }
        if (!regtrig.zonesCheck(game.getZoneOf(regtrig.getHostCard()))) {
            return false; // Host card isn't where it needs to be.
        }

        // Torpor Orb check
        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)
                && !regtrig.isStatic() && mode.equals(TriggerType.ChangesZone)) {
            if (runParams.get("Destination") instanceof String) {
                String dest = (String) runParams.get("Destination");
                if (dest.equals("Battlefield") && runParams.get("Card") instanceof Card) {
                    Card card = (Card) runParams.get("Card");
                    if (card.isCreature()) {
                        return false;
                    }
                }
            }
        } // Torpor Orb check

        // Any trigger should cause the phase not to skip
        for (Player p : Singletons.getModel().getGame().getPlayers()) {
            p.getController().autoPassCancel();
        }

        regtrig.setRunParams(runParams);

        // All tests passed, execute ability.
        if (regtrig instanceof TriggerTapsForMana) {
            final SpellAbility abMana = (SpellAbility) runParams.get("AbilityMana");
            if (null != abMana && null != abMana.getManaPart()) {
                abMana.setUndoable(false);
            }
        }

        final AbilityFactory abilityFactory = new AbilityFactory();

        SpellAbility sa = null;
        Card host = game.getCardState(regtrig.getHostCard());

        if (host == null) {
            host = regtrig.getHostCard();
        }

        sa = regtrig.getOverridingAbility();
        if (sa == null) {
            if (!triggerParams.containsKey("Execute")) {
                sa = new Ability(regtrig.getHostCard(), SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                    }
                };
            } else {
                sa = abilityFactory.getAbility(host.getSVar(triggerParams.get("Execute")), host);
            }
        }
        sa.setTrigger(true);
        sa.setSourceTrigger(regtrig.getId());
        regtrig.setTriggeringObjects(sa);
        if (regtrig.getStoredTriggeredObjects() != null) {
            sa.setAllTriggeringObjects(regtrig.getStoredTriggeredObjects());
        }

        sa.setActivatingPlayer(host.getController());
        if (triggerParams.containsKey("TriggerController")) {
            Player p = AbilityUtils.getDefinedPlayers(regtrig.getHostCard(), triggerParams.get("TriggerController"), sa).get(0);
            sa.setActivatingPlayer(p);
        }

        sa.setStackDescription(sa.toString());
        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        Player decider = null;
        boolean mand = false;
        if (triggerParams.containsKey("OptionalDecider")) {
            sa.setOptionalTrigger(true);
            mand = false;
            decider = AbilityUtils.getDefinedPlayers(host, triggerParams.get("OptionalDecider"), sa).get(0);
        } else {
            mand = true;

            SpellAbility ability = sa;
            while (ability != null) {
                final Target tgt = ability.getTarget();

                if (tgt != null) {
                    tgt.setMandatory(true);
                }
                ability = ability.getSubAbility();
            }
        }
        final boolean isMandatory = mand;

        WrappedAbility wrapperAbility = new WrappedAbility(regtrig, sa, decider);
        wrapperAbility.setTrigger(true);
        wrapperAbility.setMandatory(isMandatory);
        wrapperAbility.setDescription(wrapperAbility.getStackDescription());

        if (regtrig.isStatic()) {
            if (wrapperAbility.getActivatingPlayer().isHuman()) {
                game.getActionPlay().playSpellAbilityNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, false);
            } else {
                wrapperAbility.doTrigger(isMandatory, (AIPlayer)wrapperAbility.getActivatingPlayer());
                ComputerUtil.playNoStack((AIPlayer)wrapperAbility.getActivatingPlayer(), wrapperAbility, game);
            }
        } else {
            game.getStack().addSimultaneousStackEntry(wrapperAbility);
        }
        regtrig.setTriggeredSA(wrapperAbility);
        return true;
    }

    private final ArrayList<Integer> triggersAlwaysAccept = new ArrayList<Integer>();
    private final ArrayList<Integer> triggersAlwaysDecline = new ArrayList<Integer>();

    /**
     * Sets the always accept trigger.
     * 
     * @param trigID
     *            the new always accept trigger
     */
    public final void setAlwaysAcceptTrigger(final int trigID) {
        if (this.triggersAlwaysDecline.contains(trigID)) {
            this.triggersAlwaysDecline.remove((Object) trigID);
        }

        if (!this.triggersAlwaysAccept.contains(trigID)) {
            this.triggersAlwaysAccept.add(trigID);
        }
    }

    /**
     * Sets the always decline trigger.
     * 
     * @param trigID
     *            the new always decline trigger
     */
    public final void setAlwaysDeclineTrigger(final int trigID) {
        if (this.triggersAlwaysAccept.contains(trigID)) {
            this.triggersAlwaysAccept.remove((Object) trigID);
        }

        if (!this.triggersAlwaysDecline.contains(trigID)) {
            this.triggersAlwaysDecline.add(trigID);
        }
    }

    /**
     * Sets the always ask trigger.
     * 
     * @param trigID
     *            the new always ask trigger
     */
    public final void setAlwaysAskTrigger(final int trigID) {
        this.triggersAlwaysAccept.remove((Object) trigID);
        this.triggersAlwaysDecline.remove((Object) trigID);
    }

    /**
     * Checks if is always accepted.
     * 
     * @param trigID
     *            the trig id
     * @return true, if is always accepted
     */
    public final boolean isAlwaysAccepted(final int trigID) {
        return this.triggersAlwaysAccept.contains(trigID);
    }

    /**
     * Checks if is always declined.
     * 
     * @param trigID
     *            the trig id
     * @return true, if is always declined
     */
    public final boolean isAlwaysDeclined(final int trigID) {
        return this.triggersAlwaysDecline.contains(trigID);
    }

    /**
     * Clear trigger settings.
     */
    public final void clearTriggerSettings() {
        this.triggersAlwaysAccept.clear();
        this.triggersAlwaysDecline.clear();
    }
}
