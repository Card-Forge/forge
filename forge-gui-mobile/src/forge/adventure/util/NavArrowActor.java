package forge.adventure.util;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

public class NavArrowActor extends Actor {

    public float navTargetAngle = 0.0f;
    private Animation<TextureRegion> currentAnimation;
    private Array<Sprite> sprites;
    float timer;

    public NavArrowActor() {
        if (sprites == null) {
            //TODO: Expand compass sprite to have color coded arrows, swap sprites based on distance to target
            sprites = Config.instance().getAtlas("maps/tileset/compass.atlas").createSprites();
            if (sprites.isEmpty())
                System.out.print("NavArrow sprite not found");
        }
        currentAnimation = new Animation<>(0.4f, sprites);
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

        //TODO: Simplify params somehow for readability? All this does is spin the image around the player.
        batch.draw(currentFrame, getX() - currentFrame.getRegionWidth() / 2, getY() - currentFrame.getRegionHeight() / 2, (currentFrame.getRegionWidth() * 0.5f), (currentFrame.getRegionHeight() * 0.5f), currentFrame.getRegionWidth(), currentFrame.getRegionHeight(), 1, 1, navTargetAngle);
    }
}
