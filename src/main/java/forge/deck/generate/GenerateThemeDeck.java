package forge.deck.generate;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.MyRandom;
import forge.error.ErrorViewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>GenerateThemeDeck class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class GenerateThemeDeck {
    private BufferedReader in = null;

    /**
     * <p>Constructor for GenerateThemeDeck.</p>
     */
    public GenerateThemeDeck() {

    }

    /**
     * <p>getThemeNames.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getThemeNames() {
        ArrayList<String> ltNames = new ArrayList<String>();

        File file = new File("res/quest/themes/");

        if (!file.exists()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- file not found -- filename is "
                    + file.getAbsolutePath());
        }

        if (!file.isDirectory()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- not a directory -- "
                    + file.getAbsolutePath());
        }

        String[] fileList = file.list();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].endsWith(".thm")) {
                ltNames.add(fileList[i].substring(0, fileList[i].indexOf(".thm")));
            }
        }

        return ltNames;
    }

    /**
     * <p>getThemeDeck.</p>
     *
     * @param themeName a {@link java.lang.String} object.
     * @param size a int.
     * @return a {@link forge.CardList} object.
     */
    public CardList getThemeDeck(final String themeName, final int size) {
        CardList tDeck = new CardList();

        ArrayList<Grp> groups = new ArrayList<Grp>();

        Map<String, Integer> cardCounts = new HashMap<String, Integer>();

        String s = "";
        int bLandPercentage = 0;
        boolean testing = false;

        // read theme file
        String tFileName = "res/quest/themes/" + themeName + ".thm";
        File tFile = new File(tFileName);
        if (!tFile.exists()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file not found -- filename is "
                    + tFile.getAbsolutePath());
        }

        try {
            in = new BufferedReader(new FileReader(tFile));
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is "
                    + tFile.getPath());
        }

        s = readLine();
        while (!s.equals("End")) {
            if (s.startsWith("[Group")) {
                Grp g = new Grp();

                String[] ss = s.replaceAll("[\\[\\]]", "").split(" ");
                for (int i = 0; i < ss.length; i++) {
                    if (ss[i].startsWith("Percentage")) {
                        String p = ss[i].substring("Percentage".length() + 1);
                        g.Percentage = Integer.parseInt(p);
                    }
                    if (ss[i].startsWith("MaxCnt")) {
                        String m = ss[i].substring("MaxCnt".length() + 1);
                        g.MaxCnt = Integer.parseInt(m);
                    }
                }

                s = readLine();
                while (!s.equals("[/Group]")) {
                    g.Cardnames.add(s);
                    cardCounts.put(s, 0);

                    s = readLine();
                }

                groups.add(g);
            }

            if (s.startsWith("BasicLandPercentage")) {
                bLandPercentage = Integer.parseInt(s.substring("BasicLandPercentage".length() + 1));
            }

            if (s.equals("Testing")) {
                testing = true;
            }

            s = readLine();
        }

        try {
            in.close();
        } catch (IOException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is "
                    + tFile.getPath());
        }

        String tmpDeck = "";

        // begin assigning cards to the deck
        Random r = MyRandom.random;

        for (int i = 0; i < groups.size(); i++) {
            Grp g = groups.get(i);
            float p = (float) ((float) g.Percentage * .01);
            int grpCnt = (int) (p * (float) size);
            int cnSize = g.Cardnames.size();
            tmpDeck += "Group" + i + ":" + grpCnt + "\n";

            for (int j = 0; j < grpCnt; j++) {
                s = g.Cardnames.get(r.nextInt(cnSize));

                int lc = 0;
                while (cardCounts.get(s) >= g.MaxCnt || lc > size) // don't keep looping forever
                {
                    s = g.Cardnames.get(r.nextInt(cnSize));
                    lc++;
                }
                if (lc > size) {
                    throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- looped too much -- filename is "
                            + tFile.getAbsolutePath());
                }

                int n = cardCounts.get(s);
                tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer()));
                cardCounts.put(s, n + 1);
                tmpDeck += s + "\n";

            }
        }

        int numBLands = 0;
        if (bLandPercentage > 0) {    // if theme explicitly defines this
            float p = (float) ((float) bLandPercentage * .01);
            numBLands = (int) (p * (float) size);
        } else { // otherwise, just fill in the rest of the deck with basic lands
            numBLands = size - tDeck.size();
        }

        tmpDeck += "numBLands:" + numBLands + "\n";

        if (numBLands > 0)    // attempt to optimize basic land counts according to color representation
        {
            CCnt[] clrCnts = {new CCnt("Plains", 0),
                    new CCnt("Island", 0),
                    new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0),
                    new CCnt("Forest", 0)};

            // count each instance of a color in mana costs
            // TODO count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                String mc = tDeck.get(i).getManaCost();

                for (int j = 0; j < mc.length(); j++) {
                    char c = mc.charAt(j);

                    if (c == 'W') {
                        clrCnts[0].Count++;
                    } else if (c == 'U') {
                        clrCnts[1].Count++;
                    } else if (c == 'B') {
                        clrCnts[2].Count++;
                    } else if (c == 'R') {
                        clrCnts[3].Count++;
                    } else if (c == 'G') {
                        clrCnts[4].Count++;
                    }
                }
            }

            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += clrCnts[i].Count;
                tmpDeck += clrCnts[i].Color + ":" + clrCnts[i].Count + "\n";
            }

            tmpDeck += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (clrCnts[i].Count > 0) {    // calculate number of lands for each color
                    float p = (float) clrCnts[i].Count / (float) totalColor;
                    int nLand = (int) ((float) numBLands * p);
                    tmpDeck += "numLand-" + clrCnts[i].Color + ":" + nLand + "\n";

                    cardCounts.put(clrCnts[i].Color, 2);
                    for (int j = 0; j < nLand; j++) {
                        tDeck.add(AllZone.getCardFactory().getCard(clrCnts[i].Color, AllZone.getComputerPlayer()));
                    }
                }
            }
        }
        tmpDeck += "DeckSize:" + tDeck.size() + "\n";

        if (tDeck.size() < size) {
            int diff = size - tDeck.size();

            for (int i = 0; i < diff; i++) {
                s = tDeck.get(r.nextInt(tDeck.size())).getName();

                while (cardCounts.get(s) >= 4) {
                    s = tDeck.get(r.nextInt(tDeck.size())).getName();
                }

                int n = cardCounts.get(s);
                tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer()));
                cardCounts.put(s, n + 1);
                tmpDeck += "Added:" + s + "\n";
            }
        } else if (tDeck.size() > size) {
            int diff = tDeck.size() - size;

            for (int i = 0; i < diff; i++) {
                Card c = tDeck.get(r.nextInt(tDeck.size()));

                while (c.isBasicLand()) {
                    c = tDeck.get(r.nextInt(tDeck.size()));
                }

                tDeck.remove(c);
                tmpDeck += "Removed:" + s + "\n";
            }
        }

        tmpDeck += "DeckSize:" + tDeck.size() + "\n";
        if (testing) {
            ErrorViewer.showError(tmpDeck);
        }

        return tDeck;
    }

    /**
     * <p>readLine.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    private String readLine() {
        //makes the checked exception, into an unchecked runtime exception
        try {
            String s = in.readLine();
            if (s != null) {
                s = s.trim();
            }
            return s;
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("GenerateThemeDeck : readLine error");
        }
    } //readLine(Card)

    /**
     * 
     * TODO Write javadoc for this type.
     *
     */
    class CCnt {
        public String Color;
        public int Count;

        public CCnt(String clr, int cnt) {
            Color = clr;
            Count = cnt;
        }
    }

    /**
     * 
     * TODO Write javadoc for this type.
     *
     */
    class Grp {
        public ArrayList<String> Cardnames = new ArrayList<String>();
        public int MaxCnt;
        public int Percentage;
    }
}

