package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge;
import forge.Graphics;
import forge.adventure.scene.ArenaScene;
import forge.adventure.util.Config;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.sound.SoundSystem;
import forge.toolbox.FContainer;
import forge.toolbox.FProgressBar;
import forge.util.MyRandom;

public class TransitionScreen extends FContainer {
    private BGAnimation bgAnimation;
    private FProgressBar progressBar;
    Runnable runnable;
    TextureRegion textureRegion, screenUIBackground, playerAvatar, vsTexture;
    String enemyAtlasPath;
    private String message = "";
    boolean matchTransition, isloading, isIntro, isFadeMusic, isArenaScene;

    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading) {
        this(proc, screen, enterMatch, loading, false, false);
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, String loadingMessage) {
        this(proc, screen, enterMatch, loading, false, false, loadingMessage, null, null, "");
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, boolean intro, boolean fadeMusic) {
        this(proc, screen, enterMatch, loading, intro, fadeMusic, "", null, null, "");
    }
    public TransitionScreen(Runnable proc, TextureRegion screen, boolean enterMatch, boolean loading, boolean intro, boolean fadeMusic, String loadingMessage, TextureRegion vsIcon, TextureRegion player, String enemy) {
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
        Forge.advStartup = intro && Forge.selector.equals("Adventure");
        if (Forge.getCurrentScene() instanceof ArenaScene) {
            isArenaScene = true;
            screenUIBackground = ((ArenaScene) Forge.getCurrentScene()).getUIBackground();
        } else {
            isArenaScene = false;
            screenUIBackground = null;
        }
        playerAvatar = player;
        enemyAtlasPath = enemy;
        vsTexture = vsIcon;
    }

    public FProgressBar getProgressBar() {
        return progressBar;
    }
    @Override
    protected void doLayout(float width, float height) {

    }
    public boolean isMatchTransition() {
        return matchTransition;
    }
    public void disableMatchTransition() {
        matchTransition = false;
    }

    private class BGAnimation extends ForgeAnimation {
        float DURATION = isArenaScene ? 1.2f : 0.6f;
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
                FSkinTexture bgTexture = Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE;
                if (bgTexture != null) {
                    g.setAlphaComposite(percentage);
                    g.drawImage(bgTexture, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                    g.setAlphaComposite(oldAlpha);
                }
                float xmod = Forge.getScreenHeight() > 2000 ? 1.5f : 1f;
                xmod *= 1f;//static logo only
                float ymod;
                if (FSkin.getLogo() != null) {
                    ymod = Forge.getScreenHeight()/2 + (FSkin.getLogo().getHeight()*xmod)/2;
                    g.drawImage(FSkin.getLogo(), Forge.getScreenWidth()/2 - (FSkin.getLogo().getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkin.getLogo().getHeight()*xmod)/2, FSkin.getLogo().getWidth()*xmod, FSkin.getLogo().getHeight()*xmod);
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
                    progressBar.setBounds(x, Forge.getScreenHeight() - h * 2f, w, h);
                    progressBar.setValue(multi);
                    if (multi == 100 && !message.isEmpty()) {
                        progressBar.setDescription(message);
                    }
                    g.draw(progressBar);
                }
            } else if (matchTransition) {
                if (textureRegion != null) {
                    if (isArenaScene) {
                        float screenW = Forge.isLandscapeMode() ? Forge.getScreenWidth() : Forge.getScreenHeight();
                        float screenH = Forge.isLandscapeMode() ? Forge.getScreenHeight() : Forge.getScreenWidth();
                        float scale = screenW/4;
                        float centerX = screenW/2;
                        float centerY = screenH/2;
                        TextureRegion enemyAvatar = Config.instance().getAtlas(enemyAtlasPath).createSprite("Avatar");
                        enemyAvatar.flip(true, false);
                        g.setColorRGBA(1f, 1f, 1f, 1.8f - percentage);
                        g.drawImage(screenUIBackground, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                        g.resetColorRGBA(oldAlpha);
                        if (Forge.isLandscapeMode()) {
                            g.drawImage(playerAvatar, scale / 3 * percentage, centerY - scale / 2, scale, scale);
                            g.drawImage(enemyAvatar, screenW - scale - (percentage * scale / 3), centerY - scale / 2, scale, scale);
                            float vsScale = (screenW / 3.2f) * percentage;
                            g.startRotateTransform(screenW/2, screenH/2, 180-(180*percentage));
                            g.drawImage(vsTexture, centerX - vsScale / 2, centerY - vsScale / 2, vsScale, vsScale);
                            g.endTransform();
                        } else {
                            g.drawImage(playerAvatar, centerY - scale / 2, scale / 3 * percentage, scale, scale);
                            g.drawImage(enemyAvatar,centerY - scale / 2 ,screenW - scale - (percentage * scale / 3), scale, scale);
                            float vsScale = (screenW / 3.2f) * percentage;
                            g.startRotateTransform(screenH/2, screenW/2, 180-(180*percentage));
                            g.drawImage(vsTexture, centerY - vsScale / 2, centerX - vsScale / 2, vsScale, vsScale);
                            g.endTransform();
                        }
                        g.setAlphaComposite(1-(percentage+0.6f));
                        g.drawImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                        g.setAlphaComposite(oldAlpha);
                    } else {
                        if (GuiBase.isAndroid()) {
                            g.drawChromatic(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage);
                        } else {
                            int max = Forge.isLandscapeMode() ? Forge.getScreenHeight() / 32 : Forge.getScreenWidth() / 32;
                            int min = Forge.isLandscapeMode() ? Forge.getScreenHeight() / 64 : Forge.getScreenWidth() / 64;
                            int val = MyRandom.getRandom().nextInt(max - min) + min;
                            g.drawPixelatedWarp(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), val * percentage);
                        }
                    }
                }
            } else if (isIntro) {
                if (textureRegion != null) {
                    if (Forge.advStartup) {
                        g.drawGrayTransitionImage(Forge.getAssets().fallback_skins().get(0), 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), false, percentage);
                        g.setAlphaComposite(1-percentage);
                        g.drawImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                        g.setAlphaComposite(oldAlpha);
                    } else {
                        g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                        g.setAlphaComposite(1-percentage);
                        g.drawImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
                        g.setAlphaComposite(oldAlpha);
                    }
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
