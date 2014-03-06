package forge.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.utils.Utils;

public abstract class FScreen extends FContainer {
    public static final float BTN_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.8f;
    public static final float BTN_WIDTH = BTN_HEIGHT;

    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);
    private static final FSkinColor clr = clrTheme.stepColor(0);
    private static final FSkinColor d40 = clr.stepColor(-40);
    private static final FSkinColor d80 = clr.stepColor(-80);

    private final FLabel btnBack, lblHeader, btnMenu;

    protected FScreen(boolean showBackButton, String headerCaption, boolean showMenuButton) {
        if (showBackButton) {
            btnBack = add(new FLabel.Builder().icon(new BackIcon()).pressedColor(clr).align(HAlignment.CENTER).command(new Runnable() {
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
            btnMenu = add(new FLabel.Builder().icon(FSkinImage.FAVICON).pressedColor(clr).align(HAlignment.CENTER).command(new Runnable() {
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
        Forge.openScreen(new forge.screens.settings.SettingsScreen()); //TODO: Build menu containing settings item
        //buildMenu();
    }

    protected void buildMenu() {
        
    }

    @Override
    protected final void doLayout(float width, float height) {
        float headerX = BTN_WIDTH;
        float headerWidth = width - 2 * headerX;
        float headerHeight = BTN_HEIGHT;

        if (btnBack != null) {
            btnBack.setBounds(0, 0, BTN_WIDTH, BTN_HEIGHT);
        }
        if (btnMenu != null) {
            btnMenu.setBounds(width - BTN_WIDTH, 0, BTN_WIDTH, BTN_HEIGHT);
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
            g.fillRect(d80, 0, 0, w, BTN_HEIGHT);
            g.drawLine(1, d40, 0, BTN_HEIGHT, w, BTN_HEIGHT);
        }
    }

    private static class BackIcon implements FImage {
        private static final float THICKNESS = 3;
        private static final FSkinColor COLOR = FSkinColor.get(Colors.CLR_TEXT);

        @Override
        public float getWidth() {
            return BTN_WIDTH;
        }

        @Override
        public float getHeight() {
            return BTN_HEIGHT;
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            float xMid = x + w / 3; 
            float yMid = y + h / 2;
            float offsetX = h / 8;
            float offsetY = w / 4;

            g.drawLine(THICKNESS, COLOR, xMid + offsetX, yMid - offsetY, xMid - offsetX, yMid + 1);
            g.drawLine(THICKNESS, COLOR, xMid - offsetX, yMid  - 1, xMid + offsetX, yMid + offsetY);
        }
    }
}
