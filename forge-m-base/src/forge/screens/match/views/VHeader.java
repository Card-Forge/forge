package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.utils.ForgePreferences.FPref;
import forge.utils.Utils;

public class VHeader extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.6f;

    private static final FSkinFont TAB_FONT = FSkinFont.get(12);
    private static final FSkinColor TAB_SEL_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor TAB_FORE_COLOR = TAB_SEL_FORE_COLOR.alphaColor(0.5f);
    private static final FSkinColor DISPLAY_AREA_BACK_COLOR = FSkinColor.get(Colors.CLR_INACTIVE).alphaColor(0.5f);

    private final List<HeaderTab> tabs = new ArrayList<HeaderTab>();
    private HeaderTab selectedTab;

    public VHeader() {
        addTab("Game", new VLog());
        addTab("Players", new VPlayers());
        addTab("Log", new VLog());
        addTab("Combat", new VCombat());
        addTab("Dev", new VDev());
        addTab("Stack", new VStack());
    }

    private void addTab(String text, VDisplayArea displayArea) {
        tabs.add(add(new HeaderTab(text, displayArea)));
    }

    public HeaderTab getTabGame() {
        return tabs.get(0);
    }

    public HeaderTab getTabPlayers() {
        return tabs.get(1);
    }
    
    public HeaderTab getTabLog() {
        return tabs.get(2);
    }
    
    public HeaderTab getTabCombat() {
        return tabs.get(3);
    }
    
    public HeaderTab getTabDev() {
        return tabs.get(4);
    }
    
    public HeaderTab getTabStack() {
        return tabs.get(5);
    }

    public HeaderTab getSelectedTab() {
        return selectedTab;
    }

    private void setSelectedTab(HeaderTab selectedTab0) {
        if (selectedTab == selectedTab0) {
            return;
        }

        if (selectedTab != null) {
            selectedTab.displayArea.setVisible(false);
        }

        selectedTab = selectedTab0;

        if (selectedTab != null) {
            selectedTab.displayArea.setVisible(true);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        getTabDev().setVisible(FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED));

        int visibleTabCount = 0;
        float minWidth = 0;
        for (HeaderTab tab : tabs) {
            if (tab.isVisible()) {
                minWidth += tab.minWidth;
                visibleTabCount++;
            }
        }
        float tabWidth;
        float x = 0;
        float dx = (width - minWidth) / visibleTabCount;
        for (HeaderTab tab : tabs) {
            if (tab.isVisible()) {
                tabWidth = tab.minWidth + dx;
                tab.setBounds(x, 0, tabWidth, height);
                x += tabWidth;
            }
        }
    }

    @Override
    public void drawBackground(Graphics g) { 
        float w = getWidth();
        float h = getHeight();
        g.fillRect(FScreen.HEADER_BACK_COLOR, 0, 0, w, h);

        if (selectedTab == null) { 
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, h, w, h);
        }
        else { //draw background and border for selected zone if needed
            VDisplayArea selectedDisplayArea = selectedTab.displayArea;
            g.fillRect(DISPLAY_AREA_BACK_COLOR, 0, selectedDisplayArea.getTop(), w, selectedDisplayArea.getHeight());

            //leave gap at selected zone tab
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, h, selectedTab.getLeft(), h);
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, selectedTab.getRight(), h, w, h);
        }
    }

    public class HeaderTab extends FDisplayObject {
        private String text;
        private String caption;
        private float minWidth;
        private final VDisplayArea displayArea;

        private HeaderTab(String text0, VDisplayArea displayArea0) {
            text = text0;
            displayArea = displayArea0;
            update();
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (selectedTab == this) {
                setSelectedTab(null);
            }
            else {
                setSelectedTab(this);
            }
            return true;
        }

        public void update() {
            displayArea.update();
            int count = displayArea.getCount();
            if (count >= 0) {
                caption = text + " (" + String.valueOf(count) + ")";
            }
            else { //if getCount() returns -1, don't include count in caption
                caption = text;
            }
            minWidth = TAB_FONT.getFont().getBounds(caption).width;
            VHeader.this.revalidate();
        }

        @Override
        public void draw(Graphics g) {
            float x, y, w, h;
            float paddingX = 2;
            float paddingY = 2;

            FSkinColor foreColor;
            if (selectedTab == this) {
                y = 0;
                w = getWidth();
                h = getHeight();
                float yAcross;
                y += paddingY;
                yAcross = y;
                y--;
                h++;
                g.fillRect(DISPLAY_AREA_BACK_COLOR, 0, paddingY, w, getHeight() - paddingY);
                g.startClip(-1, y, w + 2, h); //use clip to ensure all corners connect
                g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, yAcross, w, yAcross);
                g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, y, 0, h);
                g.drawLine(1, FScreen.HEADER_LINE_COLOR, w, y, w, h);
                g.endClip();

                foreColor = TAB_SEL_FORE_COLOR;
            }
            else { //draw right separator
                x = getWidth();
                y = getHeight() / 4;
                g.drawLine(1, FScreen.HEADER_LINE_COLOR, x, y, x, getHeight() - y);

                foreColor = TAB_FORE_COLOR;
            }

            x = paddingX;
            y = paddingY;
            w = getWidth() - 2 * paddingX;
            h = getHeight() - 2 * paddingY;
            g.drawText(caption, TAB_FONT, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
        }
    }
}
