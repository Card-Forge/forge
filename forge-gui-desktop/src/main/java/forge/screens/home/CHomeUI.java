package forge.screens.home;

import java.util.List;

import javax.swing.JMenu;

import forge.Singletons;
import forge.UiCommand;
import forge.gui.FNetOverlay;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.net.FServer;
import forge.net.FServerManager;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.sanctioned.VSubmenuConstructed;
import forge.toolbox.FAbsolutePositioner;

/**
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum CHomeUI implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    Object previousDoc = null;

    private LblMenuItem lblSelected = new LblMenuItem(VSubmenuConstructed.SINGLETON_INSTANCE);

    /** Programatically selects a menu item.
     *  @param id0 {@link forge.gui.framework.EDocID} */
    public void itemClick(EDocID id0) {
        final ForgePreferences prefs = FModel.getPreferences();

        if (lblSelected != null) {
            lblSelected.setSelected(false);
            lblSelected.repaintSelf();
        }

        if (previousDoc != null) {
            if (!previousDoc.equals(id0.getDoc().getLayoutControl())) {
                MenuUtil.setMenuProvider(null);
            }
        }

        FAbsolutePositioner.SINGLETON_INSTANCE.hideAll();
        id0.getDoc().populate();
        id0.getDoc().getLayoutControl().update();
        lblSelected = VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(id0);
        lblSelected.setSelected(true);

        prefs.setPref(FPref.SUBMENU_CURRENTMENU, id0.toString());
        prefs.save();

        previousDoc = id0.getDoc().getLayoutControl();
    }

    /** @param lbl0 {@link forge.screens.home.LblMenuItem} */
    public void setLblSelected(final LblMenuItem lbl0) {
        this.lblSelected = lbl0;
    }

    /** @return {@link javax.swing.JLabel} */
    public LblMenuItem getLblSelected() {
        return lblSelected;
    }

    @Override
    public void register() {
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
                //final NetServer srv = FServer.getServer();
                //srv.listen(ForgeConstants.SERVER_PORT_NUMBER);

                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(true);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(false);

                FNetOverlay.SINGLETON_INSTANCE.showUp("");//"Server listening on port " + srv.getPortNumber());
            }
        });

        VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setCommand(new Runnable() {
            @Override
            public void run() {
                FServer.getServer().stop();
                FServerManager.getInstance().stopServer();
                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(false);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(true);

                FNetOverlay.SINGLETON_INSTANCE.getWindow().setVisible(false);
            }
        });
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
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /**
     * Pulls previous menu selection from preferences
     * and clicks it programatically.
     */
    private void selectPrevious() {
        EDocID selected = null;
        try {
            selected = EDocID.valueOf(FModel.getPreferences().getPref(FPref.SUBMENU_CURRENTMENU));
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
}
