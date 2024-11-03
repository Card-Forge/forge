package forge.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.gui.FThreads;

public class CoverScreen extends TransitionScreen {
    private CoverAnimation coverAnimation;
    Runnable runnable;
    TextureRegion textureRegion;

    public CoverScreen(Runnable r, TextureRegion t) {
        runnable = r;
        textureRegion = t;
        coverAnimation = new CoverAnimation();
    }

    private class CoverAnimation extends ForgeAnimation {
        float DURATION = 0.6f;
        private float progress = 0;

        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (textureRegion != null) {
                g.drawPortalFade(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), Math.min(percentage, 1f), true);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        final boolean[] run = {false};//clears transition via runnable so this will reset anyway

        @Override
        protected void onEnd(boolean endingAll) {
            if (runnable != null) {
                if (run[0])
                    return;
                run[0] = true;
                FThreads.invokeInEdtNowOrLater(runnable);
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        coverAnimation.start();
        coverAnimation.drawBackground(g);
    }

    @Override
    protected void doLayout(float width, float height) {

    }
}
