package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.util.Config;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.Random;

/**
 * Point of interest stored in the world
 */
public class PointOfInterest implements SaveFileContent {


    @Override
    public void load(SaveFileData saveFileData) {

        data=PointOfInterestData.getPointOfInterest(saveFileData.readString("name"));
        position.set(saveFileData.readVector2("position"));
        rectangle.set(saveFileData.readRectangle("rectangle"));
        spriteIndex=saveFileData.readInt("spriteIndex");


        oldMapId="";
        Array<Sprite> textureAtlas = Config.instance().getAtlas(this.data.spriteAtlas).createSprites(this.data.sprite);
        sprite = textureAtlas.get(spriteIndex);
    }

    @Override
    public SaveFileData save() {

        SaveFileData data=new SaveFileData();
        data.store("name",this.data.name);
        data.store("position",position);
        data.store("rectangle",rectangle);
        data.store("spriteIndex",spriteIndex);
        return data;
    }

    PointOfInterestData data;
    final Vector2 position=new Vector2();
    Sprite sprite;
    int spriteIndex;
    final Rectangle rectangle=new Rectangle();
    String oldMapId="";
    public PointOfInterest() {
    }
    public PointOfInterest(PointOfInterestData d, Vector2 pos, Random rand) {
        Array<Sprite> textureAtlas = Config.instance().getAtlas(d.spriteAtlas).createSprites(d.sprite);
        if (textureAtlas.isEmpty()) {
            System.out.print("sprite " + d.sprite + " not found");
        }
        spriteIndex = rand.nextInt(Integer.SIZE - 1) % textureAtlas.size;
        sprite = textureAtlas.get(spriteIndex);
        data = d;
        position.set(pos);

        rectangle.set(position.x, position.y, sprite.getWidth(), sprite.getHeight());
    }
    public PointOfInterest(PointOfInterestData d, PointOfInterest parent) {
        spriteIndex = parent.spriteIndex;
        sprite = parent.sprite;
        data = d;
        position.set(parent.position);
        oldMapId=parent.getID();
        rectangle.set(position.x, position.y, sprite.getWidth(), sprite.getHeight());
    }
    public Sprite getSprite() {
        return sprite;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getTilePosition(int tileSize) {
        return new Vector2((int) ((position.x + (sprite.getWidth() / 2)) / tileSize), (int) position.y / tileSize);
    }

    public Rectangle getBoundingRectangle() {
        return rectangle;
    }

    public PointOfInterestData getData() {
        return data;
    }

    public long getSeedOffset() {
        return  (long)position.x*715567   +(long)position.y+(data.name+"/"+oldMapId).hashCode();
    }

    public String getID() {
        return getSeedOffset()+data.name+"/"+oldMapId;
    }

}
