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
package forge.view.toolbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import forge.AllZone;

/**
 * <p>
 * FRoundedPanel.
 * </p>
 * A subclass of JPanel with any of four corners rounded, drop shadow, and 1px
 * line border.
 * 
 * Limitations: Cannot tile background image, cannot set border width.
 * 
 */
@SuppressWarnings("serial")
public class FRoundedPanel extends JPanel {
    private boolean[] borders = { true, true, true, true };
    private boolean[] corners = { true, true, true, true }; // NW, SW, SE, NE
    private Color shadowColor = new Color(150, 150, 150, 150);
    private Color borderColor = AllZone.getSkin().getClrBorders();
    private int shadowOffset = 5;
    private int cornerRadius = 10;
    private boolean showShadow = false;

    /**
     * <p>
     * FRoundedPanel.
     * </p>
     * 
     * Constructor, null layout manager.
     */
    public FRoundedPanel() {
        super();
        this.setOpaque(false);
    }

    /**
     * <p>
     * FRoundedPanel.
     * </p>
     * 
     * Constructor.
     * 
     * @param lm
     *            the lm
     */
    public FRoundedPanel(final LayoutManager lm) {
        this();
        this.setLayout(lm);
    }

    /**
     * paintComponent is the guts of FRoundedPanel. It paints the borders and
     * rounded corners determined by the conditional arrays <b>borders[ ]</b>
     * and <b>corners[ ]</b>.
     * 
     * @param g
     *            &emsp; Graphics obj
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final int w = this.getWidth();
        final int h = this.getHeight();
        int so = this.shadowOffset;
        final int r = this.cornerRadius;

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (this.showShadow) {
            // Mid, left, right rectangles: shadow
            g2d.setColor(this.shadowColor);
            g2d.fillRect(r + so, so, w - (2 * r) - so, h - so);
            g2d.fillRect(so, r + so, r, h - (2 * r) - so);
            g2d.fillRect(w - r, r + so, r, h - (2 * r) - so);

            // Corners: shadow
            // NW
            if (this.corners[0]) {
                g2d.fillArc(so, so, 2 * r, 2 * r, 90, 90);
            } else {
                g2d.fillRect(so, so, r, r);
            }
            // SW
            if (this.corners[1]) {
                g2d.fillArc(so, h - (2 * r), 2 * r, 2 * r, 180, 90);
            } else {
                g2d.fillRect(so, h - r, r, r);
            }
            // SE
            if (this.corners[2]) {
                g2d.fillArc(w - (2 * r), h - (2 * r), 2 * r, 2 * r, 270, 90);
            } else {
                g2d.fillRect(w - r, h - r, r, r);
            }
            // NE
            if (this.corners[3]) {
                g2d.fillArc(w - (2 * r), so, 2 * r, 2 * r, 0, 90);
            } else {
                g2d.fillRect(w - r, so, r, r);
            }
        } // End if(showShadow)
        else {
            so = 0;
            so = 0;
        }

        // Mid, left, right rectangles: content
        g2d.setColor(this.getBackground());
        g2d.fillRect(r, 0, w - (2 * r) - so, h - so);
        g2d.fillRect(0, r, r, h - (2 * r) - so);
        g2d.fillRect(w - r - so, r, r, h - (2 * r) - so);

        // Corners: content
        // NW
        if (this.corners[0]) {
            g2d.fillArc(0, 0, 2 * r, 2 * r, 90, 90);
        } else {
            g2d.fillRect(0, 0, r, r);
        }
        // SW
        if (this.corners[1]) {
            g2d.fillArc(0, h - (2 * r) - so, 2 * r, 2 * r, 180, 90);
        } else {
            g2d.fillRect(0, h - r - so, r, r);
        }
        // SE
        if (this.corners[2]) {
            g2d.fillArc(w - (2 * r) - so, h - (2 * r) - so, 2 * r, 2 * r, 270, 90);
        } else {
            g2d.fillRect(w - r - so, h - r - so, r, r);
        }
        // NE
        if (this.corners[3]) {
            g2d.fillArc(w - (2 * r) - so, 0, 2 * r, 2 * r, 0, 90);
        } else {
            g2d.fillRect(w - r - so, 0, r, r);
        }

        // Mid, left, right rectangles: border
        g2d.setColor(this.borderColor);

        if (this.borders[0]) {
            g2d.drawLine(r, 0, w - r - so, 0);
        }
        if (this.borders[1]) {
            g2d.drawLine(0, r, 0, h - r - so);
        }
        if (this.borders[2]) {
            g2d.drawLine(r, h - so - 1, w - r - so, h - so - 1);
        }
        if (this.borders[3]) {
            g2d.drawLine(w - so - 1, r, w - so - 1, h - r - so);
        }

        // Corners: border
        // NW
        if (this.corners[0]) {
            g2d.drawArc(0, 0, 2 * r, 2 * r, 90, 90);
        } else {
            if (this.borders[0]) {
                g2d.drawLine(0, 0, r, 0);
            }
            if (this.borders[1]) {
                g2d.drawLine(0, 0, 0, r);
            }
        }
        // SW
        if (this.corners[1]) {
            g2d.drawArc(0, h - (2 * r) - so, 2 * r, (2 * r) - 1, 180, 90);
        } else {
            if (this.borders[1]) {
                g2d.drawLine(0, h - so - 1, 0, h - r - so - 1);
            }
            if (this.borders[2]) {
                g2d.drawLine(0, h - so - 1, r, h - so - 1);
            }
        }
        // SE
        if (this.corners[2]) {
            g2d.drawArc(w - (2 * r) - so, h - (2 * r) - so, (2 * r) - 1, (2 * r) - 1, 270, 90);
        } else {
            if (this.borders[2]) {
                g2d.drawLine(w - so - 1, h - so - 1, w - r - so, h - so - 1);
            }
            if (this.borders[3]) {
                g2d.drawLine(w - so - 1, h - so - 1, w - so - 1, h - r - so);
            }
        }
        // NE
        if (this.corners[3]) {
            g2d.drawArc(w - (2 * r) - so, 0, (2 * r) - 1, (2 * r) - 1, 0, 90);
        } else {
            if (this.borders[0]) {
                g2d.drawLine(w - so - 1, 0, w - so - r, 0);
            }
            if (this.borders[3]) {
                g2d.drawLine(w - so - 1, 0, w - so - 1, r);
            }
        }
    }

    /**
     * <p>
     * setShadowColor.
     * </p>
     * Sets color of shadow behind rounded panel.
     * 
     * @param c
     *            the new shadow color
     */
    public void setShadowColor(final Color c) {
        this.shadowColor = c;
    }

