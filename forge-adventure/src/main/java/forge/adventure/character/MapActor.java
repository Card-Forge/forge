package forge.adventure.character;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Map Actor base class for Actors on the map
 * implements collision detection.
 */
public class MapActor extends Actor {


    Texture debugTexture;
    float collisionHeight=1.0f;
    private Texture getDebugTexture() {
        if (debugTexture == null) {
            Pixmap pixmap = new Pixmap((int) getWidth(), (int) getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setColor(1.0f,0,0,0.5f);
            pixmap.fillRectangle(0, (int) getHeight()- (int)boundingRect.getHeight(), (int) boundingRect.getWidth(), (int) boundingRect.getHeight());
            debugTexture = new Texture(pixmap);
            pixmap.dispose();

        }
        return debugTexture;
    }
    Rectangle boundingRect;

    boolean isCollidingWithPlayer=false;
    protected void onPlayerCollide()
    {

    }
    boolean boundDebug=false;
    public void setBoundDebug(boolean debug)
    {
        boundDebug=debug;
    }
    @Override
    public void draw(Batch batch, float alpha) {

        if(boundDebug)
            batch.draw(getDebugTexture(),getX(),getY());
    }
    @Override
    protected void positionChanged() {

        updateBoundingRect();
        super.positionChanged();
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        updateBoundingRect();
    }

    void updateBoundingRect() {
        boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight()*collisionHeight);
    }

    public Rectangle boundingRect() {
        return boundingRect;
    }
    public boolean collideWithPlayer(PlayerSprite other) {


        boolean newIsColliding= collideWith(other);
        if(newIsColliding)
        {
            if(!isCollidingWithPlayer)
                onPlayerCollide();
            isCollidingWithPlayer=true;
        }
        else
        {
            isCollidingWithPlayer=false;
        }
        return isCollidingWithPlayer;
    }
    public boolean collideWith(Rectangle other) {
       return boundingRect().overlaps(other);

    }

    public boolean collideWith(MapActor other) {
        return collideWith(other.boundingRect());
    }

    public boolean collideWith(Actor other) {
        return boundingRect.x < other.getX() + other.getWidth() && boundingRect.x + boundingRect.width > other.getX() && boundingRect.y < other.getY() + other.getHeight() && boundingRect.y + boundingRect.height > other.getY();

    }
}
