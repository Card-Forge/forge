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
package forge.game.limited;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityMana;
import forge.deck.Deck;
import forge.util.MyRandom;

/**
 * <p>
 * BoosterDraftAI class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class BoosterDraftAI {

    /** The bd. */
    private IBoosterDraft bd = null;
    // once a deck has this number of creatures the computer randomly
    // picks a card, so the final computer deck has 12-20 creatures
    // minimum of creatures per deck
    // private static final int nCreatures = 16;
    /**
     * Constant <code>nDecks=7.</code>
     */
    private static final int N_DECKS = 7;

    // holds all the cards for each of the computer's decks
    private final CardList[] deck = new CardList[BoosterDraftAI.N_DECKS];
    private final String[][] deckColor = new String[BoosterDraftAI.N_DECKS][];

    /**
     * Constant <code>colorToLand.</code>
     */
    private static Map<String, String> colorToLand = new TreeMap<String, String>();

    // picks one Card from in_choose, removes that card, and returns the list
    // returns the cards not picked

    /**
     * <p>
     * choose.
     * </p>
     * 
     * @param chooseFrom
     *            a {@link forge.CardList} object.
     * @param player
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public Card choose(final CardList chooseFrom, final int player) {
        // in_choose should ONLY be on the RIGHT side of any equal sign
        // only 1 card should be removed from in_choose

        if (Constant.Runtime.DEV_MODE[0]) {
            System.out.println("Player[" + player + "] pack: " + chooseFrom.toString());
        }

        final CardList wouldPick = new CardList();
        boolean hasPicked = false;
        Card pickedCard = new Card();

        final CardList aiPlayables = chooseFrom.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                boolean unPlayable = c.getSVar("RemAIDeck").equals("True");
                unPlayable |= c.getSVar("RemRandomDeck").equals("True") && c.getSVar("DeckWants").equals("");
                return !unPlayable;
            }
        });

        if (this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {

            CardList walkers = aiPlayables.getType("Planeswalker");
            if (walkers.size() > 0) {
                pickedCard = walkers.get(0);
                hasPicked = true;
            }

            if (!hasPicked) {
                final CardList creatures = aiPlayables.getType("Creature");
                creatures.sort(new CreatureComparator());
                debugCreatures(creatures);

                if (creatures.size() > 0) {
                    pickedCard = creatures.get(0);
                    hasPicked = true;
                }
            }
            if (hasPicked && !pickedCard.isColorless()) {
                this.playerColors.get(player).setColor1(pickedCard.getColor().get(0).toStringArray().get(0));
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Player[" + player + "] Color1: " + this.playerColors.get(player).getColor1());
                }
                this.playerColors.get(player).setMana1(
                        this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor1()));

                // if the first pick has more than one color add the second as
                // second color to draft
                if (pickedCard.getColor().get(0).toStringArray().size() > 1) {
                    this.playerColors.get(player).setColor2(pickedCard.getColor().get(0).toStringArray().get(1));
                    if (Constant.Runtime.DEV_MODE[0]) {
                        System.out.println("Player[" + player + "] Color2: "
                                + this.playerColors.get(player).getColor2());
                    }

                    this.playerColors.get(player).setMana2(
                            this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor2()));
                }
            }
        } else if (!this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {

            CardList walkers = aiPlayables.getType("Planeswalker");
            if (walkers.size() > 0) {
                pickedCard = walkers.get(0);
                hasPicked = true;
            }

            if (!hasPicked) {
                final CardList creatures = aiPlayables.getType("Creature").getMonoColored(true);
                creatures.sort(new CreatureComparator());
                debugCreatures(creatures);

                if (creatures.size() > 0) {
                    pickedCard = creatures.get(0);
                    hasPicked = true;
                }
            }
            String pickedCardColor = pickedCard.getColor().get(0).toStringArray().get(0);
            if (hasPicked && !pickedCard.isColorless() && !pickedCardColor.equals(this.playerColors.get(player).getColor1())) {
                this.playerColors.get(player).setColor2(pickedCardColor);
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Player[" + player + "] Color2: " + this.playerColors.get(player).getColor2());
                }

                this.playerColors.get(player).setMana2(
                        this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor2()));
            }
        } else {
            CardList typeList;
            CardList colorList;

            colorList = aiPlayables.getOnly2Colors(this.playerColors.get(player).getColor1(),
                    this.playerColors.get(player).getColor2());

            if (colorList.size() > 0) {
                // Since we want about 15 creatures and 7 non-creatures in our deck, we want to pick
                // about 2 creatures for every 1 non-creature. So put 2 creatures in our wouldPick
                // list, and 1 non-creature.
                typeList = colorList.getType("Creature");
                if (typeList.size() > 0) {
                    typeList.sort(new CreatureComparator());
                    wouldPick.add(typeList.get(0));
                    if (typeList.size() > 1) {
                        wouldPick.add(typeList.get(1));
                    }
                }

                typeList = colorList.getType("Instant");
                typeList.addAll(colorList.getType("Sorcery"));
                typeList.addAll(colorList.getType("Enchantment"));
                typeList.addAll(colorList.getType("Artifact"));
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(0));
                }

                typeList = colorList.getType("Planeswalker");
                if (typeList.size() > 0) {
                    // just take it...
                    pickedCard = typeList.get(0);
                    hasPicked = true;
                }

            } else {
                /*
                 * if (!playerColors.get(player).Splash.equals("none")) { //
                 * pick randomly from splash color colorList =
                 * AIPlayables.getColor(playerColors.get(player).Splash); if
                 * (colorList.size() > 0) { Random r = new Random();
                 * list.add(colorList.get(r.nextInt(colorList.size()))); } }
                 * else { // pick splash color ArrayList<String> otherColors =
                 * new ArrayList<String>(); for (int i=0; i<5; i++)
                 * otherColors.add(Constant.Color.onlyColors[i]);
                 * otherColors.remove(playerColors.get(player).Color1);
                 * otherColors.remove(playerColors.get(player).Color2);
                 * 
                 * colorList = new CardList(); for (int i=0;
                 * i<otherColors.size(); i++)
                 * colorList.add(in_choose.getColor(otherColors.get(i)));
                 * 
                 * if (colorList.size() > 0) { Random r = new Random();
                 * pickedCard = colorList.get(r.nextInt(colorList.size()));
                 * playerColors.get(player).Splash =
                 * pickedCard.getColor().get(0).toStringArray().get(0);
                 * System.out
                 * .println("Player["+player+"] Splash: "+playerColors.
                 * get(player).Splash); playerColors.get(player).ManaS =
                 * playerColors
                 * .get(player).ColorToMana(playerColors.get(player).Splash);
                 * hasPicked = true; } }
                 */
                typeList = aiPlayables.getType("Land");
                if (typeList.size() > 0) {
                    for (int i = 0; i < typeList.size(); i++) {
                        final ArrayList<AbilityMana> maList = typeList.get(i).getManaAbility();
                        for (int j = 0; j < maList.size(); j++) {
                            if (maList.get(j).canProduce(this.playerColors.get(player).getMana1())
                                    || maList.get(j).canProduce(this.playerColors.get(player).getMana2())) {
                                wouldPick.add(typeList.get(i));
                            }
                        }
                    }
                }
            }
        }

        if (!hasPicked) {
            final Random r = new Random();

            if (wouldPick.size() > 0) {
                pickedCard = wouldPick.get(r.nextInt(wouldPick.size()));
            } else {
                pickedCard = chooseFrom.get(r.nextInt(chooseFrom.size()));
            }

            hasPicked = true;
        }

        if (hasPicked) {
            chooseFrom.remove(pickedCard);
            this.deck[player].add(pickedCard);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Player[" + player + "] picked " + pickedCard.getName() + " ("
                        + pickedCard.getManaCost() + ") " + pickedCard.getType().toString() + "\n");
            }
        }

        return pickedCard;
    }

    /*
     * I get some wierd error when I have this method, I don't know whats wrong
     * 
     * private void checkDeckList(CardList[] deck) { if(deck.length != nDecks)
     * throw new RuntimeException(
     * "BoosterDraftAI : checkDeckList() error, deck list size is not 7 - "
     * +deck.length);
     * 
     * for(int i = 0; i < nDecks; i++) { if(deck[i].size() != 22) { throw new
     * RuntimeException
     * ("BoosterDraftAI : checkDeckList() error, deck list size is not 22 - "
     * +deck[i].size() +" - " +deck.toString()); } if(countCreatures(deck[i]) <
     * nCreatures) throw new RuntimeException(
     * "BoosterDraftAI : checkDeckList() error, deck needs more creatures - "
     * +countCreatures(deck[i]));
     * 
     * for(int inner = 0; inner < 22; inner++) if(!
     * CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][0]) && !
     * CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][1]))
     * throw new RuntimeException(
     * "BoosterDraftAI : checkDeckList() error, deck has different card colors"
     * ); }//for }//checkDeckList()
     */

    // private int countCreatures(CardList list) {return
    // list.getType("Creature").size();}

    /**
     * <p>
     * testColors.
     * </p>
     * 
     * @param n
     *            an array of int.
     */
    private void testColors(final int[] n) {
        if (n.length != BoosterDraftAI.N_DECKS) {
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers array length does not equal 7");
        }

        final Set<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < BoosterDraftAI.N_DECKS; i++) {
            set.add(Integer.valueOf(n[i]));
        }

        if (set.size() != BoosterDraftAI.N_DECKS) {
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers not unique");
        }

        for (int i = 0; i < BoosterDraftAI.N_DECKS; i++) {
            if ((n[i] < 0) || (this.deckColorChoices.length <= n[i])) {
                throw new RuntimeException("BoosterDraftAI : testColors error, index out of range - " + n[i]);
            }
        }
    } // testColors()

    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDecks() {
        // check CardList[] deck for errors
        // checkDeckList(deck);

        final Deck[] out = new Deck[this.deck.length];

        for (int i = 0; i < this.deck.length; i++) {
            // addLand(deck[i], deckColor[i]);
            // out[i] = getDeck(deck[i]);
            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Deck[" + i + "]");
            }

            out[i] = new BoosterDeck(this.deck[i], this.playerColors.get(i));
        }
        return out;
    } // getDecks()

    // returns 7 different ints, within the range of 0-9

    /**
     * <p>
     * getDeckColors.
     * </p>
     * 
     * @return an array of int.
     */
    private int[] getDeckColors() {
        final int[] out = new int[BoosterDraftAI.N_DECKS];
        int start = MyRandom.getRandom().nextInt(10);

        for (int i = 0; i < out.length; i++) {
            // % to get an index between 0 and deckColorChoices.length
            out[i] = start % this.deckColorChoices.length;
            start++;
        }
        this.testColors(out);

        return out;
    } // getDeckColors()

    /**
     * <p>
     * Constructor for BoosterDraftAI.
     * </p>
     */
    public BoosterDraftAI() {
        // choose colors for decks
        final int[] n = this.getDeckColors();
        for (int i = 0; i < n.length; i++) {
            this.deckColor[i] = this.deckColorChoices[n[i]];
        }

        // initialize color map
        BoosterDraftAI.colorToLand.put(Constant.Color.BLACK, "Swamp");
        BoosterDraftAI.colorToLand.put(Constant.Color.BLUE, "Island");
        BoosterDraftAI.colorToLand.put(Constant.Color.GREEN, "Forest");
        BoosterDraftAI.colorToLand.put(Constant.Color.RED, "Mountain");
        BoosterDraftAI.colorToLand.put(Constant.Color.WHITE, "Plains");

        // Initialize deck array and playerColors list
        for (int i = 0; i < this.deck.length; i++) {
            this.deck[i] = new CardList();
            this.playerColors.add(new DeckColors());
        }

    } // BoosterDraftAI()

    /**
     * Gets the bd.
     * 
     * @return the bd
     */
    public IBoosterDraft getBd() {
        return this.bd;
    }

    /**
     * Sets the bd.
     * 
     * @param bd0
     *            the bd to set
     */
    public void setBd(final IBoosterDraft bd0) {
        this.bd = bd0;
    }

    private final ArrayList<DeckColors> playerColors = new ArrayList<DeckColors>();

    // all 10 two color combinations
    private final String[][] deckColorChoices = { { Constant.Color.BLACK, Constant.Color.BLUE },
            { Constant.Color.BLACK, Constant.Color.GREEN }, { Constant.Color.BLACK, Constant.Color.RED },
            { Constant.Color.BLACK, Constant.Color.WHITE },

            { Constant.Color.BLUE, Constant.Color.GREEN }, { Constant.Color.BLUE, Constant.Color.RED },
            { Constant.Color.BLUE, Constant.Color.WHITE },

            { Constant.Color.GREEN, Constant.Color.RED }, { Constant.Color.GREEN, Constant.Color.WHITE },

            { Constant.Color.RED, Constant.Color.WHITE } };

    private static void debugCreatures(CardList creatures) {
        if (Constant.Runtime.DEV_MODE[0]) {
            for (Card c : creatures) {
                System.out.println(c.toString() + ": Cost " + c.getCMC() + ", Eval " + CardFactoryUtil.evaluateCreature(c));
            }
        }
    }
} // BoosterDraftAI()

