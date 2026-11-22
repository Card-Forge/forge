package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.adventure.data.RewardData;
import forge.adventure.util.JSONStringLoader;
import forge.adventure.util.Reward;

import java.util.HashMap;

/**
 * RewardSprite
 * Character sprite that represents reward pickups.
 */

public class RewardSprite extends CharacterSprite {
    private final static String default_reward = "[\n" +
            "\t\t{\n" +
            "\t\t\t\"type\": \"gold\",\n" +
            "\t\t\t\"count\": 10,\n" +
            "\t\t\t\"addMaxCount\": 100,\n" +
            "\t\t}\n" +
            "\t]";

    private static final HashMap<String, RewardData[]> rewardJsonMap = new HashMap<>(256);
    private final Array<Reward> rewardCollection = new Array<>(8);
    private boolean isMapPopulated = false;

    private int id;
    private RewardData[] rewards = null;

    public RewardSprite(String data, String _sprite){
        super(_sprite);

        final String cacheKey = (data != null) ? data : default_reward;

        RewardData[] cachedData = rewardJsonMap.get(cacheKey);
        if (cachedData == null) {
            if (data != null) {
                cachedData = JSONStringLoader.parse(RewardData[].class, data, default_reward);
            } else { // Shouldn't happen, but make sure it doesn't fly by.
                System.err.print("Reward data is null. Using a default reward.");
                cachedData = JSONStringLoader.parse(RewardData[].class, default_reward, default_reward);
            }
            rewardJsonMap.put(cacheKey, cachedData);
        }

        this.rewards = cachedData;
    }

    public RewardSprite(int _id, String data, String _sprite){
        this(data, _sprite);
        this.id = _id; // The ID is for remembering removals.
    }

    @Override
    void updateBoundingRect() { // We want rewards to take a full tile.
        boundingRect.set(getX(), getY(), getWidth(), getHeight());
    }

    // act() -> onActing() MapStage call
    public Array<Reward> getRewards() {
        // Only assemble the reward data array objects once on the initial request pass
        if (!isMapPopulated) {
            isMapPopulated = true;
            rewardCollection.clear();

            if (rewards != null) {
                for (int i = 0; i < rewards.length; i++) {
                    RewardData rdata = rewards[i];
                    if (rdata != null) {
                        rewardCollection.addAll(rdata.generate(false, true));
                    }
                }
            }
        }
        return rewardCollection;
    }

    public int getId() {
        return id;
    }
}
