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
package forge.planarconquest;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane.Region;
import forge.properties.ForgeConstants;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;

public final class ConquestData {
    /** Holds the latest version of the Conquest Data. */
    public static final int CURRENT_VERSION_NUMBER = 0;

    // This field places the version number into QD instance,
    // but only when the object is created through the constructor
    // DO NOT RENAME THIS FIELD
    private int versionNumber = ConquestData.CURRENT_VERSION_NUMBER;

    private String name;
    private int wins, losses;
    private int winStreakBest = 0;
    private int winStreakCurrent = 0;
    private int day = 1;
    private int difficulty;
    private ConquestPlane startingPlane, currentPlane;
    private int currentRegionIndex;
    private EnumMap<ConquestPlane, ConquestPlaneData> planeDataMap = new EnumMap<ConquestPlane, ConquestPlaneData>(ConquestPlane.class);

    private final CardPool collection = new CardPool();
    private final HashMap<String, Deck> decks = new HashMap<String, Deck>();

    public ConquestData() { //needed for XML serialization
    }

    public ConquestData(String name0, int difficulty0, ConquestPlane startingPlane0, PaperCard startingCommander0) {
        name = name0;
        difficulty = difficulty0;
        startingPlane = startingPlane0;
        currentPlane = startingPlane0;
        addCommander(startingCommander0);
    }

    private void addCommander(PaperCard card) {
        ConquestCommander commander = new ConquestCommander(card, currentPlane.getCardPool());
        getCurrentPlaneData().getCommanders().add(commander);
        decks.put(commander.getDeck().getName(), commander.getDeck());
        collection.addAll(commander.getDeck().getMain());
        collection.add(card);
    }

    public String getName() {
        return name;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getDay() {
        return day;
    }

    public ConquestPlane getStartingPlane() {
        return startingPlane;
    }

    public ConquestPlane getCurrentPlane() {
        return currentPlane;
    }

    public ConquestPlaneData getCurrentPlaneData() {
        ConquestPlaneData planeData = planeDataMap.get(currentPlane);
        if (planeData == null) {
            planeData = new ConquestPlaneData();
            planeDataMap.put(currentPlane, planeData);
        }
        return planeData;
    }

    public Region getCurrentRegion() {
        return currentPlane.getRegions().get(currentRegionIndex);
    }

    public void incrementRegion(int dir) {
        if (dir > 0) {
            currentRegionIndex++;
            if (currentRegionIndex >= currentPlane.getRegions().size()) {
                currentRegionIndex = 0;
            }
        }
        else {
            currentRegionIndex--;
            if (currentRegionIndex < 0) {
                currentRegionIndex = currentPlane.getRegions().size() - 1;
            }
        }
    }

    public CardPool getCollection() {
        return collection;
    }

    public ConquestDeckMap getDeckStorage() {
        return new ConquestDeckMap(decks);
    }

    public void addWin() {
        wins++;
        winStreakCurrent++;
        getCurrentPlaneData().addWin();

        if (winStreakCurrent > winStreakBest) {
            winStreakBest = winStreakCurrent;
        }
    }

    public void addLoss() {
        losses++;
        winStreakCurrent = 0;
        getCurrentPlaneData().addLoss();
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getWinStreakBest() {
        return winStreakBest;
    }

    public int getWinStreakCurrent() {
        return winStreakCurrent;
    }

    // SERIALIZATION - related things
    // This must be called by XML-serializer via reflection
    public Object readResolve() {
        return this;
    }

    public void saveData() {
        ConquestDataIO.saveData(this);
    }

    public int getVersionNumber() {
        return versionNumber;
    }
    public void setVersionNumber(final int versionNumber0) {
        versionNumber = versionNumber0;
    }

    public void rename(final String newName) {
        File newpath = new File(ForgeConstants.CONQUEST_SAVE_DIR, newName + ".dat");
        File oldpath = new File(ForgeConstants.CONQUEST_SAVE_DIR, name + ".dat");
        oldpath.renameTo(newpath);

        name = newName;
        saveData();
    }
}
