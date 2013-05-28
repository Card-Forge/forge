package forge.gui.home;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.gui.FNetOverlay;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.net.NetServer;
import forge.properties.ForgePreferences;
import forge.properties.NewConstants;
import forge.properties.ForgePreferences.FPref;

/** 
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum CHomeUI implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private LblMenuItem lblSelected = new LblMenuItem(VSubmenuConstructed.SINGLETON_INSTANCE);

    /** Programatically selects a menu item.
     *  @param id0 {@link forge.gui.framework.EDocID} */
    public void itemClick(EDocID id0) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        if (lblSelected != null) {
            lblSelected.setSelected(false);
            lblSelected.repaintSelf();
        }

        id0.getDoc().populate();
        id0.getDoc().getLayoutControl().update();
        lblSelected = VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(id0);
        lblSelected.setSelected(true);

        prefs.setPref(FPref.SUBMENU_CURRENTMENU, id0.toString());
        Singletons.getModel().getPreferences().save();
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
    
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        selectPrevious();
        VHomeUI.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            @Override
            public void run() {
                Singletons.getControl().changeState(FControl.Screens.DECK_EDITOR_CONSTRUCTED);
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorConstructed());
            }
        });

        VHomeUI.SINGLETON_INSTANCE.getLblExit().setCommand(new Command() {
            @Override
            public void run() {
                System.exit(0);
            }
        });
        
        VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setCommand(new Runnable() {
            @Override
            public void run() {
                NetServer srv = Singletons.getControl().getServer(); 
                srv.listen(NewConstants.SERVER_PORT_NUMBER);

                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(true);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(false);

                FNetOverlay.SINGLETON_INSTANCE.showUp("Server listening on port " + srv.getPortNumber());
            }
        });
        
        VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setCommand(new Runnable() {
            @Override
            public void run() {
                Singletons.getControl().getServer().stop();
                VHomeUI.SINGLETON_INSTANCE.getLblStopServer().setEnabled(false);
                VHomeUI.SINGLETON_INSTANCE.getLblStartServer().setEnabled(true);

                FNetOverlay.SINGLETON_INSTANCE.getPanel().setVisible(false);
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
}
