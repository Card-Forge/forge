package forge.screens;

import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.menu.FPopupMenu;
import forge.screens.home.HomeScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Callback;
import forge.util.Utils;

public abstract class FScreen extends FContainer {
    public static final FSkinColor TEXTURE_OVERLAY_COLOR = FSkinColor.get(Colors.CLR_THEME);

    private final Header header;

    protected FScreen(String headerCaption) {
        this(headerCaption == null ? null : new DefaultHeader(headerCaption));
    }
    protected FScreen(String headerCaption, FPopupMenu menu) {
        this(new MenuHeader(headerCaption, menu));
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

    public Rectangle getDropDownBoundary() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }

    public void onActivate() {
    }

    public void onSwitchAway(Callback<Boolean> canSwitchCallback) {
        canSwitchCallback.run(true);
    }

    public void onClose(Callback<Boolean> canCloseCallback) {
        if (canCloseCallback != null) { //will be null if app exited
            canCloseCallback.run(true);
        }
    }

    public void showMenu() {
        if (header instanceof MenuHeader) {
            ((MenuHeader)header).btnMenu.trigger();
        }
        else { //just so settings screen if no menu header
            SettingsScreen.show(false);
        }
    }

    @Override
    protected final void doLayout(float width, float height) {
        if (width > height) { //handle landscape layout special
            doLandscapeLayout(width, height);
        }
        else if (header != null) {
            header.setBounds(0, 0, width, header.getPreferredHeight());
            doLayout(header.getHeight(), width, height);
        }
        else {
            doLayout(0, width, height);
        }
    }

    protected abstract void doLayout(float startY, float width, float height);

    //do layout for landscape mode and return width for any screen hosted on top of this screen
    protected float doLandscapeLayout(float width, float height) {
        //just use normal doLayout function by default after making room for header menu
        float startY = 0;
        if (header != null) {
            float headerWidth = header.doLandscapeLayout(width, height);
            if (headerWidth == 0) { //if header doesn't support landscape layout, make room for it at top
                header.setBounds(0, 0, width, header.getPreferredHeight());
                startY += header.getHeight();
            }
            else { //otherwise make room for it on right
                width -= headerWidth;
            }
        }
        doLayout(startY, width, height);
        return width;
    }

    //get screen to serve as the backdrop for this screen when in landscape mode
    public FScreen getLandscapeBackdropScreen() {
        return HomeScreen.instance; //use home screen as backdrop when in landscape mode by default
    }

    @Override
    public void setSize(float width, float height) {
        if (Forge.isLandscapeMode()) {
            //adjust size if in landscape mode and has a backdrop
            FScreen backdrop = getLandscapeBackdropScreen();
            if (backdrop != null) {
                width = backdrop.doLandscapeLayout(width, height);
            }
        }
        if (getWidth() == width && getHeight() == height) {
            if (header != null) {
                header.onScreenActivate(); //let header handle when screen activated
            }
            return;
        }
        super.setSize(width, height);
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        if (Forge.isLandscapeMode()) {
            //allow touch events on backdrop screen if any
            FScreen backdrop = getLandscapeBackdropScreen();
            if (backdrop != null) {
                backdrop.buildTouchListeners(screenX, screenY, listeners);
            }
        }
        super.buildTouchListeners(screenX, screenY, listeners);
    }

    @Override
    public void draw(Graphics g) {
        if (Forge.isLandscapeMode() && getLeft() == 0) { //check that left is 0 to avoid infinite loop
            //draw landscape backdrop first if needed
            FScreen backdrop = getLandscapeBackdropScreen();
            if (backdrop != null) {
                g.draw(backdrop);
                //temporarily shift into position for drawing in front of backdrop
                setLeft(Forge.getScreenWidth() - getWidth());
                g.draw(this);
                setLeft(0);
                return;
            }
        }
        super.draw(g);
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (Forge.isLandscapeMode() && getLandscapeBackdropScreen() != null) {
            return; //don't draw background if this screen has a backdrop
        }
        float w = getWidth();
        float h = getHeight();
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
        g.fillRect(TEXTURE_OVERLAY_COLOR, 0, 0, w, h);
    }

    public static abstract class Header extends FContainer {
        public static final FSkinColor BTN_PRESSED_COLOR = TEXTURE_OVERLAY_COLOR.alphaColor(1f);
        public static final FSkinColor LINE_COLOR = BTN_PRESSED_COLOR.stepColor(-40);
        public static final FSkinColor BACK_COLOR = BTN_PRESSED_COLOR.stepColor(-80);
        public static final float LINE_THICKNESS = Utils.scale(1);

        public abstract float getPreferredHeight();

        //handle when screen activated
        protected void onScreenActivate() {
        }

        //do layout for landscape mode and return needed width
        public abstract float doLandscapeLayout(float screenWidth, float screenHeight);
    }
    private static class DefaultHeader extends Header {
        protected static final float HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);
        protected static final FSkinFont FONT = FSkinFont.get(16);

        protected final FLabel btnBack, lblCaption;

