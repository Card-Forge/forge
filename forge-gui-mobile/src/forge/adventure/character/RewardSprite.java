package forge.adventure.character;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.RewardData;
import forge.adventure.util.JSONStringLoader;
import forge.adventure.util.Reward;

/**
 * RewardSprite
 * Character sprite that represents reward pickups.
 */

public class RewardSprite extends CharacterSprite {
    private final String default_reward = "[\n" +
            "\t\t{\n" +
            "\t\t\t\"type\": \"gold\",\n" +
            "\t\t\t\"count\": 10,\n" +
            "\t\t\t\"addMaxCount\": 100,\n" +
            "\t\t}\n" +
            "\t]";

    private int id;
    private RewardData[] rewards = null;

    public RewardSprite(String data, String _sprite){
        super(_sprite);
        if (data != null) {
            rewards = JSONStringLoader.parse(RewardData[].class, data, default_reward);
        } else { //Shouldn't happen, but make sure it doesn't fly by.
            System.err.printf("Reward data is null. Using a default reward.");
            rewards = JSONStringLoader.parse(RewardData[].class, default_reward, default_reward);
        }
    }

    public RewardSprite(int _id, String data, String _sprite){
        this(data, _sprite);
        this.id = _id; //The ID is for remembering removals.
    }

    @Override
    void updateBoundingRect() { //We want rewards to take a full tile.
        boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public Array<Reward> getRewards() { //Get list of rewards.
        Array<Reward> ret = new Array<Reward>();
        if(rewards == null) return ret;
        for(RewardData rdata:rewards) {
            ret.addAll(rdata.generate(false));
        }
        return ret;
    }

    public int getId() {
        return id;
    }
}
