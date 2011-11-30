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
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandArgs;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactoryCharm;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.gui.input.Input;

/**
 * <p>
 * TriggerHandler class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerHandler {

    private final ArrayList<String> suppressedModes = new ArrayList<String>();

    private final ArrayList<Trigger> delayedTriggers = new ArrayList<Trigger>();

    /**
     * Clean up temporary triggers.
     */
    public final void cleanUpTemporaryTriggers() {
        final CardList absolutelyAllCards = new CardList();
        absolutelyAllCards.addAll(AllZone.getHumanPlayer().getAllCards());
        absolutelyAllCards.addAll(AllZone.getComputerPlayer().getAllCards());

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
     * suppressMode.
     * </p>
     * 
     * @param mode
     *            a {@link java.lang.String} object.
     */
    public final void suppressMode(final String mode) {
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
    public final void clearSuppression(final String mode) {
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
    public static Trigger parseTrigger(final HashMap<String, String> mapParams, final Card host, final boolean intrinsic) {
        Trigger ret = null;

        final String mode = mapParams.get("Mode");
        if (mode.equals("AbilityCast")) {
            ret = new TriggerSpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("Always")) {
            ret = new TriggerAlways(mapParams, host, intrinsic);
        } else if (mode.equals("AttackerBlocked")) {
            ret = new TriggerAttackerBlocked(mapParams, host, intrinsic);
        } else if (mode.equals("AttackersDeclared")) {
            ret = new TriggerAttackersDeclared(mapParams, host, intrinsic);
        } else if (mode.equals("AttackerUnblocked")) {
            ret = new TriggerAttackerUnblocked(mapParams, host, intrinsic);
        } else if (mode.equals("Attacks")) {
            ret = new TriggerAttacks(mapParams, host, intrinsic);
        } else if (mode.equals("BecomesTarget")) {
            ret = new TriggerBecomesTarget(mapParams, host, intrinsic);
        } else if (mode.equals("Blocks")) {
            ret = new TriggerBlocks(mapParams, host, intrinsic);
        } else if (mode.equals("Championed")) {
            ret = new TriggerChampioned(mapParams, host, intrinsic);
        } else if (mode.equals("ChangesZone")) {
            ret = new TriggerChangesZone(mapParams, host, intrinsic);
        } else if (mode.equals("Clashed")) {
            ret = new TriggerClashed(mapParams, host, intrinsic);
        } else if (mode.equals("CounterAdded")) {
            ret = new TriggerCounterAdded(mapParams, host, intrinsic);
        } else if (mode.equals("Cycled")) {
            ret = new TriggerCycled(mapParams, host, intrinsic);
        } else if (mode.equals("DamageDone")) {
            ret = new TriggerDamageDone(mapParams, host, intrinsic);
        } else if (mode.equals("Discarded")) {
            ret = new TriggerDiscarded(mapParams, host, intrinsic);
        } else if (mode.equals("Drawn")) {
            ret = new TriggerDrawn(mapParams, host, intrinsic);
        } else if (mode.equals("LandPlayed")) {
            ret = new TriggerLandPlayed(mapParams, host, intrinsic);
        } else if (mode.equals("LifeGained")) {
            ret = new TriggerLifeGained(mapParams, host, intrinsic);
        } else if (mode.equals("LifeLost")) {
            ret = new TriggerLifeLost(mapParams, host, intrinsic);
        } else if (mode.equals("Phase")) {
            ret = new TriggerPhase(mapParams, host, intrinsic);
        } else if (mode.equals("Sacrificed")) {
            ret = new TriggerSacrificed(mapParams, host, intrinsic);
        } else if (mode.equals("Shuffled")) {
            ret = new TriggerShuffled(mapParams, host, intrinsic);
        } else if (mode.equals("SpellAbilityCast")) {
            ret = new TriggerSpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("SpellCast")) {
            ret = new TriggerSpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("Taps")) {
            ret = new TriggerTaps(mapParams, host, intrinsic);
        } else if (mode.equals("TapsForMana")) {
            ret = new TriggerTapsForMana(mapParams, host, intrinsic);
        } else if (mode.equals("TurnFaceUp")) {
            ret = new TriggerTurnFaceUp(mapParams, host, intrinsic);
        } else if (mode.equals("Unequip")) {
            ret = new TriggerUnequip(mapParams, host, intrinsic);
        } else if (mode.equals("Untaps")) {
            ret = new TriggerUntaps(mapParams, host, intrinsic);
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
     */
    public final void runTrigger(final String mode, final Map<String, Object> runParams) {
        if (this.suppressedModes.contains(mode)) {
            return;
        }

        final Player playerAP = AllZone.getPhase().getPlayerTurn();

        // This is done to allow the list of triggers to be modified while
        // triggers are running.
        final ArrayList<Trigger> delayedTriggersWorkingCopy = new ArrayList<Trigger>(this.delayedTriggers);
        CardList allCards = AllZoneUtil.getCardsInGame();

        // Static triggers
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (t.getMapParams().containsKey("Static")) {
                    this.runSingleTrigger(t, mode, runParams);
                }
            }
        }

        /*
         * //This was supposed to make sure that things like Adaptive Automaton
         * //that used a static trigger to start pumping something
         * //"As they ETB" to make sure they then trigger cards //already on the
         * bf that would react to it. //However, this broke auras because
         * checkStateEffects //expect auras to be attached to something, which
         * they //aren't yet. And even if it (correctly?) sent unattached
         * //auras to the graveyard,that would result in all auras //going
         * straight to the grave no matter what. this.suppressMode("Always");
         * AllZone.getGameAction().checkStateEffects(true);
         * this.clearSuppression("Always");
         */

        // AP
        allCards = playerAP.getAllCards();
        allCards.addAll(AllZoneUtil.getCardsIn(Constant.Zone.Stack).getController(playerAP));
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (!t.getMapParams().containsKey("Static")) {
                    this.runSingleTrigger(t, mode, runParams);
                }
            }
        }
        for (int i = 0; i < delayedTriggersWorkingCopy.size(); i++) {
            final Trigger deltrig = delayedTriggersWorkingCopy.get(i);
            if (deltrig.getHostCard().getController().equals(playerAP)) {
                if (this.runSingleTrigger(deltrig, mode, runParams)) {
                    delayedTriggersWorkingCopy.remove(deltrig);
                    this.delayedTriggers.remove(deltrig);
                    i--;
                }
            }
        }

        // NAP
        allCards = playerAP.getOpponent().getAllCards();
        allCards.addAll(AllZoneUtil.getCardsIn(Constant.Zone.Stack).getController(playerAP.getOpponent()));
        for (final Card c : allCards) {
            for (final Trigger t : c.getTriggers()) {
                if (!t.getMapParams().containsKey("Static")) {
                    this.runSingleTrigger(t, mode, runParams);
                }
            }
        }
        for (int i = 0; i < delayedTriggersWorkingCopy.size(); i++) {
            final Trigger deltrig = delayedTriggersWorkingCopy.get(i);
            if (deltrig.getHostCard().getController().equals(playerAP.getOpponent())) {
                if (this.runSingleTrigger(deltrig, mode, runParams)) {
                    delayedTriggersWorkingCopy.remove(deltrig);
                    this.delayedTriggers.remove(deltrig);
                    i--;
                }
            }
        }
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
     * @return a boolean.
     */
    private boolean runSingleTrigger(final Trigger regtrig, final String mode, final Map<String, Object> runParams) {
        final Map<String, String> params = regtrig.getMapParams();

        if (!params.get("Mode").equals(mode)) {
            return false; // Not the right mode.
        }
        if (!regtrig.zonesCheck()) {
            return false; // Host card isn't where it needs to be.
        }
        if (!regtrig.phasesCheck()) {
            return false; // It's not the right phase to go off.
        }
        if (!regtrig.requirementsCheck()) {
            return false; // Conditions aren't right.
        }
        if (regtrig.getHostCard().isFaceDown() && regtrig.getIsIntrinsic()) {
            return false; // Morphed cards only have pumped triggers go off.
        }
        if (regtrig instanceof TriggerAlways) {
            if (AllZone.getStack().hasStateTrigger(regtrig.getId())) {
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

        // Torpor Orb check
        final CardList torporOrbs = AllZoneUtil.getCardsIn(Zone.Battlefield, "Torpor Orb");

        if (torporOrbs.size() != 0) {
            if (params.containsKey("Destination")) {
                if ((params.get("Destination").equals("Battlefield") || params.get("Destination").equals("Any"))
                        && mode.equals("ChangesZone")
                        && ((params.get("ValidCard").contains("Creature")) || (params.get("ValidCard").contains("Self") && regtrig
                                .getHostCard().isCreature()))) {
                    return false;
                }
            } else {
                if (mode.equals("ChangesZone")
                        && ((params.get("ValidCard").contains("Creature")) || (params.get("ValidCard").contains("Self") && regtrig
                                .getHostCard().isCreature()))) {
                    return false;
                }
            }

        } // Torpor Orb check

        final Player[] decider = new Player[1];
        final Player[] controller = new Player[1];

        // Any trigger should cause the phase not to skip
        AllZone.getPhase().setSkipPhase(false);

        regtrig.setRunParams(runParams);

        // All tests passed, execute ability.
        if (regtrig instanceof TriggerTapsForMana) {
            final AbilityMana abMana = (AbilityMana) runParams.get("Ability_Mana");
            if (null != abMana) {
                abMana.setUndoable(false);
            }
        }

        final AbilityFactory abilityFactory = new AbilityFactory();

        final SpellAbility[] sa = new SpellAbility[1];
        Card host = AllZoneUtil.getCardState(regtrig.getHostCard());

        if (host == null) {
            host = regtrig.getHostCard();
        }

        sa[0] = regtrig.getOverridingAbility();
        if (sa[0] == null) {
            if (!params.containsKey("Execute")) {
                sa[0] = new Ability(regtrig.getHostCard(), "0") {
                    @Override
                    public void resolve() {
                    }
                };
            } else {
                sa[0] = abilityFactory.getAbility(host.getSVar(params.get("Execute")), host);
            }
        }
        sa[0].setTrigger(true);
        sa[0].setSourceTrigger(regtrig.getId());
        regtrig.setTriggeringObjects(sa[0]);
        if (regtrig.getStoredTriggeredObjects() != null) {
            sa[0].setAllTriggeringObjects(regtrig.getStoredTriggeredObjects());
        }

        controller[0] = host.getController();
        sa[0].setActivatingPlayer(host.getController());
        sa[0].setStackDescription(sa[0].toString());
        // TODO - for Charms to supports AI, this needs to be removed
        if (sa[0].getActivatingPlayer().isHuman()) {
            AbilityFactoryCharm.setupCharmSAs(sa[0]);
        }
        boolean mand = false;
        if (params.containsKey("OptionalDecider")) {
            sa[0].setOptionalTrigger(true);
            mand = false;
            decider[0] = AbilityFactory.getDefinedPlayers(host, params.get("OptionalDecider"), sa[0]).get(0);
        } else {
            mand = true;

            SpellAbility ability = sa[0];
            while (ability != null) {
                final Target tgt = ability.getTarget();

                if (tgt != null) {
                    tgt.setMandatory(true);
                }
                ability = ability.getSubAbility();
            }
        }
        final boolean isMandatory = mand;

        // Wrapper ability that checks the requirements again just before
        // resolving, for intervening if clauses.
        // Yes, it must wrap ALL SpellAbility methods in order to handle
        // possible corner cases.
        // (The trigger can have a hardcoded OverridingAbility which can make
        // use of any of the methods)
        final Ability wrapperAbility = new Ability(regtrig.getHostCard(), "0") {

            @Override
            public boolean isWrapper() {
                return true;
            }

            @Override
            public void setPaidHash(final HashMap<String, CardList> hash) {
                sa[0].setPaidHash(hash);
            }

            @Override
            public HashMap<String, CardList> getPaidHash() {
                return sa[0].getPaidHash();
            }

            @Override
            public void setPaidList(final CardList list, final String str) {
                sa[0].setPaidList(list, str);
            }

            @Override
            public CardList getPaidList(final String str) {
                return sa[0].getPaidList(str);
            }

            @Override
            public void addCostToHashList(final Card c, final String str) {
                sa[0].addCostToHashList(c, str);
            }

            @Override
            public void resetPaidHash() {
                sa[0].resetPaidHash();
            }

            @Override
            public HashMap<String, Object> getTriggeringObjects() {
                return sa[0].getTriggeringObjects();
            }

            @Override
            public void setAllTriggeringObjects(final HashMap<String, Object> triggeredObjects) {
                sa[0].setAllTriggeringObjects(triggeredObjects);
            }

            @Override
            public void setTriggeringObject(final String type, final Object o) {
                sa[0].setTriggeringObject(type, o);
            }

            @Override
            public Object getTriggeringObject(final String type) {
                return sa[0].getTriggeringObject(type);
            }

            @Override
            public boolean hasTriggeringObject(final String type) {
                return sa[0].hasTriggeringObject(type);
            }

            @Override
            public void resetTriggeringObjects() {
                sa[0].resetTriggeringObjects();
            }

            @Override
            public boolean canPlay() {
                return sa[0].canPlay();
            }

            @Override
            public boolean canPlayAI() {
                return sa[0].canPlayAI();
            }

            @Override
            public void chooseTargetAI() {
                sa[0].chooseTargetAI();
            }

            @Override
            public SpellAbility copy() {
                return sa[0].copy();
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return sa[0].doTrigger(mandatory);
            }

            @Override
            public AbilityFactory getAbilityFactory() {
                return sa[0].getAbilityFactory();
            }

            @Override
            public Player getActivatingPlayer() {
                return sa[0].getActivatingPlayer();
            }

            @Override
            public Input getAfterPayMana() {
                return sa[0].getAfterPayMana();
            }

            @Override
            public Input getAfterResolve() {
                return sa[0].getAfterResolve();
            }

            @Override
            public Input getBeforePayMana() {
                return sa[0].getBeforePayMana();
            }

            @Override
            public Command getBeforePayManaAI() {
                return sa[0].getBeforePayManaAI();
            }

            @Override
            public Command getCancelCommand() {
                return sa[0].getCancelCommand();
            }

            @Override
            public CommandArgs getChooseTargetAI() {
                return sa[0].getChooseTargetAI();
            }

            @Override
            public String getDescription() {
                return sa[0].getDescription();
            }

            @Override
            public String getMultiKickerManaCost() {
                return sa[0].getMultiKickerManaCost();
            }

            @Override
            public String getReplicateManaCost() {
                return sa[0].getReplicateManaCost();
            }

            @Override
            public SpellAbilityRestriction getRestrictions() {
                return sa[0].getRestrictions();
            }

            @Override
            public Card getSourceCard() {
                return sa[0].getSourceCard();
            }

            @Override
            public String getStackDescription() {
                final StringBuilder sb = new StringBuilder(regtrig.toString());
                if (this.getTarget() != null) {
                    sb.append(" (Targeting ");
                    for (final Object o : this.getTarget().getTargets()) {
                        sb.append(o.toString());
                        sb.append(", ");
                    }
                    if (sb.toString().endsWith(", ")) {
                        sb.setLength(sb.length() - 2);
                    } else {
                        sb.append("ERROR");
                    }
                    sb.append(")");
                }

                return sb.toString();
            }

            @Override
            public AbilitySub getSubAbility() {
                return sa[0].getSubAbility();
            }

            @Override
            public Target getTarget() {
                return sa[0].getTarget();
            }

            @Override
            public Card getTargetCard() {
                return sa[0].getTargetCard();
            }

            @Override
            public CardList getTargetList() {
                return sa[0].getTargetList();
            }

            @Override
            public Player getTargetPlayer() {
                return sa[0].getTargetPlayer();
            }

            @Override
            public String getXManaCost() {
                return sa[0].getXManaCost();
            }

            @Override
            public boolean isAbility() {
                return sa[0].isAbility();
            }

            @Override
            public boolean isBuyBackAbility() {
                return sa[0].isBuyBackAbility();
            }

            @Override
            public boolean isCycling() {
                return sa[0].isCycling();
            }

            @Override
            public boolean isExtrinsic() {
                return sa[0].isExtrinsic();
            }

            @Override
            public boolean isFlashBackAbility() {
                return sa[0].isFlashBackAbility();
            }

            @Override
            public boolean isIntrinsic() {
                return sa[0].isIntrinsic();
            }

            @Override
            public boolean isKickerAbility() {
                return sa[0].isKickerAbility();
            }

            @Override
            public boolean isMultiKicker() {
                return sa[0].isMultiKicker();
            }

            @Override
            public boolean isReplicate() {
                return sa[0].isReplicate();
            }

            @Override
            public boolean isSpell() {
                return sa[0].isSpell();
            }

            @Override
            public boolean isTapAbility() {
                return sa[0].isTapAbility();
            }

            @Override
            public boolean isUntapAbility() {
                return sa[0].isUntapAbility();
            }

            @Override
            public boolean isXCost() {
                return sa[0].isXCost();
            }

            @Override
            public void resetOnceResolved() {
                // Fixing an issue with Targeting + Paying Mana
                // sa[0].resetOnceResolved();
            }

            @Override
            public void setAbilityFactory(final AbilityFactory af) {
                sa[0].setAbilityFactory(af);
            }

            @Override
            public void setActivatingPlayer(final Player player) {
                sa[0].setActivatingPlayer(player);
            }

            @Override
            public void setAdditionalManaCost(final String cost) {
                sa[0].setAdditionalManaCost(cost);
            }

            @Override
            public void setAfterPayMana(final Input in) {
                sa[0].setAfterPayMana(in);
            }

            @Override
            public void setAfterResolve(final Input in) {
                sa[0].setAfterResolve(in);
            }

            @Override
            public void setBeforePayMana(final Input in) {
                sa[0].setBeforePayMana(in);
            }

            @Override
            public void setBeforePayManaAI(final Command c) {
                sa[0].setBeforePayManaAI(c);
            }

            @Override
            public void setCancelCommand(final Command cancelCommand) {
                sa[0].setCancelCommand(cancelCommand);
            }

            @Override
            public void setChooseTargetAI(final CommandArgs c) {
                sa[0].setChooseTargetAI(c);
            }

            @Override
            public void setDescription(final String s) {
                sa[0].setDescription(s);
            }

            @Override
            public void setFlashBackAbility(final boolean flashBackAbility) {
                sa[0].setFlashBackAbility(flashBackAbility);
            }

            @Override
            public void setIsBuyBackAbility(final boolean b) {
                sa[0].setIsBuyBackAbility(b);
            }

            @Override
            public void setIsCycling(final boolean b) {
                sa[0].setIsCycling(b);
            }

            @Override
            public void setIsMultiKicker(final boolean b) {
                sa[0].setIsMultiKicker(b);
            }

            @Override
            public void setIsReplicate(final boolean b) {
                sa[0].setIsReplicate(b);
            }

            @Override
            public void setIsXCost(final boolean b) {
                sa[0].setIsXCost(b);
            }

            @Override
            public void setKickerAbility(final boolean kab) {
                sa[0].setKickerAbility(kab);
            }

            @Override
            public void setManaCost(final String cost) {
                sa[0].setManaCost(cost);
            }

            @Override
            public void setMultiKickerManaCost(final String cost) {
                sa[0].setMultiKickerManaCost(cost);
            }

            @Override
            public void setReplicateManaCost(final String cost) {
                sa[0].setReplicateManaCost(cost);
            }

            @Override
            public void setPayCosts(final Cost abCost) {
                sa[0].setPayCosts(abCost);
            }

            @Override
            public void setRestrictions(final SpellAbilityRestriction restrict) {
                sa[0].setRestrictions(restrict);
            }

            @Override
            public void setSourceCard(final Card c) {
                sa[0].setSourceCard(c);
            }

            @Override
            public void setStackDescription(final String s) {
                sa[0].setStackDescription(s);
            }

            @Override
            public void setSubAbility(final AbilitySub subAbility) {
                sa[0].setSubAbility(subAbility);
            }

            @Override
            public void setTarget(final Target tgt) {
                sa[0].setTarget(tgt);
            }

            @Override
            public void setTargetCard(final Card card) {
                sa[0].setTargetCard(card);
            }

            @Override
            public void setTargetList(final CardList list) {
                sa[0].setTargetList(list);
            }

            @Override
            public void setTargetPlayer(final Player p) {
                sa[0].setTargetPlayer(p);
            }

            @Override
            public void setType(final String s) {
                sa[0].setType(s);
            }

            @Override
            public void setXManaCost(final String cost) {
                sa[0].setXManaCost(cost);
            }

            @Override
            public boolean wasCancelled() {
                return sa[0].wasCancelled();
            }

            @Override
            public void setSourceTrigger(final int id) {
                sa[0].setSourceTrigger(id);
            }

            @Override
            public int getSourceTrigger() {
                return sa[0].getSourceTrigger();
            }

            @Override
            public void setOptionalTrigger(final boolean b) {
                sa[0].setOptionalTrigger(b);
            }

            @Override
            public boolean isOptionalTrigger() {
                return sa[0].isOptionalTrigger();
            }

            // //////////////////////////////////////
            // THIS ONE IS ALL THAT MATTERS
            // //////////////////////////////////////
            @Override
            public void resolve() {
                if (!(regtrig instanceof TriggerAlways)) {
                    // State triggers
                    // don't do the whole
                    // "Intervening If"
                    // thing.
                    if (!regtrig.requirementsCheck()) {
                        return;
                    }
                }

                if (decider[0] != null) {
                    if (decider[0].isHuman()) {
                        if (TriggerHandler.this.triggersAlwaysAccept.contains(this.getSourceTrigger())) {
                            // No need to do anything.
                        } else if (TriggerHandler.this.triggersAlwaysDecline.contains(this.getSourceTrigger())) {
                            return;
                        } else {
                            final StringBuilder buildQuestion = new StringBuilder("Use triggered ability of ");
                            buildQuestion.append(regtrig.getHostCard().getName()).append("(")
                                    .append(regtrig.getHostCard().getUniqueNumber()).append(")?");
                            buildQuestion.append("\r\n(");
                            buildQuestion.append(params.get("TriggerDescription").replace("CARDNAME",
                                    regtrig.getHostCard().getName()));
                            buildQuestion.append(")\r\n");
                            if (sa[0].getTriggeringObjects().containsKey("Attacker")) {
                                buildQuestion.append("[Attacker: " +sa[0].getTriggeringObjects().get("Attacker") + "]");
                            }
                            if (!GameActionUtil.showYesNoDialog(regtrig.getHostCard(), buildQuestion.toString())) {
                                return;
                            }
                        }
                    } else {
                        // This isn't quite right, but better than canPlayAI
                        if (!sa[0].doTrigger(isMandatory)) {
                            return;
                        }
                    }
                }

                if (controller[0].isHuman()) {
                    // Card src =
                    // (Card)(sa[0].getSourceCard().getTriggeringObject("Card"));
                    // System.out.println("Trigger resolving for "+mode+".  Card = "+src);
                    AllZone.getGameAction().playSpellAbilityNoStack(sa[0], true);
                } else {
                    // commented out because i don't think this should be called
                    // again here
                    // sa[0].doTrigger(isMandatory);
                    ComputerUtil.playNoStack(sa[0]);
                }

                // Add eventual delayed trigger.
                if (params.containsKey("DelayedTrigger")) {
                    final String sVarName = params.get("DelayedTrigger");
                    final Trigger deltrig = TriggerHandler.parseTrigger(regtrig.getHostCard().getSVar(sVarName),
                            regtrig.getHostCard(), true);
                    deltrig.setStoredTriggeredObjects(this.getTriggeringObjects());
                    TriggerHandler.this.registerDelayedTrigger(deltrig);
                }
            }
        };
        wrapperAbility.setTrigger(true);
        wrapperAbility.setMandatory(isMandatory);
        wrapperAbility.setDescription(wrapperAbility.getStackDescription());
        /*
         * if(host.getController().isHuman()) {
         * AllZone.getGameAction().playSpellAbility(wrapperAbility); } else {
         * wrapperAbility.doTrigger(isMandatory);
         * ComputerUtil.playStack(wrapperAbility); }
         */

        // Card src = (Card)(sa[0].getSourceCard().getTriggeringObject("Card"));
        // System.out.println("Trigger going on stack for "+mode+".  Card = "+src);

        if (params.containsKey("Static") && params.get("Static").equals("True")) {
            AllZone.getGameAction().playSpellAbilityNoStack(wrapperAbility, false);
        } else {
            AllZone.getStack().addSimultaneousStackEntry(wrapperAbility);
        }
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
