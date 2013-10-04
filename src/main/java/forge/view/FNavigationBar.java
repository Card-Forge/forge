package forge.view;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SpringLayout;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import forge.Singletons;
import forge.gui.menus.ForgeMenu;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FDigitalClock;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

@SuppressWarnings("serial")
public class FNavigationBar extends FTitleBarBase {

    private static final ForgeMenu forgeMenu = Singletons.getControl().getForgeMenu();
    private static final ForgePreferences prefs = Singletons.getModel().getPreferences();

    private final FButton btnForge = new FButton("Forge");
    private final FDigitalClock clock = new FDigitalClock();
    private long timeMenuHidden = 0;

    public FNavigationBar(FFrame f) {
        super(f);
        btnForge.setFocusable(false);
        btnForge.setPreferredSize(new Dimension(100, 23));
        setIconImage(this.frame.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(btnForge).setForeground(foreColor);
        FSkin.get(clock).setForeground(foreColor);
        addControls();
    }
    
    @Override
    protected void addControls() {
        add(btnForge);
        layout.putConstraint(SpringLayout.WEST, btnForge, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, btnForge, 1, SpringLayout.NORTH, this);
        addForgeButtonListeners();

        super.addControls();

        add(clock);
        layout.putConstraint(SpringLayout.EAST, clock, -6, SpringLayout.WEST, btnMinimize);
        layout.putConstraint(SpringLayout.NORTH, clock, 4, SpringLayout.NORTH, this);
        updateClockVisibility();
    }
    
    @Override
    public void handleMaximizedChanged() {
        super.handleMaximizedChanged();
        updateClockVisibility();
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
        forgeMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                btnForge.setToggled(false);
                timeMenuHidden = System.currentTimeMillis();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {}
        });
    }
    
    //only show clock if maximized and status bar hidden
    public void updateClockVisibility() {
        this.clock.setVisible(this.frame.getMaximized() && prefs.getPrefBoolean(FPref.UI_HIDE_STATUS_BAR));
    }
    
    public void showForgeMenu(boolean hideIfAlreadyShown) {
        if (!btnForge.isToggled()) {
            btnForge.setToggled(true);
            forgeMenu.show(this, 1, this.getHeight());
        }
        else if (hideIfAlreadyShown) {
            forgeMenu.setVisible(false);
        }
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
