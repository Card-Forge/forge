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
package forge.model;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.card.CardEdition;
import forge.card.IUnOpenedProduct;
import forge.card.UnOpenedProduct;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// import forge.deck.Deck;

/**
 * This is a CardBlock class.
 */
public final class CardBlock implements Comparable<CardBlock> {
    private final int orderNum;
    private final String name;
    private final List<CardEdition> sets;
    private final Map<String, MetaSet> metaSets = new TreeMap<String, MetaSet>();
    private final CardEdition landSet;
    private final int cntBoostersDraft;
    private final int cntBoostersSealed;
    private Predicate<PaperCard> filter = null;

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
    public CardBlock(final int index, final String name, final List<CardEdition> sets, final List<MetaSet> metas,
            final CardEdition landSet, final int cntBoostersDraft, final int cntBoostersSealed) {
        this.orderNum = index;
        this.name = name;
        this.sets = java.util.Collections.unmodifiableList(sets);
        for(MetaSet m : metas) {
            this.metaSets.put(m.getCode(), m);
        }
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
    public List<CardEdition> getSets() {
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
    public Predicate<PaperCard> getFilter() {
        if (this.filter == null) {
            this.filter = this.buildFilter();
        }
        return this.filter;
    }

    private Predicate<PaperCard> buildFilter() {
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
        if (this.metaSets.isEmpty() && this.sets.isEmpty()) {
            return this.name + " (empty)";
        } else if (this.metaSets.size() + this.getNumberSets() < 2) {
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

        private final CardEdition.Collection editions;
        /**
         * TODO: Write javadoc for Constructor.
         * @param pathname
         * @param editions0
         */
        public Reader(String pathname, CardEdition.Collection editions0) {
            super(pathname, CardBlock.FN_GET_NAME);
            editions = editions0;
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected CardBlock read(String line, int i) {
            final String[] sParts = TextUtil.splitWithParenthesis(line, ',', 3);
            String name = sParts[0];

            String[] numbers = sParts[1].trim().split("/");
            int draftBoosters = StringUtils.isNumeric(numbers[0]) ? Integer.parseInt(numbers[0]) : 0;
            int sealedBoosters = StringUtils.isNumeric(numbers[1]) ? Integer.parseInt(numbers[1]) : 0;
            CardEdition landSet = editions.getEditionByCodeOrThrow(numbers[2]);

            List<CardEdition> sets = new ArrayList<CardEdition>();
            List<MetaSet> metas = new ArrayList<MetaSet>();
            
            String[] setNames = TextUtil.splitWithParenthesis(sParts[2], ' ' );
            for(final String set : setNames ) {
                if(set.startsWith("Meta-")) {
                    String metaSpec = set.substring(5);
                    boolean noDraft = metaSpec.startsWith("NoDraft-");
                    if( noDraft ) metaSpec = metaSpec.substring(8);
                    metas.add(new MetaSet(metaSpec, !noDraft));
                } else {
                    sets.add(editions.getEditionByCodeOrThrow(set));
                }
            }

            return new CardBlock(i+1, name, sets, metas, landSet, draftBoosters, sealedBoosters);
        }

    }

    /**
     * The number of normal sets in the block.
     *
     * @return int, number of sets.
     */
    public int getNumberSets() {
    	return sets == null ? 0 : sets.size();
    }

    public Iterable<String> getMetaSetNames() {
        return metaSets.keySet();
    }

    public MetaSet getMetaSet(String key) {
       return metaSets.get(key);
    }

    /**
     * Tries to create a booster for the selected meta-set code.
     */
    public IUnOpenedProduct getBooster(final String code) {
        MetaSet ms = metaSets.get(code);
        return ms == null ? new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(code)) : ms.getBooster();
    }
}
