package forge.gui.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import forge.AllZone;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.views.VMessage;

/** 
 * Top level view class for match UI drag layout.<br>
 * Has access methods for all draggable documents.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VMatchUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    // Instantiate non-singleton tab instances
    private final IVDoc field0 = new VField(EDocID.FIELD_0, AllZone.getComputerPlayer());
    private final IVDoc field1 = new VField(EDocID.FIELD_1, AllZone.getHumanPlayer());

    private final IVDoc hand0 = new VHand(EDocID.HAND_0, AllZone.getComputerPlayer());
    private final IVDoc hand1 = new VHand(EDocID.HAND_1, AllZone.getHumanPlayer());


    // Other instantiations
    private final CMatchUI control = null;
    private boolean isPopulated = false;

    /** */
    @Override
    public void instantiate() {
    }

    /** */
    @Override
    public void populate() {
        if (isPopulated) { return; }
        else { isPopulated = true; }
        SLayoutIO.loadLayout(null);

        /*System.out.println(SwingUtilities.isEventDispatchThread());
        final SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                SLayoutIO.loadLayout(null);
                return null;
            }
        };
        w.execute();

        // Pull dev mode if necessary, remove parent cell if required.
        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            VDev.SINGLETON_INSTANCE.getParentCell().removeDoc(VDev.SINGLETON_INSTANCE);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final DragCell c : FView.SINGLETON_INSTANCE.getDragCells()) {
                    if (c.getDocs().size() == 0) {
                        SRearrangingUtil.fillGap(c);
                        FView.SINGLETON_INSTANCE.removeDragCell(c);
                    }
                }
            }
        });*/
    }

    //========== Retrieval methods

    /** @return {@link forge.gui.match.CMatchUI} */
    public CMatchUI getControl() {
        return this.control;
    }

    /** @return {@link java.util.List}<{@link forge.gui.match.nonsigleton.VField}> */
    public List<VField> getFieldViews() {
        final List<VField> lst = new ArrayList<VField>();
        lst.add((VField) field0);
        lst.add((VField) field1);
        return lst;
    }

    /** @return {@link java.util.List}<{@link forge.gui.match.nonsigleton.VHand}> */
    public List<VHand> getHandViews() {
        final List<VHand> lst = new ArrayList<VHand>();
        lst.add((VHand) hand0);
        lst.add((VHand) hand1);
        return lst;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnCancel() {
        return VMessage.SINGLETON_INSTANCE.getBtnCancel();
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnOK() {
        return VMessage.SINGLETON_INSTANCE.getBtnOK();
    }
}
