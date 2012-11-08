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
package forge.control.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.Constant;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.CostMana;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * <p>
 * InputPayManaCostUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPayManaCostUtil {

    /**
     * <p>
     * activateManaAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param card
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public static ManaCost activateManaAbility(final SpellAbility sa, final Card card, ManaCost manaCost) {
        // make sure computer's lands aren't selected
        if (card.getController().isComputer()) {
            return manaCost;
        }

        
        final StringBuilder cneeded = new StringBuilder();
        final StringBuilder colorRequired = new StringBuilder();
        boolean choice = true;
        boolean skipExpress = false;

        for (final String color : Constant.Color.MANA_COLORS) {
            String shortColor = InputPayManaCostUtil.getShortColorString(color);
            if (manaCost.isNeeded(color)) {
                cneeded.append(shortColor);
            }
            if (manaCost.isColor(shortColor)) {
                colorRequired.append(shortColor);
            }
        }

        List<AbilityActivated> abilities = new ArrayList<AbilityActivated>();
        // you can't remove unneeded abilities inside a for(am:abilities) loop :(
        
        for(AbilityActivated ma : card.getManaAbility()) {
            ma.setActivatingPlayer(Singletons.getControl().getPlayer());
            AbilityManaPart m = ma.getManaPart();
            if (!ma.canPlay()) {
                continue;
            } else if (!InputPayManaCostUtil.canMake(ma, cneeded.toString())) {
                continue;
            } else if (AbilityFactory.isInstantSpeed(ma)) {
                continue;
            } else if (!m.meetsManaRestrictions(sa)) {
                continue;
            }

            abilities.add(ma);
            
            if (!skipExpress) {
                // skip express mana if the ability is not undoable
                if (!m.isUndoable()) {
                    skipExpress = true;
                    continue;
                }
            }
        }
        if (abilities.isEmpty()) {
            return manaCost;
        }

        // Store some information about color costs to help with any mana choices
        String colorsNeeded = colorRequired.toString();
        if ("1".equals(colorsNeeded)) {  // only colorless left
            if (sa.getSourceCard() != null
                    && !sa.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").equals("")) {
                colorsNeeded = "";
                String[] negEffects = sa.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                for (String negColor : negEffects) {
                    // convert long color strings to short color strings
                    if (negColor.length() > 1) {
                        negColor = InputPayManaCostUtil.getShortColorString(negColor);
                    }
                    if (!colorsNeeded.contains(negColor)) {
                      colorsNeeded = colorsNeeded.concat(negColor);
                    }
                }
            }
            else {
                colorsNeeded = "W";
            }
        }
        else {
            // remove colorless from colors needed
            colorsNeeded = colorsNeeded.replace("1", "");
        }

        // If the card has sunburst or any other ability that tracks mana spent,
        // skip express Mana choice
        if (sa.getSourceCard() != null
                && sa.getSourceCard().hasKeyword("Sunburst") && sa.isSpell()) {
            colorsNeeded = "WUBRG";
            skipExpress = true;
        }

        if (!skipExpress) {
            // express Mana Choice
            final ArrayList<AbilityActivated> colorMatches = new ArrayList<AbilityActivated>();

            for (final AbilityActivated am : abilities) {
                AbilityManaPart m = am.getManaPart();
                if (m.isReflectedMana()) {
                    final List<String> reflectableColors = CardUtil.getReflectableManaColors(am, am, new ArrayList<String>(), new ArrayList<Card>());
                    for (final String color : reflectableColors) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                } else if (m.isAnyMana()) {
                        colorMatches.add(am);
                } else {
                    String[] colorsProduced;
                    if (m.isComboMana()) {
                        colorsProduced = m.getComboColors().split(" ");
                    }
                    else {
                        colorsProduced = m.getManaProduced().split(" ");
                    }
                    for (final String color : colorsProduced) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                }
            }

            if ((colorMatches.size() == 0)) {
                // can only match colorless just grab the first and move on.
                choice = false;
            } else if (colorMatches.size() < abilities.size()) {
                // leave behind only color matches
                abilities = colorMatches;
            }
        }

        AbilityActivated chosen = abilities.get(0);
        if ((1 < abilities.size()) && choice) {
            final Map<String, AbilityActivated> ability = new HashMap<String, AbilityActivated>();
            for (final AbilityActivated am : abilities) {
                ability.put(am.toString(), am);
            }
            chosen = GuiChoose.one("Choose mana ability", abilities);
        }

        // save off color needed for use by any mana and reflected mana
        chosen.getManaPart().setExpressChoice(colorsNeeded);

        Singletons.getModel().getGame().getAction().playSpellAbility(chosen);

        manaCost = Singletons.getControl().getPlayer().getManaPool().payManaFromAbility(sa, manaCost, chosen);

        //AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
        // DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
        return manaCost;

    }

    /**
     * <p>
     * activateManaAbility.
     * </p>
     * @param color a String that represents the Color the mana is coming from
     * @param sa a SpellAbility that is being paid for
     * @param manaCost the amount of mana remaining to be paid
     * 
     * @return ManaCost the amount of mana remaining to be paid after the mana is activated
     */
    public static ManaCost activateManaAbility(String color, final SpellAbility sa, ManaCost manaCost) {
        ManaPool mp = Singletons.getControl().getPlayer().getManaPool();

        // Convert Color to short String
        String manaStr = "1";
        if (!color.equalsIgnoreCase("Colorless")) {
            manaStr = CardUtil.getShortColor(color);
        }

        return mp.payManaFromPool(sa, manaCost, manaStr);
    }

    /**
     * <p>
     * canMake.  color is like "G", returns "Green".
     * </p>
     * 
     * @param am
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean canMake(final AbilityActivated am, final String mana) {
        if (mana.contains("1")) {
            return true;
        }
        AbilityManaPart m = am.getManaPart();
        if (mana.contains("S") && m.isSnow()) {
            return true;
        }
        if (m.isAnyMana()) {
            return true;
        }
        if (m.isReflectedMana()) {
            final List<String> reflectableColors = CardUtil.getReflectableManaColors(am, am, new ArrayList<String>(), new ArrayList<Card>());
            for (final String color : reflectableColors) {
                if (mana.contains(InputPayManaCostUtil.getShortColorString(color))) {
                    return true;
                }
            }
        } else {
            String[] colorsProduced;
            if (m.isComboMana()) {
                colorsProduced = m.getComboColors().split(" ");
            }
            else {
                colorsProduced = m.getManaProduced().split(" ");
            }
            for (final String color : colorsProduced) {
                if (mana.contains(color)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * getLongColorString.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLongColorString(final String color) {
        final Map<String, String> m = new HashMap<String, String>();
        m.put("G", Constant.Color.GREEN);
        m.put("R", Constant.Color.RED);
        m.put("U", Constant.Color.BLUE);
        m.put("B", Constant.Color.BLACK);
        m.put("W", Constant.Color.WHITE);
        m.put("S", Constant.Color.SNOW);

        Object o = m.get(color);

        if (o == null) {
            o = Constant.Color.COLORLESS;
        }

        return o.toString();
    }

    /**
     * <p>
     * getShortColorString.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getShortColorString(final String color) {
        final Map<String, String> m = new HashMap<String, String>();
        m.put(Constant.Color.GREEN, "G");
        m.put(Constant.Color.RED, "R");
        m.put(Constant.Color.BLUE, "U");
        m.put(Constant.Color.BLACK, "B");
        m.put(Constant.Color.WHITE, "W");
        m.put(Constant.Color.COLORLESS, "1");
        m.put(Constant.Color.SNOW, "S");

        final Object o = m.get(color);

        return o.toString();
    }

    /**
     * <p>
     * input_payXMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param costMana
     *            TODO
     * @param numX
     *            a int.
     * 
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputPayXMana(final SpellAbility sa, final CostPayment payment, final CostMana costMana,
            final int numX) {
        final Input payX = new InputMana() {
            private static final long serialVersionUID = -6900234444347364050L;
            private int xPaid = 0;
            private String colorsPaid = sa.getSourceCard().getColorsPaid();
            private ManaCost manaCost = new ManaCost(Integer.toString(numX));
    
            @Override
            public void showMessage() {
                if ((xPaid == 0 && costMana.isxCantBe0()) || 
                        !this.manaCost.toString().equals(Integer.toString(numX))) {
                    ButtonUtil.enableOnlyCancel();
                    // only cancel if partially paid an X value
                    // or X is 0, and x can't be 0
                } else {
                    ButtonUtil.enableAll();
                }
                
                StringBuilder msg = new StringBuilder("Pay X Mana Cost for ");
                msg.append(sa.getSourceCard().getName()).append("\n").append(this.xPaid);
                msg.append(" Paid so far.");
                if (costMana.isxCantBe0()) {
                    msg.append(" X Can't be 0.");
                }
                
                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
            }
    
            // selectCard
            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    // this really shouldn't happen but just in case
                    return;
                }
    
                this.manaCost = activateManaAbility(sa, card, this.manaCost);
                if (this.manaCost.isPaid()) {
                    if (!this.colorsPaid.contains(this.manaCost.getColorsPaid())) {
                        this.colorsPaid += this.manaCost.getColorsPaid();
                    }
                    this.manaCost = new ManaCost(Integer.toString(numX));
                    this.xPaid++;
                }
    
                if (Singletons.getModel().getMatch().getInput().getInput() == this) {
                    this.showMessage();
                }
            }
    
            @Override
            public void selectButtonCancel() {
                this.stop();
                payment.cancelCost();
                Singletons.getControl().getPlayer().getZone(ZoneType.Battlefield).updateObservers();
            }
    
            @Override
            public void selectButtonOK() {
                this.stop();
                payment.getCard().setXManaCostPaid(this.xPaid);
                payment.paidCost(costMana);
                payment.getCard().setColorsPaid(this.colorsPaid);
                payment.getCard().setSunburstValue(this.colorsPaid.length());
            }
    
            @Override
            public void selectManaPool(String color) {
                this.manaCost = activateManaAbility(color, sa, this.manaCost);
                if (this.manaCost.isPaid()) {
                    if (!this.colorsPaid.contains(this.manaCost.getColorsPaid())) {
                        this.colorsPaid += this.manaCost.getColorsPaid();
                    }
                    this.manaCost = new ManaCost(Integer.toString(numX));
                    this.xPaid++;
                }
    
                if (Singletons.getModel().getMatch().getInput().getInput() == this) {
                    this.showMessage();
                }
            }
    
        };
    
        return payX;
    }
}

