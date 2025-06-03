package forge.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinTexture;
import forge.gui.FThreads;

public class CoverScreen extends TransitionScreen {
    private CoverAnimation coverAnimation;
    private Runnable runnable;
    private TextureRegion textureRegion;

    public CoverScreen(Runnable r, TextureRegion t) {
        runnable = r;
        textureRegion = t;
        coverAnimation = new CoverAnimation();
    }

    private class CoverAnimation extends ForgeAnimation {
        float DURATION = 0.8f;
        private float progress = 0;

        public void drawBackground(Graphics g) {
            float oldAlpha = g.getfloatAlphaComposite();
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            g.setAlphaComposite(percentage);
            g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.drawImage(FSkin.getLogo(), Forge.getScreenWidth() / 2f - FSkin.getLogo().getWidth() / 2f, Forge.getScreenHeight() / 2f - FSkin.getLogo().getHeight() / 2f, FSkin.getLogo().getWidth(), FSkin.getLogo().getHeight());
            g.setAlphaComposite(oldAlpha);
            g.drawPortalFade(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), Math.min(percentage, 1f), false);
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
