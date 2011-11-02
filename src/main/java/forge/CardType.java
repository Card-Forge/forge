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
    private boolean removeSuperTypes;
    private boolean removeCardTypes;
    private boolean removeSubTypes;
    private boolean removeCreatureTypes;
    private long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public final long getTimestamp() {
        return timeStamp;
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
        type = types;
        removeType = removeTypes;
        removeSuperTypes = removeSuperType;
        removeCardTypes = removeCardType;
        removeSubTypes = removeSubType;
        removeCreatureTypes = removeCreatureType;
        timeStamp = stamp;
    }

    /**
     * 
     * getType.
     * 
     * @return type
     */
    public final ArrayList<String> getType() {
        return type;
    }

    /**
     * 
     * getRemoveType.
     * 
     * @return removeType
     */
    public final ArrayList<String> getRemoveType() {
        return removeType;
    }

    /**
     * 
     * isRemoveSuperTypes.
     * 
     * @return removeSuperTypes
     */
    public final boolean isRemoveSuperTypes() {
        return removeSuperTypes;
    }

    /**
     * 
     * isRemoveCardTypes.
     * 
     * @return removeCardTypes
     */
    public final boolean isRemoveCardTypes() {
        return removeCardTypes;
    }

    /**
     * 
     * isRemoveSubTypes.
     * 
     * @return removeSubTypes
     */
    public final boolean isRemoveSubTypes() {
        return removeSubTypes;
    }

    /**
     * 
     * isRemoveCreatureTypes.
     * 
     * @return removeCreatureTypes
     */
    public final boolean isRemoveCreatureTypes() {
        return removeCreatureTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final CardType anotherCardType) {
        int returnValue = 0;
        long anotherTimeStamp = anotherCardType.getTimestamp();
        if (this.timeStamp < anotherTimeStamp) {
            returnValue = -1;
        } else if (this.timeStamp > anotherTimeStamp) {
            returnValue = 1;
        }
        return returnValue;
    }

}
