package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Class to add sprites to a map
 */
public class TextureSprite extends MapActor{

    private final TextureRegion region;

    public TextureSprite(TextureRegion region)
    {

        this.region = region;
        setWidth(region.getRegionWidth());
        setHeight(region.getRegionHeight());
    }
    @Override
    public void draw (Batch batch, float parentAlpha) {
        batch.draw(region,getX(),getY(),getWidth(),getHeight());
    }

}
