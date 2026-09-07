package forge.adventure.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.DialogData;
import forge.adventure.stage.SpriteGroup;
import forge.adventure.util.Config;

import java.util.HashMap;

/**
 * CharacterSprite base class for animated sprites on the map
 */

public class CharacterSprite extends MapActor {
    private static final float DEFAULT_ANIMATION_FRAME_DURATION = 0.2f;
    private static final float MAX_DEATH_ANIMATION_DURATION = 3f;
    private static final float MAX_ACTION_ANIMATION_DURATION = 5f;
    private final HashMap<AnimationTypes, HashMap<AnimationDirections, Animation<TextureRegion>>> animations = new HashMap<>();
    float timer;
    private Animation<TextureRegion> currentAnimation = null;
    private AnimationTypes currentAnimationType = AnimationTypes.Idle;
    private AnimationDirections currentAnimationDir = AnimationDirections.None;
    private final Array<Sprite> avatar = new Array<>();
    public boolean hidden = false;
    public boolean inactive = false;
    private String atlasPath;
    private float wakeTimer = 0.0f;
    public DialogData.ConditionData[] spawnConditions = new DialogData.ConditionData[0]; //List of conditions for the sprite to spawn.

    public CharacterSprite(int id, String path) {
        super(id);
        collisionHeight = 0.4f;
        atlasPath = path;
        load(path);
    }

    public CharacterSprite(String path) {
        this(0, path);
    }

    @Override
    void updateBoundingRect() {//We want a slimmer box for the player entity so it can navigate terrain without getting stuck.
        boundingRect.set(getX() + 4, getY(), getWidth() - 6, getHeight() * collisionHeight);
    }

