/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import forge.game.GameFormat;
import forge.util.FileUtil;

/**
 * The Class FormatUtils.
 */
public final class FormatUtils {

    private final Map<String, GameFormat> formats = new TreeMap<String, GameFormat>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Gets the standard.
     * 
     * @return the standard
     */
    public GameFormat getStandard() {
        return formats.get("Standard");
    }

    /**
     * Gets the extended.
     * 
     * @return the extended
     */
    public GameFormat getExtended() {
        return formats.get("Extended");
    }

    /**
     * Gets the modern.
     * 
     * @return the modern
     */
    public GameFormat getModern() {
        return formats.get("Modern");
    }

    // list are immutable, no worries
    /**
     * Gets the formats.
     * 
     * @return the formats
     */
    public Collection<GameFormat> getFormats() {
        return formats.values();
    }

    /**
     * Instantiates a new format utils.
     */
    public FormatUtils() {
        final List<String> fData = FileUtil.readFile("res/blockdata/formats.txt");

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            String name = null;
            final List<String> sets = new ArrayList<String>(); // default: all
                                                               // sets
            // allowed
            final List<String> bannedCards = new ArrayList<String>(); // default:
            // nothing
            // banned

            final String[] sParts = s.trim().split("\\|");
            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("sets".equals(key)) {
                    sets.addAll(Arrays.asList(kv[1].split(", ")));
                } else if ("banned".equals(key)) {
                    bannedCards.addAll(Arrays.asList(kv[1].split("; ")));
                }
            }
            if (name == null) {
                throw new RuntimeException("Format must have a name! Check formats.txt file");
            }
            final GameFormat thisFormat = new GameFormat(name, sets, bannedCards);

            formats.put(name, thisFormat);
        }
    }
}

/** 
 * TODO: Write javadoc for this type.
 *
 */
