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

import com.google.common.base.Function;
import forge.achievement.PlaneswalkerAchievements;
import forge.assets.ISkinImage;
import forge.card.CardDb;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.gui.SOptionPane;
import forge.util.Localizer;
import forge.util.CardTranslation;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public final class ConquestData {
    private static final String XML_FILE = "data.xml";

    private File directory;
    private String name, xmlFilename;
    private PaperCard planeswalker;
    private ISkinImage planeswalkerToken;
    private ConquestLocation currentLocation;
    private int selectedCommanderIndex;
    private int aetherShards;
    private int planeswalkEmblems;

    private final ConquestRecord chaosBattleRecord;
    private final Map<String, ConquestPlaneData> planeDataMap = new HashMap<>();
    private final HashSet<PaperCard> unlockedCards = new HashSet<>();
    private final List<ConquestCommander> commanders = new ArrayList<>();
    private final HashSet<PaperCard> newCards = new HashSet<>();
    private final HashSet<PaperCard> exiledCards = new HashSet<>();

    public ConquestData(String name0, ConquestPlane startingPlane0, PaperCard startingPlaneswalker0, PaperCard startingCommander0) {
        name = name0;
        directory = new File(ForgeConstants.CONQUEST_SAVE_DIR, name.replace(' ', '_')); //prevent issue on Android where data won't save if directory name contains spaces
        xmlFilename = directory.getPath() + ForgeConstants.PATH_SEPARATOR + XML_FILE;
        aetherShards = FModel.getConquestPreferences().getPrefInt(CQPref.AETHER_START_SHARDS);
        currentLocation = new ConquestLocation(startingPlane0, 0, 0, 0);
        unlockPlane(startingPlane0);
        setPlaneswalker(startingPlaneswalker0);

        //unlock starting commander, starting planeswalker, and all cards in generated deck
        unlockCard(startingCommander0);
        unlockCard(startingPlaneswalker0);
        ConquestCommander commander = getSelectedCommander();
        for (Entry<PaperCard, Integer> entry : commander.getDeck().getMain()) {
            PaperCard card = entry.getKey();
            if (!card.getRules().getType().isBasicLand()) { //ignore basic lands
                unlockCard(card);
            }
        }
        chaosBattleRecord = new ConquestRecord();
    }

    public ConquestData(File directory0) {
        name = directory0.getName().replace('_', ' '); //restore spaces in name
        directory = directory0;
        xmlFilename = directory.getPath() + ForgeConstants.PATH_SEPARATOR + XML_FILE;

        ConquestRecord chaosBattleRecord0 = null;
        try {
            XmlReader xml = new XmlReader(xmlFilename);
            CardDb cardDb = FModel.getMagicDb().getCommonCards();
            setPlaneswalker(xml.read("planeswalker", cardDb));
            aetherShards = xml.read("aetherShards", aetherShards);
            planeswalkEmblems = xml.read("planeswalkEmblems", planeswalkEmblems);
            currentLocation = xml.read("currentLocation", ConquestLocation.class);
            selectedCommanderIndex = xml.read("selectedCommanderIndex", selectedCommanderIndex);
            chaosBattleRecord0 = xml.read("chaosBattleRecord", ConquestRecord.class);
            xml.read("unlockedCards", unlockedCards, cardDb);
            xml.read("newCards", newCards, cardDb);
            xml.read("exiledCards", exiledCards, cardDb);
            xml.read("commanders", commanders, ConquestCommander.class);
            xml.read("planeDataMap", planeDataMap, ConquestPlaneData.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (chaosBattleRecord0 == null) {
            chaosBattleRecord0 = new ConquestRecord();
        }
        chaosBattleRecord = chaosBattleRecord0;
    }

    public void saveData() {
        FileUtil.ensureDirectoryExists(directory);

        try {
            XmlWriter xml = new XmlWriter(xmlFilename, "data");
            xml.write("planeswalker", planeswalker);
            xml.write("aetherShards", aetherShards);
            xml.write("planeswalkEmblems", planeswalkEmblems);
            xml.write("currentLocation", currentLocation);
            xml.write("selectedCommanderIndex", selectedCommanderIndex);
            xml.write("chaosBattleRecord", chaosBattleRecord);
            xml.write("unlockedCards", unlockedCards);
            xml.write("newCards", newCards);
            xml.write("exiledCards", exiledCards);
            xml.write("commanders", commanders);
            xml.write("planeDataMap", planeDataMap);
            xml.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }

    public PaperCard getPlaneswalker() {
        return planeswalker;
    }
    public void setPlaneswalker(PaperCard planeswalker0) {
        planeswalker = planeswalker0;
        planeswalkerToken = PlaneswalkerAchievements.getTrophyImage(planeswalker.getName());
    }

    public ISkinImage getPlaneswalkerToken() {
        return planeswalkerToken;
    }

    public Iterable<PaperCard> getSortedPlaneswalkers() {
        List<PaperCard> planeswalkers = new ArrayList<>();
        for (PaperCard card : unlockedCards) {
            if (card.getRules().getType().isPlaneswalker() && !isInExile(card)) {
                planeswalkers.add(card);
            }
        }
        Collections.sort(planeswalkers);
        return planeswalkers;
    }

    public ConquestPlane getCurrentPlane() {
        return currentLocation.getPlane();
    }

    public ConquestLocation getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(ConquestLocation currentLocation0) {
        currentLocation = currentLocation0;
        getCurrentPlaneData().setLocation(currentLocation0);
    }

    private ConquestPlaneData getPlaneData(ConquestPlane plane) {
        return planeDataMap.get(plane.getName());
    }
    public ConquestPlaneData getCurrentPlaneData() {
        return getPlaneData(getCurrentPlane());
    }

    public boolean isPlaneUnlocked(ConquestPlane plane) {
        return planeDataMap.containsKey(plane.getName());
    }

    public int getUnlockedPlaneCount() {
        return planeDataMap.size();
    }

    public int getAccessiblePlaneCount() {
        // TODO: Java 8 stream implementation of filtering
        int i = 0;
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (!plane.isUnreachable()) {
                i++;
            }
        }
        return i;
    }

    public void unlockPlane(ConquestPlane plane) {
        if (isPlaneUnlocked(plane)) { return; }

        planeDataMap.put(plane.getName(), new ConquestPlaneData(plane));
    }

    public void planeswalkTo(ConquestPlane plane) {
        ConquestPlaneData planeData = getPlaneData(plane);
        if (planeData == null) { return; }

        setCurrentLocation(planeData.getLocation());
    }

    public ConquestCommander getSelectedCommander() {
        return commanders.get(selectedCommanderIndex);
    }
    public void setSelectedCommander(ConquestCommander commander) {
        selectedCommanderIndex = commanders.indexOf(commander);
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

            //add card to available commanders if eligible
            if (card.getRules().canBeCommander()) {
                ConquestCommander commander;
                if (commanders.isEmpty()) { //generate deck for starting commander
                    commander = new ConquestCommander(card, getCurrentPlane());
                    selectedCommanderIndex = 0;
                }
                else {
                    commander = new ConquestCommander(card);
                }
                commanders.add(commander);
            }
        }
    }
    public void unlockCards(Iterable<PaperCard> cards) {
        for (PaperCard card : cards) {
            unlockCard(card);
        }
    }

    public boolean isInExile(PaperCard card) {
        return exiledCards.contains(card);
    }

    public Iterable<PaperCard> getExiledCards() {
        return exiledCards;
    }

    public boolean exileCards(Collection<PaperCard> cards, int value) {
        int count = cards.size();
        if (count == 0) { return false; }

        String title = count == 1 ? Localizer.getInstance().getMessage("lblExileCard") : Localizer.getInstance().getMessage("lblExileNCard", String.valueOf(count));
        String cardStr = (count == 1 ? Localizer.getInstance().getMessage("lblCard") : Localizer.getInstance().getMessage("lblCards"));

        List<ConquestCommander> commandersBeingExiled = null;

        StringBuilder message = new StringBuilder(Localizer.getInstance().getMessage("lblExileFollowCardsToReceiveNAE", cardStr, "{AE}", String.valueOf(value)));
        for (PaperCard card : cards) {
            if (planeswalker == card) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblCurrentPlaneswalkerCannotBeExiled"), title, SOptionPane.INFORMATION_ICON);
                return false;
            }

            StringBuilder commandersUsingCard = new StringBuilder();
            for (ConquestCommander commander : commanders) {
                if (commander.getCard() == card) {
                    if (!commander.getDeck().getMain().isEmpty()) {
                        SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblCannotCommanderWithDefinedDeck"), title, SOptionPane.INFORMATION_ICON);
                        return false;
                    }
                    if (commandersBeingExiled == null) {
                        commandersBeingExiled = new ArrayList<>();
                    }
                    commandersBeingExiled.add(commander); //cache commander to make it easier to remove later
                }
                if (commander.getDeck().getMain().contains(card)) {
                    commandersUsingCard.append("\n").append(CardTranslation.getTranslatedName(commander.getName()));
                }
            }

            if (commandersUsingCard.length() > 0) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblCommandersCardCannotBeExiledByCard", CardTranslation.getTranslatedName(card.getName()), commandersUsingCard), title, SOptionPane.INFORMATION_ICON);
                return false;
            }

            message.append("\n").append(CardTranslation.getTranslatedName(card.getName()));
        }

        if (SOptionPane.showConfirmDialog(message.toString(), title, Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel"))) {
            if (exiledCards.addAll(cards)) {
                if (commandersBeingExiled != null) {
                    commanders.removeAll(commandersBeingExiled);
                }
                rewardAEtherShards(value);
                saveData();
                return true;
            }
        }
        return false;
    }

    public boolean retrieveCardsFromExile(Collection<PaperCard> cards, int cost) {
        int count = cards.size();
        if (count == 0) { return false; }

        String title = count == 1 ? Localizer.getInstance().getMessage("lblRetrieveCard") : Localizer.getInstance().getMessage("lblRetrieveNCard", String.valueOf(count));
        String cardStr = (count == 1 ? Localizer.getInstance().getMessage("lblCard") : Localizer.getInstance().getMessage("lblCards"));
        if (aetherShards < cost) {
            SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNotEnoughShardsToRetrieveCards", cardStr), title, SOptionPane.INFORMATION_ICON);
            return false;
        }

        StringBuilder message = new StringBuilder(Localizer.getInstance().getMessage("lblSpendAECostToRetrieveCardsFromExile", "{AE}", String.valueOf(cost), cardStr));
        for (PaperCard card : cards) {
            message.append("\n").append(card.getName());
        }
        if (SOptionPane.showConfirmDialog(message.toString(), title, Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel"))) {
            if (exiledCards.removeAll(cards)) {
                for (PaperCard card : cards) {
                    if (card.getRules().canBeCommander()) { //add back commander for card if needed
                        commanders.add(new ConquestCommander(card));
                    }
                }
                spendAEtherShards(cost);
                saveData();
                return true;
            }
        }
        return false;
    }

    public int getUnlockedCardCount() {
        return unlockedCards.size();
    }

    public Iterable<ConquestCommander> getCommanders() {
        return commanders;
    }

    public void addWin(ConquestBattle event) {
        ConquestPlaneData planeData = getPlaneData(event.getLocation().getPlane());
        if (planeData == null) { return; }

        planeData.addWin(event);
        getSelectedCommander().getRecord().addWin();
        event.setConquered(true);
    }

    public void addLoss(ConquestBattle event) {
        ConquestPlaneData planeData = getPlaneData(event.getLocation().getPlane());
        if (planeData == null) { return; }

        planeData.addLoss(event);
        getSelectedCommander().getRecord().addLoss();
    }

    public int getAEtherShards() {
        return aetherShards;
    }
    public void rewardAEtherShards(int shards) {
        aetherShards += shards;
    }
    public boolean spendAEtherShards(int shards) {
        if (aetherShards >= shards) {
            aetherShards -= shards;
            return true;
        }
        return false;
    }

    public int getPlaneswalkEmblems() {
        return planeswalkEmblems;
    }
    public void rewardPlaneswalkEmblems(int emblems) {
        planeswalkEmblems += emblems;
    }
    public boolean spendPlaneswalkEmblems(int emblems) {
        if (planeswalkEmblems >= emblems) {
            planeswalkEmblems -= emblems;
            return true;
        }
        return false;
    }

    public String getProgress() {
        int conquered = 0;
        int total = 0;

        for (ConquestPlane plane : FModel.getPlanes()) {
            ConquestPlaneData planeData = planeDataMap.get(plane.getName());
            if (planeData != null) {
                conquered += planeData.getConqueredCount();
            }
            total += plane.getEventCount();
        }

        return Math.round(100f * (float)conquered / (float)total) + "%";
    }

    public ConquestRecord getChaosBattleRecord() {
        return chaosBattleRecord;
    }

    public void updateStatLabels(IVConquestStats view, ConquestPlane plane) {
        int wins = 0;
        int losses = 0;
        int conqueredCount = 0;
        int totalEventCount = 0;
        int unlockedCardCount = 0;
        int totalCardCount = 0;
        int commanderCount = 0;
        int totalCommanderCount = 0;
        int planeswalkerCount = 0;
        int totalPlaneswalkerCount = 0;

        if (plane != null) {
            ConquestPlaneData planeData = planeDataMap.get(plane.getName());
            if (planeData != null) {
                wins = planeData.getTotalWins();
                losses = planeData.getTotalLosses();
                conqueredCount = planeData.getConqueredCount();
            }

            for (ConquestCommander commander : commanders) {
                if (plane.getCommanders().contains(commander.getCard())) {
                    commanderCount++;
                }
            }

            totalEventCount = plane.getEventCount();
            totalCardCount = plane.getCardPool().size();
            totalCommanderCount = plane.getCommanders().size();

            for (PaperCard card : plane.getCardPool().getAllCards()) {
                boolean unlocked = hasUnlockedCard(card);
                if (unlocked) {
                    unlockedCardCount++;
                }
                if (card.getRules().getType().isPlaneswalker()) {
                    if (unlocked) {
                        planeswalkerCount++;
                    }
                    totalPlaneswalkerCount++;
                }
            }
        }
        else {
            for (ConquestPlane p : FModel.getPlanes()) {
                if (p.isUnreachable()) {
                    continue;
                }
                ConquestPlaneData planeData = planeDataMap.get(p.getName());
                if (planeData != null) {
                    wins += planeData.getTotalWins();
                    losses += planeData.getTotalLosses();
                    conqueredCount += planeData.getConqueredCount();
                }

                totalEventCount += p.getEventCount();
                totalCardCount += p.getCardPool().size();
                totalCommanderCount += p.getCommanders().size();

                for (PaperCard card : p.getCardPool().getAllCards()) {
                    boolean unlocked = hasUnlockedCard(card);
                    if (unlocked) {
                        unlockedCardCount++;
                    }
                    if (card.getRules().getType().isPlaneswalker()) {
                        if (unlocked) {
                            planeswalkerCount++;
                        }
                        totalPlaneswalkerCount++;
                    }
                }
            }
            commanderCount = commanders.size();
        }

        view.getLblAEtherShards().setText(Localizer.getInstance().getMessage("lblAetherShards") + ": " + aetherShards);
        view.getLblPlaneswalkEmblems().setText(Localizer.getInstance().getMessage("lblPlaneswalkEmblems") + ": " + planeswalkEmblems);
        view.getLblTotalWins().setText(Localizer.getInstance().getMessage("lblTotalWins") + ": " + wins);
        view.getLblTotalLosses().setText(Localizer.getInstance().getMessage("lblTotalLosses") + ": " + losses);
        view.getLblConqueredEvents().setText(Localizer.getInstance().getMessage("lblConqueredEvents") + ": " + formatRatio(conqueredCount, totalEventCount));
        view.getLblUnlockedCards().setText(Localizer.getInstance().getMessage("lblUnlockedCards") + ": " + formatRatio(unlockedCardCount, totalCardCount));
        view.getLblCommanders().setText(Localizer.getInstance().getMessage("lblCommanders") + ": " + formatRatio(commanderCount, totalCommanderCount));
        view.getLblPlaneswalkers().setText(Localizer.getInstance().getMessage("lblPlaneswalkers") + ": " + formatRatio(planeswalkerCount, totalPlaneswalkerCount));
    }

    private String formatRatio(int numerator, int denominator) {
        if (denominator == 0) {
            return "0 / 0 (0%)";
        }
        return numerator + " / " + denominator + " (" + Math.round(100f * (float)numerator / (float)denominator) + "%)";
    }

    public void rename(final String newName) {
        name = newName;
        File directory0 = new File(ForgeConstants.CONQUEST_SAVE_DIR, name.replace(' ', '_'));
        directory.renameTo(directory0);
        directory = directory0;
        xmlFilename = directory.getPath() + ForgeConstants.PATH_SEPARATOR + XML_FILE;
    }

    public void resetNewCards() {
        newCards.clear();
    }

    private static final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare =
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            return FModel.getConquest().getModel().newCards.contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };
    private static final Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet =
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            return FModel.getConquest().getModel().newCards.contains(from.getKey()) ? "NEW" : "";
        }
    };

    public static Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, fnNewCompare, fnNewGet);
        return colOverrides;
    }

    public List<ConquestLocation> getPath(ConquestLocation destLoc) {
        PathFinder pathFinder = new PathFinder();
        return pathFinder.findPath(destLoc);
    }

    private class PathFinder {
        private final HashSet<Node> closedSet = new HashSet<>();
        private final HashSet<Node> openSet = new HashSet<>();
        private final Node[][] map;

        private PathFinder() {
            ConquestPlane plane = getCurrentPlane();
            int xMax = plane.getCols();
            int yMax = plane.getRegions().size() * plane.getRowsPerRegion();
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
                    List<ConquestLocation> path = new ArrayList<>();
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
            int y = loc.getRegionIndex() * loc.getPlane().getRowsPerRegion() + loc.getRow();
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

                int regionIndex = y / plane.getRowsPerRegion();
                int col = x;
                int row = y % plane.getRowsPerRegion();
                loc = new ConquestLocation(plane, regionIndex, row, col);
            }

            public boolean isBlocked() {
                if (blocked == null) { //determine if node is blocked one time
                    ConquestPlaneData planeData = getCurrentPlaneData();
                    if (planeData.hasConquered(loc)) {
                        blocked = false;
                    }
                    else {
                        //if location isn't conquered or bordering a conquered location, there's no path to reach it
                        blocked = true;
                        for (ConquestLocation neighbor : loc.getNeighbors()) {
                            if (planeData.hasConquered(neighbor)) {
                                blocked = false;
                                break;
                            }
                        }
                    }
                }
                return blocked;
            }
        }
    }
}
