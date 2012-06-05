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
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.mana.ManaCostShard;
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
                if (c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True")) {
                    return false;
                }
                return true;
            }
        });

        if (this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            //
            final CardList creatures = aiPlayables.getType("Creature").getColored();
            creatures.sort(this.bestCreature);
            // for (int i=0; i<creatures.size(); i++)
            // System.out.println("creature[" + i + "]: " +
            // creatures.get(i).getName());

            if (creatures.size() > 0) {
                pickedCard = creatures.get(creatures.size() - 1);
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

                hasPicked = true;
            }
        } else if (!this.playerColors.get(player).getColor1().equals("none")
                && this.playerColors.get(player).getColor2().equals("none")) {
            final CardList creatures = aiPlayables.getType("Creature").getMonoColored();
            creatures.sort(this.bestCreature);
            // for (int i=0; i<creatures.size(); i++)
            // System.out.println("creature[" + i + "]: " +
            // creatures.get(i).getName());

            if (creatures.size() > 0) {
                pickedCard = creatures.get(creatures.size() - 1);
                this.playerColors.get(player).setColor2(pickedCard.getColor().get(0).toStringArray().get(0));
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Player[" + player + "] Color2: " + this.playerColors.get(player).getColor2());
                }

                this.playerColors.get(player).setMana2(
                        this.playerColors.get(player).colorToMana(this.playerColors.get(player).getColor2()));
                hasPicked = true;
            }
        } else {
            CardList typeList;
            CardList colorList;

            colorList = aiPlayables.getOnly2Colors(this.playerColors.get(player).getColor1(),
                    this.playerColors.get(player).getColor2());

            if (colorList.size() > 0) {
                typeList = colorList.getType("Creature");
                if (typeList.size() > 0) {
                    typeList.sort(this.bestCreature);
                    typeList.reverse();
                    wouldPick.add(typeList.get(0));
                    if (typeList.size() > 1) {
                        wouldPick.add(typeList.get(1));
                    }
                }

                typeList = colorList.getType("Instant");
                typeList.addAll(colorList.getType("Sorcery"));
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(typeList.size() / 2));
                }

                typeList = colorList.getType("Enchantment");
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(0));
                }

                typeList = colorList.getType("Planeswalker");
                if (typeList.size() > 0) {
                    wouldPick.add(typeList.get(0));
                }

                typeList = colorList.getType("Artifact");
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(0));
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
                wouldPick.shuffle();
                pickedCard = wouldPick.get(r.nextInt(wouldPick.size()));
            } else {
                chooseFrom.shuffle();
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

            out[i] = this.buildDeck(this.deck[i], this.playerColors.get(i));
        }
        return out;
    } // getDecks()

    /**
     * <p>
     * buildDeck.
     * </p>
     * 
     * @param dList
     *            a {@link forge.CardList} object.
     * @param pClrs
     *            a {@link forge.game.limited.DeckColors} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck buildDeck(final CardList dList, final DeckColors pClrs) {

        final CardList outList = new CardList();
        int cardsNeeded = 22;
        int landsNeeded = 18;

        final CardList aiPlayables = dList.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !(c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True"));
            }
        });
        for (int i = 0; i < aiPlayables.size(); i++) {
            dList.remove(aiPlayables.get(i));
        }

        final CardList creatures = aiPlayables.getType("Creature").getOnly2Colors(pClrs.getColor1(), pClrs.getColor2());

        int nCreatures = 15;

        creatures.sort(this.bestCreature);
        creatures.reverse();

        // 1.Add up to 15 on-color creatures
        int i = 0;
        while (nCreatures > 0 && creatures.size() > 0) {
            final Card c = creatures.get(0);

            outList.add(c);
            cardsNeeded--;
            nCreatures--;
            aiPlayables.remove(c);
            creatures.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }

            i++;
        }

        /*CardList otherCreatures = aiPlayables.getType("Creature");
        while ((nCreatures > 1) && (otherCreatures.size() > 1)) {
            final Card c = otherCreatures.get(MyRandom.getRandom().nextInt(otherCreatures.size() - 1));
            outList.add(c);
            cardsNeeded--;
            nCreatures--;
            aiPlayables.remove(c);

            otherCreatures = aiPlayables.getType("Creature");

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("AddCreature: " + c.getName() + " (" + c.getManaCost() + ")");
            }
        }*/

        CardList others = aiPlayables.getNotType("Creature").getNotType("Land")
                .getOnly2Colors(pClrs.getColor1(), pClrs.getColor2());

        // 2.Try to fill up to 22 with on-color non-creature cards
        int ii = 0;
        while (cardsNeeded > 0 && others.size() > 0) {
            int index = 0;
            if (others.size() > 1) {
                index = MyRandom.getRandom().nextInt(others.size() - 1);
            }
            final Card c = others.get(index);

            // out.addMain(c.getName());
            outList.add(c);
            cardsNeeded--;
            aiPlayables.remove(c);

            others = aiPlayables.getNotType("Creature").getNotType("Land")
                    .getOnly2Colors(pClrs.getColor1(), pClrs.getColor2());

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Others[" + ii++ + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }
        }

        i = 0;
        // 3.Try to fill up to 22 with on-color creatures cards (if more than 15 are present)
        while (cardsNeeded > 0 && (0 < creatures.size())) {
            final Card c = creatures.get(0);

            outList.add(c);
            cardsNeeded--;
            aiPlayables.remove(c);
            creatures.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }

            i++;
        }

        CardList nonLands = aiPlayables.getNotType("Land")
                .getOnly2Colors(pClrs.getColor1(), pClrs.getColor2());

        // 4. If there are still on-color cards and the average cmc is low add a 23rd card.
        if (cardsNeeded == 0 && CardListUtil.getAverageCMC(outList) < 3 && !nonLands.isEmpty()) {
            Card c = nonLands.get(0);
            outList.add(c);
            aiPlayables.remove(0);
            landsNeeded--;
        }

        // 5. If there are still less than 22 non-land cards add off-color cards.
        ii = 0;
        CardList z = aiPlayables.getNotType("Land");
        while ((cardsNeeded > 0) && (z.size() > 1)) {

            // if (z.size() < 1)
            // throw new
            // RuntimeException("BoosterDraftAI : buildDeck() error, deck does not have enough non-lands");
            final Card c = z.get(MyRandom.getRandom().nextInt(z.size() - 1));

            // out.addMain(c.getName());
            outList.add(c);
            cardsNeeded--;
            aiPlayables.remove(c);

            z = aiPlayables.getNotType("Land");

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("NonLands[" + ii++ + "]:" + c.getName() + "(" + c.getManaCost() + ")");
            }
        }

        // 6. If it's not a mono color deck, add non-basic lands.
        CardList lands = aiPlayables.getType("Land");
        while (!pClrs.getColor1().equals(pClrs.getColor2()) && landsNeeded > 0 && lands.size() > 0) {
            final Card c = lands.get(0);

            outList.add(c);
            landsNeeded--;
            aiPlayables.remove(c);

            lands = aiPlayables.getType("Land");

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Land:" + c.getName());
            }
        }

        // attempt to optimize basic land counts according
        // to color representation

        final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                new CCnt("Mountain", 0), new CCnt("Forest", 0) };

        // count each card color using mana costs
        // TODO: count hybrid mana differently?
        for (i = 0; i < outList.size(); i++) {
            final CardManaCost mc = outList.get(i).getManaCost();

            // count each mana symbol in the mana cost
            for (ManaCostShard shard : mc.getShards()) {
                byte mask = shard.getColorMask();

                if ((mask & CardColor.WHITE) > 0) {
                    clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                }
                if ((mask & CardColor.BLUE) > 0) {
                    clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                }
                if ((mask & CardColor.BLACK) > 0) {
                    clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                }
                if ((mask & CardColor.RED) > 0) {
                    clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                }
                if ((mask & CardColor.GREEN) > 0) {
                    clrCnts[4].setCount(clrCnts[4].getCount() + 1);
                }
            }
        }

        if (landsNeeded > 0) {
            // total of all ClrCnts
            int totalColor = 0;
            for (i = 0; i < 5; i++) {
                totalColor += clrCnts[i].getCount();
                // tmpDeck += ClrCnts[i].Color + ":" + ClrCnts[i].Count + "\n";
            }

            // tmpDeck += "totalColor:" + totalColor + "\n";

            for (i = 0; i < 5; i++) {
                if (clrCnts[i].getCount() > 0) { // calculate number of lands
                                                 // for
                    // each color
                    final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                    final int nLand = (int) (landsNeeded * p) + 1;
                    // tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand +
                    // "\n";
                    if (Constant.Runtime.DEV_MODE[0]) {
                        System.out.println("Basics[" + clrCnts[i].getColor() + "]:" + nLand);
                    }

                    // just to prevent a null exception by the deck size fixing
                    // code
                    // CardCounts.put(ClrCnts[i].Color, nLand);

                    for (int j = 0; j <= nLand; j++) {
                        final Card c = AllZone.getCardFactory().getCard(clrCnts[i].getColor(),
                                AllZone.getComputerPlayer());
                        c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                        outList.add(c);
                        landsNeeded--;
                    }
                }
            }
            int n = 0;
            while (landsNeeded > 0) {
                if (clrCnts[n].getCount() > 0) {
                    final Card c = AllZone.getCardFactory().getCard(clrCnts[n].getColor(), AllZone.getComputerPlayer());
                    c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                    outList.add(c);
                    landsNeeded--;

                    if (Constant.Runtime.DEV_MODE[0]) {
                        System.out.println("AddBasics: " + c.getName());
                    }
                }
                if (++n > 4) {
                    n = 0;
                }
            }
        }

        while (outList.size() > 40) {
            final Card c = outList.get(MyRandom.getRandom().nextInt(outList.size() - 1));
            outList.remove(c);
            aiPlayables.add(c);
        }

        while (outList.size() < 40) {
            if (aiPlayables.size() > 1) {
                final Card c = aiPlayables.get(MyRandom.getRandom().nextInt(aiPlayables.size() - 1));
                outList.add(c);
                aiPlayables.remove(c);
            } else if (aiPlayables.size() == 1) {
                final Card c = aiPlayables.get(0);
                outList.add(c);
                aiPlayables.remove(c);
            } else {
                //if no playable cards remain fill up with basic lands
                for (i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) {
                        final Card c = AllZone.getCardFactory().getCard(clrCnts[i].getColor(),
                                AllZone.getComputerPlayer());
                        c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                        outList.add(c);
                        break;
                    }
                }
            }
        }
        if (outList.size() == 40) {
            final Deck out = new Deck();
            out.getMain().add(outList);
            out.getSideboard().add(aiPlayables);
            out.getSideboard().add(dList);
            return out;
        }
        throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");
    }

    /*
     * private Deck getDeck(CardList list) { Deck out = new
     * Deck(GameType.Draft); for(int i = 0; i < list.size(); i++)
     * out.addMain(list.get(i).getName());
     * 
     * return out; }//getDeck()
     * 
     * //add Land to list argument private void addLand(CardList list, String[]
     * color) { Card land; for(int i = 0; i < 9; i++) { land =
     * AllZone.getCardFactory().getCard(colorToLand.get(color[0]).toString(),
     * AllZone.getComputerPlayer());
     * 
     * land.setCurSetCode(land.getMostRecentSet());
     * land.setImageFilename(CardUtil.buildFilename(land));
     * 
     * list.add(land);
     * 
     * land =
     * AllZone.getCardFactory().getCard(colorToLand.get(color[1]).toString(),
     * AllZone.getComputerPlayer());
     * 
     * land.setCurSetCode(land.getMostRecentSet());
     * land.setImageFilename(CardUtil.buildFilename(land));
     * 
     * list.add(land); }
     * 
     * //if(list.getType("Land").size() != 18) //throw new RuntimeException(
     * "BoosterDraftAI : addLand() error, deck does not have 18 lands - "
     * +list.getType("Land").size());
     * 
     * //if(list.size() != 40) //throw new
     * RuntimeException("BoosterDraftAI : addLand() error, deck is not 40 cards - "
     * +list.size()); }//addLand()
     */

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

        // initilize color map
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

    private final Comparator<Card> bestCreature = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            int cmcA = a.getCMC();
            if (cmcA == 0) {
                cmcA = 1;
            }
            cmcA *= 10;

            int cmcB = b.getCMC();
            if (cmcB == 0) {
                cmcB = 1;
            }
            cmcB *= 10;

            final int attA = a.getBaseAttack() * 10;
            final int attB = b.getBaseAttack() * 10;

            final int defA = a.getBaseDefense() * 10;
            final int defB = b.getBaseDefense() * 10;

            final int keyA = a.getKeyword().size() * 10;
            final int keyB = b.getKeyword().size() * 10;

            final int abA = a.getSpellAbility().length * 10;
            final int abB = b.getSpellAbility().length * 10;

            final int trgA = a.getTriggers().size() * 10;
            final int trgB = b.getTriggers().size() * 10;

            int rarA = 0;
            int rarB = 0;

            if (a.getCurSetRarity().equals("Common")) {
                rarA = 1;
            } else if (a.getCurSetRarity().equals("Uncommon")) {
                rarA = 2;
            } else if (a.getCurSetRarity().equals("Rare")) {
                rarA = 4;
            } else if (a.getCurSetRarity().equals("Mythic")) {
                rarA = 8;
            }

            if (b.getCurSetRarity().equals("Common")) {
                rarB = 1;
            } else if (b.getCurSetRarity().equals("Uncommon")) {
                rarB = 2;
            } else if (b.getCurSetRarity().equals("Rare")) {
                rarB = 4;
            } else if (b.getCurSetRarity().equals("Mythic")) {
                rarB = 8;
            }

            final int scoreA = ((attA + defA) / cmcA) + keyA + abA + trgA + rarA;
            final int scoreB = ((attB + defB) / cmcB) + keyB + abB + trgB + rarB;

            return scoreA - scoreB;
        }
    };
} // BoosterDraftAI()

