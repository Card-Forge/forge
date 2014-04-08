package forge.screens.workshop;

import forge.Singletons;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.screens.workshop.controllers.CCardScript;
import forge.screens.workshop.views.VWorkshopCatalog;

import javax.swing.*;

/** 
/** 
 * Top level view class; instantiates and assembles
 * tabs used in deck editor UI drag layout.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 * 
 */
public enum VWorkshopUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#instantiate()
     */
    @Override
    public void instantiate() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#populate()
     */
    @Override
    public void populate() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().focus();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onSwitching(forge.gui.framework.FScreen)
     */
    @Override
    public boolean onSwitching(FScreen fromScreen, FScreen toScreen) {
        return CCardScript.SINGLETON_INSTANCE.canSwitchAway(false);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onClosing()
     */
    @Override
    public boolean onClosing(FScreen screen) {
    	//don't close tab, but return to home screen if this called
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
    	return false;
    }
}
