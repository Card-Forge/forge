package forge.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.gui.GuiBase;
import forge.toolbox.FContainer;

public class TransitionScreen extends FContainer {
    private BGAnimation bgAnimation;
    Runnable runnable;
    TextureRegion textureRegion;
    boolean matchTransition, isloading;

    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading) {
        bgAnimation = new BGAnimation();
        runnable = proc;
        textureRegion = screen;
        matchTransition = enterMatch;
        isloading = loading;
    }

    @Override
    protected void doLayout(float width, float height) {

    }

    private class BGAnimation extends ForgeAnimation {
        float DURATION = 0.6f;
        private float progress = 0;
        Texture transition_bg = new Texture(GuiBase.isAndroid() ? Gdx.files.internal("fallback_skin").child("title_bg_lq.png") : Gdx.files.classpath("fallback_skin").child("title_bg_lq.png"));

        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (isloading) {
                g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                if (transition_bg != null) {
                    g.setAlphaComposite(percentage);
                    g.drawImage(transition_bg, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                    g.setAlphaComposite(oldAlpha);
                }
                float xmod = Forge.getScreenHeight() > 2000 ? 1.5f : 1f;
                xmod *= percentage;
                if (FSkin.hdLogo != null) {
                    g.drawImage(FSkin.hdLogo, Forge.getScreenWidth()/2 - (FSkin.hdLogo.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkin.hdLogo.getHeight()*xmod)/2, FSkin.hdLogo.getWidth()*xmod, FSkin.hdLogo.getHeight()*xmod);
                } else {
                    g.drawImage(FSkinImage.LOGO,Forge.getScreenWidth()/2 - (FSkinImage.LOGO.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkinImage.LOGO.getHeight()*xmod)/1.5f, FSkinImage.LOGO.getWidth()*xmod, FSkinImage.LOGO.getHeight()*xmod);
                }
            } else if (matchTransition) {
                if (textureRegion != null)
                    g.drawWarpImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage);
            } else {
                if (textureRegion != null)
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
