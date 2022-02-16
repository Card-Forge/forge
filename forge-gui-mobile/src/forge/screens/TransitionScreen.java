package forge.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.toolbox.FContainer;

public class TransitionScreen extends FContainer {
    private BGAnimation bgAnimation;
    Runnable runnable;
    TextureRegion textureRegion;
    boolean matchTransition;

    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch) {
        bgAnimation = new BGAnimation();
        runnable = proc;
        textureRegion = screen;
        matchTransition = enterMatch;
    }

    @Override
    protected void doLayout(float width, float height) {

    }

    private class BGAnimation extends ForgeAnimation {
        float DURATION = 0.6f;
        private float progress = 0;

        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (matchTransition) {
                g.drawWarpImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage);
            } else {
                g.drawGrayTransitionImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), false, percentage);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (runnable != null)
                runnable.run();
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        bgAnimation.start();
        bgAnimation.drawBackground(g);
    }
}
