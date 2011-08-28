package forge.card;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import forge.Card;


/**
 * <p>CardDb class.</p>
 *
 * @author Forge
 * @version $Id: CardDb.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardDb {
    private static volatile CardDb onlyInstance = null; // 'volatile' keyword makes this working
    public static CardDb instance() {
        if (onlyInstance == null) {
            synchronized (CardDb.class) {
                if (onlyInstance == null) { // It's broken under 1.4 and below, on 1.5+ works again!
                    onlyInstance = new CardDb();
                }
            }
        }
        return onlyInstance;
    }

    // Here oracle cards
    private final Map<String, CardRules> cards = new Hashtable<String, CardRules>();
    // Here are refs, get them by name
    private final Map<String, CardPrinted> uniqueCards = new Hashtable<String, CardPrinted>();

    // need this to obtain cardReference by name+set+artindex
    private final Map<String, Map<String, CardPrinted[]>> allCardsBySet = new Hashtable<String, Map<String, CardPrinted[]>>();
    // this is the same list in flat storage
    private final List<CardPrinted> allCardsFlat = new ArrayList<CardPrinted>();


    private CardDb() {
       this(new MtgDataParser()); // I wish cardname.txt parser was be here.
    }

    private CardDb(final Iterator<CardRules> parser) {
        while (parser.hasNext()) {
            addNewCard(parser.next());
        }
    }

    public void addNewCard(final CardRules card) {
        if (null == card) { return; }
        //System.out.println(card.getName());
        String cardName = card.getName();

        // 1. register among oracle uniques
        cards.put(cardName, card);

        // 2. Save refs into two lists: one flat and other keyed with sets & name
        CardPrinted lastAdded = null;
        for (Entry<String, CardInSet> s : card.getSetsPrinted()) {
            String set = s.getKey();

            // get this set storage, if not found, create it!
            Map<String, CardPrinted[]> setMap = allCardsBySet.get(set);
            if (null == setMap) {
                setMap = new Hashtable<String, CardPrinted[]>();
                allCardsBySet.put(set, setMap);
            }

            int count = s.getValue().getCopiesCount();
            CardPrinted[] cardCopies = new CardPrinted[count];
            setMap.put(cardName, cardCopies);
            for (int i = 0; i < count; i++) {
                lastAdded = CardPrinted.build(card, set, s.getValue().getRarity(), i);
                allCardsFlat.add(lastAdded);
                cardCopies[i] = lastAdded;
            }
        }
        uniqueCards.put(cardName, lastAdded);
    }

    // Single fetch
    public CardPrinted getCard(final String name) {
        // Sometimes they read from decks things like "CardName|Set" - but we can handle it
        int pipePos = name.indexOf('|'); 
        if (pipePos >= 0) { return getCard(name.substring(0, pipePos), name.substring(pipePos+1)); }
        // OK, plain name here
        CardPrinted card = uniqueCards.get(name);
        if (card != null) { return card; }
        throw new NoSuchElementException(String.format("Card '%s' not found in our database.", name)); 
    }
    // Advanced fetch by name+set
    public CardPrinted getCard(final String name, final String set) { return getCard(name, set, 1); }
    public CardPrinted getCard(final String name, final String set, final int artIndex) {
        // 1. get set
        Map<String, CardPrinted[]> cardsFromset = allCardsBySet.get(set);
        if (null == cardsFromset) {
            String err = String.format("Asked for card '%s' from set '%s': that set was not found. :(", name, set);
            throw new NoSuchElementException(err);
        }
        // 2. Find the card itself
        CardPrinted[] cards = cardsFromset.get(name);
        if (null == cards) {
            String err = String.format("Asked for card '%s' from '%s': set found, but the card wasn't. :(", name, set);
            throw new NoSuchElementException(err);
        }
        // 3. Get the proper copy
        if (artIndex > 0 && artIndex <= cards.length) { return cards[artIndex]; }
        String err = String.format("Asked for '%s' from '%s' #%d: db didn't find that copy.", name, set, artIndex);
        throw new NoSuchElementException(err);
    }

    // Fetch from Forge's Card instance. Well, there should be no errors, but we'll still check
    public CardPrinted getCard(final Card forgeCard) {
        String name = forgeCard.getName();
        String set = forgeCard.getCurSetCode();
        if (StringUtils.isNotBlank(set)) { return getCard(name, set); }
        return getCard(name);
    }

    // Multiple fetch
    public List<CardPrinted> getCards(final List<String> names) {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        for (String name : names) { result.add(getCard(name)); }
        return result;
    }

    // returns a list of all cards from their respective latest editions
    public Iterable<CardPrinted> getAllUniqueCards() {
        return uniqueCards.values();
    }

    public List<CardPrinted> getAllCards() { return allCardsFlat; }

}
