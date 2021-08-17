package forge.adventure.world;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PointOfIntrestMap implements Serializable {

    private final int numberOfChunks;
    int tileSize;
    int chunkSize;
    private List<PointOfIntrest>[][] mapObjects;

    PointOfIntrestMap(int chunkSize, int tiles, int numberOfChunks) {
        this.tileSize = tiles;
        this.chunkSize = chunkSize;
        this.numberOfChunks = numberOfChunks;
        mapObjects = new List[numberOfChunks][numberOfChunks];
        for (int x = 0; x < numberOfChunks; x++) {
            for (int y = 0; y < numberOfChunks; y++) {
                mapObjects[x][y] = new ArrayList();
            }
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        out.writeObject(mapObjects);
        out.writeInt(tileSize);
        out.writeInt(chunkSize);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        mapObjects = (List<PointOfIntrest>[][]) in.readObject();
        tileSize = in.readInt();
        chunkSize = in.readInt();


    }

    public void add(PointOfIntrest obj) {
        int chunkX = (int) ((obj.position.x / tileSize) / chunkSize);
        int chunkY = (int) ((obj.position.y / tileSize) / chunkSize);
        if (chunkX >= numberOfChunks || chunkY >= numberOfChunks || chunkX < 0 || chunkY < 0)
            return;
        mapObjects[chunkX][chunkY].add(obj);
    }

    public List<PointOfIntrest> pointsOfIntrest(int chunkX, int chunky) {
        return mapObjects[chunkX][chunky];
    }
}
