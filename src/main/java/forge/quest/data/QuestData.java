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
package forge.quest.data;

import java.util.HashMap;
import java.util.Map;

import forge.Singletons;
import forge.game.GameFormat;
import forge.quest.QuestMode;
import forge.quest.io.QuestDataIO;

//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

/**
 * <p>
 * QuestData class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class QuestData {

    /** Holds the latest version of the Quest Data. */
    public static final int CURRENT_VERSION_NUMBER = 6;

    // This field places the version number into QD instance,
    // but only when the object is created through the constructor
    // DO NOT RENAME THIS FIELD
    /** The version number. */
    private int versionNumber = QuestData.CURRENT_VERSION_NUMBER;

    private GameFormatQuest format;
    private final String name;

    // Quest mode - there should be an enum :(
    /** The mode. */
    private QuestMode mode;

    // Quest world ID, if any
    private String worldId;
    // gadgets

    private final QuestAssets assets;
    private final QuestAchievements achievements;
    private final Map<Integer, String> petSlots = new HashMap<Integer, String>();


    /**
     * Instantiates a new quest data.
     * @param mode2
     *      quest mode
     * @param diff
     *  achievement diff
     * @param name2
     *      quest name
     * @param formatString
     *      String, persistent format for the quest (null if none).
     * @param userFormat
     *      user-defined format, if any (null if none).
     * @param allowSetUnlocks
     *      allow set unlocking during quest
     * @param startingWorld
     *      starting world
     */
    public QuestData(String name2, int diff, QuestMode mode2, GameFormat userFormat, 
            boolean allowSetUnlocks, final String startingWorld) {
        this.name = name2;

        if (userFormat != null) {
            this.format = new GameFormatQuest(userFormat, allowSetUnlocks);
        }
        this.mode = mode2;
        this.achievements = new QuestAchievements(diff);
        this.assets = new QuestAssets(format);
        this.worldId = startingWorld;

    }

    /**
     * Gets the mode.
     * 
     * @return the mode
     */
    public QuestMode getMode() {
        return this.mode;
    }

    /**
     * Gets the persistent format, null if not assigned.
     * 
     * @return GameFormatQuest, the persistent format
     */
    public GameFormatQuest getFormat() {
        return this.format;
    }

    // SERIALIZATION - related things
    // This must be called by XML-serializer via reflection
    /**
     * Read resolve.
     * 
     * @return the object
     */
    public Object readResolve() {
        return this;
    }

    /**
     * Save data.
     */
    public void saveData() {
        QuestDataIO.saveData(this);
    }

    /**
     * Gets the version number.
     * 
     * @return the versionNumber
     */
    public int getVersionNumber() {
        return this.versionNumber;
    }

    /**
     * Sets the version number.
     * 
     * @param versionNumber0
     *            the versionNumber to set
     */
    public void setVersionNumber(final int versionNumber0) {
        this.versionNumber = versionNumber0;
    }

    /**
     * Gets the name.
     *
     * @return {@link java.lang.String}
     */
    public String getName() {
        return this.name;
    }

    public QuestAssets getAssets() {
        return assets;
    }

    public Map<Integer, String> getPetSlots() {
        return petSlots;
    }

    public QuestAchievements getAchievements() {
        return achievements;
    }

    public String getWorldId() {
        return worldId;
    }

    /**
     * Sets the world id to null or a legal world id.
     * @param newId
     *      String, the world id to set (must be null or legal).
     */
    public void setWorldId(final String newId) {
        if (newId != null && Singletons.getModel().getWorlds().get(newId) == null) {
            throw new RuntimeException("Tried to set illegal (unknown) world id: " + newId);
        }

        worldId = newId;
    }

}
