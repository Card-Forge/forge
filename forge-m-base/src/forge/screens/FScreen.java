package forge.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.utils.Utils;

public abstract class FScreen extends FContainer {
    public static final float BTN_WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float BTN_HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);
    private static final FSkinColor clr = clrTheme.stepColor(0);
    private static final FSkinColor a100 = clr.alphaColor(100f / 255f);
    private static final FSkinColor d40 = clr.stepColor(-40);
    private static final FSkinColor d80 = clr.stepColor(-80);

    private final FLabel btnBack, lblHeader, btnMenu;

    protected FScreen(boolean showBackButton, String headerCaption, boolean showMenuButton) {
        if (showBackButton) {
            btnBack = add(new FLabel.ButtonBuilder().icon(FSkinImage.BACK).command(new Runnable() {
                @Override
                public void run() {
                    Forge.back();
                }
            }).build());
        }
        else {
            btnBack = null; 
        }
        if (headerCaption != null) {
            lblHeader = add(new FLabel.Builder().text(headerCaption).fontSize(16).align(HAlignment.CENTER).build());
        }
        else {
            lblHeader = null;
        }
        if (showMenuButton) {
            btnMenu = add(new FLabel.ButtonBuilder().icon(FSkinImage.FAVICON).command(new Runnable() {
                @Override
                public void run() {
                    showMenu();
                }
            }).build());
        }
        else {
            btnMenu = null;
        }
    }

    public void onOpen() {
    }

    public boolean onSwitch() {
        return true;
    }

    public boolean onClose() {
        return true;
    }

    public void showMenu() {
        buildMenu();
    }

    protected void buildMenu() {
        
    }

    @Override
    protected final void doLayout(float width, float height) {
        float headerX = 0;
        float insets = 0;
        float headerWidth = width;
        float headerHeight = BTN_HEIGHT;

        if (btnBack != null) {
            btnBack.setBounds(insets, insets, BTN_WIDTH, BTN_HEIGHT);
            headerX = btnBack.getWidth();
            headerWidth -= headerX;
        }
        if (btnMenu != null) {
            btnMenu.setBounds(width - BTN_WIDTH - insets, insets, BTN_WIDTH, BTN_HEIGHT);
            headerWidth -= btnMenu.getWidth();
        }
        if (lblHeader != null) {
            lblHeader.setBounds(headerX, 0, headerWidth, headerHeight);

            doLayout(headerHeight, width, height);
        }
        else {
            doLayout(0, width, height);
        }
    }

    protected abstract void doLayout(float startY, float width, float height);

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
        g.fillRect(clrTheme, 0, 0, w, h);

        if (lblHeader != null) { //draw custom background behind header label
            float x = lblHeader.getLeft() + 6;
            float y = lblHeader.getTop() + 1;
            w -= x;
            h = lblHeader.getHeight() - 2;
            g.fillRect(d80, x, y + 5, w, h - 5);
            g.fillRect(a100, x + 5, y, w - 5, h - 5);
            g.drawRect(d40, x + 5, y, w - 5, h - 5);
        }
    }
}
