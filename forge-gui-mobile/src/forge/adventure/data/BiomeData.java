package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the biomes
 */
public class BiomeData implements Serializable {
    public float startPointX;
    public float startPointY;
    public float noiseWeight;
    public float distWeight;
    public String name;
    public String tilesetAtlas;
    public String tilesetName;
    public BiomeTerrainData[] terrain;
    public float width;
    public float height;
    public String color;
    public boolean collision;
    public boolean invertHeight;
    public String[] spriteNames;
    public String[] enemies;
    public String[] pointsOfInterest;
    public BiomeStructureData[] structures;

    private ArrayList<EnemyData> enemyList;
    private ArrayList<PointOfInterestData> pointOfInterestList;

    private final Random rand = MyRandom.getRandom();
    public Color GetColor() {
        return Color.valueOf(color);
    }

    public ArrayList<EnemyData> getEnemyList() {
        if (enemyList == null) {
            enemyList = new ArrayList<>();
            if (enemies == null)
                return enemyList;
            for (EnemyData data : new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
                for (String enemyName:enemies)
                {
                    if(data.name.equals(enemyName))
                    {
                        enemyList.add(data);
                        break;
                    }
                }
            }
        }
        return enemyList;
    }

    public ArrayList<PointOfInterestData> getPointsOfInterest() {
        if (pointOfInterestList == null) {
            pointOfInterestList = new ArrayList<PointOfInterestData>();
            if(pointsOfInterest==null)
                return pointOfInterestList;
            Array<PointOfInterestData> allTowns = PointOfInterestData.getAllPointOfInterest();
            for (PointOfInterestData data : new Array.ArrayIterator<>(allTowns)) {
                for (String poiName:pointsOfInterest)
                {
                    if(data.name.equals(poiName))
                    {
                        pointOfInterestList.add(data);
                        break;
                    }
                }
            }
        }
        return pointOfInterestList;
    }

    public EnemyData getEnemy(float difficultyFactor) {
        EnemyData bestData = null;
        float biggestNumber = 0.0f;
        for (EnemyData data : enemyList) {
            float newNumber= ( 1.0f + (data.spawnRate * rand.nextFloat()) ) * difficultyFactor;
            if (newNumber > biggestNumber) {
                biggestNumber=newNumber;
                bestData=data;
            }
        }
        return bestData;
    }
}