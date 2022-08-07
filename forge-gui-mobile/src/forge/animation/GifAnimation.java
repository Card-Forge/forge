package forge.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;

public class GifAnimation extends ForgeAnimation {
    private final Animation<TextureRegion> animation;
    private TextureRegion currentFrame;
    private float stateTime;

    public GifAnimation(String filename) {
        animation = GifDecoder.loadGIFAnimation(PlayMode.NORMAL, Gdx.files.absolute(filename).read());
    }

    @Override
    public void start() {
        currentFrame = animation.getKeyFrame(0);
        super.start();
    }

    @Override
    protected boolean advance(float dt) {
        stateTime += dt;
        currentFrame = animation.getKeyFrame(stateTime);
        return currentFrame != null;
    }

    public void draw(Graphics g, float x, float y, float w, float h) {
        if (currentFrame != null) {
            g.drawImage(currentFrame, x, y, w, h);
        }
    }

    @Override
    protected void onEnd(boolean endingAll) {
    }
}
