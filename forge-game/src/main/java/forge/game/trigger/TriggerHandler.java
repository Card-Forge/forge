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
import forge.game.io.GameStateDeserializer;
import forge.game.io.GameStateSerializer;
import forge.game.io.IGameStateObject;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.*;

public class TriggerHandler implements IGameStateObject {
    private final ArrayList<TriggerType> suppressedModes = new ArrayList<TriggerType>();

    private final ArrayList<Trigger> delayedTriggers = new ArrayList<Trigger>();
    private final List<TriggerWaiting> waitingTriggers = new ArrayList<TriggerWaiting>();
    private final Game game;

    /**
     * TODO: Write javadoc for Constructor.
     * @param gameState
     */
    public TriggerHandler(Game gameState) {
        game = gameState;
    }

    public final void cleanUpTemporaryTriggers() {
        final List<Card> absolutelyAllCards = game.getCardsInGame();
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

    public final void registerDelayedTrigger(final Trigger trig) {
        this.delayedTriggers.add(trig);
    }

    public final void clearDelayedTrigger() {
        this.delayedTriggers.clear();
    }

    public final void clearDelayedTrigger(Card card) {
        ArrayList<Trigger> deltrigs = new ArrayList<Trigger>(this.delayedTriggers);

        for (Trigger trigger : deltrigs) {
            if (trigger.getHostCard().equals(card)) {
                this.delayedTriggers.remove(trigger);
            }
        }
    }


    public final void suppressMode(final TriggerType mode) {
        this.suppressedModes.add(mode);
    }

    public final void clearSuppression(final TriggerType mode) {
        this.suppressedModes.remove(mode);
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

    public final void runTrigger(final TriggerType mode, final Map<String, Object> runParams, boolean holdTrigger) {
        if (this.suppressedModes.contains(mode)) {
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
        List<Card> allCards = new ArrayList<Card>(game.getCardsIn(ZoneType.listValueOf("Battlefield,Command")));
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (canRunTrigger(t, TriggerType.Always, runParams)) {
                    this.runSingleTrigger(t, runParams);
                    checkStatics = true;
                }
            }
        }
        return checkStatics;
    }

    public final boolean runWaitingTriggers() {
        ArrayList<TriggerWaiting> waiting = new ArrayList<TriggerWaiting>(waitingTriggers);
        waitingTriggers.clear();
        if (waiting.isEmpty()) {
            return false;
        }

        boolean haveWaiting = false;
        for (TriggerWaiting wt : waiting) {
            haveWaiting |= runWaitingTrigger(wt);
        }

        return haveWaiting;
    }

    public final boolean runWaitingTrigger(TriggerWaiting wt) {
        final TriggerType mode = wt.getMode();
        final Map<String, Object> runParams = wt.getParams();

        final Player playerAP = game.getPhaseHandler().getPlayerTurn();
        if (playerAP == null) {
            // This should only happen outside of games, so it's safe to just
            // abort.
            return false;
        }

        // This is done to allow the list of triggers to be modified while
        // triggers are running.
        final ArrayList<Trigger> delayedTriggersWorkingCopy = new ArrayList<Trigger>(this.delayedTriggers);
        List<Card> allCards = new ArrayList<Card>();
        for(Player p : game.getPlayers())
        {
            allCards.addAll(p.getAllCards());
        }
        //allCards.addAll(game.getCardsIn(ZoneType.Stack));
        boolean checkStatics = false;

        // Static triggers
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (t.isStatic() && canRunTrigger(t, mode, runParams)) {
                    this.runSingleTrigger(t, runParams);
                    checkStatics = true;
                }
            }
        }

        if (runParams.containsKey("Destination")) {
            // Check static abilities when a card enters the battlefield
            String type = (String) runParams.get("Destination");
            checkStatics |= type.equals("Battlefield");
        }

        // AP 
        checkStatics |= runNonStaticTriggersForPlayer(playerAP, mode, runParams, delayedTriggersWorkingCopy);

