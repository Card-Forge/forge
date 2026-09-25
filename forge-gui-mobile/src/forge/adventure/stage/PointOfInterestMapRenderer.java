package forge.adventure.stage;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

/**
 * Custom renderer to render the game stage between the map layers of a tiled map
 */
public class PointOfInterestMapRenderer extends OrthogonalTiledMapRenderer {
    private final MapStage stage;

    public PointOfInterestMapRenderer(MapStage stage) {
        super(null, stage.getBatch());
        this.stage = stage;
    }

    @Override
    public void render() {
        if (map == null) {
            return;
        }

        beginRender();

        MapLayers layers = map.getLayers();
        int layerCount = layers.getCount();
        MapLayer spriteTargetLayer = stage.getSpriteLayer();

        for (int i = 0; i < layerCount; i++) {
            MapLayer layer = layers.get(i);
            if (layer == null) {
                continue;
            }

            renderMapLayer(layer);

            if (layer == spriteTargetLayer) {
                stage.draw(batch);
            }
        }

        endRender();
    }

    public void loadMap(TiledMap map, String sourceMap, String targetMap, int spawnPoint) {
        stage.loadMap(map, sourceMap, targetMap, spawnPoint);
        super.setMap(map);
    }

    public void updateCamera() {
        Camera camera = stage.getCamera();
        camera.update();
        // with the unified SpriteBatch, you need to reset the color or you may have
        // semi/transparent texture set by other scenes
        batch.setColor(Color.WHITE);
        batch.setProjectionMatrix(camera.combined);
    }
}
