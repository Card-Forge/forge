package forge.deck.generate;

import forge.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>GenerateConstructedMultiColorDeck class.</p>
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

    private Map<String, String> map = new HashMap<String, String>();
    private Map<String, String[]> multiMap = new HashMap<String, String[]>();

    /**
     * <p>Constructor for GenerateConstructedMultiColorDeck.</p>
     */
    public GenerateConstructedMultiColorDeck() {
        setupBasicLandMap();
        setupMultiMap();
    }

    /**
     * <p>setupBasicLandMap.</p>
     */
    private void setupBasicLandMap() {
        map.put(Constant.Color.Black, "Swamp");
        map.put(Constant.Color.Blue, "Island");
        map.put(Constant.Color.Green, "Forest");
        map.put(Constant.Color.Red, "Mountain");
        map.put(Constant.Color.White, "Plains");
    }

    /**
     * <p>setupMultiMap.</p>
     */
    private void setupMultiMap() {
        multiMap.put(Constant.Color.Black + Constant.Color.Blue, new String[]{"Underground Sea", "Watery Grave"});
        multiMap.put(Constant.Color.Black + Constant.Color.Green, new String[]{"Bayou", "Overgrown Tomb"});
        multiMap.put(Constant.Color.Black + Constant.Color.Red, new String[]{"Badlands", "Blood Crypt"});
        multiMap.put(Constant.Color.Black + Constant.Color.White, new String[]{"Scrubland", "Godless Shrine"});
        multiMap.put(Constant.Color.Blue + Constant.Color.Black, new String[]{"Underground Sea", "Watery Grave"});
        multiMap.put(Constant.Color.Blue + Constant.Color.Green, new String[]{"Tropical Island", "Breeding Pool"});
        multiMap.put(Constant.Color.Blue + Constant.Color.Red, new String[]{"Volcanic Island", "Steam Vents"});
        multiMap.put(Constant.Color.Blue + Constant.Color.White, new String[]{"Tundra", "Hallowed Fountain"});
        multiMap.put(Constant.Color.Green + Constant.Color.Black, new String[]{"Bayou", "Overgrown Tomb"});
        multiMap.put(Constant.Color.Green + Constant.Color.Blue, new String[]{"Tropical Island", "Breeding Pool"});
        multiMap.put(Constant.Color.Green + Constant.Color.Red, new String[]{"Taiga", "Stomping Ground"});
        multiMap.put(Constant.Color.Green + Constant.Color.White, new String[]{"Savannah", "Temple Garden"});
        multiMap.put(Constant.Color.Red + Constant.Color.Black, new String[]{"Badlands", "Blood Crypt"});
        multiMap.put(Constant.Color.Red + Constant.Color.Blue, new String[]{"Volcanic Island", "Steam Vents"});
        multiMap.put(Constant.Color.Red + Constant.Color.Green, new String[]{"Taiga", "Stomping Ground"});
        multiMap.put(Constant.Color.Red + Constant.Color.White, new String[]{"Plateau", "Sacred Foundry"});
        multiMap.put(Constant.Color.White + Constant.Color.Black, new String[]{"Scrubland", "Godless Shrine"});
        multiMap.put(Constant.Color.White + Constant.Color.Blue, new String[]{"Tundra", "Hallowed Fountain"});
        multiMap.put(Constant.Color.White + Constant.Color.Green, new String[]{"Savannah", "Temple Garden"});
        multiMap.put(Constant.Color.White + Constant.Color.Red, new String[]{"Plateau", "Sacred Foundry"});
    }


    /**
     * <p>generate3ColorDeck.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList generate3ColorDeck() {
        CardList deck;

        int check;

        do {
            deck = get3ColorDeck();
            check = deck.getType("Creature").size();

        } while (check < 16 || 24 < check);

        addLand(deck, 3);

        if (deck.size() != 60)
            throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " + deck.size());

        return deck;
    }

    /**
     * <p>generate5ColorDeck.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList generate5ColorDeck() {
        CardList deck;

        deck = get5ColorDeck();

        addLand(deck, 5);

        if (deck.size() != 60)
            throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " + deck.size());

        return deck;
    }

    /**
     * <p>addLand.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @param colors a int.
     */
    private void addLand(CardList list, int colors) {
        if (colors == 3) {
            int numberBasic = 2;
            Card land;
            for (int i = 0; i < numberBasic; i++) {

                land = AllZone.getCardFactory().getCard(map.get(color1).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color2).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color3).toString(), AllZone.getComputerPlayer());
                list.add(land);
            }

            int numberDual = 4;
            for (int i = 0; i < numberDual; i++) {
                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color2)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color3)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color2 + color3)[0], AllZone.getComputerPlayer());
                list.add(land);
            }
            for (int i = 0; i < 2; i++) {
                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color2)[1], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color3)[1], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color2 + color3)[1], AllZone.getComputerPlayer());
                list.add(land);
            }
        } else if (colors == 5) {
            int numberBasic = 1;
            Card land;
            for (int i = 0; i < numberBasic; i++) {

                land = AllZone.getCardFactory().getCard(map.get(color1).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color2).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color3).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color4).toString(), AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(map.get(color5).toString(), AllZone.getComputerPlayer());
                list.add(land);
            }


            int numberDual = 2;
            for (int i = 0; i < numberDual; i++) {
                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color2)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color3)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color4)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color1 + color5)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color2 + color3)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color2 + color4)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color2 + color5)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color3 + color4)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color3 + color5)[0], AllZone.getComputerPlayer());
                list.add(land);

                land = AllZone.getCardFactory().getCard(multiMap.get(color4 + color5)[0], AllZone.getComputerPlayer());
                list.add(land);
            }

        }
    }//addLand()

    /**
     * Filters out cards by color and their suitability for being placed in
     * a randomly created deck.
     *
     * @param colors  the number of different colors the deck should have;
     * if this is a number other than 3 or 5, we return an empty list.
     * 
     * @return a subset of all cards in the CardFactory database 
     * which might be empty, but never null
     */
    private CardList getCards(int colors) {
        return filterBadCards(AllZone.getCardFactory(), colors);
    }//getCards()

    /**
     * <p>get3ColorDeck.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    private CardList get3ColorDeck() {
        CardList deck = get3Colors(getCards(3));

        CardList out = new CardList();
        deck.shuffle();

        //trim deck size down to 36 cards, presumes 24 land, for a total of 60 cards
        for (int i = 0; i < 36 && i < deck.size(); i++)
            out.add(deck.get(i));

        return out;
    }

    /**
     * <p>get5ColorDeck.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    private CardList get5ColorDeck() {
        CardList deck = get5Colors(getCards(5));

        CardList out = new CardList();
        deck.shuffle();

        //trim deck size down to 36 cards, presumes 24 land, for a total of 60 cards
        for (int i = 0; i < 36 && i < deck.size(); i++)
            out.add(deck.get(i));

        return out;
    }

    /**
     * <p>get3Colors.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList get3Colors(CardList in) {
        int a;
        int b;
        int c;

        a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
        do {
            b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
            c = CardUtil.getRandomIndex(Constant.Color.onlyColors);
        } while (a == b || a == c || b == c);//do not want to get the same color thrice

        color1 = Constant.Color.onlyColors[a];
        color2 = Constant.Color.onlyColors[b];
        color3 = Constant.Color.onlyColors[c];

        CardList out = new CardList();
        out.addAll(CardListUtil.getColor(in, color1));
        out.addAll(CardListUtil.getColor(in, color2));
        out.addAll(CardListUtil.getColor(in, color3));
        out.shuffle();

        CardList artifact = in.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                //is this really a colorless artifact and not something
                //wierd like Sarcomite Myr which is a colored artifact
                return c.isArtifact() &&
                        CardUtil.getColors(c).contains(Constant.Color.Colorless) &&
                        !Singletons.getModel().getPreferences().deckGenRmvArtifacts;
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (c.isCreature() &&
                        c.getNetAttack() <= 1 &&
                        Singletons.getModel().getPreferences().deckGenRmvSmall) {
                    return false;
                }

                return true;
            }
        });

        out = filterBadCards(out, 3);
        return out;
    }

    /**
     * <p>get5Colors.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList get5Colors(CardList in) {

        color1 = Constant.Color.Black;
        color2 = Constant.Color.Blue;
        color3 = Constant.Color.Green;
        color4 = Constant.Color.Red;
        color5 = Constant.Color.White;

        CardList out = new CardList();
        /*
        out.addAll(CardListUtil.getColor(in, color1));
        out.addAll(CardListUtil.getColor(in, color2));
        out.addAll(CardListUtil.getColor(in, color3));
        out.addAll(CardListUtil.getColor(in, color4));
        out.addAll(CardListUtil.getColor(in, color5));
        */
        out.addAll(CardListUtil.getGoldCards(in));
        out.shuffle();

        CardList artifact = in.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                //is this really a colorless artifact and not something
                //wierd like Sarcomite Myr which is a colored artifact
                return c.isArtifact() &&
                        CardUtil.getColors(c).contains(Constant.Color.Colorless) &&
                        !Singletons.getModel().getPreferences().deckGenRmvArtifacts;
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (c.isCreature() &&
                        c.getNetAttack() <= 1 &&
                        Singletons.getModel().getPreferences().deckGenRmvSmall) {
                    return false;
                }

                return true;
            }
        });

        out = filterBadCards(out, 3);
        return out;
    }


    /**
     * Filters out cards by color and their suitability for being placed in
     * a randomly created deck.
     *
     * @param sequence  an Iterable of Card instances
     * 
     * @param colors  the number of different colors the deck should have;
     * if this is a number other than 3 or 5, we return an empty list.
     * 
     * @return a subset of sequence <= sequence which might be empty, but
     * never null
     */
    private CardList filterBadCards(Iterable<Card> sequence, int colors) {
        final ArrayList<Card> goodLand = new ArrayList<Card>();
        //goodLand.add("Faerie Conclave");
        //goodLand.add("Forbidding Watchtower");
        //goodLand.add("Treetop Village");

        CardList out = new CardList();
        if (colors == 3) {

            out = CardFilter.filter(sequence, new CardListFilter() {
                public boolean addCard(Card c) {
                    ArrayList<String> list = CardUtil.getColors(c);

                    if (list.size() == 3) {
                        if (!list.contains(color1) || !list.contains(color2) || !list.contains(color3))
                            return false;
                    } else if (list.size() == 2) {
                        if (!(list.contains(color1) && list.contains(color2)) &&
                                !(list.contains(color1) && list.contains(color3)) &&
                                !(list.contains(color2) && list.contains(color3)))
                            return false;
                    }

                    return CardUtil.getColors(c).size() <= 3 &&
                            !c.isLand() && //no land
                            !c.getSVar("RemRandomDeck").equals("True") &&
                            !c.getSVar("RemAIDeck").equals("True") || //OR very important
                            goodLand.contains(c.getName());
                }
            });
        } else if (colors == 5) {
            out = CardFilter.filter(sequence, new CardListFilter() {
                public boolean addCard(Card c) {
                    return CardUtil.getColors(c).size() >= 2 && //only get multicolored cards
                            !c.isLand() && //no land
                            !c.getSVar("RemRandomDeck").equals("True") &&
                            !c.getSVar("RemAIDeck").equals("True") || //OR very important
                            goodLand.contains(c.getName());
                }
            });

        }

        return out;
    }//filterBadCards()
}
