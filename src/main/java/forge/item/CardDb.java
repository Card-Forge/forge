package forge.item;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import net.slightlymagic.braids.util.lambda.Lambda1;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.Card;
import forge.card.CardInSet;
import forge.card.CardRules;
import forge.card.MtgDataParser;

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
    private final List<CardPrinted> allCardsFlat = new ArrayList<CardPrinted>();

    // Lambda to get rules for selects from list of printed cards
    /** The Constant fnGetCardPrintedByForgeCard. */
    public static final Lambda1<CardPrinted, Card> FN_GET_CARD_PRINTED_BY_FORGE_CARD = new Lambda1<CardPrinted, Card>() {
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
            lastAdded = CardPrinted.build(card, set, s.getValue().getRarity(), i, card.isAltState(),
                    card.isDoubleFaced());
            this.allCardsFlat.add(lastAdded);
            cardCopies[i] = lastAdded;
        }

        return lastAdded;
    }

    /**
     * Checks if is card supported.
     * 
     * @param cardName
     *            the card name
     * @return true, if is card supported
     */
    public boolean isCardSupported(final String cardName) {
        final ImmutablePair<String, String> nameWithSet = CardDb.splitCardName(cardName);
        if (nameWithSet.right == null) {
            return this.uniqueCards.containsKey(nameWithSet.left.toLowerCase());
        }
        // Set exists?
        final Map<String, CardPrinted[]> cardsFromset = this.allCardsBySet.get(nameWithSet.right.toUpperCase());
        if (cardsFromset == null) {
            return false;
        }
        // Card exists?
        final CardPrinted[] cardCopies = cardsFromset.get(nameWithSet.left.toLowerCase());
        return (cardCopies != null) && (cardCopies.length > 0);
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

    // Single fetch
    /**
     * Gets the card.
     * 
     * @param name
     *            the name
     * @return the card
     */
    public CardPrinted getCard(final String name) {
        // Sometimes they read from decks things like "CardName|Set" - but we
        // can handle it
        final ImmutablePair<String, String> nameWithSet = CardDb.splitCardName(name);
        if (nameWithSet.right != null) {
            return this.getCard(nameWithSet.left, nameWithSet.right);
        }
        // OK, plain name here
        final CardPrinted card = this.uniqueCards.get(nameWithSet.left.toLowerCase());
        if (card != null) {
            return card;
        }
        throw new NoSuchElementException(String.format("Card '%s' not found in our database.", name));
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
    public Iterable<CardPrinted> getAllCards() {
        return this.allCardsFlat;
    }

}
