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

    private final Map<String, String> map = new HashMap<String, String>();

    /**
     * <p>
     * Constructor for GenerateConstructedDeck.
     * </p>
     */
    public GenerateConstructedDeck() {
        this.setupMap();
    }

    /**
     * <p>
     * setupMap.
     * </p>
     */
    private void setupMap() {
        this.map.put(Constant.Color.BLACK, "Swamp");
        this.map.put(Constant.Color.BLUE, "Island");
        this.map.put(Constant.Color.GREEN, "Forest");
        this.map.put(Constant.Color.RED, "Mountain");
        this.map.put(Constant.Color.WHITE, "Plains");
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
            deck = this.get2ColorDeck();
            check = deck.getType("Creature").size();

        } while ((check < 16) || (24 < check));

        this.addLand(deck);

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
            land = AllZone.getCardFactory().getCard(this.map.get(this.color1).toString(), AllZone.getComputerPlayer());
            list.add(land);

            land = AllZone.getCardFactory().getCard(this.map.get(this.color2).toString(), AllZone.getComputerPlayer());
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
        return this.filterBadCards(AllZone.getCardFactory());
    } // getCards()

    /**
     * <p>
     * get2ColorDeck.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private CardList get2ColorDeck() {
        final CardList deck = this.get2Colors(this.getCards());

        final CardList out = new CardList();
        deck.shuffle();

        // trim deck size down to 34 cards, presumes 26 land, for a total of 60
        // cards
        for (int i = 0; (i < 34) && (i < deck.size()); i++) {
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
            a = CardUtil.getRandomIndex(Constant.Color.ONLY_COLORS);
            b = CardUtil.getRandomIndex(Constant.Color.ONLY_COLORS);
        } while (a == b); // do not want to get the same color twice

        this.color1 = Constant.Color.ONLY_COLORS[a];
        this.color2 = Constant.Color.ONLY_COLORS[b];

        CardList out = new CardList();
        out.addAll(CardListUtil.getColor(in, this.color1));
        out.addAll(CardListUtil.getColor(in, this.color2));
        out.shuffle();

        final CardList artifact = in.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                // is this really a colorless artifact and not something
                // weird like Sarcomite Myr which is a colored artifact
                return c.isArtifact() && CardUtil.getColors(c).contains(Constant.Color.COLORLESS)
                        && !Singletons.getModel().getPreferences().isDeckGenRmvArtifacts();
            }
        });
        out.addAll(artifact);

        out = out.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.isCreature() && (c.getNetAttack() <= 1)
                        && Singletons.getModel().getPreferences().isDeckGenRmvSmall()) {
                    return false;
                }

                return true;
            }
        });

        out = this.filterBadCards(out);
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

        final CardList out = CardFilter.filter(sequence, new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                final ArrayList<String> list = CardUtil.getColors(c);
                if (list.size() == 2) {
                    if (!(list.contains(GenerateConstructedDeck.this.color1) && list
                            .contains(GenerateConstructedDeck.this.color2))) {
                        return false;
                    }
                }
                return ((CardUtil.getColors(c).size() <= 2 // only dual colored
                        )
                        // gold cards
                        && !c.isLand() // no land
                        && !c.getSVar("RemRandomDeck").equals("True") && !c.getSVar("RemAIDeck").equals("True"))
                // OR very important
                        || goodLand.contains(c.getName());
            }
        });

        return out;
    } // filterBadCards()
}
