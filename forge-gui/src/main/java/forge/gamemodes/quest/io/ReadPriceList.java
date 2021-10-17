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
package forge.gamemodes.quest.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.card.MagicColor;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.MyRandom;

/**
 * <p>
 * ReadPriceList class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class ReadPriceList {

    /** Constant <code>comment="//"</code>. */
    private static final String COMMENT = "//";

    private Map<String, Integer> priceMap;

    /**
     * <p>
     * Constructor for ReadPriceList.
     * </p>
     */
    public ReadPriceList() {
        this.setup();
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        this.priceMap = this.readFile(ForgeConstants.QUEST_CARD_PRICE_FILE);
        this.priceMap.putAll(this.readFile(ForgeConstants.PRICES_BOOSTER_FILE));
    } // setup()

    /**
     * <p>
     * readFile.
     * </p>
     *
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.util.HashMap} object.
     */
    private Map<String, Integer> readFile(final String file) {
        final Map<String, Integer> map = new HashMap<>();

        final List<String> lines = FileUtil.readFile(file);
        for (final String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            if (line.startsWith(ReadPriceList.COMMENT)) {
                continue;
            }

            final String[] s = line.split("=");
            if (s.length < 2) { continue; } //skip line if not in correct format

            final String name = s[0].trim();
            final String price = s[1].trim();

            try {
                int val = Integer.parseInt(price.trim());

                if (!(MagicColor.Constant.BASIC_LANDS.contains(name) || MagicColor.Constant.SNOW_LANDS.contains(name)) && !ForgeConstants.PRICES_BOOSTER_FILE.equals(file)) {
                    float ff;
                    if (MyRandom.getRandom().nextInt(100) < 90) {
                        ff = MyRandom.getRandom().nextInt(10) * (float) .01;
                    } else {
                        // +/- 50%
                        ff = MyRandom.getRandom().nextInt(50) * (float) .01;
                    }

                    if (MyRandom.getRandom().nextInt(100) < 50) {
                        val = (int) (val * (1 - ff));
                    } else {
                        // +ff%
                        val = (int) (val * (1 + ff));
                    }
                }

                map.put(name, val);
            } catch (final NumberFormatException nfe) {
                System.err.println("NumberFormatException: " + nfe.getMessage());
            }
        }
        return map;
    } // readFile()

    /**
     * <p>
     * getPriceList.
     * </p>
     *
     * @return a {@link java.util.Map} object.
     */
    public final Map<String, Integer> getPriceList() {
        return this.priceMap;
    }
}
