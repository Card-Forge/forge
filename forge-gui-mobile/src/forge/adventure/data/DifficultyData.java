package forge.adventure.data;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the difficulties
 */
public class DifficultyData {
    public String name="";
    public int startingLife=10;
    public int staringMoney=10;
    public float enemyLifeFactor=1;
    public boolean startingDifficulty;
    public int spawnRank = 1; //0 for "easy", 1 for "normal", 2 for "hard". To filter map spawns based on this.
    public float sellFactor=0.2f;
    public float goldLoss=0.2f;
    public float lifeLoss=0.2f;
    public String[] startItems=new String[0];
    public ObjectMap<String,String> starterDecks = null;
    public ObjectMap<String,String> constructedStarterDecks= null;

}
