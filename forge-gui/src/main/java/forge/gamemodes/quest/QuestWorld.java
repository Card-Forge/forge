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
package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;

import forge.deck.Deck;
import forge.game.GameFormat;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.storage.StorageReaderFile;

/** 
 * This function holds the "world info" for the current quest.
 *
 */
public class QuestWorld implements Comparable<QuestWorld>{
    private final String name;
    private final String dir;
    private final GameFormatQuest format;
    public static final String STANDARDWORLDNAME = "Random Standard";
    public static final String PIONEERWORLDNAME = "Random Pioneer";
    public static final String MODERNWORLDNAME = "Random Modern";
    public static final String RANDOMCOMMANDERWORLDNAME = "Random Commander";
    public static final String MAINWORLDNAME = "Main world";

    private boolean isCustom;

    /**
     * Instantiate a new quest world.
     * @param useName String, the display name for the world
     * @param useDir String, the basedir that contains the duels and challenges for the quest world
     * @param useFormat GameFormatQuest that contains the initial format for the world
     */
    public QuestWorld(final String useName, final String useDir, final GameFormatQuest useFormat) {
        name = useName;
        dir = useDir;
        format = useFormat;
        isCustom = false;
    }

    /**
     * Instantiate a new quest world.
     * @param useName String, the display name for the world
     * @param useDir String, the basedir that contains the duels and challenges for the quest world
     * @param useFormat GameFormatQuest that contains the initial format for the world
     * @param isCustom0 boolean determining whether the world is from the user's custom folder
     */
    public QuestWorld(final String useName, final String useDir, final GameFormatQuest useFormat, final boolean isCustom0) {
        name = useName;
        dir = useDir;
        format = useFormat;
        isCustom = isCustom0;
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

    public GameFormatQuest getFormat() {
        return format;
    }

    public Collection<PaperCard> getAllCards() {
        GameFormat format0 = format;
        if (format0 == null) {
            format0 = FModel.getQuest().getMainFormat();
        }
        if (format0 != null) {
            return format0.getAllCards();
        }
        return FModel.getMagicDb().getCommonCards().getAllCards();
    }

    @Override
    public final String toString() {
        return this.getName();
    }

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
         */
        public Reader(String file0) {
            super(file0, QuestWorld.FN_GET_NAME);
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected QuestWorld read(String line, int i) {
            String useName = null;
            String useDir = null;
            GameFormatQuest useFormat = null;

            final List<String> sets = new ArrayList<>();
            final List<String> bannedCards = new ArrayList<>(); // if both empty, no format

            String key, value;
            String[] pieces = line.split("\\|");
            for (String piece : pieces) {
                int idx = piece.indexOf(':');
                if (idx != -1) {
                    key = piece.substring(0, idx).trim().toLowerCase();
                    value = piece.substring(idx + 1).trim();
                }
                else {
                    alertInvalidLine(line, "Invalid plane definition.");
                    key = piece.trim().toLowerCase();
                    value = "";
                }
                switch(key) {
                case "name":
                    useName = value;
                    break;
                case "dir":
                    useDir = value;
                    break;
                case "sets":
                    sets.addAll(Arrays.asList(value.split(", ")));
                    break;
                case "banned":
                    bannedCards.addAll(Arrays.asList(value.split("; ")));
                    break;
                default:
                    alertInvalidLine(line, "Invalid quest world definition.");
                    break;
                }
            }

            if (useName == null) {
                alertInvalidLine(line, "Quest world must have a name.");
                return null;
            }

            if (!sets.isEmpty() || !bannedCards.isEmpty()) {
                useFormat = new GameFormatQuest(useName, sets, bannedCards);
            }

            if (useName.equalsIgnoreCase(QuestWorld.STANDARDWORLDNAME)){
                useFormat = new GameFormatQuest(QuestWorld.STANDARDWORLDNAME,
                        FModel.getFormats().getStandard().getAllowedSetCodes(),
                        FModel.getFormats().getStandard().getBannedCardNames(),false);
            }

            if (useName.equalsIgnoreCase(QuestWorld.PIONEERWORLDNAME)){
                useFormat = new GameFormatQuest(QuestWorld.PIONEERWORLDNAME,
                        FModel.getFormats().getPioneer().getAllowedSetCodes(),
                        FModel.getFormats().getPioneer().getBannedCardNames(),false);
            }

            if (useName.equalsIgnoreCase(QuestWorld.MODERNWORLDNAME)){
                useFormat = new GameFormatQuest(QuestWorld.MODERNWORLDNAME,
                        FModel.getFormats().getModern().getAllowedSetCodes(),
                        FModel.getFormats().getModern().getBannedCardNames(),false);
            }

            if (useName.equalsIgnoreCase(QuestWorld.RANDOMCOMMANDERWORLDNAME)){
                useFormat = new GameFormatQuest(QuestWorld.RANDOMCOMMANDERWORLDNAME,
                        FModel.getFormats().getFormat("Commander").getAllowedSetCodes(),
                        FModel.getFormats().getFormat("Commander").getBannedCardNames(),false);
            }

            // System.out.println("Creating quest world " + useName + " (index " + useIdx + ", dir: " + useDir);
            // if (useFormat != null) { System.out.println("SETS: " + sets + "\nBANNED: " + bannedCards); }

            return new QuestWorld(useName, useDir, useFormat);

        }

    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(QuestWorld other) {
        if (null == other)
            return 1;
        if (this.name == null)
            return -1;  // dummy, no format!
        return name.compareTo(other.name);
    }

    public static Set<QuestWorld> getAllQuestWorldsOfCard(PaperCard card) {
        Set<QuestWorld> result = new HashSet<>();
        for (QuestWorld qw : FModel.getWorlds()) {
            GameFormat format = qw.getFormat();
            if (format == null) {
                format = FModel.getQuest().getMainFormat();
            }
            if (format == null || format.getFilterRules().apply(card)) {
                result.add(qw);
            }
        }
        return result;
    }

    public static Set<QuestWorld> getAllQuestWorldsOfDeck(Deck deck) {
        Set<QuestWorld> result = new HashSet<>();
        for (QuestWorld qw : FModel.getWorlds()) {
            GameFormat format = qw.getFormat();
            if (format == null) {
                format = FModel.getQuest().getMainFormat();
            }
            if (format == null || format.isDeckLegal(deck)) {
                result.add(qw);
            }
        }
        return result;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }
}
