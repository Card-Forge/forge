package forge.adventure.stage;


import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.MapActor;
import forge.adventure.character.OnCollide;
import forge.adventure.scene.SceneType;

public class MapStage extends GameStage {

    public static MapStage instance;
    Array<MapActor> actors = new Array<>();

    public static MapStage getInstance() {
        return instance == null ? instance = new MapStage() : instance;
    }

    public void addMapActor(MapObject obj, MapActor newActor) {
        newActor.setX(Float.parseFloat(obj.getProperties().get("x").toString()));
        newActor.setY(Float.parseFloat(obj.getProperties().get("y").toString()));
        newActor.setWidth(Float.parseFloat(obj.getProperties().get("width").toString()));
        newActor.setHeight(Float.parseFloat(obj.getProperties().get("height").toString()));
        actors.add(newActor);
        foregroundSprites.addActor(newActor);
    }

    public void loadMap(TiledMap map) {

        for (MapActor actor : actors) {
            foregroundSprites.removeActor(actor);

        }
        float width = Float.parseFloat(map.getProperties().get("width").toString());
        float height = Float.parseFloat(map.getProperties().get("height").toString());
        float tileHeight = Float.parseFloat(map.getProperties().get("tileheight").toString());
        float tileWidth = Float.parseFloat(map.getProperties().get("tilewidth").toString());
        setBounds(width * tileWidth, height * tileHeight);

        MapLayer layer = map.getLayers().get("Objects");
        for (MapObject obj : layer.getObjects()) {
            Object typeObject = obj.getProperties().get("type");
            if (typeObject != null) {
                String type = obj.getProperties().get("type").toString();

                switch (type) {
                    case "entry":
                        GetPlayer().setPosition(Float.parseFloat(obj.getProperties().get("x").toString()), Float.parseFloat(obj.getProperties().get("y").toString()));
                        GetPlayer().setMovementDirection(Vector2.Zero);
                        break;
                    case "exit":
                        addMapActor(obj, new OnCollide(() -> exit()));
                        break;
                }
            }
        }
    }

    private boolean exit() {

        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene.instance);
        return true;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        for (MapActor actor : actors) {
            if (actor.collideWith(player)) {

            }
        }
    }
}
