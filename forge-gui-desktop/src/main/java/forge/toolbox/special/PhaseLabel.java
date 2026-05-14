package forge.toolbox.special;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import forge.game.phase.PhaseType;
import forge.toolbox.FSkin;

/**
 * Shows phase labels, handles repainting and on/off states. A PhaseLabel
 * has "skip" and "active" states, meaning "this phase is (not) skipped" and
 * "this is the current phase".
 */
@SuppressWarnings("serial")
public class PhaseLabel extends JLabel {
    /** Tint used when a yield marker is targeted at this (player, phase) cell. */
    private static final Color YIELD_MARKER_COLOR = new Color(0xFFA528);

    private final PhaseType phaseType;
    private boolean enabled = true;
    private boolean active = false;
    private boolean hover = false;
    private boolean yieldMarked = false;
    private Runnable onToggled;
    private Runnable onRightClick;


    /**
     * Shows phase labels, handles repainting and on/off states. A
     * PhaseLabel has "skip" and "active" states, meaning
     * "this phase is (not) skipped" and "this is the current phase".
     *
     * @param txt &emsp; Label text
     * @param phaseType0 &emsp; The PhaseType this label represents (may be null for header/spacer labels)
     */
    public PhaseLabel(final String txt, final PhaseType phaseType0) {
        super(txt);
        this.phaseType = phaseType0;
        this.setHorizontalTextPosition(SwingConstants.CENTER);
        this.setHorizontalAlignment(SwingConstants.CENTER);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (PhaseLabel.this.onRightClick != null) {
                        PhaseLabel.this.onRightClick.run();
                    }
                    return;
                }
                PhaseLabel.this.enabled = !PhaseLabel.this.enabled;
                if (PhaseLabel.this.onToggled != null) {
                    PhaseLabel.this.onToggled.run();
                }
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                PhaseLabel.this.hover = true;
                PhaseLabel.this.repaintOnlyThisLabel();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                PhaseLabel.this.hover = false;
                PhaseLabel.this.repaintOnlyThisLabel();
            }
        });
    }

    public PhaseType getPhaseType() {
        return phaseType;
    }

    public boolean isYieldMarked() {
        return yieldMarked;
    }

    public void setYieldMarked(final boolean b) {
        if (this.yieldMarked != b) {
            this.yieldMarked = b;
            repaintOnlyThisLabel();
        }
    }

    /**
     * Determines whether play pauses at this phase or not.
     *
     * @param b &emsp; boolean, true if play pauses
     */
    @Override
    public void setEnabled(final boolean b) {
        this.enabled = b;
    }

    /**
     * Determines whether play pauses at this phase or not.
     *
     * @return boolean
     */
    public boolean getEnabled() {
        return this.enabled;
    }

    /** Fires after the user toggles this label by clicking. */
    public void setOnToggled(final Runnable r) {
        this.onToggled = r;
    }

    /** Fires when the user right-clicks this label. */
    public void setOnRightClick(final Runnable r) {
        this.onRightClick = r;
    }

    /**
     * Makes this phase the current phase (or not).
     *
     * @param b &emsp; boolean, true if phase is current
     */
    public void setActive(final boolean b) {
        this.active = b;
        this.repaintOnlyThisLabel();
    }

    /**
     * Determines if this phase is the current phase (or not).
     *
     * @return boolean
     */
    public boolean getActive() {
        return this.active;
    }

    /** Prevent label from repainting the whole screen. */
    public void repaintOnlyThisLabel() {
        final Dimension d = PhaseLabel.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    public void paintComponent(final Graphics g) {
        final int w = this.getWidth();
        final int h = this.getHeight();

        // Precedence: hover > yieldMarked > active/enabled combinations.
        if (this.hover) {
            FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_HOVER));
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }
        else if (this.yieldMarked) {
            g.setColor(YIELD_MARKER_COLOR);
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }
        else if (this.active && this.enabled) {
            FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_ENABLED));
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }
        else if (!this.active && this.enabled) {
            FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_ENABLED));
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }
        else if (this.active && !this.enabled) {
            FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_DISABLED));
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }
        else {
            FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_DISABLED));
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        }

        if (this.yieldMarked) {
            drawChevron(g, w, h);
        } else {
            super.paintComponent(g);
        }
    }

    private void drawChevron(final Graphics g, final int w, final int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            // Two back-to-back triangles — total width is `size`, total height is `size`.
            int size = Math.max(6, (int) (h * 0.55));
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            int[] xs1 = {x, x + size / 2, x};
            int[] ys1 = {y, y + size / 2, y + size};
            g2.fillPolygon(xs1, ys1, 3);
            int[] xs2 = {x + size / 2, x + size, x + size / 2};
            int[] ys2 = {y, y + size / 2, y + size};
            g2.fillPolygon(xs2, ys2, 3);
        } finally {
            g2.dispose();
        }
    }
}
