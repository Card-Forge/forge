package forge.gui.deckeditor;

import javax.swing.SwingWorker;

import forge.gui.deckeditor.controllers.CCardCatalog;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;

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
        final SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                SLayoutIO.loadLayout(null);
                VCardCatalog.SINGLETON_INSTANCE.focusTable();
                return null;
            }
        };
        CCardCatalog.SINGLETON_INSTANCE.applyCurrentFilter();
        w.execute();
    }
}
