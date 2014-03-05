package forge.toolbox;

import java.util.Stack;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;

public class FOverlay extends FContainer {
    private static final FSkinColor BACKDROP_COLOR = FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(120);
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
    private static final float CORNER_RADIUS = 10;
    private static final Stack<FOverlay> overlays = new Stack<FOverlay>();

    public FOverlay() {
    }

    @Override
    public void drawBackground(Graphics g) {
        g.fillRect(BACKDROP_COLOR, 0, 0, this.getWidth(), this.getHeight());
        g.drawRoundRect(BORDER_COLOR, 0, 0, this.getWidth(), this.getHeight(), CORNER_RADIUS);
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        if (visible0) {
            overlays.push(this);
        }
        else {
            overlays.pop();
        }
        super.setVisible(visible0);
    }

    public void setTitle(String title0) {
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }
}