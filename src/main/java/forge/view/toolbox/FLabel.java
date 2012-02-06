package forge.view.toolbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import forge.Command;
import forge.Singletons;

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
public class FLabel extends JLabel {
    /** 
     * Uses the amazing Builder pattern to facilitate/encourage inline styling.
     * Credit to Effective Java 2 (Joshua Bloch), a fantastic book.
     * Methods in builder can be chained. To declare:
     * <code>new FLabel.Builder().method1(foo).method2(bar).method3(baz)...</code>
     * <br>and then call build() to make the label (don't forget that part).
    */
    public static class Builder extends FLabel {
        //========== Default values for FLabel are set here.
        private double  bldFontScaleFactor  = 0.6;
        private double  bldIconScaleFactor  = 0.8;
        private int     bldFontScaleBy      = SwingConstants.VERTICAL;
        private int     bldFontStyle        = Font.PLAIN;
        private float   bldIconAlpha        = 1.0f;

        private boolean bldSelectable       = false;
        private boolean bldHoverable        = false;
        private boolean bldOpaque           = false;
        private boolean bldIconInBackground = false;
        private boolean bldFontScaleAuto    = true;
        private boolean bldIconScaleAuto    = true;

        private String  bldText, bldToolTip;
        private ImageIcon bldIcon;
        private int bldFontAlign;
        private Command bldCmd;

        // Build!
        /** @return {@link forge.view.toolbox.FLabel} */
        public FLabel build() { return new FLabel(this); }

        // Begin builder methods.
        /**@param s0 &emsp; {@link java.lang.String}
         * @return {@link forge.view.toolbox.Builder} */
        public Builder text(String s0) { this.bldText = s0; return this; }

        /**@param s0 &emsp; {@link java.lang.String}
         * @return {@link forge.view.toolbox.Builder} */
        public Builder tooltip(String s0) { this.bldToolTip = s0; return this; }

        /**@param i0 &emsp; {@link javax.swing.ImageIcon}
         * @return {@link forge.view.toolbox.Builder} */
        public Builder icon(ImageIcon i0) { this.bldIcon = i0; return this; }

        /**@param i0 &emsp; SwingConstants.CENTER, .LEFT, or .RIGHT
         * @return {@link forge.view.toolbox.Builder} */
        public Builder fontAlign(int i0) { this.bldFontAlign = i0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.view.toolbox.Builder} */
        public Builder opaque(boolean b0) { this.bldOpaque = b0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.view.toolbox.Builder} */
        public Builder hoverable(boolean b0) { this.bldHoverable = b0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.view.toolbox.Builder} */
        public Builder selectable(boolean b0) { this.bldSelectable = b0; return this; }

        /**@param c0 &emsp; {@link forge.Command} to execute if clicked
         * @return {@link forge.view.toolbox.Builder} */
        public Builder cmdClick(Command c0) { this.bldCmd = c0; return this; }

        /**@param i0 &emsp; Font.PLAIN, Font.BOLD, or Font.ITALIC
         * @return {@link forge.view.toolbox.Builder} */
        public Builder fontStyle(int i0) { this.bldFontStyle = i0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.view.toolbox.Builder} */
        public Builder fontScaleAuto(boolean b0) { this.bldFontScaleAuto = b0; return this; }

        /**@param d0 &emsp; double between 0 and 1, 0.6 by default
         * @return {@link forge.view.toolbox.Builder} */
        public Builder fontScaleFactor(double d0) { this.bldFontScaleFactor = d0; return this; }

        /**@param i0 &emsp; SwingConstants.HORIZONTAL or .VERTICAL
         * @return {@link forge.view.toolbox.Builder} */
        public Builder fontScaleBy(int i0) { this.bldFontScaleBy = i0; return this; }

        /**@param b0 &emsp; boolean
         * @return {@link forge.view.toolbox.Builder} */
        public Builder iconScaleAuto(boolean b0) { this.bldIconScaleAuto = b0; return this; }

        /**@param d0 &emsp; double between 0 and 1, 0.8 by default
         * @return {@link forge.view.toolbox.Builder} */
        public Builder iconScaleFactor(double d0) { this.bldIconScaleFactor = d0; return this; }

        /**@param b0 &emsp; boolean, icon will be drawn independent of text
         * @return {@link forge.view.toolbox.Builder} */
        public Builder iconInBackground(boolean b0) { this.bldIconInBackground = b0; return this; }

        /**@param f0 &emsp; 0.0f - 1.0f. If icon is in background, this alpha is applied.
         * @return {@link forge.view.toolbox.Builder} */
        public Builder iconAlpha(float f0) { this.bldIconAlpha = f0; return this; }
    }

    //========== Constructors
    /** Must have protected constructor to allow subclassing. */
    protected FLabel() { }

