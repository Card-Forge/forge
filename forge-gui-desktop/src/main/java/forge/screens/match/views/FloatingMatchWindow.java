/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.views;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.view.FDialog;

/**
 * Base for the match windows that float over the board instead of living in a
 * dock cell. Handles remembering where the user dragged the window to, and
 * placing it sensibly the first time it appears.
 */
@SuppressWarnings("serial")
public abstract class FloatingMatchWindow extends FDialog {
    private static final String COORD_DELIM = ",";

    private final FPref locPref;
    private final Timer saveLocTimer;

    /** Set once the window has been placed, so later shows don't yank it back. */
    private boolean located;
    /** Bounds after the last move we made ourselves — used to tell our moves from the user's. */
    private Rectangle lastAutoBounds;

    protected FloatingMatchWindow(final FPref locPref0, final boolean allowResize) {
        super(JOptionPane.getRootFrame(), false, allowResize, "2");
        locPref = locPref0;
        getTitleBar().setCloseButtonVisible(false);

        // Coalesce bursts of move/resize events into a single preference write.
        saveLocTimer = new Timer(400, e -> saveBounds()); //non-repeating, so it stops itself
        saveLocTimer.setRepeats(false);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(final ComponentEvent e) { onBoundsChanged(); }
            @Override public void componentResized(final ComponentEvent e) { onBoundsChanged(); }
        });
    }

    /** The panel this window's contents are laid out in. */
    public JPanel getContentPanel() {
        return (JPanel) getContentPane();
    }

    /**
     * Runs a move or resize of our own. Component events arrive after the fact,
     * so the resulting bounds are recorded and later compared against, rather
     * than using a flag that would already be cleared by then.
     */
    protected void reposition(final Runnable move) {
        move.run();
        lastAutoBounds = getBounds();
    }

    private void onBoundsChanged() {
        if (!located || !isVisible()) { return; }
        if (getBounds().equals(lastAutoBounds)) { return; } //our own doing, not the user's
        saveLocTimer.restart();
    }

    @Override
    public void setVisible(final boolean visible) {
        final boolean wasVisible = isVisible();
        if (visible && !wasVisible) {
            applyStoredLocation(); //place before showing, so it doesn't flash at the wrong spot
        }
        super.setVisible(visible);
        if (visible && !wasVisible) {
            // FDialog.setVisible re-centres on every show; undo that so the
            // window stays where the user put it.
            applyStoredLocation();
            located = true;
        }
    }

    protected void applyStoredLocation() {
        final Rectangle b = storedBounds();
        if (b == null) {
            placeByDefault();
            return;
        }
        reposition(() -> {
            if (restoresSize()) {
                setBounds(b);
            } else {
                setLocation(b.x, b.y);
            }
        });
        if (!isOnScreen()) { placeByDefault(); } //display layout may have changed since last run
    }

    /** True once the user has dragged the window somewhere of their own choosing. */
    protected boolean isUserPlaced() {
        return storedBounds() != null;
    }

    /** Whether the stored size is restored along with the stored position. */
    protected boolean restoresSize() {
        return true;
    }

    /** Where the window sits before the user has moved it. */
    protected void placeByDefault() {
        final Rectangle r = ownerBounds();
        reposition(() -> setLocation(
                r.x + (r.width - getWidth()) / 2,
                r.y + (r.height - getHeight()) / 2));
    }

    protected static Rectangle ownerBounds() {
        final Window owner = JOptionPane.getRootFrame();
        return owner != null && owner.isShowing()
                ? owner.getBounds()
                : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    protected Rectangle storedBounds() {
        final String s = FModel.getPreferences().getPref(locPref);
        if (s == null || s.isEmpty()) { return null; }
        final String[] parts = s.split(COORD_DELIM);
        if (parts.length != 4) { return null; }
        try {
            return new Rectangle(
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (final NumberFormatException ex) {
            return null;
        }
    }

    private boolean isOnScreen() {
        final Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        // Require a decent chunk of the title bar to be reachable, not just one pixel.
        return screen.intersects(new Rectangle(getX(), getY(), getWidth(), 30));
    }

    private void saveBounds() {
        final Point p = getLocation();
        FModel.getPreferences().setPref(locPref,
                p.x + COORD_DELIM + p.y + COORD_DELIM + getWidth() + COORD_DELIM + getHeight());
        FModel.getPreferences().save();
    }
}
