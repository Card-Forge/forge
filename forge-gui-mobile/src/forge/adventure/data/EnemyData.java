package forge.adventure.data;

import forge.adventure.util.CardUtil;
import forge.deck.Deck;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information of enemies
 */
public class EnemyData {
    public String name;
    public String sprite;
    public String deck;
    public boolean copyPlayerDeck = false;
    public String ai;
    public float spawnRate;
    public float difficulty;
    public float speed;
    public int life;
    public RewardData[] rewards;
    public String[] equipment;
    public String colors = "";

    public EnemyData() { }
    public EnemyData(EnemyData enemyData) {
        name           = enemyData.name;
        sprite         = enemyData.sprite;
        deck           = enemyData.deck;
        ai             = enemyData.ai;
        spawnRate      = enemyData.spawnRate;
        copyPlayerDeck = enemyData.copyPlayerDeck;
        difficulty     = enemyData.difficulty;
        speed          = enemyData.speed;
        life           = enemyData.life;
        equipment      = enemyData.equipment;
        colors         = enemyData.colors;
        if(enemyData.rewards == null) {
            rewards=null;
        } else {
            rewards = new RewardData[enemyData.rewards.length];
            for(int i=0; i<rewards.length; i++)
                rewards[i]=new RewardData(enemyData.rewards[i]);
        }
    }

    public Deck generateDeck(boolean isFantasyMode) {
        return CardUtil.getDeck(deck, true, isFantasyMode);
    }
}
