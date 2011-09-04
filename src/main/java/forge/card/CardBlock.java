package forge.card;

import java.util.List;

/**
 * This is a CardBlock class.
 */
public class CardBlock implements Comparable<CardBlock> {
    private static final CardSet[] EMPTY_SET_ARRAY = new CardSet[]{};

    private final int orderNum;
    private final String name;
    private final CardSet[] sets;
    private final CardSet landSet;
    private final int cntBoostersDraft;
    private final int cntBoostersSealed;

    public CardBlock(final int index, final String name, final List<CardSet> sets,
            final CardSet landSet, final int cntBoostersDraft, final int cntBoostersSealed) {
        this.orderNum = index;
        this.name = name;
        this.sets = sets.toArray(EMPTY_SET_ARRAY);
        this.landSet = landSet;
        this.cntBoostersDraft = cntBoostersDraft;
        this.cntBoostersSealed = cntBoostersSealed;
    }

    public final String getName() {
        return name;
    }

    public final CardSet[] getSets() {
        return sets;
    }

    public final CardSet getLandSet() {
        return landSet;
    }

    public final int getCntBoostersDraft() {
        return cntBoostersDraft;
    }

    public final int getCntBoostersSealed() {
        return cntBoostersSealed;
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
    public String toString() { return name; }

}
