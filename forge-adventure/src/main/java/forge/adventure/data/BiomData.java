package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class BiomData implements Serializable {
    private final Random rand = new Random();
    public float startPointX;
    public float startPointY;
    public float noiseWeight;
    public float distWeight;
    public String name;
    public String tilesetAtlas;
    public String tilesetName;
    public BiomTerrainData[] terrain;
    public float width;
    public float height;
    public String color;
    public boolean invertHeight;
    public String[] spriteNames;
    public List<String> enemies;
    public List<String> pointsOfInterest;

    private Array<EnemyData> enemyList;
    private Array<PointOfInterestData> pointOfIntrestList;

    public Color GetColor() {
        return Color.valueOf(color);
    }

    public Array<EnemyData> GetEnemyList() {
        if (enemyList == null) {
            enemyList = new Array<EnemyData>();
            if (enemies == null)
                return enemyList;
            for (EnemyData data : WorldData.getAllEnemies()) {
                if (enemies.contains(data.name)) {
                    enemyList.add(data);
                }
            }
        }
        return enemyList;
    }

    public Array<PointOfInterestData> getPointsOfIntrest() {
        if (pointOfIntrestList == null) {
            pointOfIntrestList = new Array<PointOfInterestData>();
            if(pointsOfInterest==null)
                return pointOfIntrestList;
            Array<PointOfInterestData> alltowns = PointOfInterestData.getAllPointOfInterest();
            for (PointOfInterestData data : alltowns) {
                if (pointsOfInterest.contains(data.name)) {
                    pointOfIntrestList.add(data);
                }
            }
        }
        return pointOfIntrestList;
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