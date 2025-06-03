package forge.menu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input;
import forge.Forge;
import forge.Graphics;
import forge.screens.FScreen.Header;
import forge.screens.match.MatchController;

public class FMenuBar extends Header {
    private final List<FMenuTab> tabs = new ArrayList<>();
    private int selected = -1;

    public void addTab(String text0, FDropDown dropDown0) {
        addTab(text0, dropDown0, false);
    }

    public void addTab(String text0, FDropDown dropDown0, boolean iconOnly) {
        FMenuTab tab = new FMenuTab(text0, this, dropDown0, tabs.size(), iconOnly);
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
        int iconOnlyTabCount = 0;
        float iconMinWidth = 0;
        for (FMenuTab tab : tabs) {
            if (tab.isVisible()) {
                if (Forge.isLandscapeMode()) {
                    if (!tab.iconOnly) {
                        minWidth += tab.getMinWidth();
                        visibleTabCount++;
                    } else {
                        iconMinWidth += tab.getMinWidth();
                        iconOnlyTabCount++;
                    }
                } else {
                    minWidth += tab.getMinWidth();
                    visibleTabCount++;
                }
            }
        }
        int tabWidth;
        int x = 0;
        float dx = (width - minWidth) / visibleTabCount;
        for (FMenuTab tab : tabs) {
            if (tab.isVisible()) {
                if (tab.iconOnly) {
                    tab.setActiveIcon(MatchController.instance.getGameView().getRevealedCollection() != null);
                }
                tabWidth = Math.round(tab.getMinWidth() + dx);
                if (x + tabWidth > width) {
                    tabWidth = Math.round(width - x); //prevent final tab extending off screen
                }
                tab.setBounds(tab.iconOnly ? 0 : x, 0, tab.iconOnly ? tab.getMinWidth() + FMenuTab.PADDING * 2 : tabWidth, height);
                x += tab.iconOnly ? tab.getMinWidth() + FMenuTab.PADDING * 2 : tabWidth;
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(getBackColor(), 0, 0, w, h);
    }

    @Override
    public float doLandscapeLayout(float screenWidth, float screenHeight) {
        return 0;
    }

    public void setNextSelected() {
        selected++;
        closeAll();
        if (selected > tabs.size())
            selected = 0;
        try {
            tabs.get(selected).showDropDown();
        } catch (Exception e) {
        }
        if (selected > tabs.size()) {
            closeAll();
            selected = tabs.size();
            return;
        }
    }

    public void setPreviousSelected() {
        selected--;
        closeAll();
        if (selected < 0)
            selected = tabs.size();
        try {
            tabs.get(selected).showDropDown();
        } catch (Exception e) {
        }
        if (selected < 0) {
            closeAll();
            selected = -1;
            return;
        }
    }

    public void closeAll() {
        for (FMenuTab fMenuTab : tabs) {
            fMenuTab.hideDropDown();
        }
    }

    public boolean isShowingMenu(boolean anyDropdown) {
        return tabs.stream().anyMatch(tab -> tab.isShowingDropdownMenu(anyDropdown));
    }

    public void clearSelected() {
        selected--;
        if (selected < -1)
            selected = tabs.size();
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Input.Keys.BUTTON_SELECT) { //show menu tabs
            setNextSelected();
            return true;
        }
        return super.keyDown(keyCode);
    }
}
