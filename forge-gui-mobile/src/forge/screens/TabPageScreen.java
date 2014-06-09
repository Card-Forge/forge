package forge.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class TabPageScreen<T extends TabPageScreen<T>> extends FScreen {
    protected final TabPage<T>[] tabPages;
    private TabPage<T> selectedPage;

    @SuppressWarnings("unchecked")
    public TabPageScreen(TabPage<T>[] tabPages0) {
        super(new TabHeader<T>(tabPages0));

        int index = 0;
        tabPages = tabPages0;
        for (TabPage<T> tabPage : tabPages) {
            tabPage.index = index++;
            tabPage.parentScreen = (T)this;
            add(tabPage);
            tabPage.setVisible(false);
        }
        setSelectedPage(tabPages[0]);
    }

    public TabPage<T> getSelectedPage() {
        return selectedPage;
    }
    public void setSelectedPage(TabPage<T> tabPage0) {
        if (selectedPage == tabPage0) { return; }

        if (selectedPage != null) {
            selectedPage.setVisible(false);
        }
        selectedPage = tabPage0;
        if (selectedPage != null) {
            selectedPage.setVisible(true);
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        height -= startY;
        for (TabPage<T> tabPage : tabPages) {
            tabPage.setBounds(0, startY, width, height);
        }
    }

    private static class TabHeader<T extends TabPageScreen<T>> extends Header {
        public static final float HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 1.4f);
        private static final float BACK_BUTTON_WIDTH = Math.round(HEIGHT / 2);
        private static final FSkinColor SEPARATOR_COLOR = BACK_COLOR.stepColor(-40);

        private final FLabel btnBack;

        public TabHeader(TabPage<T>[] tabPages) {
            btnBack = add(new FLabel.Builder().iconScaleAuto(false).icon(new BackIcon(BACK_BUTTON_WIDTH, BACK_BUTTON_WIDTH)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    Forge.back();
                }
            }).build());
            btnBack.setSize(BACK_BUTTON_WIDTH, HEIGHT);

            for (TabPage<T> tabPage : tabPages) {
                add(tabPage.tab);
            }
        }

        @Override
        public float getPreferredHeight() {
            return HEIGHT;
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), HEIGHT);
        }

        @Override
        public void drawOverlay(Graphics g) {
            //draw right border for back button
            float x = btnBack.getWidth() - LINE_THICKNESS / 2;
            g.drawLine(LINE_THICKNESS, SEPARATOR_COLOR, x, 0, x, getHeight());

            //draw bottom border for header
            float y = HEIGHT - LINE_THICKNESS / 2;
            g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y, getWidth(), y);
        }

        @Override
        protected void doLayout(float width, float height) {
            int tabCount = -1; //start at -1 so back button not counted
            for (FDisplayObject child : getChildren()) {
                if (child.isVisible()) {
                    tabCount++;
                }
            }
            float x = 0;
            float tabWidth = (width - BACK_BUTTON_WIDTH) / tabCount;
            for (FDisplayObject child : getChildren()) {
                if (x == 0) { //skip back button
                    x += BACK_BUTTON_WIDTH;
                }
                else if (child.isVisible()) {
                    child.setBounds(x, 0, tabWidth, height);
                    x += tabWidth;
                }
            }
        }
    }

    public static abstract class TabPage<T extends TabPageScreen<T>> extends FContainer {
        private static final float TAB_PADDING = TabHeader.HEIGHT * 0.1f;
        private static final FSkinColor SEL_TAB_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
        private static final FSkinColor TAB_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private static final FSkinFont TAB_FONT = FSkinFont.get(12);

        protected T parentScreen;
        protected String caption;
        protected FImage icon;
        private int index;
        private final Tab tab;

        protected TabPage(String caption0, FImage icon0) {
            caption = caption0;
            icon = icon0;
            tab = new Tab();
        }

        public void showTab() {
            tab.setVisible(true);
        }
        public void hideTab() {
            tab.setVisible(false);
        }

        protected class Tab extends FDisplayObject {
            @Override
            public boolean tap(float x, float y, int count) {
                parentScreen.setSelectedPage(TabPage.this);
                return true;
            }

            public void setVisible(boolean b0) {
                if (isVisible() == b0) { return; }
                super.setVisible(b0);
                parentScreen.getHeader().revalidate(); //revalidate header to account for tab visiblility change

                if (!b0 && parentScreen.getSelectedPage() == TabPage.this) {
                    //select next page if this page is hidden
                    for (int i = index + 1; i < parentScreen.tabPages.length; i++) {
                        if (parentScreen.tabPages[i].tab.isVisible()) {
                            parentScreen.setSelectedPage(parentScreen.tabPages[i]);
                            return;
                        }
                    }
                    //select previous page if selecting next page is not possible
                    for (int i = index - 1; i >= 0; i--) {
                        if (parentScreen.tabPages[i].tab.isVisible()) {
                            parentScreen.setSelectedPage(parentScreen.tabPages[i]);
                            return;
                        }
                    }
                    parentScreen.setSelectedPage(null);
                }
            }

            @Override
            public void draw(Graphics g) {
                float w = getWidth();
                float h = getHeight();
                if (parentScreen.getSelectedPage() == TabPage.this) {
                    g.fillRect(SEL_TAB_COLOR, Header.LINE_THICKNESS / 2, 0, w - Header.LINE_THICKNESS, h);
                }

                //draw caption
                float y = h - TAB_PADDING - TAB_FONT.getCapHeight();
                g.drawText(caption, TAB_FONT, TAB_FORE_COLOR, TAB_PADDING, y - TAB_PADDING, w - 2 * TAB_PADDING, h - y + TAB_PADDING, false, HAlignment.CENTER, true);

                //draw icon if one
                if (icon != null) {
                    float iconHeight = y - 2 * TAB_PADDING;
                    float iconWidth = iconHeight * icon.getWidth() / icon.getHeight();
                    float maxWidth = w - 2 * TAB_PADDING;
                    if (iconWidth > maxWidth) {
                        iconHeight *= maxWidth / iconWidth;
                        iconWidth = maxWidth;
                    }
                    g.drawImage(icon, (w - iconWidth) / 2, (y - iconHeight) / 2, iconWidth, iconHeight);
                }

                //draw right border if needed
                if (getLeft() < TabPage.this.getWidth() - getWidth() - 1) {
                    float x = getWidth() - Header.LINE_THICKNESS / 2;
                    g.drawLine(Header.LINE_THICKNESS, TabHeader.SEPARATOR_COLOR, x, 0, x, h);
                }
            }
        }
    }
}
