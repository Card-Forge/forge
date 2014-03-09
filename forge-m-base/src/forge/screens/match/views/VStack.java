package forge.screens.match.views;

import forge.Forge.Graphics;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.utils.Utils;

public class VStack extends FContainer {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = WIDTH * FCardPanel.ASPECT_RATIO;

    public VStack() {
        setSize(WIDTH, HEIGHT);
    }

    public void update() {
        //TODO
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
    }
}
