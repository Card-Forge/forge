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
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardEdition.CardInSet;
import forge.card.CardRarity;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.storage.StorageBase;
import forge.util.storage.StorageReaderFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.Map.Entry;


public class GameFormat implements Comparable<GameFormat> {
    private final String name;
    public enum FormatType {Sanctioned, Casual, Historic, Digital, Custom}
    public enum FormatSubType {Rotating, Eternal, Commander, Planechase, Block, Videogame, MTGO, Custom}

    // contains allowed sets, when empty allows all sets
    private FormatType formatType;
    private FormatSubType formatSubType;

    protected final List<String> allowedSetCodes; // this is mutable to support quest mode set unlocks
    protected final List<CardRarity> allowedRarities;
    protected final List<String> bannedCardNames;
    protected final List<String> restrictedCardNames;
    protected final List<String> additionalCardNames; // for cards that are legal but not reprinted in any of the allowed Sets
    
    protected final transient List<String> allowedSetCodes_ro;
    protected final transient List<String> bannedCardNames_ro;
    protected final transient List<String> restrictedCardNames_ro;
    protected final transient List<String> additionalCardNames_ro;

    protected final transient Predicate<PaperCard> filterRules;
    protected final transient Predicate<PaperCard> filterPrinted;

    private final int index;

    public GameFormat(final String fName, final Iterable<String> sets, final List<String> bannedCards) {
        this(fName, sets, bannedCards, null, null, null, 0, FormatType.Custom, FormatSubType.Custom);
    }
    
    public static final GameFormat NoFormat = new GameFormat("(none)", null, null, null, null, null, Integer.MAX_VALUE, FormatType.Custom, FormatSubType.Custom);
    
    public GameFormat(final String fName, final Iterable<String> sets, final List<String> bannedCards,
                      final List<String> restrictedCards, final List<String> additionalCards,
                      final List<CardRarity> rarities, int compareIdx, FormatType formatType, FormatSubType formatSubType) {
        this.index = compareIdx;
        this.formatType = formatType;
        this.formatSubType = formatSubType;
        this.name = fName;

        if(sets != null) {
            Set<String> parsedSets = new HashSet<>();
            for (String set : sets) {
                if (StaticData.instance().getEditions().get(set) == null) {
                    System.out.println("Set " + set + " in format " + fName + " does not match any valid editions!");
                    continue;
                }
                parsedSets.add(set);

            }
            allowedSetCodes = Lists.newArrayList(parsedSets);
        }else{
            allowedSetCodes = new ArrayList<String>();
        }

        bannedCardNames = bannedCards == null ? new ArrayList<String>() : Lists.newArrayList(bannedCards);
        restrictedCardNames = restrictedCards == null ? new ArrayList<String>() : Lists.newArrayList(restrictedCards);
        additionalCardNames = additionalCards == null ? new ArrayList<String>() : Lists.newArrayList(additionalCards);
        allowedRarities = rarities == null ? Lists.newArrayList() : rarities;

        this.allowedSetCodes_ro = Collections.unmodifiableList(allowedSetCodes);
        this.bannedCardNames_ro = Collections.unmodifiableList(bannedCardNames);
        this.restrictedCardNames_ro = Collections.unmodifiableList(restrictedCardNames);
        this.additionalCardNames_ro = Collections.unmodifiableList(additionalCardNames);

        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
    }
    private Predicate<PaperCard> buildFilter(boolean printed) {
        Predicate<PaperCard> p = Predicates.not(IPaperCard.Predicates.names(this.bannedCardNames_ro));
        if (!this.allowedSetCodes_ro.isEmpty()) {
            p = Predicates.and(p, printed ?
                    IPaperCard.Predicates.printedInSets(this.allowedSetCodes_ro, printed) :
                    StaticData.instance().getCommonCards().wasPrintedInSets(this.allowedSetCodes_ro));
        }
        if (!this.allowedRarities.isEmpty()) {
            List<Predicate<? super PaperCard>> crp = Lists.newArrayList();
            for (CardRarity cr: this.allowedRarities) {
                crp.add(StaticData.instance().getCommonCards().wasPrintedAtRarity(cr));
            }
            p = Predicates.and(p, Predicates.or(crp));
        }
        if (!this.additionalCardNames_ro.isEmpty() && !printed) {
            p = Predicates.or(p, IPaperCard.Predicates.names(this.additionalCardNames_ro));
        }
        return p;
    }

    private Predicate<PaperCard> buildFilterPrinted() {
        return buildFilter(true);
    }

    private Predicate<PaperCard> buildFilterRules() {
        return buildFilter(false);
    }

    public String getName() {
        return this.name;
    }

    public FormatType getFormatType() {
        return this.formatType;
    }

    public FormatSubType getFormatSubType() {
        return this.formatSubType;
    }

    public List<String> getAllowedSetCodes() {
        return this.allowedSetCodes_ro;
    }

    public List<String> getBannedCardNames() {
        return this.bannedCardNames_ro;
    }

    public List<String> getRestrictedCards() {
        return restrictedCardNames_ro;
    }

    public List<String> getAdditionalCards() {
        return additionalCardNames_ro;
    }

    public List<CardRarity> getAllowedRarities() {
        return allowedRarities;
    }

    public List<PaperCard> getAllCards() {
        List<PaperCard> cards = new ArrayList<PaperCard>();
        CardDb commonCards = StaticData.instance().getCommonCards();
        for (String setCode : allowedSetCodes_ro) {
            CardEdition edition = StaticData.instance().getEditions().get(setCode);
            if (edition != null) {
                for (CardInSet card : edition.getCards()) {
                    if (!bannedCardNames_ro.contains(card.name)) {
                        PaperCard pc = commonCards.getCard(card.name, setCode);
                        if (pc != null) {
                            cards.add(pc);
                        }
                    }
                }
            }
        }
        return cards;
    }

