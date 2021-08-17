package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class BiomData implements Serializable {
    private final Random rand = new Random();
    public double startPointX;
    public double startPointY;
    public double noiceWeight;
    public double distWeight;
    public String name;
    public String tileset;
    public double width;
    public double height;
    public String color;
    public boolean invertHeight;
    public List<String> spriteNames;
    public List<String> enemies;
    public List<String> pointsOfIntrest;

    private Array<EnemyData> enemyList;
    private Array<PointOfIntrestData> pointOfIntrestList;

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

    public Array<PointOfIntrestData> getPointsOfIntrest() {
        if (pointOfIntrestList == null) {
            pointOfIntrestList = new Array<PointOfIntrestData>();
            if (pointsOfIntrest == null)
                return pointOfIntrestList;
            Json json = new Json();
            FileHandle handle = forge.adventure.util.Res.CurrentRes.GetFile("world/pointsOfIntrest.json");
            if (handle.exists()) {
                Array<PointOfIntrestData> alltowns = json.fromJson(Array.class, PointOfIntrestData.class, handle);
                for (PointOfIntrestData data : alltowns) {
                    if (pointsOfIntrest.contains(data.name)) {
                        pointOfIntrestList.add(data);
                    }
                }
            }

        }
        return pointOfIntrestList;
    }

    public EnemyData getEnemy(float spawnFactor, float difficultyFactor) {
        float randValue = rand.nextFloat() * spawnFactor;
        for (EnemyData data : enemyList) {
            if (randValue >= data.spawnRate && data.difficulty < difficultyFactor) {
                return data;
            }
        }
        return null;
    }
}