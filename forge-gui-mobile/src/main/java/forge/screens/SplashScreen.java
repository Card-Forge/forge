package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FProgressBar;

public class SplashScreen extends FContainer {
    private TextureRegion splashTexture;
    private Texture splashBGTexture;
    private FProgressBar progressBar;
    private FSkinFont disclaimerFont;
    private boolean preparedForDialogs, showModeSelector, init, animateLogo, hideBG, hideBtn, startClassic, clear;
    private FButton btnAdventure, btnHome;
    private BGAnimation bgAnimation;

    public SplashScreen() {
        progressBar = getProgressBar();
        bgAnimation = getBgAnimation();
    }

    public BGAnimation getBgAnimation() {
        if (bgAnimation == null) {
            bgAnimation = new BGAnimation();
        }
        return bgAnimation;
    }
    public FProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new FProgressBar();
            progressBar.setDescription("Welcome to Forge");
        }
        return progressBar;
    }

    public void setSplashTexture(TextureRegion textureRegion) {
        splashTexture = textureRegion;
    }
    public void setSplashBGTexture(Texture texture) {
        splashBGTexture = texture;
    }

    public void startClassic() {
        startClassic = true;
        hideBtn = true;
        hideBG = true;
        bgAnimation.DURATION = 1f;
        bgAnimation.progress = 0;
        bgAnimation.openAdventure = false;
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    //prepare for showing dialogs on top of splash screen if needed
    public void prepareForDialogs() {
        if (preparedForDialogs) {
            return;
        }

        //establish fallback colors for before actual colors are loaded
        Color defaultColor = new Color(0, 0, 0, 0);
        for (final FSkinColor.Colors c : FSkinColor.Colors.values()) {
            switch (c) {
                case CLR_BORDERS:
                case CLR_TEXT:
                    c.setColor(FProgressBar.SEL_FORE_COLOR);
                    break;
                case CLR_ACTIVE:
                case CLR_THEME2:
                    c.setColor(FProgressBar.SEL_BACK_COLOR);
                    break;
                case CLR_INACTIVE:
                    c.setColor(FSkinColor.stepColor(FProgressBar.SEL_BACK_COLOR, -80));
                    break;
                default:
                    c.setColor(defaultColor);
                    break;
            }
        }
        FSkinColor.updateAll();
        preparedForDialogs = true;
    }

    public void setShowModeSelector(boolean value) {
        showModeSelector = value;
    }

    public boolean isShowModeSelector() {
        return showModeSelector;
    }

    private class BGAnimation extends ForgeAnimation {
        float DURATION = 0.8f;
        private float progress = 0;
        private boolean finished, openAdventure;
        private void drawAdventureBackground(Graphics g) {
            if (splashTexture == null)
                return;
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(percentage);
            g.drawRepeatingImage(splashBGTexture, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(oldAlpha);

            float x, y, w, h;
            float backgroundRatio = (float) splashTexture.getRegionWidth() / splashTexture.getRegionHeight();
            float screenRatio = getWidth() / getHeight();
            if (backgroundRatio > screenRatio) {
                x = 0;
                w = getWidth();
                h = getWidth() * backgroundRatio;
                y = (getHeight() - h) / 2;
            } else {
                y = 0;
                h = getHeight();
                w = getHeight() / backgroundRatio;
                x = (getWidth() - w) / 2;
            }
            float hmod = Forge.isLandscapeMode() ? 1f : 1.3f;
            float ymod = 2.6f;
            g.drawImage(splashTexture, Forge.getScreenWidth()/2f - (w*percentage*hmod)/2 , Forge.getScreenHeight()/2f - (h*percentage*hmod)/ymod, (w*percentage)*hmod, (h*percentage)*hmod);

            y += h * 295f / 450f;
            if (disclaimerFont == null) {
                disclaimerFont = FSkinFont.get(9);
            }
            float disclaimerHeight = 30f / 450f * h;
            if (Forge.forcedEnglishonCJKMissing && !clear) {
                clear = true;
                FSkinFont.preloadAll("");
                disclaimerFont = FSkinFont.get(9);
            }
            float padding = 20f / 450f * w;
            float pbHeight = 57f / 450f * h;
            y += 78f / 450f * h;

            float w2 = Forge.isLandscapeMode() ? Forge.getScreenWidth() / 2f : Forge.getScreenHeight() / 2f;
            float h2 = 57f / 450f * (w2/2);

            String version = "v." + Forge.getDeviceAdapter().getVersionString();
            g.drawText(version, disclaimerFont, FProgressBar.SEL_FORE_COLOR, x, getHeight() - disclaimerHeight, w, disclaimerHeight, false, Align.center, true);
            progressBar.setBounds((Forge.getScreenWidth() - w2)/2, Forge.getScreenHeight() - h2 * 2f, w2, h2);
            g.draw(progressBar);
        }
        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (startClassic) {
                showSplash(g, 1 - percentage);
            } else {
                if (animateLogo) {
                    //bg
                    drawTransition(g, openAdventure, percentage);
                    g.setAlphaComposite(1 - percentage);
                    g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());
                    g.setAlphaComposite(oldAlpha);
                    //logo
                    g.setAlphaComposite(oldAlpha - percentage);
                    float xmod = Forge.getScreenHeight() > 1000 ? 1.5f : Forge.getScreenHeight() > 800 ? 1.3f : 1f;
                    xmod += 10 * percentage;
                    g.drawImage(FSkin.getLogo(), getWidth() / 2 - (FSkin.getLogo().getWidth() * xmod) / 2, getHeight() / 2 - (FSkin.getLogo().getHeight() * xmod) / 1.5f, FSkin.getLogo().getWidth() * xmod, FSkin.getLogo().getHeight() * xmod);
                    g.setAlphaComposite(oldAlpha);
                } else {
                    g.setAlphaComposite(hideBG ? 1 - percentage : 1);
                    if (showModeSelector) {
                        showSelector(g, hideBG ? 1 - percentage : 1);
                    } else {
                        showSplash(g, 1);
                    }
                    g.setAlphaComposite(oldAlpha);
                    if (hideBG) {
                        g.setAlphaComposite(0 + percentage);
                        drawTransition(g, openAdventure, percentage);
                        g.setAlphaComposite(oldAlpha);
                    }
                }
            }
            if (hideBtn) {
                if (btnAdventure != null) {
                    float y = btnAdventure.getTop();
                    btnAdventure.setTop(y + (getHeight() / 16 * percentage));
                }
                if (btnHome != null) {
                    float y = btnHome.getTop();
                    btnHome.setTop(y + (getHeight() / 16 * percentage));
                }
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (animateLogo || hideBG) {
                if (openAdventure)
                    Forge.openAdventure();
                else
                    Forge.openHomeDefault();
                Forge.clearSplashScreen();
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        bgAnimation.start();
        if (!Forge.selector.equalsIgnoreCase("Adventure"))
            bgAnimation.drawBackground(g);
        else
            bgAnimation.drawAdventureBackground(g);
    }

    void drawTransition(Graphics g, boolean openAdventure, float percentage) {
        Texture t = Forge.getAssets().fallback_skins().get("title");
        TextureRegion tr = null;
        if (t != null)
            tr = new TextureRegion(t);
        float oldAlpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(percentage);
        if (openAdventure) {
            if (tr != null) {
                g.drawGrayTransitionImage(tr, 0, 0, getWidth(), getHeight(), false, percentage * 1);
            }
        } else {
            g.fillRect(FSkinColor.get(FSkinColor.Colors.CLR_THEME), 0, 0, getWidth(), getHeight());
        }
        g.setAlphaComposite(oldAlpha);
    }

    private void showSelector(Graphics g, float alpha) {
        if (splashTexture == null) {
            return;
        }
        g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());

        float x, y, w, h;
        float backgroundRatio = (float) splashTexture.getRegionWidth() / splashTexture.getRegionHeight();
        float screenRatio = getWidth() / getHeight();
        if (backgroundRatio > screenRatio) {
            x = 0;
            w = getWidth();
            h = getWidth() * backgroundRatio;
            y = (getHeight() - h) / 2;
        } else {
            y = 0;
            h = getHeight();
            w = getHeight() / backgroundRatio;
            x = (getWidth() - w) / 2;
        }
        if (FSkin.getLogo() != null) {
            float xmod = Forge.getScreenHeight() > 1000 ? 1.5f : Forge.getScreenHeight() > 800 ? 1.3f : 1f;
            g.drawImage(FSkin.getLogo(), getWidth() / 2 - (FSkin.getLogo().getWidth() * xmod) / 2, getHeight() / 2 - (FSkin.getLogo().getHeight() * xmod) / 1.5f, FSkin.getLogo().getWidth() * xmod, FSkin.getLogo().getHeight() * xmod);
        } else {
            g.drawImage(FSkinImage.LOGO, getWidth() / 2 - (FSkinImage.LOGO.getWidth() * 2f) / 2, getHeight() / 2 - (FSkinImage.LOGO.getHeight() * 2f) / 1.3f, FSkinImage.LOGO.getWidth() * 2f, FSkinImage.LOGO.getHeight() * 2f);
        }
        y += h * 295f / 450f;
        float padding = 20f / 450f * w;
        float height = 57f / 450f * h;

        if (!init) {
            init = true;
            btnAdventure = new FButton(Forge.getLocalizer().getMessageorUseDefault("lblAdventureMode", "Adventure Mode"));
            btnHome = new FButton(Forge.getLocalizer().getMessageorUseDefault("lblClassicMode", "Classic Mode"));
            btnAdventure.setCommand(e -> {
                if (FSkin.getLogo() == null) {
                    hideBG = true;
                    hideBtn = true;
                    bgAnimation.progress = 0;
                    bgAnimation.openAdventure = true;
                } else {
                    hideBtn = true;
                    animateLogo = true;
                    bgAnimation.progress = 0;
                    bgAnimation.openAdventure = true;
                }
            });
            btnHome.setCommand(e -> {
                if (FSkin.getLogo() == null) {
                    hideBG = true;
                    hideBtn = true;
                    bgAnimation.progress = 0;
                    bgAnimation.openAdventure = false;
                } else {
                    hideBtn = true;
                    animateLogo = true;
                    bgAnimation.progress = 0;
                    bgAnimation.openAdventure = false;
                }
            });
            float btn_w = (w - 2 * padding);
            float btn_x = x + padding;
            float multiplier = Forge.isLandscapeMode() ? 1 : 1.2f;
            float btn_y = (y + padding) * multiplier;
            btnHome.setFont(FSkinFont.get(22));
            btnAdventure.setFont(FSkinFont.get(22));
            btnHome.setBounds(btn_x, btn_y, btn_w, height);
            add(btnHome);
            btnAdventure.setBounds(btn_x, btn_y + height + padding / 2, btn_w, height);
            add(btnAdventure);
        }
    }

    private void showSplash(Graphics g, float alpha) {
        float oldAlpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(alpha);
        drawDisclaimer(g);
        g.setAlphaComposite(oldAlpha);
    }

    void drawDisclaimer(Graphics g) {
        if (splashTexture == null) {
            return;
        }
        g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());

        float x, y, w, h;
        float backgroundRatio = (float) splashTexture.getRegionWidth() / splashTexture.getRegionHeight();
        float screenRatio = getWidth() / getHeight();
        if (backgroundRatio > screenRatio) {
            x = 0;
            w = getWidth();
            h = getWidth() * backgroundRatio;
            y = (getHeight() - h) / 2;
        } else {
            y = 0;
            h = getHeight();
            w = getHeight() / backgroundRatio;
            x = (getWidth() - w) / 2;
        }
        g.drawImage(splashTexture, x, y, w, h);

        y += h * 295f / 450f;
        if (disclaimerFont == null) {
            disclaimerFont = FSkinFont.get(9);
        }
        float disclaimerHeight = 30f / 450f * h;
        String disclaimer = "Forge is not affiliated in any way with Wizards of the Coast.\n"
                + "Forge is open source software, released under the GNU General Public License.";
        if (Forge.forcedEnglishonCJKMissing && !clear) {
            clear = true;
            FSkinFont.preloadAll("");
            disclaimerFont = FSkinFont.get(9);
        }
        g.drawText(disclaimer, disclaimerFont, FProgressBar.SEL_FORE_COLOR,
                x, y, w, disclaimerHeight, true, Align.center, true);

        float padding = 20f / 450f * w;
        float pbHeight = 57f / 450f * h;
        y += 78f / 450f * h;
        progressBar.setBounds(x + padding, y, w - 2 * padding, pbHeight);
        g.draw(progressBar);

        String version = "v." + Forge.getDeviceAdapter().getVersionString();
        g.drawText(version, disclaimerFont, FProgressBar.SEL_FORE_COLOR, x, getHeight() - disclaimerHeight, w, disclaimerHeight, false, Align.center, true);
    }
}
