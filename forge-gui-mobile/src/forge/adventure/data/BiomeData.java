package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

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
    private final Random rand = new Random();
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
    public boolean invertHeight;
    public String[] spriteNames;
    public List<String> enemies;
    public List<String> pointsOfInterest;

    private ArrayList<EnemyData> enemyList;
    private ArrayList<PointOfInterestData> pointOfInterestList;

    public Color GetColor() {
        return Color.valueOf(color);
    }

    public ArrayList<EnemyData> getEnemyList() {
        if (enemyList == null) {
            enemyList = new ArrayList<EnemyData>();
            if (enemies == null)
                return enemyList;
            for (EnemyData data : new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
                if (enemies.contains(data.name)) {
                    enemyList.add(data);
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
                if (pointsOfInterest.contains(data.name)) {
                    pointOfInterestList.add(data);
                }
            }
        }
        return pointOfInterestList;
    }

    public EnemyData getEnemy(float difficultyFactor) {
        EnemyData bestData=null;
        float biggestNumber=0;
        for (EnemyData data : enemyList) {
            float newNumber=data.spawnRate *rand.nextFloat()*difficultyFactor;
            if (newNumber>biggestNumber) {
                biggestNumber=newNumber;
                bestData=data;
            }
        }
        return bestData;
    }
}