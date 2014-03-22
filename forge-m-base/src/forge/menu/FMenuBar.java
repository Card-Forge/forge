package forge.menu;

import java.util.ArrayList;
import java.util.List;

import forge.toolbox.FContainer;
import forge.utils.Utils;

public class FMenuBar extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.6f;

    private final List<FMenuTab> tabs = new ArrayList<FMenuTab>();

    public void addTab(FMenuTab tab) {
        tabs.add(add(tab));
    }

    @Override
    protected void doLayout(float width, float height) {
        int visibleTabCount = 0;
        float minWidth = 0;
        for (FMenuTab tab : tabs) {
            if (tab.isVisible()) {
                minWidth += tab.getMinWidth();
                visibleTabCount++;
            }
        }
        float tabWidth;
        float x = 0;
        float dx = (width - minWidth) / visibleTabCount;
        for (FMenuTab tab : tabs) {
            if (tab.isVisible()) {
                tabWidth = tab.getMinWidth() + dx;
                tab.setBounds(x, 0, tabWidth, height);
                x += tabWidth;
            }
        }
    }
}
