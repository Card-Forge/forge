package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Res;
import forge.adventure.util.TemplateTmxMapLoader;
import forge.adventure.world.PointOfIntrest;

public class TileMapScene extends HudScene {


    TiledMap map;
    BatchTiledMapRenderer tiledMapRenderer;

    public TileMapScene() {
        super(MapStage.getInstance());


    }

    @Override
    public void dispose() {
        if (map != null)
            map.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());


        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling? GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        stage.getCamera().update();
        tiledMapRenderer.setView(stage.getCamera().combined, 0, 0, Scene.GetIntendedWidth(), Scene.GetIntendedHeight());
        tiledMapRenderer.render();
        stage.draw();
        hud.draw();
        //Batch.end();
    }

    @Override
    public void ResLoaded() {
        //map=new TmxMapLoader().load("F:\\Develop\\Forge\\forge\\forge-gui\\res\\adventure\\Shandalar\\world\\town\\town.tmx");


    }

    public void load(PointOfIntrest point) {
        map = new TemplateTmxMapLoader().load(Res.CurrentRes.GetFilePath(point.getData().map));
        tiledMapRenderer = new OrthogonalTiledMapRenderer(map);

        stage.GetPlayer().setPosition(0, 0);

        ((MapStage) stage).loadMap(map);

    }
}

