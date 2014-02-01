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
package forge.deck;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.StaticData;

import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.CardDb.SetPreference;
import forge.deck.io.DeckFileHeader;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.item.IPaperCard;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.ItemPool;
import forge.util.ItemPoolSorter;



/**
 * <p>
 * Deck class.
 * </p>
 * 
 * The set of MTG legal cards that become player's library when the game starts.
 * Any other data is not part of a deck and should be stored elsewhere. Current
 * fields allowed for deck metadata are Name, Title, Description and Deck Type.
 */
@SuppressWarnings("serial")
public class Deck extends DeckBase implements Iterable<Entry<DeckSection, CardPool>> {
    private final Map<DeckSection, CardPool> parts = new EnumMap<DeckSection, CardPool>(DeckSection.class);
    private final Set<String>                tags  = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    // gameType is from Constant.GameType, like GameType.Regular
    /**
     * <p>
     * Decks have their named finalled.
     * </p>
     */
    public Deck() {
        this("");
    }

    /**
     * Instantiates a new deck.
     *
     * @param name0 the name0
     */
    public Deck(final String name0) {
        super(name0);
        getOrCreate(DeckSection.Main);
    }

    @Override
    public String getItemType() {
        return "Deck";
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    public CardPool getMain() {
        return this.parts.get(DeckSection.Main);
    }

    // may return nulls
    public CardPool get(DeckSection deckSection) {
        return this.parts.get(deckSection);
    }

    public boolean has(DeckSection deckSection) {
        final CardPool cp = get(deckSection);
        return cp != null && !cp.isEmpty();
    }

    // will return new if it was absent
    public CardPool getOrCreate(DeckSection deckSection) {
        CardPool p = get(deckSection);
        if (p != null) {
            return p;
        }
        p = new CardPool();
        this.parts.put(deckSection, p);
        return p;
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#cloneFieldsTo(forge.deck.DeckBase)
     */
    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);
        final Deck result = (Deck) clone;
        for (Entry<DeckSection, CardPool> kv : parts.entrySet()) {
            CardPool cp = new CardPool();
            result.parts.put(kv.getKey(), cp);
            cp.addAll(kv.getValue());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(final String name0) {
        return new Deck(name0);
    }

    /**
     * From file.
     *
     * @param deckFile the deck file
     * @return the deck
     */
    public static Deck fromFile(final File deckFile) {
        return Deck.fromSections(FileSection.parseSections(FileUtil.readFile(deckFile)));
    }

    /**
     * From sections.
     *
     * @param sections the sections
     * @return the deck
     */
    public static Deck fromSections(final Map<String, List<String>> sections) {
        return Deck.fromSections(sections, false);
    }

    /**
     * From sections.
     *
     * @param sections the sections
     * @param canThrowExtendedErrors the can throw extended errors
     * @return the deck
     */
    public static Deck fromSections(final Map<String, List<String>> sections, final boolean canThrowExtendedErrors) {
        if (sections == null || sections.isEmpty()) {
            return null;
        }

        final DeckFileHeader dh = DeckSerializer.readDeckMetadata(sections, canThrowExtendedErrors);
        if (dh == null) {
            return null;
        }

        final Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());
        d.tags.addAll(dh.getTags());

        boolean hasExplicitlySpecifiedSet = false;
        
        for (Entry<String, List<String>> s : sections.entrySet()) {
            DeckSection sec = DeckSection.smartValueOf(s.getKey());
            if (sec == null) {
                continue;
            }
            
            for(String k : s.getValue()) 
                if ( k.indexOf(CardDb.NameSetSeparator) > 0 )
                    hasExplicitlySpecifiedSet = true;

            CardPool pool = CardPool.fromCardList(s.getValue());
            // I used to store planes and schemes under sideboard header, so this will assign them to a correct section
            IPaperCard sample = pool.get(0);
            if (sample != null && ( sample.getRules().getType().isPlane() || sample.getRules().getType().isPhenomenon())) {
                sec = DeckSection.Planes;
            }
            if (sample != null && sample.getRules().getType().isScheme()) {
                sec = DeckSection.Schemes;
            }

            d.parts.put(sec, pool);
        }
        
        if (!hasExplicitlySpecifiedSet) {
            d.convertByXitaxMethod();
        }
            
        return d;
    }

    public void convertByXitaxMethod() {
        CardEdition earliestSet = StaticData.instance().getEditions().getEarliestEditionWithAllCards(getAllCardsInASinglePool());

        Calendar cal = Calendar.getInstance();
        cal.setTime(earliestSet.getDate());
        cal.add(Calendar.DATE, 1);
        Date dayAfterNewestSetRelease = cal.getTime();
        
        for(Entry<DeckSection, CardPool> p : parts.entrySet()) {
            if( p.getKey() == DeckSection.Planes || p.getKey() == DeckSection.Schemes || p.getKey() == DeckSection.Avatar)
                continue;
            
            CardPool newPool = new CardPool();
            
            for(Entry<PaperCard, Integer> cp : p.getValue()){
                String cardName = cp.getKey().getName();
                int artIndex = cp.getKey().getArtIndex();
                
                PaperCard c = StaticData.instance().getCommonCards().getCardFromEdition(cardName, dayAfterNewestSetRelease, SetPreference.LatestCoreExp, artIndex);
                if( null == c ) {
                    c = StaticData.instance().getCommonCards().getCardFromEdition(cardName, dayAfterNewestSetRelease, SetPreference.LatestCoreExp, -1);
                    if( c == null)
                        c = StaticData.instance().getCommonCards().getCardFromEdition(cardName, dayAfterNewestSetRelease, SetPreference.Latest, -1);
                    // this is to randomize art of all those cards
                    if( c == null ) // I give up!
                        c = cp.getKey();
                    newPool.add(cardName, c.getEdition(), cp.getValue());
                } else
                    newPool.add(c, cp.getValue());
            }
            parts.put(p.getKey(), newPool);
        }
        

    }

    private static List<String> writeCardPool(final ItemPool<PaperCard> pool) {
        List<Entry<PaperCard, Integer>> main2sort = Lists.newArrayList(pool);
        Collections.sort(main2sort, ItemPoolSorter.BY_NAME_THEN_SET);
        final List<String> out = new ArrayList<String>();
        for (final Entry<PaperCard, Integer> e : main2sort) {
            out.add(serializeSingleCard(e.getKey(), e.getValue()));
        }
        return out;
    }

    private static String serializeSingleCard(PaperCard card, Integer n) {
        final boolean hasBadSetInfo = "???".equals(card.getEdition()) || StringUtils.isBlank(card.getEdition());
        StringBuilder sb = new StringBuilder();
        sb.append(n).append(" ").append(card.getName());

        if (!hasBadSetInfo) {
            int artCount = StaticData.instance().getCommonCards().getArtCount(card.getName(), card.getEdition());

            sb.append("|").append(card.getEdition());

            if (artCount > 1) {
                sb.append("|").append(card.getArtIndex()); // indexes start at 1 to match image file name conventions
            }
        }
        if(card.isFoil()) {
            sb.append(CardDb.foilSuffix);
        }
        return sb.toString();
    }

    /**
     * <p>
     * writeDeck.
     * </p>
     *
     * @return the list
     */
    public List<String> save() {
        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));

