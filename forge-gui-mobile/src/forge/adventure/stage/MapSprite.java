package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.BiomeSpriteData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.world.WorldSave;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Sprite actor that will render trees and rocks on the over world
 */
public class MapSprite extends Actor {

    static public int BackgroundLayer = -1;
    static public int SpriteLayer = 0;
    TextureRegion texture;

    public MapSprite(Vector2 pos, TextureRegion sprite) {

        texture = sprite;
        setPosition(pos.x, pos.y);
        setHeight(texture.getRegionHeight());
        setWidth(texture.getRegionWidth()); 
    }

    public static Array<Actor> GetMapSprites(int chunkX, int chunkY) {
        Array<Actor> actorGroup = new Array<>();
        List<PointOfInterest> pointsOfInterest = WorldSave.getCurrentSave().getWorld().getPointsOfInterest(chunkX, chunkY);
        for (PointOfInterest poi : pointsOfInterest) {

            Actor sprite = new PointOfInterestMapSprite(poi);
            actorGroup.add(sprite);
        }

        List<Pair<Vector2, Integer>> objects = WorldSave.getCurrentSave().getWorld().GetMapObjects(chunkX, chunkY);

        for (Pair<Vector2, Integer> entry : objects) {
            BiomeSpriteData data = WorldSave.getCurrentSave().getWorld().getObject(entry.getValue());
            if (data.layer != SpriteLayer)
                continue;
            Actor sprite = new MapSprite(entry.getKey(), WorldSave.getCurrentSave().getWorld().getData().GetBiomeSprites().getSprite(data.name, (int) entry.getKey().x + (int) entry.getKey().y * 11483));
            actorGroup.add(sprite);
        }
        return actorGroup;
    }

    public static Array<Actor> GetMapSpritesBackground(int chunkX, int chunkY) {
        List<Pair<Vector2, Integer>> objects = WorldSave.getCurrentSave().getWorld().GetMapObjects(chunkX, chunkY);
        Array<Actor> actorGroup = new Array<>();
        for (Pair<Vector2, Integer> entry : objects) {
            BiomeSpriteData data = WorldSave.getCurrentSave().getWorld().getObject(entry.getValue());
            if (data.layer != BackgroundLayer)
                continue;
            Actor sprite = new MapSprite(entry.getKey(), WorldSave.getCurrentSave().getWorld().getData().GetBiomeSprites().getSprite(data.name, (int) entry.getKey().x + (int) entry.getKey().y * 11483));
            actorGroup.add(sprite);
        }
        return actorGroup;
    }

    public static String getBoundaryName(int chunkX, int chunkY) {
        String boundary = "";
        List<PointOfInterest> poi = WorldSave.getCurrentSave().getWorld().getPointsOfInterest(chunkX, chunkY);

        for (PointOfInterest p : poi) {
            if ("town".equalsIgnoreCase(p.getData().type)) {
                if (p.getData().name.startsWith("Waste"))
                    boundary = "waste";
                else if (p.getData().name.startsWith("Plains"))
                    boundary = "white";
                else if (p.getData().name.startsWith("Forest"))
                    boundary = "green";
                else if (p.getData().name.startsWith("Island"))
                    boundary = "blue";
                else if (p.getData().name.startsWith("Mountain"))
                    boundary = "red";
                else if (p.getData().name.startsWith("Swamp"))
                    boundary = "black";
                break;
            }
        }
        return boundary;
    }

    //BitmapFont font;
    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (texture == null)
            return;
        batch.draw(texture, getX(), getY());
        //font.draw(batch,String.valueOf(getZIndex()),getX()-(getWidth()/2),getY());
    }

}
