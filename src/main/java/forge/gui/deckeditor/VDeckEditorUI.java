package forge.gui.deckeditor;

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;

/** 
/** 
 * Top level view class; instantiates and assembles
 * tabs used in deck editor UI drag layout.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 * 
 */
public enum VDeckEditorUI implements IVTopLevelUI {
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
                VCardCatalog.SINGLETON_INSTANCE.getItemManager().focus();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onSwitching(forge.gui.framework.FScreen)
     */
    @Override
    public boolean onSwitching(FScreen screen) {
        return CDeckEditorUI.SINGLETON_INSTANCE.canExit(); //ensure deck saved before switching away
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onClosing()
     */
    @Override
    public boolean onClosing(FScreen screen) {
        if (screen == FScreen.DECK_EDITOR_CONSTRUCTED) {
            //don't close tab if Constructed editor, but return to home screen if this called
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
            return false;
        }
        return CDeckEditorUI.SINGLETON_INSTANCE.canExit();
    }
}
