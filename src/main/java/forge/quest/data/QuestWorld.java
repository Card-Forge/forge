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
package forge.quest.data;

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
        return dir + "/duels";
    }

    /**
     * The quest world challenges directory.
     * @return String, the challenges directory
     */
    public String getChallengesDir() {
        return dir + "/challenges";
    }

    /**
     * The quest world format if specified.
     * @return GameFormatQuest, the format
     */
    public GameFormatQuest getFormat() {
        return format;
    }
}
