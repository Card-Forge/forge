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
package forge.gamemodes.quest.data;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import forge.game.GameFormat;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.io.QuestDataIO;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

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
public class QuestData {
    /** Holds the latest version of the Quest Data. */
    public static final int CURRENT_VERSION_NUMBER = 13;

    // This field places the version number into QD instance,
    // but only when the object is created through the constructor
    // DO NOT RENAME THIS FIELD
    /** The version number. */
    private int versionNumber = QuestData.CURRENT_VERSION_NUMBER;

    private GameFormatQuest format;
    private String name;

    // Quest mode - there should be an enum :(
    /** The mode. */
    private QuestMode mode;

    // Quest world ID, if any
    private String worldId;
    // gadgets

    private QuestAssets assets;
    private QuestAchievements achievements;
    private final Map<Integer, String> petSlots = new HashMap<>();
    private int matchLength = 3;

    public HashSet<StarRating> Ratings = new HashSet<>();

    public String currentDeck = "DEFAULT";

    /**
     * Holds the subformat for this quest. Defaults to DeckConstructionRules.Default.
     */
    public DeckConstructionRules deckConstructionRules = DeckConstructionRules.Default;

    public QuestData() { //needed for XML serialization
    }

    /**
     * Instantiates a new quest data.
     * @param name0
     *      quest name
     * @param diff
     *  achievement diff
     * @param mode0
     *      quest mode
     * @param userFormat
     *      user-defined format, if any (null if none).
     * @param allowSetUnlocks
     *      allow set unlocking during quest
     * @param startingWorld
     *      starting world
     * @param dcr
     *      deck construction rules e.g. Commander
     */
    public QuestData(String name0, int diff, QuestMode mode0, GameFormat userFormat,
                     boolean allowSetUnlocks, final String startingWorld, DeckConstructionRules dcr) {
        this.name = name0;

        if (userFormat != null) {
            this.format = new GameFormatQuest(userFormat, allowSetUnlocks);
        }
        this.mode = mode0;
        this.achievements = new QuestAchievements(diff);
        this.assets = new QuestAssets(format);
        this.worldId = startingWorld;
        this.deckConstructionRules = dcr;
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

    /**
     * Rename this quest.
     *
     * @param newName
     *            the new name to set
     */
    public void rename(final String newName) {
        File newpath = new File(ForgeConstants.QUEST_SAVE_DIR, newName + ".dat");
        File oldpath = new File(ForgeConstants.QUEST_SAVE_DIR, this.name + ".dat");
        oldpath.renameTo(newpath);

        newpath = new File(ForgeConstants.QUEST_SAVE_DIR, newName + ".dat.bak");
        oldpath = new File(ForgeConstants.QUEST_SAVE_DIR, this.name + ".dat.bak");
        oldpath.renameTo(newpath);

        this.name = newName;
        QuestDataIO.saveData(this);
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
        if (newId != null && FModel.getWorlds().get(newId) == null) {
            throw new RuntimeException("Tried to set illegal (unknown) world id: " + newId);
        }

        worldId = newId;
    }

    public void setMatchLength(int len) { matchLength = len; }
    public int getMatchLength() { return matchLength; }
}
