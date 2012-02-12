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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.border.Border;

import forge.Command;

/** 
 * Core panel used in UI.
 * <br><br>
 * Adjustable features of FPanel:<br>
 * - Hoverable<br>
 * - Selectable<br>
 * - Tiled background texture<br>
 * - Foreground picture, option for stretch to fit<br>
 * - Rounded corners<br>
 * - Border toggle<br>
 */
@SuppressWarnings("serial")
public class FPanel extends JPanel {
    //========== Variable initialization
    // Defaults for adjustable values
    private boolean selectable          = false;
    private boolean hoverable           = false;
    private boolean foregroundStretch   = false;
    private Image   foregroundImage     = null;
    private Image   backgroundTexture   = null;
    private Color   borderColor         = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
    private boolean borderToggle        = true;
    private int     cornerDiameter      = 20;

    // Mouse handling
    private boolean selected, hovered;
    private Command cmdClick;
    // Width/height of panel, bg img, scaled bg img, texture img. Coords.
    private int pw, ph, iw, ih, sw, sh, tw, th, x, y;
    // Image aspect ratio (width / height)
    private double iar;
    // Graphics clone to avoid clobbering original
    private Graphics2D g2d;
    // Clip rounded corner shape
    private Area clip;
    private BasicStroke borderStroke = new BasicStroke(2.0f);

    /** Core panel used in UI. See class javadoc for more details. */
    public FPanel() {
        super();

        // Opacity must be removed for proper rounded corner drawing
        this.setOpaque(false);

        // Background will follow skin theme.
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
    }

    // Mouse event handler
    private final MouseAdapter madEvents = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
        @Override
        public void mouseExited(MouseEvent e) { hovered = false; repaint(); }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (cmdClick != null) { cmdClick.execute(); }
            if (!selectable) { return; }