    protected void load(String path) {
        if (path == null || path.isEmpty()) return;
        animations.clear();
        for (AnimationTypes stand : AnimationTypes.values()) {
            if (stand == AnimationTypes.Avatar) {
                avatar.addAll(Config.instance().getAnimatedSprites(path, stand.toString()));
                continue;
            }
            HashMap<AnimationDirections, Animation<TextureRegion>> dirs = new HashMap<>();
            for (AnimationDirections dir : AnimationDirections.values()) {

                Array<Sprite> anim;
                if (dir == AnimationDirections.None)
                    anim = Config.instance().getAnimatedSprites(path, stand.toString());
                else
                    anim = Config.instance().getAnimatedSprites(path, stand.toString() + dir.toString());

                if (anim.size != 0) {
                    float frameDuration = DEFAULT_ANIMATION_FRAME_DURATION;
                    if (stand == AnimationTypes.Death) {
                        frameDuration = Math.min(frameDuration, MAX_DEATH_ANIMATION_DURATION / anim.size);
                    }
                    dirs.put(dir, new Animation<>(frameDuration, anim));
                    if (getWidth() == 0.0)//init size onload
                    {
                        setWidth(anim.first().getWidth());
                        setHeight(anim.first().getHeight());
                    }
                }
            }
            animations.put(stand, dirs);
        }


        for (AnimationTypes stand : AnimationTypes.values()) {
            if (stand == AnimationTypes.Avatar) {
                continue;
            }
            HashMap<AnimationDirections, Animation<TextureRegion>> dirs = animations.get(stand);

            if (!dirs.containsKey(AnimationDirections.None) && dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.None, (dirs.get(AnimationDirections.Right)));
            }
            if (!dirs.containsKey(AnimationDirections.Right) && dirs.containsKey(AnimationDirections.None)) {
                dirs.put(AnimationDirections.Right, (dirs.get(AnimationDirections.None)));
            }
            if (!dirs.containsKey(AnimationDirections.Left) && dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.Left, FlipAnimation(dirs.get(AnimationDirections.Right)));
            }
            if (dirs.containsKey(AnimationDirections.Left) && !dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.Right, FlipAnimation(dirs.get(AnimationDirections.Left)));
            }
            if (!dirs.containsKey(AnimationDirections.LeftUp) && dirs.containsKey(AnimationDirections.Left)) {
                dirs.put(AnimationDirections.LeftUp, dirs.get(AnimationDirections.Left));
            }
            if (!dirs.containsKey(AnimationDirections.LeftDown) && dirs.containsKey(AnimationDirections.Left)) {
                dirs.put(AnimationDirections.LeftDown, dirs.get(AnimationDirections.Left));
            }
            if (!dirs.containsKey(AnimationDirections.RightDown) && dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.RightDown, dirs.get(AnimationDirections.Right));
            }
            if (!dirs.containsKey(AnimationDirections.RightUp) && dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.RightUp, dirs.get(AnimationDirections.Right));
            }
            if (!dirs.containsKey(AnimationDirections.Up) && dirs.containsKey(AnimationDirections.Right)) {
                dirs.put(AnimationDirections.Up, dirs.get(AnimationDirections.Right));
            }
            if (!dirs.containsKey(AnimationDirections.Down) && dirs.containsKey(AnimationDirections.Left)) {
                dirs.put(AnimationDirections.Down, dirs.get(AnimationDirections.Left));
            }
        }

        setAnimation(AnimationTypes.Idle);
        setDirection(AnimationDirections.Right);
    }

    static public Animation<TextureRegion> FlipAnimation(Animation<TextureRegion> anim) {
        TextureRegion[] texReg = anim.getKeyFrames();
        Array<TextureRegion> newReg = new Array<>();
        for (TextureRegion reg : texReg) {
            TextureRegion cpy = new TextureRegion(reg);
            cpy.flip(true, false);
            newReg.add(cpy);
        }
        return new Animation<>(anim.getFrameDuration(), newReg);
    }

    public void setAnimation(AnimationTypes type) {
        Animation<TextureRegion> animation = getAnimation(type, currentAnimationDir);
        if (animation == null) {
            return;
        }

        if (currentAnimationType != type || currentAnimation != animation || isOneShotAnimation(type)) {
            currentAnimationType = type;
            currentAnimation = animation;
            if (isOneShotAnimation(type)) {
                timer = 0.0f;
            }
        }
    }

    /**
     * Returns the capped duration of an action animation in the sprite's current direction.
     * Uses the supplied fallback when the atlas does not define that animation.
     */
    public float getActionAnimationDuration(AnimationTypes type, float fallbackDuration) {
        Animation<TextureRegion> animation = getAnimation(type, currentAnimationDir);
        float duration = animation == null ? fallbackDuration : animation.getAnimationDuration();
        return Math.min(duration, MAX_ACTION_ANIMATION_DURATION);
    }

    private Animation<TextureRegion> getAnimation(AnimationTypes type, AnimationDirections direction) {
        HashMap<AnimationDirections, Animation<TextureRegion>> dirs = animations.get(type);
        if (dirs == null || dirs.isEmpty()) {
            return null;
        }

        Animation<TextureRegion> animation = dirs.get(direction);
        return animation == null ? dirs.get(AnimationDirections.Right) : animation;
    }

    private boolean isOneShotAnimation(AnimationTypes type) {
        return type == AnimationTypes.Attack
                || type == AnimationTypes.Death
                || type == AnimationTypes.Hit;
    }

    private void updateAnimation() {
        Animation<TextureRegion> animation = getAnimation(currentAnimationType, currentAnimationDir);
        if (animation == null) {
            animation = getAnimation(AnimationTypes.Idle, currentAnimationDir);
        }
        if (animation != null) {
            currentAnimation = animation;
        }
    }

    public void setDirection(AnimationDirections dir) {
        if (currentAnimationDir != dir) {
            currentAnimationDir = dir;
            updateAnimation();
        }
    }


    @Override
    protected void positionChanged() {
        Actor parent = getParent();
        if (parent instanceof SpriteGroup) {
            ((SpriteGroup) parent).UpdateActorZ(this);
        }
        super.positionChanged();
    }

    @Override
    public void moveBy(float x, float y) {
        moveBy(x, y, 0.0f);
    }

    public void moveBy(float x, float y, float delta) {

        if (inactive) {
            return;
        }

        if (hidden) {
            if (animations.containsKey(AnimationTypes.Wake)) {
                //Todo: Need another check here if we want objects revealed by activateMapObject to play wake animation
                setAnimation(AnimationTypes.Wake);
                wakeTimer = 0.0f;
                hidden = false;
            } else return;
        }

        if (currentAnimationType == AnimationTypes.Wake && wakeTimer <= currentAnimation.getAnimationDuration()) {
            wakeTimer += delta;
            return;
        }
        super.moveBy(x, y);
        if (x == 0 && y == 0) {
            return;
        }
        Vector2 vec = new Vector2(x, y);
        float degree = vec.angleDeg();

        if (!hidden)
            setAnimation(AnimationTypes.Walk);
        if (degree < 22.5)
            setDirection(AnimationDirections.Right);
        else if (degree < 22.5 + 45)
            setDirection(AnimationDirections.RightUp);
        else if (degree < 22.5 + 45 * 2)
            setDirection(AnimationDirections.Up);
        else if (degree < 22.5 + 45 * 3)
            setDirection(AnimationDirections.LeftUp);
        else if (degree < 22.5 + 45 * 4)
            setDirection(AnimationDirections.Left);
        else if (degree < 22.5 + 45 * 5)
            setDirection(AnimationDirections.LeftDown);
        else if (degree < 22.5 + 45 * 6)
            setDirection(AnimationDirections.Down);
        else if (degree < 22.5 + 45 * 7)
            setDirection(AnimationDirections.RightDown);
        else
            setDirection(AnimationDirections.Right);

    }

    public Vector2 pos() {
        return new Vector2(getX(), getY());
    }


    @Override
    public void act(float delta) {
        timer += delta;
        super.act(delta);

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (currentAnimation == null || hidden || inactive) {
            return;
        }
        super.draw(batch, parentAlpha);
        beforeDraw(batch, parentAlpha);

        TextureRegion currentFrame;
        if (currentAnimationType.equals(AnimationTypes.Wake)) {
            currentFrame = currentAnimation.getKeyFrame(wakeTimer, false);
        } else {
            currentFrame = currentAnimation.getKeyFrame(timer, !isOneShotAnimation(currentAnimationType));
        }

        float scale = 1f;
        if (this instanceof EnemySprite) {
            scale = ((EnemySprite) this).getData().scale;
        }

        setHeight(currentFrame.getRegionHeight() * scale);
        setWidth(currentFrame.getRegionWidth() * scale);
        Color oldColor = batch.getColor().cpy();
        batch.setColor(getColor());

        batch.draw(currentFrame, getX(), getY(), getWidth(), getHeight());
        batch.setColor(oldColor);
        super.draw(batch, parentAlpha);
        //batch.draw(getDebugTexture(),getX(),getY());

    }


    public Sprite getAvatar() {
        if (avatar == null || avatar.isEmpty())
            return null;
        return avatar.first();
    }

    public String getAtlasPath() {
        return atlasPath;
    }

    public Sprite getAvatar(int index) {
        return avatar.get(index);
    }

    public enum AnimationTypes {
        Idle,
        Walk,
        Death,
        Attack,
        Hit,
        Avatar,
        Hidden,
        Wake
    }

    public enum AnimationDirections {
        None,
        Right,
        RightDown,
        Down,
        LeftDown,
        Left,
        LeftUp,
        Up,
        RightUp
    }

}
