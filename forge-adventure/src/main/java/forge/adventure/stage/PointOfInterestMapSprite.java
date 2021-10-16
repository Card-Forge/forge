package forge.adventure.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import forge.adventure.world.PointOfInterest;

/**
 * MapSprite for points of interest to add a bounding rect for collision detection
 */
public class PointOfInterestMapSprite extends MapSprite {
    PointOfInterest pointOfInterest;
    Texture debugTexture;
    Rectangle boundingRect;

    public PointOfInterestMapSprite(PointOfInterest point) {
        super(point.getPosition(), point.getSprite());
        pointOfInterest = point;
        boundingRect = new Rectangle(getX(), getY(), texture.getRegionWidth(), texture.getRegionHeight());
    }

    public PointOfInterest getPointOfInterest() {
        return pointOfInterest;
    }

    private Texture getDebugTexture() {
        if (debugTexture == null) {
            Pixmap pixmap = new Pixmap(texture.getRegionWidth(), texture.getRegionHeight(), Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.RED);
            pixmap.drawRectangle(0, 0, (int) getWidth(), (int) getHeight());
            debugTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return debugTexture;
    }

    public Rectangle getBoundingRect() {
        return boundingRect;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        //batch.draw(getDebugTexture(),getX(),getY());
    }
}
