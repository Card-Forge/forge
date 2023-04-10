package forge.adventure.util;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

public class NavArrowActor extends Actor {

    Sprite texture;
    public float navTargetAngle = 0.0f;

    public NavArrowActor() {
        //TODO: Expand compass sprite to have color coded arrows, swap sprites based on distance to target
        Array<Sprite> textureAtlas = Config.instance().getAtlas("maps/tileset/compass.atlas").createSprites("compass");
        if (textureAtlas.isEmpty()) {
            System.out.print("NavArrow sprite not found");
        }
        texture = textureAtlas.get(0);
        setHeight(texture.getRegionHeight());
        setWidth(texture.getRegionWidth());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (texture == null)
            return;
        //TODO: Simplify params somehow for readability? All this does is spin the image around the player.
        batch.draw(texture, getX()-texture.getWidth()/2, getY()-texture.getHeight()/2 ,(texture.getWidth()*texture.getScaleX()/2),(texture.getHeight()*texture.getScaleY()/2), texture.getWidth(), texture.getHeight(), texture.getScaleX(), texture.getScaleY(), navTargetAngle);
    }
}
