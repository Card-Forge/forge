package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import forge.adventure.util.AdventureQuestController;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
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
                for (String enemyName : enemies) {
                    if (data.getName().equals(enemyName)) {
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
            if (pointsOfInterest == null)
                return pointOfInterestList;
            Array<PointOfInterestData> allTowns = PointOfInterestData.getAllPointOfInterest();
            for (PointOfInterestData data : new Array.ArrayIterator<>(allTowns)) {
                for (String poiName : pointsOfInterest) {
                    if (data.name.equals(poiName)) {
                        pointOfInterestList.add(data);
                        break;
                    }
                }
            }
        }
        ArrayList<PointOfInterestData> cavesDungeon = new ArrayList<>();
        for (PointOfInterestData data : pointOfInterestList) {
            if ("cave".equalsIgnoreCase(data.type) || "dungeon".equalsIgnoreCase(data.type)) {
                cavesDungeon.add(data);
            }
        }
        pointOfInterestList.removeAll(cavesDungeon);
        pointOfInterestList.addAll(cavesDungeon); //move to bottom..
        return pointOfInterestList;
    }

    public EnemyData getEnemy(float difficultyFactor) {
        //todo: implement difficultyFactor
        Map<String, Float> boostedSpawns = AdventureQuestController.instance().getBoostedSpawns(enemyList);
        float totalDistribution = 0.0f;
        for (EnemyData data : enemyList) {
            float boost = boostedSpawns.getOrDefault(data.getName(), 0.0f); //Each active quest stage will divide 2.0f across any valid enemies to defeat
            totalDistribution += data.spawnRate + boost;
        }
        int i = 0;
        for (float f = totalDistribution * rand.nextFloat(); i < enemyList.size(); i++)
        {
            f -= ( enemyList.get(i).spawnRate + boostedSpawns.getOrDefault(enemyList.get(i).getName(), 0.0f));
            if (f <= 0.0f){
                return enemyList.get(i);
            }
        }
        return Aggregates.random(enemyList); //fallback, shouldn't reach this point but guarantee that we return something
    }
}