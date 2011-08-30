package forge.card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>CardManaCost class.</p>
 *
 * @author Forge
 * @version $Id: CardManaCost.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class CardManaCost implements Comparable<CardManaCost> {
    private final List<CardManaCostShard> shards = new ArrayList<CardManaCostShard>();
    private final int genericCost;
    private final boolean isEmpty; // lands cost
    private final String stringValue; // precalculated for toString;
    
    private Float compareWeight = null;
    
    public static final CardManaCost empty = new CardManaCost();
    
    // pass mana cost parser here
    private CardManaCost() {
        isEmpty = true;
        genericCost = 0;
        stringValue = "";
    }

    // public ctor, should give it a mana parser
    public CardManaCost(final ManaParser parser) {
        if (!parser.hasNext()) {
            throw new RuntimeException("Empty manacost passed to parser (this should have been handled before)");
        }
        isEmpty = false;
        while (parser.hasNext()) {
            CardManaCostShard shard = parser.next();
            if (shard != null) { shards.add(shard); } // null is OK - that was generic mana
        }
        genericCost = parser.getTotalColorlessCost(); // collect generic mana here
        stringValue = getSimpleString();
    }

    private String getSimpleString() {
        if (shards.isEmpty()) { return Integer.toString(genericCost); }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = false;
        if (genericCost > 0) { sb.append(genericCost); isFirst = false; }
        for (CardManaCostShard s : shards) {
            if (!isFirst) { sb.append(' '); }
            else { isFirst = false; }
            sb.append(s.toString());
        }
        return sb.toString();
    }

    public int getCMC() {
        int sum = 0;
        for (CardManaCostShard s : shards) { sum += s.cmc; }
        return sum + genericCost;
    }

    public byte getColorProfile() {
        byte result = 0;
        for (CardManaCostShard s : shards) { result |= s.getColorMask(); }
        return result;
    }

    @Override
    public int compareTo(final CardManaCost o) { return getCompareWeight().compareTo(o.getCompareWeight()); }
    private Float getCompareWeight() {
        if (compareWeight == null) {
            float weight = genericCost;
            for (CardManaCostShard s : shards) { weight += s.cmpc; }
            if (isEmpty) {
                weight = -1; // for those who doesn't even have a 0 sign on card
            }
            compareWeight = Float.valueOf(weight);
        }
        return compareWeight;
    }
    
    @Override
    public String toString() {
        return stringValue;
    }

    public interface ManaParser extends Iterator<CardManaCostShard>
    {
        int getTotalColorlessCost(); 
    }
    

}