    // Call this using FLabel.Builder()...
    private FLabel(Builder b0) {
        super(b0.bldText);

        // Init fields from builder
        this.fontScaleFactor = b0.bldFontScaleFactor;
        this.iconScaleFactor = b0.bldIconScaleFactor;

        this.opaque = b0.bldOpaque;
        this.iconInBackground = b0.bldIconInBackground;
        this.fontScaleAuto = b0.bldFontScaleAuto;
        this.iconScaleAuto = b0.bldIconScaleAuto;
        this.selectable = b0.bldSelectable;

        this.setFontScaleBy(b0.bldFontScaleBy);
        this.setFontStyle(b0.bldFontStyle);
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

    // Final inits
    private final Color clrText = Singletons.getView().getSkin().getColor(FSkin.Colors.CLR_TEXT);
    private final Color clrBorders = Singletons.getView().getSkin().getColor(FSkin.Colors.CLR_BORDERS);
    private final Color clrHover = Singletons.getView().getSkin().getColor(FSkin.Colors.CLR_HOVER);
    private final Color clrInactive = Singletons.getView().getSkin().getColor(FSkin.Colors.CLR_INACTIVE);
    private final Color clrActive = Singletons.getView().getSkin().getColor(FSkin.Colors.CLR_ACTIVE);

    // Custom properties, assigned either at realization (using builder)
    // or dynamically (using methods below).
    private double fontScaleFactor, iconScaleFactor;
    private int fontScaleBy, fontStyle;
    private boolean selectable, selected, hoverable, hovered, opaque,
        iconInBackground, fontScaleAuto, iconScaleAuto;

    // Various variables used in image rendering.
    private Image img;
    private Graphics2D g2d;
    private Command cmdClick;
    private int x, y, w, h, iw, ih, sw, sh, ref;
    private double iar;

    private AlphaComposite alphaDim, alphaStrong;

    // Resize adapter
    private final ComponentAdapter cadResize = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            if (fontScaleAuto) {
                ref = (fontScaleBy == SwingConstants.VERTICAL ? getHeight() : getWidth());
                switch (fontStyle) {
                    case Font.BOLD:
                        setFont(Singletons.getView().getSkin().getBoldFont((int) (ref * fontScaleFactor)));
                        repaint();
                        break;
                    case Font.ITALIC:
                        setFont(Singletons.getView().getSkin().getItalicFont((int) (ref * fontScaleFactor)));
                        break;
                    default:
                        setFont(Singletons.getView().getSkin().getFont((int) (ref * fontScaleFactor)));
                }
            }

            // Non-background icon
            if (img != null && iconScaleAuto  && !iconInBackground) {
                h = (int) (getHeight() * iconScaleFactor);
                w = (int) (h * iar * iconScaleFactor);
                if (w == 0 || h == 0) { return; }

                FLabel.super.setIcon(new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
            }
        }
    };

    // Mouse event handler
    private final MouseAdapter madEvents = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
        @Override
        public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        @Override
        public void mouseClicked(MouseEvent e) {
            cmdClick.execute();
            if (!selectable) { return; }
            if (selected) { setSelected(false); }
            else { setSelected(true); }
        }
    };

    /** @param b0 &emsp; boolean */
    // Must be public.
    public void setHoverable(boolean b0) {
        this.hoverable = b0;
        if (!b0) { this.removeMouseListener(madEvents); }
        else { this.addMouseListener(madEvents); }
    }

    /** @param b0 &emsp; boolean */
    // Must be public.
    public void setSelected(boolean b0) {
        this.selected = b0;
        repaint();
    }

    /** Sets alpha if icon is in background.
     * @param f0 &emsp; float */
    // NOT public; must be set when label is built.
    private void setIconAlpha(float f0) {
        this.alphaDim = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f0);
        this.alphaStrong = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    }

    /** @param i0 &emsp; int, must be SwingConstants.HORIZONTAL or VERTICAL */
    // NOT public; must be set when label is built.
    private void setFontScaleBy(int i0) {
        if (i0 != SwingConstants.HORIZONTAL && i0 != SwingConstants.VERTICAL) {
            throw new IllegalArgumentException("FLabel$setScaleBy "
                    + "must be passed either SwingConstants.HORIZONTAL "
                    + "or SwingConstants.VERTICAL.");
        }

        this.fontScaleBy = i0;
    }

    /** @param i0 &emsp; Font.PLAIN, .BOLD, or .ITALIC */
    // NOT public; must be set when label is built.
    private void setFontStyle(int i0) {
        if (i0 != Font.PLAIN && i0 != Font.BOLD && i0 != Font.ITALIC) {
            throw new IllegalArgumentException("FLabel$setFontStyle "
                    + "must be passed either Font.PLAIN, Font.BOLD, or Font.ITALIC.");
        }
        this.fontStyle = i0;
    }

    /** @param i0 &emsp; SwingConstants.CENTER, .LEFT or .RIGHT */
    // NOT public; must be set when label is built.
    private void setFontAlign(int i0) {
        if (i0 != SwingConstants.CENTER && i0 != SwingConstants.LEFT && i0 != SwingConstants.RIGHT) {
            throw new IllegalArgumentException("FLabel$setFontStyle "
                    + "must be passed either SwingConstants.CENTER, "
                    + "SwingConstants.LEFT, or SwingConstants.RIGHT");
        }
        this.setHorizontalAlignment(i0);
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
    public void setCommand(Command c0) {
        this.cmdClick = c0;
    }

    @Override
    public void setOpaque(final boolean b0) {
        // Must be overridden to allow drawing order of background, icon, string
        this.opaque = b0;
        super.setOpaque(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        g2d = (Graphics2D) g.create();
        w = getWidth();
        h = getHeight();

        // Opacity, select
        if (this.opaque && !selected) {
            g2d.setColor(clrInactive);
            g2d.fillRect(0, 0, w, h);
        }
        else if (selectable && selected) {
            g2d.setColor(clrActive);
            g2d.fillRect(0, 0, w, h);
        }

        // Hover
        if (hoverable && hovered && !selected) {
            g2d.setColor(clrHover);
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(clrBorders);
            g2d.drawRect(0, 0, w - 1, h - 1);
        }

        // Icon in background
        if (iconInBackground) {
            x = 3;
            sh = (int) ((h - 2 * x) * iconScaleFactor);
            sw = (int) (sh * iar);
            y = (int) ((h - sh) / 2);

            if (hoverable && hovered && !selected) {
                g2d.setComposite(alphaStrong);
            }
            else {
                g2d.setComposite(alphaDim);
            }
            g2d.drawImage(img, x, y, sw + x, sh + y, 0, 0, iw, ih, null);
        }

        super.paintComponent(g);
    }
}
