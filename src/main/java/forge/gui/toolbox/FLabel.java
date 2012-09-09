package forge.gui.toolbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import forge.Command;
import forge.gui.framework.ILocalRepaint;

/** 
 * Uses the Builder pattern to facilitate/encourage inline styling.
 * Credit to Effective Java 2 (Joshua Bloch).
 * Methods in builder can be chained. To declare:
 * <code>new FLabel.Builder().method1(foo).method2(bar).method3(baz)...</code>
 * <br>and then call build() to make the label (don't forget that part).
 * <br><br>
 * Adjustable features of FLabel:<br>
 * - Automatic font scaling (60% size by default, can toggle on/off)<br>
 * - Automatic icon scaling (80% size by default, can toggle on/off)<br>
 * - Scale font according to height or width<br>
 * - Hoverable<br>
 * - Selectable<br>
 * - Can execute command when clicked
 */
@SuppressWarnings("serial")
public class FLabel extends JLabel implements ILocalRepaint {
    /** 
     * Uses the Builder pattern to facilitate/encourage inline styling.
     * Credit to Effective Java 2 (Joshua Bloch).
     * Methods in builder can be chained. To declare:
     * <code>new FLabel.Builder().method1(foo).method2(bar).method3(baz)...</code>
     * <br>and then call build() to make the label (don't forget that part).
    */
    public static class Builder extends FLabel {
        //========== Default values for FLabel are set here.
        private double  bldIconScaleFactor  = 0.8;
        private int     bldFontStyle        = Font.PLAIN;
        private int     bldFontSize         = 14;
        private float   bldIconAlpha        = 1.0f;
        private int     bldIconAlignX       = SwingConstants.LEFT;
        private Point   bldIconInsets       = new Point(0, 0);

        private boolean bldSelectable       = false;
        private boolean bldHoverable        = false;
        private boolean bldOpaque           = false;
        private boolean bldIconInBackground = false;
        private boolean bldIconScaleAuto    = true;

        private String  bldText, bldToolTip;
        private ImageIcon bldIcon;
        private int bldFontAlign;
        private Command bldCmd;

        // Build!
        /** @return {@link forge.gui.toolbox.FLabel} */
        public FLabel build() { return new FLabel(this); }

        // Begin builder methods.
        /**@param s0 &emsp; {@link java.lang.String}
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder text(final String s0) { this.bldText = s0; return this; }

        /**@param s0 &emsp; {@link java.lang.String}
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder tooltip(final String s0) { this.bldToolTip = s0; return this; }

        /**@param i0 &emsp; {@link javax.swing.ImageIcon}
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder icon(final ImageIcon i0) { this.bldIcon = i0; return this; }

        /**@param i0 &emsp; SwingConstants.CENTER, .LEFT, or .RIGHT
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder fontAlign(final int i0) { this.bldFontAlign = i0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder opaque(final boolean b0) { this.bldOpaque = b0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder hoverable(final boolean b0) { this.bldHoverable = b0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder selectable(final boolean b0) { this.bldSelectable = b0; return this; }

        /**@param c0 &emsp; {@link forge.Command} to execute if clicked
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder cmdClick(final Command c0) { this.bldCmd = c0; return this; }

        /**@param i0 &emsp; int
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder fontSize(final int i0) { this.bldFontSize = i0; return this; }

        /**@param i0 &emsp; Font.PLAIN, Font.BOLD, or Font.ITALIC
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder fontStyle(final int i0) { this.bldFontStyle = i0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconScaleAuto(final boolean b0) { this.bldIconScaleAuto = b0; return this; }

        /**@param d0 &emsp; double between 0 and 1, 0.8 by default
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconScaleFactor(final double d0) { this.bldIconScaleFactor = d0; return this; }

        /**@param b0 &emsp; boolean, icon will be drawn independent of text
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconInBackground(final boolean b0) { this.bldIconInBackground = b0; return this; }

        /**@param f0 &emsp; 0.0f - 1.0f. If icon is in background, this alpha is applied.
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconAlpha(final float f0) { this.bldIconAlpha = f0; return this; }

        /**@param i0 &emsp; Int. Only available for background icon.
         * SwingConstants.HORIZONTAL .VERTICAL or .CENTER
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconAlignX(final int i0) { this.bldIconAlignX = i0; return this; }

        /**@param i0 &emsp; Point. Only available for background icon.
         * Additional padding to top left corner of icon, after alignX.
         * @return {@link forge.gui.toolbox.Builder} */
        public Builder iconInsets(final Point i0) { this.bldIconInsets = i0; return this; }
    }

