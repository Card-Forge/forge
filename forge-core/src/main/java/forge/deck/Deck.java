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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardDb;
import forge.item.IPaperCard;
import forge.item.PaperCard;

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
    private final Map<DeckSection, CardPool> parts = new EnumMap<>(DeckSection.class);
    private final Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    // Supports deferring loading a deck until we actually need its contents. This works in conjunction with
    // the lazy card load feature to ensure we don't need to load all cards on start up.
    private Map<String, List<String>> deferredSections;

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
        other.cloneFieldsTo(this);
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
        loadDeferredSections();
        return parts.get(DeckSection.Main);
    }

    public List<PaperCard> getCommanders() {
        List<PaperCard> result = Lists.newArrayList();
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return result;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            result.add(c.getKey());
        }
        if (result.size() > 1) { //sort by type so signature spell comes after oathbreaker
            Collections.sort(result, new Comparator<PaperCard>() {
                @Override
                public int compare(final PaperCard c1, final PaperCard c2) {
                    return Boolean.compare(c1.getRules().canBeSignatureSpell(), c2.getRules().canBeSignatureSpell());
                }
            });
        }
        return result;
    }

    //at least for now, Oathbreaker will only support one oathbreaker and one signature spell
    public PaperCard getOathbreaker() {
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return null;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            PaperCard card = c.getKey();
            if (card.getRules().canBeOathbreaker()) {
                return card;
            }
        }
        return null;
    }
    public PaperCard getSignatureSpell() {
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return null;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            PaperCard card = c.getKey();
            if (card.getRules().canBeSignatureSpell()) {
                return card;
            }
        }
        return null;
    }

    // may return nulls
    public CardPool get(DeckSection deckSection) {
        loadDeferredSections();
        return parts.get(deckSection);
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

    public void setDeferredSections(Map<String, List<String>> deferredSections) {
        this.deferredSections = deferredSections;
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#cloneFieldsTo(forge.deck.DeckBase)
     */
    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);
        final Deck result = (Deck) clone;
        loadDeferredSections();
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

    private void loadDeferredSections() {
        if (deferredSections == null) {
            return;
        }

        boolean hasExplicitlySpecifiedSet = false;
        for (Entry<String, List<String>> s : deferredSections.entrySet()) {
            DeckSection sec = DeckSection.smartValueOf(s.getKey());
            if (sec == null) {
                continue;
            }

            final List<String> cardsInSection = s.getValue();
            for (String k : cardsInSection) 
                if (k.indexOf(CardDb.NameSetSeparator) > 0)
                    hasExplicitlySpecifiedSet = true;

            CardPool pool = CardPool.fromCardList(cardsInSection);
            // I used to store planes and schemes under sideboard header, so this will assign them to a correct section
            IPaperCard sample = pool.get(0);
            if (sample != null && (sample.getRules().getType().isPlane() || sample.getRules().getType().isPhenomenon())) {
                sec = DeckSection.Planes;
            }
            if (sample != null && sample.getRules().getType().isScheme()) {
                sec = DeckSection.Schemes;
            }
            putSection(sec, pool);
        }

        deferredSections = null;
        if (!hasExplicitlySpecifiedSet) {
            convertByXitaxMethod();
        }
    }

    private void convertByXitaxMethod() {
        Date dateWithAllCards = StaticData.instance().getEditions().getEarliestDateWithAllCards(getAllCardsInASinglePool());
        String artOption = StaticData.instance().getPrefferedArtOption();

        for (Entry<DeckSection, CardPool> p : parts.entrySet()) {
            if (p.getKey() == DeckSection.Planes || p.getKey() == DeckSection.Schemes || p.getKey() == DeckSection.Avatar)
                continue;

            CardPool newPool = new CardPool();

            for (Entry<PaperCard, Integer> cp : p.getValue()) {
                PaperCard card = cp.getKey();
                int count = cp.getValue();

                PaperCard replacementCard;
                switch (artOption) {
                    case "Latest":
                        replacementCard = StaticData.instance().getCardFromLatestorEarliest(card);
                        break;
                    case "Earliest":
                        replacementCard = StaticData.instance().getCardFromEarliestCoreExp(card);
                        break;
                    default:
                        replacementCard = StaticData.instance().getCardByEditionDate(card, dateWithAllCards);
                }

                if (replacementCard.getArtIndex() == card.getArtIndex()) {
                    if (card.hasImage())
                        newPool.add(card, count);
                    else
                        newPool.add(replacementCard, count);
                } else {
                    if (card.hasImage())
                        newPool.add(card.getName(), card.getEdition(), count); // this is to randomize art
                    else
                        newPool.add(replacementCard, count);
                }
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
        loadDeferredSections();
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
        loadDeferredSections();
        for (CardPool part : parts.values()) {
            if (!part.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }

    @Override
    public Deck getHumanDeck() {
        return this;
    }
}