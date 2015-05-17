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
package forge.deck;

import forge.deck.CardPool;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.IDeckGenPool;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.MyRandom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * ThemeDeckGenerator class.
 * </p>
 * 
 * @author Forge
 * @version $Id: ThemeDeckGenerator.java 25022 2014-03-02 14:19:50Z teferi $
 */
public class DeckGeneratorTheme extends DeckGeneratorBase {
    private int basicLandPercentage = 0;
    private String basicLandSet = null;
    private boolean testing = false;

    /**
     * <p>
     * Constructor for ThemeDeckGenerator.
     * </p>
     */
    public DeckGeneratorTheme(IDeckGenPool pool0) {
        super(pool0, DeckFormat.Constructed);
        this.maxDuplicates = 4;
    }

    /**
     * <p>
     * getThemeNames.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public static final List<String> getThemeNames() {
        final List<String> ltNames = new ArrayList<String>();

        final File file = new File(ForgeConstants.THEMES_DIR);

        if (!file.exists()) {
            throw new RuntimeException("ThemeDeckGenerator : getThemeNames error -- file not found -- filename is "
                    + file.getAbsolutePath());
        }

        if (!file.isDirectory()) {
            throw new RuntimeException("ThemeDeckGenerator : getThemeNames error -- not a directory -- "
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
    public final CardPool getThemeDeck(final String themeName, final int size, final StringBuilder errorBuilder) {
        String s = "";
        String[] ss;

        // read theme file
        final String tFileName = ForgeConstants.THEMES_DIR + "/" + themeName + ".thm";
        List<String> lines = FileUtil.readFile(tFileName);

        final List<Grp> groups = readGroups(lines);

        // begin assigning cards to the deck
        final Random r = MyRandom.getRandom();

        for (int i = 0; i < groups.size(); i++) {
            final Grp g = groups.get(i);
            final float p = (float) (g.percentage * .01);
            final int grpCnt = (int) (p * size);
            final int cnSize = g.cardnames.size();
            errorBuilder.append("Group" + i + ":" + grpCnt + "\n");

            for (int j = 0; j < grpCnt; j++) {
                s = g.cardnames.get(r.nextInt(cnSize));
                ss = s.split("\\|");
                
                int lc = 0;
                while ((cardCounts.get(ss[0]) >= g.maxCnt) || (lc > 999)) {
                    // looping
                    // forever
                    s = g.cardnames.get(r.nextInt(cnSize));
                    ss = s.split("\\|");
                    lc++;
                }
                if (lc > 999) {
                    throw new RuntimeException("ThemeDeckGenerator : getThemeDeck -- looped too much -- filename is "
                            + tFileName);
                }

                final int n = cardCounts.get(ss[0]);
                if (ss.length == 1) {
                	tDeck.add(pool.getCard(ss[0]));
                }
                else {
                	tDeck.add(pool.getCard(ss[0],ss[1]));
                }
                cardCounts.put(ss[0], n + 1);
                errorBuilder.append(s + "\n");
            }
        }

        int numBLands;
        if (basicLandPercentage > 0) { // if theme explicitly defines this
            numBLands = (int) (size * basicLandPercentage / 100f);
        }
        else { // otherwise, just fill in the rest of the deck with basic lands
            numBLands = size - tDeck.countAll();
        }

        errorBuilder.append("numBLands:" + numBLands + "\n");

        addBasicLand(numBLands,basicLandSet);

        errorBuilder.append("DeckSize:" + tDeck.countAll() + "\n");

        adjustDeckSize(size);

        errorBuilder.append("DeckSize:" + tDeck.countAll() + "\n");
        if (!testing) {
            errorBuilder.delete(0, errorBuilder.length()); //clear if not testing
        }

        return tDeck;
    }

    private class Grp {
        /** The Cardnames. */
        private final List<String> cardnames = new ArrayList<String>();

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
            	final String[] ss = s.split("\\|");
                basicLandPercentage = Integer.parseInt(ss[0].substring("BasicLandPercentage".length() + 1));
                if(ss.length > 1)
                	basicLandSet = ss[1];
            }
            else if (s.equals("Testing")) {
                testing = true;
            }
            else if (g != null) {
                g.cardnames.add(s);
                final String[] ss = s.split("\\|");
                cardCounts.put(ss[0], 0);
            }
        }
        return groups;

    }
}
