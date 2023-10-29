package forge.adventure.pointofintrest;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.util.Config;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.io.Serializable;
import java.util.Random;

/**
 * Point of interest stored in the world
 */
public class PointOfInterest implements Serializable, SaveFileContent {

    @Override
    public void load(SaveFileData saveFileData) {

        position.set(saveFileData.readVector2("position"));
        data=PointOfInterestData.getPointOfInterest(saveFileData.readString("name"));
        rectangle.set(saveFileData.readRectangle("rectangle"));
        spriteIndex=saveFileData.readInt("spriteIndex");
        if (saveFileData.containsKey("active")){
            active = saveFileData.readBool("active");
        }
        else
        {
            active = data.active;
        }

        oldMapId="";
        Array<Sprite> textureAtlas = Config.instance().getPOISprites(this.data);
        sprite = textureAtlas.get(spriteIndex%textureAtlas.size);
    }

    @Override
    public SaveFileData save() {

        SaveFileData data=new SaveFileData();
        data.store("name",this.data.name);
        data.store("position",position);
        data.store("rectangle",rectangle);
        data.store("spriteIndex",spriteIndex);
        data.store("active",active);
        return data;
    }

    PointOfInterestData data;
    final Vector2 position=new Vector2();
    transient Sprite sprite;
    int spriteIndex;
    final Rectangle rectangle=new Rectangle();
    String oldMapId="";
    boolean active = true;
    public PointOfInterest() {
    }
    public PointOfInterest(PointOfInterestData d, Vector2 pos, Random rand) {
        Array<Sprite> textureAtlas = Config.instance().getPOISprites(d);
        if (textureAtlas.isEmpty()) {
            System.out.print("sprite " + d.sprite + " not found");
        }
        spriteIndex = rand.nextInt(Integer.SIZE - 1) % textureAtlas.size;
        sprite = textureAtlas.get(spriteIndex);
        data = d;
        active = d.active;
        position.set(pos);

        rectangle.set(position.x, position.y, sprite.getWidth(), sprite.getHeight());
    }
    public PointOfInterest(PointOfInterestData d, PointOfInterest parent) {
        spriteIndex = parent.spriteIndex;
        sprite = parent.sprite;
        data = d;
        active = d.active;
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
        return new Vector2(((position.x + (sprite.getWidth() / 2)) / tileSize), position.y / tileSize);
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

    public boolean getActive() {return active;}

    public void setActive(boolean active) {this.active = active;}

    public Vector2 getNavigationVector(Vector2 origin){
        Vector2 navVector = new Vector2(rectangle.x + rectangle.getWidth() / 2, rectangle.y + rectangle.getHeight() / 2);
        if (origin != null) navVector.sub(origin);

        return navVector;
    }
}