    /**
     * <p>
     * setBorderColor.
     * </p>
     * Sets color of border around rounded panel.
     * 
     * @param c
     *            the new border color
     */
    public void setBorderColor(final Color c) {
        this.borderColor = c;
    }

    /**
     * <p>
     * setShadowOffset.
     * </p>
     * Sets offset of shadow from rounded panel.
     * 
     * @param i
     *            the new shadow offset
     */
    public void setShadowOffset(int i) {
        if (i < 0) {
            i = 0;
        }
        this.shadowOffset = i;
    }

    /**
     * <p>
     * setCornerRadius.
     * </p>
     * Sets radius of each corner on rounded panel.
     * 
     * @param r0
     *            &emsp; int
     */
    public void setCornerRadius(int r0) {
        if (r0 < 0) {
            r0 = 0;
        }

        this.cornerRadius = r0;
    }

    /**
     * <p>
     * setCorners.
     * </p>
     * Sets if corners should be rounded or not in the following order: NW, SW,
     * SE, NE
     * 
     * @param vals
     *            &emsp; boolean[4]
     */
    public void setCorners(final boolean[] vals) {
        if (vals.length != 4) {
            throw new IllegalArgumentException("FRoundedPanel > setCorners requires an array of booleans of length 4.");
        }

        this.corners = vals;
    }

    /**
     * <p>
     * setBorders.
     * </p>
     * Sets if borders should be displayed or not following order: N, W, S, E.
     * Only works for square corners, rounded corners will be shown with
     * borders.
     * 
     * @param vals
     *            &emsp; boolean[4]
     */
    public void setBorders(final boolean[] vals) {
        if (vals.length != 4) {
            throw new IllegalArgumentException("FRoundedPanel > setBorders requires an array of booleans of length 4.");
        }

        this.borders = vals;
    }

    /**
     * Sets the show shadow.
     * 
     * @param b
     *            the new show shadow
     */
    public void setShowShadow(final boolean b) {
        this.showShadow = b;
    }
}
