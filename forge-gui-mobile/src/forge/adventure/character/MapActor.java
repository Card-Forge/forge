package forge.adventure.character;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.util.Config;
import forge.util.MyRandom;

/**
 * Map Actor base class for Actors on the map
 * implements collision detection.
 */
public class MapActor extends Actor {


    private boolean removeIfEffectsAreFinished;

    public void removeAfterEffects() {
        removeIfEffectsAreFinished=true;
    }

    class CurrentEffect
    {
        public CurrentEffect(String path,ParticleEffect effect,Vector2 offset,boolean overlay)
        {
            this.path = path;
            this.effect=effect;
            this.offset=offset;
            this.overlay=overlay;
        }

        private String path;
        public ParticleEffect effect;
        public Vector2 offset;
        public boolean overlay=true;
    }

    Texture debugTexture;
    protected float collisionHeight=1.0f;
    final int objectId;
    Array<CurrentEffect> effects=new Array<>();

    public void removeEffect(String effectFly) {

        for(int i=0;i<effects.size;i++)
        {
            CurrentEffect currentEffect =effects.get(i);
            if(currentEffect.path.equals(effectFly))
            {
                for(ParticleEmitter emitter:currentEffect.effect.getEmitters()) {
                    emitter.setContinuous(false);
                }
            }
        }
    }
    public void playEffect(String path,float duration,boolean overlay,Vector2 offset)
    {
        ParticleEffect effect = new ParticleEffect();
        effect.load(Config.instance().getFile(path),Config.instance().getFile(path).parent());
        effects.add(new CurrentEffect(path,effect,offset,overlay));
        if(duration!=0)//ParticleEffect.setDuration uses an integer for some reason
        {
            for(ParticleEmitter emitter:effect.getEmitters()){
                emitter.setContinuous(false);
                emitter.duration = duration;
                emitter.durationTimer = 0.0F;
            }

        }
        effect.start();
    }
    public void playEffect(String path,float duration,boolean overlay)
    {
        playEffect(path,duration,overlay,Vector2.Zero);
    }
    public void playEffect(String path,float duration)
    {
        playEffect(path,duration,true,Vector2.Zero);
    }
    public void playEffect(String path)
    {
        playEffect(path,0,true,Vector2.Zero);
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
            //pixmap.setColor(1.0f,0,0,0.5f);
            pixmap.setColor(MyRandom.getRandom().nextFloat(),MyRandom.getRandom().nextFloat(),MyRandom.getRandom().nextFloat(),0.5f);

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
        {
            batch.draw(getDebugTexture(),getX(),getY());
        }



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
            effect.effect.setPosition(getX()+getHeight()/2+effect.offset.x,getY()+getWidth()/2+effect.offset.y);
            if(effect.effect.isComplete())
            {
                effects.removeIndex(i);
                i--;
                effect.effect.dispose();
            }
        }
        if(effects.size==0&&removeIfEffectsAreFinished&&getParent()!=null)
            getParent().removeActor(this);

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
