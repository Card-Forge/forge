package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

import forge.planarconquest.ConquestPlane.Region;

public class ConquestLocation {
    private ConquestPlane plane;
    private int regionIndex;
    private int row;
    private int col;

    public ConquestLocation() {
    }

    public ConquestPlane getPlane() {
        return plane;
    }

    public Region getRegion() {
        if (regionIndex == -1 || regionIndex == plane.getRegions().size()) {
            return null; //indicates we're on portal row
        }
        return plane.getRegions().get(regionIndex);
    }

    public int getRegionIndex() {
        return regionIndex;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void travelToPlane(ConquestPlane plane0) {
        if (plane == plane0) { return; }
        if (plane == null /*|| plane0 follows plane in travel order */) {
            regionIndex = -1; //start on bottom portal row
        }
        else {
            regionIndex = plane.getRegions().size(); //start on top portal row
        }
        plane = plane0;
    }

    public List<ConquestLocation> getNeighbors() {
        return getNeighbors(regionIndex, row, col);
    }
    public List<ConquestLocation> getNeighbors(int regionIndex0, int row0, int col0) {
        List<ConquestLocation> locations = new ArrayList<ConquestLocation>();
        if (row0 > 0) {
            
        }
        else if (regionIndex0 > 0) {
            
        }
        return locations;
    }
}
