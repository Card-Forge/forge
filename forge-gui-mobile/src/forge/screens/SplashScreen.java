package forge.screens;

import com.badlogic.gdx.Gdx;
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

    private static final Color GLASS_EDGE = new Color(1f, 1f, 1f, 0.28f);
    private static final Color TEXT_SOFT = new Color(1f, 1f, 1f, 0.82f);

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
            drawModernBar(g, (Forge.getScreenWidth() - w2 * 0.8f)/2, Forge.getScreenHeight() - h2 * 2f + h2 * 0.25f, w2 * 0.8f, h2 * 0.5f);
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
                Forge.maybePromptForBulkCdnSync();
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

    private float shownFraction;
    public boolean isBarSettled() { return shownFraction >= 0.999f; }
    //slim pill progress bar in the theme's progress colours: a translucent track with a thin light edge, the fill and the text on it
    private void drawModernBar(Graphics g, float x, float y, float w, float h) {
        final float radius = h / 2;
        final Color back = FProgressBar.getBackColor(), sel = FProgressBar.getSelBackColor();
        g.fillRoundRect(GLASS_EDGE, x, y, w, h, radius);
        final float edge = Math.max(1f, h * 0.06f);
        g.fillRoundRect(new Color(back.r, back.g, back.b, 0.82f), x + edge, y + edge, w - 2 * edge, h - 2 * edge, radius - edge);
        final float max = Math.max(1, progressBar.getMaximum());
        final float target = Math.max(0f, Math.min(1f, progressBar.getValue() / max));
        final float dt = Gdx.graphics.getDeltaTime();
        shownFraction += (target - shownFraction) * Math.min(1f, dt * 10f);
        if (target - shownFraction < 0.002f) { shownFraction = target; }
        if (shownFraction != target) { Gdx.graphics.requestRendering(); }
        final float fraction = shownFraction;
        if (fraction > 0f) {
            final float inner = edge * 2;
            final float fillW = Math.max(h - 2 * inner, (w - 2 * inner) * fraction);
            g.fillRoundRect(sel, x + inner, y + inner, fillW, h - 2 * inner, (h - 2 * inner) / 2);
        }
        final String message = progressBar.getMessage();
        if (message != null) {
            g.drawText(message, FSkinFont.get(11), FProgressBar.getSelForeColor(), x + radius, y, w - 2 * radius, h, false, Align.center, true);
        }
    }

    //the splash artwork fills the screen: a dimmed, cropped copy behind the centred original, which fades into it at the sides
    private void drawSplashBackdrop(Graphics g, float x, float y, float w, float h) {
        final float sw = getWidth(), sh = getHeight();
        final float scale = Math.max(sw / w, sh / h);
        final float cw = w * scale, ch = h * scale;
        g.drawImage(splashTexture, (sw - cw) / 2, (sh - ch) / 2, cw, ch);
        final Color dim = new Color(0f, 0f, 0f, 0.62f), clear = new Color(0f, 0f, 0f, 0f);
        g.fillRect(dim, 0, 0, sw, sh);
        g.drawImage(splashTexture, x, y, w, h);
        final float fade = Math.min(w * 0.18f, Math.max(0f, x)); //only where the artwork doesn't reach the screen edge
        if (fade > 0) {
            g.fillGradientRect(dim, clear, false, x, y, fade, h);
            g.fillGradientRect(clear, dim, false, x + w - fade, y, fade, h);
        }
        g.fillGradientRect(new Color(0f, 0f, 0f, 0.35f), clear, true, 0, 0, sw, sh * 0.25f);
        g.fillGradientRect(clear, new Color(0f, 0f, 0f, 0.55f), true, 0, sh * 0.6f, sw, sh * 0.4f);
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
        drawSplashBackdrop(g, x, y, w, h);

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
        g.drawText(disclaimer, disclaimerFont, TEXT_SOFT,
                x, y, w, disclaimerHeight, true, Align.center, true);

        float padding = 20f / 450f * w;
        float pbHeight = 57f / 450f * h;
        y += 78f / 450f * h;
        progressBar.setBounds(x + padding, y, w - 2 * padding, pbHeight);
        final float barW = Math.min(w - 2 * padding, getWidth() * 0.5f), barH = pbHeight * 0.45f;
        drawModernBar(g, (getWidth() - barW) / 2, y + (pbHeight - barH) / 2, barW, barH);

        String version = "v." + Forge.getDeviceAdapter().getVersionString();
        g.drawText(version, disclaimerFont, FProgressBar.SEL_FORE_COLOR, x, getHeight() - disclaimerHeight, w, disclaimerHeight, false, Align.center, true);
    }
}
