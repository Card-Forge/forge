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
package forge.quest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;

import forge.quest.data.GameFormatQuest;
import forge.util.StorageReaderFile;

/** 
 * This function holds the "world info" for the current quest.
 *
 */
public class QuestWorld {
    private final int index; // Used internally to identify the quest world
    private final String name;
    private final String dir;
    private final GameFormatQuest format;

    /**
     * Instantiate a new quest world.
     * @param useIdx int, the quest world internal identifier
     * @param useName String, the display name for the world
     * @param useDir String, the basedir that contains the duels and challenges for the quest world
     * @param useFormat GameFormatQuest that contains the initial format for the world
     */
    public QuestWorld(final int useIdx, final String useName, final String useDir, final GameFormatQuest useFormat) {
        index = useIdx;
        name = useName;
        dir = useDir;
        format = useFormat;
    }

    /**
     * The quest world internal identifier.
     * @return int, the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * The quest world display name.
     * @return String, the display name
     */
    public String getName() {
        return name;
    }

    /**
     * The quest world duels directory.
     * @return String, the duels directory
     */
    public String getDuelsDir() {
        return dir == null ? null : dir + "/duels";
    }

    /**
     * The quest world challenges directory.
     * @return String, the challenges directory
     */
    public String getChallengesDir() {
        return dir == null ? null : dir + "/challenges";
    }

    /**
     * The quest world format if specified.
     * @return GameFormatQuest, the format
     */
    public GameFormatQuest getFormat() {
        return format;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        return this.getName();
    }

    /**
     * FN_GET_NAME for reader.
     */
    public static final Function<QuestWorld, String> FN_GET_NAME = new Function<QuestWorld, String>() {

        @Override
        public String apply(QuestWorld arg1) {
            return arg1.getName();
        }
    };

    /**
     * Class for reading world definitions.
     */
    public static class Reader extends StorageReaderFile<QuestWorld> {

        /**
         * TODO: Write javadoc for Constructor.
         * @param file0
         * @param keySelector0
         */
        public Reader(String file0) {
            super(file0, QuestWorld.FN_GET_NAME);
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected QuestWorld read(String line) {
            String useName = null;
            String useDir = null;
            int useIdx = 0;
            GameFormatQuest useFormat = null;

            final List<String> sets = new ArrayList<String>();
            final List<String> bannedCards = new ArrayList<String>(); // if both empty, no format

            final String[] sParts = line.trim().split("\\|");

            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("index".equals(key)) {
                    useIdx = new Integer(kv[1]);
                } else if ("name".equals(key)) {
                    useName = kv[1];
                } else if ("dir".equals(key)) {
                    useDir = kv[1];
                } else if ("sets".equals(key)) {
                    sets.addAll(Arrays.asList(kv[1].split(", ")));
                } else if ("banned".equals(key)) {
                    bannedCards.addAll(Arrays.asList(kv[1].split("; ")));
                }
            }
            if (useIdx < 0) {
                throw new RuntimeException("Illegal index " + useIdx + "! Check worlds.txt file");
            }
            else if (useName == null) {
                throw new RuntimeException("World " + useIdx + " must have a name! Check worlds.txt file");
            }
            else if (useDir == null && useIdx != 0) {
                throw new RuntimeException("World '" + useName + "' must have a directory! Check worlds.txt file");
            }

            if (!sets.isEmpty() || bannedCards.isEmpty()) {
                useFormat = new GameFormatQuest(useName, sets, bannedCards);
            }

            // System.out.println("Creating quest world " + useName + " (index " + useIdx + ", dir: " + useDir);
            // if (useFormat != null) { System.out.println("SETS: " + sets + "\nBANNED: " + bannedCards); }

            return new QuestWorld(useIdx, useName, useDir, useFormat);

        }

    }
}
