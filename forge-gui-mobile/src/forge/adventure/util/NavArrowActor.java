package forge.adventure.util;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Color;

/**
 * NavArrowActor
 * Renders an animated arrow pointing towards active targets.
 */
public class NavArrowActor extends Actor {

    public float navTargetAngle = 0.0f;
    private final Animation<TextureRegion> currentAnimation;
    private static Array<Sprite> sprites = null;
    float timer;

    public NavArrowActor() {
        if (sprites == null) {
            //TODO: Expand compass sprite to have color coded arrows, swap sprites based on distance to target
            sprites = Config.instance().getAtlas("maps/tileset/compass.atlas").createSprites();
            if (sprites.isEmpty()) {
                System.out.print("NavArrow sprite not found");
            }
        }

        this.currentAnimation = new Animation<>(0.4f, sprites);
    }

    @Override
    public void act(float delta) {
        timer += delta;
        super.act(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (currentAnimation == null) {
            return;
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(timer, true);

        final float frameWidth = currentFrame.getRegionWidth();
        final float frameHeight = currentFrame.getRegionHeight();

        setHeight(frameHeight);
        setWidth(frameWidth);

        final Color oldColor = batch.getColor();
        final Color actorColor = getColor();

        final float targetAlpha = actorColor.a * parentAlpha;
        batch.setColor(actorColor.r, actorColor.g, actorColor.b, targetAlpha);

        //TODO: Simplify params somehow for readability? All this does is spin the image around the player.
        batch.draw(
                currentFrame,
                getX() - frameWidth / 2f,
                getY() - frameHeight / 2f,
                frameWidth * 0.5f,
                frameHeight * 0.5f,
                frameWidth,
                frameHeight,
                1f,
                1f,
                navTargetAngle
        );

        batch.setColor(oldColor);
    }
}
