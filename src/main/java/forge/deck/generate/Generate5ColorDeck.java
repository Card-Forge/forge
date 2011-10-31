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
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;

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
    private Map<String, String> clrMap = null;
    private ArrayList<String> notColors = null;
    private ArrayList<String> dl = null;
    private Map<String, Integer> cardCounts = null;

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
     * @param clr1
     *            a {@link java.lang.String} object.
     * @param clr2
     *            a {@link java.lang.String} object.
     * @param clr3
     *            a {@link java.lang.String} object.
     * @param clr4
     *            a {@link java.lang.String} object.
     * @param clr5
     *            a {@link java.lang.String} object.
     */
    public Generate5ColorDeck(final String clr1, final String clr2, final String clr3, final String clr4,
            final String clr5) {
        this.r = MyRandom.getRandom();

        this.cardCounts = new HashMap<String, Integer>();

        this.clrMap = new HashMap<String, String>();
        this.clrMap.put("white", "W");
        this.clrMap.put("blue", "U");
        this.clrMap.put("black", "B");
        this.clrMap.put("red", "R");
        this.clrMap.put("green", "G");

        this.notColors = new ArrayList<String>();
        this.notColors.add("white");
        this.notColors.add("blue");
        this.notColors.add("black");
        this.notColors.add("red");
        this.notColors.add("green");

        this.color1 = clr1;
        this.color2 = clr2;
        this.color3 = clr3;
        this.color4 = clr4;
        this.color5 = clr5;

        this.notColors.remove(this.color1);
        this.notColors.remove(this.color2);
        this.notColors.remove(this.color3);
        this.notColors.remove(this.color4);
        this.notColors.remove(this.color5);

        this.dl = GenerateDeckUtil.getDualLandList("WUBRG");

        for (int i = 0; i < this.dl.size(); i++) {
            this.cardCounts.put(this.dl.get(i), 0);
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
     *            a PlayerType
     * @return a {@link forge.CardList} object.
     */
    public final CardList get5ColorDeck(final int size, final PlayerType pt) {
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
        CardList cL1 = allCards.getColor(this.color1);
        cL1.addAll(allCards.getColor(Constant.Color.COLORLESS));
        CardList cL2 = allCards.getColor(this.color2);
        CardList cL3 = allCards.getColor(this.color3);
        CardList cL4 = allCards.getColor(this.color4);
        CardList cL5 = allCards.getColor(this.color5);

        // remove multicolor cards that don't match the colors
        final CardListFilter clrF = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                for (int i = 0; i < Generate5ColorDeck.this.notColors.size(); i++) {
                    if (c.getManaCost().contains(
                            Generate5ColorDeck.this.clrMap.get(Generate5ColorDeck.this.notColors.get(i)))) {
                        return false;
                    }
                }
                return true;
            }
        };
        cL1 = cL1.filter(clrF);
        cL2 = cL2.filter(clrF);
        cL3 = cL3.filter(clrF);
        cL4 = cL4.filter(clrF);
        cL5 = cL5.filter(clrF);

        // build subsets based on type
        final CardList cr1 = cL1.getType("Creature");
        final CardList cr2 = cL2.getType("Creature");
        final CardList cr3 = cL3.getType("Creature");
        final CardList cr4 = cL4.getType("Creature");
        final CardList cr5 = cL5.getType("Creature");

        final String[] ise = { "Instant", "Sorcery", "Enchantment", "Planeswalker", "Artifact.nonCreature" };
        final CardList sp1 = cL1.getValidCards(ise, null, null);
        final CardList sp2 = cL2.getValidCards(ise, null, null);
        final CardList sp3 = cL3.getValidCards(ise, null, null);
        final CardList sp4 = cL4.getValidCards(ise, null, null);
        final CardList sp5 = cL5.getValidCards(ise, null, null);

        // final card pools
        final CardList cr12345 = new CardList();
        final CardList sp12345 = new CardList();

        // used for mana curve in the card pool
        final int[] minCMC = { 1 };
        final int[] maxCMC = { 3 };
        final CardListFilter cmcF = new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                final int cCMC = c.getCMC();
                return (cCMC >= minCMC[0]) && (cCMC <= maxCMC[0]);
            }
        };

        // select cards to build card pools using a mana curve
        for (int i = 3; i > 0; i--) {
            final CardList cr1CMC = cr1.filter(cmcF);
            final CardList cr2CMC = cr2.filter(cmcF);
            final CardList cr3CMC = cr3.filter(cmcF);
            final CardList cr4CMC = cr4.filter(cmcF);
            final CardList cr5CMC = cr5.filter(cmcF);

            final CardList sp1CMC = sp1.filter(cmcF);
            final CardList sp2CMC = sp2.filter(cmcF);
            final CardList sp3CMC = sp3.filter(cmcF);
            final CardList sp4CMC = sp4.filter(cmcF);
            final CardList sp5CMC = sp5.filter(cmcF);

            for (int j = 0; j < i; j++) {
                Card c = cr1CMC.get(this.r.nextInt(cr1CMC.size()));
                cr12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr2CMC.get(this.r.nextInt(cr2CMC.size()));
                cr12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr3CMC.get(this.r.nextInt(cr3CMC.size()));
                cr12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr4CMC.get(this.r.nextInt(cr4CMC.size()));
                cr12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = cr5CMC.get(this.r.nextInt(cr5CMC.size()));
                cr12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp1CMC.get(this.r.nextInt(sp1CMC.size()));
                sp12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp2CMC.get(this.r.nextInt(sp2CMC.size()));
                sp12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp3CMC.get(this.r.nextInt(sp3CMC.size()));
                sp12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp4CMC.get(this.r.nextInt(sp4CMC.size()));
                sp12345.add(c);
                this.cardCounts.put(c.getName(), 0);

                c = sp5CMC.get(this.r.nextInt(sp5CMC.size()));
                sp12345.add(c);
                this.cardCounts.put(c.getName(), 0);
            }

            minCMC[0] += 2;
            maxCMC[0] += 2;
            // resulting mana curve of the card pool
            // 18x 1 - 3
            // 12x 3 - 5
            // 6x 5 - 7
            // =36x - card pool could support up to a 257 card deck (all 4-ofs
            // plus basic lands)
        }

        // shuffle card pools
        cr12345.shuffle();
        sp12345.shuffle();

        // calculate card counts
        float p = (float) (creatPercentage * .01);
        final int creatCnt = (int) (p * size);
        tmpDeck += "Creature Count:" + creatCnt + "\n";

        p = (float) (spellPercentage * .01);
        final int spellCnt = (int) (p * size);
        tmpDeck += "Spell Count:" + spellCnt + "\n";

        // build deck from the card pools
        for (int i = 0; i < creatCnt; i++) {
            Card c = cr12345.get(this.r.nextInt(cr12345.size()));

            lc = 0;
            while ((this.cardCounts.get(c.getName()) > 3) || (lc > 100)) {
                c = cr12345.get(this.r.nextInt(cr12345.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- Cr123");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        for (int i = 0; i < spellCnt; i++) {
            Card c = sp12345.get(this.r.nextInt(sp12345.size()));

            lc = 0;
            while ((this.cardCounts.get(c.getName()) > 3) || (lc > 100)) {
                c = sp12345.get(this.r.nextInt(sp12345.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- Sp123");
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
        } else { // otherwise, just fill in the rest of the deck with basic
                 // lands
            numLands = size - tDeck.size();
        }

        tmpDeck += "numLands:" + numLands + "\n";

        final int nDLands = (numLands / 4);
        for (int i = 0; i < nDLands; i++) {
            String s = this.dl.get(this.r.nextInt(this.dl.size()));

            lc = 0;
            while ((this.cardCounts.get(s) > 3) || (lc > 20)) {
                s = this.dl.get(this.r.nextInt(this.dl.size()));
                lc++;
            }
            if (lc > 20) {
                throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- DL");
            }

            tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer()));
            final int n = this.cardCounts.get(s);
            this.cardCounts.put(s, n + 1);
            tmpDeck += s + "\n";
        }

        numLands -= nDLands;

        if (numLands > 0) // attempt to optimize basic land counts according to
                          // color representation
        {
            final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0), new CCnt("Forest", 0) };

            // count each card color using mana costs
            // TODO: count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                final String mc = tDeck.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    final char c = mc.charAt(j);

                    if (c == 'W') {
                        clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                    } else if (c == 'U') {
                        clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                    } else if (c == 'B') {
                        clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                    } else if (c == 'R') {
                        clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                    } else if (c == 'G') {
                        clrCnts[4].setCount(clrCnts[4].getCount() + 1);
                    }
                }
            }

            // total of all ClrCnts
            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += clrCnts[i].getCount();
                tmpDeck += clrCnts[i].getColor() + ":" + clrCnts[i].getCount() + "\n";
            }

            tmpDeck += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (clrCnts[i].getCount() > 0) { // calculate number of lands
                                                 // for each color
                    p = (float) clrCnts[i].getCount() / (float) totalColor;
                    final int nLand = (int) (numLands * p);
                    tmpDeck += "nLand-" + clrCnts[i].getColor() + ":" + nLand + "\n";

                    // just to prevent a null exception by the deck size fixing
                    // code
                    this.cardCounts.put(clrCnts[i].getColor(), nLand);

                    for (int j = 0; j <= nLand; j++) {
                        tDeck.add(AllZone.getCardFactory().getCard(clrCnts[i].getColor(), AllZone.getComputerPlayer()));
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
                    throw new RuntimeException("Generate5ColorDeck : get5ColorDeck -- looped too much -- undersize");
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

                while (c.isBasicLand()) { // don't remove basic lands
                    c = tDeck.get(this.r.nextInt(tDeck.size()));
                }

                tDeck.remove(c);
                tmpDeck += "Removed:" + c.getName() + "\n";
            }
        }

        tmpDeck += "DeckSize:" + tDeck.size() + "\n";
        if (ForgeProps.getProperty("showdeck/5color", "false").equals("true")) {
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
