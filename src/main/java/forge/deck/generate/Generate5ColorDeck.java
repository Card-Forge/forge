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
import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Constant;
import forge.PlayerType;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyRandom;
import forge.util.Predicate;

/**
 * <p>
 * Generate5ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate5ColorDeck {
    private String color1 = "white";
    private String color2 = "blue";
    private String color3 = "black";
    private String color4 = "red";
    private String color5 = "green";
    private Random r = null;
    private Map<String, String> colorMap = null;
    private ArrayList<String> notColors = null;
    private ArrayList<String> dualLands = null;
    private Map<String, Integer> cardCounts = null;
    private int maxDuplicates = 4;

    /**
     * Instantiates a new generate5 color deck.
     */
    public Generate5ColorDeck() {
        this("white", "blue", "black", "red", "green");
    }

    /**
     * <p>
     * Constructor for Generate5ColorDeck.
     * </p>
     * 
     * @param color1
     *            a {@link java.lang.String} object.
     * @param color2
     *            a {@link java.lang.String} object.
     * @param color3
     *            a {@link java.lang.String} object.
     * @param color4
     *            a {@link java.lang.String} object.
     * @param color5
     *            a {@link java.lang.String} object.
     */
    public Generate5ColorDeck(final String color1, final String color2, final String color3, final String color4,
            final String color5) {
        this.r = MyRandom.getRandom();

        this.cardCounts = new HashMap<String, Integer>();

        this.colorMap = new HashMap<String, String>();
        this.colorMap.put("white", "W");
        this.colorMap.put("blue", "U");
        this.colorMap.put("black", "B");
        this.colorMap.put("red", "R");
        this.colorMap.put("green", "G");

        this.notColors = new ArrayList<String>();
        this.notColors.add("white");
        this.notColors.add("blue");
        this.notColors.add("black");
        this.notColors.add("red");
        this.notColors.add("green");

        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.color4 = color4;
        this.color5 = color5;

        this.notColors.remove(this.color1);
        this.notColors.remove(this.color2);
        this.notColors.remove(this.color3);
        this.notColors.remove(this.color4);
        this.notColors.remove(this.color5);

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS)) {
            this.maxDuplicates = 1;
        }

        this.dualLands = GenerateDeckUtil.getDualLandList("WUBRG");

        for (int i = 0; i < this.dualLands.size(); i++) {
            this.cardCounts.put(this.dualLands.get(i), 0);
        }
    }

    /**
     * <p>
     * get3ColorDeck.
     * </p>
     * 
     * @param deckSize
     *            a int.
     * @param playerType
     *            a PlayerType
     * @return a {@link forge.CardList} object.
     */
    public final CardList get5ColorDeck(final int deckSize, final PlayerType playerType) {
        int loopCounter = 0; // loop counter to prevent infinite card selection loops
        String tmpDeckErrorMessage = "";
        final CardList tempDeck = new CardList();

        final int landsPercentage = 44;
        final int creaturePercentage = 34;
        final int spellPercentage = 22;

        // start with all cards
        // remove cards that generated decks don't like
        Predicate<Card> toUse = playerType == PlayerType.HUMAN ? GenerateDeckUtil.humanCanPlay : GenerateDeckUtil.aiCanPlay;
        CardList allCards = new CardList(toUse.select(AllZone.getCardFactory()));
        
        // reduce to cards that match the colors
        CardList cardList1 = allCards.getColor(this.color1);
        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS)) {
            cardList1.addAll(allCards.getColor(Constant.Color.COLORLESS));
        }
        CardList cardList2 = allCards.getColor(this.color2);
        CardList cardList3 = allCards.getColor(this.color3);
        CardList cardList4 = allCards.getColor(this.color4);
        CardList cardList5 = allCards.getColor(this.color5);

        // remove multicolor cards that don't match the colors
        final CardListFilter cardListFilter = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                for (int i = 0; i < Generate5ColorDeck.this.notColors.size(); i++) {
                    if (c.getManaCost().contains(
                            Generate5ColorDeck.this.colorMap.get(Generate5ColorDeck.this.notColors.get(i)))) {
                        return false;
                    }
                }
                return true;
            }
        };
        cardList1 = cardList1.filter(cardListFilter);
        cardList2 = cardList2.filter(cardListFilter);
        cardList3 = cardList3.filter(cardListFilter);
        cardList4 = cardList4.filter(cardListFilter);
        cardList5 = cardList5.filter(cardListFilter);

        // build subsets based on type
        final CardList creatureCardList1 = cardList1.getType("Creature");
        final CardList creatureCardList2 = cardList2.getType("Creature");
        final CardList creatureCardList3 = cardList3.getType("Creature");
        final CardList creatureCardList4 = cardList4.getType("Creature");
        final CardList creatureCardList5 = cardList5.getType("Creature");

        final String[] nonCreatureSpells = { "Instant", "Sorcery", "Enchantment", "Planeswalker", "Artifact.nonCreature" };
        final CardList spellCardList1 = cardList1.getValidCards(nonCreatureSpells, null, null);
        final CardList spellCardList2 = cardList2.getValidCards(nonCreatureSpells, null, null);
        final CardList spellCardList3 = cardList3.getValidCards(nonCreatureSpells, null, null);
        final CardList spellCardList4 = cardList4.getValidCards(nonCreatureSpells, null, null);
        final CardList spellCardList5 = cardList5.getValidCards(nonCreatureSpells, null, null);

        // final card pools
        final CardList creatures12345 = new CardList();
        final CardList spells12345 = new CardList();

        // used for mana curve in the card pool
        final int[] minConvertedManaCost = { 0 };
        final int[] maxConvertedManaCost = { 2 };
        final CardListFilter convertedManaCostFilter = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                final int cardConvertedManaCost = c.getCMC();
                return (cardConvertedManaCost >= minConvertedManaCost[0]) && (cardConvertedManaCost <= maxConvertedManaCost[0]);
            }
        };

        // select cards to build card pools using a mana curve
        for (int i = 3; i > 0; i--) {
            if (i == 1) {
                maxConvertedManaCost[0] = 20; //the last category is open ended
            }
            final CardList creature1ConvertedManaCost = creatureCardList1.filter(convertedManaCostFilter);
            final CardList creature2ConvertedManaCost = creatureCardList2.filter(convertedManaCostFilter);
            final CardList creature3ConvertedManaCost = creatureCardList3.filter(convertedManaCostFilter);
            final CardList creature4ConvertedManaCost = creatureCardList4.filter(convertedManaCostFilter);
            final CardList creature5ConvertedManaCost = creatureCardList5.filter(convertedManaCostFilter);

            final CardList spell1ConvertedManaCost = spellCardList1.filter(convertedManaCostFilter);
            final CardList spell2ConvertedManaCost = spellCardList2.filter(convertedManaCostFilter);
            final CardList spell3ConvertedManaCost = spellCardList3.filter(convertedManaCostFilter);
            final CardList spell4ConvertedManaCost = spellCardList4.filter(convertedManaCostFilter);
            final CardList spell5ConvertedManaCost = spellCardList5.filter(convertedManaCostFilter);

            for (int j = 0; j < i; j++) {
                Card c = creature1ConvertedManaCost.get(this.r.nextInt(creature1ConvertedManaCost.size()));
                creatures12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = creature2ConvertedManaCost.get(this.r.nextInt(creature2ConvertedManaCost.size()));
                creatures12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = creature3ConvertedManaCost.get(this.r.nextInt(creature3ConvertedManaCost.size()));
                creatures12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = creature4ConvertedManaCost.get(this.r.nextInt(creature4ConvertedManaCost.size()));
                creatures12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = creature5ConvertedManaCost.get(this.r.nextInt(creature5ConvertedManaCost.size()));
                creatures12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = spell1ConvertedManaCost.get(this.r.nextInt(spell1ConvertedManaCost.size()));
                spells12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = spell2ConvertedManaCost.get(this.r.nextInt(spell2ConvertedManaCost.size()));
                spells12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = spell3ConvertedManaCost.get(this.r.nextInt(spell3ConvertedManaCost.size()));
                spells12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = spell4ConvertedManaCost.get(this.r.nextInt(spell4ConvertedManaCost.size()));
                spells12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = spell5ConvertedManaCost.get(this.r.nextInt(spell5ConvertedManaCost.size()));
                spells12345.add(c);
                this.cardCounts.put(c.getName(), 0);
            }

            minConvertedManaCost[0] += 2;
            maxConvertedManaCost[0] += 2;
            // resulting mana curve of the card pool
            // 30x 0 - 2
            // 20x 3 - 5
            // 10x 6 - 20
            // =60x - card pool
        }

        // shuffle card pools
        creatures12345.shuffle();
        spells12345.shuffle();

        // calculate card counts
        float p = (float) (creaturePercentage * .01);
        final int creatureCount = (int) (p * deckSize);
        tmpDeckErrorMessage += "Creature Count:" + creatureCount + "\n";

        p = (float) (spellPercentage * .01);
        final int spellCount = (int) (p * deckSize);
        tmpDeckErrorMessage += "Spell Count:" + spellCount + "\n";

        // build deck from the card pools
        for (int i = 0; i < creatureCount; i++) {
            Card c = creatures12345.get(this.r.nextInt(creatures12345.size()));

            loopCounter = 0;
            while ((this.cardCounts.get(c.getName()) > (this.maxDuplicates - 1)) || (loopCounter > 100)) {
                c = creatures12345.get(this.r.nextInt(creatures12345.size()));
                loopCounter++;
            }
            if (loopCounter > 100) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- Cr123");
            }

            tempDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeckErrorMessage += c.getName() + " " + c.getManaCost() + "\n";
        }

        for (int i = 0; i < spellCount; i++) {
            Card c = spells12345.get(this.r.nextInt(spells12345.size()));

            loopCounter = 0;
            while ((this.cardCounts.get(c.getName()) > (this.maxDuplicates - 1)) || (loopCounter > 100)) {
                c = spells12345.get(this.r.nextInt(spells12345.size()));
                loopCounter++;
            }
            if (loopCounter > 100) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- Sp123");
            }

            tempDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeckErrorMessage += c.getName() + " " + c.getManaCost() + "\n";
        }

        // Add lands
        int numberOfLands = 0;
        if (landsPercentage > 0) {
            p = (float) (landsPercentage * .01);
            numberOfLands = (int) (p * deckSize);
        }
        /*
         * else { // otherwise, just fill in the rest of the deck with basic //
         * lands numLands = size - tDeck.size(); }
         */

        tmpDeckErrorMessage += "numLands:" + numberOfLands + "\n";

        final int numberOfDualLands = (numberOfLands / 4);
        for (int i = 0; i < numberOfDualLands; i++) {
            String s = this.dualLands.get(this.r.nextInt(this.dualLands.size()));

            loopCounter = 0;
            while ((this.cardCounts.get(s) > 3) || (loopCounter > 20)) {
                s = this.dualLands.get(this.r.nextInt(this.dualLands.size()));
                loopCounter++;
            }
            if (loopCounter > 20) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- DL");
            }

            tempDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer()));
            final int n = this.cardCounts.get(s);
            this.cardCounts.put(s, n + 1);
            tmpDeckErrorMessage += s + "\n";
        }

        numberOfLands -= numberOfDualLands;

        if (numberOfLands > 0) {
            // attempt to optimize basic land counts according to
            // color representation
            final ColorCount[] colorCounts = { new ColorCount("Plains", 0), new ColorCount("Island", 0), new ColorCount("Swamp", 0),
                    new ColorCount("Mountain", 0), new ColorCount("Forest", 0) };

            // count each card color using mana costs
            // TODO: count hybrid mana differently?
            for (int i = 0; i < tempDeck.size(); i++) {
                final String mc = tempDeck.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    final char c = mc.charAt(j);

                    if (c == 'W') {
                        colorCounts[0].setCount(colorCounts[0].getCount() + 1);
                    } else if (c == 'U') {
                        colorCounts[1].setCount(colorCounts[1].getCount() + 1);
                    } else if (c == 'B') {
                        colorCounts[2].setCount(colorCounts[2].getCount() + 1);
                    } else if (c == 'R') {
                        colorCounts[3].setCount(colorCounts[3].getCount() + 1);
                    } else if (c == 'G') {
                        colorCounts[4].setCount(colorCounts[4].getCount() + 1);
                    }
                }
            }

            // total of all ColorCounts
            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += colorCounts[i].getCount();
                tmpDeckErrorMessage += colorCounts[i].getColor() + ":" + colorCounts[i].getCount() + "\n";
            }

            tmpDeckErrorMessage += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (colorCounts[i].getCount() > 0) { // calculate number of lands
                                                 // for each color
                    p = (float) colorCounts[i].getCount() / (float) totalColor;
                    final int nLand = (int) (numberOfLands * p);
                    tmpDeckErrorMessage += "nLand-" + colorCounts[i].getColor() + ":" + nLand + "\n";

                    // just to prevent a null exception by the deck size fixing
                    // code
                    this.cardCounts.put(colorCounts[i].getColor(), nLand);

                    for (int j = 0; j <= nLand; j++) {
                        tempDeck.add(AllZone.getCardFactory().getCard(colorCounts[i].getColor(), AllZone.getComputerPlayer()));
                    }
                }
            }
        }
        tmpDeckErrorMessage += "DeckSize:" + tempDeck.size() + "\n";

        // fix under-sized or over-sized decks, due to integer arithmetic
        if (tempDeck.size() < deckSize) {
            final int diff = deckSize - tempDeck.size();

            for (int i = 0; i < diff; i++) {
                Card c = tempDeck.get(this.r.nextInt(tempDeck.size()));

                loopCounter = 0;
                while ((this.cardCounts.get(c.getName()) > 3) || (loopCounter > deckSize)) {
                    c = tempDeck.get(this.r.nextInt(tempDeck.size()));
                    loopCounter++;
                }
                if (loopCounter > deckSize) {
                    throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- undersize");
                }

                final int n = this.cardCounts.get(c.getName());
                tempDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
                this.cardCounts.put(c.getName(), n + 1);
                tmpDeckErrorMessage += "Added:" + c.getName() + "\n";
            }
        } else if (tempDeck.size() > deckSize) {
            final int diff = tempDeck.size() - deckSize;

            for (int i = 0; i < diff; i++) {
                Card c = tempDeck.get(this.r.nextInt(tempDeck.size()));

                while (c.isBasicLand()) { // don't remove basic lands
                    c = tempDeck.get(this.r.nextInt(tempDeck.size()));
                }

                tempDeck.remove(c);
                tmpDeckErrorMessage += "Removed:" + c.getName() + "\n";
            }
        }

        tmpDeckErrorMessage += "DeckSize:" + tempDeck.size() + "\n";
        if (ForgeProps.getProperty("showdeck/5color", "false").equals("true")) {
            ErrorViewer.showError(tmpDeckErrorMessage);
        }

        return tempDeck;
    }

    private class ColorCount {
        private final String color;
        private int count;

        public ColorCount(final String color, final int count) {
            this.color = color;
            this.count = count;
        }

        /**
         * 
         * @return
         */
        public String getColor() {
            return this.color;
        }

        /**
         * 
         * @return
         */
        public int getCount() {
            return this.count;
        }

        /**
         * 
         * @param color
         */
        public void setCount(final int count) {
            this.count = count;
        }
    }
}
