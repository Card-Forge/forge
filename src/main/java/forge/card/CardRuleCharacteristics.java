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
package forge.card;

import java.util.Map;
import java.util.TreeMap;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class CardRuleCharacteristics {
    private String cardName = null;
    private CardType cardType = null;
    private CardManaCost manaCost = CardManaCost.EMPTY;
    private CardColor color = null;
    private String ptLine = null;
    private String[] cardRules = null;
    private Map<String, CardInSet> setsData = new TreeMap<String, CardInSet>();

    /**
     * Gets the card name.
     * 
     * @return the cardName
     */
    public final String getCardName() {
        return this.cardName;
    }

    /**
     * Sets the card name.
     * 
     * @param cardName0
     *            the cardName to set
     */
    public final void setCardName(final String cardName0) {
        this.cardName = cardName0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the card type.
     * 
     * @return the cardType
     */
    public final CardType getCardType() {
        return this.cardType;
    }

    /**
     * Sets the card type.
     * 
     * @param cardType0
     *            the cardType to set
     */
    public final void setCardType(final CardType cardType0) {
        this.cardType = cardType0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the mana cost.
     * 
     * @return the manaCost
     */
    public final CardManaCost getManaCost() {
        return this.manaCost;
    }

    /**
     * Sets the mana cost.
     * 
     * @param manaCost0
     *            the manaCost to set
     */
    public final void setManaCost(final CardManaCost manaCost0) {
        this.manaCost = manaCost0; // TODO: Add 0 to parameter's name.
        this.color = new CardColor(this.manaCost);
    }

    /**
     * Gets the color.
     * 
     * @return the color
     */
    public final CardColor getColor() {
        return this.color;
    }

    /**
     * Sets the color.
     * 
     * @param color0
     *            the color to set
     */
    public final void setColor(final CardColor color0) {
        this.color = color0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the pt line.
     * 
     * @return the ptLine
     */
    public final String getPtLine() {
        return this.ptLine;
    }

    /**
     * Sets the pt line.
     * 
     * @param ptLine0
     *            the ptLine to set
     */
    public final void setPtLine(final String ptLine0) {
        this.ptLine = ptLine0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the card rules.
     * 
     * @return the cardRules
     */
    public final String[] getCardRules() {
        return this.cardRules;
    }

    /**
     * Sets the card rules.
     * 
     * @param cardRules0
     *            the cardRules to set
     */
    public final void setCardRules(final String[] cardRules0) {
        this.cardRules = cardRules0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the sets data.
     * 
     * @return the setsData
     */
    public final Map<String, CardInSet> getSetsData() {
        return this.setsData;
    }

    /**
     * Sets the sets data.
     * 
     * @param setsData0
     *            the setsData to set
     */
    public final void setSetsData(final Map<String, CardInSet> setsData0) {
        this.setsData = setsData0; // TODO: Add 0 to parameter's name.
    }
}
