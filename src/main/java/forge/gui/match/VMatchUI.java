package forge.gui.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.views.VDev;
import forge.gui.match.views.VMessage;
import forge.properties.ForgePreferences.FPref;
import forge.view.FView;

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

    /** */
    @Override
    public void instantiate() {
    }

    /** */
    @Override
    public void populate() {
        SLayoutIO.loadLayout(null);

        // Dev mode disabled? Remove from parent cell if exists.
        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            if (VDev.SINGLETON_INSTANCE.getParentCell() != null) {
                final DragCell parent = VDev.SINGLETON_INSTANCE.getParentCell();
                parent.removeDoc(VDev.SINGLETON_INSTANCE);
                VDev.SINGLETON_INSTANCE.setParentCell(null);

                // If dev mode was first tab, the new first tab needs re-selecting.
                if (parent.getDocs().size() > 0) {
                    parent.setSelected(parent.getDocs().get(0));
                }
            }
        }
        // Dev mode enabled? May already by added, or put in message cell by default.
        else {
            if (VDev.SINGLETON_INSTANCE.getParentCell() == null) {
                VMessage.SINGLETON_INSTANCE.getParentCell().addDoc(VDev.SINGLETON_INSTANCE);
            }
        }

        // Fill in gaps
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
        });
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
