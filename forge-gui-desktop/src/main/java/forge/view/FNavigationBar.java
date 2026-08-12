package forge.view;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.google.common.collect.Lists;

import forge.Singletons;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gui.framework.FScreen;
import forge.gui.framework.ILocalRepaint;
import forge.localinstance.properties.ForgePreferences;
import forge.menus.ForgeMenu;
import forge.menus.LayoutMenu;
import forge.toolbox.FButton;
import forge.toolbox.FDigitalClock;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;
import forge.util.ReflectionUtil;


@SuppressWarnings("serial")
public class FNavigationBar extends FTitleBarBase {

    private static final ForgeMenu forgeMenu = Singletons.getControl().getForgeMenu();
    private static final int revealSpeed = 300;
    private static final int revealDelay = 150;
    private static final int initialHideDelay = 500;

    private final FButton btnForge = new FButton("Forge");
    private final List<NavigationTab> tabs = Lists.newArrayList();
    private final FDigitalClock clock = new FDigitalClock();
    private final JPanel pnlReveal = new JPanel();
    private NavigationTab selectedTab;
    private int revealDir = 0;
    private long timeMenuHidden = 0;
    private Timer incrementRevealTimer, checkForRevealChangeTimer;
    private boolean hidden;

    public FNavigationBar(final FFrame f) {
        super(f);
        this.setBorder(new FSkin.MatteSkinBorder(0, 0, 2, 0, bottomEdgeColor));
        this.setLocation(0, -visibleHeight); //hide by default
        this.setPreferredSize(new Dimension(this.owner.getWidth(), visibleHeight));
        btnForge.setFocusable(false);
        btnForge.setPreferredSize(new Dimension(100, 23));
        btnForge.setForeground(foreColor);
        clock.setForeground(foreColor);
        addControls();
        setupPnlReveal();
        updateBtnCloseTooltip();
    }

    public void updateBtnCloseTooltip() {
        final Localizer localizer = Localizer.getInstance();
        switch (Singletons.getControl().getCloseAction()) {
        case NONE:
            btnClose.setToolTipText(localizer.getMessage("lblClose"));
            break;
        case CLOSE_SCREEN:
            btnClose.setToolTipText(this.selectedTab.screen.getCloseButtonTooltip());
            break;
        case EXIT_FORGE:
            btnClose.setToolTipText(localizer.getMessage("lblExitForge"));
            break;
        }
    }

    @Override
    protected void addControls() {
        add(btnForge);
        layout.putConstraint(SpringLayout.WEST, btnForge, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, btnForge, -1, SpringLayout.SOUTH, this);
        addForgeButtonListeners();

        addNavigationTab(FScreen.HOME_SCREEN);
        addNavigationTab(FScreen.DECK_EDITOR_CONSTRUCTED);
        if (ForgePreferences.DEV_MODE) {
            //TODO: Make Workshop available outside developer mode when custom cards supported
            addNavigationTab(FScreen.WORKSHOP_SCREEN);
        }

        super.addControls();

        add(clock);
        layout.putConstraint(SpringLayout.EAST, clock, -6, SpringLayout.WEST, btnLockTitleBar);
        layout.putConstraint(SpringLayout.SOUTH, clock, -5, SpringLayout.SOUTH, this);
        updateClockVisibility();
    }

    private NavigationTab addNavigationTab(final FScreen screen) {
        final NavigationTab tab = new NavigationTab(screen);
        if (tabs.size() == 0) {
            tab.setSelected(true);
            selectedTab = tab;
            layout.putConstraint(SpringLayout.WEST, tab, 1, SpringLayout.EAST, btnForge);
        } else {
            layout.putConstraint(SpringLayout.WEST, tab, 1, SpringLayout.EAST, tabs.get(tabs.size() - 1));
        }
        layout.putConstraint(SpringLayout.SOUTH, tab, 0, SpringLayout.SOUTH, this);
        tabs.add(tab);
        add(tab);
        return tab;
    }

    private NavigationTab getTab(final FScreen screen) {
        for (final NavigationTab tab : tabs) {
            if (tab.screen == screen) {
                return tab;
            }
        }
        return null;
    }

    public boolean canSwitch(final FScreen toScreen) {
        return (selectedTab == null || selectedTab.screen.onSwitching(toScreen));
    }

    public void updateSelectedTab() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        NavigationTab tab = getTab(screen);
        if (tab == null) {
            tab = addNavigationTab(screen); //if tab not found, add and select it
        } else if (tab == selectedTab) {
            return;
        }