            if (selected) { setSelected(false); }
            else { setSelected(true); }
        }
    };

    //========== Mutators
    /**@param i0 &emsp; int */
    public void setCornerDiameter(int i0) {
        if (i0 < 0) { i0 = 0; }
        this.cornerDiameter = i0;
    }

    /** @param c0 &emsp; {@link forge.Command} on click */
    public void setCommand(Command c0) {
        this.cmdClick = c0;
    }

    /** @return {@link forge.Command} */
    public Command getCommand() {
        return this.cmdClick;
    }

    /** @param b0 &emsp; boolean */
    public void setHoverable(boolean b0) {
        hoverable = b0;
        this.confirmDrawEfficiency();
        if (!b0) { this.removeMouseListener(madEvents); }
        else { this.addMouseListener(madEvents); }
    }

    /** @param b0 &emsp; boolean */
    public void setSelectable(boolean b0) {
        this.selectable = b0;
        this.confirmDrawEfficiency();
    }

    /** @param b0 &emsp; boolean */
    public void setSelected(boolean b0) {
        selected = b0;
        if (b0) { this.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE)); }
        else    { this.setBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE)); }
        repaint();
    }

    /** @param i0 &emsp; {@link java.awt.Image} */
    public void setForegroundImage(Image i0) {
        if (i0 == null) { return; }

        this.foregroundImage = i0;
        this.iw = i0.getWidth(null);
        this.ih = i0.getHeight(null);
        this.iar = (double) iw / (double) ih;
    }

    /** @param i0 &emsp; {@link javax.swing.ImageIcon} */
    public void setForegroundImage(ImageIcon i0) {
        setForegroundImage(i0.getImage());
    }

    /** @param b0 &emsp; boolean, stretch the foreground to fit */
    public void setForegroundStretch(final boolean b0) {
        this.foregroundStretch = b0;
    }

    /** @param i0 &emsp; {@link java.awt.Image} */
    public void setBackgroundTexture(Image i0) {
        if (i0 == null) { return; }

        this.backgroundTexture = i0;
        this.tw = i0.getWidth(null);
        this.th = i0.getHeight(null);
    }

    /** @param i0 &emsp; {@link javax.swing.ImageIcon} */
    public void setBackgroundTexture(ImageIcon i0) {
        setBackgroundTexture(i0.getImage());
    }

    /** @param b0 &emsp; boolean */
    public void setBorderToggle(final boolean b0) {
        this.borderToggle = b0;
    }

    /** @param c0 &emsp; {@link java.awt.Color} */
    public void setBorderColor(final Color c0) {
        this.borderColor = c0;
    }

    @Override
    public void setBorder(Border b0) { }

    @Override
    public void setOpaque(boolean b0) { super.setOpaque(false); }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        pw = this.getWidth();
        ph = this.getHeight();
        g2d = (Graphics2D) g.create();

        // Set clip for rounded area
        if (cornerDiameter > 0) {
            clip = new Area(new RoundRectangle2D.Float(0, 0, pw, ph, cornerDiameter, cornerDiameter));
            g2d.setClip(clip);
        }

        // Draw background as required
        if (foregroundStretch && foregroundImage != null) {
            drawForegroundStretched(g2d);
        }
        else if (this.backgroundTexture != null) {
            drawBackgroundTexture(g2d);
        }
        else {
            drawBackgroundColor(g2d);
        }

        // Draw foreground as required
        if (!foregroundStretch && foregroundImage != null) {
            drawForegroundScaled(g2d);
        }

        if (borderToggle) {
            drawBorder(g2d);
        }

        // Clear memory
        if (clip != null) { clip.reset(); }
        g2d.dispose();
    }

    //========== Special draw methods
    private void drawBackgroundColor(final Graphics g0) {
        // Color background as appropriate
        if (selected)           { g0.setColor(FSkin.getColor(FSkin.Colors.CLR_ACTIVE)); }
        else if (hovered)       { g0.setColor(FSkin.getColor(FSkin.Colors.CLR_HOVER)); }
        else if (selectable)    { g0.setColor(FSkin.getColor(FSkin.Colors.CLR_INACTIVE)); }
        else                    { g0.setColor(getBackground()); }

        // Parent must be drawn onto clipped object.
        g0.fillRoundRect(0, 0, pw, ph, cornerDiameter, cornerDiameter);
    }

    private void drawBackgroundTexture(final Graphics g0) {
        this.x = 0;
        this.y = 0;

        while (this.x < this.pw) {
            while (this.y < this.ph) {
                g0.drawImage(this.backgroundTexture, (int) this.x, (int) this.y, null);
                this.y += this.th;
            }
            this.x += this.tw;
            this.y = 0;
        }
        this.x = 0;
    }

    private void drawForegroundScaled(final Graphics g0) {
        // Scaling 1: First dimension larger than panel
        if (iw >= pw) { // Image is wider than panel? Shrink to width.
            sw = pw;
            sh = (int) (sw / iar);
        }
        else if (ih >= ph) { // Image is taller than panel? Shrink to height.
            sh = ph;
            sw = (int) (sh * iar);
        }
        else {  // Image is smaller than panel? No scaling.
            sw = iw;
            sh = ih;
        }

        // Scaling step 2: Second dimension larger than panel
        if (sh > ph) { // Scaled image still taller than panel?
            sh = ph;
            sw = (int) (sh * iar);
        }
        else if (sw > pw) { // Scaled image still wider than panel?
            sw = pw;
            sh = (int) (sw / iar);
        }

        // Scaling step 3: Center image in panel
        x = (int) ((pw - sw) / 2);
        y = (int) ((ph - sh) / 2);
        g0.drawImage(foregroundImage, x, y, sw + x, sh + y, 0, 0, iw, ih, null);
    }

    private void drawForegroundStretched(final Graphics g0) {
        g0.drawImage(foregroundImage, 0, 0, pw, ph, 0, 0, iw, ih, null);
    }

    private void drawBorder(final Graphics2D g0) {
        g0.setColor(borderColor);
        g0.setStroke(borderStroke);
        g0.drawRoundRect(0, 0, pw, ph, cornerDiameter, cornerDiameter);
    }

    /** Improves performance by checking to see if any graphics will cancel
     * each other out, so unnecessary out-of-sight graphics will not stack up. */
    private void confirmDrawEfficiency() {
        if (hoverable && foregroundStretch) {
            throw new IllegalArgumentException("\nAn FPanel may not be simultaneously "
                    + "hoverable and have a stretched foreground image.\n"
                    + "Please adjust the panel's declaration to use one or the other.");
        }

        if (hoverable && backgroundTexture != null) {
            throw new IllegalArgumentException("\nAn FPanel may not be simultaneously "
                    + "hoverable and have a background texture.\n"
                    + "Please adjust the panel's declaration to use one or the other.");
        }

        if (selectable && foregroundStretch) {
            throw new IllegalArgumentException("\nAn FPanel may not be simultaneously "
                    + "selectable and have a stretched foreground image.\n"
                    + "Please adjust the panel's declaration to use one or the other.");
        }

        if (selectable && backgroundTexture != null) {
            throw new IllegalArgumentException("\nAn FPanel may not be simultaneously "
                    + "selectable and have a background texture.\n"
                    + "Please adjust the panel's declaration to use one or the other.");
        }
    }
}
