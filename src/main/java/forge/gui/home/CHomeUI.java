package forge.gui.home;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.properties.ForgePreferences;
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

        if (!id0.equals(EDocID.HOME_EXIT)) {
            id0.getDoc().populate();
            id0.getDoc().getLayoutControl().update();
            lblSelected = VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(id0);
            lblSelected.setSelected(true);

            prefs.setPref(FPref.SUBMENU_CURRENTMENU, id0.toString());
            Singletons.getModel().getPreferences().save();
        }
    }

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
    }

    /**
     * Pulls previous menu selection from preferences
     * and clicks it programatically.
     */
    public void selectPrevious() {
        EDocID selected = null;
        try {
            selected = EDocID.valueOf(Singletons.getModel()
                .getPreferences().getPref(FPref.SUBMENU_CURRENTMENU));
        } catch (final Exception e) { }

        if (selected != null && VMainMenu.SINGLETON_INSTANCE.getAllSubmenuLabels().get(selected) != null) {
            itemClick(selected);
        }
        else {
            itemClick(EDocID.HOME_CONSTRUCTED);
        }
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
}
