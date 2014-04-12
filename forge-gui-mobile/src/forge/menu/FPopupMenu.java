package forge.menu;

import forge.Forge;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;

public abstract class FPopupMenu extends FDropDownMenu {
    FDisplayObject owner;
    float x, y;

    public void show(FDisplayObject owner0, float x0, float y0) {
        owner = owner0;
        x = owner.localToScreenX(x0);
        y = owner.localToScreenY(y0);
        show();
    }

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
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
