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
package forge;

import java.util.ArrayList;

/**
 * <p>
 * Card_Color class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardType implements Comparable<CardType> {
    // takes care of individual card types
    private ArrayList<String> type = new ArrayList<String>();
    private ArrayList<String> removeType = new ArrayList<String>();
    private final boolean removeSuperTypes;
    private final boolean removeCardTypes;
    private final boolean removeSubTypes;
    private final boolean removeCreatureTypes;
    private long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public final long getTimestamp() {
        return this.timeStamp;
    }

    /**
     * Instantiates a new card_ type.
     * 
     * @param types
     *            an ArrayList<String>
     * @param removeTypes
     *            an ArrayList<String>
     * @param removeSuperType
     *            a boolean
     * @param removeCardType
     *            a boolean
     * @param removeSubType
     *            a boolean
     * @param removeCreatureType
     *            a boolean
     * @param stamp
     *            a long
     */
    CardType(final ArrayList<String> types, final ArrayList<String> removeTypes, final boolean removeSuperType,
            final boolean removeCardType, final boolean removeSubType, final boolean removeCreatureType,
            final long stamp) {
        this.type = types;
        this.removeType = removeTypes;
        this.removeSuperTypes = removeSuperType;
        this.removeCardTypes = removeCardType;
        this.removeSubTypes = removeSubType;
        this.removeCreatureTypes = removeCreatureType;
        this.timeStamp = stamp;
    }

    /**
     * 
     * getType.
     * 
     * @return type
     */
    public final ArrayList<String> getType() {
        return this.type;
    }

    /**
     * 
     * getRemoveType.
     * 
     * @return removeType
     */
    public final ArrayList<String> getRemoveType() {
        return this.removeType;
    }

    /**
     * 
     * isRemoveSuperTypes.
     * 
     * @return removeSuperTypes
     */
    public final boolean isRemoveSuperTypes() {
        return this.removeSuperTypes;
    }

    /**
     * 
     * isRemoveCardTypes.
     * 
     * @return removeCardTypes
     */
    public final boolean isRemoveCardTypes() {
        return this.removeCardTypes;
    }

    /**
     * 
     * isRemoveSubTypes.
     * 
     * @return removeSubTypes
     */
    public final boolean isRemoveSubTypes() {
        return this.removeSubTypes;
    }

    /**
     * 
     * isRemoveCreatureTypes.
     * 
     * @return removeCreatureTypes
     */
    public final boolean isRemoveCreatureTypes() {
        return this.removeCreatureTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final CardType anotherCardType) {
        int returnValue = 0;
        final long anotherTimeStamp = anotherCardType.getTimestamp();
        if (this.timeStamp < anotherTimeStamp) {
            returnValue = -1;
        } else if (this.timeStamp > anotherTimeStamp) {
            returnValue = 1;
        }
        return returnValue;
    }

}
