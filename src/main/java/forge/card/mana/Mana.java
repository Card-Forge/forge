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
import forge.card.MagicColor;
import forge.card.spellability.AbilityManaPart;

/**
 * <p>
 * Mana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana {
    private byte color;
    private Card sourceCard = null;
    private AbilityManaPart manaAbility = null;
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
    public Mana(final String col, final Card source, final AbilityManaPart manaAbility) {
        this.color = MagicColor.fromName(col);
        if (manaAbility != null) {
          this.manaAbility = manaAbility;
          if (!manaAbility.getManaRestrictions().isEmpty()) {
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
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Mana)) return false;
        Mana m2 = (Mana) other;

        if ( color != m2.color )
            return false;

        AbilityManaPart mp = this.getManaAbility();
        AbilityManaPart mp2 = m2.getManaAbility();
        if ( (mp == null) != (mp2 == null) )
            return false;

        return mp == mp2 || mp.getManaRestrictions().equals(mp2.getManaRestrictions());
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
        return MagicColor.toShortString(color);
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
     * isColor.
     * </p>
     * 
     * @param col
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isColor(final String col) {
        return this.getColor().equals(col);
    }

    /**
     * <p>
     * Getter for the field <code>color</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getColor() {
        return MagicColor.toLongString(this.color);
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
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.AbilityMana} object.
     */
    public final AbilityManaPart getManaAbility() {
        return this.manaAbility;
    }

    public byte getColorCode() {
        return color;
    }


    public boolean isColorless() {
        return color == 0;
    }

}
