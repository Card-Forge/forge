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
    public long getTimestamp() {
        return timeStamp;
    }

    /**
     * <p>Constructor for Card_PT.</p>
     *
     * @param mc          a {@link forge.card.mana.ManaCost} object.
     * @param c           a {@link forge.Card} object.
     * @param addToColors a boolean.
     * @param baseColor   a boolean.
     */
    Card_Type(ArrayList<String> types, boolean removeSuperType, boolean removeCardType, boolean removeSubType, 
    		boolean removeCreatureType, long stamp) {
    	type = types;
    	removeSuperTypes = removeSuperType;
        removeCardTypes = removeCardType;
        removeSubTypes = removeSubType;
        removeCreatureTypes = removeCreatureType;
    	timeStamp = stamp;
    }

    public ArrayList<String> getType() {
    	return type;
    }

    public boolean isRemoveSuperTypes() {
    	return removeSuperTypes;
    }

    public boolean isRemoveCardTypes() {
    	return removeCardTypes;
    }

    public boolean isRemoveSubTypes() {
    	return removeSubTypes;
    }

    public boolean isRemoveCreatureTypes() {
    	return removeCreatureTypes;
    }

    @Override
    public int compareTo(final Card_Type anotherCardType) {
        int returnValue = 0;
        long anotherTimeStamp = anotherCardType.getTimestamp();
        if (this.timeStamp < anotherTimeStamp)
            returnValue = -1;
        else if (this.timeStamp > anotherTimeStamp)
            returnValue = 1;
        return returnValue;    
    }

}
