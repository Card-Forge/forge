package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

import forge.assets.FSkinProp;
import forge.util.Aggregates;

public class ConquestPlaneMap {
    //use 2 dimensional array to represent tile grid
    private final int gridSize;
    private final HexagonTile[][] grid;
    private int tileCount;
    private int tileWidth, tileHeight, padding;

    public ConquestPlaneMap(ConquestPlane plane) {
        int size = (int)Math.round(Math.sqrt(plane.getCardPool().size() / 3)); //use card pool size to determine grid size
        if (size % 2 == 0) {
            size++; //ensure grid size is an odd number
        }
        gridSize = size;
        grid = new HexagonTile[gridSize][gridSize];
        generateRandomGrid();
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

    private void generateRandomGrid() {
        int startQ = gridSize / 2; //player will start at center of grid always
        int startR = startQ;
        int colorIndex = 0; //TODO: Determine initial color index from planeswalker
        addTile(startQ, startR, colorIndex);

        int max = gridSize - 1;
        int minCount = Math.round(gridSize * gridSize * 0.75f); //ensure at least 75% of the grid has tiles
        while (tileCount < minCount) {
            //add a tile in a random location and then ensure it can be reached from start tile
            int q = Aggregates.randomInt(0, max);
            int r = Aggregates.randomInt(0, max);
            while (addTile(q, r, colorIndex)) {
                //alternate which coordinate is incremented as we move towards center
                if (tileCount % 2 == 0 && r != startR) {
                    if (r > startR) {
                        r--;
                    }
                    else {
                        r++;
                    }
                }
                else if (q > startQ) {
                    q--;
                }
                else if (q < startQ) {
                    q++;
                }
                else if (r > startR) {
                    r--;
                }
                else {
                    r++;
                }
            }
            colorIndex = Aggregates.randomInt(0, 4); //pick a random color for the next set of tiles
        }
    }

    private boolean addTile(int q, int r, int colorIndex) {
        if (grid[q][r] == null) {
            grid[q][r] = new HexagonTile(q, r, colorIndex);
            tileCount++;
            return true;
        }
        return false;
    }

    public class HexagonTile {
        private final int q, r;
        private final int colorIndex;

        private HexagonTile(int q0, int r0, int colorIndex0) {
            q = q0;
            r = r0;
            colorIndex = colorIndex0;
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
