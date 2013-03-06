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
package forge.game.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.Command;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.AbilityTriggered;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.card.spellability.TargetChoices;
import forge.card.spellability.TargetSelection;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputPayManaExecuteCommands;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.event.SpellResolvedEvent;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.gui.GuiChoose;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.util.MyObservable;
import forge.view.ButtonUtil;

/**
 * <p>
 * MagicStack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MagicStack extends MyObservable {
    private final List<SpellAbility> simultaneousStackEntryList = new ArrayList<SpellAbility>();

    private final Stack<SpellAbilityStackInstance> stack = new Stack<SpellAbilityStackInstance>();
    private final Stack<SpellAbilityStackInstance> frozenStack = new Stack<SpellAbilityStackInstance>();

    private boolean frozen = false;
    private boolean bResolving = false;

    private final List<Card> thisTurnCast = new ArrayList<Card>();
    private List<Card> lastTurnCast = new ArrayList<Card>();
    private Card curResolvingCard = null;

    private final GameState game;

    /**
     * TODO: Write javadoc for Constructor.
     * @param gameState
     */
    public MagicStack(GameState gameState) {
        game = gameState;
    }

    /**
     * <p>
     * isFrozen.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFrozen() {
        return this.frozen;
    }

    /**
     * <p>
     * Setter for the field <code>frozen</code>.
     * </p>
     * 
     * @param frozen0
     *            a boolean.
     */
    public final void setFrozen(final boolean frozen0) {
        this.frozen = frozen0;
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        this.getStack().clear();
        this.simultaneousStackEntryList.clear();
        this.frozen = false;
        this.lastTurnCast.clear();
        this.thisTurnCast.clear();
        this.curResolvingCard = null;
        this.getFrozenStack().clear();
        this.updateObservers();
    }

    /**
     * <p>
     * isSplitSecondOnStack.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSplitSecondOnStack() {
        for(SpellAbilityStackInstance si : this.getStack()) {
            if (si.isSpell() && si.getSourceCard().hasKeyword("Split second")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * freezeStack.
     * </p>
     */
    public final void freezeStack() {
        this.frozen = true;
    }

    /**
     * <p>
     * addAndUnfreeze.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addAndUnfreeze(final SpellAbility ability) {
        ability.getRestrictions().abilityActivated();
        if ((ability.getRestrictions().getActivationNumberSacrifice() != -1)
                && (ability.getRestrictions().getNumberTurnActivations() >= ability.getRestrictions()
                        .getActivationNumberSacrifice())) {
            ability.getSourceCard().addExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
        }

        // if the ability is a spell, but not a copied spell and its not already
        // on the stack zone, move there
        if (ability.isSpell()) {
            final Card source = ability.getSourceCard();
            if (!source.isCopiedSpell() && !source.isInZone(ZoneType.Stack)) {
                ability.setSourceCard(game.getAction().moveToStack(source));
            }
        }
        
        // Always add the ability here and always unfreeze the stack
        this.add(ability);
        this.unfreezeStack();
    }

    /**
     * <p>
     * unfreezeStack.
     * </p>
     */
    public final void unfreezeStack() {
        this.frozen = false;
        boolean checkState = !this.getFrozenStack().isEmpty();
        // Add all Frozen Abilities onto the stack
        while (!this.getFrozenStack().isEmpty()) {
            final SpellAbility sa = this.getFrozenStack().pop().getSpellAbility();
            this.add(sa);
        }
        // Add all waiting triggers onto the stack
        checkState |= Singletons.getModel().getGame().getTriggerHandler().runWaitingTriggers(false);
        if (checkState) {
            this.chooseOrderOfSimultaneousStackEntryAll();
            game.getAction().checkStateEffects();
        }
    }

    /**
     * <p>
     * clearFrozen.
     * </p>
     */
    public final void clearFrozen() {
        // TODO: frozen triggered abilities and undoable costs have nasty
        // consequences
        this.frozen = false;
        this.getFrozenStack().clear();
    }

    /**
     * <p>
     * removeFromFrozenStack.
     * </p>
     * @param sa
     *            a SpellAbility.
     */
    public final void removeFromFrozenStack(SpellAbility sa) {
        SpellAbilityStackInstance si = this.getInstanceFromSpellAbility(sa);
        this.getFrozenStack().remove(si);
        if (this.getFrozenStack().isEmpty()) {
            clearFrozen();
        }
    }

    /**
     * <p>
     * setResolving.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setResolving(final boolean b) {
        this.bResolving = b;
        if (!this.bResolving) {
            this.chooseOrderOfSimultaneousStackEntryAll();
        }
    }

    /**
     * <p>
     * getResolving.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isResolving() {
        return this.bResolving;
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param useX
     *            a boolean.
     */
    public final void add(final SpellAbility sp, final boolean useX) {
        if (!useX) {
            this.add(sp);
        } else {

            // TODO: make working triggered abilities!
            if (sp.isManaAbility() || (sp instanceof AbilityTriggered)) {
                AbilityUtils.resolve(sp, false);
                //sp.resolve();
            } else {
                this.push(sp);
                /*
                 * if (sp.getTargetCard() != null)
                 * CardFactoryUtil.checkTargetingEffects(sp,
                 * sp.getTargetCard());
                 */
            }
        }
    }

    /**
     * <p>
     * getMultiKickerSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid getMultiKickerSpellCostChange(final SpellAbility sa) {
        final int max = 25;
        final String[] numbers = new String[max];
        for (int no = 0; no < max; no++) {
            numbers[no] = String.valueOf(no);
        }

        ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getManaCost());
        String mana = manaCost.toString();

        int multiKickerPaid = game.getActionPlay().getCostCuttingGetMultiKickerManaCostPaid();

        String numberManaCost = " ";

        if (mana.toString().length() == 1) {
            numberManaCost = mana.toString().substring(0, 1);
        } else if (mana.toString().length() == 0) {
            numberManaCost = "0"; // Should Never Occur
        } else {
            numberManaCost = mana.toString().substring(0, 2);
        }

        numberManaCost = numberManaCost.trim();

        for (int check = 0; check < max; check++) {
            if (numberManaCost.equals(numbers[check])) {

                if ((check - multiKickerPaid) < 0) {
                    multiKickerPaid = multiKickerPaid - check;
                    game.getActionPlay().setCostCuttingGetMultiKickerManaCostPaid(multiKickerPaid);
                    mana = mana.replaceFirst(String.valueOf(check), "0");
                } else {
                    mana = mana.replaceFirst(String.valueOf(check), String.valueOf(check - multiKickerPaid));
                    multiKickerPaid = 0;
                    game.getActionPlay().setCostCuttingGetMultiKickerManaCostPaid(multiKickerPaid);
                }
            }
            mana = mana.trim();
            if (mana.equals("")) {
                mana = "0";
            }
            manaCost = new ManaCostBeingPaid(mana);
        }
        final String colorCut = game.getActionPlay().getCostCuttingGetMultiKickerManaCostPaidColored();

        for (int colorCutIx = 0; colorCutIx < colorCut.length(); colorCutIx++) {
            if ("WUGRB".contains(colorCut.substring(colorCutIx, colorCutIx + 1))
                    && !mana.equals(mana.replaceFirst((colorCut.substring(colorCutIx, colorCutIx + 1)), ""))) {
                mana = mana.replaceFirst(colorCut.substring(colorCutIx, colorCutIx + 1), "");

                game.getActionPlay().setCostCuttingGetMultiKickerManaCostPaidColored(
                        game.getActionPlay().getCostCuttingGetMultiKickerManaCostPaidColored()
                                .replaceFirst(colorCut.substring(colorCutIx, colorCutIx + 1), ""));

                mana = mana.trim();
                if (mana.equals("")) {
                    mana = "0";
                }
                manaCost = new ManaCostBeingPaid(mana);
            }
        }

        return manaCost;
    }

    // TODO: this may be able to use a straight copy of MultiKicker cost change
    /**
     * <p>
     * getReplicateSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid getReplicateSpellCostChange(final SpellAbility sa) {
        final ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getManaCost());
        // String Mana = manaCost.toString();
        return manaCost;
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void add(final SpellAbility sp) {
        final ArrayList<TargetChoices> chosenTargets = sp.getAllTargetChoices();

        if (sp.isManaAbility()) { // Mana Abilities go straight through
            AbilityUtils.resolve(sp, false);
            //sp.resolve();
            sp.resetOnceResolved();
            game.getGameLog().add("Mana", sp.getSourceCard() + " - " + sp.getDescription(), 4);
            return;
        }

        if (sp.isSpell()) {
            Spell spell = (Spell) sp;
            if (spell.isCastFaceDown()) {
                sp.getSourceCard().turnFaceDown();
            }
        }

        if (this.frozen) {
            final SpellAbilityStackInstance si = new SpellAbilityStackInstance(sp);
            this.getFrozenStack().push(si);
            return;
        }

        //============= GameLog ======================
        StringBuilder sb = new StringBuilder();
        sb.append(sp.getActivatingPlayer());
        if (sp.isSpell()) {
            sb.append(" cast ");
        }
        else if (sp.isAbility()) {
            sb.append(" activated ");
        }

        if (sp.getStackDescription().startsWith("Morph ")) {
            sb.append("Morph");
        } else {
            sb.append(sp.getSourceCard());
        }

        if (sp.getTarget() != null) {
            sb.append(" targeting ");
            for (TargetChoices ch : chosenTargets) {
                if (null != ch) {
                    sb.append(ch.getTargetedString());
                }
            }
        }
        sb.append(".");

        game.getGameLog().add("AddToStack", sb.toString(), 2);
        //============= GameLog ======================

        // if activating player slips through the cracks, assign activating
        // Player to the controller here
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        if (game.getPhaseHandler().is(PhaseType.CLEANUP)) {
            // If something triggers during Cleanup, need to repeat
            game.getPhaseHandler().repeatPhase();
        }

        if ((sp instanceof AbilityTriggered) || (sp instanceof AbilityStatic)) {
            // TODO: make working triggered ability
            sp.resolve();
            game.getAction().checkStateEffects();
            //GuiDisplayUtil.updateGUI();
        } else {
            if (sp.getOptionalAdditionalCosts() != null) {
                for (String s : sp.getOptionalAdditionalCosts()) {
                    sp.getSourceCard().addOptionalAdditionalCostsPaid(s);
                }
            }
            if (sp.getSourceCard().isCopiedSpell()) {
                this.push(sp);
            } else if (!sp.isMultiKicker() && !sp.isReplicate() && !sp.isXCost()) {
                this.push(sp);
            } else if ((sp.getPayCosts() != null) && !sp.isMultiKicker() && !sp.isReplicate()) {
                this.push(sp);
            } else if (sp.isXCost()) {
                // TODO: convert any X costs to use abCost so it happens earlier
                final SpellAbility sa = sp;
                final ManaCost mc = new ManaCost( new ManaCostParser(Integer.toString(sa.getXManaCost())));
                final Ability ability = new Ability(sp.getSourceCard(), mc) {
                    @Override
                    public void resolve() {
                        final Card crd = this.getSourceCard();
                        crd.addXManaCostPaid(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    @Override
                    public void execute() {
                        MagicStack.this.push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -2224875229611007788L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        final Card crd = sa.getSourceCard();
                        Singletons.getModel().getMatch().getInput().setInput(
                                new InputPayManaExecuteCommands(game, "Pay X cost for " + crd.getName() + " (X="
                                        + crd.getXManaCostPaid() + ")\r\n", ability.getManaCost().toString(), this, unpaidCommand,
                                        true));
                    }
                };

                final Card crd = sa.getSourceCard();
                Player player = sp.getSourceCard().getController();
                if (player.isHuman()) {
                    Singletons.getModel().getMatch().getInput().setInput(
                            new InputPayManaExecuteCommands(game, "Pay X cost for " + sp.getSourceCard().getName() + " (X="
                                    + crd.getXManaCostPaid() + ")\r\n", ability.getManaCost().toString(), paidCommand,
                                    unpaidCommand, true));
                } else {
                    // computer
                    final int neededDamage = CardFactoryUtil.getNeededXDamage(sa);

                    while (ComputerUtilCost.canPayCost(ability, player) && (neededDamage != sa.getSourceCard().getXManaCostPaid())) {
                        ComputerUtil.playNoStack((AIPlayer)player, ability, game);
                    }
                    this.push(sa);
                }
            } else if (sp.isMultiKicker()) {
                // TODO: convert multikicker support in abCost so this doesn't
                // happen here
                // both X and multi is not supported yet

                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost()) {
                    @Override
                    public void resolve() {
                        this.getSourceCard().addMultiKickerMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    @Override
                    public void execute() {
                        MagicStack.this.push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -6037161763374971106L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        
                        final ManaCostBeingPaid manaCost = MagicStack.this.getMultiKickerSpellCostChange(ability);
                        
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {
                            String prompt;
                            int mkCostPaid = game.getActionPlay().getCostCuttingGetMultiKickerManaCostPaid(); 
                            String mkCostPaidColored = game.getActionPlay().getCostCuttingGetMultiKickerManaCostPaidColored();
                            int mkMagnitude = sa.getSourceCard().getMultiKickerMagnitude();
                            if ((mkCostPaid == 0) && mkCostPaidColored.equals("")) {
                                prompt = String.format("Multikicker for %s\r\nTimes Kicked: %d\r\n", sa.getSourceCard(), mkMagnitude );
                            } else {
                                prompt = String.format("Multikicker for %s\r\nMana in Reserve: %s %s\r\nTimes Kicked: %d", sa.getSourceCard(), 
                                        (mkCostPaid != 0) ? Integer.toString(mkCostPaid) : "", mkCostPaidColored, mkMagnitude);
                            }
                            Input toSet = new InputPayManaExecuteCommands(game, prompt, manaCost.toString(), this, unpaidCommand);
                            Singletons.getModel().getMatch().getInput().setInput(toSet);
                        }
                    }
                };
                Player activating = sp.getActivatingPlayer();

                if (activating.isHuman()) {
                    sa.getSourceCard().addMultiKickerMagnitude(-1);
                    paidCommand.execute();
                } else {
                    // computer

                    while (ComputerUtilCost.canPayCost(ability, activating)) {
                        ComputerUtil.playNoStack((AIPlayer)activating, ability, game);
                    }

                    this.push(sa);
                }
            } else if (sp.isReplicate()) {
                // TODO: convert multikicker/replicate support in abCost so this
                // doesn't happen here
                // X and multi and replicate are not supported yet

                final SpellAbility sa = sp;
                MagicStack.this.push(sa);
                final Ability ability = new Ability(sp.getSourceCard(), sp.getReplicateManaCost()) {
                    @Override
                    public void resolve() {
                        this.getSourceCard().addReplicateMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3180458633098297855L;

                    @Override
                    public void execute() {
                        for (int i = 0; i < sp.getSourceCard().getReplicateMagnitude(); i++) {
                            CardFactory.copySpellontoStack(sp.getSourceCard(), sp.getSourceCard(), sp, false);
                        }
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = 132624005072267304L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        final ManaCostBeingPaid manaCost = MagicStack.this.getReplicateSpellCostChange(ability);
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {
                            String prompt = String.format("Replicate for %s\r\nTimes Replicated: %d\r\n", sa.getSourceCard(), sa.getSourceCard().getReplicateMagnitude());
                            Input toSet = new InputPayManaExecuteCommands(game, prompt, manaCost.toString(), this, unpaidCommand);
                            Singletons.getModel().getMatch().getInput().setInput(toSet);
                        }
                    }
                };

                Player controller = sp.getSourceCard().getController();
                if (controller.isHuman()) {
                    sa.getSourceCard().addReplicateMagnitude(-1);
                    paidCommand.execute();
                } else {
                    // computer
                    while (ComputerUtilCost.canPayCost(ability, controller)) {
                        ComputerUtil.playNoStack((AIPlayer)controller, ability, game);
                    }

                    this.push(sa);
                }
            }

        }

        // Copied spells aren't cast
        // per se so triggers shouldn't
        // run for them.
        if (!(sp instanceof AbilityStatic) && !sp.isCopied()) {
            // Run SpellAbilityCast triggers
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Cost", sp.getPayCosts());
            runParams.put("Player", sp.getSourceCard().getController());
            runParams.put("Activator", sp.getActivatingPlayer());
            runParams.put("CastSA", sp);
            game.getTriggerHandler().runTrigger(TriggerType.SpellAbilityCast, runParams, true);

            // Run SpellCast triggers
            if (sp.isSpell()) {
                game.getTriggerHandler().runTrigger(TriggerType.SpellCast, runParams, true);
            }

            // Run AbilityCast triggers
            if (sp.isAbility() && !sp.isTrigger()) {
                game.getTriggerHandler().runTrigger(TriggerType.AbilityCast, runParams, true);
            }

            // Run Cycled triggers
            if (sp.isCycling()) {
                runParams.clear();
                runParams.put("Card", sp.getSourceCard());
                game.getTriggerHandler().runTrigger(TriggerType.Cycled, runParams, false);
            }

            // Run BecomesTarget triggers
            // Create a new object, since the triggers aren't happening right away
            runParams = new HashMap<String, Object>();
            runParams.put("SourceSA", sp);
            if (!chosenTargets.isEmpty()) { 
                HashSet<Object> distinctObjects = new HashSet<Object>();
                for (final TargetChoices tc : chosenTargets) {
                    if ((tc != null) && (tc.getTargetCards() != null)) {
                        for (final Object tgt : tc.getTargets()) {
                            // Track distinct objects so Becomes targets don't trigger for things like:
                            // Seeds of Strength or Pyrotechnics
                            if (distinctObjects.contains(tgt)) {
                                continue;
                            }
                            
                            distinctObjects.add(tgt);
                            runParams.put("Target", tgt);
                            game.getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
                        }
                    }
                }
            }

            // Not sure these clauses are necessary. Consider it a precaution
            // for backwards compatibility for hardcoded cards.
            else if (sp.getTargetCard() != null) {
                runParams.put("Target", sp.getTargetCard());

                game.getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
            }
        }

        /*
         * Whenever a player casts a spell, counter it if a card with the same
         * name is in a graveyard or a nontoken permanent with the same name is
         * on the battlefield.
         */
        if (sp.isSpell() && game.isCardInPlay("Bazaar of Wonders")) {
            boolean found = false;
            List<Card> all = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.NON_TOKEN);
            final List<Card> graves = game.getCardsIn(ZoneType.Graveyard);
            all.addAll(graves);

            for (final Card c : all) {
                if (sp.getSourceCard().getName().equals(c.getName())) {
                    found = true;
                }
            }

            if (found) {
                final List<Card> bazaars = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Bazaar of Wonders")); // should
                // only
                // be
                // 1...
                for (final Card bazaar : bazaars) {
                    final SpellAbility counter = new Ability(bazaar, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (game.getStack().size() > 0) {
                                game.getStack().pop();
                            }
                        } // resolve()
                    }; // SpellAbility
                    counter.setStackDescription(bazaar.getName() + " - counter " + sp.getSourceCard().getName() + ".");
                    this.add(counter);
                }
            }
        }

        if (this.getSimultaneousStackEntryList().size() > 0) {
            game.getPhaseHandler().passPriority();
        }
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    public final int size() {
        return this.getStack().size();
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEmpty() {
        return this.getStack().size() == 0;
    }

    // Push should only be used by add.
    /**
     * <p>
     * push.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void push(final SpellAbility sp) {
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        final SpellAbilityStackInstance si = new SpellAbilityStackInstance(sp);
        this.getStack().push(si);

        // 2012-07-21 the following comparison needs to move below the pushes but somehow screws up priority
        // When it's down there. That makes absolutely no sense to me, so i'm putting it back for now
        if (!((sp instanceof AbilityTriggered) || (sp instanceof AbilityStatic))) {
            // when something is added we need to setPriority
            game.getPhaseHandler().setPriority(sp.getActivatingPlayer());
        }

        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc());
        this.updateObservers();

        if (sp.isSpell() && !sp.getSourceCard().isCopiedSpell()) {
            this.thisTurnCast.add(sp.getSourceCard());

            GameActionUtil.executePlayCardEffects(sp);
        }
    }

    /**
     * <p>
     * resolveStack.
     * </p>
     */
    public final void resolveStack() {
        // Resolving the Stack

        // TODO: change to use forge.view.FView?
        //GuiDisplayUtil.updateGUI();

        this.freezeStack(); // freeze the stack while we're in the middle of
                            // resolving
        this.setResolving(true);

        final SpellAbility sa = this.pop();

        game.getPhaseHandler().resetPriority(); // ActivePlayer gains priority first
                                            // after Resolve
        final Card source = sa.getSourceCard();
        curResolvingCard = source;

        if (this.hasFizzled(sa, source, false)) { // Fizzle
            // TODO: Spell fizzles, what's the best way to alert player?
            Log.debug(source.getName() + " ability fizzles.");
            game.getGameLog().add("ResolveStack", source.getName() + " ability fizzles.", 2);
            this.finishResolving(sa, true);
        } else if (sa.getApi() != null) {
            game.getGameLog().add("ResolveStack", sa.getStackDescription(), 2);
            AbilityUtils.handleRemembering(sa);
            AbilityUtils.resolve(sa, true);
        } else {
            game.getGameLog().add("ResolveStack", sa.getStackDescription(), 2);
            sa.resolve();
            this.finishResolving(sa, false);
            // do creatures ETB from here?
        }
        sa.getSourceCard().setXManaCostPaid(0);

        game.getEvents().post(new SpellResolvedEvent(source, sa));

        if (source.hasStartOfKeyword("Haunt") && !source.isCreature()
                && game.getZoneOf(source).is(ZoneType.Graveyard)) {
            final List<Card> creats = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            final Ability haunterDiesWork = new Ability(source, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    game.getAction().exile(source);
                    this.getTargetCard().addHauntedBy(source);
                }
            };
            for (int i = 0; i < creats.size(); i++) {
                if (!creats.get(i).canBeTargetedBy(haunterDiesWork)) {
                    creats.remove(i);
                    i--;
                }
            }
            if (creats.size() != 0) {
                haunterDiesWork.setDescription("");

                final Input target = new Input() {
                    private static final long serialVersionUID = 1981791992623774490L;

                    @Override
                    public void showMessage() {
                        CMatchUI.SINGLETON_INSTANCE.showMessage("Choose target creature to haunt.");
                        ButtonUtil.disableAll();
                    }

                    @Override
                    public void selectCard(final Card c) {
                        Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                        if (!zone.is(ZoneType.Battlefield) || !c.isCreature()) {
                            return;
                        }
                        if (c.canBeTargetedBy(haunterDiesWork)) {
                            haunterDiesWork.setTargetCard(c);
                            MagicStack.this.add(haunterDiesWork);
                            this.stop();
                        } else {
                            CMatchUI.SINGLETON_INSTANCE.showMessage("Cannot target this card (Shroud? Protection?).");
                        }
                    }
                };

                if (source.getController().isHuman()) {
                    Singletons.getModel().getMatch().getInput().setInput(target);
                } else {
                    // AI choosing what to haunt
                    final List<Card> oppCreats = CardLists.filterControlledBy(creats, source.getController().getOpponents());
                    if (oppCreats.size() != 0) {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(oppCreats));
                    } else {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(creats));
                    }
                    this.add(haunterDiesWork);
                }
            }
        }
    }

    /**
     * <p>
     * removeCardFromStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle
     *            a boolean.
     * @since 1.0.15
     */
    public final void removeCardFromStack(final SpellAbility sa, final boolean fizzle) {
        Card source = sa.getSourceCard();

        // do nothing
        if (sa.getSourceCard().isCopiedSpell() || sa.isAbility()) {
        }
        // Handle cards that need to be moved differently
        else if (sa.isBuyBackAbility() && !fizzle) {
            game.getAction().moveToHand(source);
        } else if (sa.isFlashBackAbility()) {
            game.getAction().exile(source);
            sa.setFlashBackAbility(false);
        } else if (source.hasKeyword("Rebound")
                && source.getCastFrom() == ZoneType.Hand
                && game.getZoneOf(source).is(ZoneType.Stack)
                && source.getOwner().equals(source.getController())) //"If you cast this spell from your hand"
        {

            //Move rebounding card to exile
            source = game.getAction().exile(source);

            source.setSVar("ReboundAbilityTrigger", "DB$ Play | Defined$ Self "
                    + "| WithoutManaCost$ True | Optional$ True");

            //Setup a Rebound-trigger
            final Trigger reboundTrigger = forge.card.trigger.TriggerHandler.parseTrigger("Mode$ Phase "
                    + "| Phase$ Upkeep | ValidPlayer$ You | OptionalDecider$ You | Execute$ ReboundAbilityTrigger "
                    + "| TriggerDescription$ At the beginning of your next upkeep, you may cast " + source.toString()
                    + " without paying it's manacost.", source, true);

            game.getTriggerHandler().registerDelayedTrigger(reboundTrigger);
        }

        // If Spell and still on the Stack then let it goto the graveyard or
        // replace its own movement
        else if (!source.isCopiedSpell() && (source.isInstant() || source.isSorcery() || fizzle)
                && source.isInZone(ZoneType.Stack)) {
            game.getAction().moveToGraveyard(source);
        }
    }

    /**
     * <p>
     * finishResolving.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle
     *            a boolean.
     * @since 1.0.15
     */
    public final void finishResolving(final SpellAbility sa, final boolean fizzle) {

        // remove card from the stack
        this.removeCardFromStack(sa, fizzle);

        // After SA resolves we have to do a handful of things
        this.setResolving(false);
        this.unfreezeStack();
        sa.resetOnceResolved();

        game.getAction().checkStateEffects();

        game.getPhaseHandler().setPlayersPriorityPermission(true);

        this.curResolvingCard = null;

        // TODO: change to use forge.view.FView?
        //GuiDisplayUtil.updateGUI();
        this.updateObservers();

        // TODO: this is a huge hack. Why is this necessary?
        // hostCard in AF is not the same object that's on the battlefield
        // verified by System.identityHashCode(card);
        final Card tmp = sa.getSourceCard();
        tmp.setCanCounter(true); // reset mana pumped counter magic flag
        if (tmp.getClones().size() > 0) {
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.equals(tmp)) {
                    c.setClones(tmp.getClones());
                }
            }

        }
    }

    /**
     * <p>
     * hasFizzled.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean hasFizzled(final SpellAbility sa, final Card source, final boolean parentFizzled) {
        // Can't fizzle unless there are some targets
        boolean fizzle = false;

        Target tgt = sa.getTarget();
        if (tgt != null) {
            if (tgt.getMinTargets(source, sa) == 0 && tgt.getNumTargeted() == 0) {
                // Nothing targeted, and nothing needs to be targeted.
            }
            else {
                // Some targets were chosen, fizzling for this subability is now possible
                fizzle = true;
                // With multi-targets, as long as one target is still legal,
                // we'll try to go through as much as possible
                final ArrayList<Object> tgts = tgt.getTargets();
                final TargetChoices choices = tgt.getTargetChoices();
                for (final Object o : tgts) {
                    boolean invalidTarget = false;
                    if (o instanceof Player) {
                        final Player p = (Player) o;
                        invalidTarget = !(p.canBeTargetedBy(sa));
                        // TODO Remove target?
                        if (invalidTarget) {
                            choices.removeTarget(p);
                        }
                    }
                    else if (o instanceof Card) {
                        final Card card = (Card) o;
                        Card current = game.getCardState(card);

                        invalidTarget = current.getTimestamp() != card.getTimestamp();

                        invalidTarget |= !(CardFactoryUtil.isTargetStillValid(sa, card));

                        if (invalidTarget) {
                            choices.removeTarget(card);
                        }
                        // Remove targets
                    }
                    else if (o instanceof SpellAbility) {
                        final SpellAbility tgtSA = (SpellAbility) o;
                        invalidTarget = !(TargetSelection.matchSpellAbility(sa, tgtSA, tgt));
                        // TODO Remove target?
                        if (invalidTarget) {
                            choices.removeTarget(tgtSA);
                        }
                    }
                    fizzle &= invalidTarget;
                }
            }
        }
        else if (sa.getTargetCard() != null) {
            fizzle = !CardFactoryUtil.isTargetStillValid(sa, sa.getTargetCard());
        }
        else {
            // Set fizzle to the same as the parent if there's no target info
            fizzle = parentFizzled;
        }

        if (sa.getSubAbility() == null) {
            return fizzle;
        }

        return hasFizzled(sa.getSubAbility(), source, fizzle) && fizzle;
    }

    /**
     * <p>
     * pop.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility pop() {
        final SpellAbilityStackInstance si = this.getStack().pop();
        final SpellAbility sp = si.getSpellAbility();
        // NOTE (12/04/22): Update Observers here causes multi-targeting bug
        // We Update Observers after the Stack Finishes Resolving
        // No need to do it sooner
        //this.updateObservers();
        return sp;
    }

    // CAREFUL! Peeking while an SAs Targets are being choosen may cause issues
    // index = 0 is the top, index = 1 is the next to top, etc...
    /**
     * <p>
     * peekInstance.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbilityStackInstance}
     *         object.
     */
    public final SpellAbilityStackInstance peekInstance(final int index) {
        return this.getStack().get(index);
    }

    /**
     * <p>
     * peekAbility.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility peekAbility(final int index) {
        return this.getStack().get(index).getSpellAbility();
    }

    /**
     * <p>
     * peekInstance.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbilityStackInstance}
     *         object.
     */
    public final SpellAbilityStackInstance peekInstance() {
        return this.getStack().peek();
    }

    /**
     * <p>
     * peekAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility peekAbility() {
        return this.getStack().peek().getSpellAbility();
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void remove(final SpellAbility sa) {
        final SpellAbilityStackInstance si = this.getInstanceFromSpellAbility(sa);

        if (si == null) {
            return;
        }

        this.remove(si);
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param si
     *            a {@link forge.card.spellability.SpellAbilityStackInstance}
     *            object.
     */
    public final void remove(final SpellAbilityStackInstance si) {
        this.getStack().remove(si);
        this.getFrozenStack().remove(si);
        this.updateObservers();
    }

    /**
     * <p>
     * getInstanceFromSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbilityStackInstance}
     *         object.
     */
    public final SpellAbilityStackInstance getInstanceFromSpellAbility(final SpellAbility sa) {
        // TODO: Confirm this works!
        for (final SpellAbilityStackInstance si : this.getStack()) {
            if (si.getSpellAbility().equals(sa)) {
                return si;
            }
        }
        return null;
    }

    /**
     * <p>
     * hasSimultaneousStackEntries.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSimultaneousStackEntries() {
        return this.getSimultaneousStackEntryList().size() > 0;
    }

    public final void clearSimultaneousStack() {
        this.simultaneousStackEntryList.clear();
    }

    /**
     * <p>
     * addSimultaneousStackEntry.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addSimultaneousStackEntry(final SpellAbility sa) {
        this.getSimultaneousStackEntryList().add(sa);
    }

    /**
     * <p>
     * chooseOrderOfSimultaneousStackEntryAll.
     * </p>
     */
    public final void chooseOrderOfSimultaneousStackEntryAll() {
        final Player playerTurn = game.getPhaseHandler().getPlayerTurn();

        this.chooseOrderOfSimultaneousStackEntry(playerTurn);

        if (playerTurn != null) {
            for (final Player otherP : playerTurn.getAllOtherPlayers()) {
                this.chooseOrderOfSimultaneousStackEntry(otherP);
            }
        }
    }

    /**
     * <p>
     * chooseOrderOfSimultaneousStackEntry.
     * </p>
     * 
     * @param activePlayer
     *            a {@link forge.game.player.Player} object.
     */
    public final void chooseOrderOfSimultaneousStackEntry(final Player activePlayer) {
        if (this.getSimultaneousStackEntryList().size() == 0) {
            return;
        }

        final ArrayList<SpellAbility> activePlayerSAs = new ArrayList<SpellAbility>();
        for (int i = 0; i < this.getSimultaneousStackEntryList().size(); i++) {
            SpellAbility sa = this.getSimultaneousStackEntryList().get(i);
            Player activator = sa.getActivatingPlayer();
            if (activator == null) {
                if (sa.getSourceCard().getController().equals(activePlayer)) {
                    activePlayerSAs.add(sa);
                    this.getSimultaneousStackEntryList().remove(i);
                    i--;
                }
            } else {
                if (activator.equals(activePlayer)) {
                    activePlayerSAs.add(sa);
                    this.getSimultaneousStackEntryList().remove(i);
                    i--;
                }
            }
        }
        if (activePlayerSAs.size() == 0) {
            return;
        }

        if (activePlayer.isComputer()) {
            for (final SpellAbility sa : activePlayerSAs) {
                sa.doTrigger(sa.isMandatory(), (AIPlayer) activePlayer);
                ComputerUtil.playStack(sa, (AIPlayer) activePlayer, game);
            }
        } else {
            // If only one, just add as necessary
            if (activePlayerSAs.size() == 1) {
                SpellAbility next = activePlayerSAs.get(0);
                if (next.isTrigger()) {
                    game.getActionPlay().playSpellAbility(next, activePlayer);
                } else {
                    this.add(next);
                }
            } else {
                // Otherwise, gave a dual list form to create instead of needing to do it one at a time
                List<SpellAbility> orderedSAs = GuiChoose.order("Select order for Simultaneous Spell Abilities", "Resolve first", 0, activePlayerSAs, null, null);
                int size = orderedSAs.size();
                for (int i = size - 1; i >= 0; i--) {
                    SpellAbility next = orderedSAs.get(i);
                    if (next.isTrigger()) {
                        game.getActionPlay().playSpellAbility(next, activePlayer);
                    } else {
                        this.add(next);
                    }
                }
            }
        }

    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param triggerID
     *            the trigger id
     * @return true, if successful
     */
    public final boolean hasStateTrigger(final int triggerID) {
        for (final SpellAbilityStackInstance sasi : this.getStack()) {
            if (sasi.isStateTrigger(triggerID)) {
                return true;
            }
        }

        for (final SpellAbility sa : this.simultaneousStackEntryList) {
            if (sa.getSourceTrigger() == triggerID) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the simultaneous stack entry list.
     * 
     * @return the simultaneousStackEntryList
     */
    public final List<SpellAbility> getSimultaneousStackEntryList() {
        return this.simultaneousStackEntryList;
    }

    /**
     * Gets the stack.
     * 
     * @return the stack
     */
    public final Stack<SpellAbilityStackInstance> getStack() {
        return this.stack;
    }

    /**
     * Gets the frozen stack.
     * 
     * @return the frozenStack
     */
    public final Stack<SpellAbilityStackInstance> getFrozenStack() {
        return this.frozenStack;
    }

    /**
     * Accessor for the field thisTurnCast.
     * 
     * @return a CardList.
     */
    public final List<Card> getCardsCastThisTurn() {
        return this.thisTurnCast;
    }

    /**
     * clearCardsCastThisTurn.
     */
    public final void clearCardsCastThisTurn() {
        this.thisTurnCast.clear();
    }

    /**
     * 
     * setCardsCastLastTurn.
     */
    public final void setCardsCastLastTurn() {
        this.lastTurnCast = new ArrayList<Card>(this.thisTurnCast);
    }

    /**
     * Accessor for the field lastTurnCast.
     * 
     * @return a CardList.
     */
    public final List<Card> getCardsCastLastTurn() {
        return this.lastTurnCast;
    }

    /**
     * Checks if is resolving.
     *
     * @param c the c
     * @return true, if is resolving
     */
    public final boolean isResolving(Card c) {
        if (!this.isResolving() || this.curResolvingCard == null) {
            return false;
        }

        return c.equals(this.curResolvingCard);
    }
}
