package forge.screens;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public abstract class FScreen extends FContainer {
    public static final FSkinColor TEXTURE_OVERLAY_COLOR = FSkinColor.get(Colors.CLR_THEME);
    public static final FSkinColor HEADER_BTN_PRESSED_COLOR = TEXTURE_OVERLAY_COLOR.alphaColor(1f);
    public static final FSkinColor HEADER_LINE_COLOR = HEADER_BTN_PRESSED_COLOR.stepColor(-40);
    public static final FSkinColor HEADER_BACK_COLOR = HEADER_BTN_PRESSED_COLOR.stepColor(-80);

    public static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);

    private final FLabel btnBack, lblHeader, btnMenu;

    protected FScreen(boolean showBackButton, String headerCaption, boolean showMenuButton) {
        if (showBackButton) {
            btnBack = add(new FLabel.Builder().icon(new BackIcon()).pressedColor(HEADER_BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
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
            btnMenu = add(new FLabel.Builder().icon(FSkinImage.FAVICON).pressedColor(HEADER_BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    showMenu();
                }
            }).build());
        }
        else {
            btnMenu = null;
        }
    }

    public String getHeaderCaption() {
        if (lblHeader == null) { return ""; }
        return lblHeader.getText();
    }
    public void setHeaderCaption(String headerCaption) {
        if (lblHeader == null) { return; }
        lblHeader.setText(headerCaption);
    }

    public void onActivate() {
    }

    public boolean onSwitchAway() {
        return true;
    }

    public boolean onClose(boolean canCancel) {
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
        float headerX = HEADER_HEIGHT;
        float headerWidth = width - 2 * headerX;

        if (btnBack != null) {
            btnBack.setBounds(0, 0, HEADER_HEIGHT, HEADER_HEIGHT);
        }
        if (btnMenu != null) {
            btnMenu.setBounds(width - HEADER_HEIGHT, 0, HEADER_HEIGHT, HEADER_HEIGHT);
        }
        if (lblHeader != null) {
            lblHeader.setBounds(headerX, 0, headerWidth, HEADER_HEIGHT);

            doLayout(HEADER_HEIGHT, width, height);
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
        g.fillRect(TEXTURE_OVERLAY_COLOR, 0, 0, w, h);

        if (lblHeader != null) { //draw custom background behind header label
            g.fillRect(HEADER_BACK_COLOR, 0, 0, w, HEADER_HEIGHT);
            g.drawLine(1, HEADER_LINE_COLOR, 0, HEADER_HEIGHT, w, HEADER_HEIGHT);
        }
    }

    private static class BackIcon implements FImage {
        private static final float THICKNESS = Utils.scaleMax(3);
        private static final FSkinColor COLOR = FSkinColor.get(Colors.CLR_TEXT);

        @Override
        public float getWidth() {
            return HEADER_HEIGHT;
        }

        @Override
        public float getHeight() {
            return HEADER_HEIGHT;
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

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            if (Forge.endKeyInput()) { return true; }

            Forge.back(); //go back on escape by default
            return true;
        }
        return super.keyDown(keyCode);
    }
}
