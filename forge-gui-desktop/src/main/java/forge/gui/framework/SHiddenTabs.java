package forge.gui.framework;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import forge.view.FView;

/**
 * Tracks tabs that have been taken out of the layout temporarily, so they can be
 * put back where they came from.
 * <p>
 * Limited and quest deck editors hide several tabs while they are open. The record
 * of what was hidden is kept here rather than in each editor, because the layout is
 * global: two editors can be constructed for the same screen, and the one that
 * restores is not always the one that hid. Keeping it here also lets
 * {@link SLayoutIO} write hidden tabs into a saved layout, so a save taken while
 * they are out does not drop them from the file.
 *
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SHiddenTabs {
    private SHiddenTabs() { }

    /** Where a hidden doc came from: its cell, and that cell's bounds at the time. */
    private record Home(DragCell cell, RectangleOfDouble bounds) { }

    /** Hidden docs mapped to where they came from, in the order they were hidden. */
    private static final Map<IVDoc<? extends ICDoc>, Home> HIDDEN = new LinkedHashMap<>();

    /**
     * Takes each doc out of the layout, remembering where it came from.
     * <p>
     * Hiding a doc that is already hidden does nothing. An editor's update() runs
     * every time it becomes current, and a second editor can be constructed for a
     * screen before the first has restored, so without this a repeat call would
     * overwrite a real cell with nothing and the doc could never be put back.
     */
    @SafeVarargs
    public static void hide(final IVDoc<? extends ICDoc>... docs) {
        for (final IVDoc<? extends ICDoc> doc : docs) {
            if (doc == null || HIDDEN.containsKey(doc)) {
                continue;
            }
            final DragCell parent = doc.getParentCell();
            if (parent == null) {
                continue; // not placed in this layout, so there is nothing to restore
            }

            parent.updateRoughBounds();
            HIDDEN.put(doc, new Home(parent, parent.getRoughBounds()));

            parent.removeDoc(doc);
            doc.setParentCell(null);

            if (!parent.getDocs().isEmpty()) {
                // the hidden doc may have been the selected tab
                parent.setSelected(parent.getDocs().get(0));
            } else {
                // hiding emptied the cell, so close the gap it leaves behind
                SwingUtilities.invokeLater(() -> {
                    SRearrangingUtil.fillGap(parent);
                    FView.SINGLETON_INSTANCE.removeDragCell(parent);
                });
            }
        }
    }

    /** Puts every hidden doc back, in the order it was hidden. */
    public static void restoreAll() {
        if (HIDDEN.isEmpty()) {
            return;
        }
        final Map<IVDoc<? extends ICDoc>, Home> toRestore = new LinkedHashMap<>(HIDDEN);
        HIDDEN.clear();

        for (final Map.Entry<IVDoc<? extends ICDoc>, Home> entry : toRestore.entrySet()) {
            final DragCell target = restoreTarget(entry.getValue());
            if (target != null) {
                target.addDoc(entry.getKey());
            }
        }
    }

    public static boolean isHidden(final IVDoc<? extends ICDoc> doc) {
        return HIDDEN.containsKey(doc);
    }

    /**
     * Hidden docs mapped to the cell each should be written into or restored to.
     * Empty when nothing is hidden. Used by {@link SLayoutIO} when saving.
     */
    public static Map<IVDoc<? extends ICDoc>, DragCell> getHidden() {
        final Map<IVDoc<? extends ICDoc>, DragCell> out = new LinkedHashMap<>();
        for (final Map.Entry<IVDoc<? extends ICDoc>, Home> entry : HIDDEN.entrySet()) {
            final DragCell target = restoreTarget(entry.getValue());
            if (target != null) {
                out.put(entry.getKey(), target);
            }
        }
        return out;
    }

    /**
     * The cell a hidden doc should go back to. Normally the one it came from, but
     * hiding the last doc in a cell tears that cell down, so it may no longer be in
     * the layout. In that case pick whichever cell now covers the space it used to
     * occupy, since that is the one that grew to fill the gap.
     */
    private static DragCell restoreTarget(final Home home) {
        final List<DragCell> live = FView.SINGLETON_INSTANCE.getDragCells();
        if (live.isEmpty()) {
            return null;
        }
        if (home.cell() != null && live.contains(home.cell())) {
            return home.cell();
        }

        final RectangleOfDouble was = home.bounds();
        if (was != null) {
            final double cx = was.getX() + (was.getW() / 2);
            final double cy = was.getY() + (was.getH() / 2);
            final List<DragCell> covering = new ArrayList<>();
            for (final DragCell cell : live) {
                cell.updateRoughBounds();
                final RectangleOfDouble b = cell.getRoughBounds();
                if (cx >= b.getX() && cx <= b.getX() + b.getW()
                        && cy >= b.getY() && cy <= b.getY() + b.getH()) {
                    covering.add(cell);
                }
            }
            if (!covering.isEmpty()) {
                return covering.get(0);
            }
        }
        return live.get(0);
    }
}
