package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.stage.SpriteGroup;
import forge.adventure.util.Config;

import java.util.HashMap;

/**
 * CharacterSprite base class for animated sprites on the map
 */

public class CharacterSprite extends MapActor {
    private final HashMap<AnimationTypes, HashMap<AnimationDirections, Animation<TextureRegion>>> animations = new HashMap<>();
    float timer;
    private Animation<TextureRegion> currentAnimation = null;
    private AnimationTypes currentAnimationType = AnimationTypes.Idle;
    private AnimationDirections currentAnimationDir = AnimationDirections.None;
    private Sprite avatar;

    public CharacterSprite(String path) {
        collisionHeight=0.4f;
        load(path);
    }

    protected void load(String path) {
        TextureAtlas atlas = Config.instance().getAtlas(path);
        /*
        for (Texture texture : new ObjectSet.ObjectSetIterator<>( atlas.getTextures()))
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
         */
        animations.clear();
        for (AnimationTypes stand : AnimationTypes.values()) {
            if (stand == AnimationTypes.Avatar) {
                avatar = atlas.createSprite(stand.toString());
                continue;
            }
            HashMap<AnimationDirections, Animation<TextureRegion>> dirs = new HashMap<>();
            for (AnimationDirections dir : AnimationDirections.values()) {

                Array<Sprite> anim;
                if (dir == AnimationDirections.None)
                    anim = atlas.createSprites(stand.toString());
                else
                    anim = atlas.createSprites(stand.toString() + dir.toString());
                if (anim.size != 0) {
                    dirs.put(dir, new Animation<>(0.2f, anim));
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
        if (currentAnimationType != type) {
            currentAnimationType = type;
            updateAnimation();
        }
    }

    private void updateAnimation() {
        AnimationTypes aniType = currentAnimationType;
        AnimationDirections aniDir = currentAnimationDir;
        if (!animations.containsKey(aniType)) {
            aniType = AnimationTypes.Idle;
        }
        if (!animations.containsKey(aniType)) {
            return;
        }
        HashMap<AnimationDirections, Animation<TextureRegion>> dirs = animations.get(aniType);

        if (!dirs.containsKey(aniDir)) {
            aniDir = AnimationDirections.Right;
        }
        if (!dirs.containsKey(aniDir)) {
            return;
        }
        currentAnimation = dirs.get(aniDir);
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
        super.moveBy(x, y);
        if (x == 0 && y == 0) {
            return;
        }
        Vector2 vec = new Vector2(x, y);
        float degree = vec.angleDeg();

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
        if (currentAnimation == null)
            return;
        TextureRegion currentFrame = currentAnimation.getKeyFrame(timer, true);
        setHeight(currentFrame.getRegionHeight());
        setWidth(currentFrame.getRegionWidth());
        batch.draw(currentFrame, getX(), getY());
        super.draw(batch,parentAlpha);
        //batch.draw(getDebugTexture(),getX(),getY());

    }

    public Sprite getAvatar() {
        return avatar;
    }

    public enum AnimationTypes {
        Idle,
        Walk,
        Death,
        Attack,
        Hit,
        Avatar
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
