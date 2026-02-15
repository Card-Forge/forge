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

    // Other instantiations
    private final CMatchUI control;

    VMatchUI(final CMatchUI control) {
        this.control = control;
    }

    @Override
    public void instantiate() {
    }

    /**
     * Computes the full available region for field cells in a given row by
     * scanning non-field cells. Returns a RectangleOfDouble covering the
     * horizontal span between adjacent non-field cells.
     */
    private RectangleOfDouble computeFieldRegion(final DragCell baseFieldCell) {
        final RectangleOfDouble fieldBounds = baseFieldCell.getRoughBounds();
        final double rowY = fieldBounds.getY();
        final double rowH = fieldBounds.getH();

        double leftBoundary = 0.0;
        double rightBoundary = 1.0;

        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            // Skip only the target cell and dynamic cells. The other base
            // field cell is included so it acts as a boundary when both
            // base cells share the same row (e.g. side-by-side layouts).
            if (cell == baseFieldCell || dynamicCells.contains(cell)) {
                continue;
            }
            final RectangleOfDouble cb = cell.getRoughBounds();
            // Check for vertical overlap with the field row.
            if (cb.getY() < rowY + rowH && cb.getY() + cb.getH() > rowY) {
                final double cellRight = cb.getX() + cb.getW();
                final double cellLeft = cb.getX();
                if (cellRight <= fieldBounds.getX() + 0.001) {
                    // Cell is to the left of the field region.
                    leftBoundary = Math.max(leftBoundary, cellRight);
                } else if (cellLeft >= fieldBounds.getX() + fieldBounds.getW() - 0.001) {
                    // Cell is to the right of the field region.
                    rightBoundary = Math.min(rightBoundary, cellLeft);
                }
            }
        }
        return new RectangleOfDouble(leftBoundary, rowY,
                rightBoundary - leftBoundary, rowH);
    }

    /**
     * Removes all extra field assignments and any cells that become empty,
     * then restores base field cells to full width.
     * Idempotent — safe to call when no dynamic state exists.
     */
    private void cleanupDynamicCells() {
        final DragCell c0 = lstFields.get(0).getParentCell();
        final DragCell c1 = lstFields.get(1).getParentCell();

        // Strip extra fields (index 2+) and remove their parent cells if
        // emptied. This handles both dynamic split cells and cells loaded
        // from a saved layout that contained extra field slots.
        for (int i = 2; i < lstFields.size(); i++) {
            final VField vField = lstFields.get(i);
            final DragCell parent = vField.getParentCell();
            if (parent != null) {
                parent.removeDoc(vField);
                vField.setParentCell(null);
                if (parent != c0 && parent != c1 && parent.getDocs().isEmpty()) {
                    SRearrangingUtil.fillGap(parent);
                    FView.SINGLETON_INSTANCE.removeDragCell(parent);
                }
            }
        }

        // Force-remove any remaining tracked dynamic cells that weren't
        // already removed above (e.g., if non-field docs were added to them).
        for (final DragCell cell : dynamicCells) {
            FView.SINGLETON_INSTANCE.removeDragCell(cell);
        }
        dynamicCells.clear();

        // Sync rough bounds with actual pixel positions before computing
        // field regions. This ensures boundary detection works with accurate
        // positions even if rough bounds drifted from earlier operations.
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            cell.updateRoughBounds();
        }

        // Restore base cells to full width via dynamic computation.
        if (c0 != null) {
            c0.setRoughBounds(computeFieldRegion(c0));
        }
        if (c1 != null) {
            c1.setRoughBounds(computeFieldRegion(c1));
        }
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


        // Clean up any existing dynamic split cells before re-assigning.
        if (lstFields.size() > 2) {
            cleanupDynamicCells();
        }

        // Ensure all field views are added to the layout
        for (int i = 0; i < 2; i++) {
            VField vField = lstFields.get(i);
            // Check if field is in a visible cell (not a stale reference from old layout)
            DragCell parentCell = vField.getParentCell();
            if (parentCell != null && parentCell.isShowing()) {
                continue;
            }

            // Base fields: use REPORT_LOG's cell as fallback
            DragCell fallbackCell = EDocID.REPORT_LOG.getDoc().getParentCell();
            if (fallbackCell != null) {
                fallbackCell.addDoc(vField);
            }
        }

        assignExtraFieldsToCells();

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

        // When ROWS mode is active and c0/c1 share the same row (e.g.
        // side-by-side layout after gap-fill), rearrange them vertically
        // so opponents (c1) are on top and player (c0) is on bottom.
        boolean rearrangedRows = false;
        if (rowsMode) {
            final DragCell c0 = lstFields.get(0).getParentCell();
            final DragCell c1 = lstFields.get(1).getParentCell();
            if (c0 != null && c1 != null) {
                final RectangleOfDouble rb0 = c0.getRoughBounds();
                final RectangleOfDouble rb1 = c1.getRoughBounds();
                if (Math.abs(rb0.getY() - rb1.getY()) < 0.01) {
                    final double left = Math.min(rb0.getX(), rb1.getX());
                    final double right = Math.max(rb0.getX() + rb0.getW(),
                            rb1.getX() + rb1.getW());
                    final double fullW = right - left;
                    final double halfH = rb0.getH() / 2.0;
                    c1.setRoughBounds(new RectangleOfDouble(
                            left, rb0.getY(), fullW, halfH));
                    c0.setRoughBounds(new RectangleOfDouble(
                            left, rb0.getY() + halfH, fullW, halfH));
                    rearrangedRows = true;
                }
            }
        }

        // Collect extra fields grouped by target cell index.
        // For odd player counts in grid mode, prefer the top row (index 1)
        // so the local player is alone on the bottom row.
        final boolean preferTop = !rowsMode && (lstFields.size() % 2 == 1);
        final Map<Integer, List<VField>> fieldsByCell = new LinkedHashMap<>();
        for (int i = 2; i < lstFields.size(); i++) {
            final int target = rowsMode ? 1 : (preferTop ? ((i + 1) % 2) : (i % 2));
            fieldsByCell.computeIfAbsent(target, k -> new ArrayList<>()).add(lstFields.get(i));
        }

        for (final Map.Entry<Integer, List<VField>> entry : fieldsByCell.entrySet()) {
            final DragCell cell = lstFields.get(entry.getKey()).getParentCell();
            final List<VField> fields = entry.getValue();

            if (!splitMode) {
                for (final VField vField : fields) {
                    cell.addDoc(vField);
                }
            } else {
                // Split using percentage-based rough bounds.
                final RectangleOfDouble rb = cell.getRoughBounds();
                final int totalSlots = 1 + fields.size();
                final double slotW = rb.getW() / totalSlots;

                // Shrink original cell to leftmost slot.
                cell.setRoughBounds(new RectangleOfDouble(rb.getX(), rb.getY(), slotW, rb.getH()));

                // Create a new cell for each extra field.
                for (int i = 0; i < fields.size(); i++) {
                    final DragCell newCell = new DragCell();
                    final double newX = rb.getX() + slotW * (i + 1);
                    final double newW = (i < fields.size() - 1) ? slotW : (rb.getW() - slotW * (i + 1));
                    newCell.setRoughBounds(new RectangleOfDouble(newX, rb.getY(), newW, rb.getH()));
                    FView.SINGLETON_INSTANCE.addDragCell(newCell);
                    newCell.addDoc(fields.get(i));
                    dynamicCells.add(newCell);
                }
            }
        }

        // Apply rough bounds to pixel positions via the resize system.
        if (splitMode || rearrangedRows) {
            SResizingUtil.resizeWindow();
        }
    }

    /**
     * Re-applies field layout and panel style preferences to the current
     * match view. Called from menu when preferences change mid-game.
     */
    public void relayoutMultiplayerFields() {
        if (lstFields.size() <= 2) {
            return;
        }
        cleanupDynamicCells();
        assignExtraFieldsToCells();
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
