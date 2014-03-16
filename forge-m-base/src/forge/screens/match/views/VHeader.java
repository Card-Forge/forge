package forge.screens.match.views;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.FControl;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.utils.ForgePreferences.FPref;

public class VHeader extends FContainer {
    public static final float HEIGHT = VAvatar.HEIGHT - VPhaseIndicator.HEIGHT;

    private static final FSkinFont TAB_FONT = FSkinFont.get(12);
    private static final FSkinColor TAB_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor DISPLAY_AREA_BACK_COLOR = FSkinColor.get(Colors.CLR_INACTIVE).alphaColor(0.5f);
    private static final float TAB_PADDING_X = 6f;

    private final HeaderTab tabPlayers;
    private final HeaderTab tabLog;
    private final HeaderTab tabCombat;
    private final HeaderTab tabDev;
    private final HeaderTab tabStack;
    private final FLabel btnMenu;
    private HeaderTab selectedTab;

    public VHeader() {
        tabPlayers = add(new HeaderTab("Players", new VPlayers()));
        tabLog = add(new HeaderTab("Log", new VLog()));
        tabCombat = add(new HeaderTab("Combat", new VCombat()));
        tabDev = add(new HeaderTab("Dev", new VDev()));
        tabStack = add(new HeaderTab("Stack", new VStack()));

        btnMenu = add(new FLabel.Builder().icon(FSkinImage.FAVICON).pressedColor(FScreen.HEADER_BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new Runnable() {
            @Override
            public void run() {
                FControl.getView().showMenu();
            }
        }).build());
    }
    
    public HeaderTab getTabPlayers() {
        return tabPlayers;
    }
    
    public HeaderTab getTabLog() {
        return tabLog;
    }
    
    public HeaderTab getTabCombat() {
        return tabCombat;
    }
    
    public HeaderTab getTabDev() {
        return tabDev;
    }
    
    public HeaderTab getTabStack() {
        return tabStack;
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
        float x = 0;
        x = tabPlayers.layout(x);
        x = tabLog.layout(x);
        x = tabCombat.layout(x);
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            x = tabDev.layout(x);
            tabDev.setVisible(true);
        }
        else {
            tabDev.setVisible(false);
        }
        x = tabStack.layout(x);

        btnMenu.setBounds(width - height, 0, height, height);
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
        private final VDisplayArea displayArea;

        private HeaderTab(String text0, VDisplayArea displayArea0) {
            text = text0;
            displayArea = displayArea0;
            setHeight(HEIGHT);
            update();
        }

        private float layout(float x) {
            setLeft(x);
            return x + getWidth();
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
            setWidth(TAB_FONT.getFont().getBounds(caption).width + TAB_PADDING_X);
            VHeader.this.revalidate();
        }

        @Override
        public void draw(Graphics g) {
            float x, y, w, h;
            float paddingX = 2;
            float paddingY = 2;

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
            }

            x = paddingX;
            y = paddingY;
            w = getWidth() - 2 * paddingX;
            h = getHeight() - 2 * paddingY;
            g.drawText(caption, TAB_FONT, TAB_FORE_COLOR, x, y, w, h, false, HAlignment.CENTER, true);
        }
    }
}
