package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

import forge.assets.FSkinProp;
import forge.card.MagicColor;
import forge.planarconquest.ConquestPlane.Region;
import forge.util.Aggregates;
import forge.util.FCollectionView;

public class ConquestPlaneMap {
    private static final double sqrt3 = (float)Math.sqrt(3);

    //use 2 dimensional array to represent tile grid
    private final int gridSize;
    private final HexagonTile[][] grid;
    private int tileCount;
    private int tileWidth, tileHeight, padding;

    public ConquestPlaneMap(ConquestPlane plane) {
        int size = (int)Math.round(Math.sqrt(plane.getCardPool().size() / 2)); //use card pool size to determine grid size
        if (size % 2 == 0) {
            size++; //ensure grid size is an odd number
        }
        gridSize = size;
        grid = new HexagonTile[gridSize][gridSize];
        generateRandomGrid(plane);
    }

    public void setTileWidth(int tileWidth0) {
        if (tileWidth == tileWidth0) { return; }
        tileWidth = tileWidth0;
        tileHeight = Math.round((float)tileWidth * (float)FSkinProp.IMG_HEXAGON_TILE.getHeight() / (float)FSkinProp.IMG_HEXAGON_TILE.getWidth());
        padding = tileHeight / 2;
    }

    public int getWidth() {
        return tileWidth * 3 / 4 * (gridSize - 1) + tileWidth + 2 * padding;
    }
    public int getHeight() {
        return tileHeight * gridSize + tileHeight / 2 + 2 * padding;
    }

    public HexagonTile getTileAtPoint(float x, float y) {
        //convert pixel to axial coordinates
        x = (x - tileWidth / 2) / tileWidth - padding;
        double t1 = y / (tileWidth / 2), t2 = Math.floor(x + t1);
        int r = (int)Math.floor((Math.floor(t1 - x) + t2) / 3); 
        int q = (int)Math.floor((Math.floor(2 * x + 1) + t2) / 3) - r;

        //convert axial to offset coordinates
        r += (q - (q & 1)) / 2;

        if (r < 0 || q < 0 || q >= gridSize || r >= gridSize) {
            return null;
        }
        return grid[q][r];
    }

    public void draw(IPlaneMapRenderer renderer, int scrollLeft, int scrollTop, int visibleWidth, int visibleHeight) {
        scrollLeft -= padding;
        scrollTop -= padding;

        int dx = tileWidth * 3 / 4;
        int startQ = getIndexInRange((int)Math.floor((float)(scrollLeft - tileWidth) / (float)dx) + 1);
        int startR = getIndexInRange((int)Math.floor((float)(scrollTop - tileHeight * 1.5f) / (float)tileHeight) + 1);
        int endQ = getIndexInRange((int)Math.floor((float)(scrollLeft + visibleWidth - tileWidth) / (float)dx) + 2);
        int endR = getIndexInRange((int)Math.floor((float)(scrollTop + visibleHeight - tileHeight * 1.5f) / (float)tileHeight) + 2);

        int y;
        int x = startQ * dx - scrollLeft;
        int startY = startR * tileHeight - scrollTop;
        for (int q = startQ; q <= endQ; q++) {
            y = startY;
            if (q % 2 == 1) {
                y += tileHeight / 2; //odd columns start lower
            }
            for (int r = startR; r <= endR; r++) {
                HexagonTile tile = grid[q][r];
                if (tile != null) {
                    renderer.draw(tile, x, y, tileWidth, tileHeight);
                }
                y += tileHeight;
            }
            x += dx;
        }
    }

    private int getIndexInRange(int index) {
        if (index < 0) {
            return 0;
        }
        if (index >= gridSize) {
            return gridSize - 1;
        }
        return index;
    }

    public static interface IPlaneMapRenderer {
        void draw(HexagonTile tile, int x, int y, int w, int h);
    }

    private void generateRandomGrid(ConquestPlane plane) {
        int center = gridSize / 2; //player will start at center of grid always
        addTile(center, center, null);

        //divide the grid into regions
        FCollectionView<Region> regions = plane.getRegions();
        int regionCount = regions.size();
        double regionAngle = 2 * Math.PI / regionCount;

        //these assume a tile width of 2, radius of 1
        double centerX = 1.5 * center;
        double centerY = sqrt3 * (center + 0.5f * (center & 1));
        double maxDist = (centerX + centerY) / 2;

        //create a circular map, divided into regions
        for (int q = 0; q < gridSize; q++) {
            for (int r = 0; r < gridSize; r++) {
                double x = 1.5 * q;
                double y = sqrt3 * (r + 0.5f * (q & 1));
                double dx = x - centerX;
                double dy = y - centerY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > maxDist) {
                    continue;
                }
                double angle = Math.atan2(dy, dx) + Math.PI;
                int regionIndex = (int)Math.floor(angle / regionAngle);
                if (regionIndex >= regionCount) {
                    regionIndex = 0;
                }
                addTile(q, r, regions.get(regionIndex));
            }
        }
    }

    private boolean addTile(int q, int r, Region region) {
        if (grid[q][r] == null) {
            grid[q][r] = new HexagonTile(q, r, region);
            tileCount++;
            return true;
        }
        return false;
    }

    public class HexagonTile {
        private final int q, r;
        private final Region region;
        private final int colorIndex;

        private HexagonTile(int q0, int r0, Region region0) {
            q = q0;
            r = r0;
            region = region0;
            if (region == null) {
                colorIndex = 0;
            }
            else if (region.getColorSet().isColorless()) {
                colorIndex = 0;
            }
            else if (region.getColorSet().isMonoColor()) {
                int index;
                byte color = region.getColorSet().getColor();
                for (index = 0; index < 5; index++) {
                    if (MagicColor.WUBRG[index] == color) {
                        break;
                    }
                }
                colorIndex = index;
            }
            else { //for multicolor regions, choose one of the colors at random
                int index;
                while (true) {
                    index = Aggregates.randomInt(0, 4);
                    if (region.getColorSet().hasAnyColor(MagicColor.WUBRG[index])) {
                        break;
                    }
                }
                colorIndex = index;
            }
        }

        public int getColorIndex() {
            return colorIndex;
        }

        public List<HexagonTile> getNeighbors() {
            List<HexagonTile> neighbors = new ArrayList<HexagonTile>();
            addToList(q, r - 1, neighbors); //tile above
            addToList(q, r + 1, neighbors); //tile below
            if (q % 2 == 0) {
                addToList(q - 1, r - 1, neighbors); //tile left and up
                addToList(q - 1, r, neighbors); //tile left and down
                addToList(q + 1, r - 1, neighbors); //tile right and up
                addToList(q + 1, r, neighbors); //tile right and down
            }
            else {
                addToList(q - 1, r, neighbors); //tile left and up
                addToList(q - 1, r + 1, neighbors); //tile left and down
                addToList(q + 1, r, neighbors); //tile right and up
                addToList(q + 1, r + 1, neighbors); //tile right and down
            }
            return neighbors;
        }
    }

    private void addToList(int q, int r, List<HexagonTile> list) {
        if (r < 0 || q < 0 || q >= gridSize || r >= gridSize) {
            return;
        }
        HexagonTile tile = grid[q][r];
        if (tile != null) {
            list.add(tile);
        }
    }
}