        public DefaultHeader(String headerCaption) {
            btnBack = add(new FLabel.Builder().icon(new BackIcon(HEIGHT, HEIGHT)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    Forge.back();
                }
            }).build());
            lblCaption = add(new FLabel.Builder().text(headerCaption).font(FONT).align(HAlignment.CENTER).build());
        }

        @Override
        public float getPreferredHeight() {
            return HEIGHT;
        }

        @Override
        public float doLandscapeLayout(float screenWidth, float screenHeight) {
            return 0; //default header doesn't need to display for landscape mode
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        public void drawOverlay(Graphics g) {
            if (Forge.isLandscapeMode() && getWidth() < Forge.getCurrentScreen().getWidth()) {
                //in landscape mode, draw left border for sidebar if needed
                g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, 0, 0, getHeight());
                return;
            }
            float y = HEIGHT - LINE_THICKNESS / 2;
            g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y, getWidth(), y);
        }

        @Override
        protected void doLayout(float width, float height) {
            btnBack.setBounds(0, 0, height, height);
            lblCaption.setBounds(height, 0, width - 2 * height, height);
        }
    }
    protected static class MenuHeader extends DefaultHeader {
        private final FLabel btnMenu;
        private final FPopupMenu menu;

        public MenuHeader(String headerCaption, FPopupMenu menu0) {
            super(headerCaption);
            menu = menu0;
            btnMenu = add(new FLabel.Builder().icon(new MenuIcon(HEIGHT, HEIGHT)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    menu.show(btnMenu, 0, HEIGHT);
                }
            }).build());
        }

        @Override
        public void drawOverlay(Graphics g) {
            if (Forge.isLandscapeMode() && displaySidebarForLandscapeMode()) {
                //in landscape mode, draw left border for header
                g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, 0, 0, getHeight());
                return;
            }
            super.drawOverlay(g);
        }

        @Override
        protected void doLayout(float width, float height) {
            super.doLayout(width, height);

            menu.hide(); //ensure menu hidden when screen resized

            if (Forge.isLandscapeMode() && displaySidebarForLandscapeMode()) {
                //for landscape mode, hide menu button and display menu as sidebar
                btnBack.setBounds(0, 0, 0, 0);
                lblCaption.setBounds(0, 0, 0, 0);
                btnMenu.setBounds(0, 0, 0, 0);
                menu.show(getLeft(), 0, width, height);
                return;
            }

            btnMenu.setBounds(width - height, 0, height, height);
        }

        @Override
        protected void onScreenActivate() {
            //ensure menu layout refreshed for sidebar when screen activated
            if (Forge.isLandscapeMode() && displaySidebarForLandscapeMode()) {
                menu.hide();
                menu.show(getLeft(), 0, getWidth(), getHeight());
            }
        }

        protected boolean displaySidebarForLandscapeMode() {
            return true;
        }

        @Override
        public float doLandscapeLayout(float screenWidth, float screenHeight) {
            if (displaySidebarForLandscapeMode()) {
                float width = screenHeight * HomeScreen.MAIN_MENU_WIDTH_FACTOR * 0.8f;
                setBounds(screenWidth - width, 0, width, screenHeight);
                return width;
            }
            return 0;
        }
    }

    protected static class BackIcon implements FImage {
        private static final float THICKNESS = Utils.scale(3);
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
            g.drawLine(THICKNESS, COLOR, xMid - offsetX, yMid - 1, xMid + offsetX, yMid + offsetY);
        }
    }

    protected static class MenuIcon implements FImage {
        private static final FSkinColor COLOR = FSkinColor.get(Colors.CLR_TEXT);

        private final float width, height;
        public MenuIcon(float width0, float height0) {
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
            float thickness = Math.round(h / 5);
            float delta = Math.round(thickness * 1.75f);
            y += (h - 2 * delta - thickness) / 2;

            g.fillRect(COLOR, x, y, thickness, thickness);
            y += delta;
            g.fillRect(COLOR, x, y, thickness, thickness);
            y += delta;
            g.fillRect(COLOR, x, y, thickness, thickness);
            x += delta;
            y -= 2 * delta;
            w -= delta;
            g.fillRect(COLOR, x, y, w, thickness);
            y += delta;
            g.fillRect(COLOR, x, y, w, thickness);
            y += delta;
            g.fillRect(COLOR, x, y, w, thickness);
        }
    }

    protected boolean allowBackInLandscapeMode() {
        return getLandscapeBackdropScreen() != HomeScreen.instance; //don't allow going back if home screen is backdrop by default
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            if (Forge.endKeyInput()) { return true; }

            if (Forge.isLandscapeMode() && !allowBackInLandscapeMode()) {
                Forge.exit(false); //prompt to exit if attempting to go back from screen that doesn't allow back in landscape mode
                return true;
            }
            Forge.back(); //go back on escape by default
            return true;
        }
        if (keyCode == Keys.F5) { //allow revalidating current screen when running desktop app
            revalidate();
        }
        return super.keyDown(keyCode);
    }
}