    //========== Constructors
    /** Must have protected constructor to allow Builder to subclass. */
    protected FLabel() { }

    // Call this using FLabel.Builder()...
    private FLabel(final Builder b0) {
        super(b0.bldText);

        // Init fields from builder
        this.iconScaleFactor = b0.bldIconScaleFactor;

        this.opaque = b0.bldOpaque;
        this.iconInBackground = b0.bldIconInBackground;
        this.iconScaleAuto = b0.bldIconScaleAuto;
        this.selectable = b0.bldSelectable;
        this.iconAlignX = b0.bldIconAlignX;
        this.iconInsets = b0.bldIconInsets;

        this.setFontStyle(b0.bldFontStyle);
        this.setFontSize(b0.bldFontSize);
        this.setIconAlpha(b0.bldIconAlpha);
        this.setCommand(b0.bldCmd);
        this.setFontAlign(b0.bldFontAlign);
        this.setText(b0.bldText);
        this.setToolTipText(b0.bldToolTip);
        this.setHoverable(b0.bldHoverable);

        // Call this last; to allow the properties which affect icons to already be in place.
        this.setIcon(b0.bldIcon);

        // Non-custom display properties
        this.setForeground(clrText);
        this.setBackground(clrInactive);
        this.setVerticalTextPosition(SwingConstants.CENTER);
        this.setVerticalAlignment(SwingConstants.CENTER);

        // Resize adapter
        this.removeComponentListener(cadResize);
        this.addComponentListener(cadResize);
    }

