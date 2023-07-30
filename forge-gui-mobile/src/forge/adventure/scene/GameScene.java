package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.google.common.collect.Sets;
import forge.Forge;
import forge.adventure.data.BiomeData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Current;
import forge.adventure.world.World;
import forge.util.TextUtil;

import java.util.List;
import java.util.Set;

/**
 * Game scene main over world scene
 * does render the WorldStage and HUD
 */
public class GameScene extends HudScene {
    public GameScene() {
        super(WorldStage.getInstance());
    }

    private static GameScene object;
    private String location = "";

    public static GameScene instance() {
        if (object == null)
            object = new GameScene();
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

    public String getAdventurePlayerLocation(boolean forHeader, boolean skipRoads) {
        if (MapStage.getInstance().isInMap()) {
            location = forHeader ? TileMapScene.instance().rootPoint.getData().name : TileMapScene.instance().rootPoint.getData().type;
        } else {
            World world = Current.world();
            //this gets the name of the layer... this shoud be based on boundaries...
            int currentBiome = World.highestBiome(world.getBiomeMapXY((int) stage.getPlayerSprite().getX() / world.getTileSize(), (int) stage.getPlayerSprite().getY() / world.getTileSize()));
            List<BiomeData> biomeData = world.getData().GetBiomes();
            if (biomeData.size() <= currentBiome) //shouldn't be the case but default to waste
                if (skipRoads) {
                    location = forHeader ? "Waste Map" : "waste";
                } else {
                    location = "";
                }
            else {
                BiomeData data = biomeData.get(currentBiome);
                location = forHeader ? TextUtil.capitalize(data.name) + " Map" : data.name;
            }
        }
        return location;
    }

    public PointOfInterest getMapPOI() {
        if (MapStage.getInstance().isInMap()) {
            return TileMapScene.instance().rootPoint;
        }
        return null;
    }

    public boolean isNotInWorldMap() {
        String location = getAdventurePlayerLocation(false, true);
        Set<String> locationTypes = Sets.newHashSet("capital", "castle", "cave", "dungeon", "town");
        return locationTypes.contains(location);
    }
}

