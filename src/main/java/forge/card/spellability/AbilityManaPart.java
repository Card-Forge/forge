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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.mana.Mana;
import forge.card.mana.ManaPool;
import forge.card.trigger.TriggerType;
import forge.control.input.InputPayManaCostUtil;
import forge.game.player.Player;

/**
 * <p>
 * Abstract AbilityMana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityManaPart implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-6816356991224950520L</code>. */
    private static final long serialVersionUID = -6816356991224950520L;

    private String origProduced;
    private String lastExpressChoice = "";
    private String manaRestrictions = "";
    transient private ArrayList<Mana> lastProduced = new ArrayList<Mana>();
    private int amount = 1;

    /** The reflected. */
    private boolean reflected = false;

    /** The undoable. */
    private boolean undoable = true;

    /** The canceled. */
    private boolean canceled = false;

    transient private final Card sourceCard;

    transient private Cost cost;

    // Spells paid with this mana spell can't be countered.
    private boolean cannotCounterSpell;
    
    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param parse
     *            a {@link java.lang.String} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @param num
     *            a int.
     */
    public AbilityManaPart(final Card sourceCard, final String parse, final Map<String, String> params) {
        this(sourceCard, new Cost(sourceCard, parse, true), params);
    }

    public AbilityManaPart(final Card sourceCard, final Cost cost, final Map<String, String> params) {
        this.sourceCard = sourceCard;
        this.cost = cost;
        
        origProduced = params.containsKey("Produced") ? params.get("Produced") : "1";
        if (params.containsKey("RestrictValid")) {
            this.manaRestrictions = params.get("RestrictValid");
        }
        
        this.cannotCounterSpell = params.containsKey("AddsNoCounter");

    }

    /**
     * <p>
     * produceMana.
     * </p>
     * @param ability 
     */
    public final void produceMana(SpellAbility sa) {
        this.produceMana(this.getManaProduced(), this.getSourceCard().getController(), sa);
    }

    /**
     * <p>
     * produceMana.
     * </p>
     * 
     * @param produced
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param sa 
     */
    public final void produceMana(final String produced, final Player player, SpellAbility sa) {
        final Card source = this.getSourceCard();
        final ManaPool manaPool = player.getManaPool();
        // change this, once ManaPool moves to the Player
        // this.getActivatingPlayer().ManaPool.addManaToFloating(origProduced,
        // getSourceCard());

        //clear lastProduced
        this.lastProduced.clear();

        // loop over mana produced string
        for (final String c : produced.split(" ")) {
            try {
                int colorlessAmount = Integer.parseInt(c);
                final String color = InputPayManaCostUtil.getLongColorString(c);
                for (int i = 0; i < colorlessAmount; i++) {
                    this.lastProduced.add(new Mana(color, source, this));
                }
            } catch (NumberFormatException e) {
                final String color = InputPayManaCostUtil.getLongColorString(c);
                this.lastProduced.add(new Mana(color, source, this));
            }
        }

        // add the mana produced to the mana pool
        manaPool.addManaToFloating(this.lastProduced);

        // TODO all of the following would be better as trigger events
        // "tapped for mana"
        if (source.getName().equals("Rainbow Vale")) {
            this.undoable = false;
            source.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
        }

        if (source.getName().equals("Undiscovered Paradise")) {
            this.undoable = false;
            // Probably best to conver this to an Extrinsic Ability
            source.setBounceAtUntap(true);
        }

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();

        runParams.put("Card", source);
        runParams.put("Player", player);
        runParams.put("AbilityMana", sa);
        runParams.put("Produced", produced);
        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.TapsForMana, runParams);

    } // end produceMana(String)

    /**
     * <p>
     * getProducedMana.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getManaProduced() {
        StringBuilder sb = new StringBuilder();
        if (this.amount == 0) {
            sb.append("0");
        }
        else {
            try {
                // if baseMana is an integer(colorless), just multiply amount and baseMana
                int base = Integer.parseInt(this.getOrigProduced());
                sb.append(base * this.amount);
            } catch (NumberFormatException e) {
                for (int i = 0; i < this.amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(mana());
                }
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * cannotCounterPaidWith.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public boolean cannotCounterPaidWith() {
        return cannotCounterSpell;
    }

    /**
     * <p>
     * getManaRestrictions.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getManaRestrictions() {
        return this.manaRestrictions;
    }

    /**
     * <p>
     * meetsManaRestrictions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public boolean meetsManaRestrictions(final SpellAbility sa) {
        // No restrictions
        if (this.manaRestrictions.isEmpty()) {
            return true;
        }

        // Loop over restrictions
        for (String restriction : this.manaRestrictions.split(",")) {
            if (restriction.startsWith("CostContainsX")) {
                if (sa.isXCost()) {
                    return true;
                }
                else {
                    continue;
                }
            }

            if (sa.isAbility()) {
                if (restriction.startsWith("Activated")) {
                    restriction = restriction.replace("Activated", "Card");
                }
                else {
                    continue;
                }
            }

            if (sa.getSourceCard() != null) {
                if (sa.getSourceCard().isValid(restriction, this.getSourceCard().getController(), this.getSourceCard())) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * <p>
     * mana.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String mana() {
        if (this.getOrigProduced().contains("Chosen")) {
            if (this.getSourceCard() != null && !this.getSourceCard().getChosenColor().isEmpty()) {
                return InputPayManaCostUtil.getShortColorString(this.getSourceCard()
                        .getChosenColor().get(0));
            }
        }
        return this.getOrigProduced();
    }

    /**
     * <p>
     * setReflectedMana.
     * </p>
     * 
     * @param bReflect
     *            a boolean.
     */
    public final void setReflectedMana(final boolean bReflect) {
        this.reflected = bReflect;
    }

    /**
     * <p>
     * setAnyChoice.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setExpressChoice(String s) {
        this.lastExpressChoice = s;
    }

    /**
     * <p>
     * Getter for the field <code>lastAnyChoice</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExpressChoice() {
        return this.lastExpressChoice;
    }

    /**
     * <p>
     * clearExpressChoice.
     * </p>
     *
     */
    public void clearExpressChoice() {
        this.lastExpressChoice = "";
    }

    /**
     * <p>
     * Getter for the field <code>lastProduced</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public ArrayList<Mana> getLastProduced() {
        return this.lastProduced;
    }

    /**
     * <p>
     * isSnow.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSnow() {
        return this.getSourceCard().isSnow();
    }

    /**
     * <p>
     * isSacrifice.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSacrifice() {
        return this.cost.getSacCost();
    }

    /**
     * <p>
     * isReflectedMana.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isReflectedMana() {
        return this.reflected;
    }

    /**
     * <p>
     * isAnyMana.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAnyMana() {
        return this.getOrigProduced().contains("Any");
    }

    /**
     * <p>
     * isComboMana.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isComboMana() {
        return this.getOrigProduced().contains("Combo");
    }

    /**
     * <p>
     * canProduce.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean canProduce(final String s) {
        if (isAnyMana()) {
            return true;
        }

        if (this.getOrigProduced().contains("Chosen")) {
            if (this.getSourceCard() != null && !this.getSourceCard().getChosenColor().isEmpty()
                    && InputPayManaCostUtil.getShortColorString(this.getSourceCard().getChosenColor().get(0))
                    .contains(s)) {
                return true;
            }
        }
        return this.getOrigProduced().contains(s);
    }

    /**
     * <p>
     * isBasic.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBasic() {
        if (this.getOrigProduced().length() != 1 && !this.getOrigProduced().contains("Any")
                && !this.getOrigProduced().contains("Chosen")) {
            return false;
        }

        if (this.amount > 1) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * isUndoable.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isUndoable() {
        return this.undoable && this.cost.isUndoable() && this.getSourceCard().isInPlay();
    }

    /**
     * <p>
     * Setter for the field <code>undoable</code>.
     * </p>
     * 
     * @param bUndo
     *            a boolean.
     */
    public final void setUndoable(final boolean bUndo) {
        this.undoable = bUndo;
    }

    /**
     * <p>
     * Setter for the field <code>canceled</code>.
     * </p>
     * 
     * @param bCancel
     *            a boolean.
     */
    public final void setCanceled(final boolean bCancel) {
        this.canceled = bCancel;
    }

    /**
     * <p>
     * Getter for the field <code>canceled</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCanceled() {
        return this.canceled;
    }

    /**
     * <p>
     * undo.
     * </p>
     */
    public final void undo() {
        if (this.isUndoable()) {
            this.cost.refundPaidCost(this.getSourceCard());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        // Mana abilities with same Descriptions are "equal"
        if ((o == null) || !(o instanceof AbilityManaPart)) {
            return false;
        }

        final AbilityManaPart abm = (AbilityManaPart) o;

//        if (abm.getType() != this.getType()) {
//            return false;
//        }
        
        return cost.equals(abm.cost) && sourceCard.equals(abm.sourceCard); 

//        return abm.toUnsuppressedString().equals(this.toUnsuppressedString());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getSourceCard().hashCode()));
    }

    /**
     * @return the origProduced
     */
    public String getOrigProduced() {
        return origProduced;
    }

    /**
     * @return the color available in combination mana
     */
    public String getComboColors() {
        String retVal = "";
        if (this.getOrigProduced().contains("Combo")) {
            retVal = this.getOrigProduced().replace("Combo ", "");
            if (retVal.contains("Any")) {
                retVal = "W U B R G";
            }
        }
        return retVal;
    }

    public Card getSourceCard() {
        return sourceCard;
    }

} // end class AbilityMana

