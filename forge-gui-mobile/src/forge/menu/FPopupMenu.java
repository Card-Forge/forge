package forge.menu;

import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;

public abstract class FPopupMenu extends FDropDownMenu {
    FDisplayObject owner;
    float x, y;
    private Vector2 pressPoint;

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

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
    }

    @Override
    protected void onHidden() {
        if (pressPoint != null) {
            //if owner kept pressed while open, release when menu hidden
            owner.release(pressPoint.x, pressPoint.y);
        }
    }

    @Override
    protected void updateSizeAndPosition() {
        FScreen screen = Forge.getCurrentScreen();
        float screenWidth = screen.getWidth();
        float screenHeight = screen.getHeight();

        paneSize = updateAndGetPaneSize(screenWidth, screenHeight);
        if (x + paneSize.getWidth() > screenWidth) {
            x = screenWidth - paneSize.getWidth();
        }
        if (y + paneSize.getHeight() > screenHeight) {
            y = screenHeight - paneSize.getHeight();
        }

        setBounds(Math.round(x), Math.round(y), Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));
    }
}
