package forge.gui.deckeditor;

import javax.swing.SwingWorker;

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

               /* this can be used for toggling panels in and out of the view (e.g. dev mode)

                make  are separate
                layout states for each deck editor state.
                however, the empty cells should still be removed.

                VFilters.SINGLETON_INSTANCE.getParentCell().removeDoc(VFilters.SINGLETON_INSTANCE);
                VProbabilities.SINGLETON_INSTANCE.getParentCell().removeDoc(VProbabilities.SINGLETON_INSTANCE);

                System.out.println(FView.SINGLETON_INSTANCE.getDragCells().size());

                for (final DragCell c : FView.SINGLETON_INSTANCE.getDragCells()) {
                    if (c.getDocs().size() == 0) {
                        SRearrangingUtil.fillGap(c);
                        FView.SINGLETON_INSTANCE.removeDragCell(c);
                    }
                } */
                return null;
            }
        };
        w.execute();
    }
}
