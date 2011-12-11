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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.MyRandom;
import forge.error.ErrorViewer;

/**
 * <p>
 * GenerateThemeDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GenerateThemeDeck {
    private BufferedReader in = null;

    /**
     * <p>
     * Constructor for GenerateThemeDeck.
     * </p>
     */
    public GenerateThemeDeck() {

    }

    /**
     * <p>
     * getThemeNames.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public static final ArrayList<String> getThemeNames() {
        final ArrayList<String> ltNames = new ArrayList<String>();

        final File file = new File("res/quest/themes/");

        if (!file.exists()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- file not found -- filename is "
                    + file.getAbsolutePath());
        }

        if (!file.isDirectory()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- not a directory -- "
                    + file.getAbsolutePath());
        }

        final String[] fileList = file.list();
        for (final String element : fileList) {
            if (element.endsWith(".thm")) {
                ltNames.add(element.substring(0, element.indexOf(".thm")));
            }
        }

        return ltNames;
    }

    /**
     * <p>
     * getThemeDeck.
     * </p>
     * 
     * @param themeName
     *            a {@link java.lang.String} object.
     * @param size
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getThemeDeck(final String themeName, final int size) {
        final CardList tDeck = new CardList();

        final ArrayList<Grp> groups = new ArrayList<Grp>();

        final Map<String, Integer> cardCounts = new HashMap<String, Integer>();

        String s = "";
        int bLandPercentage = 0;
        boolean testing = false;

        // read theme file
        final String tFileName = "res/quest/themes/" + themeName + ".thm";
        final File tFile = new File(tFileName);
        if (!tFile.exists()) {
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file not found -- filename is "
                    + tFile.getAbsolutePath());
        }

        try {
            this.in = new BufferedReader(new FileReader(tFile));
        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is "
                    + tFile.getPath());
        }

        s = this.readLine();
        while (!s.equals("End")) {
            if (s.startsWith("[Group")) {
                final Grp g = new Grp();

                final String[] ss = s.replaceAll("[\\[\\]]", "").split(" ");
                for (final String element : ss) {
                    if (element.startsWith("Percentage")) {
                        final String p = element.substring("Percentage".length() + 1);
                        g.percentage = Integer.parseInt(p);
                    }
                    if (element.startsWith("MaxCnt")) {
                        final String m = element.substring("MaxCnt".length() + 1);
                        g.maxCnt = Integer.parseInt(m);
                    }
                }

                s = this.readLine();
                while (!s.equals("[/Group]")) {
                    g.cardnames.add(s);
                    cardCounts.put(s, 0);

                    s = this.readLine();
                }

                groups.add(g);
            }

            if (s.startsWith("BasicLandPercentage")) {
                bLandPercentage = Integer.parseInt(s.substring("BasicLandPercentage".length() + 1));
            }

            if (s.equals("Testing")) {
                testing = true;
            }

            s = this.readLine();
        }

        try {
            this.in.close();
        } catch (final IOException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is "
                    + tFile.getPath());
        }

        String tmpDeck = "";

        // begin assigning cards to the deck
        final Random r = MyRandom.getRandom();

        for (int i = 0; i < groups.size(); i++) {
            final Grp g = groups.get(i);
            final float p = (float) (g.percentage * .01);
            final int grpCnt = (int) (p * size);
            final int cnSize = g.cardnames.size();
            tmpDeck += "Group" + i + ":" + grpCnt + "\n";

            for (int j = 0; j < grpCnt; j++) {
                s = g.cardnames.get(r.nextInt(cnSize));

                int lc = 0;
                while ((cardCounts.get(s) >= g.maxCnt) || (lc > size)) {
                    // looping
                    // forever
                    s = g.cardnames.get(r.nextInt(cnSize));
                    lc++;
                }
                if (lc > size) {
                    throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- looped too much -- filename is "
                            + tFile.getAbsolutePath());
                }

                final int n = cardCounts.get(s);
                tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer()));
                cardCounts.put(s, n + 1);
                tmpDeck += s + "\n";

            }
        }

        int numBLands = 0;
        if (bLandPercentage > 0) { // if theme explicitly defines this
            final float p = (float) (bLandPercentage * .01);
            numBLands = (int) (p * size);
        } else { // otherwise, just fill in the rest of the deck with basic
                 // lands
            numBLands = size - tDeck.size();
        }

        tmpDeck += "numBLands:" + numBLands + "\n";

        if (numBLands > 0) {
            // attempt to optimize basic land counts according to
            // color representation
            final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0), new CCnt("Forest", 0) };

            // count each instance of a color in mana costs
            // TODO count hybrid mana differently?
            for (int i = 0; i < tDeck.size(); i++) {
                final String mc = tDeck.get(i).getManaCost();

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

            int totalColor = 0;
            for (int i = 0; i < 5; i++) {
                totalColor += clrCnts[i].count;
                tmpDeck += clrCnts[i].color + ":" + clrCnts[i].count + "\n";
            }

            tmpDeck += "totalColor:" + totalColor + "\n";

            for (int i = 0; i < 5; i++) {
                if (clrCnts[i].count > 0) { // calculate number of lands for
                                            // each color
                    final float p = (float) clrCnts[i].count / (float) totalColor;
                    final int nLand = (int) (numBLands * p);
                    tmpDeck += "numLand-" + clrCnts[i].color + ":" + nLand + "\n";

                    cardCounts.put(clrCnts[i].color, 2);
                    for (int j = 0; j < nLand; j++) {
                        tDeck.add(AllZone.getCardFactory().getCard(clrCnts[i].color, AllZone.getComputerPlayer()));
                    }
                }
            }
        }
        tmpDeck += "DeckSize:" + tDeck.size() + "\n";

        if (tDeck.size() < size) {
            final int diff = size - tDeck.size();

            for (int i = 0; i < diff; i++) {
                s = tDeck.get(r.nextInt(tDeck.size())).getName();

                while (cardCounts.get(s) >= 4) {
                    s = tDeck.get(r.nextInt(tDeck.size())).getName();
                }

                final int n = cardCounts.get(s);
                tDeck.add(AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer()));
                cardCounts.put(s, n + 1);
                tmpDeck += "Added:" + s + "\n";
            }
        } else if (tDeck.size() > size) {
            final int diff = tDeck.size() - size;

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
     * <p>
     * readLine.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    private String readLine() {
        // makes the checked exception, into an unchecked runtime exception
        try {
            String s = this.in.readLine();
            if (s != null) {
                s = s.trim();
            }
            return s;
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("GenerateThemeDeck : readLine error");
        }
    } // readLine(Card)

    /**
     * 
     * TODO Write javadoc for this type.
     * 
     */
    class CCnt {

        /** The Color. */
        private final String color;

        /** The Count. */
        private int count;

        /**
         * Instantiates a new c cnt.
         * 
         * @param clr
         *            the clr
         * @param cnt
         *            the cnt
         */
        public CCnt(final String clr, final int cnt) {
            this.color = clr;
            this.count = cnt;
        }
    }

    /**
     * 
     * TODO Write javadoc for this type.
     * 
     */
    class Grp {

        /** The Cardnames. */
        private final ArrayList<String> cardnames = new ArrayList<String>();

        /** The Max cnt. */
        private int maxCnt;

        /** The Percentage. */
        private int percentage;
    }
}
