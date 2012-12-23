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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Constant;
import forge.Singletons;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayManaCostUtil;
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
    private Mana getManaFrom(final ArrayList<Mana> pool, final String manaStr, final SpellAbility saBeingPaidFor) {
        final String[] colors = manaStr.split("/");
        boolean wantSnow = false;
        for (int i = 0; i < colors.length; i++) {
            colors[i] = InputPayManaCostUtil.getLongColorString(colors[i]);
            if (colors[i].equals(Constant.Color.SNOW)) {
                wantSnow = true;
            }
        }

        Mana payment = null;
        final ArrayList<Mana> manaChoices = new ArrayList<Mana>();

        for (final Mana thisShard : pool) {
            // check mana restrictions first
            if (!thisShard.getSourceAbility().meetsManaRestrictions(saBeingPaidFor)) {
                continue;
            }

            if (thisShard.isColor(colors)) {
                if (payment == null) {
                    payment = thisShard;
                } else if (!payment.isRestricted() && thisShard.isRestricted()) {
                    payment = thisShard;
                } else if (payment.isSnow() && !thisShard.isSnow()) {
                    payment = thisShard;
                }
            } else if (wantSnow && thisShard.isSnow()) {
                if (payment == null) {
                    payment = thisShard;
                }
                else {
                    int mCol = ManaPool.MAP.get(thisShard.getColor());
                    int cCol = ManaPool.MAP.get(payment.getColor());
                    if (payment.isColor(Constant.Color.COLORLESS)) {
                        // do nothing Snow Colorless should be used first
                    } else if (thisShard.isColor(Constant.Color.COLORLESS)) {
                        // give preference to Colorless Snow mana over Colored
                        payment = thisShard;
                    } else if (this.floatingTotals[mCol] > this.floatingTotals[cCol]) {
                        // give preference to Colored mana that there is more of
                        payment = thisShard;
                    }
                }
            } else if (colors[0].equals(Constant.Color.COLORLESS)) { // colorless
                if ((payment == null) && thisShard.isColor(Constant.Color.COLORLESS)) {
                    payment = thisShard; // Colorless fits the bill nicely
                } else if (payment == null) {
                    manaChoices.add(thisShard);
                } else if (!payment.isRestricted() && thisShard.isRestricted()) {
                    payment = thisShard;
                } else if (payment.isSnow() && !thisShard.isSnow()) {
                    // nonSnow colorless is better to spend than Snow colorless
                    payment = thisShard;
                }
            }
        }

        if (payment != null) {
            return payment;
        }

        if (colors[0].equals(Constant.Color.COLORLESS)) {
            if (manaChoices.size() == 1) {
                payment = manaChoices.get(0);
            } else if (manaChoices.size() > 1) {
                final int[] normalMana = { 0, 0, 0, 0, 0, 0 };
                final int[] snowMana = { 0, 0, 0, 0, 0, 0 };
                final String[] manaStrings = { Constant.Color.WHITE, Constant.Color.BLUE, Constant.Color.BLACK,
                        Constant.Color.RED, Constant.Color.GREEN, Constant.Color.COLORLESS };

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
                        alChoice.add(manaStrings[i] + "(" + normalMana[i] + ")");
                    }
                    if (snowMana[i] > 0) {
                        alChoice.add("{S}" + manaStrings[i] + "(" + snowMana[i] + ")");
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
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost payManaFromPool(final SpellAbility saBeingPaidFor, ManaCost manaCost) {

        // paying from Mana Pool
        if (manaCost.isPaid() || this.isEmpty()) {
            return manaCost;
        }

        boolean keepPayingFromPool = true;
        final ArrayList<Mana> manaPaid = saBeingPaidFor.getPayingMana();

        while (keepPayingFromPool) {
            final String costStr = manaCost.toString().replace("X ", "").replace("P", "").replace(" ", "/");
            // get a mana of this type from floating, bail if none available
            final Mana mana = this.getManaFrom(this.floatingMana, costStr, saBeingPaidFor);
            if (mana == null) {
                keepPayingFromPool = false; // no matching mana in the pool
            }
            else {
                manaCost.payMana(mana);
                manaPaid.add(mana);
                this.removeManaFrom(this.floatingMana, mana);
                if (mana.addsNoCounterMagic() && saBeingPaidFor.getSourceCard() != null) {
                    saBeingPaidFor.getSourceCard().setCanCounter(false);
                }
                if (manaCost.isPaid()) {
                    keepPayingFromPool = false;
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
     *            a {@link forge.card.mana.ManaCost} object.
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost payManaFromPool(final SpellAbility saBeingPaidFor, final ManaCost manaCost, final String manaStr) {
        if (manaStr.trim().equals("") || manaCost.isPaid()) {
            return manaCost;
        }

        final ArrayList<Mana> manaPaid = saBeingPaidFor.getPayingMana();

        // get a mana of this type from floating, bail if none available
        final Mana mana = this.getManaFrom(this.floatingMana, manaStr, saBeingPaidFor);
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
     *            a {@link forge.card.mana.ManaCost} object.
     * @param ma
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost payManaFromAbility(final SpellAbility sa, ManaCost manaCost, final SpellAbility ma) {
        if (manaCost.isPaid() || this.isEmpty()) {
            return manaCost;
        }

        // Mana restriction must be checked before this method is called

        final List<SpellAbility> paidAbs = sa.getPayingManaAbilities();
        final List<Mana> manaPaid = sa.getPayingMana();

        paidAbs.add(ma); // assumes some part on the mana produced by the ability will get used
        for (final Mana mana : ma.getManaPart().getLastProduced()) {
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
