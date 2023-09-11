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
package forge.game.mana;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityKey;
import forge.game.cost.CostPayment;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventManaPool;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityUnspentMana;
import forge.game.zone.ZoneType;

/**
 * <p>
 * ManaPool class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class ManaPool extends ManaConversionMatrix implements Iterable<Mana> {
    private final Player owner;
    private final ArrayListMultimap<Byte, Mana> floatingMana = ArrayListMultimap.create();

    public ManaPool(final Player player) {
        owner = player;
        restoreColorReplacements();
    }

    public final int getAmountOfColor(final byte color) {
        Collection<Mana> ofColor = floatingMana.get(color);
        return ofColor == null ? 0 : ofColor.size();
    }

    public void addMana(final Mana mana) {
        addMana(mana, true);
    }
    public void addMana(final Mana mana, boolean updateView) {
        floatingMana.put(mana.getColor(), mana);
        if (updateView) {
            owner.updateManaForView();
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Added, mana));
        }
    }

    public final void add(final Iterable<Mana> manaList) {
        for (final Mana m : manaList) {
            addMana(m);
        }
    }

    /**
     * <p>
     * willManaBeLostAtEndOfPhase.
     *
     * @return - whether floating mana will be lost if the current phase ended right now
     * </p>
     */
    public final boolean willManaBeLostAtEndOfPhase() {
        if (floatingMana.isEmpty()) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        if (!owner.getGame().getReplacementHandler().getReplacementList(ReplacementType.LoseMana, runParams, ReplacementLayer.Other).isEmpty()) {
            return false;
        }

        int safeMana = 0;
        for (final byte c : StaticAbilityUnspentMana.getManaToKeep(owner)) {
            safeMana += getAmountOfColor(c);
        }

        // TODO isPersistentMana

        return totalMana() != safeMana; //won't lose floating mana if all mana is of colors that aren't going to be emptied
    }

    public final boolean hasBurn() {
        final Game game = owner.getGame();
        return game.getRules().hasManaBurn() || game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.manaBurn);
    }

    public final List<Mana> clearPool(boolean isEndOfPhase) {
        // isEndOfPhase parameter: true = end of phase, false = mana drain effect
        List<Mana> cleared = Lists.newArrayList();
        if (floatingMana.isEmpty()) { return cleared; }

        Byte convertTo = null;

        // TODO move this lower in case all mana would be persistent
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        runParams.put(AbilityKey.Mana, "C");
        switch (owner.getGame().getReplacementHandler().run(ReplacementType.LoseMana, runParams)) {
        case NotReplaced:
            break;
        case Skipped:
            return cleared;
        default:
            convertTo = ManaAtom.fromName((String) runParams.get(AbilityKey.Mana));
            break;

        }

        final List<Byte> keys = Lists.newArrayList(floatingMana.keySet());
        if (isEndOfPhase) {
            keys.removeAll(StaticAbilityUnspentMana.getManaToKeep(owner));
        }
        if (convertTo != null) {
            keys.remove(convertTo);
        }

        for (Byte b : keys) {
            Collection<Mana> cm = floatingMana.get(b);
            final List<Mana> pMana = Lists.newArrayList();
            if (isEndOfPhase && !owner.getGame().getPhaseHandler().is(PhaseType.CLEANUP)) {
                for (final Mana mana : cm) {
                    if (mana.getManaAbility() != null && mana.getManaAbility().isPersistentMana()) {
                        pMana.add(mana);
                    }
                }
            }
            cm.removeAll(pMana);
            if (convertTo != null) {
                convertManaColor(b, convertTo);
                cm.addAll(pMana);
            } else {
                cleared.addAll(cm);
                cm.clear();
                floatingMana.putAll(b, pMana);
            }
        }

        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
        return cleared;
    }

    private void convertManaColor(final byte originalColor, final byte toColor) {
        List<Mana> convert = Lists.newArrayList();
        Collection<Mana> cm = floatingMana.get(originalColor);
        for (Mana m : cm) {
            convert.add(new Mana(toColor, m.getSourceCard(), m.getManaAbility()));
        }
        cm.clear();
        floatingMana.putAll(toColor, convert);
        owner.updateManaForView();
    }

    public boolean removeMana(final Mana mana) {
        return removeMana(mana, true);
    }
    public boolean removeMana(final Mana mana, boolean updateView) {
        boolean success = false;
        // make sure to remove the most recent in case of rollback
        int lastIdx = floatingMana.get(mana.getColor()).lastIndexOf(mana);
        if (lastIdx != -1) {
            success = floatingMana.get(mana.getColor()).remove(lastIdx) != null;
        }
        if (success && updateView) {
            owner.updateManaForView();
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Removed, mana));
        }
        return success;
    }

    public final void payManaFromAbility(final SpellAbility saPaidFor, ManaCostBeingPaid manaCost, final SpellAbility saPayment) {
        // Mana restriction must be checked before this method is called
        final List<SpellAbility> paidAbs = saPaidFor.getPayingManaAbilities();

        paidAbs.add(saPayment); // assumes some part on the mana produced by the ability will get used

        // need to get all mana from all ManaAbilities of the SpellAbility
        for (AbilityManaPart mp : saPayment.getAllManaParts()) {
            for (final Mana mana : mp.getLastManaProduced()) {
                if (!saPaidFor.allowsPayingWithShard(mp.getSourceCard(), mana.getColor())) {
                    continue;
                }
                if (tryPayCostWithMana(saPaidFor, manaCost, mana, false)) {
                    saPaidFor.getPayingMana().add(mana);
                }
            }
        }
    }

    public boolean tryPayCostWithColor(byte colorCode, SpellAbility saPaidFor, ManaCostBeingPaid manaCost, List<Mana> manaSpentToPay) {
        Mana manaFound = null;
        Collection<Mana> cm = floatingMana.get(colorCode);

        for (final Mana mana : cm) {
            if (mana.getManaAbility() != null && !mana.getManaAbility().meetsManaRestrictions(saPaidFor)) {
                continue;
            }

            if (!saPaidFor.allowsPayingWithShard(mana.getSourceCard(), colorCode)) {
                continue;
            }

            manaFound = mana;
            break;
        }

        if (manaFound != null && tryPayCostWithMana(saPaidFor, manaCost, manaFound, false)) {
            manaSpentToPay.add(manaFound);
            return true;
        }
        return false;
    }

    public boolean tryPayCostWithMana(final SpellAbility sa, ManaCostBeingPaid manaCost, final Mana mana, boolean test) {
        if (!manaCost.isNeeded(mana, this)) {
            return false;
        }
        // only pay mana into manaCost when the Mana could be removed from the Mana pool
        // if the mana wasn't in the mana pool then something is wrong
        if (!removeMana(mana)) {
            return false;
        }
        manaCost.payMana(mana, this);

        return true;
    }

    public final boolean isEmpty() {
        return floatingMana.isEmpty();
    }

    public final int totalMana() {
        return floatingMana.values().size();
    }

    //Account for mana part of ability when undoing it
    public boolean accountFor(final AbilityManaPart ma) {
        if (ma == null) {
            return false;
        }
        if (floatingMana.isEmpty()) {
            return false;
        }

        final List<Mana> removeFloating = Lists.newArrayList();

        boolean manaNotAccountedFor = false;
        // loop over mana produced by mana ability
        for (Mana mana : ma.getLastManaProduced()) {
            Collection<Mana> poolLane = floatingMana.get(mana.getColor());

            if (poolLane != null && poolLane.contains(mana)) {
                removeFloating.add(mana);
            } else {
                manaNotAccountedFor = true;
                break;
            }
        }

        // When is it legitimate for all the mana not to be accountable?
        // TODO: Does this condition really indicate an bug in Forge?
        if (manaNotAccountedFor) {
            return false;
        }

        for (Mana m : removeFloating) {
            removeMana(m);
        }
        return true;
    }

    public static void refundMana(List<Mana> manaSpent, Player player, SpellAbility sa) {
        player.getManaPool().add(manaSpent);
        manaSpent.clear();
    }

    public final void refundManaPaid(final SpellAbility sa) {
        Player p = sa.getActivatingPlayer();

        // Send all mana back to your mana pool, before accounting for it.

        // move non-undoable paying mana back to floating
        refundMana(sa.getPayingMana(), owner, sa);

        List<SpellAbility> payingAbilities = sa.getPayingManaAbilities();

        // start with the most recent
        Collections.reverse(payingAbilities);

        for (final SpellAbility am : payingAbilities) {
            // undo paying abilities if we can
            am.undo();
        }

        for (final SpellAbility am : payingAbilities) {
            // Recursively refund abilities that were used.
            refundManaPaid(am);
            p.getGame().getStack().clearUndoStack(am);
        }

        payingAbilities.clear();

        // update battlefield of activating player - to redraw cards used to pay mana as untapped
        p.getGame().fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
    }

    public boolean canPayForShardWithColor(ManaCostShard shard, byte color) {
        if (shard.isOfKind(ManaAtom.COLORLESS) && color == ManaAtom.GENERIC) {
            return false; // FIXME: testing Colorless against Generic is a recipe for disaster, but probably there should be a better fix.
        }

        byte line = getPossibleColorUses(color);

        for (byte outColor : ManaAtom.MANATYPES) {
            if ((line & outColor) != 0 && shard.canBePaidWithManaOfColor(outColor)) {
                return true;
            }
        }

        return shard.canBePaidWithManaOfColor((byte)0);
    }

    /**
     * Checks if the given mana cost can be paid from floating mana.
     * @param cost mana cost to pay for
     * @param sa ability to pay for
     * @param player activating player
     * @param test actual payment is made if this is false
     * @param manaSpentToPay list of mana spent
     * @return whether the floating mana is sufficient to pay the cost fully
     */
    public static boolean payManaCostFromPool(final ManaCostBeingPaid cost, final SpellAbility sa, final Player player,
            final boolean test, List<Mana> manaSpentToPay) {
        final boolean hasConverge = sa.getHostCard().hasConverge();
        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = CostPayment.getMana(player, part, sa, hasConverge ? cost.getColorsPaid() : -1, cost.getXManaCostPaidByColor());
                if (mana != null) {
                    if (player.getManaPool().tryPayCostWithMana(sa, cost, mana, test)) {
                        manaSpentToPay.add(mana);
                    }
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            if (test) {
                refundMana(manaSpentToPay, player, sa);
            }
            CostPayment.handleOfferings(sa, test, cost.isPaid());
            return true;
        }
        return false;
    }

    @Override
    public Iterator<Mana> iterator() {
        return floatingMana.values().iterator();
    }

}
