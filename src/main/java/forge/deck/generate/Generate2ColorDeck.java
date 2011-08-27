package forge.deck.generate;

import forge.*;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>Generate2ColorDeck class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Generate2ColorDeck {
    private String color1 = "";
    private String color2 = "";
    private Random r = null;
    private Map<String, String> ClrMap = null;
    private ArrayList<String> notColors = null;
    private ArrayList<DLnd> DualLands = null;
    private ArrayList<String> DL = null;
    private Map<String, Integer> CardCounts = null;

    /**
     * <p>Constructor for Generate2ColorDeck.</p>
     *
     * @param Clr1 a {@link java.lang.String} object.
     * @param Clr2 a {@link java.lang.String} object.
     */
    public Generate2ColorDeck(String Clr1, String Clr2) {
        r = MyRandom.random;

        CardCounts = new HashMap<String, Integer>();

        ClrMap = new HashMap<String, String>();
        ClrMap.put("white", "W");
        ClrMap.put("blue", "U");
        ClrMap.put("black", "B");
        ClrMap.put("red", "R");
        ClrMap.put("green", "G");

        notColors = new ArrayList<String>();
        notColors.add("white");
        notColors.add("blue");
        notColors.add("black");
        notColors.add("red");
        notColors.add("green");

        DualLands = new ArrayList<DLnd>();
        DualLands.add(new DLnd("Tundra", "WU"));
        DualLands.add(new DLnd("Hallowed Fountain", "WU"));
        DualLands.add(new DLnd("Underground Sea", "UB"));
        DualLands.add(new DLnd("Watery Grave", "UB"));
        DualLands.add(new DLnd("Badlands", "BR"));
        DualLands.add(new DLnd("Blood Crypt", "BR"));
        DualLands.add(new DLnd("Taiga", "RG"));
        DualLands.add(new DLnd("Stomping Ground", "RG"));
        DualLands.add(new DLnd("Savannah", "GW"));
        DualLands.add(new DLnd("Temple Garden", "GW"));
        DualLands.add(new DLnd("Scrubland", "WB"));
        DualLands.add(new DLnd("Godless Shrine", "WB"));
        DualLands.add(new DLnd("Volcanic Island", "UR"));
        DualLands.add(new DLnd("Steam Vents", "UR"));
        DualLands.add(new DLnd("Bayou", "BG"));
        DualLands.add(new DLnd("Overgrown Tomb", "BG"));
        DualLands.add(new DLnd("Plateau", "RW"));
        DualLands.add(new DLnd("Sacred Foundry", "RW"));
        DualLands.add(new DLnd("Tropical Island", "GU"));
        DualLands.add(new DLnd("Breeding Pool", "GU"));

        if (Clr1.equals("AI")) {
            // choose first color
            color1 = notColors.get(r.nextInt(5));

            // choose second color
            String c2 = notColors.get(r.nextInt(5));
            while (c2.equals(color1))
                c2 = notColors.get(r.nextInt(5));
            color2 = c2;
        } else {
            color1 = Clr1;
            color2 = Clr2;
        }

        notColors.remove(color1);
        notColors.remove(color2);

        DL = new ArrayList<String>();
        for (int i = 0; i < DualLands.size(); i++) {
            DLnd d = DualLands.get(i);
            if (d.Mana.contains(ClrMap.get(color1)) && d.Mana.contains(ClrMap.get(color2))) {
                DL.add(d.Name);
                CardCounts.put(d.Name, 0);
            }
        }

    }

    /**
     * <p>get2ColorDeck.</p>
     *
     * @param Size a int.
     * @return a {@link forge.CardList} object.
     */
    public CardList get2ColorDeck(int Size) {
        int lc = 0; // loop counter to prevent infinite card selection loops
        String tmpDeck = "";
        CardList tDeck = new CardList();

        int LandsPercentage = 42;
        int CreatPercentage = 34;
        int SpellPercentage = 24;

        // start with all cards
        // remove cards that generated decks don't like
        CardList AllCards = CardFilter.filter(AllZone.getCardFactory(), new CardListFilter() {
            public boolean addCard(Card c) {
                return !(c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True"));
            }
        });

        // reduce to cards that match the colors
        CardList CL1 = AllCards.getColor(color1);
        CL1.addAll(AllCards.getColor(Constant.Color.Colorless));
        CardList CL2 = AllCards.getColor(color2);

        // remove multicolor cards that don't match the colors
        CardListFilter clrF = new CardListFilter() {
            public boolean addCard(Card c) {
                for (int i = 0; i < notColors.size(); i++) {
                    if (c.getManaCost().contains(ClrMap.get(notColors.get(i)))) {
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

        String ISE[] = {"Instant", "Sorcery", "Enchantment", "Planeswalker", "Artifact"};
        CardList Sp1 = CL1.getValidCards(ISE, null, null);
        CardList Sp2 = CL2.getValidCards(ISE, null, null);

        // final card pools
        CardList Cr12 = new CardList();
        CardList Sp12 = new CardList();

        // used for mana curve in the card pool
        final int MinCMC[] = {1}, MaxCMC[] = {2};
        CardListFilter cmcF = new CardListFilter() {
            public boolean addCard(Card c) {
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
                CardCounts.put(c.getName(), 0);

                c = Cr2CMC.get(r.nextInt(Cr2CMC.size()));
                Cr12.add(c);
                CardCounts.put(c.getName(), 0);

                c = Sp1CMC.get(r.nextInt(Sp1CMC.size()));
                Sp12.add(c);
                CardCounts.put(c.getName(), 0);

                c = Sp2CMC.get(r.nextInt(Sp2CMC.size()));
                Sp12.add(c);
                CardCounts.put(c.getName(), 0);
            }

            MinCMC[0] += 2;
            MaxCMC[0] += 2;
            // resulting mana curve of the card pool
            //16x 1 - 2
            //12x 3 - 4
            //8x 5 - 6
            //4x 7 - 8
            //=40x - card pool could support up to a 275 card deck (all 4-ofs plus basic lands)
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
            while (CardCounts.get(c.getName()) > 3 || lc > 100) {
                c = Cr12.get(r.nextInt(Cr12.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- Cr12");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            int n = CardCounts.get(c.getName());
            CardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        for (int i = 0; i < SpellCnt; i++) {
            Card c = Sp12.get(r.nextInt(Sp12.size()));

            lc = 0;
            while (CardCounts.get(c.getName()) > 3 || lc > 100) {
                c = Sp12.get(r.nextInt(Sp12.size()));
                lc++;
            }
            if (lc > 100) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- Sp12");
            }

            tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
            int n = CardCounts.get(c.getName());
            CardCounts.put(c.getName(), n + 1);
            tmpDeck += c.getName() + " " + c.getManaCost() + "\n";
        }

        // Add lands
        int numLands = 0;
        if (LandsPercentage > 0) {
            p = (float) ((float) LandsPercentage * .01);
            numLands = (int) (p * (float) Size);
        } else  {   // otherwise, just fill in the rest of the deck with basic lands
            numLands = Size - tDeck.size();
        }

        tmpDeck += "numLands:" + numLands + "\n";

        int nDLands = (numLands / 6);
        for (int i = 0; i < nDLands; i++) {
            String s = DL.get(r.nextInt(DL.size()));

            lc = 0;
            while (CardCounts.get(s) > 3 || lc > 20) {
                s = DL.get(r.nextInt(DL.size()));
                lc++;
            }
            if (lc > 20) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- DL");
            }

            tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getHumanPlayer()));
            int n = CardCounts.get(s);
            CardCounts.put(s, n + 1);
            tmpDeck += s + "\n";
        }

        numLands -= nDLands;

        if (numLands > 0)    // attempt to optimize basic land counts according to color representation
        {
            CCnt ClrCnts[] = {new CCnt("Plains", 0),
                    new CCnt("Island", 0),
                    new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0),
                    new CCnt("Forest", 0)};

            // count each card color using mana costs
            // TODO: count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                String mc = tDeck.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    char c = mc.charAt(j);

                    if (c == 'W') {
                        ClrCnts[0].Count++;
                    }
                    else if (c == 'U') {
                        ClrCnts[1].Count++;
                    }
                    else if (c == 'B') {
                        ClrCnts[2].Count++;
                    }
                    else if (c == 'R') {
                        ClrCnts[3].Count++;
                    }
                    else if (c == 'G') {
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
                if (ClrCnts[i].Count > 0) {    // calculate number of lands for each color
                    p = (float) ClrCnts[i].Count / (float) totalColor;
                    int nLand = (int) ((float) numLands * p);
                    tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand + "\n";

                    // just to prevent a null exception by the deck size fixing code
                    CardCounts.put(ClrCnts[i].Color, nLand);

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
                while (CardCounts.get(c.getName()) > 3 || lc > Size) {
                    c = tDeck.get(r.nextInt(tDeck.size()));
                    lc++;
                }
                if (lc > Size) {
                    throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- undersize");
                }

                int n = CardCounts.get(c.getName());
                tDeck.add(AllZone.getCardFactory().getCard(c.getName(), AllZone.getComputerPlayer()));
                CardCounts.put(c.getName(), n + 1);
                tmpDeck += "Added:" + c.getName() + "\n";
            }
        } else if (tDeck.size() > Size) {
            int diff = tDeck.size() - Size;

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(r.nextInt(tDeck.size()));

                while (c.isBasicLand()) {   // don't remove basic lands
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

        public CCnt(String clr, int cnt) {
            Color = clr;
            Count = cnt;
        }
    }

    private class DLnd {
        public String Name;
        public String Mana;

        public DLnd(String nm, String mn) {
            Name = nm;
            Mana = mn;
        }
    }
}
