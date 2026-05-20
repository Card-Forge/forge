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
package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import forge.card.MagicColor;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.MyRandom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads card price lists for Adventure mode.
 * Prices are loaded per-adventure using Config.getFile() fallback
 * (adventure-specific file first, then common).
 *
 * Supported directives (comment lines at the top of the file):
 *   // mode: optional  - Player controls via settings toggle (default)
 *   // mode: forced    - Custom prices always active, settings toggle hidden
 *   // fluctuation: true - Enable random price variation
 */
public class AdventureReadPriceList {

    private static final String COMMENT = "//";
    private static final String FLUCTUATION_DIRECTIVE = "// fluctuation:";
    private static final String MODE_DIRECTIVE = "// mode:";

    public enum PriceMode { FORCED, OPTIONAL }

    public static class PriceData {
        public final Map<String, Integer> prices;
        public final PriceMode mode;

        PriceData(Map<String, Integer> prices, PriceMode mode) {
            this.prices = prices;
            this.mode = mode;
        }
    }

    /**
     * Load card prices from the current adventure's cardprices.txt.
     * Uses Config.getFile() so each plane can override common prices.
     *
     * @return PriceData with prices and mode, or empty prices if no file exists
     */
    public static PriceData loadPrices() {
        FileHandle handle = Config.instance().getFile(Paths.CARD_PRICES);
        if (handle == null || !handle.exists()) {
            return new PriceData(new HashMap<>(), PriceMode.OPTIONAL);
        }
        PriceData data = readFile(handle.path());
        // Also load booster prices from the common list (Simisays's booster pricing)
        data.prices.putAll(readPriceEntries(ForgeConstants.ADVENTURE_BOOSTER_PRICE_FILE));
        return data;
    }

    private static PriceData readFile(final String file) {
        final Map<String, Integer> map = new HashMap<>();

        final List<String> lines = FileUtil.readFile(file);

        // Parse directives from comment lines at the top
        boolean fluctuate = false;
        PriceMode mode = PriceMode.OPTIONAL;
        for (final String line : lines) {
            String trimmed = line.trim().toLowerCase();
            if (trimmed.startsWith(FLUCTUATION_DIRECTIVE)) {
                fluctuate = trimmed.substring(FLUCTUATION_DIRECTIVE.length()).trim().equals("true");
            } else if (trimmed.startsWith(MODE_DIRECTIVE)) {
                String modeStr = trimmed.substring(MODE_DIRECTIVE.length()).trim();
                if (modeStr.equals("optional")) {
                    mode = PriceMode.OPTIONAL;
                }
            } else if (!trimmed.isEmpty() && !trimmed.startsWith(COMMENT)) {
                // Stop scanning after first non-comment, non-empty line
                break;
            }
        }

        for (final String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith(COMMENT)) {
                continue;
            }

            final String[] s = line.split("=");
            if (s.length < 2) {
                continue;
            }

            final String name = s[0].trim();
            final String price = s[1].trim();

            try {
                int val = Integer.parseInt(price);

                if (fluctuate
                        && !MagicColor.Constant.BASIC_LANDS.contains(name)
                        && !MagicColor.Constant.SNOW_LANDS.contains(name)) {
                    float fluctuation;
                    if (MyRandom.getRandom().nextInt(100) < 90) {
                        // 90% of the time: +/- 10%
                        fluctuation = MyRandom.getRandom().nextInt(10) * 0.01f;
                    } else {
                        // 10% of the time: +/- 50%
                        fluctuation = MyRandom.getRandom().nextInt(50) * 0.01f;
                    }

                    if (MyRandom.getRandom().nextInt(100) < 50) {
                        val = (int) (val * (1 - fluctuation));
                    } else {
                        val = (int) (val * (1 + fluctuation));
                    }
                }

                map.put(name, val);
            } catch (final NumberFormatException nfe) {
                System.err.println("AdventureReadPriceList: invalid price for '" + name + "': " + nfe.getMessage());
            }
        }
        return new PriceData(map, mode);
    }

    /**
     * Read plain name=price entries from a file, no directives or fluctuation.
     * Used for booster price lists.
     */
    private static Map<String, Integer> readPriceEntries(final String file) {
        final Map<String, Integer> map = new HashMap<>();
        final List<String> lines = FileUtil.readFile(file);
        for (final String line : lines) {
            if (line.trim().isEmpty() || line.startsWith(COMMENT)) {
                continue;
            }
            final String[] s = line.split("=");
            if (s.length < 2) {
                continue;
            }
            try {
                map.put(s[0].trim(), Integer.parseInt(s[1].trim()));
            } catch (final NumberFormatException nfe) {
                System.err.println("AdventureReadPriceList: invalid price for '" + s[0].trim() + "': " + nfe.getMessage());
            }
        }
        return map;
    }
}
