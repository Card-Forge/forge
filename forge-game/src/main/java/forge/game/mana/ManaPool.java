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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventManaPool;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * ManaPool class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPool implements Iterable<Mana> {

    private final Multimap<Byte, Mana> floatingMana = ArrayListMultimap.create();

    /** Constant <code>map</code>. */
    private final Player owner;

    /**
     * <p>
     * Constructor for ManaPool.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
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
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Added, mana));
    }

    /**
     * <p>
     * addManaToFloating.
     * </p>
     * 
     * @param manaList
     *           a {@link java.util.ArrayList} object.
     */
    public final void add(final Iterable<Mana> manaList) {
        for (final Mana m : manaList) {
            this.addMana(m);
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

        if (owner.hasKeyword("Green mana doesn't empty from your mana pool as steps and phases end.") &&
                getAmountOfColor(MagicColor.GREEN) == totalMana()) {
            return false; //won't lose floating mana if all mana is green and green mana isn't going to be emptied
        }

        return true;
    }

    /**
     * <p>
     * clearPool.
     * 
     * @return - the amount of mana removed this way
     * </p>
     */
    public final int clearPool(boolean isEndOfPhase) {
        // isEndOfPhase parameter: true = end of phase, false = mana drain effect
        if (floatingMana.isEmpty()) { return 0; }

        if (isEndOfPhase && owner.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.manapoolsDontEmpty)) {
            return 0;
        }

        int numRemoved = 0;
        boolean keepGreenMana = isEndOfPhase && owner.hasKeyword("Green mana doesn't empty from your mana pool as steps and phases end.");
        boolean convertToColorless = owner.hasKeyword("Convert unused mana to Colorless");

        List<Byte> keys = Lists.newArrayList(floatingMana.keySet());
        if (keepGreenMana) {
            keys.remove(Byte.valueOf(MagicColor.GREEN));
        }
        if (convertToColorless) {
            keys.remove(Byte.valueOf(MagicColor.COLORLESS));
        }

        for (Byte b : keys) {
            if (isEndOfPhase && !owner.getGame().getPhaseHandler().is(PhaseType.CLEANUP)) {
                final List<Mana> pMana = new ArrayList<Mana>();
                for (final Mana mana : floatingMana.get(b)) {
                    if (mana.getManaAbility()!= null && mana.getManaAbility().isPersistentMana()) {
                        pMana.add(mana);
                    }
                }
                if (convertToColorless) {
                    floatingMana.get(b).removeAll(pMana);
                    ConvertManaColor(b, MagicColor.COLORLESS);
                } else {
                    numRemoved += floatingMana.get(b).size() - pMana.size();
                    floatingMana.get(b).clear();
                    floatingMana.putAll(b, pMana);
                }
            }
            else {
                if (convertToColorless) {
                    ConvertManaColor(b, MagicColor.COLORLESS);
                } else {
                    numRemoved += floatingMana.get(b).size();
                    floatingMana.get(b).clear();
                }
            }
        }

        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
        return numRemoved;
    }

    private void ConvertManaColor(final byte originalColor, final byte toColor) {
        List<Mana> convert = new ArrayList<Mana>();
        for (Mana m : floatingMana.get(originalColor)) {
            convert.add(new Mana(toColor, m.getSourceCard(), m.getManaAbility()));
        }
        floatingMana.get(originalColor).clear();
        floatingMana.putAll(toColor, convert);
    }

    /**
     * <p>
     * removeManaFrom.
     * </p>
     *
     * @param mana
     *            a {@link forge.game.mana.Mana} object.
     */
    private void removeMana(final Mana mana) {
        Collection<Mana> cm = floatingMana.get(mana.getColor());
        if (cm.remove(mana)) {
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Removed, mana));
        }
    }
    
    /**
     * <p>
     * subtractManaFromAbility.
     * </p>
     * 
     * @param saPaidFor
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param manaCost
     *            a {@link forge.game.mana.ManaCostBeingPaid} object.
     * @return a {@link forge.game.mana.ManaCostBeingPaid} object.
     */
    public final void payManaFromAbility(final SpellAbility saPaidFor, ManaCostBeingPaid manaCost, final SpellAbility saPayment) {
        // Mana restriction must be checked before this method is called

        final List<SpellAbility> paidAbs = saPaidFor.getPayingManaAbilities();
        AbilityManaPart abManaPart = saPayment.getManaPartRecursive();

        paidAbs.add(saPayment); // assumes some part on the mana produced by the ability will get used
        for (final Mana mana : abManaPart.getLastManaProduced()) {
            if( tryPayCostWithMana(saPaidFor, manaCost, mana) )
                saPaidFor.getPayingMana().add(0, mana);
        }
    }

    public boolean tryPayCostWithColor(byte colorCode, SpellAbility saPaidFor, ManaCostBeingPaid manaCost) {
        Mana manaFound = null;
        String restriction = manaCost.getSourceRestriction();
        Collection<Mana> coloredMana = this.floatingMana.get(colorCode);

        for (final Mana mana : coloredMana) {
            if (mana.getManaAbility() != null && !mana.getManaAbility().meetsManaRestrictions(saPaidFor)) {
                continue;
            }

            if (StringUtils.isNotBlank(restriction) && !mana.getSourceCard().isType(restriction)) {
                continue;
            }

            manaFound = mana;
            break;
        }
        
        
        boolean result = manaFound != null && tryPayCostWithMana(saPaidFor, manaCost, manaFound);
        if(result)
            saPaidFor.getPayingMana().add(0, manaFound);
        return result;
    }

    
    public boolean tryPayCostWithMana(final SpellAbility sa, ManaCostBeingPaid manaCost, final Mana mana) {
        if (!manaCost.isNeeded(mana, this)) {
            return false;
        }
        manaCost.payMana(mana, this);
        this.removeMana(mana);
        if (mana.addsNoCounterMagic(sa) && sa.getHostCard() != null) {
            sa.getHostCard().setCanCounter(false);
        }
        if (sa.isSpell() && sa.getHostCard() != null) {
            final Card host = sa.getHostCard();
            if (mana.addsKeywords(sa) && mana.addsKeywordsType()
                    && host.isType(mana.getManaAbility().getAddsKeyowrdsType())) {
                final long timestamp = sa.getHostCard().getGame().getNextTimestamp();
                final List<String> kws = Arrays.asList(mana.getAddedKeywords().split(" & "));
                host.addChangedCardKeywords(kws, new ArrayList<String>(), false, timestamp);
                if (mana.addsKeywordsUntil()) {
                    final GameCommand untilEOT = new GameCommand() {
                        private static final long serialVersionUID = -8285169579025607693L;

                        @Override
                        public void run() {
                            if (!kws.isEmpty()) {
                                for (String kw : kws) {
                                    if (kw.startsWith("HIDDEN")) {
                                        sa.getHostCard().removeHiddenExtrinsicKeyword(kw);
                                    }
                                }
                                host.removeChangedCardKeywords(timestamp);
                            }
                            host.getGame().fireEvent(new GameEventCardStatsChanged(host));
                        }
                    };
                    String until = mana.getManaAbility().getAddsKeywordsUntil();
                    if ("UntilEOT".equals(until)) {
                        host.getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                }
            }
            if (mana.addsCounters(sa)) {
                mana.getManaAbility().createETBCounters(sa.getHostCard());
            }
        }
        return true;
    }

    /**
     * <p>
     * totalMana.
     * </p>
     * 
     * @return a int.
     */
    public final int totalMana() {
        return floatingMana.values().size();
    }

    //Account for mana part of ability when undoing it
    public boolean accountFor(final AbilityManaPart ma) {
    	if (ma == null) {
    		return false;
    	}
        if (this.floatingMana.isEmpty()) {
            return false;
        }

        final ArrayList<Mana> removeFloating = new ArrayList<Mana>();

        boolean manaNotAccountedFor = false;
        // loop over mana produced by mana ability
        for (Mana mana : ma.getLastManaProduced()) {
            Collection<Mana> poolLane = this.floatingMana.get(mana.getColor());

            if (poolLane != null && poolLane.contains(mana)) {
                removeFloating.add(mana);
            }
            else {
                manaNotAccountedFor = true;
                break;
            }
        }

        // When is it legitimate for all the mana not to be accountable?
        // Does this condition really indicate an bug in Forge?
        if (manaNotAccountedFor) {
            return false;
        }

        for (int k = 0; k < removeFloating.size(); k++) {
            this.removeMana(removeFloating.get(k));
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
            this.addMana(m);
        }
        manaPaid.clear();

        List<SpellAbility> payingAbilities = sa.getPayingManaAbilities();
        for (final SpellAbility am : payingAbilities) {
            // undo paying abilities if we can
            am.undo();
        }

        for (final SpellAbility am : payingAbilities) {
            // Recursively refund abilities that were used.
            this.refundManaPaid(am);
        }

        payingAbilities.clear();

        // update battlefield of activating player - to redraw cards used to pay mana as untapped
        Player p = sa.getActivatingPlayer();
        p.getGame().fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
    }
    
    // Conversion matrix ORs byte values to make mana more payable
    // Restrictive matrix ANDs byte values to make mana less payable
    private final byte[] colorConversionMatrix = new byte[MagicColor.WUBRG.length + 1];
    private final byte[] colorRestrictionMatrix = new byte[MagicColor.WUBRG.length + 1];
    private static final byte[] identityMatrix = { MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK, MagicColor.RED, MagicColor.GREEN, 0 };

    public void adjustColorReplacement(byte originalColor, byte replacementColor, boolean additive) {
        // Fix the index without hardcodes
        int rowIdx = MagicColor.getIndexOfFirstColor(originalColor);
        rowIdx = rowIdx < 0 ? identityMatrix.length - 1 : rowIdx;
        if (additive)
            colorConversionMatrix[rowIdx] |= replacementColor;
        else
            colorRestrictionMatrix[rowIdx] &= replacementColor;
    }
    
    public void restoreColorReplacements() {
        for(int i = 0; i < colorConversionMatrix.length; i++)
            colorConversionMatrix[i] = identityMatrix[i];

        for(int i = 0; i < colorRestrictionMatrix.length; i++)
            colorRestrictionMatrix[i] = MagicColor.ALL_COLORS;
    }

    public byte getPossibleColorUses(byte color) {
        // Take the current conversion value, AND with restrictions to get mana usage
        int rowIdx = MagicColor.getIndexOfFirstColor(color);
        int matrixIdx = rowIdx < 0 ? identityMatrix.length - 1 : rowIdx;

        byte colorUse = colorConversionMatrix[matrixIdx];
        colorUse &= colorRestrictionMatrix[matrixIdx];
        return colorUse;
    }

    public boolean canPayForShardWithColor(ManaCostShard shard, byte color) {
        byte line = getPossibleColorUses(color);
        for(int i = 0; i < MagicColor.NUMBER_OR_COLORS; i++) {
            byte outColor = MagicColor.WUBRG[i];
            if (( line & outColor) != 0  && shard.canBePaidWithManaOfColor(outColor))
                return true;
        }
        return shard.canBePaidWithManaOfColor((byte)0);
    }

    @Override
    public Iterator<Mana> iterator() {
        return floatingMana.values().iterator();
    }
}
