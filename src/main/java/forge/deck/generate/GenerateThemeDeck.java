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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import forge.error.ErrorViewer;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.FileUtil;
import forge.util.MyRandom;

/**
 * <p>
 * GenerateThemeDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GenerateThemeDeck extends GenerateColoredDeckBase {
    private int basicLandPercentage = 0;
    private boolean testing = false;

    /**
     * <p>
     * Constructor for GenerateThemeDeck.
     * </p>
     */
    public GenerateThemeDeck() {
        this.maxDuplicates = 4;
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
    public final ItemPoolView<CardPrinted> getThemeDeck(final String themeName, final int size) {
        String s = "";

        // read theme file
        final String tFileName = "res/quest/themes/" + themeName + ".thm";
        List<String> lines = FileUtil.readFile(tFileName);

        final List<Grp> groups = readGroups(lines);

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
                while ((cardCounts.get(s) >= g.maxCnt) || (lc > 999)) {
                    // looping
                    // forever
                    s = g.cardnames.get(r.nextInt(cnSize));
                    lc++;
                }
                if (lc > 999) {
                    throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- looped too much -- filename is "
                            + tFileName);
                }

                final int n = cardCounts.get(s);
                tDeck.add(CardDb.instance().getCard(s));
                cardCounts.put(s, n + 1);
                tmpDeck += s + "\n";

            }
        }

        int numBLands = 0;

        if (basicLandPercentage > 0) { // if theme explicitly defines this
            numBLands = (int) (size * basicLandPercentage / 100f);
        } else { // otherwise, just fill in the rest of the deck with basic
                 // lands
            numBLands = size - tDeck.countAll();
        }

        tmpDeck += "numBLands:" + numBLands + "\n";

        addBasicLand(numBLands);

        tmpDeck += "DeckSize:" + tDeck.countAll() + "\n";

        adjustDeckSize(size);

        tmpDeck += "DeckSize:" + tDeck.countAll() + "\n";
        if (testing) {
            ErrorViewer.showError(tmpDeck);
        }

        return tDeck;
    }


    private class Grp {

        /** The Cardnames. */
        private final ArrayList<String> cardnames = new ArrayList<String>();

        /** The Max cnt. */
        private int maxCnt;

        /** The Percentage. */
        private int percentage;
    }

    private List<Grp> readGroups(List<String> lines) {
        final List<Grp> groups = new ArrayList<Grp>();

        Grp g = null;
        for (String s : lines) {
            if (s.equals("End")) {
                break;
            }

            if (s.startsWith("[Group")) {
                g = new Grp();
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
                groups.add(g);

                continue;
            }

            if (s.equals("[/Group]")) {
                g = null;
            }

            if (s.startsWith("BasicLandPercentage")) {
                basicLandPercentage = Integer.parseInt(s.substring("BasicLandPercentage".length() + 1));
            } else if (s.equals("Testing")) {
                testing = true;
            } else if (g != null) {
                g.cardnames.add(s);
                cardCounts.put(s, 0);
            }

        }
        return groups;

    }
}
