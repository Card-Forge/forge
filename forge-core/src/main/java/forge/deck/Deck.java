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

import com.google.common.base.Function;
import forge.StaticData;
import forge.card.CardDb.SetPreference;
import forge.card.CardEdition;
import forge.item.PaperCard;

import java.util.*;
import java.util.Map.Entry;

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

    /**
     * Copy constructor.
     * 
     * @param other
     *            the {@link Deck} to copy.
     */
    public Deck(final Deck other) {
        this(other, other.getName());
    }

    /**
     * Copy constructor with a different name for the new deck.
     * 
     * @param other
     *            the {@link Deck} to copy.
     * @param newName
     *            the name of the new deck.
     */
    public Deck(final Deck other, final String newName) {
        super(newName);
        setComment(other.getComment());
        for (final Entry<DeckSection, CardPool> sections : other.parts.entrySet()) {
            parts.put(sections.getKey(), new CardPool(sections.getValue()));
        }
        tags.addAll(other.getTags());
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
    
    public void putSection(DeckSection section, CardPool pool) {
        this.parts.put(section, pool);
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
                    
                    if( c != null ) { 
                        newPool.add(cardName, c.getEdition(), cp.getValue()); // this is to randomize art of all those cards
                    } else // I give up!
                        newPool.add(cp.getKey(), cp.getValue());
                } else
                    newPool.add(c, cp.getValue());
            }
            parts.put(p.getKey(), newPool);
        }
        

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

    public CardPool getAllCardsInASinglePool() {
        return getAllCardsInASinglePool(true);
    }
    public CardPool getAllCardsInASinglePool(final boolean includeCommander) {
        final CardPool allCards = new CardPool(); // will count cards in this pool to enforce restricted
        allCards.addAll(this.getMain());
        if (this.has(DeckSection.Sideboard)) {
            allCards.addAll(this.get(DeckSection.Sideboard));
        }
        if (includeCommander && this.has(DeckSection.Commander)) {
            allCards.addAll(this.get(DeckSection.Commander));
        }
        // do not include schemes / avatars and any non-regular cards
        return allCards;
    }

    @Override
    public boolean isEmpty() {
        for (CardPool part : parts.values()) {
            if (!part.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
