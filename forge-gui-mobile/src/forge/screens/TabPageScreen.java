package forge.screens;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

public class TabPageScreen<T extends TabPageScreen<T>> extends FScreen {
    public static boolean COMPACT_TABS = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_TABS);

    protected final TabHeader<T> tabHeader;
    protected final TabPage<T>[] tabPages;
    private TabPage<T> selectedPage;

    @SuppressWarnings("unchecked")
    public TabPageScreen(TabPage<T>... tabPages0) {
        this(tabPages0, true);
    }
    public TabPageScreen(FEventHandler backButton,TabPage<T>... tabPages0) {
        this(tabPages0, backButton);
    }

    public TabPageScreen(TabPage<T>[] tabPages0, boolean showBackButton) {
        this(new TabHeader<>(tabPages0, showBackButton));
    }
    public TabPageScreen(TabPage<T>[] tabPages0, FEventHandler backButton) {
        this(new TabHeader<>(tabPages0, backButton));
    }
    public TabPageScreen(TabHeader<T> tabHeader0) {
        super(tabHeader0);
        tabHeader = tabHeader0;
        tabPages = tabHeader.tabPages;
        initialize();
    }

    public TabPageScreen(String headerCaption, FPopupMenu menu, TabPage<T>[] tabPages0) {
        super(headerCaption, menu);
        tabHeader = add(new TabHeader<>(tabPages0, false));
        tabHeader.showBottomBorder = false;
        tabPages = tabHeader.tabPages;
        initialize();
    }

    public TabPageScreen(String headerCaption, FPopupMenu menu, TabPage<T>[] tabPages0, boolean alwaysRenderHorizontal) {
        super(headerCaption, menu);
        tabHeader = add(new TabHeader<>(tabPages0, false));
        tabHeader.showBottomBorder = false;
        tabHeader.alwaysRenderHorizontal = alwaysRenderHorizontal;
        tabPages = tabHeader.tabPages;
        initialize();
    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        int index = 0;
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
            if (canActivateTabPage()) {
                scrollSelectedTabIntoView();
                selectedPage.onActivate();
            }
        }
    }

    protected boolean canActivateTabPage() {
        return Forge.getCurrentScreen() == this;
    }

    protected boolean showCompactTabs() {
        return COMPACT_TABS || getHeader() != tabHeader; //always show compact tabs if not in primary header
    }

    @Override
    public void onActivate() {
        if (selectedPage != null) {
            scrollSelectedTabIntoView(); //ensure selected tab in view when screen activated
            selectedPage.onActivate();
        }
    }

    private void scrollSelectedTabIntoView() {
        if (tabHeader.isScrollable) { //scroll tab into view if needed, leaving half of the previous/next tab visible if possible
            tabHeader.scroller.scrollIntoView(selectedPage.tab, selectedPage.tab.getWidth() / 2);
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        if (getHeader() != tabHeader) { //show tabs at bottom if not in main header
            float tabHeight = tabHeader.getPreferredHeight();
            tabHeader.setBounds(0, height - tabHeight, width, tabHeight);
            height -= tabHeight;
        }
        height -= startY;
        for (TabPage<T> tabPage : tabPages) {
            tabPage.setBounds(0, startY, width, height);
        }
    }

    protected static class TabHeader<T extends TabPageScreen<T>> extends Header {
        private static final float HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 1.4f);
        private static final float COMPACT_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);
        private static final float BACK_BUTTON_WIDTH = Math.round(HEIGHT / 2);
        private static final FSkinColor SEPARATOR_COLOR = BACK_COLOR.stepColor(-40);

        private final TabPage<T>[] tabPages;
        private final FLabel btnBack;
        private boolean isScrollable;
        private FDisplayObject finalVisibleTab;
        private boolean showBottomBorder = true;
        private boolean alwaysRenderHorizontal = false;

        private final FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                int tabCount = 0;
                for (FDisplayObject child : getChildren()) {
                    if (child.isVisible()) {
                        tabCount++;
                        finalVisibleTab = child;
                    }
                }

                if (Forge.isLandscapeMode() && !alwaysRenderHorizontal) {
                    //render vertically in Landscape mode
                    float y = 0;
                    for (FDisplayObject child : getChildren()) {
                        if (child.isVisible()) {
                            child.setBounds(0, y, visibleWidth, HEIGHT);
                            y += HEIGHT;
                        }
                    }
                    return new ScrollBounds(visibleWidth, y);
                }

                float x = 0;
                float tabWidth;
                isScrollable = (tabCount > 3); //support up to 3 tabs without scrolling
                if (isScrollable) {
                    tabWidth = visibleWidth / 2.5f; //support half of the third tab to make scrolling more obvious
                }
                else {
                    tabWidth = visibleWidth / tabCount;
                }
                for (FDisplayObject child : getChildren()) {
                    if (child.isVisible()) {
                        child.setBounds(x, 0, tabWidth, visibleHeight);
                        x += tabWidth;
                    }
                }
                return new ScrollBounds(isScrollable ? x : visibleWidth, visibleHeight);
            }
        });

        public TabHeader(TabPage<T>[] tabPages0, boolean showBackButton) {
            tabPages = tabPages0;
            if (showBackButton) {
                btnBack = add(new FLabel.Builder().icon(new BackIcon(BACK_BUTTON_WIDTH, BACK_BUTTON_WIDTH)).pressedColor(BTN_PRESSED_COLOR).align(Align.center).command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        Forge.back();
                    }
                }).build());
            }
            else {
                btnBack = null;
            }

            for (TabPage<T> tabPage : tabPages) {
                scroller.add(tabPage.tab);
            }
        }
        public TabHeader(TabPage<T>[] tabPages0, FEventHandler backButton) {
            tabPages = tabPages0;
            if(backButton==null) {
                btnBack = add(new FLabel.Builder().icon(new BackIcon(BACK_BUTTON_WIDTH, BACK_BUTTON_WIDTH)).pressedColor(BTN_PRESSED_COLOR).align(Align.center).command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        Forge.back();
                    }
                }).build());
            }
            else
            {
                btnBack = add(new FLabel.Builder().icon(new BackIcon(BACK_BUTTON_WIDTH, BACK_BUTTON_WIDTH)).pressedColor(BTN_PRESSED_COLOR).align(Align.center).command(backButton).build());
            }

            for (TabPage<T> tabPage : tabPages) {
                scroller.add(tabPage.tab);
            }
        }
        protected boolean showBackButtonInLandscapeMode() {
            return btnBack != null;
        }

        @Override
        public float getPreferredHeight() {
            return tabPages[0].parentScreen.showCompactTabs() ? COMPACT_HEIGHT : HEIGHT;
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        public void drawOverlay(Graphics g) {
            if (Forge.isLandscapeMode()) {
                //in landscape mode, draw left border for header
                g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, 0, 0, getHeight());
                if (showBackButtonInLandscapeMode()) { //draw top border for back button
                    float y = btnBack.getTop() - LINE_THICKNESS / 2;
                    g.drawLine(LINE_THICKNESS, SEPARATOR_COLOR, 0, y, getWidth(), y);
                }
                return;
            }

            //draw right border for back button
            if (btnBack != null) {
                float x = btnBack.getWidth() - LINE_THICKNESS / 2;
                g.drawLine(LINE_THICKNESS, SEPARATOR_COLOR, x, 0, x, getHeight());
            }

            //draw bottom border for header
            if (showBottomBorder) {
                float y = getHeight() - LINE_THICKNESS / 2;
                g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y, getWidth(), y);
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = 0;
            if (btnBack != null) {
                if (Forge.isLandscapeMode()) { //show back button at bottom for landscape mode
                    if (showBackButtonInLandscapeMode()) {
                        float backButtonHeight = HEIGHT * 0.7f;
                        btnBack.setBounds(0, height - backButtonHeight, width, backButtonHeight);
                        height -= backButtonHeight;
                    }
                }
                else {
                    btnBack.setIconScaleAuto(tabPages[0].parentScreen.showCompactTabs());
                    btnBack.setSize(BACK_BUTTON_WIDTH, height);
                    x += BACK_BUTTON_WIDTH;
                }
            }
            scroller.setBounds(x, 0, width - x, height);
        }

        @Override
        public float doLandscapeLayout(float screenWidth, float screenHeight) {
            float width = HEIGHT * 1.25f;
            setBounds(screenWidth - width, 0, width, screenHeight);
            return width;
        }
    }

    public static abstract class TabPage<T extends TabPageScreen<T>> extends FContainer {
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

        public String getCaption() {
            return caption;
        }

        public FImage getIcon() {
            return icon;
        }

        protected void onActivate() {
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            //switch to next/previous tab page when flung left or right
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX < 0) {
                    if (index < parentScreen.tabPages.length - 1) {
                        parentScreen.setSelectedPage(parentScreen.tabPages[index + 1]);
                    }
                }
                else if (index > 0) {
                    parentScreen.setSelectedPage(parentScreen.tabPages[index - 1]);
                }
                return true;
            }
            return false;
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
                boolean isLandscapeMode = Forge.isLandscapeMode();

                float w = getWidth();
                float h = getHeight();
                float padding = h * 0.1f;
                if (parentScreen.getSelectedPage() == TabPage.this) {
                    g.fillRect(SEL_TAB_COLOR, Header.LINE_THICKNESS / 2, 0, w - Header.LINE_THICKNESS, h);
                }
                w -= 2 * padding;

                //draw caption and icon
                if (parentScreen.showCompactTabs() && !isLandscapeMode) {
                    h -= 2 * padding;
                    if (icon == null) {
                        g.drawText(caption, TAB_FONT, TAB_FORE_COLOR, padding, padding, w, h, false, Align.center, true);
                    }
                    else {
                        //center combination of icon and text
                        float iconWidth = h * icon.getWidth() / icon.getHeight();
                        float iconOffset = iconWidth + padding;

                        float x = padding;
                        float y = padding;
                        float dx;
                        FSkinFont font = TAB_FONT;
                        while (true) {
                            dx = (w - iconOffset - font.getMultiLineBounds(caption).width) / 2;
                            if (dx > 0) {
                                x += dx;
                                break;
                            }
                            if (!font.canShrink()) {
                                break;
                            }
                            font = font.shrink();
                        }

                        g.drawImage(icon, x, y, iconWidth, h);

                        x += iconOffset;
                        w -= iconOffset;
                        g.startClip(x, y, w, h);
                        g.drawText(caption, font, TAB_FORE_COLOR, x, y, w, h, false, Align.left, true);
                        g.endClip();
                    }
                } else {
                    float y = h - padding - TAB_FONT.getCapHeight();
                    g.drawText(caption, TAB_FONT, TAB_FORE_COLOR, padding, y - padding, w, h - y + padding, false, Align.center, true);

                    if (icon != null) {
                        float iconHeight = y - 2 * padding;
                        float iconWidth = iconHeight * icon.getWidth() / icon.getHeight();
                        if (iconWidth > w) {
                            iconHeight *= w / iconWidth;
                            iconWidth = w;
                        }
                        float mod = isHovered() ? iconWidth/8f : 0;
                        g.drawImage(icon, (padding + (w - iconWidth) / 2)-mod/2, ((y - iconHeight) / 2)-mod/2, iconWidth+mod, iconHeight+mod);
                    }
                }

                //draw right/bottom border if needed
                if (parentScreen.tabHeader.finalVisibleTab != this || isLandscapeMode) {
                    if (isLandscapeMode) {
                        float y = getHeight() - Header.LINE_THICKNESS / 2;
                        g.drawLine(Header.LINE_THICKNESS, TabHeader.SEPARATOR_COLOR, 0, y, getWidth(), y);
                    }
                    else {
                        float x = getWidth() - Header.LINE_THICKNESS / 2;
                        g.drawLine(Header.LINE_THICKNESS, TabHeader.SEPARATOR_COLOR, x, 0, x, getHeight());
                    }
                }
            }
        }
    }
}
