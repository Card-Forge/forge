package forge.card;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;

import net.slightlymagic.maxmtg.Predicate;

/**
 * This is a CardBlock class.
 */
public final class CardBlock implements Comparable<CardBlock> {
    private static final CardSet[] EMPTY_SET_ARRAY = new CardSet[]{};

    private final int orderNum;
    private final String name;
    private final CardSet[] sets;
    private final CardSet landSet;
    private final int cntBoostersDraft;
    private final int cntBoostersSealed;
    private Predicate<CardPrinted> filter = null;

    public CardBlock(final int index, final String name, final List<CardSet> sets,
            final CardSet landSet, final int cntBoostersDraft, final int cntBoostersSealed) {
        this.orderNum = index;
        this.name = name;
        this.sets = sets.toArray(EMPTY_SET_ARRAY);
        this.landSet = landSet;
        this.cntBoostersDraft = cntBoostersDraft;
        this.cntBoostersSealed = cntBoostersSealed;
    }

    public String getName() {
        return name;
    }

    public CardSet[] getSets() {
        return sets;
    }

    public CardSet getLandSet() {
        return landSet;
    }

    public int getCntBoostersDraft() {
        return cntBoostersDraft;
    }

    public int getCntBoostersSealed() {
        return cntBoostersSealed;
    }
    
    public Predicate<CardPrinted> getFilter()
    {
        if (filter == null) { filter = buildFilter(); }
        return filter;
    }
    
    private Predicate<CardPrinted> buildFilter()
    {
        List<String> setCodes = new ArrayList<String>();
        for (int i = 0; i < sets.length; i++) {
            setCodes.add(sets[i].getCode());
        }
        return CardPrinted.Predicates.printedInSets(setCodes, true);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((landSet == null) ? 0 : landSet.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        CardBlock other = (CardBlock) obj;
        if (!landSet.equals(other.landSet)) { return false; }
        if (!name.equals(other.name)) { return false; }
        return true;
    }

    @Override
    public int compareTo(final CardBlock o) {
        return this.orderNum - o.orderNum;
    }

    @Override
    public String toString() { return name + " (block)"; }

}
