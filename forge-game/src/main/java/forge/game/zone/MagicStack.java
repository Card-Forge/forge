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

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingDeque;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.GameCommand;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventZone;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.util.TextUtil;

/**
 * <p>
 * MagicStack class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class MagicStack /* extends MyObservable */ implements Iterable<SpellAbilityStackInstance> {
    private final List<SpellAbility> simultaneousStackEntryList = Lists.newArrayList();
    private final List<SpellAbility> activePlayerSAs = Lists.newArrayList();

    // They don't provide a LIFO queue, so had to use a deque
    private final Deque<SpellAbilityStackInstance> stack = new LinkedBlockingDeque<>();
    private final Stack<SpellAbilityStackInstance> frozenStack = new Stack<>();
    private final Stack<SpellAbility> undoStack = new Stack<>();
    private Player undoStackOwner;

    private boolean frozen = false;
    private boolean bResolving = false;

    private final List<Card> thisTurnCast = Lists.newArrayList();
    private List<Card> lastTurnCast = Lists.newArrayList();
    private Card curResolvingCard = null;
    private final Map<String, List<GameCommand>> commandList = Maps.newHashMap();

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

    private int maxDistinctSources = 0;
    public int getMaxDistinctSources() { return maxDistinctSources; }
    public void resetMaxDistinctSources() { maxDistinctSources = 0; }

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
            if (si.isSpell() && si.getSourceCard().hasKeyword(Keyword.SPLIT_SECOND)) {
                return true;
            }
        }
        return false;
    }

    public final void freezeStack() {
        frozen = true;
    }

    public final void addAndUnfreeze(final SpellAbility ability) {
        final Card source = ability.getHostCard();

        // if the ability is a spell, but not a copied spell and its not already
        // on the stack zone, move there
        if (ability.isSpell() && !source.isCopiedSpell()) {
            if (!source.isInZone(ZoneType.Stack)) {
                ability.setHostCard(game.getAction().moveToStack(source, ability));
            }
            if (ability.equals(source.getCastSA())) {
                SpellAbility cause = ability.copy(source, true);

                cause.setLastStateBattlefield(game.getLastStateBattlefield());
                cause.setLastStateGraveyard(game.getLastStateGraveyard());

                source.setCastSA(cause);
            }
            source.cleanupExiledWith();
        }

        // Always add the ability here and always unfreeze the stack
        add(ability);
        unfreezeStack();
    }

    public final void unfreezeStack() {
        frozen = false;

        // Add all Frozen Abilities onto the stack
        while (!frozenStack.isEmpty()) {
            final SpellAbilityStackInstance si = frozenStack.pop();
            add(si.getSpellAbility(), si);
        }
        // Add all waiting triggers onto the stack
        game.getTriggerHandler().resetActiveTriggers();
        game.getTriggerHandler().runWaitingTriggers();
    }

    public final void clearFrozen() {
        // TODO: frozen triggered abilities and undoable costs have nasty consequences
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

        SpellAbility sa = undoStack.peek();
        sa.undo();
        clearUndoStack(sa);
        sa.getActivatingPlayer().getManaPool().refundManaPaid(sa);
        return true;
    }
    public final void clearUndoStack(SpellAbility sa) {
        clearUndoStack(Lists.newArrayList(sa));
    }
    private final void clearUndoStack(List<SpellAbility> sas) {
        for (SpellAbility sa : sas) {
            // reset in case a trigger stopped it on a previous activation
            sa.setUndoable(true);
            int idx = undoStack.lastIndexOf(sa);
            if (idx != -1) {
                undoStack.remove(idx);
            }
        }
        if (undoStack.isEmpty()) {
            undoStackOwner = null;
        }
    }
    public final void clearUndoStack() {
        if (undoStackOwner == null) { return; }
        clearUndoStack(Lists.newArrayList(undoStack));
        undoStackOwner = null;
    }
    public Iterable<SpellAbility> filterUndoStackByHost(final Card c) {
        return Iterables.filter(undoStack, CardTraitPredicates.isHostCard(c));
    }

    public final void add(SpellAbility sp) {
        add(sp, null);
    }
    public final void add(SpellAbility sp, SpellAbilityStackInstance si) {
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
            if (!canUndo(activator)) {
                clearUndoStack(); //clear if undo stack owner changes
                undoStackOwner = activator;
            }
            undoStack.push(sp);
        } else {
            clearUndoStack();
        }

        if (sp.isManaAbility()) { // Mana Abilities go straight through
            if (!sp.isCopied()) {
                // Copied abilities aren't activated, so they shouldn't change these values
                source.addAbilityActivated(sp);
            }
            Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(source.getController());
            runParams.put(AbilityKey.Cost, sp.getPayCosts());
            runParams.put(AbilityKey.Activator, activator);
            runParams.put(AbilityKey.SpellAbility, sp);
            game.getTriggerHandler().runTrigger(TriggerType.SpellAbilityCast, runParams, true);
            if (sp.isActivatedAbility()) {
                game.getTriggerHandler().runTrigger(TriggerType.AbilityCast, runParams, true);
            }

            AbilityUtils.resolve(sp);

            final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromCard(source);
            runParams2.put(AbilityKey.SpellAbility, sp);
            game.getTriggerHandler().runTrigger(TriggerType.AbilityResolves, runParams2, false);

            game.getGameLog().add(GameLogEntryType.MANA, source + " - " + sp.getDescription());
            sp.resetOnceResolved();
            return;
        }

        if (sp.isSpell()) {
            source.setController(activator, 0);

            if (source.isFaceDown() && !sp.isCastFaceDown()) {
                source.turnFaceUp(null);
            }

            // force the card be altered for alt states
            source.setSplitStateToPlayAbility(sp);

            // copied always add to stack zone
            if (source.isCopiedSpell()) {
                game.getStackZone().add(source);
            }
        }

        if (!sp.isCopied() && !hasLegalTargeting(sp)) {
            String str = source + " - [Couldn't add to stack, failed to target] - " + sp.getDescription();
            System.err.println(str + sp.getAllTargetChoices());
            game.getGameLog().add(GameLogEntryType.STACK_ADD, str);
            return;
        }

        //cancel auto-pass for all opponents of activating player
        //when a new non-triggered ability is put on the stack
        if (!sp.isTrigger()) {
            for (final Player p : activator.getOpponents()) {
                p.getController().autoPassCancel();
            }
        }

        if (si == null && sp.isActivatedAbility() && !sp.isCopied()) {
            // if not already copied use a fresh instance
            SpellAbility original = sp;
            sp = sp.copy();
            // need to reapply text changes
            sp.changeText();
            sp.setOriginalAbility(original);
            original.setXManaCostPaid(null);
            if (original.getApi() == ApiType.Charm) {
                // reset chain
                original.setSubAbility(null);
            }
        }

        if (frozen && !sp.hasParam("IgnoreFreeze")) {
            si = new SpellAbilityStackInstance(sp);
            frozenStack.push(si);
            return;
        }

        if (sp.isAbility() && !sp.isCopied()) {
            source.addAbilityActivated(sp);
        }

        if (sp instanceof AbilityStatic || (sp.isTrigger() && sp.getTrigger().getOverridingAbility() instanceof AbilityStatic)) {
            AbilityUtils.resolve(sp);
            // AbilityStatic should do nothing below
            return;
        }

        // The ability is added to stack HERE
        si = push(sp, si);

        // Copied spells aren't cast per se so triggers shouldn't run for them.
        Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(sp.getHostCard().getController());

        if (sp.isSpell() && !sp.isCopied()) {
            final Card lki = CardUtil.getLKICopy(sp.getHostCard());
            runParams.put(AbilityKey.CardLKI, lki);
            thisTurnCast.add(lki);
            sp.getActivatingPlayer().addSpellCastThisTurn();
        }

        runParams.put(AbilityKey.Cost, sp.getPayCosts());
        runParams.put(AbilityKey.Activator, sp.getActivatingPlayer());
        runParams.put(AbilityKey.SpellAbility, si.getSpellAbility());
        runParams.put(AbilityKey.CurrentStormCount, thisTurnCast.size());
        runParams.put(AbilityKey.CurrentCastSpells, Lists.newArrayList(thisTurnCast));

        if (!sp.isCopied()) {
            // Run SpellAbilityCast triggers
            game.getTriggerHandler().runTrigger(TriggerType.SpellAbilityCast, runParams, true);

            sp.applyPayingManaEffects();

            // Run SpellCast triggers
            if (sp.isSpell()) {
                if (source.isCommander() && source.getCastFrom() != null && ZoneType.Command == source.getCastFrom().getZoneType()
                        && source.getOwner().equals(activator)) {
                    activator.incCommanderCast(source);
                }
                game.getTriggerHandler().runTrigger(TriggerType.SpellCast, runParams, true);
                executeCastCommand(si.getSpellAbility().getHostCard());
            }

            // Run AbilityCast triggers
            if (sp.isActivatedAbility()) {
                game.getTriggerHandler().runTrigger(TriggerType.AbilityCast, runParams, true);
            }

            // Run Cycled triggers
            if (sp.isCycling()) {
                activator.addCycled(sp);
            }

            // Log number of Equips
            if (sp.isEquip()) {
                activator.addEquipped();
            }

            if (sp.hasParam("Crew")) {
                // Trigger crews!
                runParams.put(AbilityKey.Vehicle, sp.getHostCard());
                runParams.put(AbilityKey.Crew, sp.getPaidList("TappedCards", true));
                game.getTriggerHandler().runTrigger(TriggerType.Crewed, runParams, false);
            }
        } else {
            // Run Copy triggers
            if (sp.isSpell()) {
                game.getTriggerHandler().runTrigger(TriggerType.SpellCopy, runParams, false);
            }
            game.getTriggerHandler().runTrigger(TriggerType.SpellAbilityCopy, runParams, false);
        }
        if (sp.isSpell()) {
            game.getTriggerHandler().runTrigger(TriggerType.SpellCastOrCopy, runParams, false);
        }

        // Run BecomesTarget triggers
        // Create a new object, since the triggers aren't happening right away
        List<TargetChoices> chosenTargets = sp.getAllTargetChoices();
        if (!chosenTargets.isEmpty()) {
            runParams = AbilityKey.newMap();
            SpellAbility s = sp;
            if (si != null) {
                s = si.getSpellAbility();
                chosenTargets = s.getAllTargetChoices();
            }
            runParams.put(AbilityKey.SourceSA, s);
            Set<GameObject> distinctObjects = Sets.newHashSet();
            for (final TargetChoices tc : chosenTargets) {
                for (final GameObject tgt : tc) {
                    // Track distinct objects so Becomes targets don't trigger for things like:
                    // Seeds of Strength
                    if (!distinctObjects.add(tgt)) {
                        continue;
                    }

                    if (tgt instanceof Card && !((Card) tgt).hasBecomeTargetThisTurn()) {
                        runParams.put(AbilityKey.FirstTime, null);
                        ((Card) tgt).setBecameTargetThisTurn(true);
                    }
                    runParams.put(AbilityKey.Target, tgt);
                    game.getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
                }
            }
            runParams.put(AbilityKey.Targets, distinctObjects);
            runParams.put(AbilityKey.Cause, s.getHostCard());
            game.getTriggerHandler().runTrigger(TriggerType.BecomesTargetOnce, runParams, false);
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
    private SpellAbilityStackInstance push(final SpellAbility sp, SpellAbilityStackInstance si) {
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getHostCard().getController());
            System.out.println(sp.getHostCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        if (sp.isSpell() && sp.getMayPlay() != null) {
            sp.getMayPlay().incMayPlayTurn();
        }
        si = si == null ? new SpellAbilityStackInstance(sp) : si;

        stack.addFirst(si);
        int stackIndex = stack.size() - 1;

        int distinctSources = 0;
        Set<Integer> sources = new TreeSet<>();
        for (SpellAbilityStackInstance s : stack) {
            if (s.isSpell()) {
                distinctSources += 1;
            } else {
                sources.add(s.getSourceCard().getId());
            }
        }
        distinctSources += sources.size();
        if (distinctSources > maxDistinctSources) maxDistinctSources = distinctSources;

        // 2012-07-21 the following comparison needs to move below the pushes but somehow screws up priority
        // When it's down there. That makes absolutely no sense to me, so i'm putting it back for now
        if (!(sp.isTrigger() || (sp instanceof AbilityStatic))) {
            // when something is added we need to setPriority
            game.getPhaseHandler().setPriority(sp.getActivatingPlayer());
        }

        GameActionUtil.checkStaticAfterPaying(sp.getHostCard());

        if (sp.isActivatedAbility() && sp.isPwAbility()) {
            sp.getActivatingPlayer().setActivateLoyaltyAbilityThisTurn(true);
        }
        game.updateStackForView();
        game.fireEvent(new GameEventSpellAbilityCast(sp, si, stackIndex, false));
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

        // ActivePlayer gains priority first after Resolve
        game.getPhaseHandler().resetPriority();

        final Card source = sa.getHostCard();
        curResolvingCard = source;

        boolean thisHasFizzled = hasFizzled(sa, source, null);

        if (!thisHasFizzled) {
            game.copyLastState();
        }

        // Change controller of activating player if it was set in SA
        if (sa.getControlledByPlayer() != null) {
            sa.getActivatingPlayer().addController(sa.getControlledByPlayer().getLeft(), sa.getControlledByPlayer().getRight());
        }

        if (thisHasFizzled) { // Fizzle
            if (sa.isBestow()) {
                // 702.102e: if its target is illegal, the effect making it an Aura spell ends.
                // It continues resolving as a creature spell.
                source.unanimateBestow();
                SpellAbility first = source.getFirstSpellAbility();
                // need to set activating player
                first.setActivatingPlayer(sa.getActivatingPlayer());
                game.fireEvent(new GameEventCardStatsChanged(source));
                AbilityUtils.resolve(first);
            } else if (sa.isMutate()) {
                SpellAbility first = source.getFirstSpellAbility();
                // need to set activating player
                first.setActivatingPlayer(sa.getActivatingPlayer());
                game.fireEvent(new GameEventCardStatsChanged(source));
                AbilityUtils.resolve(first);
            } else {
                // TODO: Spell fizzles, what's the best way to alert player?
                Log.debug(source.getName() + " ability fizzles.");
            }
        } else if (sa.getApi() != null) {
            AbilityUtils.handleRemembering(sa);
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(source);
            runParams.put(AbilityKey.SpellAbility, sa);
            game.getTriggerHandler().runTrigger(TriggerType.AbilityResolves, runParams, false);
            AbilityUtils.resolve(sa);
        } else {
            sa.resolve();
            // do creatures ETB from here?
        }

        // Change controller back if it was changed
        if (sa.getControlledByPlayer() != null) {
            sa.getActivatingPlayer().removeController(sa.getControlledByPlayer().getLeft());
            // Cleanup controlled by player states
            sa.setControlledByPlayer(-1, null);
            sa.setManaCostBeingPaid(null);
        }

        game.fireEvent(new GameEventSpellResolved(sa, thisHasFizzled));
        finishResolving(sa, thisHasFizzled);

        game.copyLastState();
        if (isEmpty()) {
            // FIXME: assuming that if the stack is empty, no reason to hold on to old LKI data (everything is a new object). Is this correct?
            game.clearChangeZoneLKIInfo();
        }
    }

    private final void finishResolving(final SpellAbility sa, final boolean fizzle) {
        // SpellAbility is removed from the stack here
        // temporarily removed removing SA after resolution
        final SpellAbilityStackInstance si = getInstanceMatchingSpellAbilityID(sa);

        // remove SA and card from the stack
        removeCardFromStack(sa, si, fizzle);

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

        // xManaCostPaid will reset when cast the spell, comment out to fix Venarian Gold
        // sa.getHostCard().setXManaCostPaid(0);
    }

    private final void removeCardFromStack(final SpellAbility sa, final SpellAbilityStackInstance si, final boolean fizzle) {
        Card source = sa.getHostCard();

        // need to update active trigger
        game.getTriggerHandler().resetActiveTriggers();

        if (sa.isAbility()) {
            // do nothing
            return;
        }

        if (source.isCopiedSpell() && source.isInZone(ZoneType.Stack)) {
            game.getAction().ceaseToExist(source, true);
            return;
        }

        if ((source.isInstant() || source.isSorcery() || fizzle) &&
                source.isInZone(ZoneType.Stack)) {
            // If Spell and still on the Stack then let it goto the graveyard or replace its own movement
            Map<AbilityKey, Object> params = AbilityKey.newMap();
            params.put(AbilityKey.StackSa, sa);
            params.put(AbilityKey.StackSi, si);
            params.put(AbilityKey.Fizzle, fizzle);
            game.getAction().moveToGraveyard(source, null, params);
        }
    }

    public final boolean hasLegalTargeting(final SpellAbility sa) {
        if (sa == null) {
            return true;
        }
        if (!sa.isTargetNumberValid()) {
            return false;
        }
        return hasLegalTargeting(sa.getSubAbility());
    }

    private final boolean hasFizzled(final SpellAbility sa, final Card source, final Boolean parentFizzled) {
        // Can't fizzle unless there are some targets
        Boolean fizzle = null;
        boolean rememberTgt = sa.getRootAbility().hasParam("RememberOriginalTargets");

        if (sa.usesTargeting()) {
            if (sa.isZeroTargets()) {
                // Nothing targeted, and nothing needs to be targeted.
            } else {
                // Some targets were chosen, fizzling for this subability is now possible
                //fizzle = true;
                // With multi-targets, as long as one target is still legal,
                // we'll try to go through as much as possible
                final TargetChoices choices = sa.getTargets();
                for (final GameObject o : Lists.newArrayList(sa.getTargets())) {
                    boolean invalidTarget = false;
                    if (rememberTgt) {
                        source.addRemembered(o);
                    }
                    if (o instanceof Card) {
                        final Card card = (Card) o;
                        Card current = game.getCardState(card);
                        if (current != null) {
                            invalidTarget = current.getTimestamp() != card.getTimestamp();
                        }
                        invalidTarget = invalidTarget || !sa.canTarget(card);
                    } else if (o instanceof SpellAbility) {
                        SpellAbilityStackInstance si = getInstanceMatchingSpellAbilityID((SpellAbility)o);
                        invalidTarget = si == null ? true : !sa.canTarget(si.getSpellAbility());
                    } else {
                        invalidTarget = !sa.canTarget(o);
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
            fizzle = !sa.canTarget(sa.getTargetCard());
        } else {
            // Set fizzle to the same as the parent if there's no target info
            fizzle = parentFizzled;
        }

        if (sa.getSubAbility() == null) {
            if (fizzle != null && fizzle && rememberTgt) {
                source.clearRemembered();
            }
            return fizzle != null && fizzle.booleanValue();
        }
        return hasFizzled(sa.getSubAbility(), source, fizzle) && (fizzle == null || fizzle.booleanValue());
    }

    public final SpellAbilityStackInstance peek() {
        return stack.peekFirst();
    }

    public final SpellAbility peekAbility() {
        return stack.peekFirst().getSpellAbility();
    }

    public final void remove(final SpellAbilityStackInstance si) {
        stack.remove(si);
        frozenStack.remove(si);
        game.updateStackForView();
        SpellAbility sa = si.getSpellAbility();
        game.fireEvent(new GameEventSpellRemovedFromStack(sa));
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
        for (SpellAbility sa : Lists.newArrayList(simultaneousStackEntryList)) {
            Player activator = sa.getActivatingPlayer();
            if (activator == null) {
                if (sa.getHostCard().getController().equals(p)) {
                    simultaneousStackEntryList.remove(sa);
                }
            } else {
                if (activator.equals(p)) {
                    simultaneousStackEntryList.remove(sa);
                }
            }
        }
    }

    public final SpellAbilityStackInstance getInstanceMatchingSpellAbilityID(final SpellAbility sa) {
        for (final SpellAbilityStackInstance si : stack) {
            if (sa.getId() == si.getSpellAbility().getId()) {
                return si;
            }
        }
        return null;
    }

    public final SpellAbility getSpellMatchingHost(final Card host) {
        for (final SpellAbilityStackInstance si : stack) {
            if (si.isSpell() && host.equals(si.getSpellAbility().getHostCard())) {
                return si.getSpellAbility();
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

        if (playerTurn == null) {
            // caused by DevTools before first turn
            return false;
        }

        if (playerTurn.hasLost()) {
            playerTurn = game.getNextPlayerAfter(playerTurn);
        }

        Player whoAddsToStack = playerTurn;
        do {
            result |= chooseOrderOfSimultaneousStackEntry(whoAddsToStack, false);
            // 2014-08-10 Fix infinite loop when a player dies during a multiplayer game during their turn
            whoAddsToStack = game.getNextPlayerAfter(whoAddsToStack);
        } while (whoAddsToStack != null && whoAddsToStack != playerTurn);
        // 603.3b (Strict Proctor)
        whoAddsToStack = playerTurn;
        do {
            result |= chooseOrderOfSimultaneousStackEntry(whoAddsToStack, true);
            whoAddsToStack = game.getNextPlayerAfter(whoAddsToStack);
        } while (whoAddsToStack != null && whoAddsToStack != playerTurn);
        return result;
    }

    private final boolean chooseOrderOfSimultaneousStackEntry(final Player activePlayer, boolean isAbilityTriggered) {
        if (simultaneousStackEntryList.isEmpty()) {
            return false;
        }

        activePlayerSAs.clear();
        for (SpellAbility sa : simultaneousStackEntryList) {
            if (isAbilityTriggered != (sa.isTrigger() && sa.getTrigger().getMode() == TriggerType.AbilityTriggered)) {
                continue;
            }

            Player activator = sa.getActivatingPlayer();
            if (activator == null) {
                activator = sa.getHostCard().getController();
            }

            if (activator.equals(activePlayer)) {
                adjustAuraHost(sa);
                activePlayerSAs.add(sa);
            }
        }
        simultaneousStackEntryList.removeAll(activePlayerSAs);

        if (activePlayerSAs.isEmpty()) {
            return false;
        }

        activePlayer.getController().orderAndPlaySimultaneousSa(activePlayerSAs);
        activePlayerSAs.clear();
        return true;
    }

    // 400.7f Abilities of Auras that trigger when the enchanted permanent leaves the battlefield
    // can find the new object that Aura became in its owner’s graveyard
    public void adjustAuraHost(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Trigger trig = sa.getTrigger();
        final Card newHost = game.getCardState(host);
        if (host.isAura() && newHost.isInZone(ZoneType.Graveyard) && trig.getMode() == TriggerType.ChangesZone && 
                "Graveyard".equals(trig.getParam("Destination")) && "Card.EnchantedBy".equals(trig.getParam("ValidCard"))) {
            sa.setHostCard(newHost);
        }
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

        for (final SpellAbility sa : activePlayerSAs) {
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
        game.getStackZone().resetCardsAddedThisTurn();
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
        } else {
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

    public final boolean hasSourceOnStack(final Card source, final Predicate<SpellAbility> pred) {
        if (source == null) {
            return false;
        }
        for (SpellAbilityStackInstance si : stack) {
            if (si.isTrigger() && si.getSourceCard().equals(source)) {
                if (pred == null || pred.apply(si.getSpellAbility())) {
                    return true;
                }
            }
        }
        for (SpellAbility sa : simultaneousStackEntryList) {
            if (sa.isTrigger() && sa.getHostCard().equals(source)) {
                if (pred == null || pred.apply(sa)) {
                    return true;
                }
            }
        }
        return false;
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
        return TextUtil.concatNoSpace(simultaneousStackEntryList.toString(),"==", frozenStack.toString(), "==", stack.toString());
    }
}
