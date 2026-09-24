package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import forge.adventure.pointofintrest.PointOfInterest;

/**
 * MapSprite for points of interest to add a bounding rect for collision detection
 */
public class PointOfInterestMapSprite extends MapSprite {

    private final PointOfInterest pointOfInterest;
    private final Rectangle boundingRect;

    public PointOfInterestMapSprite(PointOfInterest point) {
        super(point.getPosition(), point.getSprite(), point);
        this.pointOfInterest = point;
        this.boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public PointOfInterest getPointOfInterest() {
        return pointOfInterest;
    }

    public MapSprite getMapSprite() {
        return this;
    }

    public Rectangle getBoundingRect() {
        return boundingRect;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (pointOfInterest != null && pointOfInterest.getActive()) {
            super.draw(batch, parentAlpha);
        }
    }
}