        // NAPs
        for (Player nap : game.getPlayers()) {
            if (!nap.equals(playerAP))
                checkStatics |= runNonStaticTriggersForPlayer(nap, mode, runParams, delayedTriggersWorkingCopy);
        }

        return checkStatics;
    }

    private boolean runNonStaticTriggersForPlayer(final Player player, final TriggerType mode, 
            final Map<String, Object> runParams, final ArrayList<Trigger> delayedTriggersWorkingCopy ) {
        
        boolean checkStatics = false;
        List<Card> playerCards = player.getAllCards();
        
        // check LKI copies for triggers
        if (runParams.containsKey("Destination") && !ZoneType.Battlefield.name().equals(runParams.get("Destination"))
                && runParams.containsKey("Card")) {
            Card card = (Card) runParams.get("Card");
            if (card.getController() == player) {
                for (final Trigger t : card.getTriggers()) {
                    if (!t.isStatic() && (card.isCloned() || !t.isIntrinsic()) 
                            && canRunTrigger(t, mode, runParams)) {
                        this.runSingleTrigger(t, runParams);
                        checkStatics = true;
                    }
                }
            }
        }

        for (final Card c : playerCards) {
            for (final Trigger t : c.getTriggers()) {
                if (!t.isStatic() && canRunTrigger(t, mode, runParams)) {
                    this.runSingleTrigger(t, runParams);
                    checkStatics = true;
                }
            }
        }

        for (Trigger deltrig : delayedTriggersWorkingCopy) {
            if (deltrig.getHostCard().getController().equals(player)) {
                if (this.canRunTrigger(deltrig, mode, runParams)) {
                    this.runSingleTrigger(deltrig, runParams);
                    this.delayedTriggers.remove(deltrig);
                }
            }
        }
        return checkStatics;
    }

    private boolean canRunTrigger(final Trigger regtrig, final TriggerType mode, final Map<String, Object> runParams) {
        if (regtrig.getMode() != mode) {
            return false; // Not the right mode.
        }

        if (!regtrig.phasesCheck(game)) {
            return false; // It's not the right phase to go off.
        }

        if (!regtrig.requirementsCheck(game, runParams)) {
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

        sa = regtrig.getOverridingAbility();
        if (sa == null) {
            if (!triggerParams.containsKey("Execute")) {
                sa = new Ability(regtrig.getHostCard(), ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                    }
                };
            } else {
                sa = AbilityFactory.getAbility(host.getSVar(triggerParams.get("Execute")), host);
            }
        }
        host = game.getCardState(regtrig.getHostCard());
        sa.setHostCard(host);
        sa.setTrigger(true);
        sa.setSourceTrigger(regtrig.getId());
        regtrig.setTriggeringObjects(sa);
        sa.setTriggerRemembered(regtrig.getTriggerRemembered());
        if (regtrig.getStoredTriggeredObjects() != null) {
            sa.setAllTriggeringObjects(regtrig.getStoredTriggeredObjects());
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
            mand = false;
            decider = AbilityUtils.getDefinedPlayers(host, triggerParams.get("OptionalDecider"), sa).get(0);
        } else {
            mand = true;

            SpellAbility ability = sa;
            while (ability != null) {
                final TargetRestrictions tgt = ability.getTargetRestrictions();

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
            wrapperAbility.getActivatingPlayer().getController().playTrigger(host, wrapperAbility, isMandatory);
        } else {
            game.getStack().addSimultaneousStackEntry(wrapperAbility);
        }
        regtrig.setTriggeredSA(wrapperAbility);
        
        if (triggerParams.containsKey("OneOff")) {
            if (regtrig.getHostCard().isImmutable()) {
                Player p = regtrig.getHostCard().getController();
                p.getZone(ZoneType.Command).remove(regtrig.getHostCard());
            } else {
                regtrig.getHostCard().getTriggers().remove(regtrig);
            }
        }

    }

    @Override
    public void loadState(GameStateDeserializer gsd) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void saveState(GameStateSerializer gss) {
        // TODO Auto-generated method stub
        
    }
}
