package forge.view;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import forge.Singletons;
import forge.control.FControl.Screens;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.framework.ILocalRepaint;
import forge.gui.home.CHomeUI;
import forge.gui.menus.ForgeMenu;
import forge.gui.menus.LayoutMenu;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FDigitalClock;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.JLabelSkin;
import forge.gui.toolbox.FSkin.SkinColor;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.util.TypeUtil;

@SuppressWarnings("serial")
public class FNavigationBar extends FTitleBarBase {

    private static final ForgeMenu forgeMenu = Singletons.getControl().getForgeMenu();
    private static final int revealSpeed = 300;
    private static final int revealDelay = 150;
    private static final int initialHideDelay = 500;

    private final FButton btnForge = new FButton("Forge");
    private final ArrayList<NavigationTab> tabs = new ArrayList<NavigationTab>();
    private final FDigitalClock clock = new FDigitalClock();
    private final JPanel pnlReveal = new JPanel();
    private NavigationTab selectedTab;
    private int revealDir = 0;
    private long timeMenuHidden = 0;
    private Timer incrementRevealTimer, checkForRevealChangeTimer;
    private boolean hidden;

    public FNavigationBar(FFrame f) {
        super(f);
        btnForge.setFocusable(false);
        btnForge.setPreferredSize(new Dimension(100, 23));
        FSkin.get(btnForge).setForeground(foreColor);
        FSkin.get(clock).setForeground(foreColor);
        addControls();        
        setupPnlReveal();
        btnClose.setToolTipText("Exit Forge");
    }
    
    @Override
    protected void addControls() {
        add(btnForge);
        layout.putConstraint(SpringLayout.WEST, btnForge, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, btnForge, -1, SpringLayout.SOUTH, this);
        addForgeButtonListeners();

        addNavigationTab(CHomeUI.SINGLETON_INSTANCE, false);
        addNavigationTab(CDeckEditorUI.SINGLETON_INSTANCE, false);

        super.addControls();

        add(clock);
        layout.putConstraint(SpringLayout.EAST, clock, -6, SpringLayout.WEST, btnLockTitleBar);
        layout.putConstraint(SpringLayout.SOUTH, clock, -5, SpringLayout.SOUTH, this);
        updateClockVisibility();
    }
    
    public void addNavigationTab(INavigationTabData data, boolean autoSelect) {
        NavigationTab tab = new NavigationTab(data);
        if (tabs.size() == 0) {
            tab.setSelected(true);
            selectedTab = tab;
            layout.putConstraint(SpringLayout.WEST, tab, 1, SpringLayout.EAST, btnForge);
        }
        else {
            layout.putConstraint(SpringLayout.WEST, tab, 1, SpringLayout.EAST, tabs.get(tabs.size() - 1));
        }
        layout.putConstraint(SpringLayout.SOUTH, tab, 0, SpringLayout.SOUTH, this);
        tabs.add(tab);
        add(tab);
        if (autoSelect) {
            setSelectedTab(tab);
        }
    }
    
    public void setSelectedTab(INavigationTabData data) {
        for (NavigationTab tab : tabs) {
            if (tab.data == data) {
                setSelectedTab(tab);
                return;
            }
        }
        addNavigationTab(data, true); //if tab not found, add and select it
    }
    
    private void setSelectedTab(NavigationTab tab) {
        if (tab != null && tab != selectedTab) {
            if (selectedTab != null) {
                selectedTab.setSelected(false);
            }
            tab.setSelected(true);
            selectedTab = tab;
        }
    }
    
    public void closeTab(INavigationTabData data) {
        for (NavigationTab tab : tabs) {
            if (tab.data == data) {
                closeTab(tab);
                return;
            }
        }
    }
    