        out.add(String.format("%s=%s", DeckFileHeader.NAME, this.getName().replaceAll("\n", "")));
        // these are optional
        if (this.getComment() != null) {
            out.add(String.format("%s=%s", DeckFileHeader.COMMENT, this.getComment().replaceAll("\n", "")));
        }
        if (!this.getTags().isEmpty()) {
            out.add(String.format("%s=%s", DeckFileHeader.TAGS, StringUtils.join(getTags(), DeckFileHeader.TAGS_SEPARATOR)));
        }

        for(Entry<DeckSection, CardPool> s : parts.entrySet()) {
            out.add(String.format("[%s]", s.getKey().toString()));
            out.addAll(Deck.writeCardPool(s.getValue()));
        }
        return out;
    }

    public static final Function<Deck, String> FN_NAME_SELECTOR = new Function<Deck, String>() {
        @Override
        public String apply(Deck arg1) {
            return arg1.getName();
        }
    };

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Entry<DeckSection, CardPool>> iterator() {
        return parts.entrySet().iterator();
    }

    /**
     * @return the associated tags, a writable set
     */
    public Set<String> getTags() {
        return tags;
    }

    public ColorSet getColor() {
         byte colorProfile = MagicColor.COLORLESS;

        for (Entry<DeckSection, CardPool> deckEntry : this) {
            switch (deckEntry.getKey()) {
            case Main:
            case Commander:
                for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                    colorProfile |= poolEntry.getKey().getRules().getColor().getColor();
                }
                break;
            default:
                break; //ignore other sections
            }
        }
        return ColorSet.fromMask(colorProfile);
    }
    
    public CardPool getAllCardsInASinglePool() {
        CardPool allCards = new CardPool(); // will count cards in this pool to enforce restricted
        allCards.addAll(this.getMain());
        if (this.has(DeckSection.Sideboard))
            allCards.addAll(this.get(DeckSection.Sideboard));
        if (this.has(DeckSection.Commander))
            allCards.addAll(this.get(DeckSection.Commander));
        // do not include schemes / avatars and any non-regular cards
        return allCards;
    }    
}
