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
package forge.card.mana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import forge.Constant;
import forge.Singletons;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.GlobalRuleChange;
import forge.game.player.Player;
import forge.gui.GuiChoose;

/**
 * <p>
 * ManaPool class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPool {
    // current paying moved to SpellAbility

    private final ArrayList<Mana> floatingMana = new ArrayList<Mana>();
    private final int[] floatingTotals = new int[6]; // WUBRGC
    private final int[] floatingSnowTotals = new int[6]; // WUBRGC

    /** Constant <code>map</code>. */
    private static final Map<String, Integer> MAP = new HashMap<String, Integer>();

    /** Constant <code>colors="WUBRG"</code>. */
    public static final String COLORS = "WUBRG";
    /** Constant <code>mcolors="1WUBRG"</code>. */
    public static final String M_COLORS = "1WUBRG";
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
        ManaPool.MAP.put(Constant.Color.WHITE, 0);
        ManaPool.MAP.put(Constant.Color.BLUE, 1);
        ManaPool.MAP.put(Constant.Color.BLACK, 2);
        ManaPool.MAP.put(Constant.Color.RED, 3);
        ManaPool.MAP.put(Constant.Color.GREEN, 4);
        ManaPool.MAP.put(Constant.Color.COLORLESS, 5);
    }

    /**
     * <p>
     * calculatManaTotals for the Player panel.
     * </p>
     * 
     */
    private void calculateManaTotals() {
        for (int i = 0; i < floatingTotals.length; i++) {
            floatingTotals[i] = 0;
            floatingSnowTotals[i] = 0;
        }

        for (final Mana m : this.floatingMana) {
            if (m.isSnow()) {
                floatingSnowTotals[ManaPool.MAP.get(m.getColor())]++;
            } else {
                floatingTotals[ManaPool.MAP.get(m.getColor())]++;
            }
        }
    }

    /**
     * <p>
     * getAmountOfColor.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public final int getAmountOfColor(final String color) {
        if (color.equals(Constant.Color.SNOW)) {
            // If looking for Snow mana return total Snow
            int total = 0;
            for (int i : this.floatingSnowTotals) {
                total += i;
            }
            return total;
        }

        // If looking for Color/Colorless total Snow and non-Snow
        int i = ManaPool.MAP.get(color);
        return this.floatingTotals[i] + this.floatingSnowTotals[i];
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     * 
     * @return a boolean.
     */
    private boolean isEmpty() {
        return this.floatingMana.size() == 0;
    }

    /**
     * <p>
     * addManaToPool.
     * </p>
     * 
     * @param pool
     *            a {@link java.util.ArrayList} object.
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    private void addManaToPool(final ArrayList<Mana> pool, final Mana mana) {
        pool.add(mana);
        if (pool.equals(this.floatingMana)) {
            int i = ManaPool.MAP.get(mana.getColor());
            if (mana.isSnow()) {
                this.floatingSnowTotals[i]++;
            }
            else {
                this.floatingTotals[i]++;
            }
        }
        owner.updateObservers();
    }

    /**
     * <p>
     * addManaToFloating.
     * </p>
     * 
     * @param manaList
     *           a {@link java.util.ArrayList} object.
     */
    public final void addManaToFloating(final ArrayList<Mana> manaList) {
        for (final Mana m : manaList) {
            this.addManaToPool(this.floatingMana, m);
        }
        Singletons.getModel().getGame().getAction().checkStateEffects();
        owner.updateObservers();
    }

    /**
     * <p>
     * clearPool.
     * 
     * @return - the amount of mana removed this way
     * </p>
     */
    public final int clearPool(boolean isEndOfPhase) {
        int numRemoved = 0;

        if (isEndOfPhase
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.manapoolsDontEmpty)) {
            return numRemoved;
        }

        if (this.floatingMana.isEmpty()) {
            this.calculateManaTotals();
            //this.owner.updateObservers();
            return numRemoved;
        }

        if (isEndOfPhase && this.owner.hasKeyword("Green mana doesn't empty from your mana pool as steps and phases end.")) {
            // Omnath in play, clear all non-green mana
            int i = 0;
            while (i < this.floatingMana.size()) {
                if (this.floatingMana.get(i).isColor(Constant.Color.GREEN)) {
                    i++;
                    continue;
                }
                numRemoved++;
                this.floatingMana.remove(i);
            }
        } else {
            numRemoved = this.floatingMana.size();
            this.floatingMana.clear();
        }
        this.calculateManaTotals();
        //this.owner.updateObservers();

        return numRemoved;
    }

    /**
     * <p>
     * getManaFrom.
     * </p>
     * 
     * @param pool
     *            a {@link java.util.ArrayList} object.
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @param saBeingPaidFor
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.Mana} object.
     */
    private Mana getMana(final String manaStr, final SpellAbility saBeingPaidFor) {
        final ArrayList<Mana> pool = this.floatingMana;
        
        //System.out.format("ManaStr='%s' ...", manaStr);
        ManaCostShard shard = ManaCostShard.parseNonGeneric(manaStr);
        //System.out.format("Shard=%s (%d)", shard.toString(), shard.getColorMask() );
        //System.out.println();

        // What are the available options?
        final List<Pair<Mana, Integer>> weightedOptions = new ArrayList<Pair<Mana, Integer>>();
        for (final Mana thisMana : pool) {

            if (!thisMana.getManaAbility().meetsManaRestrictions(saBeingPaidFor)) {
                continue;
            }

            boolean canPay = shard.canBePaidWithManaOfColor(thisMana.getColorCode());
            if (!canPay || (shard.isSnow() && !thisMana.isSnow())) {
                continue;
            }

            // prefer colorless mana to spend
            int weight = thisMana.isColorless() ? 5 : 0;

            // prefer restricted mana to spend
            if (thisMana.isRestricted()) {
                weight += 2;
            }

            // Spend non-snow mana first
            if (!thisMana.isSnow()) {
                weight += 1;
            }

            weightedOptions.add(Pair.of(thisMana, weight));
        }

        // Exclude border case
        if (weightedOptions.isEmpty()) {

            return null; // There is no matching mana in the pool
        }

        // have at least one option at this moment
        int maxWeight = Integer.MIN_VALUE;
        int equalWeights = 0;
        Mana toPay = null;
        for (Pair<Mana, Integer> option : weightedOptions) {
            int thisWeight = option.getRight();
            if (thisWeight > maxWeight) {
                maxWeight = thisWeight;
                equalWeights = 1;
                toPay = option.getLeft();
            } else if (thisWeight == maxWeight) {
                equalWeights++;
            }
        }

        // got an only one best option?
        if (equalWeights == 1) {
            return toPay;
        }

        // select equal weight possibilities
        List<Mana> options = new ArrayList<Mana>();
        for (Pair<Mana, Integer> option : weightedOptions) {
            int thisWeight = option.getRight();
            if (maxWeight == thisWeight) {
                options.add(option.getLeft());
            }
        }

        // if the options are equal, there is no difference on which to spend
        toPay = options.get(0);
        boolean allAreEqual = true;
        for (int i = 1; i < options.size(); i++) {
            if (!toPay.equals(options.get(i))) {

                allAreEqual = false;
                break;
            }
        }

        if (allAreEqual) {
            return toPay;
        }

        // Not found a good one - then let them choose
        final List<Mana> manaChoices = options;
        Mana payment = null;

        final int[] normalMana = { 0, 0, 0, 0, 0, 0 };
        final int[] snowMana = { 0, 0, 0, 0, 0, 0 };

        // loop through manaChoices adding
        for (final Mana m : manaChoices) {
            if (m.isSnow()) {
                snowMana[ManaPool.MAP.get(m.getColor())]++;
            } else {
                normalMana[ManaPool.MAP.get(m.getColor())]++;
            }
        }

        int totalMana = 0;
        final ArrayList<String> alChoice = new ArrayList<String>();
        for (int i = 0; i < normalMana.length; i++) {
            totalMana += normalMana[i];
            totalMana += snowMana[i];
            if (normalMana[i] > 0) {
                alChoice.add(Constant.Color.COLORS[i] + "(" + normalMana[i] + ")");
            }
            if (snowMana[i] > 0) {
                alChoice.add("{S}" + Constant.Color.COLORS[i] + "(" + snowMana[i] + ")");
            }
        }

        if (alChoice.size() == 1) {
            payment = manaChoices.get(0);
            return payment;
        }

        int numColorless = 0;
        if (manaStr.matches("[0-9][0-9]?")) {
            numColorless = Integer.parseInt(manaStr);
        }
        if (numColorless >= totalMana) {
            payment = manaChoices.get(0);
            return payment;
        }

        Object o;

        if (this.owner.isHuman()) {
            o = GuiChoose.oneOrNone("Pay Mana from Mana Pool", alChoice);
        } else {
            o = alChoice.get(0); // owner is computer
        }

        if (o != null) {
            String ch = o.toString();
            final boolean grabSnow = ch.startsWith("{S}");
            ch = ch.replace("{S}", "");

            ch = ch.substring(0, ch.indexOf("("));

            for (final Mana m : manaChoices) {
                if (m.isColor(ch) && (!grabSnow || (grabSnow && m.isSnow()))) {
                    if (payment == null) {
                        payment = m;
                    } else if (payment.isSnow() && !m.isSnow()) {
                        payment = m;
                    }
                }
            }
        }

        return payment;
    }

    /**
     * <p>
     * removeManaFrom.
     * </p>
     * 
     * @param pool
     *            a {@link java.util.ArrayList} object.
     * @param choice
     *            a {@link forge.card.mana.Mana} object.
     */
    private void removeManaFrom(final ArrayList<Mana> pool, final Mana choice) {
        if (choice != null && pool.contains(choice)) {
            pool.remove(choice);
            if (pool.equals(this.floatingMana)) {
                int i = ManaPool.MAP.get(choice.getColor());
                if (choice.isSnow()) {
                    this.floatingSnowTotals[i]--;
                }
                else {
                    this.floatingTotals[i]--;
                }
            }
            owner.updateObservers();
        }
    }

    /**
     * <p>
     * payManaFromPool.
     * </p>
     * 
     * @param saBeingPaidFor
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid payManaFromPool(final SpellAbility saBeingPaidFor, ManaCostBeingPaid manaCost) {

        // paying from Mana Pool
        if (manaCost.isPaid() || this.isEmpty()) {
            return manaCost;
        }

        final ArrayList<Mana> manaPaid = saBeingPaidFor.getPayingMana();

        List<String> splitCost = Arrays.asList(manaCost.toString().replace("X ", "").replace("P", "").split(" "));
        Collections.reverse(splitCost); // reverse to pay colorful parts first with matching-color mana while it lasts. 
        for(String part : splitCost) {
            int loops = StringUtils.isNumeric(part) ? Integer.parseInt(part) : 1;
            for(int i = 0; i < loops; i++ ) {
                final Mana mana = this.getMana(part, saBeingPaidFor);
                if (mana != null) {
                    manaCost.payMana(mana);
                    manaPaid.add(mana);
                    this.removeManaFrom(this.floatingMana, mana);
                    if (mana.addsNoCounterMagic() && saBeingPaidFor.getSourceCard() != null) {
                        saBeingPaidFor.getSourceCard().setCanCounter(false);
                    }
                }
            }
        }

        return manaCost;
    }

    /**
     * <p>
     * payManaFromPool.
     * </p>
     * 
     * @param saBeingPaidFor
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid payManaFromPool(final SpellAbility saBeingPaidFor, final ManaCostBeingPaid manaCost, final String manaStr) {
        if (manaStr.trim().equals("") || manaCost.isPaid()) {
            return manaCost;
        }

        final ArrayList<Mana> manaPaid = saBeingPaidFor.getPayingMana();

        // get a mana of this type from floating, bail if none available
        final Mana mana = this.getMana(manaStr, saBeingPaidFor);
        if (mana == null) {
            return manaCost; // no matching mana in the pool
        }
        else if (manaCost.isNeeded(mana)) {
                manaCost.payMana(mana);
                manaPaid.add(mana);
                this.removeManaFrom(this.floatingMana, mana);
                if (mana.addsNoCounterMagic() && saBeingPaidFor.getSourceCard() != null) {
                    saBeingPaidFor.getSourceCard().setCanCounter(false);
                }
        }
        return manaCost;
    }

    /**
     * <p>
     * subtractManaFromAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @param ma
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid payManaFromAbility(final SpellAbility sa, ManaCostBeingPaid manaCost, final SpellAbility ma) {
        if (manaCost.isPaid() || this.isEmpty()) {
            return manaCost;
        }

        // Mana restriction must be checked before this method is called

        final List<SpellAbility> paidAbs = sa.getPayingManaAbilities();
        final List<Mana> manaPaid = sa.getPayingMana();
        
        SpellAbility tail = ma;
        AbilityManaPart abManaPart = null;
        while(abManaPart == null && tail != null)
        {
            abManaPart = tail.getManaPart();
            tail = tail.getSubAbility();
        }

        paidAbs.add(ma); // assumes some part on the mana produced by the ability will get used
        for (final Mana mana : abManaPart.getLastProduced()) {
            if (manaCost.isNeeded(mana)) {
                manaCost.payMana(mana);
                manaPaid.add(mana);
                this.removeManaFrom(this.floatingMana, mana);
                if (mana.addsNoCounterMagic() && sa.getSourceCard() != null) {
                    sa.getSourceCard().setCanCounter(false);
                }
            }
        }
        return manaCost;
    }

    /**
     * <p>
     * totalMana.
     * </p>
     * 
     * @return a int.
     */
    public final int totalMana() {
        return this.floatingMana.size();
    }

    /**
     * <p>
     * clearPay.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param refund
     *            a boolean.
     */
    public final void clearManaPaid(final SpellAbility ability, final boolean refund) {
        final List<SpellAbility> abilitiesUsedToPay = ability.getPayingManaAbilities();
        final List<Mana> manaPaid = ability.getPayingMana();

        abilitiesUsedToPay.clear();
        // move non-undoable paying mana back to floating
        if (refund) {
            if (ability.getSourceCard() != null) {
                ability.getSourceCard().setCanCounter(true);
            }
            for (final Mana m : manaPaid) {
                this.addManaToPool(this.floatingMana, m);
            }
        }

        manaPaid.clear();
        this.calculateManaTotals();
        this.owner.updateObservers();
    }

    /**
     * <p>
     * accountFor.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param ma
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @return a boolean.
     */
    private boolean accountFor(final SpellAbility sa, final AbilityManaPart ma) {
        final ArrayList<Mana> manaPaid = sa.getPayingMana();

        if ((manaPaid.size() == 0) && (this.floatingMana.size() == 0)) {
          return false;
        }

        final ArrayList<Mana> removePaying = new ArrayList<Mana>();
        final ArrayList<Mana> removeFloating = new ArrayList<Mana>();

        boolean manaNotAccountedFor = false;
        // loop over mana produced by mana ability
        for (Mana mana : ma.getLastProduced()) {
            if (manaPaid.contains(mana)) {
                removePaying.add(mana);
            }
            else if (this.floatingMana.contains(mana)) {
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

        for (int k = 0; k < removePaying.size(); k++) {
            this.removeManaFrom(manaPaid, removePaying.get(k));
        }
        for (int k = 0; k < removeFloating.size(); k++) {
            this.removeManaFrom(this.floatingMana, removeFloating.get(k));
        }
        return true;
    }

    /**
     * <p>
     * refundManaPaid.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param untap
     *            a boolean.
     */
    public final void refundManaPaid(final SpellAbility sa, final boolean untap) {
        // TODO having some crash in here related to undo and not tracking
        // abilities properly
        final List<SpellAbility> payAbs = sa.getPayingManaAbilities();

        // go through paidAbilities if they are undoable
        for (final SpellAbility am : payAbs) {
            AbilityManaPart m = am.getManaPart();
            if (am.isUndoable()) {
                if (this.accountFor(sa, m)) {
                    am.undo();
                }
                // else can't account let clearPay move paying back to floating
            }
        }

        // move leftover pay back to floating
        this.clearManaPaid(sa, true);
    }
}
