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
import forge.CardFilter;
import forge.CardList;
import forge.CardListFilter;
import forge.Constant;
import forge.MyRandom;
import forge.PlayerType;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;

/**
 * <p>
 * Generate3ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate3ColorDeck {
    private String color1 = "";
    private String color2 = "";
    private String color3 = "";
    private Random r = null;
    private Map<String, String> crMap = null;
    private ArrayList<String> notColors = null;
    private ArrayList<String> dL = null;
    private Map<String, Integer> cardCounts = null;
    private int maxDuplicates = 4;

    /**
     * <p>
     * Constructor for Generate3ColorDeck.
     * </p>
     * 
     * @param clr1
     *            a {@link java.lang.String} object.
     * @param clr2
     *            a {@link java.lang.String} object.
     * @param clr3
     *            a {@link java.lang.String} object.
     */
    public Generate3ColorDeck(final String clr1, final String clr2, final String clr3) {
        this.r = MyRandom.getRandom();

        this.cardCounts = new HashMap<String, Integer>();

        this.crMap = new HashMap<String, String>();
        this.crMap.put("white", "W");
        this.crMap.put("blue", "U");
        this.crMap.put("black", "B");
        this.crMap.put("red", "R");
        this.crMap.put("green", "G");

        this.notColors = new ArrayList<String>();
        this.notColors.add("white");
        this.notColors.add("blue");
        this.notColors.add("black");
        this.notColors.add("red");
        this.notColors.add("green");

        if (Singletons.getModel().getPreferences().isDeckGenSingletons()) {
            this.maxDuplicates = 1;
        }

        if (clr1.equals("AI")) {
            // choose first color
            this.color1 = this.notColors.get(this.r.nextInt(5));

            // choose second color
            String c2 = this.notColors.get(this.r.nextInt(5));
            while (c2.equals(this.color1)) {
                c2 = this.notColors.get(this.r.nextInt(5));
            }
            this.color2 = c2;

            String c3 = this.notColors.get(this.r.nextInt(5));
            while (c3.equals(this.color1) || c3.equals(this.color2)) {
                c3 = this.notColors.get(this.r.nextInt(5));
            }
            this.color3 = c3;
        } else {
            this.color1 = clr1;
            this.color2 = clr2;
            this.color3 = clr3;
        }

        this.notColors.remove(this.color1);
        this.notColors.remove(this.color2);
        this.notColors.remove(this.color3);

        this.dL = GenerateDeckUtil.getDualLandList(this.crMap.get(this.color1) + this.crMap.get(this.color2)
                + this.crMap.get(this.color3));

        for (int i = 0; i < this.dL.size(); i++) {
            this.cardCounts.put(this.dL.get(i), 0);
        }

    }

    /**
     * <p>
     * get3ColorDeck.
     * </p>
     * 
     * @param size
     *            a int.
     * @param pt
     *            the pt
     * @return a {@link forge.CardList} object.
     */
    public final CardList get3ColorDeck(final int size, final PlayerType pt) {
        int lc = 0; // loop counter to prevent infinite card selection loops
        String tmpDeck = "";
        final CardList tDeck = new CardList();

        final int landsPercentage = 44;
        final int creatPercentage = 34;
        final int spellPercentage = 22;

        // start with all cards
        // remove cards that generated decks don't like
        final CardList allCards = CardFilter.filter(AllZone.getCardFactory(), new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.getSVar("RemRandomDeck").equals("True")) {
                    return false;
                }
                return (!c.getSVar("RemAIDeck").equals("True") || ((pt != null) && pt.equals(PlayerType.HUMAN)));
            }
        });

        // reduce to cards that match the colors
        CardList cl1 = allCards.getColor(this.color1);
        if (!Singletons.getModel().getPreferences().isDeckGenRmvArtifacts()) {
            cl1.addAll(allCards.getColor(Constant.Color.COLORLESS));
        }
        CardList cl2 = allCards.getColor(this.color2);
        CardList cl3 = allCards.getColor(this.color3);

        // remove multicolor cards that don't match the colors
        final CardListFilter clrF = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                for (int i = 0; i < Generate3ColorDeck.this.notColors.size(); i++) {
                    if (c.getManaCost().contains(
                            Generate3ColorDeck.this.crMap.get(Generate3ColorDeck.this.notColors.get(i)))) {
                        return false;
                    }
                }
                return true;
            }
        };
        cl1 = cl1.filter(clrF);
        cl2 = cl2.filter(clrF);
        cl3 = cl3.filter(clrF);

        // build subsets based on type
        final CardList cr1 = cl1.getType("Creature");
        final CardList cr2 = cl2.getType("Creature");
        final CardList cr3 = cl3.getType("Creature");

        final String[] ise = { "Instant", "Sorcery", "Enchantment", "Planeswalker", "Artifact.nonCreature" };
        final CardList sp1 = cl1.getValidCards(ise, null, null);
        final CardList sp2 = cl2.getValidCards(ise, null, null);
        final CardList sp3 = cl3.getValidCards(ise, null, null);

        // final card pools
        final CardList cr123 = new CardList();
        final CardList sp123 = new CardList();

        // used for mana curve in the card pool
        final int[] minCMC = { 0 };
        final int[] maxCMC = { 2 };
        final CardListFilter cmcF = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                final int cCMC = c.getCMC();
                return (cCMC >= minCMC[0]) && (cCMC <= maxCMC[0]);
            }
        };

        // select cards to build card pools using a mana curve
        for (int i = 3; i > 0; i--) {
            if (i==1) {
                maxCMC[0] = 20; //the last category is open ended
                i = 0; // this reduces the number of cards in the last category to 6
            }
            final CardList cr1CMC = cr1.filter(cmcF);
            final CardList cr2CMC = cr2.filter(cmcF);
            final CardList cr3CMC = cr3.filter(cmcF);

            final CardList sp1CMC = sp1.filter(cmcF);
            final CardList sp2CMC = sp2.filter(cmcF);
            final CardList sp3CMC = sp3.filter(cmcF);

            for (int j = 0; j < i+1; j++) {
                Card c = cr1CMC.get(this.r.nextInt(cr1CMC.size()));
                cr123.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr2CMC.get(this.r.nextInt(cr2CMC.size()));
                cr123.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr3CMC.get(this.r.nextInt(cr3CMC.size()));
                cr123.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp1CMC.get(this.r.nextInt(sp1CMC.size()));
                sp123.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp2CMC.get(this.r.nextInt(sp2CMC.size()));
                sp123.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp3CMC.get(this.r.nextInt(sp3CMC.size()));
                sp123.add(c);
                this.cardCounts.put(c.getName(), 0);
            }

            minCMC[0] += 3;
            maxCMC[0] += 3;
            // resulting mana curve of the card pool
            // 24x 0 - 2
            // 18x 3 - 5
            // 6x  6 - 20
            // =58x - card pool
        }

        // shuffle card pools
        cr123.shuffle();
        sp123.shuffle();

        // calculate card counts
        float p = (float) (creatPercentage * .01);
        final int creatCnt = (int) (p * size);
        tmpDeck += "Creature Count:" + creatCnt + "\n";

        p = (float) (spellPercentage * .01);
        final int spellCnt = (int) (p * size);
        tmpDeck += "Spell Count:" + spellCnt + "\n";

        // build deck from the card pools
        for (int i = 0; i < creatCnt; i++) {
            Card c = cr123.get(this.r.nextInt(cr123.size()));

            lc = 0;
            while ((this.cardCounts.get(c.getName()) > (this.maxDuplicates - 1)) || (lc > 100)) {
                c = cr123.get(this.r.nextInt(cr123.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate3ColorDeck : get3ColorDeck -- looped too much -- Cr123");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        for (int i = 0; i < spellCnt; i++) {
            Card c = sp123.get(this.r.nextInt(sp123.size()));

            lc = 0;
            while ((this.cardCounts.get(c.getName()) > (this.maxDuplicates - 1)) || (lc > 100)) {
                c = sp123.get(this.r.nextInt(sp123.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate3ColorDeck : get3ColorDeck -- looped too much -- Sp123");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        // Add lands
        int numLands = 0;
        if (landsPercentage > 0) {
            p = (float) (landsPercentage * .01);
            numLands = (int) (p * size);
        }
        /*
         * else { // otherwise, just fill in the rest of the deck with basic
         * lands numLands = size - tDeck.size(); }
         */

        tmpDeck += "numLands:" + numLands + "\n";

        final int ndLands = (numLands / 4);
        for (int i = 0; i < ndLands; i++) {
            String s = this.dL.get(this.r.nextInt(this.dL.size()));

            lc = 0;
            while ((this.cardCounts.get(s) > 3) || (lc > 20)) {
                s = this.dL.get(this.r.nextInt(this.dL.size()));
                lc++;
            }
            if (lc > 20) {
                throw new RuntimeException("Generate3ColorDeck : get3ColorDeck -- looped too much -- dL");
            }

            tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer()));
            final int n = this.cardCounts.get(s);
            this.cardCounts.put(s, n + 1);
            tmpDeck += s + "\n";
        }

        numLands -= ndLands;

        if (numLands > 0) {
            // attempt to optimize basic land counts according to
            // color representation
            final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0), new CCnt("Forest", 0) };

            // count each card color using mana costs
            // TODO count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                final String mc = tDeck.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    final char c = mc.charAt(j);

                    if (c == 'W') {
                        clrCnts[0].count++;
                    } else if (c == 'U') {
                        clrCnts[1].count++;
                    } else if (c == 'B') {
                        clrCnts[2].count++;
                    } else if (c == 'R') {
                        clrCnts[3].count++;
                    } else if (c == 'G') {
                        clrCnts[4].count++;
                    }
                }
            }

            // total of all ClrCnts
            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += clrCnts[i].count;
                tmpDeck += clrCnts[i].color + ":" + clrCnts[i].count + "\n";
            }

            tmpDeck += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (clrCnts[i].count > 0) { // calculate number of lands for
                                            // each color
                    p = (float) clrCnts[i].count / (float) totalColor;
                    final int nLand = (int) (numLands * p);
                    tmpDeck += "nLand-" + clrCnts[i].color + ":" + nLand + "\n";

                    // just to prevent a null exception by the deck size fixing
                    // code
                    this.cardCounts.put(clrCnts[i].color, nLand);

                    for (int j = 0; j <= nLand; j++) {
                        tDeck.add(AllZone.getCardFactory().getCard(clrCnts[i].color, AllZone.getComputerPlayer()));
                    }
                }
            }
        }
        tmpDeck += "DeckSize:" + tDeck.size() + "\n";

        // fix under-sized or over-sized decks, due to integer arithmetic
        if (tDeck.size() < size) {
            final int diff = size - tDeck.size();

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(this.r.nextInt(tDeck.size()));

                lc = 0;
                while ((this.cardCounts.get(c.getName()) > 3) || (lc > size)) {
                    c = tDeck.get(this.r.nextInt(tDeck.size()));
                    lc++;
                }
                if (lc > size) {
                    throw new RuntimeException("Generate3ColorDeck : get3ColorDeck -- looped too much -- undersize");
                }

                final int n = this.cardCounts.get(c.getName());
                tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
                this.cardCounts.put(c.getName(), n + 1);
                tmpDeck += "Added:" + c.getName() + "\n";
            }
        } else if (tDeck.size() > size) {
            final int diff = tDeck.size() - size;

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(this.r.nextInt(tDeck.size()));

                while (c.isBasicLand()) {
                    // don't remove basic lands
                    c = tDeck.get(this.r.nextInt(tDeck.size()));
                }

                tDeck.remove(c);
                tmpDeck += "Removed:" + c.getName() + "\n";
            }
        }

        tmpDeck += "DeckSize:" + tDeck.size() + "\n";
        if (ForgeProps.getProperty("showdeck/3color", "false").equals("true")) {
            ErrorViewer.showError(tmpDeck);
        }

        return tDeck;
    }

    private class CCnt {
        private final String color;
        private int count;

        public CCnt(final String clr, final int cnt) {
            this.color = clr;
            this.count = cnt;
        }
    }
}
