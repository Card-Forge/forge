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
import forge.util.FileSection;
import forge.util.storage.StorageReaderFile;
import forge.util.storage.StorageView;

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
     * Get a specified format.
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
        protected GameFormat read(String line, int i) {
            final List<String> sets = new ArrayList<String>(); // default: all sets allowed
            final List<String> bannedCards = new ArrayList<String>(); // default:
            // nothing
            // banned

            FileSection section = FileSection.parse(line, ":", "|");
            String name = section.get("name");
            int index = 1 + i;
            String strSets = section.get("sets");
            if ( null != strSets ) {
                sets.addAll(Arrays.asList(strSets.split(", ")));
            }
            String strCars = section.get("banned");
            if ( strCars != null ) {
                bannedCards.addAll(Arrays.asList(strCars.split("; ")));
            }

            if (name == null) {
                throw new RuntimeException("Format must have a name! Check formats.txt file");
            }
            return new GameFormat(name, sets, bannedCards, index);

        }

    }
}

/** 
 * TODO: Write javadoc for this type.
 *
 */
