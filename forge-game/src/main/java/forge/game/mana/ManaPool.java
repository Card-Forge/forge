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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.GlobalRuleChange;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventManaPool;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
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
    private final Multimap<Byte, Mana> floatingMana = ArrayListMultimap.create();

    public ManaPool(final Player player) {
        owner = player;
        restoreColorReplacements();
    }

    public final int getAmountOfColor(final byte color) {
        Collection<Mana> ofColor = floatingMana.get(color);
        return ofColor == null ? 0 : ofColor.size();
    }

    public void addMana(final Mana mana) {
        floatingMana.put(mana.getColor(), mana);
        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Added, mana));
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
        if (floatingMana.isEmpty() ||
                owner.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.manapoolsDontEmpty) ||
                owner.hasKeyword("Convert unused mana to Colorless")) {
            return false;
        }

        int safeMana = 0;
        for (final byte c : MagicColor.WUBRG) {
            final String captName = StringUtils.capitalize(MagicColor.toLongString(c));
            if (owner.hasKeyword(captName + " mana doesn't empty from your mana pool as steps and phases end.")) {
                safeMana += getAmountOfColor(c);
            }
        }

        return totalMana() != safeMana; //won't lose floating mana if all mana is of colors that aren't going to be emptied
    }

    public final List<Mana> clearPool(boolean isEndOfPhase) {
        // isEndOfPhase parameter: true = end of phase, false = mana drain effect
        List<Mana> cleared = new ArrayList<>();
        if (floatingMana.isEmpty()) { return cleared; }

        if (isEndOfPhase && owner.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.manapoolsDontEmpty)) {
            return cleared;
        }

        final boolean convertToColorless = owner.hasKeyword("Convert unused mana to Colorless");

        final List<Byte> keys = Lists.newArrayList(floatingMana.keySet());
        if (isEndOfPhase) {
            for (final Byte c : Lists.newArrayList(keys)) {
                final String captName = StringUtils.capitalize(MagicColor.toLongString(c));
                if (owner.hasKeyword(captName + " mana doesn't empty from your mana pool as steps and phases end.")) {
                    keys.remove(c);
                }
            }
        }
        if (convertToColorless) {
            keys.remove(Byte.valueOf((byte)ManaAtom.COLORLESS));
        }

        for (Byte b : keys) {
            Collection<Mana> cm = floatingMana.get(b);
            if (isEndOfPhase && !owner.getGame().getPhaseHandler().is(PhaseType.CLEANUP)) {
                final List<Mana> pMana = new ArrayList<>();
                for (final Mana mana : cm) {
                    if (mana.getManaAbility()!= null && mana.getManaAbility().isPersistentMana()) {
                        pMana.add(mana);
                    }
                }
                cm.removeAll(pMana);
                if (convertToColorless) {
                    convertManaColor(b, (byte)ManaAtom.COLORLESS);
                    cm.addAll(pMana);
                } else {
                    cleared.addAll(cm);
                    cm.clear();
                    floatingMana.putAll(b, pMana);
                }
            } else {
                if (convertToColorless) {
                    convertManaColor(b, (byte)ManaAtom.COLORLESS);
                } else {
                    cleared.addAll(cm);
                    cm.clear();
                }
            }
        }

        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
        return cleared;
    }

    private void convertManaColor(final byte originalColor, final byte toColor) {
        List<Mana> convert = new ArrayList<>();
        Collection<Mana> cm = floatingMana.get(originalColor);
        for (Mana m : cm) {
            convert.add(new Mana(toColor, m.getSourceCard(), m.getManaAbility()));
        }
        cm.clear();
        floatingMana.putAll(toColor, convert);
        owner.updateManaForView();
    }

    private boolean removeMana(final Mana mana) {
        if (floatingMana.remove(mana.getColor(), mana)) {
            owner.updateManaForView();
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Removed, mana));
            return true;
        }
        return false;
    }

    public final void payManaFromAbility(final SpellAbility saPaidFor, ManaCostBeingPaid manaCost, final SpellAbility saPayment) {
        // Mana restriction must be checked before this method is called
        final List<SpellAbility> paidAbs = saPaidFor.getPayingManaAbilities();

        paidAbs.add(saPayment); // assumes some part on the mana produced by the ability will get used

        // need to get all mana from all ManaAbilities of the SpellAbility
        for (AbilityManaPart mp : saPayment.getAllManaParts()) {
            for (final Mana mana : mp.getLastManaProduced()) {
                if (tryPayCostWithMana(saPaidFor, manaCost, mana, false)) {
                    saPaidFor.getPayingMana().add(0, mana);
                }
            }
        }
    }

    public boolean tryPayCostWithColor(byte colorCode, SpellAbility saPaidFor, ManaCostBeingPaid manaCost) {
        Mana manaFound = null;
        String restriction = manaCost.getSourceRestriction();
        Collection<Mana> cm = floatingMana.get(colorCode);

        for (final Mana mana : cm) {
            if (mana.getManaAbility() != null && !mana.getManaAbility().meetsManaRestrictions(saPaidFor)) {
                continue;
            }

            if (StringUtils.isNotBlank(restriction) && !mana.getSourceCard().getType().hasStringType(restriction)) {
                continue;
            }

            manaFound = mana;
            break;
        }

        if (manaFound != null && tryPayCostWithMana(saPaidFor, manaCost, manaFound, false)) {
            saPaidFor.getPayingMana().add(0, manaFound);
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

        final List<Mana> removeFloating = new ArrayList<>();

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

        for (int k = 0; k < removeFloating.size(); k++) {
            removeMana(removeFloating.get(k));
        }
        return true;
    }

    public final void refundManaPaid(final SpellAbility sa) {
        // Send all mana back to your mana pool, before accounting for it.
        final List<Mana> manaPaid = sa.getPayingMana();

        // move non-undoable paying mana back to floating
        if (sa.getHostCard() != null) {
            sa.getHostCard().setCanCounter(true);
        }
        for (final Mana m : manaPaid) {
            addMana(m);
        }
        manaPaid.clear();

        List<SpellAbility> payingAbilities = sa.getPayingManaAbilities();
        for (final SpellAbility am : payingAbilities) {
            // undo paying abilities if we can
            am.undo();
        }

        for (final SpellAbility am : payingAbilities) {
            // Recursively refund abilities that were used.
            refundManaPaid(am);
        }

        payingAbilities.clear();

        // update battlefield of activating player - to redraw cards used to pay mana as untapped
        Player p = sa.getActivatingPlayer();
        p.getGame().fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
    }

    public boolean canPayForShardWithColor(ManaCostShard shard, byte color) {
        if (shard.isOfKind(ManaAtom.COLORLESS) && color == ManaAtom.GENERIC) {
            return false; // FIXME: testing Colorless against Generic is a recipe for disaster, but probably there should be a better fix.
        }

        // TODO Debug this for Paying Gonti,
        byte line = getPossibleColorUses(color);

        for (byte outColor : ManaAtom.MANATYPES) {
            if ((line & outColor) != 0 && shard.canBePaidWithManaOfColor(outColor)) {
                return true;
            }
        }

        // TODO The following may not be needed anymore?
        if (((color & (byte) ManaAtom.COLORLESS) != 0) && shard.canBePaidWithManaOfColor((byte)ManaAtom.COLORLESS)) {
            return true;
        }

        return shard.canBePaidWithManaOfColor((byte)0);
    }

    @Override
    public Iterator<Mana> iterator() {
        return floatingMana.values().iterator();
    }
}
