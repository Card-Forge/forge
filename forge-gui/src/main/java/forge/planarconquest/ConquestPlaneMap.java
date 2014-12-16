package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

import forge.util.Aggregates;

public class ConquestPlaneMap {
    //use 2 dimensional array to represent tile grid
    private final int gridSize;
    private final HexagonTile[][] grid;
    private int tileCount;

    public ConquestPlaneMap(ConquestPlane plane) {
        int size = (int)Math.round(Math.sqrt(plane.getCardPool().size() / 3)); //use card pool size to determine grid size
        if (size % 2 == 0) {
            size++; //ensure grid size is an odd number
        }
        gridSize = size;
        grid = new HexagonTile[gridSize][gridSize];
        generateRandomGrid();
    }

    private void generateRandomGrid() {
        int startQ = gridSize / 2; //player will start at center of grid always
        int startR = startQ;
        addTile(startQ, startR);

        int max = gridSize - 1;
        int minCount = Math.round(gridSize * gridSize * 0.8f); //ensure at least 80% of the grid has tiles
        while (tileCount < minCount) {
            //add a tile in a random location and then ensure it can be reached from start tile
            int q = Aggregates.randomInt(0, max);
            int r = Aggregates.randomInt(0, max);
            while (addTile(q, r)) {
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
        }
    }

    private boolean addTile(int q, int r) {
        if (grid[q][r] == null) {
            grid[q][r] = new HexagonTile(q, r);
            tileCount++;
            return true;
        }
        return false;
    }

    public class HexagonTile {
        private int q, r;

        private HexagonTile(int q0, int r0) {
            q = q0;
            r = r0;
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
        if (r < 0 || q < 0 || q >= grid.length || r >= grid[0].length) {
            return;
        }
        HexagonTile tile = grid[q][r];
        if (tile != null) {
            list.add(tile);
        }
    }
}
