package forge.menu;

import forge.Graphics;
import forge.screens.FScreen.Header;

import java.util.ArrayList;
import java.util.List;

public class FMenuBar extends Header {
    private final List<FMenuTab> tabs = new ArrayList<>();

    public void addTab(String text0, FDropDown dropDown0) {
        FMenuTab tab = new FMenuTab(text0, this, dropDown0, tabs.size());
        dropDown0.setMenuTab(tab);
        tabs.add(add(tab));
    }

    public float getPreferredHeight() {
        return Math.round(FMenuTab.FONT.getLineHeight() * 2f/*fixes touch for tall devices - old value 1.5f*/ + 2 * FMenuTab.PADDING);
    }

    public int getTabCount() {
        return tabs.size();
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
        int tabWidth;
        int x = 0;
        float dx = (width - minWidth) / visibleTabCount;
        for (FMenuTab tab : tabs) {
            if (tab.isVisible()) {
                tabWidth = Math.round(tab.getMinWidth() + dx);
                if (x + tabWidth > width) {
                    tabWidth = Math.round(width - x); //prevent final tab extending off screen
                }
                tab.setBounds(x, 0, tabWidth, height);
                x += tabWidth;
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(BACK_COLOR, 0, 0, w, h);
    }

    @Override
    public float doLandscapeLayout(float screenWidth, float screenHeight) {
        return 0;
    }
}
