package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.framework.VEmptyDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.views.VDev;
import forge.screens.match.views.VField;
import forge.screens.match.views.VYield;
import forge.screens.match.views.VHand;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.view.FView;

/** 
 * Top level view class for match UI drag layout.<br>
 * Has access methods for all draggable documents.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VMatchUI implements IVTopLevelUI {
    private List<VField> lstFields = new ArrayList<>();
    private List<VHand> lstHands = new ArrayList<>();

    // Other instantiations
    private final CMatchUI control;

    VMatchUI(final CMatchUI control) {
        this.control = control;
    }

    @Override
    public void instantiate() {
    }

    @Override
    public void populate() {
        // Dev mode disabled? Remove from parent cell if exists.
        final VDev vDev = getControl().getCDev().getView();
        if (!ForgePreferences.DEV_MODE) {
            if (vDev.getParentCell() != null) {
                final DragCell parent = vDev.getParentCell();
                parent.removeDoc(vDev);
                vDev.setParentCell(null);

                // If dev mode was first tab, the new first tab needs re-selecting.
                if (parent.getDocs().size() > 0) {
                    parent.setSelected(parent.getDocs().get(0));
                }
            }
        } else if (vDev.getParentCell() == null) {
            // Dev mode enabled? May already by added, or put in message cell by default.
            getControl().getCPrompt().getView().getParentCell().addDoc(vDev);
        }

        // Yield panel - only show when experimental yield options are enabled
        final VYield vYield = getControl().getCYield().getView();
        final boolean yieldEnabled = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.YIELD_EXPERIMENTAL_OPTIONS);
        if (!yieldEnabled) {
            if (vYield.getParentCell() != null) {
                final DragCell parent = vYield.getParentCell();
                parent.removeDoc(vYield);
                vYield.setParentCell(null);

                if (parent.getDocs().size() > 0) {
                    parent.setSelected(parent.getDocs().get(0));
                }
            }
        } else if (vYield.getParentCell() == null ||
                   !FView.SINGLETON_INSTANCE.getDragCells().contains(vYield.getParentCell())) {
            // Yield enabled but not in any cell or has stale reference - add to prompt cell by default
            DragCell promptCell = EDocID.REPORT_MESSAGE.getDoc().getParentCell();
            if (promptCell == null) {
                promptCell = EDocID.REPORT_LOG.getDoc().getParentCell();
            }
            if (promptCell != null) {
                promptCell.addDoc(vYield);
            }
        }

        //focus first enabled Prompt button if returning to match screen
        if (getBtnOK().isEnabled()) {
            getBtnOK().requestFocusInWindow();
        } else if (getBtnCancel().isEnabled()) {
            getBtnCancel().requestFocusInWindow();
        }

        // Ensure all field views are added to the layout
        for (int i = 0; i < lstFields.size(); i++) {
            VField vField = lstFields.get(i);
            // Check if field is in a visible cell (not a stale reference from old layout)
            DragCell parentCell = vField.getParentCell();
            if (parentCell != null && parentCell.isShowing()) {
                continue;
            }

            if (i < 2) {
                // Base fields: use REPORT_LOG's cell as fallback
                DragCell fallbackCell = EDocID.REPORT_LOG.getDoc().getParentCell();
                if (fallbackCell != null) {
                    fallbackCell.addDoc(vField);
                }
            } else {
                // Extra players: add to corresponding base field's cell
                DragCell baseFieldCell = lstFields.get(i % 2).getParentCell();
                if (baseFieldCell != null) {
                    baseFieldCell.addDoc(vField);
                }
            }
        }

        // Determine (an) existing hand panel
        DragCell cellWithHands = null;
        for (final EDocID handId : EDocID.Hands) {
            cellWithHands = handId.getDoc().getParentCell();
            if (cellWithHands != null && cellWithHands.isShowing()) {
                break;
            }
            cellWithHands = null;
        }
        if (cellWithHands == null) {
            // Default to a cell we know exists
            cellWithHands = EDocID.REPORT_LOG.getDoc().getParentCell();
        }
        for (int iHandId = 0; iHandId < EDocID.Hands.length; iHandId++) {
            final EDocID handId = EDocID.Hands[iHandId];
            final DragCell parentCell = handId.getDoc().getParentCell();
            VHand myVHand = null;
            for (final VHand vHand : lstHands) {
                if (handId.equals(vHand.getDocumentID())) {
                    myVHand = vHand;
                    break;
                }
            }

            if (myVHand == null) {
                // Hand not present, remove cell if necessary
                if (parentCell != null) {
                    parentCell.removeDoc(handId.getDoc());
                    handId.setDoc(new VEmptyDoc(handId));
                }
            } else {
                // Hand present, add it if necessary (check isShowing for stale references)
                if (parentCell == null || !parentCell.isShowing()) {
                    final EDocID fieldDoc = EDocID.Fields[iHandId];
                    DragCell fieldCell = fieldDoc.getDoc().getParentCell();
                    if (fieldCell != null && fieldCell.isShowing()) {
                        fieldCell.addDoc(myVHand);
                        continue;
                    }
                    cellWithHands.addDoc(myVHand);
                }
            }
        }

        // Fill in gaps
        SwingUtilities.invokeLater(() -> {
            for (final DragCell c : FView.SINGLETON_INSTANCE.getDragCells()) {
                if (c.getDocs().isEmpty()) {
                    SRearrangingUtil.fillGap(c);
                    FView.SINGLETON_INSTANCE.removeDragCell(c);
                }
            }
        });
    }

    public CMatchUI getControl() {
        return this.control;
    }

    public void setFieldViews(final List<VField> lst0) {
        this.lstFields = lst0;
    }

    public List<VField> getFieldViews() {
        return lstFields;
    }

    public void setHandViews(final List<VHand> lst0) {
        this.lstHands = lst0;
    }

    public FButton getBtnCancel() {
        return getControl().getCPrompt().getView().getBtnCancel();
    }

    public FButton getBtnOK() {
        return getControl().getCPrompt().getView().getBtnOK();
    }

    public List<VHand> getHands() {
        return lstHands;
    }

    @Override
    public boolean onSwitching(final FScreen fromScreen, final FScreen toScreen) {
        return true;
    }

    @Override
    public boolean onClosing(final FScreen screen) {
        if (!Singletons.getControl().getCurrentScreen().equals(screen)) {
            // Switch to this screen if not already showing
            Singletons.getControl().setCurrentScreen(screen);
        }

        if (control.concede()) {
            //switch back to menus music when closing screen
            SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);
            return true;
        }

        return false;
    }
}
