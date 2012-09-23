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
package forge.gui.toolbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.Command;
import forge.gui.framework.ILocalRepaint;

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
public class FPanel extends JPanel implements ILocalRepaint {
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
    private int     foregroundAlign     = SwingConstants.CENTER;

    // Mouse handling
    private boolean selected, hovered;
    private Command cmdClick;
    // Width/height of panel, bg img, scaled bg img, texture img. Coords.
    private int pnlW, pnlH, imgW, imgH, scaledW, scaledH, textureW, textureH, tempX, tempY;
    // Image aspect ratio (width / height)
    private double iar;

    /** */
    public FPanel() {
        this(new FlowLayout());
    }

    /** Core panel used in UI. See class javadoc for more details.
     * @param lm0 &emsp; {@link java.awt.LayoutManager}
     */
    public FPanel(final LayoutManager lm0) {
        super(lm0);

        // Opacity must be removed for proper rounded corner drawing
        this.setOpaque(false);

        // Background will follow skin theme.
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
    }

    // Mouse event handler
    private final MouseAdapter madEvents = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) { hovered = true; repaintSelf(); }
        @Override
        public void mouseExited(final MouseEvent evt) { hovered = false; repaintSelf(); }

        @Override
        public void mouseClicked(final MouseEvent evt) {
            if (cmdClick != null) { cmdClick.execute(); }
            if (!selectable) { return; }

            if (selected) { setSelected(false); }
            else { setSelected(true); }
        }
    };

    //========== Mutators
    /**@param int0 &emsp; int */
    public void setCornerDiameter(final int int0) {
        this.cornerDiameter = (int0 <= 0 ? 0 : int0);
    }

    /** @param cmd0 &emsp; {@link forge.Command} on click */
    public void setCommand(final Command cmd0) {
        this.cmdClick = cmd0;
    }

    /** @return {@link forge.Command} */
    public Command getCommand() {
        return this.cmdClick;
    }

    /** @param bool0 &emsp; boolean */
    public void setHoverable(final boolean bool0) {
        hoverable = bool0;
        this.confirmDrawEfficiency();
        if (bool0) { this.addMouseListener(madEvents); }
        else { this.removeMouseListener(madEvents); }
    }

    /** @param bool0 &emsp; boolean */
    public void setSelectable(final boolean bool0) {
        this.selectable = bool0;
        this.confirmDrawEfficiency();
    }

    /** @param bool0 &emsp; boolean */
    public void setSelected(final boolean bool0) {
        selected = bool0;
        if (bool0) { this.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE)); }
        else    { this.setBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE)); }
        repaintSelf();
    }

    /** @param img0 &emsp; {@link java.awt.Image} */
    public void setForegroundImage(final Image img0) {
        if (img0 == null) {
            this.foregroundImage = null;
            return;
        }

        this.foregroundImage = img0;
        this.imgW = img0.getWidth(null);
        this.imgH = img0.getHeight(null);
        this.iar = (double) imgW / (double) imgH;
    }

    /** @param ii0 &emsp; {@link javax.swing.ImageIcon} */
    public void setForegroundImage(final ImageIcon ii0) {
        setForegroundImage(ii0.getImage());
    }

    /** Aligns NON-STRETCHED foreground image.
     * Must use SwingConstants.
     * @param i0 &emsp; int
     */
    public void setForegroundAlign(final int i0) {
        // Only implemented for BOTTOM at present.
        // More implementations can be added as necessary.
        // See drawForegroundScaled().

        final List<Integer> implemented = new ArrayList<Integer>();
        implemented.add(SwingConstants.BOTTOM);
        implemented.add(SwingConstants.CENTER);

        if (!implemented.contains(i0)) { throw new IllegalArgumentException(); }
        this.foregroundAlign = i0;
        implemented.clear();
    }

    /** @param bool0 &emsp; boolean, stretch the foreground to fit */
    public void setForegroundStretch(final boolean bool0) {
        this.foregroundStretch = bool0;
    }

    /** @param img0 &emsp; {@link java.awt.Image} */
    public void setBackgroundTexture(final Image img0) {
        if (img0 == null) { return; }

        this.backgroundTexture = img0;
        this.textureW = img0.getWidth(null);
        this.textureH = img0.getHeight(null);
    }

    /** @param ii0 &emsp; {@link javax.swing.ImageIcon} */
    public void setBackgroundTexture(final ImageIcon ii0) {
        setBackgroundTexture(ii0.getImage());
    }

    /** @param bool0 &emsp; boolean */
    public void setBorderToggle(final boolean bool0) {
        this.borderToggle = bool0;
    }

    /** @param clr0 &emsp; {@link java.awt.Color} */
    public void setBorderColor(final Color clr0) {
        this.borderColor = clr0;
    }

    /** @param b0 &emsp; boolean */
    public void setHovered(final boolean b0) {
        this.hovered = b0;
    }

    @Override
    public void repaintSelf() {
        final Dimension d = FPanel.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    public void paintComponent(final Graphics graphics0) {
        super.paintComponent(graphics0);

        pnlW = this.getWidth();
        pnlH = this.getHeight();
        final Graphics2D g2d = (Graphics2D) graphics0.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background as required
        if (foregroundStretch && foregroundImage != null) {
            drawForegroundStretched(g2d);
        }
        else if (this.backgroundTexture == null) {
            drawBackgroundColor(g2d);
        }
        else {
            drawBackgroundTexture(g2d);
        }

        // Draw foreground as required
        if (!foregroundStretch && foregroundImage != null) {
            drawForegroundScaled(g2d);
        }

        if (borderToggle) {
            drawBorder(g2d);
        }

        // Clear memory
        g2d.dispose();
    }

    //========== Special draw methods
    private void drawBackgroundColor(final Graphics2D g2d0) {
        // Color background as appropriate
        if (selected)           { g2d0.setColor(FSkin.getColor(FSkin.Colors.CLR_ACTIVE)); }
        else if (hovered)       { g2d0.setColor(FSkin.getColor(FSkin.Colors.CLR_HOVER)); }
        else if (selectable)    { g2d0.setColor(FSkin.getColor(FSkin.Colors.CLR_INACTIVE)); }
        else                    { g2d0.setColor(getBackground()); }

        g2d0.fillRoundRect(0, 0, pnlW, pnlH, cornerDiameter, cornerDiameter);
    }

    private void drawBackgroundTexture(final Graphics2D g2d0) {
        this.tempX = 0;
        this.tempY = 0;

        while (this.tempX < this.pnlW) {
            while (this.tempY < this.pnlH) {
                g2d0.drawImage(this.backgroundTexture, this.tempX, this.tempY, null);
                this.tempY += this.textureH;
            }
            this.tempX += this.textureW;
            this.tempY = 0;
        }
        this.tempX = 0;
    }

    private void drawForegroundScaled(final Graphics2D g2d0) {
        // Scaling 1: First dimension larger than panel
        if (imgW >= pnlW) { // Image is wider than panel? Shrink to width.
            scaledW = pnlW;
            scaledH = (int) (scaledW / iar);
        }
        else if (imgH >= pnlH) { // Image is taller than panel? Shrink to height.
            scaledH = pnlH;
            scaledW = (int) (scaledH * iar);
        }
        else {  // Image is smaller than panel? No scaling.
            scaledW = imgW;
            scaledH = imgH;
        }

        // Scaling step 2: Second dimension larger than panel
        if (scaledH > pnlH) { // Scaled image still taller than panel?
            scaledH = pnlH;
            scaledW = (int) (scaledH * iar);
        }
        else if (scaledW > pnlW) { // Scaled image still wider than panel?
            scaledW = pnlW;
            scaledH = (int) (scaledW / iar);
        }

        // Scaling step 3: Center image in panel
        switch(this.foregroundAlign) {
            case SwingConstants.BOTTOM:
                tempX = ((pnlW - scaledW) / 2);
                tempY = pnlH - scaledH;
                break;
            default:
                tempX = ((pnlW - scaledW) / 2);
                tempY = ((pnlH - scaledH) / 2);
        }

        g2d0.drawImage(foregroundImage, tempX, tempY, scaledW + tempX, scaledH + tempY, 0, 0, imgW, imgH, null);
    }

    private void drawForegroundStretched(final Graphics2D g2d0) {
        g2d0.drawImage(foregroundImage, 0, 0, pnlW, pnlH, 0, 0, imgW, imgH, null);
    }

    private void drawBorder(final Graphics2D g2d0) {
        g2d0.setColor(borderColor);
        g2d0.drawRoundRect(0, 0, pnlW - 1, pnlH - 1, cornerDiameter, cornerDiameter);
    }

    /** Improves performance by checking to see if any graphics will cancel
     * each other out, so unnecessary out-of-sight graphics will not stack up. */
    private void confirmDrawEfficiency() {
        final String str = "\nAn FPanel may not be simultaneously "
                + "%s and have a %s.\nPlease adjust the panel's declaration to use one or the other.";

        if (hoverable && foregroundStretch) {
            throw new IllegalArgumentException(String.format(str, "hoverable", "stretched foreground image"));
        }

        if (hoverable && backgroundTexture != null) {
            throw new IllegalArgumentException(String.format(str, "hoverable", "background texture"));
        }

        if (selectable && foregroundStretch) {
            throw new IllegalArgumentException(String.format(str, "selectable", "stretched foreground image"));
        }

        if (selectable && backgroundTexture != null) {
            throw new IllegalArgumentException(String.format(str, "selectable", "background texture"));
        }
    }
}
