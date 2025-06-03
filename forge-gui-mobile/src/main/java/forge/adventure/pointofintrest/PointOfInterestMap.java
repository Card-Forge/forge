package forge.adventure.pointofintrest;

import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that hold all point of interest as a list for each chunk
 */
public class PointOfInterestMap implements SaveFileContent {

    private int numberOfChunksX;
    private int numberOfChunksY;
    int tileSize;
    int chunkSize;
    private List<PointOfInterest>[][] mapObjects;

    public PointOfInterestMap(int chunkSize, int tiles, int numberOfChunksX, int numberOfChunksY) {
        this.tileSize = tiles;
        this.chunkSize = chunkSize;
        this.numberOfChunksX = numberOfChunksX;
        this.numberOfChunksY = numberOfChunksY;
        mapObjects = new List[numberOfChunksX][numberOfChunksY];
        for (int x = 0; x < numberOfChunksX; x++) {
            for (int y = 0; y < numberOfChunksY; y++) {
                mapObjects[x][y] = new ArrayList<>();
            }
        }
    }



    public void add(PointOfInterest obj) {
        int chunkX = (int) ((obj.position.x / tileSize) / chunkSize);
        int chunkY = (int) ((obj.position.y / tileSize) / chunkSize);
        if (chunkX >= numberOfChunksX || chunkY >= numberOfChunksY || chunkX < 0 || chunkY < 0)
            return;
        mapObjects[chunkX][chunkY].add(obj);
    }
    public PointOfInterest findPointsOfInterest(String name) {
        for(List<PointOfInterest>[] poiList1:mapObjects)
        {
            for(List<PointOfInterest> poiList:poiList1)
            {
                for(PointOfInterest poi:poiList)
                {
                    if(poi.getData().name.equals(name))
                        return poi;
                }
            }
        }
        return null;
    }

    public List<PointOfInterest> getAllPointOfInterest() {
        List<PointOfInterest> allPOI = new ArrayList<>();

        for(List<PointOfInterest>[] poiList1:mapObjects)
        {
            for(List<PointOfInterest> poiList:poiList1)
            {
                allPOI.addAll(poiList);
            }
        }
        return allPOI;
    }

    public List<PointOfInterest> pointsOfInterest(int chunkX, int chunkY) {
        if (chunkX >= numberOfChunksX || chunkY >= numberOfChunksY || chunkX < 0 || chunkY < 0)
            return new ArrayList<>();
        return mapObjects[chunkX][chunkY];
    }


    @Override
    public void load(SaveFileData data) {
        numberOfChunksX=data.readInt("numberOfChunksX");
        numberOfChunksY=data.readInt("numberOfChunksY");
        tileSize=data.readInt("tileSize");
        chunkSize=data.readInt("chunkSize");

        mapObjects = new List[numberOfChunksX][numberOfChunksY];
        for (int x = 0; x < numberOfChunksX; x++) {
            for (int y = 0; y < numberOfChunksY; y++) {
                mapObjects[x][y] = new ArrayList<>();
                int arraySize=data.readInt("mapObjects["+x +"]["+y+"]");
                for(int i=0;i<arraySize;i++)
                {
                    PointOfInterest pointsOfInterest=new PointOfInterest();
                    pointsOfInterest.load(data.readSubData("mapObjects["+x +"]["+y+"]["+i+"]"));
                    mapObjects[x][y].add(pointsOfInterest);
                }
            }
        }
    }

    @Override
    public SaveFileData save() {
        SaveFileData data=new SaveFileData();

        data.store("numberOfChunksX",numberOfChunksX);
        data.store("numberOfChunksY",numberOfChunksY);
        data.store("tileSize",tileSize);
        data.store("chunkSize",chunkSize);

        for (int x = 0; x < numberOfChunksX; x++) {
            for (int y = 0; y < numberOfChunksY; y++) {
                data.store("mapObjects["+x +"]["+y+"]",mapObjects[x][y].size());
                for(int i=0;i<mapObjects[x][y].size();i++)
                {
                    data.store("mapObjects["+x +"]["+y+"]["+i+"]",mapObjects[x][y].get(i).save());
                }
            }
        }
        return data;
    }
}
