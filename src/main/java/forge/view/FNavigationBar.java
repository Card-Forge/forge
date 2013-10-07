package forge.view;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.Timer;

import forge.Singletons;
import forge.gui.menus.ForgeMenu;
import forge.gui.menus.LayoutMenu;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FDigitalClock;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

@SuppressWarnings("serial")
public class FNavigationBar extends FTitleBarBase {

    private static final ForgeMenu forgeMenu = Singletons.getControl().getForgeMenu();
    private static final ForgePreferences prefs = Singletons.getModel().getPreferences();
    private static final int revealSpeed = 300;
    private static final int revealDelay = 150;

    private final FButton btnForge = new FButton("Forge");
    private final FDigitalClock clock = new FDigitalClock();
    private final JPanel pnlReveal = new JPanel();
    private int revealDir = 0;
    private long timeMenuHidden = 0;
    private Timer incrementRevealTimer, checkForRevealChangeTimer;
    private boolean hidden;

    public FNavigationBar(FFrame f) {
        super(f);
        btnForge.setFocusable(false);
        btnForge.setPreferredSize(new Dimension(100, 23));
        setIconImage(this.frame.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(btnForge).setForeground(foreColor);
        FSkin.get(clock).setForeground(foreColor);
        addControls();        
        setupPnlReveal();
    }
    
    @Override
    protected void addControls() {
        add(btnForge);
        layout.putConstraint(SpringLayout.WEST, btnForge, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, btnForge, -1, SpringLayout.SOUTH, this);
        addForgeButtonListeners();

        super.addControls();

        add(clock);
        layout.putConstraint(SpringLayout.EAST, clock, -6, SpringLayout.WEST, btnLockTitleBar);
        layout.putConstraint(SpringLayout.SOUTH, clock, -5, SpringLayout.SOUTH, this);
        updateClockVisibility();
    }
    
    @Override
    public void updateButtons() {
        super.updateButtons();
        updateClockVisibility();
        LayoutMenu.updateFullScreenItemText();
    }
    
    //only show clock if maximized and status bar hidden
    public void updateClockVisibility() {
        clock.setVisible(this.frame.isFullScreen() && prefs.getPrefBoolean(FPref.UI_HIDE_STATUS_BAR));
    }
    
    private void addForgeButtonListeners() {
        btnForge.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (System.currentTimeMillis() - timeMenuHidden > 250) { //time comparsion needed clicking button a second time to hide menu
                    showForgeMenu(true);
                }
            }
        });
    }
    
    public void showForgeMenu(boolean hideIfAlreadyShown) {
        if (!btnForge.isToggled()) {
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
    public String getTitle() {
        return "Forge";
    }

    @Override
    public void setTitle(String title) {
    }
    
    @Override
    public void setIconImage(Image image) {
    }
}
