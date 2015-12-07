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
import forge.itemmanager.ColumnDef;
import forge.itemmanager.IItemManager;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;
import forge.properties.ForgeConstants;
import forge.util.ItemPool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private ConquestPlane startingPlane;
    private PaperCard planeswalker;
    private ISkinImage planeswalkerToken;
    private ConquestLocation currentLocation;

    private transient ConquestCollection collection; //don't serialize this

    private final EnumMap<ConquestPlane, ConquestPlaneData> planeDataMap = new EnumMap<ConquestPlane, ConquestPlaneData>(ConquestPlane.class);
    private final HashSet<PaperCard> unlockedCards = new HashSet<PaperCard>();
    private final List<ConquestCommander> commanders = new ArrayList<ConquestCommander>();
    private final HashMap<String, Deck> decks = new HashMap<String, Deck>();
    private final ItemPool<InventoryItem> decksUsingMyCards = new ItemPool<InventoryItem>(InventoryItem.class);
    private final HashSet<PaperCard> newCards = new HashSet<PaperCard>();

    public ConquestData() { //needed for XML serialization
    }

    public ConquestData(String name0, PaperCard planeswalker0, ConquestPlane startingPlane0, PaperCard startingCommander0) {
        name = name0;
        startingPlane = startingPlane0;
        currentLocation = new ConquestLocation(startingPlane, -1, 0, Region.PORTAL_COL);
        planeswalker = planeswalker0;
        planeswalkerToken = PlaneswalkerAchievements.getTrophyImage(planeswalker.getName());
        unlockCard(planeswalker);

        //generate deck for starting commander and add all cards to collection
        ConquestCommander commander = new ConquestCommander(startingCommander0, startingPlane.getCardPool(), false);
        commanders.add(commander);
        unlockCard(startingCommander0);
        unlockCards(commander.getDeck().getMain().toFlatList());
        decks.put(commander.getDeck().getName(), commander.getDeck());
    }

    public String getName() {
        return name;
    }

    public PaperCard getPlaneswalker() {
        return planeswalker;
    }

    public ISkinImage getPlaneswalkerToken() {
        return planeswalkerToken;
    }

    public ConquestPlane getStartingPlane() {
        return startingPlane;
    }

    public ConquestPlane getCurrentPlane() {
        return currentLocation.getPlane();
    }

    public ConquestLocation getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(ConquestLocation currentLocation0) {
        currentLocation = currentLocation0;
    }

    private ConquestPlaneData getOrCreatePlaneData(ConquestPlane plane) {
        ConquestPlaneData planeData = planeDataMap.get(plane);
        if (planeData == null) {
            planeData = new ConquestPlaneData(plane);
            planeDataMap.put(plane, planeData);
        }
        return planeData;
    }

    public ConquestPlaneData getCurrentPlaneData() {
        return getOrCreatePlaneData(getCurrentPlane());
    }

    public void populateCollectionManager(IItemManager<PaperCard> manager) {
        if (collection == null) {
            collection = new ConquestCollection();
        }
        manager.setPool(collection, true);
    }

    public Iterable<PaperCard> getUnlockedCards() {
        return unlockedCards;
    }

    public boolean hasUnlockedCard(PaperCard card) {
        return unlockedCards.contains(card);
    }

    public void unlockCard(PaperCard card) {
        if (unlockedCards.add(card)) {
            newCards.add(card);
            if (collection != null) {
                collection.add(card);
            }
        }
    }
    public void unlockCards(Iterable<PaperCard> cards) {
        for (PaperCard card : cards) {
            unlockCard(card);
        }
    }

    public int getUnlockedCount() {
        return unlockedCards.size();
    }

    public Iterable<ConquestCommander> getCommanders() {
        return commanders;
    }

    public ConquestDeckMap getDeckStorage() {
        return new ConquestDeckMap(decks);
    }

    public void addWin(ConquestEvent event) {
        getOrCreatePlaneData(event.getLocation().getPlane()).addWin(event);
    }

    public void addLoss(ConquestEvent event) {
        getOrCreatePlaneData(event.getLocation().getPlane()).addLoss(event);
    }

    public String getProgress() {
        int conquered = 0;
        int total = 0;

        for (ConquestPlane plane : ConquestPlane.values()) {
            ConquestPlaneData planeData = planeDataMap.get(plane);
            if (planeData != null) {
                conquered += planeData.getConqueredCount();
            }
            total += plane.getEventCount();
        }

        return Math.round(100f * (float)conquered / (float)total) + "%";
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

    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare =
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            return newCards.contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet =
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            return newCards.contains(from.getKey()) ? "NEW" : "";
        }
    };
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.count(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.count(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    public Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, fnNewCompare, fnNewGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, fnDeckCompare, fnDeckGet);
        return colOverrides;
    }

    public List<ConquestLocation> getPath(ConquestLocation destLoc) {
        PathFinder pathFinder = new PathFinder();
        return pathFinder.findPath(destLoc);
    }

    private class PathFinder {
        private final HashSet<Node> closedSet = new HashSet<Node>();
        private final HashSet<Node> openSet = new HashSet<Node>();
        private final Node[][] map;

        private PathFinder() {
            ConquestPlane plane = getCurrentPlane();
            int xMax = Region.COLS_PER_REGION;
            int yMax = plane.getRegions().size() * Region.ROWS_PER_REGION + 2;
            map = new Node[xMax][yMax];
            for (int x = 0; x < xMax; x++) {
                for (int y = 0; y < yMax; y++) {
                    map[x][y] = new Node(plane, x, y);
                }
            }
        }

        public List<ConquestLocation> findPath(ConquestLocation destLoc) {
            Node goal = getNode(destLoc);
            if (goal.isBlocked()) { return null; } //if goal is blocked, there's no path to reach it

            Node start = getNode(getCurrentLocation());
            openSet.add(start);
            start.g_score = 0;
            start.f_score = start.g_score + distance(start, goal);

            Node current;
            while (!openSet.isEmpty()) {
                //find node in open set with lowest f_score
                current = null;
                for (Node node : openSet) {
                    if (current == null || node.f_score < current.f_score) {
                        current = node;
                    }
                }

                //if we've reach goal, reconstruct path and return it
                if (current == goal) {
                    List<ConquestLocation> path = new ArrayList<ConquestLocation>();
                    while (current != null) {
                        path.add(current.loc);
                        current = current.came_from;
                    }
                    Collections.reverse(path); //reverse path so it begins with start location
                    return path;
                }

                //move that node from open set to closed set
                openSet.remove(current);
                closedSet.add(current);

                //check neighbors for path
                checkNeighbor(current, goal, current.x - 1, current.y);
                checkNeighbor(current, goal, current.x + 1, current.y);
                checkNeighbor(current, goal, current.x, current.y - 1);
                checkNeighbor(current, goal, current.x, current.y + 1);
            }
            return null;
        }

        private void checkNeighbor(Node current, Node goal, int x, int y) {
            if (x < 0 || x >= map.length) { return; }
            Node[] column = map[x];
            if (y < 0 || y >= column.length) { return; }
            Node neighbor = column[y];
            if (neighbor.isBlocked()) { return; }
            if (closedSet.contains(neighbor)) { return; }

            int g_score = current.g_score + 1;
            if (!openSet.contains(neighbor)) {
                openSet.add(neighbor);
            }
            else if (g_score >= neighbor.g_score) {
                return; //not a better path
            }

            //this path is the best, so record it
            neighbor.came_from = current;
            neighbor.g_score = g_score;
            neighbor.f_score = neighbor.g_score + distance(neighbor, goal);
        }

        private int distance(Node from, Node to) {
            return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
        }

        private Node getNode(ConquestLocation loc) {
            int x = loc.getCol();
            int y;
            int regionCount = loc.getPlane().getRegions().size();
            int regionIndex = loc.getRegionIndex();
            if (regionIndex == -1) {
                y = 0;
            }
            else if (regionIndex == regionCount) {
                y = map[x].length - 1;
            }
            else {
                y = regionIndex * Region.ROWS_PER_REGION + loc.getRow() + 1;
            }
            return map[x][y];
        }

        private class Node {
            private final int x, y;
            private final ConquestLocation loc;
            private int g_score = Integer.MAX_VALUE;
            private int f_score = Integer.MAX_VALUE;
            private Node came_from;
            private Boolean blocked = null;

            public Node(ConquestPlane plane, int x0, int y0) {
                x = x0;
                y = y0;

                int row;
                int col = x;
                int rowIndex = y - 1;
                int regionCount = plane.getRegions().size();
                int regionIndex = rowIndex / Region.ROWS_PER_REGION;
                if (rowIndex < 0) {
                    regionIndex = -1;
                    row = 0;
                }
                else if (regionIndex >= regionCount) {
                    regionIndex = regionCount;
                    row = 0;
                }
                else {
                    row = rowIndex % Region.ROWS_PER_REGION;
                }
                loc = new ConquestLocation(plane, regionIndex, row, col);
            }

            public boolean isBlocked() {
                if (blocked == null) { //determine if node is blocked one time
                    blocked = false; //assume not blocked by default
                    if (!loc.isTraversable()) {
                        blocked = true;
                    }
                    else {
                        //if location isn't conquered or bordering a conquered location, there's no path to reach it
                        ConquestPlaneData planeData = getCurrentPlaneData();
                        if (!planeData.hasConquered(loc)) {
                            blocked = true;
                            for (ConquestLocation neighbor : loc.getNeighbors()) {
                                if (planeData.hasConquered(neighbor)) {
                                    blocked = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                return blocked;
            }
        }
    }

    @SuppressWarnings("serial")
    private class ConquestCollection extends ItemPool<PaperCard> {
        private ConquestCollection() {
            super(PaperCard.class);
            setAllowZero(true);

            //initialize to contain all available cards, with unlocked
            //having a count of 1 and the rest having a count of 0
            for (ConquestPlane plane : ConquestPlane.values()) {
                for (PaperCard card : plane.getCardPool().getAllCards()) {
                    items.put(card, hasUnlockedCard(card) ? 1 : 0);
                }
            }
        }
    }
}
