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
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardUtil;
import forge.Constant;
import forge.Player;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.InputPayManaCostUtil;

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
     *            a {@link forge.Player} object.
     */
    public ManaPool(final Player player) {
        owner = player;
        owner.updateObservers();
        clearPool();
        ManaPool.MAP.put(Constant.Color.WHITE, 0);
        ManaPool.MAP.put(Constant.Color.BLUE, 1);
        ManaPool.MAP.put(Constant.Color.BLACK, 2);
        ManaPool.MAP.put(Constant.Color.RED, 3);
        ManaPool.MAP.put(Constant.Color.GREEN, 4);
        ManaPool.MAP.put(Constant.Color.COLORLESS, 5);
    }

    /**
     * <p>
     * getManaList.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Deprecated
    public final String getManaList() {
        calculateManaTotals();
        final StringBuilder sbNormal = new StringBuilder("");
        final StringBuilder sbSnow = new StringBuilder("");
        if (!this.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                if (i == 5) {
                    if (floatingTotals[i] > 0) {
                        sbNormal.append(floatingTotals[i] + " ");
                    }
                    if (floatingSnowTotals[i] > 0) {
                        sbSnow.append(floatingSnowTotals[i] + " ");
                    }
                } else {
                    if (floatingTotals[i] > 0) {
                        for (int j = 0; j < floatingTotals[i]; j++) {
                            sbNormal.append(CardUtil.getShortColor(Constant.Color.COLORS[i])).append(" ");
                        }
                    }
                    if (floatingSnowTotals[i] > 0) {
                        for (int j = 0; j < floatingSnowTotals[i]; j++) {
                            sbSnow.append(CardUtil.getShortColor(Constant.Color.COLORS[i])).append(" ");
                        }
                    }
                }

                sbNormal.append("|");
                sbSnow.append("|");
            }
        } else {
            return ("|||||||||||");
        }

        return sbNormal.append(sbSnow).toString();

    }

    /**
     * <p>
     * calculatManaTotals for the Player panel.
     * </p>
     * 
     */
    public void calculateManaTotals() {
        for (int i = 0; i < floatingTotals.length; i++) {
            floatingTotals[i] = 0;
            floatingSnowTotals[i] = 0;
        }

        for (final Mana m : this.floatingMana) {
            if (m.isSnow()) {
                floatingSnowTotals[ManaPool.MAP.get(m.getColor())] += m.getAmount();
            } else {
                floatingTotals[ManaPool.MAP.get(m.getColor())] += m.getAmount();
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
     * getAmountOfColor.
     * </p>
     * 
     * @param color
     *            a char.
     * @return a int.
     */
    public final int getAmountOfColor(final char color) {
        return this.getAmountOfColor(Character.toString(color));
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEmpty() {
        return this.floatingMana.size() == 0;
    }

    /**
     * <p>
     * oraclize.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String oraclize(final String manaCost) {
        // converts RB to (R/B)
        final String[] parts = manaCost.split(" ");
        final StringBuilder res = new StringBuilder();
        for (String s : parts) {
            if ((s.length() == 2) && ManaPool.COLORS.contains(s.charAt(1) + "")) {
                s = s.charAt(0) + "/" + s.charAt(1);
            }
            if (s.length() == 3) {
                s = "(" + s + ")";
            }
            if (s.equals("S")) {
                s = "(S)"; // for if/when we implement snow mana
            }
            if (s.equals("X")) {
                s = "(X)"; // X costs?
            }
            res.append(s);
        }
        return res.toString();
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
    public final void addManaToPool(final ArrayList<Mana> pool, final Mana mana) {
        pool.add(mana);
        if (pool.equals(this.floatingMana)) {
            int i = ManaPool.MAP.get(mana.getColor());
            if (mana.isSnow()) {
                this.floatingSnowTotals[i] += mana.getAmount();
            }
            else {
                this.floatingTotals[i] += mana.getAmount();
            }
        }
        owner.updateObservers();
    }

    /**
     * <p>
     * addManaToFloating.
     * </p>
     * 
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void addManaToFloating(final String manaStr, final Card card) {
        final ArrayList<Mana> manaList = ManaPool.convertStringToMana(manaStr, card);
        for (final Mana m : manaList) {
            this.addManaToPool(this.floatingMana, m);
        }
        AllZone.getGameAction().checkStateEffects();
        owner.updateObservers();
    }

    /**
     * <p>
     * convertStringToMana.
     * </p>
     * 
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Mana> convertStringToMana(String manaStr, final Card card) {
        final ArrayList<Mana> manaList = new ArrayList<Mana>();
        manaStr = manaStr.trim();
        final String[] manaArr = manaStr.split(" ");

        String color = "";
        int total = 0;
        int genericTotal = 0;

        for (final String c : manaArr) {
            final String longStr = InputPayManaCostUtil.getLongColorString(c);
            if (longStr.equals(Constant.Color.COLORLESS)) {
                genericTotal += Integer.parseInt(c);
            } else if (color.equals("")) {
                color = longStr;
                total = 1;
            } else if (color.equals(longStr)) {
                total++;
            } else { // more than one color generated
                // add aggregate color
                manaList.add(new Mana(color, total, card));

                color = longStr;
                total = 1;
            }
        }
        if (total > 0) {
            manaList.add(new Mana(color, total, card));
        }
        if (genericTotal > 0) {
            manaList.add(new Mana(Constant.Color.COLORLESS, genericTotal, card));
        }

        return manaList;
    }

    /**
     * <p>
     * clearPool.
     * </p>
     */
    public final void clearPool() {
        if (this.floatingMana.size() == 0) {
            this.calculateManaTotals();
            this.owner.updateObservers();
            return;
        }

        if (AllZoneUtil.isCardInPlay("Omnath, Locus of Mana", this.owner)) {
            // Omnath in play, clear all non-green mana
            int i = 0;
            while (i < this.floatingMana.size()) {
                if (this.floatingMana.get(i).isColor(Constant.Color.GREEN)) {
                    i++;
                    continue;
                }
                this.floatingMana.remove(i);
            }
        } else {
            this.floatingMana.clear();
        }
        this.calculateManaTotals();
        this.owner.updateObservers();
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
     * @return a {@link forge.card.mana.Mana} object.
     */
    public final Mana getManaFrom(final ArrayList<Mana> pool, final String manaStr) {
        final String[] colors = manaStr.split("/");
        boolean wantSnow = false;
        for (int i = 0; i < colors.length; i++) {
            colors[i] = InputPayManaCostUtil.getLongColorString(colors[i]);
            if (colors[i].equals(Constant.Color.SNOW)) {
                wantSnow = true;
            }
        }

        Mana choice = null;
        final ArrayList<Mana> manaChoices = new ArrayList<Mana>();

        for (final Mana mana : pool) {
            if (mana.isColor(colors)) {
                if (choice == null) {
                    choice = mana;
                } else if (choice.isSnow() && !mana.isSnow()) {
                    choice = mana;
                }
            } else if (wantSnow && mana.isSnow()) {
                if (choice == null) {
                    choice = mana;
                }
                else {
                    int mCol = ManaPool.MAP.get(mana.getColor());
                    int cCol = ManaPool.MAP.get(choice.getColor());
                    if (choice.isColor(Constant.Color.COLORLESS)) {
                        // do nothing Snow Colorless should be used first
                    } else if (mana.isColor(Constant.Color.COLORLESS)) {
                        // give preference to Colorless Snow mana over Colored
                        choice = mana;
                    } else if (this.floatingTotals[mCol] > this.floatingTotals[cCol]) {
                        // give preference to Colored mana that there is more of
                        choice = mana;
                    }
                }
            } else if (colors[0].equals(Constant.Color.COLORLESS)) { // colorless
                if ((choice == null) && mana.isColor(Constant.Color.COLORLESS)) {
                    choice = mana; // Colorless fits the bill nicely
                } else if (choice == null) {
                    manaChoices.add(mana);
                } else if (choice.isSnow() && !mana.isSnow()) {
                    // nonSnow colorless is better to spend than Snow colorless
                    choice = mana;
                }
            }
        }

        if (choice != null) {
            return choice;
        }

        if (colors[0].equals(Constant.Color.COLORLESS)) {
            if (manaChoices.size() == 1) {
                choice = manaChoices.get(0);
            } else if (manaChoices.size() > 1) {
                final int[] normalMana = { 0, 0, 0, 0, 0, 0 };
                final int[] snowMana = { 0, 0, 0, 0, 0, 0 };
                final String[] manaStrings = { Constant.Color.WHITE, Constant.Color.BLUE, Constant.Color.BLACK,
                        Constant.Color.RED, Constant.Color.GREEN, Constant.Color.COLORLESS };

                // loop through manaChoices adding
                for (final Mana m : manaChoices) {
                    if (m.isSnow()) {
                        snowMana[ManaPool.MAP.get(m.getColor())] += m.getAmount();
                    } else {
                        normalMana[ManaPool.MAP.get(m.getColor())] += m.getAmount();
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
                    choice = manaChoices.get(0);
                    return choice;
                }

                int numColorless = 0;
                if (manaStr.matches("[0-9][0-9]?")) {
                    numColorless = Integer.parseInt(manaStr);
                }
                if (numColorless >= totalMana) {
                    choice = manaChoices.get(0);
                    return choice;
                }

                Object o;

                if (this.owner.isHuman()) {
                    o = GuiUtils.getChoiceOptional("Pay Mana from Mana Pool", alChoice.toArray());
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
                            if (choice == null) {
                                choice = m;
                            } else if (choice.isSnow() && !m.isSnow()) {
                                choice = m;
                            }
                        }
                    }
                }
            }
        }

        return choice;
    }

    /**
     * <p>
     * findAndRemoveFrom.
     * </p>
     * 
     * @param pool
     *            a {@link java.util.ArrayList} object.
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    public final void findAndRemoveFrom(final ArrayList<Mana> pool, final Mana mana) {
        Mana set = null;
        for (final Mana m : pool) {
            if (m.getSourceCard().equals(mana.getSourceCard()) && m.getColor().equals(mana.getColor())) {
                set = m;
                break;
            }
        }
        this.removeManaFrom(pool, set, mana.getAmount());
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
     * @param amount
     *            an int .
     */
    public final void removeManaFrom(final ArrayList<Mana> pool, final Mana choice, final int amount) {
        if (choice != null) {
            if (choice.getAmount() == amount) {
                pool.remove(choice);
            } else {
                choice.decrementAmount(amount);
            }
            if (pool.equals(this.floatingMana)) {
                int i = ManaPool.MAP.get(choice.getColor());
                if (choice.isSnow()) {
                    this.floatingSnowTotals[i] -= amount;
                }
                else {
                    this.floatingTotals[i] -= amount;
                }
            }
            owner.updateObservers();
        }
    }

    /**
     * <p>
     * formatMana.
     * </p>
     * 
     * @param manaAbility
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] formatMana(final AbilityMana manaAbility) {
        return ManaPool.formatMana(manaAbility.mana(), true);
    } // wrapper

    /**
     * <p>
     * formatMana.
     * </p>
     * 
     * @param mana2
     *            a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] formatMana(final String mana2) {
        // turns "G G" -> {"G","G"}, "2 UG"->"{"2","U/G"}, "B W U R
        // G" -> {"B","W","U","R","G"}, etc.
        return ManaPool.formatMana(mana2, false);
    }

    /**
     * <p>
     * formatMana.
     * </p>
     * 
     * @param mana2
     *            a {@link java.lang.String} object.
     * @param parsed
     *            a boolean.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] formatMana(final String mana2, final boolean parsed) {
        String mana = mana2;
        // if (Mana.isEmpty()) return null;
        if (mana.trim().equals("")) {
            return null;
        }
        if (!parsed) {
            mana = ManaPool.oraclize(mana);
        }
        try {
            final String[] colorless = { Integer.toString(Integer.parseInt(mana)) };
            return colorless;
        } catch (final NumberFormatException ex) {
        }

        final ArrayList<String> res = new ArrayList<String>();
        int colorless = 0;
        String clessString = "";
        boolean parentheses = false;
        String current = "";

        for (int i = 0; i < mana.length(); i++) {
            final char c = mana.charAt(i);
            if (c == '(') {
                parentheses = true;
                continue;
            } // Split cost handling ("(" +<W/U/B/R/G/2> + "/" + <W/U/B/R/G> +
              // ")")
            else if (parentheses) {
                if (c != ')') {
                    current += c;
                    continue;
                } else {
                    parentheses = false;
                    res.add(current);
                    current = "";
                    continue;
                }
            }
            final String s = c + "";
            if (ManaPool.COLORS.contains(s)) {
                res.add(s);
                if (clessString.trim().equals("")) {
                    continue;
                }
                try {
                    colorless += Integer.parseInt(clessString.trim());
                } catch (final NumberFormatException ex) {
                    throw new RuntimeException(
                            "Mana_Pool.getManaParts : Error, sum of noncolor mana parts is not a number - "
                                    + clessString);
                }
                clessString = "";
            } else {
                clessString += s;
            }
        }
        for (int i = 0; i < colorless; i++) {
            res.add("1");
        }

        return res.toArray(new String[0]);
    }

    /**
     * <p>
     * subtractMultiple.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param cost
     *            an array of {@link java.lang.String} objects.
     * @param m
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    private ManaCost subtractMultiple(final SpellAbility sa, final String[] cost, ManaCost m) {
        for (final String s : cost) {
            if (this.isEmpty()) {
                break;
            }

            int num = 1;
            try {
                num = Integer.parseInt(s);
            } catch (final NumberFormatException e) {
                // Not an integer, that's fine
            }

            for (int i = 0; i < num; i++) {
                if (this.isEmpty()) {
                    break;
                }

                m = this.subtractOne(sa, m, s);
            }
        }
        return m;
    }

    /**
     * <p>
     * subtractMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param m
     *            a {@link forge.card.mana.ManaCost} object.
     * @param mAbilities
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost subtractMana(final SpellAbility sa, ManaCost m, final AbilityMana... mAbilities) {
        final ArrayList<AbilityMana> paidAbs = sa.getPayingManaAbilities();  // why???

        if (mAbilities.length == 0) {
            // paying from Mana Pool
            if (m.isPaid() || this.isEmpty()) {
                return m;
            }

            final String[] cost = ManaPool.formatMana(m.toString());
            return this.subtractMultiple(sa, cost, m);
        }

        // paying via Mana Abilities
        for (final AbilityMana mability : mAbilities) {
            paidAbs.add(mability); // why???
            //TODO: Look at using new getManaProduced() method of Ability_Mana (ArsenalNut)
            //String[] cost = formatMana(mability);
            String[] cost = null;
            if (mability.isAnyMana()) {
                cost = new String[1];
                cost[0] = mability.getAnyChoice();
            }
            else {
                cost = formatMana(mability);
            }
            m = this.subtractMultiple(sa, cost, m);
        }

        return m;
    }

    /**
     * <p>
     * subtractOne.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCost} object.
     * @param manaStr
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost subtractOne(final SpellAbility sa, final ManaCost manaCost, final String manaStr) {
        if (manaStr.trim().equals("") || manaCost.isPaid()) {
            return manaCost;
        }

        final ArrayList<Mana> payMana = sa.getPayingMana();

        // get a mana of this type from floating, bail if none available
        final Mana mana = this.getManaFrom(this.floatingMana, manaStr);
        if (mana == null) {
            return manaCost; // no matching mana in the pool
        }

        final Mana[] manaArray = mana.toSingleArray();

        for (final Mana m : manaArray) {
            if (manaCost.isNeeded(m)) {
                manaCost.payMana(m);
                payMana.add(m);  // what is this used for? anything
                this.findAndRemoveFrom(this.floatingMana, m);
            } else {
                break;
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
        int total = 0;
        for (final Mana c : this.floatingMana) {
            total += c.getAmount();
        }
        return total;
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
    public final void clearPay(final SpellAbility ability, final boolean refund) {
        final ArrayList<AbilityMana> payAbs = ability.getPayingManaAbilities();
        final ArrayList<Mana> payMana = ability.getPayingMana();

        payAbs.clear();
        // move non-undoable paying mana back to floating
        if (refund) {
            for (final Mana m : payMana) {
                this.addManaToPool(this.floatingMana, m);
            }
        }

        payMana.clear();
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
     * @param mana
     *            an array of {@link java.lang.String} objects.
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean accountFor(final SpellAbility sa, final String[] mana, final Card c) {
        // TODO account for unpaying mana in payMana and floatingPool
        final ArrayList<Mana> payMana = sa.getPayingMana();

        if ((payMana.size() == 0) && (this.floatingMana.size() == 0)) {
          return false;
        }
        
        final ArrayList<Mana> removePaying = new ArrayList<Mana>();
        final ArrayList<Mana> removeFloating = new ArrayList<Mana>();

        int manaAccounted = 0;
        // loop over mana paid
        for (Mana manaPaid : payMana) {
            if (manaPaid.fromSourceCard(c)) {
                for (int i = 0;i < mana.length;i++ ) {
                    if  (manaPaid.getColor().equals(InputPayManaCostUtil.getLongColorString(mana[i]))) {
                        final int amt = manaPaid.getColorlessAmount();
                        if (amt > 0) {
                            final int difference = Integer.parseInt(mana[i]) - amt;
                            if (difference > 0) {
                                mana[i] = Integer.toString(difference);
                            } else {
                                manaAccounted += amt;
                            }
                        } else {
                            manaAccounted += manaPaid.getAmount();
                        }
                        removePaying.add(manaPaid);
                        break;
                    }
                }
            }
            if (manaAccounted == mana.length) {
                break;
            }
        }
        // loop over mana pool if not all of the generated mana is accounted for
        if (manaAccounted < mana.length) {
            for (Mana manaFloat : this.floatingMana) {
                if (manaFloat.fromSourceCard(c)) {
                    for (int i = 0;i < mana.length;i++ ) {
                        if  (manaFloat.getColor().equals(InputPayManaCostUtil.getLongColorString(mana[i]))) {
                            final int amt = manaFloat.getColorlessAmount();
                            if (amt > 0) {
                                final int difference = Integer.parseInt(mana[i]) - amt;
                                if (difference > 0) {
                                    mana[i] = Integer.toString(difference);
                                } else {
                                    manaAccounted += amt;
                                }
                            } else {
                                manaAccounted += manaFloat.getAmount();
                            }
                            removeFloating.add(manaFloat);
                            break;
                        }
                    }
                }
                if (manaAccounted == mana.length) {
                    break;
                }
            }            
        }
        // When is it legitimate for all the mana not to be accountable?
        // Does this condition really indicate an bug in Forge?
        if (manaAccounted < mana.length) {
            return false;
        }

        for (int k = 0; k < removePaying.size(); k++) {
            this.removeManaFrom(payMana, removePaying.get(k), removePaying.get(k).getAmount());
        }
        for (int k = 0; k < removeFloating.size(); k++) {
            this.removeManaFrom(this.floatingMana, removeFloating.get(k), removeFloating.get(k).getAmount());
        }
        return true;
    }
    
    /**
     * <p>
     * unpaid.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param untap
     *            a boolean.
     */
    public final void unpaid(final SpellAbility sa, final boolean untap) {
        // TODO having some crash in here related to undo and not tracking
        // abilities properly
        final ArrayList<AbilityMana> payAbs = sa.getPayingManaAbilities();

        // go through paidAbilities if they are undoable
        for (final AbilityMana am : payAbs) {
            if (am.isUndoable()) {
                //final String[] formattedMana = ManaPool.formatMana(am);
                /*String[] formattedMana = null;
                if (am.isAnyMana()) {
                    formattedMana = new String[1];
                    formattedMana[0] = am.getAnyChoice();
                }
                else {
                    formattedMana = formatMana(am);
                }*/
                final String[] formattedMana = am.getLastProduced().split(" ");
                if (this.accountFor(sa, formattedMana, am.getSourceCard())) {
                    am.undo();
                }
                // else can't account let clearPay move paying back to floating
            }
        }

        // move leftover pay back to floating
        this.clearPay(sa, true);
    }
}
