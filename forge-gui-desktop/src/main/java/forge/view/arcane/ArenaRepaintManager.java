package forge.view.arcane;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import forge.toolbox.ArenaCurtain;
import forge.toolbox.ArenaMotion;

/**
 * Keeps track of when the pictures Arena cards keep of themselves may have gone stale. A card at rest is painted
 * from such a picture instead of being drawn again, which is what lets a neighbour move at a full frame rate.
 *
 * <p>A card knows of its own repaints. What it cannot know of is a repaint of something around it, say a play area
 * repainted because whatever is selectable has changed, which it reads as it paints. So any repaint asked for of a
 * card area, or of anything holding one, outside a frame of the animation clock, puts every picture out of date.
 * Repaints of whatever holds no card (the card detail pane, the hover preview, the prompt) leave them alone.</p>
 */
public class ArenaRepaintManager extends RepaintManager {
    private static final Set<CardPanelContainer> areas = Collections.newSetFromMap(new WeakHashMap<>());
    private static int generation;
    /** Set while a card asks for its own repaint, which it counts itself. */
    static boolean cardAsking;

    /** Puts this manager in place, unless it (or one built on it) already is. */
    static void install(final JComponent anyComponent) {
        if (!(RepaintManager.currentManager(anyComponent) instanceof ArenaRepaintManager)) {
            RepaintManager.setCurrentManager(new ArenaRepaintManager());
        }
    }

    static void register(final CardPanelContainer area) {
        areas.add(area);
    }

    /** Changes whenever the pictures cards keep of themselves may no longer be right. */
    static int generation() {
        return generation;
    }

    /** Every card picture is out of date. */
    public static void invalidateAll() {
        generation++;
    }

    /**
     * Runs something that repaints the board only because what lies over it, or a card on it, has moved or come or
     * gone: say the hover preview, or a card being put in its place. No card looks any different for it.
     */
    public static void overBoard(final Runnable r) {
        final boolean was = cardAsking;
        cardAsking = true;
        try {
            r.run();
        } finally {
            cardAsking = was;
        }
    }

    private static void noticed(final Component c) {
        if (cardAsking || c == null) {
            return;
        }
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof CardPanel) {
                // something inside a card, such as one of its labels: only that card has changed
                ((CardPanel) p).arenaContentChanged();
                return;
            }
            if (p instanceof CardPanelContainer) {
                break;
            }
        }
        if (ArenaMotion.isTicking()) {
            return; // a frame of an animation, or a breathing effect: nothing a card reads has changed
        }
        if (holdsCards(c)) {
            generation++;
        }
    }

    /** Whether a card area is this, is inside it, or holds it. */
    private static boolean holdsCards(final Component c) {
        if (!(c instanceof Container)) {
            return false;
        }
        for (final CardPanelContainer area : areas) {
            if (area == c || SwingUtilities.isDescendingFrom(area, c) || SwingUtilities.isDescendingFrom(c, area)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
        if (w > 0 && h > 0) {
            noticed(c);
            ArenaCurtain.noteRepaint(c, x, y, w, h);
        }
        super.addDirtyRegion(c, x, y, w, h);
    }

    @Override
    public void addDirtyRegion(final Window window, final int x, final int y, final int w, final int h) {
        if (w > 0 && h > 0) {
            noticed(window);
            ArenaCurtain.noteRepaint(window, x, y, w, h);
        }
        super.addDirtyRegion(window, x, y, w, h);
    }
}
