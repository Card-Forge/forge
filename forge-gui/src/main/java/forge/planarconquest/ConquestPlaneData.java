package forge.planarconquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.planarconquest.ConquestPlane.Region;

public class ConquestPlaneData {
    private final List<ConquestCommander> commanders = new ArrayList<ConquestCommander>();
    private final Map<Region, RegionData> regionDataLookup = new HashMap<Region, RegionData>();

    private int wins, losses;

    public List<ConquestCommander> getCommanders() {
        return commanders;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public RegionData getRegionData(Region region) {
        RegionData regionData = regionDataLookup.get(region);
        if (regionData == null) {
            regionData = new RegionData(region);
            regionDataLookup.put(region, regionData);
        }
        return regionData;
    }

    public class RegionData {
        private final Region region;
        private final ConquestCommander[] commanders = new ConquestCommander[4];

        private RegionData(Region region0) {
            region = region0;
            commanders[0] = region.getRandomOpponent(commanders);
            commanders[1] = region.getRandomOpponent(commanders);
            commanders[2] = region.getRandomOpponent(commanders);
            //leave commanders[3] open for deployed commander
        }

        public Region getRegion() {
            return region;
        }

        public ConquestCommander getCommander(int index) {
            return commanders[index];
        }
    }
}
