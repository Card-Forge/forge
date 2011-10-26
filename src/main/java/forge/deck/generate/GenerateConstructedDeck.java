package forge.deck.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.CardFilter;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Constant;
import forge.Singletons;

/**
 * <p>
 * GenerateConstructedDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GenerateConstructedDeck {
    private String color1;
    private String color2;

    private Map<String, String> map = new HashMap<String, String>();

    /**
     * <p>
     * Constructor for GenerateConstructedDeck.
     * </p>
     */
    public GenerateConstructedDeck() {
        setupMap();
    }

    /**
     * <p>
     * setupMap.
     * </p>
     */
    private void setupMap() {
        map.put(Constant.Color.Black, "Swamp");
        map.put(Constant.Color.Blue, "Island");
        map.put(Constant.Color.Green, "Forest");
        map.put(Constant.Color.Red, "Mountain");
        map.put(Constant.Color.White, "Plains");
    }

    /**
     * <p>
     * generateDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList generateDeck() {
        CardList deck;

        int check;

        do {
            deck = get2ColorDeck();
            check = deck.getType("Creature").size();

        } while (check < 16 || 24 < check);

        addLand(deck);

        if (deck.size() != 60) {
            throw new RuntimeException(
                    "GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is "
                            + deck.size());
        }
        return deck;
    }

    // 25 lands
    /**
     * <p>
     * addLand.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    private void addLand(final CardList list) {
        Card land;
        for (int i = 0; i < 13; i++) {
            land = AllZone.getCardFactory().getCard(map.get(color1).toString(), AllZone.getComputerPlayer());
            list.add(land);

            land = AllZone.getCardFactory().getCard(map.get(color2).toString(), AllZone.getComputerPlayer());
            list.add(land);
        }
    } // addLand()

    /**
     * Creates a CardList from the set of all cards that meets the criteria for
     * color(s), type, whether the card is suitable for placement in random
     * decks and in AI decks, etc.
     * 
     * @see #filterBadCards(Iterable)
     * 
     * @return a subset of cards <= the set of all cards; might be empty, but
     *         never null
     */
    private CardList getCards() {
        return filterBadCards(AllZone.getCardFactory());
    } // getCards()

    /**
     * <p>
     * get2ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private CardList get2ColorDeck() {
        CardList deck = get2Colors(getCards());

        CardList out = new CardList();
        deck.shuffle();

        // trim deck size down to 34 cards, presumes 26 land, for a total of 60
        // cards
        for (int i = 0; i < 34 && i < deck.size(); i++) {
            out.add(deck.get(i));
        }
        return out;
    }

    /**
     * <p>
     * get2Colors.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList get2Colors(final CardList in) {
        int a;
        int b;

        do {
            a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
            b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
        } while (a == b); // do not want to get the same color twice

        color1 = Constant.Color.onlyColors[a];
        color2 = Constant.Color.onlyColors[b];

        CardList out = new CardList();
        out.addAll(CardListUtil.getColor(in, color1));
        out.addAll(CardListUtil.getColor(in, color2));
        out.shuffle();

        CardList artifact = in.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                // is this really a colorless artifact and not something
                // weird like Sarcomite Myr which is a colored artifact
                return c.isArtifact() && CardUtil.getColors(c).contains(Constant.Color.Colorless)
                        && !Singletons.getModel().getPreferences().deckGenRmvArtifacts;
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.isCreature() && c.getNetAttack() <= 1 && Singletons.getModel().getPreferences().deckGenRmvSmall) {
                    return false;
                }

                return true;
            }
        });

        out = filterBadCards(out);
        return out;
    }

    /**
     * Creates a CardList from the given sequence that meets the criteria for
     * color(s), type, whether the card is suitable for placement in random
     * decks and in AI decks, etc.
     * 
     * @param sequence
     *            an iterable over Card instances
     * 
     * @return a subset of sequence <= sequence; might be empty, but never null
     */
    private CardList filterBadCards(final Iterable<Card> sequence) {

        final ArrayList<Card> goodLand = new ArrayList<Card>();

        CardList out = CardFilter.filter(sequence, new CardListFilter() {
            public boolean addCard(final Card c) {
                ArrayList<String> list = CardUtil.getColors(c);
                if (list.size() == 2) {
                    if (!(list.contains(color1) && list.contains(color2))) {
                        return false;
                    }
                }
                return CardUtil.getColors(c).size() <= 2 // only dual colored
                                                         // gold cards
                        && !c.isLand() // no land
                        && !c.getSVar("RemRandomDeck").equals("True") && !c.getSVar("RemAIDeck").equals("True")
                        // OR very important
                        || goodLand.contains(c.getName());
            }
        });

        return out;
    } // filterBadCards()
}
