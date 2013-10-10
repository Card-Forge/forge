package forge.gui.home;

import java.util.List;

import javax.swing.JMenu;

import forge.Command;
import forge.Singletons;
import forge.control.FControl.Screens;
import forge.gui.FNetOverlay;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.gui.menus.IMenuProvider;
import forge.gui.menus.MenuUtil;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.net.FServer;
import forge.net.NetServer;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;
import forge.view.FNavigationBar.INavigationTabData;

/**
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum CHomeUI implements ICDoc, IMenuProvider, INavigationTabData {
    /** */
    SINGLETON_INSTANCE;

    Object previousDoc = null;

    private LblMenuItem lblSelected = new LblMenuItem(VSubmenuConstructed.SINGLETON_INSTANCE);

    /** Programatically selects a menu item.
     *  @param id0 {@link forge.gui.framework.EDocID} */
    public void itemClick(EDocID id0) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        if (lblSelected != null) {
            lblSelected.setSelected(false);
            lblSelected.repaintSelf();
        }

        if (previousDoc != null) {
            if (!previousDoc.equals(id0.getDoc().getLayoutControl())) {
                MenuUtil.setMenuProvider(null);
            }
        }

        id0.getDoc().populate();
        id0.getDoc().getLayoutControl().update();
        lblSelected = VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(id0);
        lblSelected.setSelected(true);

        prefs.setPref(FPref.SUBMENU_CURRENTMENU, id0.toString());
        Singletons.getModel().getPreferences().save();

        previousDoc = id0.getDoc().getLayoutControl();
    }

    /** @param lbl0 {@link forge.gui.home.LblMenuItem} */
    public void setLblSelected(final LblMenuItem lbl0) {
        this.lblSelected = lbl0;
    }

    /** @return {@link javax.swing.JLabel} */
    public LblMenuItem getLblSelected() {
        return lblSelected;
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#intialize()
     */
    @Override
    public void initialize() {

        Singletons.getControl().getForgeMenu().setProvider(this);

        selectPrevious();

        VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setCommand(new Runnable() {
            @Override
            public void run() {
                NetServer srv = FServer.instance.getServer();
                srv.listen(NewConstants.SERVER_PORT_NUMBER);

                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(true);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(false);

                FNetOverlay.SINGLETON_INSTANCE.showUp("Server listening on port " + srv.getPortNumber());
            }
        });

        VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setCommand(new Runnable() {
            @Override
            public void run() {
                FServer.instance.getServer().stop();
                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(false);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(true);

                FNetOverlay.SINGLETON_INSTANCE.getPanel().setVisible(false);
            }
        });

        Singletons.getView().getNavigationBar().setSelectedTab(this);
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#update()
     */
    @Override
    public void update() {
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /**
     * Pulls previous menu selection from preferences
     * and clicks it programatically.
     */
    private void selectPrevious() {
        EDocID selected = null;
        try {
            selected = EDocID.valueOf(Singletons.getModel()
                    .getPreferences().getPref(FPref.SUBMENU_CURRENTMENU));
        } catch (final Exception e) { }

        if (selected != null && VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(selected) != null) {
            itemClick(selected);
        }
        else {
            itemClick(EDocID.HOME_CONSTRUCTED);
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        // No specific menus associated with Home screen.
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabCaption()
     */
    @Override
    public String getTabCaption() {
        return "Home";
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabIcon()
     */
    @Override
    public SkinImage getTabIcon() {
        return FSkin.getIcon(FSkin.InterfaceIcons.ICO_FAVICON);
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabDestScreen()
     */
    @Override
    public Screens getTabDestScreen() {
        return Screens.HOME_SCREEN;
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabCloseButtonTooltip()
     */
    @Override
    public String getTabCloseButtonTooltip() {
        return null; //return null to indicate not to show close button
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#onClosingTab()
     */
    @Override
    public boolean onClosingTab() {
        Singletons.getControl().exitForge();
        return false; //don't allow closing Home tab
    }
}
