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

    private ConquestLocation(ConquestPlane plane0, int regionIndex0, int row0, int col0) {
        plane = plane0;
        regionIndex = regionIndex0;
        row = row0;
        col = col0;
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
        plane = plane0;
        regionIndex = -1; //start on bottom portal row
        row = 0;
        col = Region.PORTAL_COL;
    }

    public List<ConquestLocation> getNeighbors() {
        return getNeighbors(regionIndex, row, col);
    }
    public List<ConquestLocation> getNeighbors(int regionIndex0, int row0, int col0) {
        int regionCount = plane.getRegions().size();
        List<ConquestLocation> locations = new ArrayList<ConquestLocation>();

        //add location above
        if (row0 < Region.ROWS_PER_REGION - 1) {
            locations.add(new ConquestLocation(plane, regionIndex0, row0 + 1, col0));
        }
        else if (regionIndex0 < regionCount - 1) {
            locations.add(new ConquestLocation(plane, regionIndex0 + 1, 0, col0));
        }
        else if (regionIndex0 == regionCount - 1 && col0 == Region.PORTAL_COL) {
            locations.add(new ConquestLocation(plane, regionCount, 0, col0));
        }

        //add location below
        if (row0 > 0) {
            locations.add(new ConquestLocation(plane, regionIndex0, row0 - 1, col0));
        }
        else if (regionIndex0 > 0) {
            locations.add(new ConquestLocation(plane, regionIndex0 - 1, Region.ROWS_PER_REGION - 1, col0));
        }
        else if (regionIndex0 == 0 && col0 == Region.PORTAL_COL) {
            locations.add(new ConquestLocation(plane, -1, 0, col0));
        }

        //add locations left and right
        if (regionIndex0 >= 0 && regionIndex0 < regionCount) { //not options in portal row
            if (col0 > 0) {
                locations.add(new ConquestLocation(plane, regionIndex0, row0, col0 - 1));
            }
            if (col0 < Region.COLS_PER_REGION - 1) {
                locations.add(new ConquestLocation(plane, regionIndex0, row0, col0 + 1));
            }
        }

        return locations;
    }
}
