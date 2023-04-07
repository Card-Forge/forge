package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraLabel;
import forge.adventure.data.BiomeSpriteData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.Controls;
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
    TextraLabel searchPost = Controls.newTextraLabel("[%80][+SearchPost]");
    boolean isCaveDungeon;

    PointOfInterest point;

    public MapSprite(Vector2 pos, TextureRegion sprite, PointOfInterest point) {
        if (point != null) {
            isCaveDungeon = "cave".equalsIgnoreCase(point.getData().type) || "dungeon".equalsIgnoreCase(point.getData().type);
        }
        this.point = point;
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

        actorGroup.addAll(GetMapSpritesBackground(chunkX, chunkY));
        return actorGroup;
    }

    public static Array<Actor> GetMapSpritesBackground(int chunkX, int chunkY) {
        List<Pair<Vector2, Integer>> objects = WorldSave.getCurrentSave().getWorld().GetMapObjects(chunkX, chunkY);
        Array<Actor> actorGroup = new Array<>();
        for (Pair<Vector2, Integer> entry : objects) {
            BiomeSpriteData data = WorldSave.getCurrentSave().getWorld().getObject(entry.getValue());
            if (data.layer != BackgroundLayer)
                continue;
            Actor sprite = new MapSprite(entry.getKey(), WorldSave.getCurrentSave().getWorld().getData().GetBiomeSprites().getSprite(data.name, (int) entry.getKey().x + (int) entry.getKey().y * 11483), null);
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
        if (isCaveDungeon && point.getData().map != null && point.getID() != null){
            PointOfInterestChanges changes = WorldSave.getCurrentSave().getPointOfInterestChanges(point.getID() + point.getData().map);
            if (!changes.hasDeletedObjects()) {
                searchPost.setPosition(getX() - 7, getY() + 7);
                searchPost.draw(batch, parentAlpha);
            }
        }
        //font.draw(batch,String.valueOf(getZIndex()),getX()-(getWidth()/2),getY());
    }

}
