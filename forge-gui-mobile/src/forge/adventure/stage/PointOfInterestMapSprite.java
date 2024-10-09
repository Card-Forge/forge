package forge.adventure.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import forge.adventure.pointofintrest.PointOfInterest;

/**
 * MapSprite for points of interest to add a bounding rect for collision detection
 */
public class PointOfInterestMapSprite extends MapSprite {
    private boolean hasExited = false; // Track exit state
    PointOfInterest pointOfInterest;
    Texture debugTexture;
    Rectangle boundingRect;
    MapSprite mapSprite;

    public PointOfInterestMapSprite(PointOfInterest point) {
        super(point.getPosition(), point.getSprite(), point);
        pointOfInterest = point;
        mapSprite = this;
        boundingRect = new Rectangle(getX(), getY(), texture.getRegionWidth(), texture.getRegionHeight());
    }

    public PointOfInterest getPointOfInterest() {
        return pointOfInterest;
    }

    public MapSprite getMapSprite() {
        return mapSprite;
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

    public boolean hasExited() {
        return hasExited;
    }

    public void setHasExited(boolean hasExited) {
        this.hasExited = hasExited;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (pointOfInterest.getActive()) {
            // Draw the actual sprite of the POI
            super.draw(batch, parentAlpha);

            // Optionally draw the regular bounding box for collision detection (Red)
            batch.setColor(1, 0, 0, 1);  // Red color for the regular bounding box
            batch.draw(getDebugTexture(), getX(), getY(), getWidth(), getHeight());  // Draw the red rectangle for the original bounding box

            // Now, calculate the larger bounding box (without calling the method)
            float expansionAmount = 25;  // Define the expansion amount
            float largerX = getX() - expansionAmount / 2;
            float largerY = getY() - expansionAmount / 2;
            float largerWidth = getWidth() + expansionAmount;
            float largerHeight = getHeight() + expansionAmount;

            // Draw the larger bounding box (Blue or Green)
            batch.setColor(0, 0, 1, 1);  // Blue color for the larger bounding box (change to green with 0, 1, 0, 1)
            batch.draw(getDebugTexture(), largerX, largerY, largerWidth, largerHeight);  // Draw the larger bounding box

            // Reset the color back to white to avoid affecting other draw calls
            batch.setColor(Color.WHITE);
        }
    }


}
