package forge.menu;

import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

public abstract class FDropDown extends FScrollPane {
    public static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private Backdrop backdrop;
    private FMenuTab menuTab;
    private ScrollBounds paneSize;

    public FDropDown() {
        setVisible(false); //hide by default
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
        this.setVisible(true);
    }

    public void hide() {
        this.setVisible(false);
    }

    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        //add/remove drop down from current screen when its visibility changes
        FScreen screen = Forge.getCurrentScreen();
        if (visible0) {
            updateSizeAndPosition();

            if (autoHide()) { //add invisible backdrop if needed to allow auto-hiding when pressing outide drop down
                backdrop = new Backdrop();
                backdrop.setSize(screen.getWidth(), screen.getHeight());
                screen.add(backdrop);
            }
            screen.add(this);
        }
        else {
            screen.remove(this);
            if (backdrop != null) {
                screen.remove(backdrop);
                backdrop = null;
            }
        }
        super.setVisible(visible0);
    }

    protected abstract boolean autoHide();
    protected abstract ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight);

    private void updateSizeAndPosition() {
        FScreen screen = Forge.getCurrentScreen();
        float screenWidth = screen.getWidth();
        float screenHeight = screen.getHeight();

        Vector2 tabScreenPos = menuTab.getScreenPosition();
        float x = tabScreenPos.x;
        float y = tabScreenPos.y + menuTab.getHeight();

        float maxVisibleHeight = screenHeight - y;
        paneSize = updateAndGetPaneSize(screenWidth, maxVisibleHeight);
        if (x + paneSize.getWidth() > screenWidth) {
            x = screenWidth - paneSize.getWidth();
        }

        setBounds(x, y, paneSize.getWidth(), Math.min(paneSize.getHeight(), maxVisibleHeight));
    }

    @Override
    protected final ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return paneSize;
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.startClip(0, 0, w, h);
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, 0, w, h);
        g.drawRect(1.5f, BORDER_COLOR, 0, 0, w, h); //ensure border shows up on all sides
        g.endClip();
    }

    private class Backdrop extends FDisplayObject {
        private Backdrop() {
        }

        @Override
        public boolean press(float x, float y) {
            if (!menuTab.contains(menuTab.screenToLocalX(x), menuTab.screenToLocalY(y))) {
                hide(); //auto-hide when backdrop pressed unless over menu tab
            }
            return false; //allow press to pass through to object behind backdrop
        }

        @Override
        public void draw(Graphics g) {
            //draw nothing for backdrop
        }
    }
}