        if (selectedTab != null) {
            selectedTab.setSelected(false);
        }
        tab.setSelected(true);
        selectedTab = tab;
        updateBtnCloseTooltip();
    }

    public void closeSelectedTab() {
        closeTab(selectedTab);
    }

    public void closeTab(final FScreen screen) {
        final NavigationTab tab = getTab(screen);
        if (tab != null) {
            closeTab(tab);
        }
    }

    private void closeTab(final NavigationTab tab) {
        if (tab == null) { return; }
        if (!tab.screen.onClosing()) { return; } //give screen a chance to perform special close handling and/or cancel closing tab

        if (tab.selected) {
            //return to Home screen if selected tab closed
            //TODO: support navigation history and go to previous tab instead
            this.selectedTab = null; //prevent raising onSwitching for tab being closed
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN, true);
        }
        final int index = tabs.indexOf(tab);
        if (index != -1) {
            tabs.remove(index);
            remove(tab);
            if (index < tabs.size()) { //ensure tab to right of closed tab moved over if applicable
                layout.putConstraint(SpringLayout.WEST, tabs.get(index), 1, SpringLayout.EAST, index > 0 ? tabs.get(index - 1) : btnForge);
                revalidate(); //needed or tab doesn't appear to move over
            }
            repaint(); //needed or tab visual sticks around
        }
    }

    public boolean hasNetGame() {
        return tabs.stream().filter(t -> t.getScreen().getController() instanceof AbstractGuiGame game
                && game.isNetGame()).findAny().map(b -> true).orElse(false);
    }

    @Override
    public void updateButtons() {
        super.updateButtons();
        updateClockVisibility();
        LayoutMenu.updateFullScreenItemText();
    }

    //only show clock if Full Screen
    private void updateClockVisibility() {
        clock.setVisible(this.owner.isFullScreen());
    }

    private void addForgeButtonListeners() {
        btnForge.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (btnForge.isEnabled() && System.currentTimeMillis() - timeMenuHidden > 250) { //time comparsion needed clicking button a second time to hide menu
                    showForgeMenu(true);
                }
            }
        });
    }

    public void showForgeMenu(final boolean hideIfAlreadyShown) {
        if (!btnForge.isToggled() && forgeMenu.getPopupMenu().isEnabled()) {
            btnForge.setToggled(true);
            forgeMenu.getPopupMenu().show(this, 1, this.getHeight());
        }
        else if (hideIfAlreadyShown) {
            forgeMenu.hide();
        }
    }

    public void onForgeMenuHidden() {
        btnForge.setToggled(false);
        timeMenuHidden = System.currentTimeMillis();
    }

    //setup panel used to reveal navigation bar when hidden
    private void setupPnlReveal() {
        pnlReveal.setLocation(0, 0);
        pnlReveal.setVisible(hidden);
        pnlReveal.setOpaque(false);
        pnlReveal.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                if (revealDir == 0) {
                    startReveal();
                }
            }
        });
        incrementRevealTimer = new Timer(revealSpeed / visibleHeight, e -> incrementReveal());
        checkForRevealChangeTimer = new Timer(revealDelay, e -> checkForRevealChange());
    }

    private void startReveal() {
        if (this.getLocation().y == 0) { return; }
        if (revealDir == 0) {
            incrementRevealTimer.setInitialDelay(revealDelay);
            incrementRevealTimer.start();
            checkForRevealChangeTimer.stop();
        }
        revealDir = 1;
    }

    private void stopReveal() {
        if (this.getLocation().y == -visibleHeight) { return; }
        if (revealDir == 0) {
            incrementRevealTimer.setInitialDelay(revealDelay);
            incrementRevealTimer.start();
            checkForRevealChangeTimer.stop();
        }
        revealDir = -1;
    }

    private void incrementReveal() {
        int newY = this.getLocation().y + revealDir * 2;
        switch (revealDir) {
        case 0:
            incrementRevealTimer.stop();
            return;
        case 1:
            if (newY >= 0) {
                newY = 0;
                revealDir = 0;
                incrementRevealTimer.stop();
                checkForRevealChangeTimer.setInitialDelay(0);
                checkForRevealChangeTimer.start(); //once open fully, start another timer to check when mouse moves away
            }
            break;
        case -1:
            if (newY <= -visibleHeight) {
                newY = -visibleHeight;
                revealDir = 0;
                incrementRevealTimer.stop();
            }
            break;
        }
        this.setLocation(0, newY);
        checkForRevealChange();
    }

    private void checkForRevealChange() {
        if (hidden && this.getHeight() > 0 && !btnForge.isToggled()) { //don't change reveal while Forge menu open
            final Rectangle screenBounds = new Rectangle(this.getLocationOnScreen(), this.getSize());
            if (screenBounds.contains(MouseInfo.getPointerInfo().getLocation())) {
                startReveal();
            }
            else {
                stopReveal();
            }
        }
    }

    public void setMenuShortcutsEnabled(final boolean enabled0) {
        forgeMenu.getPopupMenu().setEnabled(enabled0);
    }

    @Override
    public void setEnabled(final boolean enabled0) {
        btnForge.setEnabled(enabled0);
        setMenuShortcutsEnabled(enabled0);
        for (final NavigationTab tab : tabs) {
            tab.setEnabled(enabled0);
        }
        btnClose.setEnabled(enabled0); //don't allow closing screens using Close button while disabled
    }

    @Override
    public void setVisible(final boolean visible) {
        hidden = !visible;
        if (pnlReveal != null) { //check needed because FTitleBarBase constructor calls this
            revealDir = 0;
            incrementRevealTimer.stop();
            checkForRevealChangeTimer.stop();
            pnlReveal.setVisible(hidden);
        }
        if (visible || this.getLocation().y < 0) {
            setLocation(0, visible ? 0 : -visibleHeight);
        }
        else if (pnlReveal != null) { //if previously fully visible, delay hiding titlebar until mouse moves away
            checkForRevealChangeTimer.setInitialDelay(initialHideDelay); //delay hiding a bit even if mouse already outside titlebar
            checkForRevealChangeTimer.start();
        }
    }

    @Override
    public void setSize(final int width, final int height) {
        super.setSize(width, height);
        pnlReveal.setSize(width, 1);
    }

    public JPanel getPnlReveal() {
        return pnlReveal;
    }

    @Override
    public void setTitle(final String title) {
    }

    @Override
    public void setIconImage(final Image image) {
    }

    public void updateTitle(final FScreen screen) {
        for (final NavigationTab tab : tabs) {
            if (tab.screen == screen) {
                tab.updateTitle();
            }
        }
    }

    private final class NavigationTab extends SkinnedLabel implements ILocalRepaint {
        private static final int fontSize = 14;
        private static final int unhoveredAlpha = 150;

        private final FScreen screen;
        private final CloseButton btnClose;
        private SkinColor backColor;
        private boolean selected = false;
        private boolean hovered = false;

        private NavigationTab(final FScreen screen0) {
            super(screen0.getTabCaption());
            this.screen = screen0;
            setOpaque(false);
            this.setIcon(screen0.getTabIcon());
            this.setForeground(foreColor.alphaColor(unhoveredAlpha));
            this.setFont(FSkin.getRelativeFont(fontSize));

            int closeButtonOffset;
            if (screen.allowTabClose()) {
                btnClose = new CloseButton();
                btnClose.setToolTipText(screen.getCloseButtonTooltip());
                closeButtonOffset = btnClose.getPreferredSize().width;
                final SpringLayout tabLayout = new SpringLayout();
                setLayout(tabLayout);
                add(btnClose);
                tabLayout.putConstraint(SpringLayout.WEST, btnClose, 4, SpringLayout.EAST, this);
                tabLayout.putConstraint(SpringLayout.SOUTH, btnClose, -2, SpringLayout.SOUTH, this);
            }
            else {
                btnClose = null;
                closeButtonOffset = 0;
            }
            setBorder(new EmptyBorder(2, 3, 2, 7 + closeButtonOffset));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (!NavigationTab.this.isEnabled()) { return; }
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (!selected) {
                            Singletons.getControl().setCurrentScreen(screen);
                        }
                    }
                    else if (SwingUtilities.isMiddleMouseButton(e) && screen.allowTabClose()) {
                        FNavigationBar.this.closeTab(NavigationTab.this);
                    }
                }
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!NavigationTab.this.isEnabled()) { return; }
                    hovered = true;
                    repaintSelf();
                }
                @Override
                public void mouseExited(final MouseEvent e) {
                    if (hovered && (btnClose == null || !btnClose.getBounds().contains(e.getPoint()))) { //ensure mouse didn't simply move onto close button
                        hovered = false;
                        repaintSelf();
                    }
                }
            });
        }

        /** @param isSelected0 &emsp; boolean */
        private void setSelected(final boolean selected0) {
            if (this.selected == selected0) { return; }
            this.selected = selected0;
            this.setFont(selected0 ? FSkin.getRelativeBoldFont(fontSize) : FSkin.getRelativeFont(fontSize));
            repaintSelf();
        }

        @Override
        public void setIcon(final Icon icon) {
            final ImageIcon imageIcon = ReflectionUtil.safeCast(icon, ImageIcon.class);
            if (imageIcon != null) {
                super.setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            }
            else {
                super.setIcon((Icon)null);
            }
        }

        @Override
        public void setEnabled(final boolean enabled0) {
            if (!enabled0 && hovered) {
                hovered = false; //ensure hovered reset if disabled
            }
            if (btnClose != null) {
                btnClose.setEnabled(enabled0);
            }
            super.setEnabled(enabled0);
        }

        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            this.setForeground(this.selected ? bottomEdgeColor.getHighContrastColor() : (this.hovered ? foreColor : foreColor.alphaColor(unhoveredAlpha)));
            repaint(0, 0, d.width, d.height);
            if (btnClose != null) {
                btnClose.repaintSelf();
            }
        }

        @Override
        public void paintComponent(final Graphics g) {
            final Graphics2D g2d = (Graphics2D)g;
            final int width = getWidth() - 1;
            final int height = visibleHeight - 1;
            final int radius = 6;
            backColor = this.selected ? bottomEdgeColor : (this.hovered ? buttonHoverColor : buttonHoverColor.alphaColor(unhoveredAlpha));
            FSkin.setGraphicsGradientPaint(g2d, 0, 0, backColor.stepColor(30), 0, height, backColor);
            g.fillRoundRect(0, 0, width, height, radius, radius);
            FSkin.setGraphicsColor(g, buttonBorderColor);
            g.drawRoundRect(0, 0, width, height, radius, radius);
            super.paintComponent(g);
        }

        public FScreen getScreen() {
            return screen;
        }

        private void updateTitle() {
            setText(screen.getTabCaption());
        }

        private class CloseButton extends JLabel implements ILocalRepaint {
            private boolean pressed, hovered;

            private CloseButton() {
                setPreferredSize(new Dimension(17, 17));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(final MouseEvent e) {
                        if (!NavigationTab.CloseButton.this.isEnabled()) { return; }
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            pressed = true;
                            repaintSelf();
                        }
                    }
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        if (pressed && SwingUtilities.isLeftMouseButton(e)) {
                            pressed = false;
                            if (hovered) { //only handle click if mouse released over button
                                repaintSelf();
                                FNavigationBar.this.closeTab(NavigationTab.this);
                            }
                        }
                    }
                    @Override
                    public void mouseEntered(final MouseEvent e) {
                        if (!NavigationTab.CloseButton.this.isEnabled()) { return; }
                        hovered = true;
                        repaintSelf();
                    }
                    @Override
                    public void mouseExited(final MouseEvent e) {
                        if (hovered) {
                            hovered = false;
                            repaintSelf();
                        }
                    }
                });
            }

            @Override
            public void setEnabled(final boolean enabled0) {
                if (!enabled0 && hovered) {
                    hovered = false; //ensure hovered reset if disabled
                }
                super.setEnabled(enabled0);
            }

            @Override
            public void repaintSelf() {
                final Dimension d = this.getSize();
                repaint(0, 0, d.width, d.height);
            }

            @Override
            public void paintComponent(final Graphics g) {
                super.paintComponent(g);

                if (hovered) {
                    if (pressed) {
                        FSkin.setGraphicsColor(g, backColor.stepColor(-40));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.translate(1, 1); //translate icon to give pressed button look
                    }
                    else {
                        FSkin.setGraphicsColor(g,  backColor.getContrastColor(20));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                }

                final int thickness = 2;
                final int offset = 4;
                final int x1 = offset;
                final int y1 = offset;
                final int x2 = getWidth() - offset - 1;
                final int y2 = getHeight() - offset - 1;

                final Graphics2D g2d = (Graphics2D) g;
                SkinColor iconColor = NavigationTab.this.getSkin().getForeground();
                if (!NavigationTab.this.isEnabled()) {
                    iconColor = iconColor.alphaColor(100);
                }
                FSkin.setGraphicsColor(g2d, iconColor);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(thickness));
                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);
            }
        }
    }
}
