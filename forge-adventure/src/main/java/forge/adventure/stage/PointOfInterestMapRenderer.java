package forge.adventure.stage;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

/**
 * Custom renderer to render the game stage between the map layers of a tiled map
 */
public class PointOfInterestMapRenderer extends OrthogonalTiledMapRenderer {
    private final MapStage stage;

    public PointOfInterestMapRenderer(MapStage stage) {
        super(null,stage.getBatch());
        this.stage = stage;
    }

    @Override
    public void render () {
        beginRender();
        for (MapLayer layer : map.getLayers()) {
            renderMapLayer(layer);
            if(layer==stage.getSpriteLayer())
            {
                //end render to draw character sprites
                batch.end();
                stage.draw();
                batch.begin();
            }
        }
        endRender();
    }

    public void loadMap(TiledMap map,String sourceMap)
    {
        stage.loadMap(map,sourceMap);

        super.setMap(map);
    }
}
