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
import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class FNavigationBar extends FTitleBarBase {
    
    private final FButton btnForge = new FButton("Forge");
    private final ForgeMenu forgeMenu = Singletons.getControl().getForgeMenu();
    private long timeMenuHidden = 0;

    public FNavigationBar(FFrame f) {
        super(f);
        btnForge.setFocusable(false);
        btnForge.setPreferredSize(new Dimension(100, 23));
        setIconImage(this.frame.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(btnForge).setForeground(foreColor);
        addControls();
    }
    
    @Override
    protected void addControls() {
        add(btnForge);
        layout.putConstraint(SpringLayout.WEST, btnForge, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, btnForge, 1, SpringLayout.NORTH, this);
        addForgeButtonListeners();
        super.addControls();
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
