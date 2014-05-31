package forge.screens;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public abstract class FScreen extends FContainer {
    public static final FSkinColor TEXTURE_OVERLAY_COLOR = FSkinColor.get(Colors.CLR_THEME);

    private final Header header;

    protected FScreen(String headerCaption) {
        this(headerCaption == null ? null : new DefaultHeader(headerCaption));
    }
    protected FScreen(Header header0) {
        header = header0;
        if (header != null) {
            add(header);
        }
    }

    public Header getHeader() {
        return header;
    }

    public void setHeaderCaption(String headerCaption) {
        if (header instanceof DefaultHeader) {
            ((DefaultHeader)header).lblCaption.setText(headerCaption);
        }
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
        SettingsScreen.show(); //TODO: Build menu containing settings item
        //buildMenu();
    }

    protected void buildMenu() {
        
    }

    @Override
    protected final void doLayout(float width, float height) {
        if (header != null) {
            header.setBounds(0, 0, width, header.getPreferredHeight());
            doLayout(header.getHeight(), width, height);
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
    }

    public static abstract class Header extends FContainer {
        public static final FSkinColor BTN_PRESSED_COLOR = TEXTURE_OVERLAY_COLOR.alphaColor(1f);
        public static final FSkinColor LINE_COLOR = BTN_PRESSED_COLOR.stepColor(-40);
        public static final FSkinColor BACK_COLOR = BTN_PRESSED_COLOR.stepColor(-80);
        public static final float LINE_THICKNESS = Utils.scaleY(1);

        public abstract float getPreferredHeight();
    }
    private static class DefaultHeader extends Header {
        private static final float HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);
        private static final FSkinFont FONT = FSkinFont.get(16);

        private final FLabel btnBack, lblCaption;

        public DefaultHeader(String headerCaption) {
            btnBack = add(new FLabel.Builder().icon(new BackIcon(HEIGHT, HEIGHT)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    Forge.back();
                }
            }).build());
            btnBack.setSize(HEIGHT, HEIGHT);
            lblCaption = add(new FLabel.Builder().text(headerCaption).font(FONT).align(HAlignment.CENTER).build());
        }

        @Override
        public float getPreferredHeight() {
            return HEIGHT;
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), HEIGHT);
        }

        @Override
        public void drawOverlay(Graphics g) {
            float y = HEIGHT - LINE_THICKNESS / 2;
            g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y, getWidth(), y);
        }

        @Override
        protected void doLayout(float width, float height) {
            lblCaption.setBounds(height, 0, width - 2 * height, height);
        }
    }

    protected static class BackIcon implements FImage {
        private static final float THICKNESS = Utils.scaleMax(3);
        private static final FSkinColor COLOR = FSkinColor.get(Colors.CLR_TEXT);

        private final float width, height;
        public BackIcon(float width0, float height0) {
            width = width0;
            height = height0;
        }

        @Override
        public float getWidth() {
            return width;
        }

        @Override
        public float getHeight() {
            return height;
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            float xMid = x + w * 0.4f; 
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
