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
    public float spawnRate;
    public float difficulty;
    public float speed;
    public int life;
    public RewardData[] rewards;

    public EnemyData()
    {

    }
    public EnemyData(EnemyData enemyData) {
        name        =enemyData.name;
        sprite      =enemyData.sprite;
        deck        =enemyData.deck;
        spawnRate   =enemyData.spawnRate;
        difficulty  =enemyData.difficulty ;
        speed       =enemyData.speed;
        life        =enemyData.life;
        if(enemyData.rewards==null)
        {
            rewards=null;
        }
        else
        {
            rewards     =new RewardData[enemyData.rewards.length];
            for(int i=0;i<rewards.length;i++)
                rewards[i]=new RewardData(enemyData.rewards[i]);
        }
    }

    public Deck generateDeck() {
        return CardUtil.getDeck(deck);
    }
}
