package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.Forge;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.BiomeData;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Current;
import forge.adventure.world.World;
import forge.util.TextUtil;

import java.util.List;

/**
 * Game scene main over world scene
 * does render the WorldStage and HUD
 */
public class GameScene extends HudScene {
    public GameScene() {
        super(WorldStage.getInstance());

    }


    private static GameScene object;

    public static GameScene instance() {
        if(object==null)
            object=new GameScene();
        return object;
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
        hud.draw();
    }


    @Override
    public void enter() {
        MapStage.getInstance().clearIsInMap();
        Forge.clearTransitionScreen();
        Forge.clearCurrentScreen();
        super.enter();
        WorldStage.getInstance().handlePointsOfInterestCollision();
    }
    public String getAdventurePlayerLocation(boolean forHeader) {
        String location = "";
        if(MapStage.getInstance().isInMap()) {
            location = forHeader? TileMapScene.instance().rootPoint.getData().name : TileMapScene.instance().rootPoint.getData().type;
        } else {
            World world= Current.world();
            PlayerSprite player = WorldStage.getInstance().getPlayerSprite();
            int currentBiome = World.highestBiome(world.getBiome((int) player.getX() / world.getTileSize(), (int) player.getY() / world.getTileSize()));
            List<BiomeData> biomeData = Current.world().getData().GetBiomes();
            try {
                BiomeData data = biomeData.get(currentBiome);
                location = forHeader? TextUtil.capitalize(data.name)+" Map" : data.name;
            } catch (Exception e) {
                e.printStackTrace();
                location = "";
            }
        }
        return location;
    }
}

