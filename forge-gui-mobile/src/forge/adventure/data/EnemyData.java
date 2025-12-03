package forge.adventure.data;

import forge.adventure.util.CardUtil;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameFormat;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information of enemies
 */
public class EnemyData implements Serializable {
    private static final long serialVersionUID = -3317270785183936320L;
    public String name;
    public String nameOverride;
    public String sprite;
    public String[] deck;
    public boolean copyPlayerDeck = false;
    public String ai;
    public boolean boss = false;
    public boolean flying = false;
    public boolean randomizeDeck = false;
    public float spawnRate;
    public float difficulty;
    public float speed;
    public float scale = 1.0f;
    public int life;
    public RewardData[] rewards;
    public String[] equipment;
    public String colors = "";

    public EnemyData nextEnemy;
    public int teamNumber = -1;

    public String[] questTags = new String[0];
    public float lifetime;
    public int gamesPerMatch = 1;

    public EnemyData() {
    }

    public EnemyData(EnemyData enemyData) {
        name            = enemyData.name;
        sprite          = enemyData.sprite;
        deck            = enemyData.deck;
        ai              = enemyData.ai;
        boss            = enemyData.boss;
        flying          = enemyData.flying;
        randomizeDeck   = enemyData.randomizeDeck;
        spawnRate       = enemyData.spawnRate;
        copyPlayerDeck  = enemyData.copyPlayerDeck;
        difficulty      = enemyData.difficulty;
        speed           = enemyData.speed;
        scale           = enemyData.scale;
        life            = enemyData.life;
        equipment       = enemyData.equipment;
        colors          = enemyData.colors;
        teamNumber      = enemyData.teamNumber;
        nextEnemy       = enemyData.nextEnemy == null ? null : new EnemyData(enemyData.nextEnemy);
        nameOverride    = enemyData.nameOverride == null ? "" : enemyData.nameOverride;
        questTags       = enemyData.questTags.clone();
        lifetime        = enemyData.lifetime;
        gamesPerMatch   = enemyData.gamesPerMatch;
        if (enemyData.scale == 0.0f) {
            scale = 1.0f;
        }
        if (enemyData.rewards == null) {
            rewards = null;
        } else {
            rewards = new RewardData[enemyData.rewards.length];
            for (int i = 0; i < rewards.length; i++)
                rewards[i] = new RewardData(enemyData.rewards[i]);
        }
    }

    public Deck generateDeck(boolean isFantasyMode, boolean useGeneticAI) {
        boolean canUseGeneticAI = useGeneticAI && life > 16;

        if (canUseGeneticAI && Config.instance().getSettingData().generateLDADecks) {
            GameFormat fmt = FModel.getFormats().getStandard();
            int rand = MyRandom.getRandom().nextInt(100);
            if (rand > 90) {
                fmt = FModel.getFormats().getLegacy();
            } else if (rand > 50) {
                fmt = FModel.getFormats().getModern();
            }
            return DeckgenUtil.buildLDACArchetypeDeck(fmt, true);
        }

        if (randomizeDeck) {
            return CardUtil.getDeck(Aggregates.random(deck), true, isFantasyMode, colors, life > 13, canUseGeneticAI);
        }
        return CardUtil.getDeck(deck[Current.player().getEnemyDeckNumber(this.getName(), deck.length)], true, isFantasyMode, colors, life > 13, canUseGeneticAI);
    }

    public String getName(){
        //todo: make this the default accessor for anything seen in UI
        if (nameOverride != null && !nameOverride.isEmpty())
            return nameOverride;
        if (name != null && !name.isEmpty())
            return name;
        return "(Unnamed Enemy)";
    }

    public boolean match(EnemyData other) {
        //equals() does not cover cases where data is updated to override speed, displayname, etc
        if (this.equals(other))
            return true;
        if (!this.name.equals(other.name))
            return false;
        if (questTags.length != other.questTags.length)
            return false;
        ArrayList<String> myQuestTags = new ArrayList<>(Arrays.asList(questTags));
        ArrayList<String> otherQuestTags = new ArrayList<>(Arrays.asList(other.questTags));
        myQuestTags.removeAll(otherQuestTags);
        return myQuestTags.isEmpty();
    }
}
