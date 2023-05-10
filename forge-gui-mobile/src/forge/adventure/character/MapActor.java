package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.util.Config;
import forge.util.MyRandom;

/**
 * Map Actor base class for Actors on the map
 * implements collision detection.
 */
public class MapActor extends Actor {

    private boolean removeIfEffectsAreFinished;

    public void removeAfterEffects() {
        removeIfEffectsAreFinished = true;
    }

    static class CurrentEffect {
        public CurrentEffect(FileHandle file, String path, ParticleEffect effect, Vector2 offset, boolean overlay) {
            this.fileHandle = file;
            this.path = path;
            this.effect = effect;
            this.offset = offset;
            this.overlay = overlay;
        }

        private final String path;
        private FileHandle fileHandle;
        public ParticleEffect effect;
        public Vector2 offset;
        public boolean overlay = true;
    }

    Texture debugTexture;
    protected float collisionHeight = 0.4f;
    final int objectId;
    Array<CurrentEffect> effects = new Array<>();

    public void removeEffect(String effectFly) {

        for (int i = 0; i < effects.size; i++) {
            CurrentEffect currentEffect = effects.get(i);
            if (currentEffect.path.equals(effectFly)) {
                for (ParticleEmitter emitter : currentEffect.effect.getEmitters()) {
                    emitter.setContinuous(false);
                }
            }
        }
    }

    public void playEffect(String path, float duration, boolean overlay, Vector2 offset) {
        FileHandle file = Config.instance().getFile(path);
        ParticleEffect effect = Forge.getAssets().getEffect(file);
        if (effect != null) {
            effect.setPosition(getCenter().x, getCenter().y);
            effects.add(new CurrentEffect(file, path, effect, offset, overlay));
            //ParticleEffect.setDuration uses an integer for some reason
            if (duration != 0) {
                for (ParticleEmitter emitter : effect.getEmitters()) {
                    emitter.setContinuous(false);
                    emitter.duration = duration;
                    emitter.durationTimer = 0.0F;
                }
            }
        }
        effect.start();
    }

    public void playEffect(String path, float duration, boolean overlay) {
        playEffect(path, duration, overlay, Vector2.Zero);
    }

    public void playEffect(String path, float duration) {
        playEffect(path, duration, true, Vector2.Zero);
    }

    public void playEffect(String path) {
        playEffect(path, 0, true, Vector2.Zero);
    }

    public MapActor(int objectId) {
        this.objectId = objectId;
    }

    public int getObjectId() {
        return objectId;
    }

    private Texture getDebugTexture() {
        if (debugTexture == null) {
            Pixmap pixmap = new Pixmap((int) getWidth(), (int) getHeight(), Pixmap.Format.RGBA8888);
            //pixmap.setColor(1.0f,0,0,0.5f);
            pixmap.setColor(MyRandom.getRandom().nextFloat(), MyRandom.getRandom().nextFloat(), MyRandom.getRandom().nextFloat(), 0.5f);

            pixmap.fillRectangle((int) (boundingRect.x - getX()), (int) (getHeight() - boundingRect.getHeight()) + (int) (boundingRect.y - getY()), (int) boundingRect.getWidth(), (int) boundingRect.getHeight());
            debugTexture = new Texture(pixmap);
            pixmap.dispose();

        }
        return debugTexture;
    }

    final Rectangle boundingRect = new Rectangle();

    boolean isCollidingWithPlayer = false;

    protected void onPlayerCollide() {

    }

    boolean boundDebug = false;

    public void setBoundDebug(boolean debug) {
        boundDebug = debug;
    }

    @Override
    public void draw(Batch batch, float alpha) {
        if (boundDebug) {
            batch.draw(getDebugTexture(), getX(), getY());
        }

        for (CurrentEffect effect : effects) {
            if (effect.overlay)
                effect.effect.draw(batch);
        }
    }

    protected void beforeDraw(Batch batch, float parentAlpha) {
        for (CurrentEffect effect : effects) {
            if (!effect.overlay)
                effect.effect.draw(batch);
        }
    }

    private Vector2 getCenter() {
        float scale = 1f;
        if (this instanceof EnemySprite) {
            scale = ((EnemySprite) this).getData().scale;
        }

        return new Vector2(getX() + (getWidth() * scale) / 2, getY() + (getHeight() * scale) / 2);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        for (int i = 0; i < effects.size; i++) {
            CurrentEffect effect = effects.get(i);
            effect.effect.update(delta);
            effect.effect.setPosition(getCenter().x + effect.offset.x, getCenter().y + effect.offset.y);
            if (effect.effect.isComplete()) {
                effects.removeIndex(i);
                i--;
                Forge.getAssets().manager().unload(effect.fileHandle.path());
            }
        }
        if (effects.size == 0 && removeIfEffectsAreFinished && getParent() != null)
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
        boundingRect.set(getX(), getY(), getWidth(), getHeight());
    }

    public Rectangle boundingRect() {
        return boundingRect;
    }

    public boolean collideWithPlayer(PlayerSprite other) {
        boolean newIsColliding = collideWith(other);
        if (newIsColliding) {
            if (!isCollidingWithPlayer)
                onPlayerCollide();
            isCollidingWithPlayer = true;
        } else {
            isCollidingWithPlayer = false;
        }
        return isCollidingWithPlayer;
    }

    public boolean collideWith(Rectangle other) {
        if (getCollisionHeight() == 0f)
            return false;
        return boundingRect().overlaps(other);
    }

    public int getId() {
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

    public void resetCollisionHeight() {
        collisionHeight = 0.4f;
    }

    public void clearCollisionHeight() {
        collisionHeight = 0f;
    }
}
