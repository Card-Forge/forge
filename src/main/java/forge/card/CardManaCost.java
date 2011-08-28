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
    

    // pass mana cost parser here
    private CardManaCost() {
        isEmpty = true;
        genericCost = 0;
        stringValue = "";
    }

    public static final CardManaCost empty = new CardManaCost();
    
    public CardManaCost(final String cost) {
        ParserMtgData parser = new ParserMtgData(cost);
        if (!parser.hasNext()) {
            throw new RuntimeException("Empty manacost passed to parser (this should have been handled before)");
        }
        isEmpty = false;
        while (parser.hasNext()) {
            CardManaCostShard shard = parser.next();
            if (shard != null) { shards.add(shard); } // null is OK - that was generic mana
        }
        genericCost = parser.getColorlessCost(); // collect generic mana here
        stringValue = getSimpleString();
    }

    private String getSimpleString() {
        if (shards.isEmpty()) return Integer.toString(genericCost);
        
        StringBuilder sb = new StringBuilder();
        boolean isFirst = false;
        if (genericCost > 0) { sb.append(genericCost); isFirst = false; }
        for (CardManaCostShard s : shards) {
            if ( !isFirst ) { sb.append(' '); }
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

    public class ParserMtgData implements Iterator<CardManaCostShard> {
        private final String cost;

        private int nextBracket;
        private int colorlessCost;


        public ParserMtgData(final String cost) {
            this.cost = cost;
            // System.out.println(cost);
            nextBracket = cost.indexOf('{');
            colorlessCost = 0;
        }
        
        public int getColorlessCost() { 
            if ( hasNext() ) { 
                throw new RuntimeException("Colorless cost should be obtained after iteration is complete");
            }
            return colorlessCost;
        }

        @Override
        public boolean hasNext() { return nextBracket != -1; }

        @Override
        public CardManaCostShard next() {
            int closeBracket = cost.indexOf('}', nextBracket);
            String unparsed = cost.substring(nextBracket + 1, closeBracket);
            nextBracket = cost.indexOf('{', closeBracket + 1);

            // System.out.println(unparsed);
            if (StringUtils.isNumeric(unparsed)) {
                colorlessCost += Integer.parseInt(unparsed);
                return null;
            }

            int atoms = 0;
            for (int iChar = 0; iChar < unparsed.length(); iChar++) {
                switch (unparsed.charAt(iChar)) {
                    case 'W': atoms |= CardManaCostShard.Atom.WHITE; break;
                    case 'U': atoms |= CardManaCostShard.Atom.BLUE; break;
                    case 'B': atoms |= CardManaCostShard.Atom.BLACK; break;
                    case 'R': atoms |= CardManaCostShard.Atom.RED; break;
                    case 'G': atoms |= CardManaCostShard.Atom.GREEN; break;
                    case '2': atoms |= CardManaCostShard.Atom.OR_2_COLORLESS; break;
                    case 'P': atoms |= CardManaCostShard.Atom.OR_2_LIFE; break;
                    case 'X': atoms |= CardManaCostShard.Atom.IS_X; break;
                    default: break;
                }
            }
            return CardManaCostShard.valueOf(atoms);
        }

        @Override
        public void remove() { } // unsuported
    }
}

