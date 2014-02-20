package forge;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FContainer;

public abstract class FScreen extends FContainer {
    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);

    public void onOpen() {
    }

    public boolean onSwitch() {
        return true;
    }

    public boolean onClose() {
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        g.drawImage(FSkinImage.BG_TEXTURE, 0, 0, getWidth(), getHeight());
        g.fillRect(clrTheme, 0, 0, getWidth(), getHeight());
    }
}
