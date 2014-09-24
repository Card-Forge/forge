package forge.screens.match;

import forge.gui.framework.*;
import forge.match.MatchUtil;
import forge.properties.ForgePreferences;
import forge.screens.match.views.*;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.view.FView;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

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
    private boolean wasClosed;
    private final CMatchUI control = null;

    private VMatchUI() {
        createEmptyDocs();
    }

    private void createEmptyDocs() {
    	 // Create empty docs for all slots
    	for (int i = 0; i < 8; i++) EDocID.Fields[i].setDoc(new VEmptyDoc(EDocID.Fields[i]));
        for (int i = 0; i < 8; i++) EDocID.Commands[i].setDoc(new VEmptyDoc(EDocID.Commands[i]));
        for (int i = 0; i < 8; i++) EDocID.Hands[i].setDoc(new VEmptyDoc(EDocID.Hands[i]));
    }

    /** */
    @Override
    public void instantiate() {
    }

    /** */
    @Override
    public void populate() {
        // Dev mode disabled? Remove from parent cell if exists.
        if (!ForgePreferences.DEV_MODE) {
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
                VPrompt.SINGLETON_INSTANCE.getParentCell().addDoc(VDev.SINGLETON_INSTANCE);
            }
        }

        //Clear previous match views if screen was previously closed
        if (wasClosed) {
            wasClosed = false;
        }
        else { //focus first enabled Prompt button if returning to match screen
            if (getBtnOK().isEnabled()) {
                getBtnOK().requestFocusInWindow();
            }
            else if (getBtnCancel().isEnabled()) {
                getBtnCancel().requestFocusInWindow();
            }
        }

        // Add extra players alternatively to existing user/AI field panels.
        for (int i = 2; i < lstFields.size(); i++) {
            // If already in layout, no need to add again.
            VField vField = lstFields.get(i);
            if (vField.getParentCell() == null) {
                lstFields.get(i % 2).getParentCell().addDoc(vField);
            }
        }

        if (MatchUtil.getGameView().isCommandZoneNeeded()) {
            // Add extra players alternatively to existing user/AI field panels.
            for (int i = 2; i < lstCommands.size(); i++) {
                // If already in layout, no need to add again.
                VCommand cmdView = lstCommands.get(i);
                if (cmdView.getParentCell() == null) {
                    lstCommands.get(i % 2).getParentCell().addDoc(cmdView);
                }
            }
        }
        else {
            //If game goesn't need command zone, remove it from existing field panels
            for (int i = 0; i < 2; i++) {
                VCommand cmdView = lstCommands.get(i);
                if (cmdView.getParentCell() != null) {
                    cmdView.getParentCell().removeDoc(cmdView);
                }
            }
        }

        // Add extra hands to existing hand panel.
        for (int i = 1; i < lstHands.size(); i++) {
            // If already in layout, no need to add again.
            if (lstHands.get(i).getParentCell() == null) { // if i == 0, we get NPE in two lines
                DragCell cellWithHand = lstHands.get(0).getParentCell();
                cellWithHand.addDoc(lstHands.get(i));
            }
        }

        // Remove any hand panels that aren't needed anymore
        for (int i = EDocID.Hands.length - 1; i >= lstHands.size(); i--) {
            DragCell cellWithHand = EDocID.Hands[i].getDoc().getParentCell();
            if (cellWithHand != null) {
                cellWithHand.removeDoc(EDocID.Hands[i].getDoc());
                EDocID.Hands[i].setDoc(new VEmptyDoc(EDocID.Hands[i]));
            }
        }

        // Fill in gaps
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final DragCell c : FView.SINGLETON_INSTANCE.getDragCells()) {
                    if (c.getDocs().isEmpty()) {
                        SRearrangingUtil.fillGap(c);
                        FView.SINGLETON_INSTANCE.removeDragCell(c);
                    }
                }
            }
        });
    }

    //========== Retrieval methods

    /** @return {@link forge.screens.match.CMatchUI} */
    public CMatchUI getControl() {
        return this.control;
    }

    /** @param lst0 List<VField> */
    public void setFieldViews(final List<VField> lst0) {
        this.lstFields = lst0;
    }

    /** @return {@link java.util.List}<{@link forge.screens.match.views.VHand}> */
    public List<VField> getFieldViews() {
        return lstFields;
    }

    /** @param lst0 List<VField> */
    public void setHandViews(final List<VHand> lst0) {
        this.lstHands = lst0;
    }

    public FButton getBtnCancel() {
        return VPrompt.SINGLETON_INSTANCE.getBtnCancel();
    }

    public FButton getBtnOK() {
        return VPrompt.SINGLETON_INSTANCE.getBtnOK();
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

    public List<VHand> getHands() {
        return lstHands;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onSwitching(forge.gui.framework.FScreen)
     */
    @Override
    public boolean onSwitching(FScreen fromScreen, FScreen toScreen) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onClosing(forge.control.FControl.Screens)
     */
    @Override
    public boolean onClosing(FScreen screen) {
        if (!MatchUtil.getGameView().isGameOver()) {
            MatchUtil.concede();
            return false; //delay hiding tab even if concede successful
        }

        //switch back to menus music when closing screen
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);

        wasClosed = true;
        return true;
    }
}
