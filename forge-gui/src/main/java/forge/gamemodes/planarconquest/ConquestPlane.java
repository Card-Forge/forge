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
package forge.gamemodes.planarconquest;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;

import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardEdition.CardInSet;
import forge.deck.generation.DeckGenPool;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.storage.StorageReaderFile;


public class ConquestPlane {
    private final String name;
    private final String directory;
    private final String description;
    private boolean unreachable;
    private final int rowsPerRegion;
    private final int cols;

    private FCollection<ConquestRegion> regions;
    private DeckGenPool cardPool;
    private FCollection<PaperCard> planeCards;
    private FCollection<PaperCard> commanders;
    private ConquestAwardPool awardPool;
    private ConquestEvent[] events;

    private ConquestPlane(String name0, String description0, int regionSize0, boolean unreachable0) {
        name = name0;
        directory = ForgeConstants.CONQUEST_PLANES_DIR + name + ForgeConstants.PATH_SEPARATOR;
        description = description0;
        unreachable = unreachable0;

        switch (regionSize0) {
        case 9:
            rowsPerRegion = 3;
            cols = 3;
            break;
        case 6:
            rowsPerRegion = 2;
            cols = 3;
            break;
        default:
            System.out.println(regionSize0 + " is not a valid region size");
            rowsPerRegion = 3; //fallback to max region size
            cols = 3;
            break;
        }
    }

    public String getName() {
        return name;
    }

    public String getDirectory() {
        return directory;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUnreachable() {
        return unreachable;
    }

    public void setTemporarilyReachable(boolean reachable) {
        unreachable = !reachable;
    }

    public FCollectionView<ConquestRegion> getRegions() {
        ensureRegionsLoaded();
        return regions;
    }

    public ConquestEvent getEvent(ConquestLocation loc) {
        ensureRegionsLoaded();
        return events[loc.getEventIndex()];
    }

    public int getEventCount() {
        ensureRegionsLoaded();
        return regions.size() * rowsPerRegion * cols;
    }

    public int getRowsPerRegion() {
        return rowsPerRegion;
    }

    public int getCols() {
        return cols;
    }

    public int getEventIndex(int regionIndex, int row, int col) {
        return regionIndex * rowsPerRegion * cols + col * rowsPerRegion + row;
    }

    public DeckGenPool getCardPool() {
        ensureRegionsLoaded();
        return cardPool;
    }

    public FCollectionView<PaperCard> getCommanders() {
        ensureRegionsLoaded();
        return commanders;
    }

    public FCollectionView<PaperCard> getPlaneCards() {
        if (planeCards == null) {
            planeCards = new FCollection<>();

            CardDb variantCards = FModel.getMagicDb().getVariantCards();
            List<String> planeCardNames = FileUtil.readFile(directory + "plane_cards.txt");
            for (String name : planeCardNames) {
                PaperCard pc = variantCards.getCard(name);
                if (pc == null) {
                    // try to get a non-variant Magic card in case a plane card with the given name does not exist
                    pc = FModel.getMagicDb().getCommonCards().getCard(name);
                    if (pc == null) {
                        System.out.println("\"" + name + "\" does not correspond to a valid Plane card or standard Magic card!");
                        continue;
                    }
                }
                planeCards.add(pc);
            }
        }
        return planeCards;
    }

    private void ensureRegionsLoaded() {
        if (regions != null) { return; }

        //load regions
        regions = new FCollection<>(new ConquestRegion.Reader(this));

        //load events
        int eventIndex = 0;
        int eventsPerRegion = rowsPerRegion * cols;
        int regionEndIndex = eventsPerRegion;
        events = new ConquestEvent[regions.size() * eventsPerRegion];
        for (ConquestRegion region : regions) {
            FCollection<ConquestEvent> regionEvents = new FCollection<>(new ConquestEvent.Reader(region));
            for (ConquestEvent event : regionEvents) {
                events[eventIndex++] = event;
                if (eventIndex == regionEndIndex) {
                    break;
                }
            }
            //if not enough events defined, create random events for remaining
            while (eventIndex < regionEndIndex) {
                events[eventIndex++] = new ConquestEvent(region, region.getName() + " - Random " + ((eventIndex % eventsPerRegion) + 1), null, null, EnumSet.noneOf(GameType.class), null, null);
            }
            regionEndIndex += eventsPerRegion;
        }

        //load card pool
        cardPool = new DeckGenPool();
        commanders = new FCollection<>();

        CardDb commonCards = FModel.getMagicDb().getCommonCards();
        List<String> bannedCards = FileUtil.readFile(directory + "banned_cards.txt");
        Set<String> bannedCardSet = bannedCards.isEmpty() ? null : new HashSet<>(bannedCards);

        List<String> setCodes = FileUtil.readFile(directory + "sets.txt");
        for (String setCode : setCodes) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
            if (edition != null) {
                for (CardInSet card : edition.getAllCardsInSet()) {
                    if (bannedCardSet == null || !bannedCardSet.contains(card.name)) {
                        addCard(commonCards.getCard(card.name, setCode));
                    }
                }
            }
        }

        List<String> additionalCards = FileUtil.readFile(directory + "cards.txt");
        for (String cardName : additionalCards) {
            addCard(commonCards.getCard(cardName));
        }

        //sort commanders by name
        Collections.sort(commanders);
    }

    private void addCard(PaperCard pc) {
        if (pc == null) { return; }

        cardPool.add(pc);
        if (pc.getRules().canBeCommander()) {
            commanders.add(pc);
        }
        ConquestRegion.addCard(pc, regions);
    }

    public String toString() {
        return name;
    }

    public static final Function<ConquestPlane, String> FN_GET_NAME = new Function<ConquestPlane, String>() {
        @Override
        public String apply(ConquestPlane plane) {
            return plane.getName();
        }
    };

    public ConquestAwardPool getAwardPool() {
        if (awardPool == null) { //delay initializing until needed
            awardPool = new ConquestAwardPool(cardPool.getAllCards());
        }
        return awardPool;
    }

    public static class Reader extends StorageReaderFile<ConquestPlane> {
        public Reader(String file0) {
            super(file0, ConquestPlane.FN_GET_NAME);
        }

        @Override
        protected ConquestPlane read(String line, int i) {
            String name = null;
            int regionSize = 0;
            String description = null;
            boolean unreachable = false;

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
                    name = value;
                    break;
                case "regionsize":
                    try {
                        regionSize = Integer.parseInt(value);
                    }
                    catch (Exception ex) {
                        System.out.println(value + " is not a valid region size");
                    }
                    break;
                case "unreachable":
                    unreachable = true;
                    break;
                case "desc":
                    description = value;
                    break;
                default:
                    alertInvalidLine(line, "Invalid plane definition.");
                    break;
                }
            }
            return new ConquestPlane(name, description, regionSize, unreachable);
        }
    }

    public static Set<ConquestPlane> getAllPlanesOfCard(PaperCard card) {
        Set<ConquestPlane> planes = new HashSet<>();
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (plane.cardPool.contains(card)) {
                planes.add(plane);
            }
        }
        return planes;
    }
}
