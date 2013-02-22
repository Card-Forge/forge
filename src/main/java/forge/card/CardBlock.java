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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.util.storage.StorageReaderFile;

// import forge.deck.Deck;

/**
 * This is a CardBlock class.
 */
public final class CardBlock implements Comparable<CardBlock> {
    private static final CardEdition[] EMPTY_SET_ARRAY = new CardEdition[] {};

    private final int orderNum;
    private final String name;
    private final CardEdition[] sets;
    private final ArrayList<MetaSet> metaSets;
    private final CardEdition landSet;
    private final int cntBoostersDraft;
    private final int cntBoostersSealed;
    private Predicate<CardPrinted> filter = null;

    /**
     * Instantiates a new card block.
     * 
     * @param index
     *            the index
     * @param name
     *            the name
     * @param sets
     *            the sets
     * @param metas
     *            the included meta-sets
     * @param landSet
     *            the land set
     * @param cntBoostersDraft
     *            the cnt boosters draft
     * @param cntBoostersSealed
     *            the cnt boosters sealed
     */
    public CardBlock(final int index, final String name, final List<CardEdition> sets, final ArrayList<MetaSet> metas,
            final CardEdition landSet, final int cntBoostersDraft, final int cntBoostersSealed) {
        this.orderNum = index;
        this.name = name;
        this.sets = sets.toArray(CardBlock.EMPTY_SET_ARRAY);
        this.metaSets = metas;
        this.landSet = landSet;
        this.cntBoostersDraft = cntBoostersDraft;
        this.cntBoostersSealed = cntBoostersSealed;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the sets.
     * 
     * @return the sets
     */
    public CardEdition[] getSets() {
        return this.sets;
    }

    /**
     * Gets the land set.
     * 
     * @return the land set
     */
    public CardEdition getLandSet() {
        return this.landSet;
    }

    /**
     * Gets the cnt boosters draft.
     * 
     * @return the cnt boosters draft
     */
    public int getCntBoostersDraft() {
        return this.cntBoostersDraft;
    }

    /**
     * Gets the cnt boosters sealed.
     * 
     * @return the cnt boosters sealed
     */
    public int getCntBoostersSealed() {
        return this.cntBoostersSealed;
    }

    /**
     * Gets the filter.
     * 
     * @return the filter
     */
    public Predicate<CardPrinted> getFilter() {
        if (this.filter == null) {
            this.filter = this.buildFilter();
        }
        return this.filter;
    }

    private Predicate<CardPrinted> buildFilter() {
        final List<String> setCodes = new ArrayList<String>();
        for (final CardEdition set : this.sets) {
            setCodes.add(set.getCode());
        }
        return IPaperCard.Predicates.printedInSets(setCodes, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.landSet == null) ? 0 : this.landSet.hashCode());
        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final CardBlock other = (CardBlock) obj;
        if (!this.landSet.equals(other.landSet)) {
            return false;
        }
        if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardBlock o) {
        return this.orderNum - o.orderNum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (this.getNumberMetaSets() + this.getNumberSets() < 1) {
            return this.name + " (empty)";
        } else if (this.getNumberMetaSets() + this.getNumberSets() < 2) {
            return this.name + " (set)";
        }
        return this.name + " (block)";
    }

    public static final Function<CardBlock, String> FN_GET_NAME = new Function<CardBlock, String>() {

        @Override
        public String apply(CardBlock arg1) {
            return arg1.getName();
        }
    };

    public static class Reader extends StorageReaderFile<CardBlock> {

        private final EditionCollection editions;
        /**
         * TODO: Write javadoc for Constructor.
         * @param pathname
         * @param keySelector0
         */
        public Reader(String pathname, EditionCollection editions0) {
            super(pathname, CardBlock.FN_GET_NAME);
            editions = editions0;
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected CardBlock read(String line) {
            final String[] sParts = line.trim().split("\\|");

            String name = null;
            int index = -1;
            final List<CardEdition> sets = new ArrayList<CardEdition>(9); // add support for up to 9 different sets in a block!
            final ArrayList<MetaSet> metas = new ArrayList<MetaSet>();
            CardEdition landSet = null;
            int draftBoosters = 3;
            int sealedBoosters = 6;

            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("index".equals(key)) {
                    index = Integer.parseInt(kv[1]);
                } else if ("set0".equals(key) || "set1".equals(key) || "set2".equals(key) || "set3".equals(key)
                        || "set4".equals(key) || "set5".equals(key) || "set6".equals(key) || "set7".equals(key)
                        || "set8".equals(key)) {
                    sets.add(editions.getEditionByCodeOrThrow(kv[1]));
                } else if ("meta0".equals(key) || "meta1".equals(key) || "meta2".equals(key) || "meta3".equals(key)
                        || "meta4".equals(key) || "meta5".equals(key) || "meta6".equals(key) || "meta7".equals(key)
                        || "meta8".equals(key)) {
                    metas.add(new MetaSet(kv[1]));
                } else if ("landsetcode".equals(key)) {
                    landSet = editions.getEditionByCodeOrThrow(kv[1]);
                } else if ("draftpacks".equals(key)) {
                    draftBoosters = Integer.parseInt(kv[1]);
                } else if ("sealedpacks".equals(key)) {
                    sealedBoosters = Integer.parseInt(kv[1]);
                }

            }
            return new CardBlock(index, name, sets, metas, landSet, draftBoosters, sealedBoosters);
        }

    }

    /**
     * The number of normal sets in the block.
     *
     * @return int, number of sets.
     */
    public int getNumberSets() {
        if (sets == null || sets.length < 1) {
            return 0;
        }
        else {
            return sets.length;
        }
    }

    /**
     * The number of meta-sets in the block.
     *
     * @return int, number of meta-sets.
     */
    public int getNumberMetaSets() {
        if (metaSets == null || metaSets.size() < 1) {
            return 0;
        }
        else {
            return metaSets.size();
        }
    }

    /**
     * Returns the requested meta-set.
     *
     * @param index
     *      int, the requested index
     * @return MetaSet, the requested meta-set.
     */
    public MetaSet getMetaSet(final int index) {
            if (index < 0 || index > this.getNumberMetaSets() - 1) {
                throw new RuntimeException("Illegal MetaSet requested: " + index);
            }
            else {
                return metaSets.get(index);
            }
    }

    /**
     * Returns true if there is a meta-set of the requested type.
     *
     * @param compare
     *      String, the requested the requested type
     * @return boolean, the requsted type was found
     */
    public boolean hasMetaSetType(final String compare) {

        if (this.getNumberMetaSets() < 1) {
            return false;
        }

        for (MetaSet mSet : metaSets) {
            if (mSet.getType().equalsIgnoreCase(compare)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to create a booster for the selected meta-set code.
     *
     * @param code
     *      String, the MetaSet code
     * @return UnOpenedProduct, the created booster.
     */
    public UnOpenedProduct getBooster(final String code) {

        if (this.getNumberMetaSets() < 1) {
            throw new RuntimeException("Attempted to get a booster pack for empty metasets.");
        }
        else {
            for (int i = 0; i < this.getNumberMetaSets(); i++) {
                if (code.equals(metaSets.get(i).getCode())) {
                    return metaSets.get(i).getBooster();
                }
            }
            throw new RuntimeException("Could not find metaset " + code + " for booster generation.");
        }
    }
}
