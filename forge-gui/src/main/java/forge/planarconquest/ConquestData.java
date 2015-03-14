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

import forge.achievement.PlaneswalkerAchievements;
import forge.assets.ISkinImage;
import forge.deck.Deck;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;
import forge.properties.ForgeConstants;
import forge.util.ItemPool;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Function;

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
    private int progress = 100;
    private int planewalkerPosition = 0;
    private int difficulty;
    private ConquestPlane startingPlane, currentPlane;
    private int currentRegionIndex;
    private EnumMap<ConquestPlane, ConquestPlaneData> planeDataMap = new EnumMap<ConquestPlane, ConquestPlaneData>(ConquestPlane.class);
    private ISkinImage planeswalkerToken = PlaneswalkerAchievements.getTrophyImage("Jace, the Mind Sculptor");

    private final HashSet<PaperCard> collection = new HashSet<PaperCard>();
    private final HashMap<String, Deck> decks = new HashMap<String, Deck>();
    private final ItemPool<InventoryItem> decksUsingMyCards = new ItemPool<InventoryItem>(InventoryItem.class);
    private final HashSet<PaperCard> newCards = new HashSet<PaperCard>();

    public ConquestData() { //needed for XML serialization
    }

    public ConquestData(String name0, int difficulty0, ConquestPlane startingPlane0, PaperCard startingCommander0) {
        name = name0;
        difficulty = difficulty0;
        startingPlane = startingPlane0;
        currentPlane = startingPlane0;
        addCommander(startingCommander0);
    }

    public List<PaperCard> addCommander(PaperCard card) {
        ConquestCommander commander = new ConquestCommander(card, currentPlane.getCardPool(), false);
        getCurrentPlaneData().getCommanders().add(commander);
        decks.put(commander.getDeck().getName(), commander.getDeck());

        List<PaperCard> newCards = new ArrayList<PaperCard>();
        for (Entry<PaperCard, Integer> entry : commander.getDeck().getMain()) {
            addCard(entry.getKey(), newCards);
        }
        addCard(card, newCards);
        return newCards;
    }

    private void addCard(PaperCard pc, List<PaperCard> newCards) {
        if (pc.getRules().getType().isBasicLand()) { return; } //ignore basic lands

        if (collection.add(pc)) {
            newCards.add(pc);
        }
    }

    public String getName() {
        return name;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getProgress() {
        return progress;
    }

    public int getPlaneswalkerPosition() {
        return planewalkerPosition;
    }
    public void setPlaneswalkerPosition(int planewalkerPosition0) {
        planewalkerPosition = planewalkerPosition0;
    }

    public ISkinImage getPlaneswalkerToken() {
        return planeswalkerToken;
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
            planeData = new ConquestPlaneData(currentPlane);
            planeDataMap.put(currentPlane, planeData);
        }
        return planeData;
    }

    public Region getCurrentRegion() {
        return currentPlane.getRegions().get(currentRegionIndex);
    }
    public boolean setCurrentRegion(Region region) {
        int index = currentPlane.getRegions().indexOf(region);
        if (index != -1 && currentRegionIndex != index) {
            currentRegionIndex = index;
            return true;
        }
        return false;
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

    public HashSet<PaperCard> getCollection() {
        return collection;
    }

    public ConquestDeckMap getDeckStorage() {
        return new ConquestDeckMap(decks);
    }

    public void addWin(ConquestCommander opponent) {
        wins++;
        winStreakCurrent++;
        getCurrentPlaneData().addWin(opponent);

        if (winStreakCurrent > winStreakBest) {
            winStreakBest = winStreakCurrent;
        }
    }

    public void addLoss(ConquestCommander opponent) {
        losses++;
        winStreakCurrent = 0;
        getCurrentPlaneData().addLoss(opponent);
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

    public void updateDecksForEachCard() {
        decksUsingMyCards.clear();
        for (final Deck deck : FModel.getConquest().getDecks()) {
            for (final Entry<PaperCard, Integer> e : deck.getMain()) {
                decksUsingMyCards.add(e.getKey());
            }
        }
    }

    public HashSet<PaperCard> getNewCards() {
        return newCards;
    }

    public final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare =
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            return newCards.contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };
    public final Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet =
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            return newCards.contains(from.getKey()) ? "NEW" : "";
        }
    };
    public final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.count(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    public final Function<Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.count(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };
}
