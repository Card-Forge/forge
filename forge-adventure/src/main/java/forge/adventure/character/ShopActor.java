package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.SceneType;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Reward;

/**
 * Map actor that will open the Shop on collision
 */
public class ShopActor extends MapActor{
    private final MapStage stage;
    private final int id;
    Array<Reward> rewardData;

    public ShopActor(MapStage stage, int id, Array<Reward> rewardData)
    {
        this.stage = stage;
        this.id = id;
        this.rewardData = rewardData;

    }

    public MapStage getMapStage()
    {
        return stage;
    }

    @Override
    public void  onPlayerCollide()
    {

        stage.GetPlayer().stop();
        ((RewardScene) SceneType.RewardScene.instance).loadRewards(rewardData, RewardScene.Type.Shop,this);
        AdventureApplicationAdapter.instance.switchScene(SceneType.RewardScene.instance);
    }

    public int getObjectID() {
        return id;
    }
}
