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
package forge.item;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.Card;
import forge.card.CardInSet;
import forge.card.CardRules;
import forge.card.MtgDataParser;
import forge.util.Aggregates;


/**
 * <p>
 * CardDb class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardDb.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardDb {
    private static volatile CardDb onlyInstance = null; // 'volatile' keyword
                                                        // makes this working
    private final String foilSuffix = " foil";

    /**
     * Instance.
     * 
     * @return the card db
     */
    public static CardDb instance() {
        if (CardDb.onlyInstance == null) {
            throw new NullPointerException("CardDb has not yet been initialized, run setup() first");
        }
        return CardDb.onlyInstance;
    }

    /**
     * Sets the up.
     * 
     * @param list
     *            the new up
     */
    public static void setup(final Iterator<CardRules> list) {
        if (CardDb.onlyInstance != null) {
            throw new RuntimeException("CardDb has already been initialized, don't do it twice please");
        }
        synchronized (CardDb.class) {
            if (CardDb.onlyInstance == null) { // It's broken under 1.4 and
                                               // below, on
                // 1.5+ works again!
                CardDb.onlyInstance = new CardDb(list);
            }
        }
    }

    // Here oracle cards
    // private final Map<String, CardRules> cards = new Hashtable<String,
    // CardRules>();

    // Here are refs, get them by name
    private final Map<String, CardPrinted> uniqueCards = new Hashtable<String, CardPrinted>();

    // need this to obtain cardReference by name+set+artindex
    private final Map<String, Map<String, CardPrinted[]>> allCardsBySet = new Hashtable<String, Map<String, CardPrinted[]>>();
    // this is the same list in flat storage
    private final List<CardPrinted> allTraditionalCardsFlat = new ArrayList<CardPrinted>();

    private final List<CardPrinted> allNonTraditionalCardsFlat = new ArrayList<CardPrinted>();

    // Lambda to get rules for selects from list of printed cards
    /** The Constant fnGetCardPrintedByForgeCard. */
    public static final Function<Card, CardPrinted> FN_GET_CARD_PRINTED_BY_FORGE_CARD = new Function<Card, CardPrinted>() {
        @Override
        public CardPrinted apply(final Card from) {
            return CardDb.instance().getCard(from.getName());
        }
    };

    private CardDb() {
        this(new MtgDataParser()); // I wish cardname.txt parser was be here.
    }

    private CardDb(final Iterator<CardRules> parser) {
        while (parser.hasNext()) {
            this.addNewCard(parser.next());
        }
        // TODO consider using Collections.unmodifiableList wherever possible
    }

    /**
     * Adds the new card.
     * 
     * @param card
     *            the card
     */
    public void addNewCard(final CardRules card) {
        if (null == card) {
            return;
        } // consider that a success
          // System.out.println(card.getName());
        final String cardName = card.getName().toLowerCase();

        // 1. register among oracle uniques
        // cards.put(cardName, card);

        // 2. Save refs into two lists: one flat and other keyed with sets &
        // name
        CardPrinted lastAdded = null;
        for (final Entry<String, CardInSet> s : card.getSetsPrinted()) {
            lastAdded = this.addToLists(card, cardName, s);
        }
        this.uniqueCards.put(cardName, lastAdded);
    }

    /**
     * Adds the to lists.
     * 
     * @param card
     *            the card
     * @param cardName
     *            the card name
     * @param s
     *            the s
     * @return the card printed
     */
    public CardPrinted addToLists(final CardRules card, final String cardName, final Entry<String, CardInSet> s) {
        CardPrinted lastAdded = null;
        final String set = s.getKey();

        // get this set storage, if not found, create it!
        Map<String, CardPrinted[]> setMap = this.allCardsBySet.get(set);
        if (null == setMap) {
            setMap = new Hashtable<String, CardPrinted[]>();
            this.allCardsBySet.put(set, setMap);
        }

        final int count = s.getValue().getCopiesCount();
        final CardPrinted[] cardCopies = new CardPrinted[count];
        setMap.put(cardName, cardCopies);
        for (int i = 0; i < count; i++) {
            lastAdded = CardPrinted.build(card, set, s.getValue().getRarity(), i);
            if (lastAdded.isTraditional()) {
                this.allTraditionalCardsFlat.add(lastAdded);
            } else {
                this.allNonTraditionalCardsFlat.add(lastAdded);
            }
            cardCopies[i] = lastAdded;
        }

        return lastAdded;
    }

    /**
     * Splits cardname into Name and set whenever deck line reads as name|set.
     */
    private static ImmutablePair<String, String> splitCardName(final String name) {
        String cardName = name; // .trim() ?
        final int pipePos = cardName.indexOf('|');

        if (pipePos >= 0) {
            final String setName = cardName.substring(pipePos + 1).trim();
            cardName = cardName.substring(0, pipePos);
            // only if set is not blank try to load it
            if (StringUtils.isNotBlank(setName) && !"???".equals(setName)) {
                return new ImmutablePair<String, String>(cardName, setName);
            }
        }
        return new ImmutablePair<String, String>(cardName, null);
    }

    private boolean isFoil(final String cardName) {
        return cardName.toLowerCase().endsWith(this.foilSuffix) && (cardName.length() > 5);
    }

    /**
     * Removes the foil suffix.
     *
     * @param cardName the card name
     * @return the string
     */
    public String removeFoilSuffix(final String cardName) {
        return cardName.substring(0, cardName.length() - 5);
    }

    /**
     * Checks if is card supported.
     * 
     * @param cardName0
     *            the card name
     * @return true, if is card supported
     */
    public boolean isCardSupported(final String cardName0) {
        if (null == cardName0) {
            return false;  // obviously
        }

        final boolean isFoil = this.isFoil(cardName0);
        final String cardName = isFoil ? this.removeFoilSuffix(cardName0) : cardName0;
        final ImmutablePair<String, String> nameWithSet = CardDb.splitCardName(cardName);
        if (nameWithSet.right == null) {
            return this.uniqueCards.containsKey(nameWithSet.left.toLowerCase());
        }
        return isCardSupported(nameWithSet.left, nameWithSet.right);
    }

    public boolean isCardSupported(final String cardName, String setName) {
        // Set exists?
        final Map<String, CardPrinted[]> cardsFromset = this.allCardsBySet.get(setName.toUpperCase());
        if (cardsFromset == null) {
            return false;
        }
        // Card exists?
        final CardPrinted[] cardCopies = cardsFromset.get(cardName.toLowerCase());
        return (cardCopies != null) && (cardCopies.length > 0);
    }

    // Single fetch
    /**
     * Gets the card.
     * 
     * @param name
     *            the name
     * @return the card
     */
    public CardPrinted getCard(final String name) {
        return this.getCard(name, false);
    }

    // Advanced fetch by name+set
    /**
     * Gets the card.
     * 
     * @param name
     *            the name
     * @param set
     *            the set
     * @return the card
     */
    public CardPrinted getCard(final String name, final String set) {
        return this.getCard(name, set, 0);
    }

    /**
     * Gets the card.
     * 
     * @param name
     *            the name
     * @param set
     *            the set
     * @param artIndex
     *            the art index
     * @return the card
     */
    public CardPrinted getCard(final String name, final String set, final int artIndex) {
        // 1. get set
        final Map<String, CardPrinted[]> cardsFromset = this.allCardsBySet.get(set.toUpperCase());
        if (null == cardsFromset) {
            final String err = String
                    .format("Asked for card '%s' from set '%s': that set was not found. :(", name, set);
            throw new NoSuchElementException(err);
        }
        // 2. Find the card itself
        final CardPrinted[] cardCopies = cardsFromset.get(name.toLowerCase());
        if (null == cardCopies) {
            final String err = String.format("Asked for card '%s' from '%s': set found, but the card wasn't. :(", name,
                    set);
            throw new NoSuchElementException(err);
        }
        // 3. Get the proper copy
        if ((artIndex >= 0) && (artIndex <= cardCopies.length)) {
            return cardCopies[artIndex];
        }
        final String err = String
                .format("Asked for '%s' from '%s' #%d: db didn't find that copy.", name, set, artIndex);
        throw new NoSuchElementException(err);
    }

    // Fetch from Forge's Card instance. Well, there should be no errors, but
    // we'll still check
    /**
     * Gets the card.
     * 
     * @param forgeCard
     *            the forge card
     * @return the card
     */
    public CardPrinted getCard(final Card forgeCard) {
        final String name = forgeCard.getName();
        final String set = forgeCard.getCurSetCode();
        if (StringUtils.isNotBlank(set)) {
            return this.getCard(name, set);
        }
        return this.getCard(name);
    }

    // Multiple fetch
    /**
     * Gets the cards.
     * 
     * @param names
     *            the names
     * @return the cards
     */
    public List<CardPrinted> getCards(final Iterable<String> names) {
        final List<CardPrinted> result = new ArrayList<CardPrinted>();
        for (final String name : names) {
            result.add(this.getCard(name));
        }
        return result;
    }

    /**
     * Gets the cards from latest sets.
     *
     * @param names the names
     * @return the cards from latest sets
     */
    public List<CardPrinted> getCardsFromLatestSets(final Iterable<String> names) {
        final List<CardPrinted> result = new ArrayList<CardPrinted>();
        for (final String name : names) {
            result.add(this.getCard(name, true));
        }
        return result;
    }

    // returns a list of all cards from their respective latest editions
    /**
     * Gets the all unique cards.
     * 
     * @return the all unique cards
     */
    public Iterable<CardPrinted> getAllUniqueCards() {
        return this.uniqueCards.values();
    }

    // public Iterable<CardRules> getAllCardRules() { return cards.values(); }
    // // still not needed
    /**
     * Gets the all cards.
     * 
     * @return the all cards
     */
    public Iterable<CardPrinted> getAllTraditionalCards() {
        return this.allTraditionalCardsFlat;
    }

    public Iterable<CardPrinted> getAllNonTraditionalCards() {
        return this.allNonTraditionalCardsFlat;
    }

    /**
     * Gets the card.
     *
     * @param name0 the name0
     * @param fromLatestSet the from latest set
     * @return the card
     */
    public CardPrinted getCard(final String name0, final boolean fromLatestSet) {
        // Sometimes they read from decks things like "CardName|Set" - but we
        // can handle it

        final boolean isFoil = this.isFoil(name0);
        final String name = isFoil ? this.removeFoilSuffix(name0) : name0;
        CardPrinted result = null;

        final ImmutablePair<String, String> nameWithSet = CardDb.splitCardName(name);
        if (nameWithSet.right != null) {
            result = this.getCard(nameWithSet.left, nameWithSet.right);
        } else {
            if (!fromLatestSet) {
                result = this.uniqueCards.get(nameWithSet.left.toLowerCase());
                if (null == result) {
                    throw new NoSuchElementException(String.format("Card '%s' not found in our database.", name));
                }
            } else {
                // OK, plain name here
                final Predicate<CardPrinted> predicate = CardPrinted.Predicates.name(nameWithSet.left);
                final Iterable<CardPrinted> namedCards = Iterables.filter(this.allTraditionalCardsFlat, predicate);
                // Find card with maximal set index
                result = Aggregates.itemWithMax(namedCards, CardPrinted.FN_GET_EDITION_INDEX);
                if (null == result) {
                    throw new NoSuchElementException(String.format("Card '%s' not found in our database.", name));
                }

            }
        }
        if (isFoil) {
            result = CardPrinted.makeFoiled(result);
        }
        return result;
    }

}
