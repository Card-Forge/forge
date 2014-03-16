package forge.screens.match.views;

import forge.Forge.Graphics;
import forge.toolbox.FCardPanel;
import forge.utils.Utils;

public class VStack extends VDisplayArea {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = WIDTH * FCardPanel.ASPECT_RATIO;

    public VStack() {
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
    }
}