    private void closeTab(NavigationTab tab) {
        if (tab == null) { return; }
        if (!tab.data.onClosingTab()) { return; } //give data a chance to perform special close handling and/or cancel closing tab

        if (tab.selected) {
            setSelectedTab(tabs.get(0)); //select home tab if selected tab closed (TODO: support navigation history and go to previous tab instead)
        }
        int index = tabs.indexOf(tab);
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
    
    @Override
    public void updateButtons() {
        super.updateButtons();
        updateClockVisibility();
        LayoutMenu.updateFullScreenItemText();
    }
    
    //only show clock if Full Screen
    private void updateClockVisibility() {
        clock.setVisible(this.frame.isFullScreen());
    }

    private void addForgeButtonListeners() {
        btnForge.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (btnForge.isEnabled() && System.currentTimeMillis() - timeMenuHidden > 250) { //time comparsion needed clicking button a second time to hide menu
                    showForgeMenu(true);
                }
            }
        });
    }
    
    public void showForgeMenu(boolean hideIfAlreadyShown) {
        if (!btnForge.isToggled() && btnForge.isEnabled()) {
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
    
    public void setForgeButtonEnabled(boolean enabled0) {
        btnForge.setEnabled(enabled0);
        forgeMenu.getPopupMenu().setEnabled(enabled0);
    }
    
    //setup panel used to reveal navigation bar when hidden
    private void setupPnlReveal() {
        pnlReveal.setVisible(hidden);
        pnlReveal.setOpaque(false);
        pnlReveal.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (revealDir == 0) {
                    startReveal();
                }
            }
        });
        incrementRevealTimer = new Timer(revealSpeed / visibleHeight, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                incrementReveal();
            }
        });
        checkForRevealChangeTimer = new Timer(revealDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkForRevealChange();
            }
        });
    }
    
    private void startReveal() {
        if (this.getHeight() == visibleHeight) { return; }
        if (revealDir == 0) {
            incrementRevealTimer.setInitialDelay(revealDelay);
            incrementRevealTimer.start();
            checkForRevealChangeTimer.stop();
        }
        revealDir = 1;
    }
    
    private void stopReveal() {
        if (this.getHeight() == 0) { return; }
        if (revealDir == 0) {
            incrementRevealTimer.setInitialDelay(revealDelay);
            incrementRevealTimer.start();
            checkForRevealChangeTimer.stop();
        }
        revealDir = -1;
    }
    
    private void incrementReveal() {
        int newHeight = this.getHeight() + revealDir * 2;
        switch (revealDir) {
            case 0:
                incrementRevealTimer.stop();
                return;
            case 1:
                if (newHeight >= visibleHeight) {
                    newHeight = visibleHeight;
                    revealDir = 0;
                    incrementRevealTimer.stop();
                    checkForRevealChangeTimer.setInitialDelay(0);
                    checkForRevealChangeTimer.start(); //once open fully, start another timer to check when mouse moves away
                }
                break;
            case -1:
                if (newHeight <= 0) {
                    newHeight = 0;
                    revealDir = 0;
                    incrementRevealTimer.stop();
                }
                break;
        }
        this.setSize(this.getWidth(), newHeight);
        revalidate();
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

    @Override
    public void setVisible(boolean visible) {
        hidden = !visible;
        if (pnlReveal != null) { //check needed because FTitleBarBase constructor calls this
            revealDir = 0;
            incrementRevealTimer.stop();
            checkForRevealChangeTimer.stop();
            pnlReveal.setVisible(hidden);
        }
        if (visible || this.getHeight() < visibleHeight) {
            super.setVisible(visible);
        }
        else if (pnlReveal != null) { //if previously fully visible, delay hiding titlebar until mouse moves away
            checkForRevealChangeTimer.setInitialDelay(initialHideDelay); //delay hiding a bit even if mouse already outside titlebar
            checkForRevealChangeTimer.start();
        }
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        pnlReveal.setBounds(x, y, width, 1);
    }
    
    public JPanel getPnlReveal() {
        return pnlReveal;
    }

    @Override
    public void setTitle(String title) {
    }
    
    @Override
    public void setIconImage(Image image) {
    }

    public interface INavigationTabData {
        public String getTabCaption();
        public SkinImage getTabIcon();
        public Screens getTabDestScreen();
        public String getTabCloseButtonTooltip();
        public boolean onClosingTab();
    }
    
    private final class NavigationTab extends JLabel implements ILocalRepaint {
        private static final int fontSize = 14;
        private static final int unhoveredAlpha = 150;
        private final FSkin.JLabelSkin<NavigationTab> skin;
        private final INavigationTabData data;
        private final CloseButton btnClose;
        private SkinColor backColor;
        private boolean selected = false;
        private boolean hovered = false;

        private NavigationTab(final INavigationTabData data0) {
            super(data0.getTabCaption());
            this.data = data0;
            setOpaque(false);
            skin = FSkin.get(this);
            skin.setIcon(data0.getTabIcon());
            skin.setForeground(foreColor.alphaColor(unhoveredAlpha));
            skin.setFont(FSkin.getFont(fontSize));
            
            int closeButtonOffset;
            String closeButtonTooltip = data.getTabCloseButtonTooltip();
            if (closeButtonTooltip != null) {
                btnClose = new CloseButton();
                btnClose.setToolTipText(closeButtonTooltip);
                closeButtonOffset = btnClose.getPreferredSize().width;
                SpringLayout tabLayout = new SpringLayout();
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
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (!selected) {
                            FNavigationBar.this.setSelectedTab(NavigationTab.this);
                            Singletons.getControl().changeStateAutoFixLayout(data.getTabDestScreen(), NavigationTab.this.getText());
                        }
                    }
                    else if (SwingUtilities.isMiddleMouseButton(e)) {
                        FNavigationBar.this.closeTab(NavigationTab.this);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaintSelf();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (btnClose == null || !btnClose.getBounds().contains(e.getPoint())) { //ensure mouse didn't simply move onto close button
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
            skin.setFont(selected0 ? FSkin.getBoldFont(fontSize) : FSkin.getFont(fontSize));
            repaintSelf();
        }
        
        @Override
        public void setIcon(Icon icon) {
            ImageIcon imageIcon = TypeUtil.safeCast(icon, ImageIcon.class);
            if (imageIcon != null) {
                super.setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(20, 20, Image.SCALE_AREA_AVERAGING)));
            }
            else {
                super.setIcon(null);
            }
        }

        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            skin.setForeground(this.selected ? bottomEdgeColor.getHighContrastColor() : (this.hovered ? foreColor : foreColor.alphaColor(unhoveredAlpha)));
            repaint(0, 0, d.width, d.height);
            if (btnClose != null) {
                btnClose.repaintSelf();
            }
        }

        @Override
        public void paintComponent(final Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            int width = getWidth() - 1;
            int height = visibleHeight - 1;
            int radius = 6;
            backColor = this.selected ? bottomEdgeColor : (this.hovered ? buttonHoverColor : buttonHoverColor.alphaColor(unhoveredAlpha));
            skin.setGraphicsGradientPaint(g2d, 0, 0, backColor.stepColor(30), 0, height, backColor);
            g.fillRoundRect(0, 0, width, height, radius, radius);
            skin.setGraphicsColor(g, buttonBorderColor);
            g.drawRoundRect(0, 0, width, height, radius, radius);
            super.paintComponent(g);
        }
        
        private class CloseButton extends JLabel implements ILocalRepaint {
            protected JLabelSkin<CloseButton> skin = FSkin.get(this);
            private boolean pressed, hovered;

            private CloseButton() {
                setPreferredSize(new Dimension(17, 17));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            pressed = true;
                            repaintSelf();
                        }
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (pressed) {
                                pressed = false;
                                if (hovered) { //only handle click if mouse released over button
                                    repaintSelf();
                                    FNavigationBar.this.closeTab(NavigationTab.this);
                                }
                            }
                        }
                    }
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaintSelf();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaintSelf();
                    }
                });
            }
            
            @Override
            public void repaintSelf() {
                final Dimension d = this.getSize();
                repaint(0, 0, d.width, d.height);
            }
            
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                if (hovered) {
                    if (pressed) {
                        skin.setGraphicsColor(g, backColor.stepColor(-40));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.translate(1, 1); //translate icon to give pressed button look
                    }
                    else {
                        skin.setGraphicsColor(g,  backColor.getContrastColor(20));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                }

                int thickness = 2;
                int offset = 4;
                int x1 = offset;
                int y1 = offset;
                int x2 = getWidth() - offset - 1;
                int y2 = getHeight() - offset - 1;

                Graphics2D g2d = (Graphics2D) g;
                skin.setGraphicsColor(g2d, NavigationTab.this.skin.getForeground());
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(thickness));
                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);
            }
        }
    }
}
