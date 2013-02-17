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
package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import forge.card.CardRulesPredicates;
import forge.item.CardPrinted;


/**
 * TODO: Write javadoc for this type.
 * 
 */
public class GameFormat implements Comparable<GameFormat> {

    private final String name;
    // contains allowed sets, when empty allows all sets
    protected final List<String> allowedSetCodes;
    protected final List<String> bannedCardNames;

    protected final transient List<String> allowedSetCodes_ro;
    protected final transient List<String> bannedCardNames_ro;

    protected final transient Predicate<CardPrinted> filterRules;
    protected final transient Predicate<CardPrinted> filterPrinted;

    private final int index; 
    
    /**
     * Instantiates a new game format.
     * 
     * @param fName
     *            the f name
     * @param sets
     *            the sets
     * @param bannedCards
     *            the banned cards
     */
    public GameFormat(final String fName, final Iterable<String> sets, final List<String> bannedCards) {
        this(fName, sets, bannedCards, 0);
    }
    
    public GameFormat(final String fName, final Iterable<String> sets, final List<String> bannedCards, int compareIdx) {
        this.index = compareIdx;
        this.name = fName;
        this.allowedSetCodes = Lists.newArrayList(sets);
        this.bannedCardNames = bannedCards == null ? new ArrayList<String>() : Lists.newArrayList(bannedCards);

        this.allowedSetCodes_ro = Collections.unmodifiableList(allowedSetCodes);
        this.bannedCardNames_ro = Collections.unmodifiableList(bannedCardNames);

        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
    }

    private Predicate<CardPrinted> buildFilterPrinted() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        if (this.allowedSetCodes == null || this.allowedSetCodes.isEmpty()) {
            return banNames;
        }
        return Predicates.and(banNames, CardPrinted.Predicates.printedInSets(this.allowedSetCodes, true));
    }

    private Predicate<CardPrinted> buildFilterRules() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        if (this.allowedSetCodes == null || this.allowedSetCodes.isEmpty()) {
            return banNames;
        }
        return Predicates.and(banNames, Predicates.compose(CardRulesPredicates.wasPrintedInSets(this.allowedSetCodes), CardPrinted.FN_GET_RULES));
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
     * Gets the set list (for GameFormatQuest).
     * 
     * @return list of allowed set codes
     */
    public List<String> getAllowedSetCodes() {
        return this.allowedSetCodes_ro;
    }

    /**
     * Gets the banned cards (for GameFormatQuest).
     * 
     * @return list of banned card names
     */
    public List<String> getBannedCardNames() {
        return this.bannedCardNames_ro;
    }

    /**
     * Gets the filter rules.
     * 
     * @return the filter rules
     */
    public Predicate<CardPrinted> getFilterRules() {
        return this.filterRules;
    }

    /**
     * Gets the filter printed.
     * 
     * @return the filter printed
     */
    public Predicate<CardPrinted> getFilterPrinted() {
        return this.filterPrinted;
    }

    /**
     * Checks if is sets the legal.
     * 
     * @param setCode
     *            the set code
     * @return true, if is sets the legal
     */
    public boolean isSetLegal(final String setCode) {
        return this.allowedSetCodes.isEmpty() || this.allowedSetCodes.contains(setCode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " (format)";
    }

    public static final Function<GameFormat, String> FN_GET_NAME = new Function<GameFormat, String>() {
        @Override
        public String apply(GameFormat arg1) {
            return arg1.getName();
        }
    };

    /* (non-Javadoc)
     * just used for ordering -- comparing the name is sufficient
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(GameFormat other) {
        if (null == other) {
            return 1;
        }
        return index - other.index;
    }

    public int getIndex() {
        return index;
    }

}
