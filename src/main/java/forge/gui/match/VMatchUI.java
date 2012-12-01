package forge.gui.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.framework.VEmptyDoc;
import forge.gui.match.nonsingleton.VCommand;
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

    private List<VCommand> lstCommands = new ArrayList<VCommand>();
    private List<VField> lstFields = new ArrayList<VField>();
    private List<VHand> lstHands = new ArrayList<VHand>();

    // Other instantiations
    private final CMatchUI control = null;

    private VMatchUI() {
        // Create empty docs for all field slots
        for (int i = 0; i < 8; i++) {
            EDocID.valueOf("FIELD_" + i).setDoc(
                    new VEmptyDoc(EDocID.valueOf("FIELD_" + i)));
        }

     // Create empty docs for all field slots
        for (int i = 0; i < 8; i++) {
            EDocID.valueOf("COMMAND_" + i).setDoc(
                    new VEmptyDoc(EDocID.valueOf("COMMAND_" + i)));
        }

        // Create empty docs for all hand slots
        for (int i = 0; i < 4; i++) {
            EDocID.valueOf("HAND_" + i).setDoc(
                    new VEmptyDoc(EDocID.valueOf("HAND_" + i)));
        }
    }

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

        // Add extra players alternatively to existing user/AI field panels.
        for (int i = 2; i < lstFields.size(); i++) {
            // If already in layout, no need to add again.
            if (lstFields.get(i).getParentCell() != null) {
                continue;
            }

            if (i % 2 == 0) {
                lstFields.get(0).getParentCell().addDoc(lstFields.get(i));
            }
            else {
                lstFields.get(1).getParentCell().addDoc(lstFields.get(i));
            }
        }

     // Add extra players alternatively to existing user/AI field panels.
        for (int i = 2; i < lstCommands.size(); i++) {
            // If already in layout, no need to add again.
            if (lstCommands.get(i).getParentCell() != null) {
                continue;
            }

            if (i % 2 == 0) {
                lstCommands.get(0).getParentCell().addDoc(lstCommands.get(i));
            }
            else {
                lstCommands.get(1).getParentCell().addDoc(lstCommands.get(i));
            }
        }

        // Add extra hands to existing hand panel.
        for (int i = 0; i < lstHands.size(); i++) {
            // If already in layout, no need to add again.
            if (lstHands.get(i).getParentCell() != null) {
                continue;
            }

            lstHands.get(0).getParentCell().addDoc(lstHands.get(i));
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

    /** @param lst0 List<VField> */
    public void setFieldViews(final List<VField> lst0) {
        this.lstFields = lst0;
    }

    /** @return {@link java.util.List}<{@link forge.gui.match.nonsigleton.VHand}> */
    public List<VField> getFieldViews() {
        return lstFields;
    }

    /** @param lst0 List<VField> */
    public void setHandViews(final List<VHand> lst0) {
        this.lstHands = lst0;
    }

    /** @return {@link java.util.List}<{@link forge.gui.match.nonsigleton.VHand}> */
    public List<VHand> getHandViews() {
        return lstHands;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnCancel() {
        return VMessage.SINGLETON_INSTANCE.getBtnCancel();
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnOK() {
        return VMessage.SINGLETON_INSTANCE.getBtnOK();
    }

    /**
     * @return the lstCommands
     */
    public List<VCommand> getCommandViews() {
        return lstCommands;
    }

    /**
     * @param lstCommands0 the lstCommands to set
     */
    public void setCommandViews(List<VCommand> lstCommands0) {
        this.lstCommands = lstCommands0;
    }
}
