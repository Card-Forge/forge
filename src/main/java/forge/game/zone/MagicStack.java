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
import forge.FThreads;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.AbilityTriggered;
import forge.card.spellability.OptionalCost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.card.spellability.TargetChoices;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.InputPayManaExecuteCommands;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.event.SpellResolvedEvent;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.HumanPlayer;
import forge.game.player.Player;
import forge.gui.GuiChoose;
import forge.util.MyObservable;

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
            ability.getSourceCard().addHiddenExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
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
        checkState |= game.getTriggerHandler().runWaitingTriggers();
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
     * add.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void add(final SpellAbility sp) {
        FThreads.assertExecutedByEdt(false);
        final ArrayList<TargetChoices> chosenTargets = sp.getAllTargetChoices();

        if (sp.isManaAbility()) { // Mana Abilities go straight through
            AbilityUtils.resolve(sp, false);
            //sp.resolve();
            sp.resetOnceResolved();
            game.getGameLog().add("Mana", sp.getSourceCard() + " - " + sp.getDescription(), 4);
            return;
        }

        if (sp.isSpell()) {
            sp.getSourceCard().setController(sp.getActivatingPlayer(), 0);
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
            for (OptionalCost s : sp.getOptionalCosts()) {
                
                sp.getSourceCard().addOptionalCostPaid(s);
            }
            if (sp.getSourceCard().isCopiedSpell()) {
                this.push(sp);
            } else if (!sp.isMultiKicker() && !sp.isReplicate()) {
                this.push(sp);
            } else if (sp.isMultiKicker()) {
                final SpellAbility sa = sp;
                final Player activating = sp.getActivatingPlayer();

                if (activating.isHuman()) {
                    while(true) {
                        int mkMagnitude = sa.getSourceCard().getKickerMagnitude();
                        String prompt = String.format("Multikicker for %s\r\nTimes Kicked: %d\r\n", sa.getSourceCard(), mkMagnitude );
                        InputPayManaExecuteCommands toSet = new InputPayManaExecuteCommands(activating, prompt, sp.getMultiKickerManaCost());
                        FThreads.setInputAndWait(toSet);
                        if ( !toSet.isPaid() )
                            break;
                        
                        sa.getSourceCard().addMultiKickerMagnitude(1);
                    }
                } else {
                    // computer
                    final Ability abilityIncreaseMultikicker = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost()) {
                        @Override
                        public void resolve() {
                            this.getSourceCard().addMultiKickerMagnitude(1);
                        }
                    };

                    while (ComputerUtilCost.canPayCost(abilityIncreaseMultikicker, activating)) {
                        ComputerUtil.playNoStack((AIPlayer)activating, abilityIncreaseMultikicker, game);
                    }
                }
                this.push(sa);
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
                final Player controller = sp.getSourceCard().getController();
                ability.setActivatingPlayer(controller);
 

                if (controller.isHuman()) {
                    sa.getSourceCard().addReplicateMagnitude(-1);
                    final Runnable addMagnitude = new Runnable() {
                         @Override
                        public void run() {
                            ability.resolve();
                            String prompt = String.format("Replicate for %s\r\nTimes Replicated: %d\r\n", sa.getSourceCard(), sa.getSourceCard().getReplicateMagnitude());
                            InputPayManaExecuteCommands toSet = new InputPayManaExecuteCommands(controller, prompt, sp.getReplicateManaCost());
                            FThreads.setInputAndWait(toSet);
                            if ( toSet.isPaid() ) { 
                                this.run();
                            } else {
                                for (int i = 0; i < sp.getSourceCard().getReplicateMagnitude(); i++) {
                                    CardFactory.copySpellontoStack(sp.getSourceCard(), sp.getSourceCard(), sp, false);
                                }
                            }
                        }
                    };
                    addMagnitude.run();
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
        
        synchronized (this.stack) {
            this.getStack().push(si);
        }

        // 2012-07-21 the following comparison needs to move below the pushes but somehow screws up priority
        // When it's down there. That makes absolutely no sense to me, so i'm putting it back for now
        if (!((sp instanceof AbilityTriggered) || (sp instanceof AbilityStatic))) {
            // when something is added we need to setPriority
            game.getPhaseHandler().setPriority(sp.getActivatingPlayer());
        }

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

        // freeze the stack while we're in the middle of resolving
        this.freezeStack(); 
        this.setResolving(true);

        // The SpellAbility isn't removed from the Stack until it finishes resolving
        // temporarily reverted removing SAs after resolution
        final SpellAbility sa = this.top();
        //final SpellAbility sa = this.pop();

        // ActivePlayer gains priority first after Resolve
        game.getPhaseHandler().resetPriority(); 

        final Card source = sa.getSourceCard();
        curResolvingCard = source;
        
        boolean thisHasFizzled = this.hasFizzled(sa, source, false);
        String messageForLog = thisHasFizzled ? source.getName() + " ability fizzles." : sa.getStackDescription();
        game.getGameLog().add("ResolveStack", messageForLog, 2);
        if (thisHasFizzled) { // Fizzle
            // TODO: Spell fizzles, what's the best way to alert player?
            Log.debug(source.getName() + " ability fizzles.");
            this.finishResolving(sa, true);
        } else if (sa.getApi() != null) {
            AbilityUtils.handleRemembering(sa);
            AbilityUtils.resolve(sa, true);
        } else {
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
                haunterDiesWork.setActivatingPlayer(sa.getActivatingPlayer());
                if (!creats.get(i).canBeTargetedBy(haunterDiesWork)) {
                    creats.remove(i);
                    i--;
                }
            }
            if (!creats.isEmpty()) {
                haunterDiesWork.setDescription("");

                if (source.getController().isHuman()) {
                    final InputSelectCards targetHaunted = new InputSelectCardsFromList(1,1, creats);
                    targetHaunted.setMessage("Choose target creature to haunt.");
                    FThreads.setInputAndWait(targetHaunted);
                    haunterDiesWork.setTargetCard(targetHaunted.getSelected().get(0));
                    MagicStack.this.add(haunterDiesWork);
                } else {
                    // AI choosing what to haunt
                    final List<Card> oppCreats = CardLists.filterControlledBy(creats, source.getController().getOpponents());
                    haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(oppCreats.isEmpty() ? creats : oppCreats));
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

        // remove SA and card from the stack
        this.removeCardFromStack(sa, fizzle);
        // SpellAbility is removed from the stack here
        // temporarily removed removing SA after resolution
        this.remove(sa);

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
                        invalidTarget = !(sa.canTargetSpellAbility(tgtSA));
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
        synchronized(this.stack)
        {
            final SpellAbilityStackInstance si = this.getStack().pop();
            final SpellAbility sp = si.getSpellAbility();
            return sp;
        }
    }

    public final SpellAbility top() {
        final SpellAbilityStackInstance si = this.getStack().peek();
        final SpellAbility sa = si.getSpellAbility();
        return sa;
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
        synchronized (this.stack) {
            this.getStack().remove(si);
        }
        synchronized (this.frozenStack) {
            this.getFrozenStack().remove(si);
        }
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
            if (si.compareToSpellAbility(sa)) {
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
            List<SpellAbility> orderedSAs = activePlayerSAs;
            if (activePlayerSAs.size() > 1) { // give a dual list form to create instead of needing to do it one at a time
                orderedSAs = GuiChoose.order("Select order for Simultaneous Spell Abilities", "Resolve first", 0, activePlayerSAs, null, null);
            }
            int size = orderedSAs.size();
            for (int i = size - 1; i >= 0; i--) {
                SpellAbility next = orderedSAs.get(i);
                if (next.isTrigger()) {
                    ((HumanPlayer)activePlayer).playSpellAbility(next);
                } else {
                    this.add(next);
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
