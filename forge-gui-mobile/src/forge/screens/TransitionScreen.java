package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.gui.FThreads;
import forge.sound.SoundSystem;
import forge.toolbox.FContainer;
import forge.toolbox.FProgressBar;

public class TransitionScreen extends FContainer {
    private BGAnimation bgAnimation;
    private FProgressBar progressBar;
    Runnable runnable;
    TextureRegion textureRegion;
    private String message = "";
    boolean matchTransition, isloading, isIntro, isFadeMusic;

    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading) {
        this(proc, screen, enterMatch, loading, false, false);
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, String loadingMessage) {
        this(proc, screen, enterMatch, loading, false, false, loadingMessage);
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, boolean intro, boolean fadeMusic) {
        this(proc, screen, enterMatch, loading, intro, fadeMusic, "");
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, boolean intro, boolean fadeMusic, String loadingMessage) {
        progressBar = new FProgressBar();
        progressBar.setMaximum(100);
        progressBar.setPercentMode(true);
        progressBar.setShowETA(false);
        bgAnimation = new BGAnimation();
        runnable = proc;
        textureRegion = screen;
        matchTransition = enterMatch;
        isloading = loading;
        isIntro = intro;
        isFadeMusic = fadeMusic;
        message = loadingMessage;
    }

    public FProgressBar getProgressBar() {
        return progressBar;
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
            if (isFadeMusic) {
                try {
                    //fade out volume
                    SoundSystem.instance.fadeModifier(1-percentage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (isloading) {
                g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                if (FSkinTexture.BG_TEXTURE != null) {
                    g.setAlphaComposite(percentage);
                    g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                    g.setAlphaComposite(oldAlpha);
                }
                float xmod = Forge.getScreenHeight() > 2000 ? 1.5f : 1f;
                xmod *= Forge.isMobileAdventureMode ? 1 : percentage;
                float ymod;
                if (FSkin.hdLogo != null) {
                    ymod = Forge.getScreenHeight()/2 + (FSkin.hdLogo.getHeight()*xmod)/2;
                    g.drawImage(FSkin.hdLogo, Forge.getScreenWidth()/2 - (FSkin.hdLogo.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkin.hdLogo.getHeight()*xmod)/2, FSkin.hdLogo.getWidth()*xmod, FSkin.hdLogo.getHeight()*xmod);
                } else {
                    ymod = Forge.getScreenHeight()/2 + (FSkinImage.LOGO.getHeight()*xmod)/1.5f;
                    g.drawImage(FSkinImage.LOGO,Forge.getScreenWidth()/2 - (FSkinImage.LOGO.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkinImage.LOGO.getHeight()*xmod)/1.5f, FSkinImage.LOGO.getWidth()*xmod, FSkinImage.LOGO.getHeight()*xmod);
                }
                //loading progressbar - todo make this accurate when generating world
                if (Forge.isMobileAdventureMode) {
                    float w = Forge.isLandscapeMode() ? Forge.getScreenWidth() / 2 : Forge.getScreenHeight() / 2;
                    float h = 57f / 450f * (w/2);
                    float x = (Forge.getScreenWidth() - w) / 2;
                    float y = ymod + 10;
                    int multi = ((int) (percentage*100)) < 97 ? (int) (percentage*100) : 100;
                    progressBar.setBounds(x, y, w, h);
                    progressBar.setValue(multi);
                    if (multi == 100 && !message.isEmpty()) {
                        progressBar.setDescription(message);
                    }
                    g.draw(progressBar);
                }
            } else if (matchTransition) {
                if (textureRegion != null)
                    g.drawWarpImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage);
            } else if (isIntro) {
                if (textureRegion != null) {
                    g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                    g.setAlphaComposite(1-percentage);
                    g.drawImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                    g.setAlphaComposite(oldAlpha);
                }
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
            if (runnable != null) {
                FThreads.invokeInEdtNowOrLater(runnable);
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        bgAnimation.start();
        bgAnimation.drawBackground(g);
    }
}
