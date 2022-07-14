package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FProgressBar;

public class SplashScreen extends FContainer {
    private TextureRegion background;
    private final FProgressBar progressBar;
    private FSkinFont disclaimerFont;
    private boolean preparedForDialogs, showModeSelector, init, animateLogo, hideBG, hideBtn, startClassic, clear;
    private FButton btnAdventure, btnHome;
    private BGAnimation bgAnimation;

    public SplashScreen() {
        progressBar = new FProgressBar();
        progressBar.setDescription("Welcome to Forge");
        bgAnimation = new BGAnimation();
    }

    public FProgressBar getProgressBar() {
        return progressBar;
    }

    public void setBackground(TextureRegion background0) {
        background = background0;
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
                    g.drawImage(FSkin.hdLogo, getWidth() / 2 - (FSkin.hdLogo.getWidth() * xmod) / 2, getHeight() / 2 - (FSkin.hdLogo.getHeight() * xmod) / 1.5f, FSkin.hdLogo.getWidth() * xmod, FSkin.hdLogo.getHeight() * xmod);
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
        bgAnimation.drawBackground(g);
    }

    void drawTransition(Graphics g, boolean openAdventure, float percentage) {
        TextureRegion tr = new TextureRegion(Forge.getAssets().fallback_skins.get(0));
        if (!Forge.isLandscapeMode() && tr != null) {
            float ar = 1.78f;
            int w = (int) (tr.getRegionHeight() / ar);
            int x = (int) ((tr.getRegionWidth() - w) / ar);
            tr.setRegion(x, 0, w, tr.getRegionHeight());
        }
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
        if (background == null) {
            return;
        }
        g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());

        float x, y, w, h;
        float backgroundRatio = (float) background.getRegionWidth() / background.getRegionHeight();
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
        if (FSkin.hdLogo != null) {
            float xmod = Forge.getScreenHeight() > 1000 ? 1.5f : Forge.getScreenHeight() > 800 ? 1.3f : 1f;
            g.drawImage(FSkin.hdLogo, getWidth() / 2 - (FSkin.hdLogo.getWidth() * xmod) / 2, getHeight() / 2 - (FSkin.hdLogo.getHeight() * xmod) / 1.5f, FSkin.hdLogo.getWidth() * xmod, FSkin.hdLogo.getHeight() * xmod);
        } else {
            g.drawImage(background, x, y, w, h);
        }
        y += h * 295f / 450f;
        float padding = 20f / 450f * w;
        float height = 57f / 450f * h;

        if (!init) {
            init = true;
            btnAdventure = new FButton(Forge.getLocalizer().getMessageorUseDefault("lblAdventureMode", "Adventure Mode"));
            btnHome = new FButton(Forge.getLocalizer().getMessageorUseDefault("lblClassicMode", "Classic Mode"));
            btnAdventure.setCommand(new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (FSkin.hdLogo == null) {
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
                }
            });
            btnHome.setCommand(new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (FSkin.hdLogo == null) {
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
        if (background == null) {
            return;
        }
        g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());

        float x, y, w, h;
        float backgroundRatio = (float) background.getRegionWidth() / background.getRegionHeight();
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
        g.drawImage(background, x, y, w, h);

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

        String version = "v. " + Forge.CURRENT_VERSION;
        g.drawText(version, disclaimerFont, FProgressBar.SEL_FORE_COLOR, x, getHeight() - disclaimerHeight, w, disclaimerHeight, false, Align.center, true);
    }
}