    public Predicate<PaperCard> getFilterRules() {
        return this.filterRules;
    }

    public Predicate<PaperCard> getFilterPrinted() {
        return this.filterPrinted;
    }

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

    @Override
    public int compareTo(GameFormat other) {
        if (null == other) {
            return 1;
        }
        if (other.formatType != formatType){
            return formatType.compareTo(other.formatType);
        }else{
            if (other.formatSubType != formatSubType){
                return formatSubType.compareTo(other.formatSubType);
            }
        }
        return name.compareTo(other.name);
        //return index - other.index;
    }

    public int getIndex() {
        return index;
    }

    public static class Reader extends StorageReaderFolder<GameFormat> {
        List<GameFormat> naturallyOrdered = new ArrayList<GameFormat>();
        
        public Reader(File file0) {
            super(file0, GameFormat.FN_GET_NAME);
        }

        @Override
        protected GameFormat read(File file) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));
            List<String> sets = null; // default: all sets allowed
            List<String> bannedCards = null; // default: nothing banned
            List<String> restrictedCards = null; // default: nothing restricted
            List<String> additionalCards = null; // default: nothing additional
            List<CardRarity> rarities = null;
            FileSection section = FileSection.parse(contents.get("format"), ":");
            String title = section.get("name");
            FormatType formatType;
            try {
                formatType = FormatType.valueOf(section.get("type"));
            } catch (Exception e) {
                formatType = FormatType.Custom;
            }
            FormatSubType formatsubType;
            try {
                formatsubType = FormatSubType.valueOf(section.get("subtype"));
            } catch (Exception e) {
                formatsubType = FormatSubType.Custom;
            }
            Integer idx = section.getInt("order");
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

            strCars = section.get("additional");
            if ( strCars != null ) {
                additionalCards = Arrays.asList(strCars.split("; "));
            }

            strCars = section.get("rarities");
            if ( strCars != null ) {
                CardRarity cr;
                rarities = Lists.newArrayList();
                for (String s: Arrays.asList(strCars.split(", "))) {
                    cr = CardRarity.smartValueOf(s);
                    if (cr.name() != "Unknown") {
                        rarities.add(cr);
                    }
                }
            }

            GameFormat result = new GameFormat(title, sets, bannedCards, restrictedCards, additionalCards, rarities, idx, formatType,formatsubType);
            naturallyOrdered.add(result);
            return result;
        }

        @Override
        protected FilenameFilter getFileFilter() {
            return TXT_FILE_FILTER;
        }

        public static final FilenameFilter TXT_FILE_FILTER = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".txt");
            }
        };
    }

    public static class Collection extends StorageBase<GameFormat> {
        private List<GameFormat> naturallyOrdered;
        
        public Collection(GameFormat.Reader reader) {
            super("Format collections", reader);
            naturallyOrdered = reader.naturallyOrdered;
            Collections.sort(naturallyOrdered);
        }

        public Iterable<GameFormat> getOrderedList() {
            return naturallyOrdered;
        }

        public Iterable<GameFormat> getSanctionedList() {
            List<GameFormat> coreList = new ArrayList<>();
            for(GameFormat format: naturallyOrdered){
                if(format.getFormatType().equals(FormatType.Sanctioned)){
                    coreList.add(format);
                }
            }
            return coreList;
        }

        public Iterable<GameFormat> getFilterList() {
            List<GameFormat> coreList = new ArrayList<>();
            for(GameFormat format: naturallyOrdered){
                if(!format.getFormatType().equals(FormatType.Historic)
                        &&!format.getFormatType().equals(FormatType.Digital)){
                    coreList.add(format);
                }
            }
            return coreList;
        }

        public Iterable<GameFormat> getHistoricList() {
            List<GameFormat> coreList = new ArrayList<>();
            for(GameFormat format: naturallyOrdered){
                if(format.getFormatType().equals(FormatType.Historic)){
                    coreList.add(format);
                }
            }
            return coreList;
        }

        public Map<String, List<GameFormat>> getHistoricMap() {
            Map<String, List<GameFormat>> coreList = new HashMap<>();
            for(GameFormat format: naturallyOrdered){
                if(format.getFormatType().equals(FormatType.Historic)){
                    String alpha = format.getName().substring(0,1);
                    if(!coreList.containsKey(alpha)){
                        coreList.put(alpha,new ArrayList<>());
                    }
                    coreList.get(alpha).add(format);
                }
            }
            return coreList;
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

        public Set<GameFormat> getAllFormatsOfCard(PaperCard card) {
            Set<GameFormat> result = new HashSet<GameFormat>();
            for (GameFormat gf : naturallyOrdered) {
                if (gf.getFilterRules().apply(card)) {
                    result.add(gf);
                }
            }
            if (result.isEmpty()) {
                result.add(NoFormat);
            }
            return result;
        }

        public Set<GameFormat> getAllFormatsOfDeck(Deck deck) {
            SortedSet<GameFormat> result = new TreeSet<GameFormat>();
            CardPool allCards = deck.getAllCardsInASinglePool();
            for(GameFormat gf : naturallyOrdered) {
                if (gf.isPoolLegal(allCards)) {
                    result.add(gf);
                }
            }
            if (result.isEmpty()) {
                result.add(NoFormat);
            }
            return result;
        }

        @Override
        public void add(GameFormat item) {
            naturallyOrdered.add(item);
        }
    }

    public final Predicate<CardEdition> editionLegalPredicate = new Predicate<CardEdition>() {
        @Override
        public boolean apply(final CardEdition subject) {
            return GameFormat.this.isSetLegal(subject.getCode());
        }
    };
}
