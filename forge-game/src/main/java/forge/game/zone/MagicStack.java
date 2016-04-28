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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.Cost;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;

/**
 * <p>
 * MagicStack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MagicStack /* extends MyObservable */ implements Iterable<SpellAbilityStackInstance> {
    private final List<SpellAbility> simultaneousStackEntryList = new ArrayList<SpellAbility>();

    // They don't provide a LIFO queue, so had to use a deque
    private final Deque<SpellAbilityStackInstance> stack = new LinkedBlockingDeque<SpellAbilityStackInstance>();
    private final Stack<SpellAbilityStackInstance> frozenStack = new Stack<SpellAbilityStackInstance>();
    private final Stack<SpellAbility> undoStack = new Stack<SpellAbility>();
    private Player undoStackOwner;

    private boolean frozen = false;
    private boolean bResolving = false;

    private final List<Card> thisTurnCast = Lists.newArrayList();
    private List<Card> lastTurnCast = Lists.newArrayList();
    private Card curResolvingCard = null;
    private final HashMap<String, List<GameCommand>> commandList = new HashMap<String, List<GameCommand>>();

    private final Game game;

    public MagicStack(Game gameState) {
        game = gameState;
    }

    public final boolean isFrozen() {
        return frozen;
    }
    public final void setFrozen(final boolean frozen0) {
        frozen = frozen0;
    }

    public final void reset() {
        clear();
        simultaneousStackEntryList.clear();
        frozen = false;
        lastTurnCast.clear();
        thisTurnCast.clear();
        curResolvingCard = null;
        frozenStack.clear();
        clearUndoStack();
        game.updateStackForView();
    }

    public final boolean isSplitSecondOnStack() {
        for(SpellAbilityStackInstance si : stack) {
            if (si.isSpell() && si.getSourceCard().hasKeyword("Split second")) {
                return true;
            }
        }
        return false;
    }

    public final void freezeStack() {
        frozen = true;
    }

    public final void addAndUnfreeze(final SpellAbility ability) {
        if (!ability.isCopied()) {
            // Copied abilities aren't activated, so they shouldn't change these values
            ability.getRestrictions().abilityActivated();
            ability.checkActivationResloveSubs();
        }

        // if the ability is a spell, but not a copied spell and its not already
        // on the stack zone, move there
        if (ability.isSpell()) {
            final Card source = ability.getHostCard();
            if (!source.isCopiedSpell() && !source.isInZone(ZoneType.Stack)) {
                ability.setHostCard(game.getAction().moveToStack(source));
            }
        }

        // Always add the ability here and always unfreeze the stack
        add(ability);
        unfreezeStack();
    }

    public final void unfreezeStack() {
        frozen = false;

        // Add all Frozen Abilities onto the stack
        while (!frozenStack.isEmpty()) {
            final SpellAbility sa = frozenStack.pop().getSpellAbility(true);
            add(sa);
        }
        // Add all waiting triggers onto the stack
        game.getTriggerHandler().runWaitingTriggers();
    }

    public final void clearFrozen() {
        // TODO: frozen triggered abilities and undoable costs have nasty
        // consequences
        frozen = false;
        frozenStack.clear();
    }

    public final boolean isResolving() {
        return bResolving;
    }
    public final void setResolving(final boolean b) {
        bResolving = b;
    }

    public final boolean canUndo(Player player) {
        return undoStackOwner == player;
    }
    public final boolean undo() {
        if (undoStack.isEmpty()) { return false; }

        SpellAbility sa = undoStack.pop();
        sa.undo();
        sa.getActivatingPlayer().getManaPool().refundManaPaid(sa);
        if (undoStack.isEmpty()) {
            undoStackOwner = null;
        }
        return true;
    }
    public final void clearUndoStack() {
        if (undoStackOwner == null) { return; }
        undoStack.clear();
        undoStackOwner = null;
    }

    public final void add(final SpellAbility sp) {
        SpellAbilityStackInstance si = null;
        final Card source = sp.getHostCard();
        Player activator = sp.getActivatingPlayer();

        // if activating player slips through the cracks, assign activating
        // Player to the controller here
        if (null == activator) {
            sp.setActivatingPlayer(source.getController());
            activator = sp.getActivatingPlayer();
            System.out.println(source.getName() + " - activatingPlayer not set before adding to stack.");
        }

        //either push onto or clear undo stack based on whether spell/ability is undoable
        if (sp.isUndoable()) {
            if (undoStackOwner != activator) {
                clearUndoStack(); //clear if undo stack owner changes
                undoStackOwner = activator;
            }
            undoStack.push(sp);
        }
        else {
            clearUndoStack();
        }

        if (sp.isManaAbility()) { // Mana Abilities go straight through
            AbilityUtils.resolve(sp);
            game.getGameLog().add(GameLogEntryType.MANA, source + " - " + sp.getDescription());
            sp.resetOnceResolved();
            return;
        }

        if (sp.isSpell()) {
            source.setController(activator, 0);
            final Spell spell = (Spell) sp;
            if (spell.isCastFaceDown()) {
                source.turnFaceDown();
            } else if (source.isFaceDown()) {
                source.turnFaceUp();
            }
        }

        if (sp.getApi() == ApiType.Charm) {
            boolean remember = sp.hasParam("RememberChoice");
            if (remember) {
                // Remember the ChoiceName here for later handling
                source.addRemembered(sp.getSubAbility().getParam("ChoiceName"));
            }
        }

        //cancel auto-pass for all opponents of activating player
        //when a new non-triggered ability is put on the stack
        if (!sp.isTrigger()) { 
            for (final Player p : activator.getOpponents()) {
                p.getController().autoPassCancel();
            }
        }

        if (frozen) {
            si = new SpellAbilityStackInstance(sp);
            frozenStack.push(si);
            return;
        }
        int totManaSpent = sp.getPayingMana().size();

        if (sp instanceof AbilityStatic) {
            // TODO: make working triggered ability
            sp.setTotalManaSpent(totManaSpent);
            AbilityUtils.resolve(sp);
        }
        else {
            for (OptionalCost s : sp.getOptionalCosts()) {
                source.addOptionalCostPaid(s);
            }
            if (sp.isCopied()) {
                si = push(sp);
            }
            else {
                if (sp.isSpell() && source.isCreature() && Iterables.any(activator.getCardsIn(ZoneType.Battlefield),
                        CardPredicates.hasKeyword("As an additional cost to cast creature spells," +
                                " you may pay any amount of mana. If you do, that creature enters " +
                                "the battlefield with that many additional +1/+1 counters on it."))) {
                    final Cost costPseudoKicker = new Cost(ManaCost.ONE, false);
                    boolean hasPaid = false;
                    do {
                        int mkMagnitude = source.getPseudoKickerMagnitude();
                        String prompt = String.format("Additional Cost for %s\r\nTimes Kicked: %d\r\n", source, mkMagnitude );
                        hasPaid = activator.getController().payManaOptional(source, costPseudoKicker, sp, prompt, ManaPaymentPurpose.Multikicker);
                        if (hasPaid) {
                            source.addPseudoMultiKickerMagnitude(1);
                            totManaSpent += 1;
                        }
                    } while (hasPaid);
                    if (source.getPseudoKickerMagnitude() > 0) {
                        String abStr = "DB$ PutCounter | Defined$ Self | ETB$ True | CounterType$ P1P1 | CounterNum$ "
                                + source.getPseudoKickerMagnitude() + " | SubAbility$ ChorusDBETBCounters";
                        String dbStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                                + "| Defined$ ReplacedCard";
                        
                        source.setSVar("ChorusETBCounters", abStr);
                        source.setSVar("ChorusDBETBCounters", dbStr);

                        String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                                + "| ReplaceWith$ ChorusETBCounters | Secondary$ True | Description$ CARDNAME"
                                + " enters the battlefield with " + source.getPseudoKickerMagnitude() + " +1/+1 counters.";

                        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, source, false);
                        re.setLayer(ReplacementLayer.Other);

                        source.addReplacementEffect(re);
                    }
                }

                // The ability is added to stack HERE
                si = push(sp);

                if (sp.isSpell() && (source.hasStartOfKeyword("Replicate")
                        || ((source.isInstant() || source.isSorcery()) && Iterables.any(activator.getCardsIn(ZoneType.Battlefield),
                                CardPredicates.hasKeyword("Each instant and sorcery spell you cast has replicate. The replicate cost is equal to its mana cost."))))) {
                    Integer magnitude = sp.getSVarInt("Replicate");
                    if (magnitude == null) {
                        magnitude = 0;
                        final Cost costReplicate = new Cost(source.getManaCost(), false);
                        boolean hasPaid = false;
                        int replicateCMC = source.getManaCost().getCMC();
                        do {
                            String prompt = String.format("Replicate for %s\r\nTimes Replicated: %d\r\n", source, magnitude);
                            hasPaid = activator.getController().payManaOptional(source, costReplicate, sp, prompt, ManaPaymentPurpose.Replicate);
                            if (hasPaid) {
                                magnitude++;
                                totManaSpent += replicateCMC;
                            }
                        } while (hasPaid);
                    }

                    // Replicate Trigger
                    String effect = String.format("AB$ CopySpellAbility | Cost$ 0 | Defined$ SourceFirstSpell | Amount$ %d", magnitude);
                    SpellAbility sa = AbilityFactory.getAbility(effect, source);
                    sa.setDescription("Replicate - " + source);
                    sa.setTrigger(true);
                    sa.setCopied(true);
                    addSimultaneousStackEntry(sa);
                }
            }
        }

        sp.setTotalManaSpent(totManaSpent);

        // Copied spells aren't cast per se so triggers shouldn't run for them.
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        if (!(sp instanceof AbilityStatic) && !sp.isCopied()) {
            // Run SpellAbilityCast triggers
            runParams.put("Cost", sp.getPayCosts());
            runParams.put("Player", sp.getHostCard().getController());
            runParams.put("Activator", sp.getActivatingPlayer());
            runParams.put("CastSA", si.getSpellAbility(true));
            runParams.put("CastSACMC", si.getSpellAbility(true).getHostCard().getCMC());
            runParams.put("CurrentStormCount", thisTurnCast.size());
            game.getTriggerHandler().runTrigger(TriggerType.SpellAbilityCast, runParams, true);

            // Run SpellCast triggers
            if (sp.isSpell()) {
                game.getTriggerHandler().runTrigger(TriggerType.SpellCast, runParams, true);
                executeCastCommand(si.getSpellAbility(true).getHostCard());
            }

            // Run AbilityCast triggers
            if (sp.isAbility() && !sp.isTrigger()) {
                game.getTriggerHandler().runTrigger(TriggerType.AbilityCast, runParams, true);
            }

            // Run Cycled triggers
            if (sp.isCycling()) {
                runParams.clear();
                runParams.put("Card", sp.getHostCard());
                game.getTriggerHandler().runTrigger(TriggerType.Cycled, runParams, false);
            }
        }

        // Run BecomesTarget triggers
        // Create a new object, since the triggers aren't happening right away
        List<TargetChoices> chosenTargets = sp.getAllTargetChoices();
        if (!chosenTargets.isEmpty()) { 
            runParams = new HashMap<String, Object>();
            SpellAbility s = sp;
            if (si != null) {
                s = si.getSpellAbility(true);
                chosenTargets = s.getAllTargetChoices();
            }
            runParams.put("SourceSA", s);
            Set<Object> distinctObjects = new HashSet<Object>();
            for (final TargetChoices tc : chosenTargets) {
                if (tc != null && tc.getTargetCards() != null) {
                    for (final Object tgt : tc.getTargets()) {
                        // Track distinct objects so Becomes targets don't trigger for things like:
                        // Seeds of Strength
                        if (distinctObjects.contains(tgt)) {
                            continue;
                        }
                        
                        distinctObjects.add(tgt);
                        if (tgt instanceof Card && !((Card) tgt).hasBecomeTargetThisTurn()) {
                            runParams.put("FirstTime", null);
                            ((Card) tgt).setBecameTargetThisTurn(true);
                        }
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

        game.fireEvent(new GameEventZone(ZoneType.Stack, sp.getActivatingPlayer(), EventValueChangeType.Added, source));

        if (sp.getActivatingPlayer() != null && !game.getCardsPlayerCanActivateInStack().isEmpty()) {
            // This is a bit of a hack that forces the update of externally activatable cards in flashback zone (e.g. Lightning Storm).
            for (Player p : game.getPlayers()) {
                p.updateFlashbackForView();
            }
        }
    }

    public final int size() {
        return stack.size();
    }

    public final boolean isEmpty() {
        return stack.isEmpty();
    }

    // Push should only be used by add.
    private SpellAbilityStackInstance push(final SpellAbility sp) {
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getHostCard().getController());
            System.out.println(sp.getHostCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        final SpellAbilityStackInstance si = new SpellAbilityStackInstance(sp);

        stack.addFirst(si);

        // 2012-07-21 the following comparison needs to move below the pushes but somehow screws up priority
        // When it's down there. That makes absolutely no sense to me, so i'm putting it back for now
        if (!(sp.isTrigger() || (sp instanceof AbilityStatic))) {
            // when something is added we need to setPriority
            game.getPhaseHandler().setPriority(sp.getActivatingPlayer());
        }

        if (sp.isSpell() && !sp.isCopied()) {
            thisTurnCast.add(sp.getHostCard());
            sp.getActivatingPlayer().addSpellCastThisTurn();
        }
        if (sp.isAbility() && sp.getRestrictions().isPwAbility()) {
            sp.getActivatingPlayer().setActivateLoyaltyAbilityThisTurn(true);
        }
        game.updateStackForView();
        game.fireEvent(new GameEventSpellAbilityCast(sp, si, false));
        return si;
    }

    public final void resolveStack() {
        // Resolving the Stack

        // freeze the stack while we're in the middle of resolving
        freezeStack(); 
        setResolving(true);

        // The SpellAbility isn't removed from the Stack until it finishes resolving
        // temporarily reverted removing SAs after resolution
        final SpellAbility sa = peekAbility();
        //final SpellAbility sa = pop();

        // ActivePlayer gains priority first after Resolve
        game.getPhaseHandler().resetPriority(); 

        final Card source = sa.getHostCard();
        curResolvingCard = source;
        
        boolean thisHasFizzled = hasFizzled(sa, source, null);
        
        if (thisHasFizzled) { // Fizzle
            if (sa.hasParam("Bestow")) {
                // 702.102d: if its target is illegal, 
                // the effect making it an Aura spell ends. 
                // It continues resolving as a creature spell.
                source.unanimateBestow();
                game.fireEvent(new GameEventCardStatsChanged(source));
                AbilityUtils.resolve(sa.getHostCard().getFirstSpellAbility());
            } else {
                // TODO: Spell fizzles, what's the best way to alert player?
                Log.debug(source.getName() + " ability fizzles.");
            }
        } else if (sa.getApi() != null) {
            AbilityUtils.handleRemembering(sa);
            AbilityUtils.resolve(sa);
        } else {
            sa.resolve();
            // do creatures ETB from here?
        }
        
        game.fireEvent(new GameEventSpellResolved(sa, thisHasFizzled));
        finishResolving(sa, thisHasFizzled);

        if (source.hasStartOfKeyword("Haunt") && !source.isCreature() && game.getZoneOf(source).is(ZoneType.Graveyard)) {
            handleHauntForNonPermanents(sa);
        }

        if (isEmpty()) {
            // FIXME: assuming that if the stack is empty, no reason to hold on to old LKI data (everything is a new object). Is this correct?
            game.clearChangeZoneLKIInfo();
        }
    }

    private void handleHauntForNonPermanents(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final CardCollection creats = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
        final Ability haunterDiesWork = new Ability(source, ManaCost.ZERO) {
            @Override
            public void resolve() {
                game.getAction().exile(source);
                getTargetCard().addHauntedBy(source);
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
            haunterDiesWork.setTargetRestrictions(new TargetRestrictions("", "Creature".split(" "), "1", "1"));
            final Card targetCard = source.getController().getController().chooseSingleEntityForEffect(creats, new SpellAbility.EmptySa(ApiType.InternalHaunt, source), "Choose target creature to haunt.");
            haunterDiesWork.setTargetCard(targetCard);
            add(haunterDiesWork);
        }
    }

    private final void finishResolving(final SpellAbility sa, final boolean fizzle) {
        // remove SA and card from the stack
        removeCardFromStack(sa, fizzle);
        // SpellAbility is removed from the stack here
        // temporarily removed removing SA after resolution
        final SpellAbilityStackInstance si = getInstanceFromSpellAbility(sa);
        
        if (si != null) {
            remove(si);
        }

        // After SA resolves we have to do a handful of things
        setResolving(false);
        unfreezeStack();
        sa.resetOnceResolved();

        //game.getAction().checkStaticAbilities();
        game.getPhaseHandler().onStackResolved();

        curResolvingCard = null;

        // TODO: this is a huge hack. Why is this necessary?
        // hostCard in AF is not the same object that's on the battlefield
        // verified by System.identityHashCode(card);
        final Card tmp = sa.getHostCard();
        tmp.setCanCounter(true); // reset mana pumped counter magic flag
        if (tmp.getClones().size() > 0) {
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.equals(tmp)) {
                    c.setClones(tmp.getClones());
                }
            }
        }
        // xManaCostPaid will reset when cast the spell, comment out to fix Venarian Gold
        // sa.getHostCard().setXManaCostPaid(0);
    }

    private final void removeCardFromStack(final SpellAbility sa, final boolean fizzle) {
        Card source = sa.getHostCard();

        if (source.isCopiedSpell() || sa.isAbility()) {
            // do nothing
        }
        else if ((source.hasKeyword("Move CARDNAME to your hand as it resolves") || sa.isBuyBackAbility()) && !fizzle) {
            // Handle cards that need to be moved differently
            // TODO: replacement effects: Rebound, Buyback and Soulfire Grand Master
            source.removeAllExtrinsicKeyword("Move CARDNAME to your hand as it resolves");
            game.getAction().moveToHand(source);
        }
        else if (sa.isFlashBackAbility()) {
            game.getAction().exile(source);
            sa.setFlashBackAbility(false);
        }
        else if (source.hasKeyword("Rebound")
                && !fizzle
                && source.getCastFrom() == ZoneType.Hand
                && game.getZoneOf(source).is(ZoneType.Stack)
                && source.getOwner().equals(source.getController())) //"If you cast this spell from your hand"
        {
            //Move rebounding card to exile
            source = game.getAction().exile(source);

            source.setSVar("ReboundAbilityTrigger", "DB$ Play | Defined$ Self "
                    + "| WithoutManaCost$ True | Optional$ True");

            //Setup a Rebound-trigger
            final Trigger reboundTrigger = forge.game.trigger.TriggerHandler.parseTrigger("Mode$ Phase "
                    + "| Phase$ Upkeep | ValidPlayer$ You | OptionalDecider$ You | Execute$ ReboundAbilityTrigger "
                    + "| TriggerDescription$ At the beginning of your next upkeep, you may cast " + source.toString()
                    + " without paying it's manacost.", source, true);

            game.getTriggerHandler().registerDelayedTrigger(reboundTrigger);
        }
        else if (!source.isCopiedSpell() &&
                (source.isInstant() || source.isSorcery() || fizzle) &&
                source.isInZone(ZoneType.Stack)) {
            // If Spell and still on the Stack then let it goto the graveyard or replace its own movement
            game.getAction().moveToGraveyard(source);
        }
    }

    private final boolean hasFizzled(final SpellAbility sa, final Card source, final Boolean parentFizzled) {
        // Check if the spellability is a trigger that was invalidated with fizzleTriggersOnStackTargeting
        if (sa.getSVar("TriggerFizzled").equals("True")) {
            return true;
        }

        // Can't fizzle unless there are some targets
        Boolean fizzle = null;
        boolean rememberTgt = sa.getRootAbility().hasParam("RememberOriginalTargets");

        TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            if (tgt.getMinTargets(source, sa) == 0 && sa.getTargets().getNumTargeted() == 0) {
                // Nothing targeted, and nothing needs to be targeted.
            }
            else {
                // Some targets were chosen, fizzling for this subability is now possible
                //fizzle = true;
                // With multi-targets, as long as one target is still legal,
                // we'll try to go through as much as possible
                final TargetChoices choices = sa.getTargets();
                for (final GameObject o : sa.getTargets().getTargets()) {
                    boolean invalidTarget = false;
                    if (rememberTgt) {
                        source.addRemembered(o);
                    }
                    if (o instanceof Card) {
                        final Card card = (Card) o;
                        Card current = game.getCardState(card);
                        invalidTarget = current.getTimestamp() != card.getTimestamp();
                        invalidTarget |= !(CardFactoryUtil.isTargetStillValid(sa, card));
                    } else {
                        invalidTarget = !o.canBeTargetedBy(sa);

                        if (o instanceof SpellAbility) {
                            invalidTarget |= this.getInstanceFromSpellAbility((SpellAbility)o) == null;
                        }
                    }
                    // Remove targets
                    if (invalidTarget) {
                        choices.remove(o);
                    } else {
                        fizzle = false;
                    }

                    if (sa.hasParam("CantFizzle")) {
                        // Gilded Drake cannot be countered by rules if the
                        // targeted card is not valid
                        fizzle = false;
                    }
                }
                if (fizzle == null) {
                    fizzle = true;
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
            if (fizzle != null && fizzle && rememberTgt) {
                source.clearRemembered();
            }
            return fizzle == null ? false : fizzle.booleanValue();
        }
        return hasFizzled(sa.getSubAbility(), source, fizzle) && (fizzle == null || fizzle.booleanValue());
    }

    public final SpellAbilityStackInstance peek() {
        return stack.peekFirst();
    }

    public final SpellAbility peekAbility() {
        return stack.peekFirst().getSpellAbility(true);
    }

    public final void remove(final SpellAbilityStackInstance si) {
        stack.remove(si);
        frozenStack.remove(si);
        game.updateStackForView();
        game.fireEvent(new GameEventSpellRemovedFromStack(si.getSpellAbility(true)));
    }

    public final void remove(final Card c) {
        for (SpellAbilityStackInstance si : stack) {
            if (c.equals(si.getSourceCard()) && si.isSpell()) {
                remove(si);
            }
        }
    }

    public final void removeInstancesControlledBy(final Player p) {
        for (SpellAbilityStackInstance si : stack) {
            if (si.getActivatingPlayer().equals(p)) {
                remove(si);
            }
        }
    }

    public void fizzleTriggersOnStackTargeting(Card c, TriggerType t) {
        for (SpellAbilityStackInstance si : stack) {
            SpellAbility sa = si.getSpellAbility(false);
            if (sa.getTriggeringObjects().containsKey("Target") && sa.getTriggeringObjects().get("Target").equals(c)) {
                if (sa instanceof WrappedAbility) {
                    WrappedAbility wi = (WrappedAbility)sa;
                    if (wi.getTrigger().getMode() == t) {
                        sa.setSVar("TriggerFizzled", "True");
                    }
                }
            }
        }
    }

    public final SpellAbilityStackInstance getInstanceFromSpellAbility(final SpellAbility sa) {
        // TODO: Confirm this works!
        for (final SpellAbilityStackInstance si : stack) {
            if (si.compareToSpellAbility(sa)) {
                return si;
            }
        }
        return null;
    }

    public final boolean hasSimultaneousStackEntries() {
        return !simultaneousStackEntryList.isEmpty();
    }

    public final void clearSimultaneousStack() {
        simultaneousStackEntryList.clear();
    }

    public final void addSimultaneousStackEntry(final SpellAbility sa) {
        simultaneousStackEntryList.add(sa);
    }

    public boolean addAllTriggeredAbilitiesToStack() {
        boolean result = false;
        Player playerTurn = game.getPhaseHandler().getPlayerTurn();

        if (playerTurn.hasLost()) {
            playerTurn = game.getNextPlayerAfter(playerTurn);
        }

        Player whoAddsToStack = playerTurn;
        do {
            result |= chooseOrderOfSimultaneousStackEntry(whoAddsToStack);
            // 2014-08-10 Fix infinite loop when a player dies during a multiplayer game during their turn
            whoAddsToStack = game.getNextPlayerAfter(whoAddsToStack);
        } while( whoAddsToStack != null && whoAddsToStack != playerTurn);
        return result;
    }

    private final boolean chooseOrderOfSimultaneousStackEntry(final Player activePlayer) {
        if (simultaneousStackEntryList.isEmpty()) {
            return false;
        }

        final List<SpellAbility> activePlayerSAs = new ArrayList<SpellAbility>();
        for (int i = 0; i < simultaneousStackEntryList.size(); i++) {
            SpellAbility sa = simultaneousStackEntryList.get(i);
            Player activator = sa.getActivatingPlayer();
            if (activator == null) {
                if (sa.getHostCard().getController().equals(activePlayer)) {
                    activePlayerSAs.add(sa);
                    simultaneousStackEntryList.remove(i);
                    i--;
                }
            } else {
                if (activator.equals(activePlayer)) {
                    activePlayerSAs.add(sa);
                    simultaneousStackEntryList.remove(i);
                    i--;
                }
            }
        }
        if (activePlayerSAs.isEmpty()) {
            return false;
        }

        activePlayer.getController().orderAndPlaySimultaneousSa(activePlayerSAs);
        return true;
    }

    public final boolean hasStateTrigger(final int triggerID) {
        for (final SpellAbilityStackInstance sasi : stack) {
            if (sasi.isStateTrigger(triggerID)) {
                return true;
            }
        }

        for (final SpellAbilityStackInstance sasi : frozenStack) {
            if (sasi.isStateTrigger(triggerID)) {
                return true;
            }
        }

        for (final SpellAbility sa : simultaneousStackEntryList) {
            if (sa.getSourceTrigger() == triggerID) {
                return true;
            }
        }
        return false;
    }

    public final List<Card> getSpellsCastThisTurn() {
        return thisTurnCast;
    }

    public final void onNextTurn() {
        if (thisTurnCast.isEmpty()) {
            lastTurnCast = Lists.newArrayList();
            return;
        }
        lastTurnCast = Lists.newArrayList(thisTurnCast);
        thisTurnCast.clear();
        game.updateStackForView();
    }

    public final List<Card> getSpellsCastLastTurn() {
        return lastTurnCast;
    }

    public final void addCastCommand(final String valid, final GameCommand c) {
        if (commandList.containsKey(valid)) {
            commandList.get(valid).add(0, c);
        }
        else {
            commandList.put(valid, Lists.newArrayList(c));
        }
    }

    private void executeCastCommand(final Card cast) {
        for (Entry<String, List<GameCommand>> ev : commandList.entrySet()) {
            if (cast.getType().hasStringType(ev.getKey())) {
                execute(ev.getValue());
            }
        }
    }

    private static void execute(final List<GameCommand> c) {
        final int length = c.size();
        for (int i = 0; i < length; i++) {
            c.remove(0).run();
        }
    }

    public final boolean isResolving(Card c) {
        if (!isResolving() || curResolvingCard == null) {
            return false;
        }
        return c.equals(curResolvingCard);
    }

    @Override
    public Iterator<SpellAbilityStackInstance> iterator() {
        return stack.iterator();
    }

    public Iterator<SpellAbilityStackInstance> reverseIterator() {
        return stack.descendingIterator();
    }

    public void clear() {
        if (stack.isEmpty()) { return; }
        stack.clear();
        game.updateStackForView();
        game.fireEvent(new GameEventSpellRemovedFromStack(null));
    }
    
    @Override 
    public String toString() {
        return String.format("%s==%s==%s", simultaneousStackEntryList, frozenStack.toString(), stack.toString()); 
    }
}
