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
package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import forge.game.GameFormat;
import forge.util.StorageView;
import forge.util.StorageReaderFile;

/**
 * The Class FormatUtils.
 */
public final class FormatCollection extends StorageView<GameFormat> {



    /**
     * TODO: Write javadoc for Constructor.
     * @param io
     */
    public FormatCollection(String filename) {
        super(new FormatReader(filename));
    }

    /**
     * Gets the standard.
     * 
     * @return the standard
     */
    public GameFormat getStandard() {
        return getMap().get("Standard");
    }

    /**
     * Gets the extended.
     * 
     * @return the extended
     */
    public GameFormat getExtended() {
        return getMap().get("Extended");
    }

    /**
     * Gets the modern.
     * 
     * @return the modern
     */
    public GameFormat getModern() {
        return getMap().get("Modern");
    }

    /** 
     * Get a specified format
     * @return the requested format
     */
    public GameFormat getFormat(String format) {
        return getMap().get(format);
    }
    
    /**
     * Instantiates a new format utils.
     */
    public static class FormatReader extends StorageReaderFile<GameFormat> {

        /**
         * TODO: Write javadoc for Constructor.
         * @param file0
         * @param keySelector0
         */
        public FormatReader(String file0) {
            super(file0, GameFormat.FN_GET_NAME);
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected GameFormat read(String line) {
            String name = null;
            final List<String> sets = new ArrayList<String>(); // default: all
                                                               // sets
            // allowed
            final List<String> bannedCards = new ArrayList<String>(); // default:
            // nothing
            // banned

            final String[] sParts = line.trim().split("\\|");
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
            return new GameFormat(name, sets, bannedCards);

        }

    }
}

/** 
 * TODO: Write javadoc for this type.
 *
 */
