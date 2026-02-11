package forge.screens.match;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.RectangleOfDouble;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.framework.VEmptyDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.views.VDev;
import forge.screens.match.views.VField;
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

    // Tracking for dynamic split-mode cells so they can be cleaned up on relayout.
    private final List<DragCell> dynamicCells = new ArrayList<>();
    private RectangleOfDouble baseRoughBounds0; // percentage bounds before splitting
    private RectangleOfDouble baseRoughBounds1;

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

        //focus first enabled Prompt button if returning to match screen
        if (getBtnOK().isEnabled()) {
            getBtnOK().requestFocusInWindow();
        } else if (getBtnCancel().isEnabled()) {
            getBtnCancel().requestFocusInWindow();
        }

        // Reconstruct base field bounds by stripping extra fields and using
        // fillGap() to expand base cells into freed space at the pixel level.
        // Then capture rough bounds from the actual pixel positions so they
        // match the user's layout regardless of stale saved rough bounds.
        if (lstFields.size() > 2 && baseRoughBounds0 == null) {
            final DragCell c0 = lstFields.get(0).getParentCell();
            final DragCell c1 = lstFields.get(1).getParentCell();

            // Strip extra fields; collect resulting empty cells for gap filling.
            final List<DragCell> emptyCells = new ArrayList<>();
            for (int i = 2; i < lstFields.size(); i++) {
                final VField vField = lstFields.get(i);
                final DragCell parent = vField.getParentCell();
                if (parent != null) {
                    parent.removeDoc(vField);
                    vField.setParentCell(null);
                    if (parent.getDocs().isEmpty() && parent != c0 && parent != c1) {
                        emptyCells.add(parent);
                    }
                }
            }

            // Fill gaps at the pixel level — neighboring cells (c0/c1) expand
            // to absorb the removed split cells' space.
            for (final DragCell empty : emptyCells) {
                try {
                    SRearrangingUtil.fillGap(empty);
                } catch (final UnsupportedOperationException e) {
                    System.out.println("[FIELD_LAYOUT_DEBUG] fillGap failed: " + e.getMessage());
                }
                FView.SINGLETON_INSTANCE.removeDragCell(empty);
            }

            // Capture base bounds from the now-expanded pixel positions.
            c0.updateRoughBounds();
            c1.updateRoughBounds();
            baseRoughBounds0 = c0.getRoughBounds();
            baseRoughBounds1 = c1.getRoughBounds();
            System.out.println("[FIELD_LAYOUT_DEBUG] populate() fields=" + lstFields.size());
            System.out.println("[FIELD_LAYOUT_DEBUG] populate() baseRoughBounds0=" + baseRoughBounds0);
            System.out.println("[FIELD_LAYOUT_DEBUG] populate() baseRoughBounds1=" + baseRoughBounds1);
        }

        assignExtraFieldsToCells();

        // Determine (an) existing hand panel
        DragCell cellWithHands = null;
        for (final EDocID handId : EDocID.Hands) {
            cellWithHands = handId.getDoc().getParentCell();
            if (cellWithHands != null) {
                break;
            }
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
                // Hand present, add it if necessary
                if (parentCell == null) {
                    final EDocID fieldDoc = EDocID.Fields[iHandId];
                    if (fieldDoc.getDoc().getParentCell() != null) {
                        fieldDoc.getDoc().getParentCell().addDoc(myVHand);
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

    /**
     * Assigns extra player fields (index 2+) to cells based on current
     * layout and panel style preferences. Split mode uses percentage-based
     * rough bounds so the layout is resolution-independent.
     */
    private void assignExtraFieldsToCells() {
        if (lstFields.size() <= 2) {
            return;
        }
        final ForgePreferences preferences = FModel.getPreferences();
        final String layout = preferences.getPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT);
        // OFF disables all field layout features — use vanilla i%2 tabbed behavior.
        final boolean rowsMode = "ROWS".equals(layout);
        final boolean splitMode = !"OFF".equals(layout)
                && "SPLIT".equals(preferences.getPref(FPref.UI_MULTIPLAYER_FIELD_PANELS));
        System.out.println("[FIELD_LAYOUT_DEBUG] assignExtraFieldsToCells() layout=" + layout
                + ", splitMode=" + splitMode + ", fieldCount=" + lstFields.size());

        // Collect extra fields grouped by target cell index.
        // Skip fields that already have a parent cell (populate() may be called multiple times).
        final Map<Integer, List<VField>> fieldsByCell = new LinkedHashMap<>();
        for (int i = 2; i < lstFields.size(); i++) {
            if (lstFields.get(i).getParentCell() != null) {
                System.out.println("[FIELD_LAYOUT_DEBUG]   field[" + i + "] skipped (already has parentCell)");
                continue;
            }
            final int target = rowsMode ? 1 : (i % 2);
            fieldsByCell.computeIfAbsent(target, k -> new ArrayList<>()).add(lstFields.get(i));
        }

        if (fieldsByCell.isEmpty()) {
            return;
        }

        // Restore base bounds before splitting so cells have their full width.
        // Only for split mode — in tabbed mode, cells keep their current pixel layout.
        if (splitMode && baseRoughBounds0 != null) {
            lstFields.get(0).getParentCell().setRoughBounds(baseRoughBounds0);
            lstFields.get(1).getParentCell().setRoughBounds(baseRoughBounds1);
        }

        for (final Map.Entry<Integer, List<VField>> entry : fieldsByCell.entrySet()) {
            final DragCell cell = lstFields.get(entry.getKey()).getParentCell();
            final List<VField> fields = entry.getValue();

            if (!splitMode) {
                for (final VField vField : fields) {
                    System.out.println("[FIELD_LAYOUT_DEBUG]   tabbed: adding field to cell, cellBounds="
                            + cell.getRoughBounds());
                    cell.addDoc(vField);
                }
            } else {
                // Split using percentage-based rough bounds.
                final RectangleOfDouble rb = cell.getRoughBounds();
                final int totalSlots = 1 + fields.size();
                final double slotW = rb.getW() / totalSlots;
                System.out.println("[FIELD_LAYOUT_DEBUG]   split: originalBounds=" + rb
                        + ", totalSlots=" + totalSlots + ", slotW=" + slotW);

                // Shrink original cell to leftmost slot.
                cell.setRoughBounds(new RectangleOfDouble(rb.getX(), rb.getY(), slotW, rb.getH()));
                System.out.println("[FIELD_LAYOUT_DEBUG]   split: shrunk original cell to "
                        + cell.getRoughBounds());

                // Create a new cell for each extra field.
                for (int i = 0; i < fields.size(); i++) {
                    final DragCell newCell = new DragCell();
                    final double newX = rb.getX() + slotW * (i + 1);
                    final double newW = (i < fields.size() - 1) ? slotW : (rb.getW() - slotW * (i + 1));
                    newCell.setRoughBounds(new RectangleOfDouble(newX, rb.getY(), newW, rb.getH()));
                    System.out.println("[FIELD_LAYOUT_DEBUG]   split: new cell[" + i + "] bounds="
                            + newCell.getRoughBounds());
                    FView.SINGLETON_INSTANCE.addDragCell(newCell);
                    newCell.addDoc(fields.get(i));
                    dynamicCells.add(newCell);
                }
            }
        }

        // Apply rough bounds to pixel positions via the resize system.
        if (splitMode) {
            SResizingUtil.resizeWindow();
        }
    }

    /**
     * Re-applies field layout and panel style preferences to the current
     * match view. Called from menu when preferences change mid-game.
     */
    public void relayoutMultiplayerFields() {
        if (lstFields.size() <= 2 || baseRoughBounds0 == null) {
            return;
        }
        System.out.println("[FIELD_LAYOUT_DEBUG] relayoutMultiplayerFields() restoring base bounds");
        System.out.println("[FIELD_LAYOUT_DEBUG]   baseRoughBounds0=" + baseRoughBounds0
                + ", baseRoughBounds1=" + baseRoughBounds1);

        // 1. Remove extra fields from their current cells; clean up empties.
        final DragCell c0 = lstFields.get(0).getParentCell();
        final DragCell c1 = lstFields.get(1).getParentCell();
        for (int i = 2; i < lstFields.size(); i++) {
            final VField vField = lstFields.get(i);
            final DragCell parent = vField.getParentCell();
            if (parent != null) {
                parent.removeDoc(vField);
                vField.setParentCell(null);
                if (parent.getDocs().isEmpty() && parent != c0 && parent != c1) {
                    FView.SINGLETON_INSTANCE.removeDragCell(parent);
                }
            }
        }

        // 2. Remove dynamically created split cells.
        for (final DragCell cell : dynamicCells) {
            FView.SINGLETON_INSTANCE.removeDragCell(cell);
        }
        dynamicCells.clear();

        // 3. Restore original base cell rough bounds.
        c0.setRoughBounds(baseRoughBounds0);
        c1.setRoughBounds(baseRoughBounds1);

        // 4. Re-assign with current preferences.
        assignExtraFieldsToCells();

        // 5. Recalculate pixel layout from rough bounds and refresh.
        SResizingUtil.resizeWindow();
        SRearrangingUtil.updateBorders();
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
