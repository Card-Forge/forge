package forge.menu;

import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.toolbox.FScrollPane;

public abstract class FDropDown extends FScrollPane {
    public static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private Backdrop backdrop;
    private FMenuTab menuTab;
    private FContainer dropDownContainer;
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

    public FContainer getDropDownContainer() {
        return dropDownContainer;
    }
    public void setDropDownContainer(FContainer dropDownContainer0) {
        dropDownContainer = dropDownContainer0;
    }

    protected FContainer getContainer() {
        FContainer container = dropDownContainer;
        if (container == null) {
            container = FOverlay.getTopOverlay();
            if (container == null) {
                container = Forge.getCurrentScreen();
            }
        }
        return container;
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
    public boolean flick(float x, float y) {
        return true; //prevent objects behind drop down handling flick
    }

    @Override
    public void setVisible(boolean visible0) {
        if (isVisible() == visible0) { return; }

        //add/remove drop down from its container, current screen, or top overlay when its visibility changes
        FContainer container = getContainer();
        if (visible0) {
            updateSizeAndPosition();

            if (autoHide()) { //add invisible backdrop if needed to allow auto-hiding when pressing outside drop down
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
            onHidden();
        }
        super.setVisible(visible0);
    }

    protected void onHidden() {
    }

    protected abstract boolean autoHide();
    protected abstract ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight);

    protected void updateSizeAndPosition() {
        if (menuTab == null) { return; }

        Rectangle boundary = Forge.getCurrentScreen().getDropDownBoundary();

        float x = menuTab.screenPos.x;
        float y = menuTab.screenPos.y + menuTab.getHeight();
        boolean showAbove;
        float maxVisibleHeight;
        if (y < boundary.y + boundary.height / 2) {
            showAbove = false;
            maxVisibleHeight = boundary.y + boundary.height - y; //prevent covering prompt
        }
        else { //handle drop downs at near bottom of screen
            showAbove = true;
            y = menuTab.screenPos.y;
            maxVisibleHeight = y - boundary.y;
        }

        paneSize = updateAndGetPaneSize(boundary.width, maxVisibleHeight);

        //round width and height so borders appear properly
        paneSize = new ScrollBounds(Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));
        if (x + paneSize.getWidth() > boundary.x + boundary.width) {
            x = boundary.x + boundary.width - paneSize.getWidth();
        }
        float height = Math.min(paneSize.getHeight(), maxVisibleHeight);
        if (showAbove) {
            //make drop down appear above
            y -= height;
        }

        setBounds(Math.round(x), Math.round(y), paneSize.getWidth(), height);
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
        super.drawOverlay(g);
        float w = getWidth();
        float h = getHeight();
        g.drawRect(2, BORDER_COLOR, 0, 0, w, h); //ensure border shows up on all sides
    }

    protected FDisplayObject getDropDownOwner() {
        return menuTab;
    }

    protected boolean hideBackdropOnPress(float x, float y) {
        FDisplayObject owner = getDropDownOwner();
        return owner == null || !owner.screenPos.contains(x, y); //auto-hide when backdrop pressed unless over owner
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
            if (hideBackdropOnPress(localToScreenX(x), y)) {
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
        public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
            if (!GuiBase.isAndroid())
                hide(); //always hide if backdrop panned
            return false; //allow pan to pass through to object behind backdrop
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            if (!GuiBase.isAndroid())
                hide(); //always hide if backdrop flung
            return false; //allow fling to pass through to object behind backdrop
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            if (!GuiBase.isAndroid())
                hide(); //always hide if backdrop zoomed
            return false; //allow zoom to pass through to object behind backdrop
        }

        @Override
        public void draw(Graphics g) {
            //draw nothing for backdrop
        }
    }
}
