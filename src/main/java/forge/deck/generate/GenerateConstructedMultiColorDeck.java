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
 * GenerateConstructedMultiColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GenerateConstructedMultiColorDeck {
    private String color1;
    private String color2;
    private String color3;
    private String color4;
    private String color5;

    private final Map<String, String> map = new HashMap<String, String>();
    private final Map<String, String[]> multiMap = new HashMap<String, String[]>();

    /**
     * <p>
     * Constructor for GenerateConstructedMultiColorDeck.
     * </p>
     */
    public GenerateConstructedMultiColorDeck() {
        this.setupBasicLandMap();
        this.setupMultiMap();
    }

    /**
     * <p>
     * setupBasicLandMap.
     * </p>
     */
    private void setupBasicLandMap() {
        this.map.put(Constant.Color.BLACK, "Swamp");
        this.map.put(Constant.Color.BLUE, "Island");
        this.map.put(Constant.Color.GREEN, "Forest");
        this.map.put(Constant.Color.RED, "Mountain");
        this.map.put(Constant.Color.WHITE, "Plains");
    }

    /**
     * <p>
     * setupMultiMap.
     * </p>
     */
    private void setupMultiMap() {
        this.multiMap.put(Constant.Color.BLACK + Constant.Color.BLUE,
                new String[] { "Underground Sea", "Watery Grave" });
        this.multiMap.put(Constant.Color.BLACK + Constant.Color.GREEN, new String[] { "Bayou", "Overgrown Tomb" });
        this.multiMap.put(Constant.Color.BLACK + Constant.Color.RED, new String[] { "Badlands", "Blood Crypt" });
        this.multiMap.put(Constant.Color.BLACK + Constant.Color.WHITE, new String[] { "Scrubland", "Godless Shrine" });
        this.multiMap.put(Constant.Color.BLUE + Constant.Color.BLACK,
                new String[] { "Underground Sea", "Watery Grave" });
        this.multiMap.put(Constant.Color.BLUE + Constant.Color.GREEN,
                new String[] { "Tropical Island", "Breeding Pool" });
        this.multiMap.put(Constant.Color.BLUE + Constant.Color.RED, new String[] { "Volcanic Island", "Steam Vents" });
        this.multiMap.put(Constant.Color.BLUE + Constant.Color.WHITE, new String[] { "Tundra", "Hallowed Fountain" });
        this.multiMap.put(Constant.Color.GREEN + Constant.Color.BLACK, new String[] { "Bayou", "Overgrown Tomb" });
        this.multiMap.put(Constant.Color.GREEN + Constant.Color.BLUE,
                new String[] { "Tropical Island", "Breeding Pool" });
        this.multiMap.put(Constant.Color.GREEN + Constant.Color.RED, new String[] { "Taiga", "Stomping Ground" });
        this.multiMap.put(Constant.Color.GREEN + Constant.Color.WHITE, new String[] { "Savannah", "Temple Garden" });
        this.multiMap.put(Constant.Color.RED + Constant.Color.BLACK, new String[] { "Badlands", "Blood Crypt" });
        this.multiMap.put(Constant.Color.RED + Constant.Color.BLUE, new String[] { "Volcanic Island", "Steam Vents" });
        this.multiMap.put(Constant.Color.RED + Constant.Color.GREEN, new String[] { "Taiga", "Stomping Ground" });
        this.multiMap.put(Constant.Color.RED + Constant.Color.WHITE, new String[] { "Plateau", "Sacred Foundry" });
        this.multiMap.put(Constant.Color.WHITE + Constant.Color.BLACK, new String[] { "Scrubland", "Godless Shrine" });
        this.multiMap.put(Constant.Color.WHITE + Constant.Color.BLUE, new String[] { "Tundra", "Hallowed Fountain" });
        this.multiMap.put(Constant.Color.WHITE + Constant.Color.GREEN, new String[] { "Savannah", "Temple Garden" });
        this.multiMap.put(Constant.Color.WHITE + Constant.Color.RED, new String[] { "Plateau", "Sacred Foundry" });
    }

    /**
     * <p>
     * generate3ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList generate3ColorDeck() {
        CardList deck;

        int check;

        do {
            deck = this.get3ColorDeck();
            check = deck.getType("Creature").size();

        } while ((check < 16) || (24 < check));

        this.addLand(deck, 3);

        if (deck.size() != 60) {
            throw new RuntimeException(
                    "GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is "
                            + deck.size());
        }

        return deck;
    }

    /**
     * <p>
     * generate5ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList generate5ColorDeck() {
        CardList deck;

        deck = this.get5ColorDeck();

        this.addLand(deck, 5);

        if (deck.size() != 60) {
            throw new RuntimeException(
                    "GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is "
                            + deck.size());
        }

        return deck;
    }

    /**
     * <p>
     * addLand.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param colors
     *            a int.
     */
    private void addLand(final CardList list, final int colors) {
        if (colors == 3) {
            final int numberBasic = 2;
            Card land;
            for (int i = 0; i < numberBasic; i++) {

                land = AllZone.getCardFactory().getCard(this.map.get(this.color1).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color2).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color3).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);
            }

            final int numberDual = 4;
            for (int i = 0; i < numberDual; i++) {
                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color2)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color3)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color2 + this.color3)[0],
                        AllZone.getComputerPlayer());
                list.add(land);
            }
            for (int i = 0; i < 2; i++) {
                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color2)[1],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color3)[1],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color2 + this.color3)[1],
                        AllZone.getComputerPlayer());
                list.add(land);
            }
        } else if (colors == 5) {
            final int numberBasic = 1;
            Card land;
            for (int i = 0; i < numberBasic; i++) {

                land = AllZone.getCardFactory().getCard(this.map.get(this.color1).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color2).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color3).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color4).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.map.get(this.color5).toString(),
                        AllZone.getComputerPlayer());
                list.add(land);
            }

            final int numberDual = 2;
            for (int i = 0; i < numberDual; i++) {
                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color2)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color3)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color4)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color1 + this.color5)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color2 + this.color3)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color2 + this.color4)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color2 + this.color5)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color3 + this.color4)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color3 + this.color5)[0],
                        AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(this.multiMap.get(this.color4 + this.color5)[0],
                        AllZone.getComputerPlayer());
                list.add(land);
            }

        }
    } // addLand()

    /**
     * Filters out cards by color and their suitability for being placed in a
     * randomly created deck.
     * 
     * @param colors
     *            the number of different colors the deck should have; if this
     *            is a number other than 3 or 5, we return an empty list.
     * 
     * @return a subset of all cards in the CardFactory database which might be
     *         empty, but never null
     */
    private CardList getCards(final int colors) {
        return this.filterBadCards(AllZone.getCardFactory(), colors);
    } // getCards()

    /**
     * <p>
     * get3ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private CardList get3ColorDeck() {
        final CardList deck = this.get3Colors(this.getCards(3));

        final CardList out = new CardList();
        deck.shuffle();

        // trim deck size down to 36 cards, presumes 24 land, for a total of 60
        // cards
        for (int i = 0; (i < 36) && (i < deck.size()); i++) {
            out.add(deck.get(i));
        }

        return out;
    }

    /**
     * <p>
     * get5ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private CardList get5ColorDeck() {
        final CardList deck = this.get5Colors(this.getCards(5));

        final CardList out = new CardList();
        deck.shuffle();

        // trim deck size down to 36 cards, presumes 24 land, for a total of 60
        // cards
        for (int i = 0; (i < 36) && (i < deck.size()); i++) {
            out.add(deck.get(i));
        }

        return out;
    }

    /**
     * <p>
     * get3Colors.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList get3Colors(final CardList in) {
        int a;
        int b;
        int c;

        a = CardUtil.getRandomIndex(Constant.Color.ONLY_COLORS);
        do {
            b = CardUtil.getRandomIndex(Constant.Color.ONLY_COLORS);
            c = CardUtil.getRandomIndex(Constant.Color.ONLY_COLORS);
        } while ((a == b) || (a == c) || (b == c)); // do not want to get the
                                                    // same
        // color thrice

        this.color1 = Constant.Color.ONLY_COLORS[a];
        this.color2 = Constant.Color.ONLY_COLORS[b];
        this.color3 = Constant.Color.ONLY_COLORS[c];

        CardList out = new CardList();
        out.addAll(CardListUtil.getColor(in, this.color1));
        out.addAll(CardListUtil.getColor(in, this.color2));
        out.addAll(CardListUtil.getColor(in, this.color3));
        out.shuffle();

        final CardList artifact = in.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                // is this really a colorless artifact and not something
                // wierd like Sarcomite Myr which is a colored artifact
                return c.isArtifact() && CardUtil.getColors(c).contains(Constant.Color.COLORLESS)
                        && !Singletons.getModel().getPreferences().isDeckGenRmvArtifacts();
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.isCreature() && (c.getNetAttack() <= 1) && Singletons.getModel().getPreferences().isDeckGenRmvSmall()) {
                    return false;
                }

                return true;
            }
        });

        out = this.filterBadCards(out, 3);
        return out;
    }

    /**
     * <p>
     * get5Colors.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList get5Colors(final CardList in) {

        this.color1 = Constant.Color.BLACK;
        this.color2 = Constant.Color.BLUE;
        this.color3 = Constant.Color.GREEN;
        this.color4 = Constant.Color.RED;
        this.color5 = Constant.Color.WHITE;

        CardList out = new CardList();
        /*
         * out.addAll(CardListUtil.getColor(in, color1));
         * out.addAll(CardListUtil.getColor(in, color2));
         * out.addAll(CardListUtil.getColor(in, color3));
         * out.addAll(CardListUtil.getColor(in, color4));
         * out.addAll(CardListUtil.getColor(in, color5));
         */
        out.addAll(CardListUtil.getGoldCards(in));
        out.shuffle();

        final CardList artifact = in.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                // is this really a colorless artifact and not something
                // wierd like Sarcomite Myr which is a colored artifact
                return c.isArtifact() && CardUtil.getColors(c).contains(Constant.Color.COLORLESS)
                        && !Singletons.getModel().getPreferences().isDeckGenRmvArtifacts();
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.isCreature() && (c.getNetAttack() <= 1) && Singletons.getModel().getPreferences().isDeckGenRmvSmall()) {
                    return false;
                }

                return true;
            }
        });

        out = this.filterBadCards(out, 3);
        return out;
    }

    /**
     * Filters out cards by color and their suitability for being placed in a
     * randomly created deck.
     * 
     * @param sequence
     *            an Iterable of Card instances
     * 
     * @param colors
     *            the number of different colors the deck should have; if this
     *            is a number other than 3 or 5, we return an empty list.
     * 
     * @return a subset of sequence <= sequence which might be empty, but never
     *         null
     */
    private CardList filterBadCards(final Iterable<Card> sequence, final int colors) {
        final ArrayList<Card> goodLand = new ArrayList<Card>();
        // goodLand.add("Faerie Conclave");
        // goodLand.add("Forbidding Watchtower");
        // goodLand.add("Treetop Village");

        CardList out = new CardList();
        if (colors == 3) {

            out = CardFilter.filter(sequence, new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    final ArrayList<String> list = CardUtil.getColors(c);

                    if (list.size() == 3) {
                        if (!list.contains(GenerateConstructedMultiColorDeck.this.color1)
                                || !list.contains(GenerateConstructedMultiColorDeck.this.color2)
                                || !list.contains(GenerateConstructedMultiColorDeck.this.color3)) {
                            return false;
                        }
                    } else if (list.size() == 2) {
                        if (!(list.contains(GenerateConstructedMultiColorDeck.this.color1) && list
                                .contains(GenerateConstructedMultiColorDeck.this.color2))
                                && !(list.contains(GenerateConstructedMultiColorDeck.this.color1) && list
                                        .contains(GenerateConstructedMultiColorDeck.this.color3))
                                && !(list.contains(GenerateConstructedMultiColorDeck.this.color2) && list
                                        .contains(GenerateConstructedMultiColorDeck.this.color3))) {
                            return false;
                        }
                    }

                    return ((CardUtil.getColors(c).size() <= 3) && !c.isLand() && // no
                                                                                  // land
                            !c.getSVar("RemRandomDeck").equals("True") && !c.getSVar("RemAIDeck").equals("True"))
                            || goodLand.contains(c.getName());        // OR very important
                }
            });
        } else if (colors == 5) {
            out = CardFilter.filter(sequence, new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return ((CardUtil.getColors(c).size() >= 2) && // only get
                                                                   // multicolored
                                                                   // cards
                            !c.isLand() && // no land
                            !c.getSVar("RemRandomDeck").equals("True") && !c.getSVar("RemAIDeck").equals("True"))
                            || goodLand.contains(c.getName());                     // OR very important
                }
            });

        }

        return out;
    } // filterBadCards()
}
