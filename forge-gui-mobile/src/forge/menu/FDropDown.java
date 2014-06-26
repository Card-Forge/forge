package forge.menu;

import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.toolbox.FScrollPane;

public abstract class FDropDown extends FScrollPane {
    public static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private Backdrop backdrop;
    private FMenuTab menuTab;
    protected ScrollBounds paneSize;

    public FDropDown() {
        super.setVisible(false); //hide by default
    }

    public FMenuTab getMenuTab() {
        return menuTab;
    }
    public void setMenuTab(FMenuTab menuTab0) {
        menuTab = menuTab0;
    }

    public void update() {
        if (isVisible()) {
            updateSizeAndPosition();
        }
    }

    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    @Override
    public boolean press(float x, float y) {
        return true; //prevent auto-hiding when drop down pressed
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (autoHide()) {
            hide(); //hide when tapped by default if configured for auto-hide
        }
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        return true; //prevent objects behind drop down handling long press
    }

    @Override
    public void setVisible(boolean visible0) {
        if (isVisible() == visible0) { return; }

        //add/remove drop down from current screen or top overlay when its visibility changes
        FContainer container = FOverlay.getTopOverlay();
        if (container == null) {
            container = Forge.getCurrentScreen();
        }
        if (visible0) {
            updateSizeAndPosition();

            if (autoHide()) { //add invisible backdrop if needed to allow auto-hiding when pressing outide drop down
                backdrop = new Backdrop();
                backdrop.setSize(container.getWidth(), container.getHeight());
                container.add(backdrop);
            }
            container.add(this);
        }
        else {
            container.remove(this);
            if (backdrop != null) {
                backdrop.setVisible(false);
                container.remove(backdrop);
                backdrop = null;
            }
        }
        super.setVisible(visible0);
    }

    protected abstract boolean autoHide();
    protected abstract ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight);

    protected void updateSizeAndPosition() {
        if (menuTab == null) { return; }

        FScreen screen = Forge.getCurrentScreen();
        float screenWidth = screen.getWidth();
        float screenHeight = screen.getHeight();

        Vector2 tabScreenPos = menuTab.getScreenPosition();
        float x = tabScreenPos.x;
        float y = tabScreenPos.y + menuTab.getHeight();

        float maxVisibleHeight = screenHeight - VPrompt.HEIGHT - y; //prevent covering prompt
        paneSize = updateAndGetPaneSize(screenWidth, maxVisibleHeight);
        if (x + paneSize.getWidth() > screenWidth) {
            x = screenWidth - paneSize.getWidth();
        }

        setBounds(Math.round(x), Math.round(y), Math.round(paneSize.getWidth()), Math.round(Math.min(paneSize.getHeight(), maxVisibleHeight)));
    }

    @Override
    protected final ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return paneSize;
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, 0, w, h);
    }

    protected boolean drawAboveOverlay() {
        return true; //draw drop downs above screen overlay by default
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.drawRect(2, BORDER_COLOR, 0, 0, w, h); //ensure border shows up on all sides
    }

    protected FDisplayObject getDropDownOwner() {
        return menuTab;
    }

    protected boolean hideBackdropOnPress(float x, float y) {
        FDisplayObject owner = getDropDownOwner();
        if (owner == null || !owner.contains(owner.getLeft() + owner.screenToLocalX(x), owner.getTop() + owner.screenToLocalY(y))) {
            return true; //auto-hide when backdrop pressed unless over owner
        }
        return false;
    }

    protected boolean preventOwnerHandlingBackupTap(float x, float y, int count) {
        //prevent owner handling this tap unless it's a sub menu and not over it
        FDisplayObject owner = getDropDownOwner();
        if (owner instanceof FSubMenu) {
            return owner.contains(owner.getLeft() + owner.screenToLocalX(x), owner.getTop() + owner.screenToLocalY(y));
        }
        return true; 
    }

    private class Backdrop extends FDisplayObject {
        private Backdrop() {
        }

        @Override
        public boolean press(float x, float y) {
            if (hideBackdropOnPress(x, y)) {
                hide();
            }
            return false; //allow press to pass through to object behind backdrop
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!isVisible()) { return false; }
            hide(); //always hide if tapped

            return preventOwnerHandlingBackupTap(x, y, count);
        }

        @Override
        public void draw(Graphics g) {
            //draw nothing for backdrop
        }
    }
}
