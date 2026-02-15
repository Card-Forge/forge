package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.EnemyData;

/**
 * Utility class to split enemies.json into individual files
 */
public class SplitEnemies {
    public static void main(String[] args) {
        Json json = new Json();
        // Ensure output is standard JSON (keys quoted, commas present)
        json.setOutputType(JsonWriter.OutputType.json);

        // Update the path to your enemies.json file
        FileHandle handle = new FileHandle("forge-gui/res/adventure/common/world/enemies.json");
        if (handle.exists()) {
            Array<EnemyData> enemies = json.fromJson(Array.class, EnemyData.class, handle);
            FileHandle folder = new FileHandle("forge-gui/res/adventure/common/world/enemies/");
            folder.mkdirs();
            for (EnemyData enemy : enemies) {
                String fileName = enemy.name.replaceAll("[^a-zA-Z0-9]", "_") + ".json";
                FileHandle file = folder.child(fileName);
                file.writeString(json.prettyPrint(enemy), false);
            }
            System.out.println("Split " + enemies.size + " enemies into individual files.");
        } else {
            System.out.println("enemies.json not found.");
        }
    }
}
