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
    protected float collisionHeight=1.0f;
    final int objectId;
    public MapActor(int objectId)
    {
        this.objectId=objectId;
    }
    public int getObjectId()
    {
        return objectId;
    }
    private Texture getDebugTexture() {
        if (debugTexture == null) {
            Pixmap pixmap = new Pixmap((int) getWidth(), (int) getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setColor(1.0f,0,0,0.5f);
            pixmap.fillRectangle((int)(boundingRect.x - getX()), (int)(getHeight()- boundingRect.getHeight()) + (int)(boundingRect.y - getY()), (int)boundingRect.getWidth(), (int)boundingRect.getHeight());
            debugTexture = new Texture(pixmap);
            pixmap.dispose();

        }
        return debugTexture;
    }
    final Rectangle boundingRect=new Rectangle();

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
        super.positionChanged();
        updateBoundingRect();
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        updateBoundingRect();
    }

    void updateBoundingRect() {
        boundingRect.set(getX(), getY(), getWidth(), getHeight()*collisionHeight);
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

    public int getId(){
        return objectId;
    }

    public boolean collideWith(MapActor other) {
        return collideWith(other.boundingRect());
    }

    public boolean collideWith(Actor other) {
        return boundingRect.x < other.getX() + other.getWidth() && boundingRect.x + boundingRect.width > other.getX() && boundingRect.y < other.getY() + other.getHeight() && boundingRect.y + boundingRect.height > other.getY();

    }

    public float getCollisionHeight() {
        return collisionHeight;
    }
}
