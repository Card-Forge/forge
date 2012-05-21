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

import forge.Card;
import forge.Constant;
import forge.card.spellability.AbilityMana;
import forge.control.input.InputPayManaCostUtil;

/**
 * <p>
 * Mana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana {
    private String color;
    private Card sourceCard = null;
    private AbilityMana sourceAbility = null;
    private boolean hasRestrictions = false;
    private boolean pumpCounterMagic = false;

    /**
     * <p>
     * Constructor for Mana.
     * </p>
     * 
     * @param col
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @param manaAbility
     *            a {@link forge.card.spellability.AbilityMana} object
     */
    public Mana(final String col, final Card source, final AbilityMana manaAbility) {
        this.color = col;
        if (manaAbility != null) {
          this.sourceAbility = manaAbility;
          if (manaAbility.getManaRestrictions() != null) {
              this.hasRestrictions = true;
          }
          if (manaAbility.cannotCounterPaidWith()) {
              this.pumpCounterMagic = true;
          }
        }
        if (source == null) {
            return;
        }

        this.sourceCard = source;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        String manaString = "";
        if (this.color.equals(Constant.Color.COLORLESS)) {
            manaString = "1";
        }
        else {
            manaString = InputPayManaCostUtil.getShortColorString(this.color);
        }

        return manaString;
    }

    /**
     * <p>
     * isSnow.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSnow() {
        return this.sourceCard.isSnow();
    }

    /**
     * <p>
     * isRestricted.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isRestricted() {
        return this.hasRestrictions;
    }

    /**
     * <p>
     * isRestricted.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean addsNoCounterMagic() {
        return this.pumpCounterMagic;
    }

    /**
     * <p>
     * fromBasicLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean fromBasicLand() {
        return this.sourceCard.isBasicLand();
    } // for Imperiosaur

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param col
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isColor(final String col) {
        return this.color.equals(col);
    }

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param colors
     *            an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public final boolean isColor(final String[] colors) {
        for (final String col : colors) {
            if (this.color.equals(col)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Getter for the field <code>color</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getColor() {
        return this.color;
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSourceCard() {
        return this.sourceCard;
    }

    /**
     * <p>
     * fromSourceCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean fromSourceCard(final Card c) {
        return this.sourceCard.equals(c);
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.AbilityMana} object.
     */
    public final AbilityMana getSourceAbility() {
        return this.sourceAbility;
    }

    /**
     * <p>
     * fromSourceCard.
     * </p>
     * 
     * @param ma
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean fromSourceAbility(final AbilityMana ma) {
        return this.sourceAbility.equals(ma);
    }

}
