package forge.adventure.pointofintrest;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.DialogData;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.util.*;

import java.io.Serializable;
import java.util.ArrayList;
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
        if (saveFileData.containsKey("displayName")){
            displayName = saveFileData.readString("displayName");
        }
        else
        {
            displayName = data==null?"":data.getDisplayName();
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
        data.store("displayName",getDisplayName());
        data.storeObject("questFlagsToActivate", questFlagsToActivate);

        return data;
    }

    PointOfInterestData data;
    final Vector2 position=new Vector2();
    transient Sprite sprite;
    int spriteIndex;
    final Rectangle rectangle=new Rectangle();
    String oldMapId="";
    boolean active = true;
    private String displayName;
    public ArrayList<DialogData.ActionData.QuestFlag> questFlagsToActivate=new ArrayList<>();
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
        for (DialogData.ActionData.QuestFlag flag : data.questFlagsToActivate) {
            questFlagsToActivate.add(flag);
        }

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
        return getSeedOffset()+data.name+"/"+data.map;
    }

    public boolean getActive() {
        for (DialogData.ActionData.QuestFlag flag : questFlagsToActivate) {
            if (Current.player().getQuestFlag(flag.key) < flag.val){
                return false;
            }
        }
        return true;
    }

    public Vector2 getNavigationVector(Vector2 origin){
        Vector2 navVector = new Vector2(rectangle.x + rectangle.getWidth() / 2, rectangle.y + rectangle.getHeight() / 2);
        if (origin != null) navVector.sub(origin);

        return navVector;
    }

    public String getDisplayName() {
        if (displayName == null || displayName.isEmpty())
            displayName = data.getDisplayName();
        return displayName;
    }
    public void setDisplayName(String val) {
        displayName = val;
    }

    public boolean hasDisplayName(){
        return displayName!= null && !displayName.isEmpty();
    }
}
