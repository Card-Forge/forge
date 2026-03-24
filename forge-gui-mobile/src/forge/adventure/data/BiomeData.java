package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Current;
import forge.util.Aggregates;
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
                for (String enemyName : enemies) {
                    if (data.getName().equals(enemyName)) {
                        enemyList.add(data);
                        break;
                    }
                }
                //Adding enemy with 0 spawn rate allows quests to boost them and add to pool temporarily.
                EnemyData zeroSpawnRate = new EnemyData(data);
                zeroSpawnRate.spawnRate = 0.0f;
                enemyList.add(zeroSpawnRate);
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

    public EnemyData getExtraSpawnEnemy(float difficultyFactor) {
        //todo: implement difficultyFactor
        List<EnemyData> extraSpawnEnemies = AdventureQuestController.instance().getExtraQuestSpawns(difficultyFactor);
        if (extraSpawnEnemies.isEmpty())
            return null;
        return Aggregates.random(extraSpawnEnemies); //fallback, shouldn't reach this point but guarantee that we return something
    }

    public EnemyData getEnemy(float difficultyFactor) {
        float totalDistribution = 0.0f;
        difficultyFactor = Current.player().getStatistic().rank();      //// conpare difficulty data to on how many wins you have on your save 
        List<EnemyData> filteredEnemies = new ArrayList<>();
        for (EnemyData data : enemyList ){
            if (data.difficulty <= difficultyFactor) { 
                filteredEnemies.add(data);
                totalDistribution += data.spawnRate;
            }
        }
        // If no enemies match the criteria, fallback to a random enemy from the original list
        if (filteredEnemies.isEmpty()) {
            return Aggregates.random(enemyList);
        }

        // Perform weighted random selection
        float f = totalDistribution * rand.nextFloat();
        int i = 0;
        for (; i < filteredEnemies.size(); i++) {
            f -= filteredEnemies.get(i).spawnRate;
            if (f <= 0.0f) {
                return filteredEnemies.get(i);
            }
        }

        // Fallback, should not normally reach here
        return Aggregates.random(filteredEnemies);
    }

    private ArrayList<String> unusedTownNames;
    public String getNewTownName() {
        return Aggregates.removeRandom(getUnusedTownNames());
    }

    public ArrayList<String> getUnusedTownNames() {
        if (unusedTownNames == null) {
            unusedTownNames = WorldData.getTownNames(this.name);
        }
        return unusedTownNames;
    }
}