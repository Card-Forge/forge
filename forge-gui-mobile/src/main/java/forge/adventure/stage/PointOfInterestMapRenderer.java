package forge.adventure.stage;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;

/**
 * Custom renderer to render the game stage between the map layers of a tiled map
 */
public class PointOfInterestMapRenderer extends OrthogonalTiledMapRendererBleeding {
    private final MapStage stage;

    public PointOfInterestMapRenderer(MapStage stage) {
        super(null, stage.getBatch());
        this.stage = stage;

    }

    @Override
    public void render() {
        Camera camera = stage.getCamera();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        beginRender();
        for (MapLayer layer : map.getLayers()) {
            renderMapLayer(layer);
            if (layer == stage.getSpriteLayer()) {
                stage.draw(batch);
            }
        }
        endRender();
    }

    public void loadMap(TiledMap map, String sourceMap, String targetMap, int spawnPoint) {
        stage.loadMap(map, sourceMap, targetMap, spawnPoint);

        super.setMap(map);
    }
}
