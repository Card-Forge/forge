package forge.menu;

import com.badlogic.gdx.math.Vector2;
import forge.Forge;
import forge.Graphics;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;

public abstract class FPopupMenu extends FDropDownMenu {
    private FDisplayObject owner;
    private float x, y;
    private Vector2 pressPoint, fixedSize;

    public void show(FDisplayObject owner0, float x0, float y0) {
        owner = owner0;
        x = owner.localToScreenX(x0);
        y = owner.localToScreenY(y0);

        if (owner instanceof FLabel || owner instanceof FButton) {
            //if owner is FLabel or FButton, keep them pressed while menu open
            owner.press(x0, y0);
            pressPoint = new Vector2(x0, y0);
        }

        show();
    }

    public void show(float screenX, float screenY, float fixedWidth, float fixedHeight) {
        x = screenX;
        y = screenY;
        fixedSize = new Vector2(fixedWidth, fixedHeight);
        setDropDownContainer(Forge.getCurrentScreen());

        show();
    }

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
    }

    @Override
    protected void onHidden() {
        if (pressPoint != null) {
            //if owner kept pressed while open, release when menu hidden
            owner.release(pressPoint.x, pressPoint.y);
            pressPoint = null;
        }
        owner = null;
        fixedSize = null;
    }

    @Override
    protected void updateSizeAndPosition() {
        if (fixedSize != null) {
            paneSize = updateAndGetPaneSize(fixedSize.x, fixedSize.y);
            setBounds(x, y, fixedSize.x, fixedSize.y);
            for (FMenuItem item : items) {
                item.setTabMode(true); //ensure items in fixed size menu are treated as tabs
            }
            return;
        }

        FScreen screen = Forge.getCurrentScreen();
        float screenWidth = screen.getWidth();
        float screenHeight = screen.getHeight();

        paneSize = updateAndGetPaneSize(screenWidth, screenHeight);

        //round width and height so borders appear properly
        paneSize = new ScrollBounds(Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));

        if (x + paneSize.getWidth() > screenWidth) {
            x = screenWidth - paneSize.getWidth();
        }
        if (y + paneSize.getHeight() > screenHeight) {
            y = screenHeight - paneSize.getHeight();
        }

        setBounds(Math.round(x), Math.round(y), paneSize.getWidth(), paneSize.getHeight());
    }

    @Override
    protected float determineMenuWidth() {
        if (fixedSize != null) {
            return fixedSize.x;
        }
        return super.determineMenuWidth();
    }

    @Override
    protected boolean autoHide() {
        return fixedSize == null; //don't auto-hide if menu has fixed size
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (fixedSize == null) { //avoid showing background if menu has fixed size
            super.drawBackground(g);
        }
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (fixedSize == null) { //avoid showing overlay if menu has fixed size
            super.drawOverlay(g);
        }
    }
}
