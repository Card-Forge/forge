package forge.toolbox.special;

import forge.toolbox.FSkin;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shows phase labels, handles repainting and on/off states. A PhaseLabel
 * has "skip" and "active" states, meaning "this phase is (not) skipped" and
 * "this is the current phase".
 */
@SuppressWarnings("serial")
public class PhaseLabel extends JLabel {
    private boolean enabled = true;
    private boolean active = false;
    private boolean hover = false;


    /**
     * Shows phase labels, handles repainting and on/off states. A
     * PhaseLabel has "skip" and "active" states, meaning
     * "this phase is (not) skipped" and "this is the current phase".
     * 
     * @param txt
     *            &emsp; Label text
     */
    public PhaseLabel(final String txt) {
        super(txt);
        this.setHorizontalTextPosition(SwingConstants.CENTER);
        this.setHorizontalAlignment(SwingConstants.CENTER);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (PhaseLabel.this.enabled) {
                    PhaseLabel.this.enabled = false;
                } else {
                    PhaseLabel.this.enabled = true;
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

    /**
     * Determines whether play pauses at this phase or not.
     * 
     * @param b
     *            &emsp; boolean, true if play pauses
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

    /**
     * Makes this phase the current phase (or not).
     * 
     * @param b
     *            &emsp; boolean, true if phase is current
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

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    public void paintComponent(final Graphics g) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        FSkin.SkinColor c;

        // Set color according to skip or active or hover state of label
        if (this.hover) {
            c = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        }
        else if (this.active && this.enabled) {
            c = FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_ENABLED);
        }
        else if (!this.active && this.enabled) {
            c = FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_ENABLED);
        }
        else if (this.active && !this.enabled) {
            c = FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_DISABLED);
        }
        else {
            c = FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_DISABLED);
        }

        // Center vertically and horizontally. Show border if active.
        FSkin.setGraphicsColor(g, c);
        g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
        super.paintComponent(g);
    }
}