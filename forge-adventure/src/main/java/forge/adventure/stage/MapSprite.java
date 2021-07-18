package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.world.MapObject;
import forge.adventure.world.WorldSave;
import javafx.util.Pair;

public class MapSprite extends Actor {

    public static Array<Actor> GetMapSprites(int chunkx, int chunky)
    {

        Array<Pair<Vector2,Integer>> objects= WorldSave.getCurrentSave().world.GetMapObjects(chunkx,chunky);
        Array<Actor> actorGroup=new Array<>();
        for(Pair<Vector2,Integer> entry : objects)
        {
            Actor sprite=new MapSprite(WorldSave.getCurrentSave().world.GetObject(entry.getValue()),entry.getKey());
            actorGroup.add(sprite);
        }
        return actorGroup;
    }
    TextureRegion texture;
    public MapSprite(MapObject mapObject, Vector2 pos)
    {

        texture=mapObject.GetTexture();
        setPosition(pos.x,pos.y);
        setHeight(mapObject.getHeight());
        setWidth(mapObject.getWidth());
        //setZIndex((int)(WorldSave.getCurrentSave().world.GetHeightInPixels()-pos.y));
        //font = new BitmapFont();
        //texture.setPosition(getX()-getWidth()/2,getY());
    }
    //BitmapFont font;
    @Override
    public void draw (Batch batch, float parentAlpha) {
        if(texture==null)
            return;
        batch.draw(texture,getX()-(getWidth()/2),getY());
        //font.draw(batch,String.valueOf(getZIndex()),getX()-(getWidth()/2),getY());
    }

}
