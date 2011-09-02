package forge;

import java.util.ArrayList;


/**
 * <p>Card_Color class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Card_Type implements Comparable<Card_Type> {
    // takes care of individual card types
    private ArrayList<String> type = new ArrayList<String>();
    private boolean removeSuperTypes;
    private boolean removeCardTypes;
    private boolean removeSubTypes;
    private boolean removeCreatureTypes;
    private long timeStamp = 0;

    /**
     * <p>getTimestamp.</p>
     *
     * @return a long.
     */
    public final long getTimestamp() {
        return timeStamp;
    }

    /**
     *
     * @param types an ArrayList<String>
     * @param removeSuperType a boolean
     * @param removeCardType a boolean
     * @param removeSubType a boolean
     * @param removeCreatureType a boolean
     * @param stamp a long
     */
    Card_Type(final ArrayList<String> types, final boolean removeSuperType, final boolean removeCardType,
            final boolean removeSubType, final boolean removeCreatureType, final long stamp)
            {
        type = types;
        removeSuperTypes = removeSuperType;
        removeCardTypes = removeCardType;
        removeSubTypes = removeSubType;
        removeCreatureTypes = removeCreatureType;
        timeStamp = stamp;
            }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return type
     */
    public final ArrayList<String> getType() {
        return type;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return removeSuperTypes
     */
    public final boolean isRemoveSuperTypes() {
        return removeSuperTypes;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return removeCardTypes
     */
    public final boolean isRemoveCardTypes() {
        return removeCardTypes;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return removeSubTypes
     */
    public final boolean isRemoveSubTypes() {
        return removeSubTypes;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return removeCreatureTypes
     */
    public final boolean isRemoveCreatureTypes() {
        return removeCreatureTypes;
    }

    @Override
    public final int compareTo(final Card_Type anotherCardType) {
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