    //========== Variable initialization
    // Final inits
    private final Color clrText = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private final Color clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
    private final Color clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
    private final Color clrInactive = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
    private final Color clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);

    // Custom properties, assigned either at realization (using builder)
    // or dynamically (using methods below).
    private double iconScaleFactor;
    private int fontStyle, iconAlignX;
    private int iw, ih;
    private boolean selectable, selected, hoverable, hovered, opaque,
        iconInBackground, iconScaleAuto;
    private Point iconInsets;

    // Various variables used in image rendering.
    private Image img;
    
    private Command cmdClick;

    private double iar;

    private AlphaComposite alphaDim, alphaStrong;

    private final ActionListener fireResize = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent evt) { resize(); resizeTimer.stop(); }
    };

    private final Timer resizeTimer = new Timer(10, fireResize);

    // Resize adapter; on a timer to prevent resizing while "sliding" between sizes
    private final ComponentAdapter cadResize = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) { resizeTimer.restart(); }
    };

    // Mouse event handler
    private final MouseAdapter madEvents = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent e) {
            hovered = true; repaintSelf();
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            hovered = false; repaintSelf();
        }
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (cmdClick != null && FLabel.this.isEnabled()) { cmdClick.execute(); }
            if (!selectable) { return; }
            if (selected) { setSelected(false); }
            else { setSelected(true); }
        }
    };

    //========== Methods
    /** @param b0 &emsp; boolean */
    // Must be public.
    public void setHoverable(final boolean b0) {
        this.hoverable = b0;
        if (!b0) { this.removeMouseListener(madEvents); }
        else { this.addMouseListener(madEvents); }
    }

    /** @param b0 &emsp; boolean */
    // Must be public.
    public void setSelected(final boolean b0) {
        this.selected = b0;
        repaintSelf();
    }

    /** Sets alpha if icon is in background.
     * @param f0 &emsp; float */
    // NOT public; must be set when label is built.
    private void setIconAlpha(final float f0) {
        this.alphaDim = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f0);
        this.alphaStrong = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    }

    private void setFontSize(final int i0) {
        switch(this.fontStyle) {
            case Font.BOLD: this.setFont(FSkin.getBoldFont(i0)); break;
            case Font.ITALIC: this.setFont(FSkin.getItalicFont(i0)); break;
            default: this.setFont(FSkin.getFont(i0));
        }
    }

    /** @param i0 &emsp; Font.PLAIN, .BOLD, or .ITALIC */
    // NOT public; must be set when label is built.
    private void setFontStyle(final int i0) {
        if (i0 != Font.PLAIN && i0 != Font.BOLD && i0 != Font.ITALIC) {
            throw new IllegalArgumentException("FLabel$setFontStyle "
                    + "must be passed either Font.PLAIN, Font.BOLD, or Font.ITALIC.");
        }
        this.fontStyle = i0;
    }

    /** @param i0 &emsp; SwingConstants.CENTER, .LEFT or .RIGHT */
    // NOT public; must be set when label is built.
    private void setFontAlign(final int i0) {
        if (i0 != SwingConstants.CENTER && i0 != SwingConstants.LEFT && i0 != SwingConstants.RIGHT) {
            throw new IllegalArgumentException("FLabel$setFontStyle "
                    + "must be passed either SwingConstants.CENTER, "
                    + "SwingConstants.LEFT, or SwingConstants.RIGHT");
        }
        this.setHorizontalAlignment(i0);
    }

    /** Resizing in MigLayout "slides" between the original and destination sizes.
     * To prevent this label from recalculating on each increment, a timer
     * is run to check that the the "sliding" is finished.  To resize this label
     * explicitly, retrieve this timer and start it.  It will stop automatically.
     * 
     * @return {@link javax.swing.Timer}
     */
    public Timer getResizeTimer() {
        return this.resizeTimer;
    }

    /** @return {@link forge.Command} */
    public Command getCommand() {
        return this.cmdClick;
    }

    /** @return boolean */
    public boolean isSelected() {
        return selected;
    }

    @Override
    // Must be public.
    public void setIcon(final Icon i0) {
        if (i0 == null) { this.img = null; return; }
        // Will need image (not icon) for scaled and non-scaled.
        if (iconInBackground) { this.img = ((ImageIcon) i0).getImage(); }
        // Will need image if not in background, but scaled.
        else if (iconScaleAuto) { this.img = ((ImageIcon) i0).getImage(); }
        // If not in background, not scaled, can use original icon.
        else { super.setIcon(i0); }

        if (img != null) {
            iw = img.getWidth(null);
            ih = img.getHeight(null);
            iar = ((double) iw) / ((double) ih);
        }
    }

    /** @param c0 &emsp; {@link forge.Command} on click */
    public void setCommand(final Command c0) {
        this.cmdClick = c0;
    }

    @Override
    public void setOpaque(final boolean b0) {
        // Must be overridden to allow drawing order of background, icon, string
        this.opaque = b0;
        super.setOpaque(false);
    }

    /** Major performance kicker - won't repaint whole screen! */
    @Override
    public void repaintSelf() {
        final Dimension d = FLabel.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

//    private int cntRepaints = 0;
    @Override
    public void paintComponent(final Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
       
        int w = getWidth();
        int h = getHeight();

//        cntRepaints++;
//        System.err.format("[%d] @ %d - Repaint: %s%n", new Date().getTime(), cntRepaints, this.getText());
//        if ( cntRepaints > 200 ) {
//            final Throwable ex = new Exception("Too many repaints");
//            ex.printStackTrace(System.err);
//        }
//        
        // Hover
        if (hoverable && hovered && !selected && isEnabled()) {
            g2d.setColor(clrHover);
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(clrBorders);
            g2d.drawRect(0, 0, w - 1, h - 1);
        } else if (this.opaque && !selected) {  // Opacity, select
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, w, h);
        } else if (selectable && selected) {
            g2d.setColor(clrActive);
            g2d.fillRect(0, 0, w, h);
        }

        // Icon in background
        if (iconInBackground) {
            int sh = (int) (h * iconScaleFactor);
            int sw = (int) (sh * iar);

            int x = iconAlignX == SwingConstants.CENTER ? (int) ((w - sw) / 2 + iconInsets.getX()): (int) iconInsets.getX();
            int y = (int) (((h - sh) / 2) + iconInsets.getY());

            Composite oldComp = g2d.getComposite();
            g2d.setComposite(hoverable && hovered && !selected ? alphaStrong : alphaDim);
            g2d.drawImage(img, x, y, sw + x, sh + y, 0, 0, iw, ih, null);
            g2d.setComposite(oldComp);
        }

        super.paintComponent(g);
    }

    private void resize() {
        // Non-background icon
        if (img != null && iconScaleAuto  && !iconInBackground) {
            int h = (int) (getHeight() * iconScaleFactor);
            int w = (int) (h * iar);
            if (w == 0 || h == 0) { return; }

            FLabel.super.setIcon(new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
        }
    }
}
