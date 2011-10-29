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
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate2ColorDeck {
    private String color1 = "";
    private String color2 = "";
    private Random r = null;
    private Map<String, String> clrMap = null;
    private ArrayList<String> notColors = null;
    private ArrayList<String> dL = null;
    private Map<String, Integer> cardCounts = null;

    /**
     * <p>
     * Constructor for Generate2ColorDeck.
     * </p>
     * 
     * @param Clr1
     *            a {@link java.lang.String} object.
     * @param Clr2
     *            a {@link java.lang.String} object.
     */
    public Generate2ColorDeck(final String Clr1, final String Clr2) {
        r = MyRandom.random;

        cardCounts = new HashMap<String, Integer>();

        clrMap = new HashMap<String, String>();
        clrMap.put("white", "W");
        clrMap.put("blue", "U");
        clrMap.put("black", "B");
        clrMap.put("red", "R");
        clrMap.put("green", "G");

        notColors = new ArrayList<String>();
        notColors.add("white");
        notColors.add("blue");
        notColors.add("black");
        notColors.add("red");
        notColors.add("green");

        if (Clr1.equals("AI")) {
            // choose first color
            color1 = notColors.get(r.nextInt(5));

            // choose second color
            String c2 = notColors.get(r.nextInt(5));
            while (c2.equals(color1)) {
                c2 = notColors.get(r.nextInt(5));
            }
            color2 = c2;
        } else {
            color1 = Clr1;
            color2 = Clr2;
        }

        notColors.remove(color1);
        notColors.remove(color2);

        dL = GenerateDeckUtil.getDualLandList(clrMap.get(color1) + clrMap.get(color2));

        for (int i = 0; i < dL.size(); i++) {
            cardCounts.put(dL.get(i), 0);
        }

    }

    /**
     * <p>
     * get2ColorDeck.
     * </p>
     * 
     * @param Size
     *            a int.
     * @param pt
     *            the pt
     * @return a {@link forge.CardList} object.
     */
    public final CardList get2ColorDeck(final int Size, final PlayerType pt) {
        int lc = 0; // loop counter to prevent infinite card selection loops
        String tmpDeck = "";
        CardList tDeck = new CardList();

        int LandsPercentage = 42;
        int CreatPercentage = 34;
        int SpellPercentage = 24;

        // start with all cards
        // remove cards that generated decks don't like
        CardList AllCards = CardFilter.filter(AllZone.getCardFactory(), new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.getSVar("RemRandomDeck").equals("True")) {
                    return false;
                }
                return (!c.getSVar("RemAIDeck").equals("True") || (pt != null && pt.equals(PlayerType.HUMAN)));
            }
        });

        // reduce to cards that match the colors
        CardList CL1 = AllCards.getColor(color1);
        CL1.addAll(AllCards.getColor(Constant.Color.COLORLESS));
        CardList CL2 = AllCards.getColor(color2);

        // remove multicolor cards that don't match the colors
        CardListFilter clrF = new CardListFilter() {
            public boolean addCard(final Card c) {
                for (int i = 0; i < notColors.size(); i++) {
                    if (c.getManaCost().contains(clrMap.get(notColors.get(i)))) {
                        return false;
                    }
                }
                return true;
            }
        };
        CL1 = CL1.filter(clrF);
        CL2 = CL2.filter(clrF);

        // build subsets based on type
        CardList Cr1 = CL1.getType("Creature");
        CardList Cr2 = CL2.getType("Creature");

        String[] ISE = { "Instant", "Sorcery", "Enchantment", "Planeswalker", "Artifact.nonCreature" };
        CardList Sp1 = CL1.getValidCards(ISE, null, null);
        CardList Sp2 = CL2.getValidCards(ISE, null, null);

        // final card pools
        CardList Cr12 = new CardList();
        CardList Sp12 = new CardList();

        // used for mana curve in the card pool
        final int MinCMC[] = { 1 }, MaxCMC[] = { 2 };
        CardListFilter cmcF = new CardListFilter() {
            public boolean addCard(final Card c) {
                int cCMC = c.getCMC();
                return (cCMC >= MinCMC[0]) && (cCMC <= MaxCMC[0]);
            }
        };

        // select cards to build card pools using a mana curve
        for (int i = 4; i > 0; i--) {
            CardList Cr1CMC = Cr1.filter(cmcF);
            CardList Cr2CMC = Cr2.filter(cmcF);
            CardList Sp1CMC = Sp1.filter(cmcF);
            CardList Sp2CMC = Sp2.filter(cmcF);

            for (int j = 0; j < i; j++) {
                Card c = Cr1CMC.get(r.nextInt(Cr1CMC.size()));
                Cr12.add(c);
                cardCounts.put(c.getName(), 0);

                c = Cr2CMC.get(r.nextInt(Cr2CMC.size()));
                Cr12.add(c);
                cardCounts.put(c.getName(), 0);

                c = Sp1CMC.get(r.nextInt(Sp1CMC.size()));
                Sp12.add(c);
                cardCounts.put(c.getName(), 0);

                c = Sp2CMC.get(r.nextInt(Sp2CMC.size()));
                Sp12.add(c);
                cardCounts.put(c.getName(), 0);
            }

            MinCMC[0] += 2;
            MaxCMC[0] += 2;
            // resulting mana curve of the card pool
            // 16x 1 - 2
            // 12x 3 - 4
            // 8x 5 - 6
            // 4x 7 - 8
            // =40x - card pool could support up to a 275 card deck (all 4-ofs
            // plus basic lands)
        }

        // shuffle card pools
        Cr12.shuffle();
        Sp12.shuffle();

        // calculate card counts
        float p = (float) ((float) CreatPercentage * .01);
        int CreatCnt = (int) (p * (float) Size);
        tmpDeck += "Creature Count:" + CreatCnt + "\n";

        p = (float) ((float) SpellPercentage * .01);
        int SpellCnt = (int) (p * (float) Size);
        tmpDeck += "Spell Count:" + SpellCnt + "\n";

        // build deck from the card pools
        for (int i = 0; i < CreatCnt; i++) {
            Card c = Cr12.get(r.nextInt(Cr12.size()));

            lc = 0;
            while (cardCounts.get(c.getName()) > 3 || lc > 100) {
                c = Cr12.get(r.nextInt(Cr12.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- Cr12");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            int n = cardCounts.get(c.getName());
            cardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        for (int i = 0; i < SpellCnt; i++) {
            Card c = Sp12.get(r.nextInt(Sp12.size()));

            lc = 0;
            while (cardCounts.get(c.getName()) > 3 || lc > 100) {
                c = Sp12.get(r.nextInt(Sp12.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- Sp12");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            int n = cardCounts.get(c.getName());
            cardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        // Add lands
        int numLands = 0;
        if (LandsPercentage > 0) {
            p = (float) ((float) LandsPercentage * .01);
            numLands = (int) (p * (float) Size);
        } else { // otherwise, just fill in the rest of the deck with basic
                 // lands
            numLands = Size - tDeck.size();
        }

        tmpDeck += "numLands:" + numLands + "\n";

        int nDLands = (numLands / 6);
        for (int i = 0; i < nDLands; i++) {
            String s = dL.get(r.nextInt(dL.size()));

            lc = 0;
            while (cardCounts.get(s) > 3 || lc > 20) {
                s = dL.get(r.nextInt(dL.size()));
                lc++;
            }
            if (lc > 20) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- DL");
            }

            tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer()));
            int n = cardCounts.get(s);
            cardCounts.put(s, n + 1);
            tmpDeck += s + "\n";
        }

        numLands -= nDLands;

        if (numLands > 0) // attempt to optimize basic land counts according to
                          // color representation
        {
            CCnt[] ClrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0), new CCnt("Forest", 0) };

            // count each card color using mana costs
            // TODO count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                String mc = tDeck.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    char c = mc.charAt(j);

                    if (c == 'W') {
                        ClrCnts[0].Count++;
                    } else if (c == 'U') {
                        ClrCnts[1].Count++;
                    } else if (c == 'B') {
                        ClrCnts[2].Count++;
                    } else if (c == 'R') {
                        ClrCnts[3].Count++;
                    } else if (c == 'G') {
                        ClrCnts[4].Count++;
                    }
                }
            }

            // total of all ClrCnts
            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += ClrCnts[i].Count;
                tmpDeck += ClrCnts[i].Color + ":" + ClrCnts[i].Count + "\n";
            }

            tmpDeck += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (ClrCnts[i].Count > 0) { // calculate number of lands for
                                            // each color
                    p = (float) ClrCnts[i].Count / (float) totalColor;
                    int nLand = (int) ((float) numLands * p);
                    tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand + "\n";

                    // just to prevent a null exception by the deck size fixing
                    // code
                    cardCounts.put(ClrCnts[i].Color, nLand);

                    for (int j = 0; j <= nLand; j++) {
                        tDeck.add(AllZone.getCardFactory().getCard(ClrCnts[i].Color, AllZone.getComputerPlayer()));
                    }
                }
            }
        }
        tmpDeck += "DeckSize:" + tDeck.size() + "\n";

        // fix under-sized or over-sized decks, due to integer arithmetic
        if (tDeck.size() < Size) {
            int diff = Size - tDeck.size();

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(r.nextInt(tDeck.size()));

                lc = 0;
                while (cardCounts.get(c.getName()) > 3 || lc > Size) {
                    c = tDeck.get(r.nextInt(tDeck.size()));
                    lc++;
                }
                if (lc > Size) {
                    throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- undersize");
                }

                int n = cardCounts.get(c.getName());
                tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
                cardCounts.put(c.getName(), n + 1);
                tmpDeck += "Added:" + c.getName() + "\n";
            }
        } else if (tDeck.size() > Size) {
            int diff = tDeck.size() - Size;

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(r.nextInt(tDeck.size()));

                while (c.isBasicLand()) { // don't remove basic lands
                    c = tDeck.get(r.nextInt(tDeck.size()));
                }

                tDeck.remove(c);
                tmpDeck += "Removed:" + c.getName() + "\n";
            }
        }

        tmpDeck += "DeckSize:" + tDeck.size() + "\n";
        if (ForgeProps.getProperty("showdeck/2color", "false").equals("true")) {
            ErrorViewer.showError(tmpDeck);
        }

        return tDeck;
    }

    private class CCnt {
        public String Color;
        public int Count;

        public CCnt(final String clr, final int cnt) {
            Color = clr;
            Count = cnt;
        }
    }
}
