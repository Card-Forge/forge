package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.BiomSpriteData;
import forge.adventure.world.PointOfInterest;
import forge.adventure.world.WorldSave;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MapSprite extends Actor {

    static public int BackgroundLayer = -1;
    static public int SpriteLayer = 0;
    TextureRegion texture;

    public MapSprite(Vector2 pos, TextureRegion sprite) {

        texture = sprite;
        setPosition(pos.x, pos.y);
        setHeight(texture.getRegionHeight());
        setWidth(texture.getRegionWidth());
        //setZIndex((int)(WorldSave.getCurrentSave().world.GetHeightInPixels()-pos.y));
        //font = new BitmapFont();
        //texture.setPosition(getX()-getWidth()/2,getY());
    }

    public static Array<Actor> GetMapSprites(int chunkx, int chunky) {
        Array<Actor> actorGroup = new Array<>();
        List<PointOfInterest> pointsOfIntrest = WorldSave.getCurrentSave().getWorld().getPointsOfIntrest(chunkx, chunky);
        for (PointOfInterest poi : pointsOfIntrest) {

            Actor sprite = new PointOfIntrestMapSprite(poi);
            actorGroup.add(sprite);
        }


        List<Pair<Vector2, Integer>> objects = WorldSave.getCurrentSave().getWorld().GetMapObjects(chunkx, chunky);

        for (Pair<Vector2, Integer> entry : objects) {
            BiomSpriteData data = WorldSave.getCurrentSave().getWorld().getObject(entry.getValue());
            if (data.layer != SpriteLayer)
                continue;
            Actor sprite = new MapSprite(entry.getKey(), WorldSave.getCurrentSave().getWorld().getData().GetBiomSprites().GetSprite(data.name, (int) entry.getKey().x + (int) entry.getKey().y * 11483));
            actorGroup.add(sprite);
        }
        return actorGroup;
    }

    public static Array<Actor> GetMapSpritesBackground(int chunkx, int chunky) {

        List<Pair<Vector2, Integer>> objects = WorldSave.getCurrentSave().getWorld().GetMapObjects(chunkx, chunky);
        Array<Actor> actorGroup = new Array<>();
        for (Pair<Vector2, Integer> entry : objects) {
            BiomSpriteData data = WorldSave.getCurrentSave().getWorld().getObject(entry.getValue());
            if (data.layer != BackgroundLayer)
                continue;
            Actor sprite = new MapSprite(entry.getKey(), WorldSave.getCurrentSave().getWorld().getData().GetBiomSprites().GetSprite(data.name, (int) entry.getKey().x + (int) entry.getKey().y * 11483));
            actorGroup.add(sprite);
        }
        return actorGroup;
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
