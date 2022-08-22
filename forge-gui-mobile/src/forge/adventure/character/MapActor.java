package forge.adventure.character;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.util.Config;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Map Actor base class for Actors on the map
 * implements collision detection.
 */
public class MapActor extends Actor {

    class CurrentEffect
    {
        public CurrentEffect(ParticleEffect effect,Vector2 offset,boolean overlay)
        {
            this.effect=effect;
            this.offset=offset;
            this.overlay=overlay;
        }
        public ParticleEffect effect;
        public Vector2 offset;
        public boolean overlay;
    }

    Texture debugTexture;
    protected float collisionHeight=1.0f;
    final int objectId;
    Array<CurrentEffect> effects=new Array<>();

    public void playEffect(String path,Vector2 offset,boolean overlay)
    {
        ParticleEffect effect = new ParticleEffect();
        effect.load(Config.instance().getFile(path),Config.instance().getFile(path).parent());
        effect.getEmitters().first().setPosition(offset.x,offset.y);
        effects.add(new CurrentEffect(effect,offset,overlay));
        effect.start();
    }
    public void playEffect(String path)
    {
        playEffect(path,Vector2.Zero,false);
    }
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



        for(CurrentEffect effect:effects)
        {

            if(effect.overlay)
                effect.effect.draw(batch);
        }

    }

    protected void beforeDraw(Batch batch, float parentAlpha) {

        for(CurrentEffect effect:effects)
        {
            if(!effect.overlay)
                effect.effect.draw(batch);
        }
    }
    @Override
    public void act(float delta) {
        super.act(delta);


        for(int i=0;i<effects.size;i++)
        {
            CurrentEffect effect=effects.get(i);
            effect.effect.update(delta);
            effect.effect.setPosition(getX()+effect.offset.x,getY()+effect.offset.y);

            if(effect.effect.isComplete())
            {
                effects.removeIndex(i);
                i--;
                effect.effect.dispose();
            }
        }
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
