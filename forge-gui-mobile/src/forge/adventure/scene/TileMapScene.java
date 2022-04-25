package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.maps.tiled.TiledMap;
import forge.Forge;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.PointOfInterestMapRenderer;
import forge.adventure.util.Config;
import forge.adventure.util.TemplateTmxMapLoader;
import forge.adventure.world.WorldSave;

/**
 * Scene that will render tiled maps.
 * Used for towns dungeons etc
 */
public class TileMapScene extends HudScene {
    TiledMap map;
    PointOfInterestMapRenderer tiledMapRenderer;
    private String nextMap;
    private float cameraWidth = 0f, cameraHeight = 0f;

    public TileMapScene() {
        super(MapStage.getInstance());
        tiledMapRenderer = new PointOfInterestMapRenderer((MapStage) stage);
    }

    public MapStage currentMap() {
        return (MapStage) stage;
    }

    @Override
    public void dispose() {
        if (map != null)
            map.dispose();
    }

    @Override
    public void act(float delta) {
        if (map == null)
            return;
        if (nextMap != null) {
            load(nextMap);
            nextMap = null;
        }
        stage.act(Gdx.graphics.getDeltaTime());
        hud.act(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void render() {
        if (map == null)
            return;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        tiledMapRenderer.setView(stage.getCamera().combined, stage.getCamera().position.x - Scene.getIntendedWidth() / 2.0f, stage.getCamera().position.y - Scene.getIntendedHeight() / 2.0f, Scene.getIntendedWidth(), Scene.getIntendedHeight());

        if (!Forge.isLandscapeMode()) {
            stage.getCamera().position.x = stage.GetPlayer().pos().x;
        }
        tiledMapRenderer.render();
        hud.draw();
    }

    @Override
    public void resLoaded() {
        MapStage.getInstance().resLoaded();
        //set initial camera width and height
        if (cameraWidth == 0f)
            cameraWidth = stage.getCamera().viewportWidth;
        if (cameraHeight == 0f)
            cameraHeight = stage.getCamera().viewportHeight;
        MapStage.getInstance().setDialogStage(hud);
        super.resLoaded();
    }

    @Override
    public void enter() {
        super.enter();
    }

    public void load(PointOfInterest point) {
        rootPoint = point;
        oldMap = point.getData().map;
        map = new TemplateTmxMapLoader().load(Config.instance().getFilePath(point.getData().map));
        ((MapStage) stage).setPointOfInterest(WorldSave.getCurrentSave().getPointOfInterestChanges(point.getID() + oldMap));
        stage.GetPlayer().setPosition(0, 0);
        WorldSave.getCurrentSave().getWorld().setSeed(point.getSeedOffset());
        tiledMapRenderer.loadMap(map, "");
    }

    PointOfInterest rootPoint;
    String oldMap;

    private void load(String targetMap) {
        map = new TemplateTmxMapLoader().load(Config.instance().getFilePath(targetMap));
        ((MapStage) stage).setPointOfInterest(WorldSave.getCurrentSave().getPointOfInterestChanges(rootPoint.getID() + targetMap));
        stage.GetPlayer().setPosition(0, 0);
        WorldSave.getCurrentSave().getWorld().setSeed(rootPoint.getSeedOffset());
        tiledMapRenderer.loadMap(map, oldMap);
        oldMap = targetMap;
    }


    @Override
    public boolean isInHudOnlyMode() {
        return MapStage.getInstance().getDialogOnlyInput();
    }

    public void loadNext(String targetMap) {
        nextMap = targetMap;
    }
}

