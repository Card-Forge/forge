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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.FileSection;
import forge.util.storage.StorageBase;
import forge.util.storage.StorageReaderFileSections;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


/**
 * TODO: Write javadoc for this type.
 * 
 */
public class GameFormat implements Comparable<GameFormat> {
    private final String name;
    // contains allowed sets, when empty allows all sets

    protected final List<String> allowedSetCodes; // this is mutable to support quest mode set unlocks 
    protected final List<String> bannedCardNames;
    protected final List<String> restrictedCardNames;
    
    protected final transient List<String> allowedSetCodes_ro;
    protected final transient List<String> bannedCardNames_ro;
    protected final transient List<String> restrictedCardNames_ro;

    protected final transient Predicate<PaperCard> filterRules;
    protected final transient Predicate<PaperCard> filterPrinted;

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
        this(fName, sets, bannedCards, null, 0);
    }
    
    public static final GameFormat NoFormat = new GameFormat("(none)", null, null, null, Integer.MAX_VALUE);
    
    public GameFormat(final String fName, final Iterable<String> sets, final List<String> bannedCards, final List<String> restrictedCards, int compareIdx) {
        this.index = compareIdx;
        this.name = fName;
        allowedSetCodes = sets == null ? new ArrayList<String>() : Lists.newArrayList(sets);
        bannedCardNames = bannedCards == null ? new ArrayList<String>() : Lists.newArrayList(bannedCards);
        restrictedCardNames = restrictedCards == null ? new ArrayList<String>() : Lists.newArrayList(restrictedCards);
        
        this.allowedSetCodes_ro = Collections.unmodifiableList(allowedSetCodes);
        this.bannedCardNames_ro = Collections.unmodifiableList(bannedCardNames);
        this.restrictedCardNames_ro = Collections.unmodifiableList(restrictedCardNames);

        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
    }

    private Predicate<PaperCard> buildFilterPrinted() {
        final Predicate<PaperCard> banNames = Predicates.not(IPaperCard.Predicates.names(this.bannedCardNames_ro));
        if (this.allowedSetCodes_ro.isEmpty()) {
            return banNames;
        }
        return Predicates.and(banNames, IPaperCard.Predicates.printedInSets(this.allowedSetCodes_ro, true));
    }

    private Predicate<PaperCard> buildFilterRules() {
        final Predicate<PaperCard> banNames = Predicates.not(IPaperCard.Predicates.names(this.bannedCardNames_ro));
        if (this.allowedSetCodes_ro.isEmpty()) {
            return banNames;
        }
        return Predicates.and(banNames, StaticData.instance().getCommonCards().wasPrintedInSets(this.allowedSetCodes_ro));
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

    public List<String> getRestrictedCards() {
        // TODO Auto-generated method stub
        return restrictedCardNames_ro;
    }

    /**
     * Gets the filter rules.
     * 
     * @return the filter rules
     */
    public Predicate<PaperCard> getFilterRules() {
        return this.filterRules;
    }

    /**
     * Gets the filter printed.
     * 
     * @return the filter printed
     */
    public Predicate<PaperCard> getFilterPrinted() {
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
        return this.allowedSetCodes_ro.isEmpty() || this.allowedSetCodes_ro.contains(setCode);
    }
    
    private boolean isPoolLegal(final CardPool allCards) {
        for (Entry<PaperCard, Integer> poolEntry : allCards) {
            if (!filterRules.apply(poolEntry.getKey())) {
                return false; //all cards in deck must pass card predicate to pass deck predicate
            }
        }
        
        if(!restrictedCardNames_ro.isEmpty() ) {
            for (Entry<PaperCard, Integer> poolEntry : allCards) {
                if( poolEntry.getValue().intValue() > 1 && restrictedCardNames_ro.contains(poolEntry.getKey().getName()))
                    return false;
            }
        }
        return true;
    }
    
    public boolean isDeckLegal(final Deck deck) {
        return isPoolLegal(deck.getAllCardsInASinglePool());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name;
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

    /**
     * Instantiates a new format utils.
     */
    public static class Reader extends StorageReaderFileSections<GameFormat> {
        List<GameFormat> naturallyOrdered = new ArrayList<GameFormat>();
        
        public Reader(File file0) {
            super(file0, GameFormat.FN_GET_NAME);
        }

        @Override
        protected GameFormat read(String title, Iterable<String> body, int idx) {
            List<String> sets = null; // default: all sets allowed
            List<String> bannedCards = null; // default: nothing banned
            List<String> restrictedCards = null; // default: nothing restricted

            FileSection section = FileSection.parse(body, ":");
            String strSets = section.get("sets");
            if ( null != strSets ) {
                sets = Arrays.asList(strSets.split(", "));
            }
            String strCars = section.get("banned");
            if ( strCars != null ) {
                bannedCards = Arrays.asList(strCars.split("; "));
            }
            
            strCars = section.get("restricted");
            if ( strCars != null ) {
                restrictedCards = Arrays.asList(strCars.split("; "));
            }

            GameFormat result = new GameFormat(title, sets, bannedCards, restrictedCards, 1 + idx); 
            naturallyOrdered.add(result);
            return result;
        }
    }

    public static class Collection extends StorageBase<GameFormat> {
        private List<GameFormat> naturallyOrdered;
        
        public Collection(GameFormat.Reader reader) {
            super("Format collections", reader);
            naturallyOrdered = reader.naturallyOrdered;
        }

        public Iterable<GameFormat> getOrderedList() {
            return naturallyOrdered;
        }

        public GameFormat getStandard() {
            return this.map.get("Standard");
        }

        public GameFormat getExtended() {
            return this.map.get("Extended");
        }

        public GameFormat getModern() {
            return this.map.get("Modern");
        }

        public GameFormat getFormat(String format) {
            return this.map.get(format);
        }

        public GameFormat getFormatOfDeck(Deck deck) {
            for(GameFormat gf : naturallyOrdered) {
                if ( gf.isDeckLegal(deck) )
                    return gf;
            }
            return NoFormat;
        }
        
        public Iterable<GameFormat> getAllFormatsOfDeck(Deck deck) {
            List<GameFormat> result = new ArrayList<GameFormat>();
            CardPool allCards = deck.getAllCardsInASinglePool();
            for(GameFormat gf : naturallyOrdered) {
                if (gf.isPoolLegal(allCards))
                    result.add(gf);
            }
            if( result.isEmpty())
                result.add(NoFormat);
            return result;
        }
    }
    
    // declared here because
    public final Predicate<CardEdition> editionLegalPredicate = new Predicate<CardEdition>() {
        @Override
        public boolean apply(final CardEdition subject) {
            return GameFormat.this.isSetLegal(subject.getCode());
        }
    };
}
