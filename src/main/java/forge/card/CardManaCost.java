package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * CardManaCost class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardManaCost.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class CardManaCost implements Comparable<CardManaCost> {
    private final List<CardManaCostShard> shards;
    private final int genericCost;
    private final boolean hasNoCost; // lands cost
    private final String stringValue; // precalculated for toString;

    private Float compareWeight = null;

    /** The Constant empty. */
    public static final CardManaCost empty = new CardManaCost();

    // pass mana cost parser here
    private CardManaCost() {
        hasNoCost = true;
        genericCost = 0;
        stringValue = "";
        shards = Collections.unmodifiableList(new ArrayList<CardManaCostShard>());
    }

    // public ctor, should give it a mana parser
    /**
     * Instantiates a new card mana cost.
     * 
     * @param parser
     *            the parser
     */
    public CardManaCost(final ManaParser parser) {
        if (!parser.hasNext()) {
            throw new RuntimeException("Empty manacost passed to parser (this should have been handled before)");
        }
        List<CardManaCostShard> shardsTemp = new ArrayList<CardManaCostShard>();
        hasNoCost = false;
        while (parser.hasNext()) {
            CardManaCostShard shard = parser.next();
            if (shard != null) {
                shardsTemp.add(shard);
            } // null is OK - that was generic mana
        }
        genericCost = parser.getTotalColorlessCost(); // collect generic mana
                                                      // here
        shards = Collections.unmodifiableList(shardsTemp);
        stringValue = getSimpleString();

    }

    private String getSimpleString() {
        if (shards.isEmpty()) {
            return Integer.toString(genericCost);
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        if (genericCost > 0) {
            sb.append(genericCost);
            isFirst = false;
        }
        for (CardManaCostShard s : shards) {
            if (!isFirst) {
                sb.append(' ');
            } else {
                isFirst = false;
            }
            sb.append(s.toString());
        }
        return sb.toString();
    }

    /**
     * Gets the cMC.
     * 
     * @return the cMC
     */
    public int getCMC() {
        int sum = 0;
        for (CardManaCostShard s : shards) {
            sum += s.cmc;
        }
        return sum + genericCost;
    }

    /**
     * Gets the color profile.
     * 
     * @return the color profile
     */
    public byte getColorProfile() {
        byte result = 0;
        for (CardManaCostShard s : shards) {
            result |= s.getColorMask();
        }
        return result;
    }

    /**
     * Gets the shards.
     * 
     * @return the shards
     */
    public List<CardManaCostShard> getShards() {
        return shards;
    }

    /**
     * Gets the generic cost.
     * 
     * @return the generic cost
     */
    public int getGenericCost() {
        return genericCost;
    }

    /**
     * Checks if is empty.
     * 
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return hasNoCost;
    }

    /**
     * Checks if is pure generic.
     * 
     * @return true, if is pure generic
     */
    public boolean isPureGeneric() {
        return shards.isEmpty() && !isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardManaCost o) {
        return getCompareWeight().compareTo(o.getCompareWeight());
    }

    private Float getCompareWeight() {
        if (compareWeight == null) {
            float weight = genericCost;
            for (CardManaCostShard s : shards) {
                weight += s.cmpc;
            }
            if (hasNoCost) {
                weight = -1; // for those who doesn't even have a 0 sign on card
            }
            compareWeight = Float.valueOf(weight);
        }
        return compareWeight;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return stringValue;
    }

    /**
     * The Interface ManaParser.
     */
    public interface ManaParser extends Iterator<CardManaCostShard> {

        /**
         * Gets the total colorless cost.
         * 
         * @return the total colorless cost
         */
        int getTotalColorlessCost();
    }

}
