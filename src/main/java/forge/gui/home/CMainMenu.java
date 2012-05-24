package forge.gui.home;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.properties.ForgePreferences.FPref;

/** 
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum CMainMenu implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /** Programatically selects a menu item.
     * @param e0 &emsp; {@forge.gui.home.EMenuItem} */
    public void itemClick(final EDocID e0) {
        VMainMenu.SINGLETON_INSTANCE.getAllSubmenuLabels().get(e0).getCommand().execute();
        VMainMenu.SINGLETON_INSTANCE.getAllSubmenuLabels().get(e0).setSelected(true);
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
