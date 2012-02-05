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
 * A custom instance of JLabel using Forge skin properties.
 * 
 * Font size can be scaled to a percentage of label height (60% by default).
 * 
 * Font scaling can be toggled.
 */
@SuppressWarnings("serial")
public class FLabel extends JLabel {
    private final FSkin skin;
    private final ComponentAdapter cadResize;
    private final MouseAdapter madEvents;
    private final Color clrText, clrBorders, clrHover, clrInactive, clrActive;

    private boolean fontScaleAuto;
    private boolean iconScaleAuto;

    private int fontScaleBy;

    private boolean iconInBackground;
    private boolean opaque;

    private boolean selectable;
    private boolean selected;
    private boolean hoverable;
    private boolean hovered;

    private double fontScaleFactor;
    private double iconScaleFactor;

    private Image img;
    private Graphics2D g2d;
    private Command cmdClick;
    private int x, y, w, h, iw, ih, sw, sh, ref;
    private double iar;
    private AlphaComposite alphaDim, alphaStrong;

    private int fontStyle;

    /** */
    public FLabel() {
        this("");
    }

    /** @param i0 &emsp; {@link javax.swing.ImageIcon} */
    public FLabel(final Icon i0) {
        this("");
        this.setIcon(i0);
    }

    /**
     * @param s0 &emsp; {@link java.lang.String}
     * @param i0 &emsp; {@link javax.swing.ImageIcon}
     */
    public FLabel(final String s0, final Icon i0) {
        this(s0);
        this.setIcon(i0);
    }

    /**
     * @param s0 &emsp; {@link java.lang.String} text
     * @param align0 &emsp; Text alignment
     */
    public FLabel(final String s0, final int align0) {
        this(s0);
        this.setHorizontalAlignment(align0);
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FLabel(final String s0) {
        super(s0);
        this.skin = Singletons.getView().getSkin();
        // Final inits
        this.clrText = skin.getColor(FSkin.Colors.CLR_TEXT);
        this.clrBorders = skin.getColor(FSkin.Colors.CLR_BORDERS);
        this.clrHover = skin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = skin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrInactive = skin.getColor(FSkin.Colors.CLR_INACTIVE);

        // Custom properties
        this.fontScaleAuto = true;
        this.fontScaleFactor = 0.6;
        this.iconScaleAuto = true;
        this.iconScaleFactor = 0.8;
        this.selectable = false;
        this.selected = false;
        this.hoverable = false;
        this.hovered = false;
        this.fontScaleBy = SwingConstants.VERTICAL;
        this.fontStyle = Font.PLAIN;
        this.iconInBackground = false;

        // Default properties
        this.setForeground(clrText);
        this.setBackground(clrInactive);
        this.setVerticalTextPosition(SwingConstants.CENTER);
        this.setVerticalAlignment(SwingConstants.CENTER);
        this.setIconBackgroundAlpha(1.0f);

        // Resize adapter
        this.cadResize = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (fontScaleAuto) {
                    ref = (fontScaleBy == SwingConstants.VERTICAL ? getHeight() : getWidth());
                    switch (fontStyle) {
                        case Font.BOLD:
                            setFont(skin.getBoldFont((int) (ref * fontScaleFactor)));
                            repaint();
                            break;
                        case Font.ITALIC:
                            setFont(skin.getItalicFont((int) (ref * fontScaleFactor)));
                            break;
                        default:
                            setFont(skin.getFont((int) (ref * fontScaleFactor)));
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
        this.removeComponentListener(cadResize);
        this.addComponentListener(cadResize);

        this.madEvents = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!selectable) { return; }
                if (selected) { setSelected(false); }
                else { setSelected(true); }
                cmdClick.execute();
            }
        };
    }

    /** @param b0 &emsp; boolean */
    public void setHoverable(boolean b0) {
        this.hoverable = b0;
        if (!b0) { this.removeMouseListener(madEvents); }
        else { this.addMouseListener(madEvents); }
    }

    /** @param b0 &emsp; boolean */
    public void setSelectable(boolean b0) {
        this.selectable = b0;
    }

    /** @param b0 &emsp; boolean */
    public void setSelected(boolean b0) {
        this.selected = b0;
        repaint();
    }

    /** @param d0 &emsp; Scale factor for font size relative to label height, percent.
     * If your font is "blowing up", try setting an explicity height/width for this label. */
    public void setFontScaleFactor(final double d0) {
        this.fontScaleFactor = d0;
    }

    /** @param b0 &emsp; {@link java.lang.boolean} */
    public void setFontScaleAuto(final boolean b0) {
        this.fontScaleAuto = b0;
    }

    /** @param d0 &emsp; Scale factor for icon size relative to label height, percent. */
    public void setIconScaleFactor(final double d0) {
        this.iconScaleFactor = d0;
    }

    /** @param b0 &emsp; Font drawn in background, or positioned by default. */
    public void setIconInBackground(final boolean b0) {
        this.iconInBackground = b0;
        // Reset icon in case this method has been called after setIcon().
        if (b0 && img != null) { setIcon(new ImageIcon(img)); }
    }

    /** @param b0 &emsp; {@link java.lang.boolean} */
    public void setIconScaleAuto(final boolean b0) {
        this.iconScaleAuto = b0;
        // Reset icon in case this method has been called after setIcon().
        if (!b0 && img != null) { setIcon(new ImageIcon(img)); }
    }

    /** Sets alpha if icon is in background.
     * @param f0 &emsp; float */
    public void setIconBackgroundAlpha(float f0) {
        this.alphaDim = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f0);
        this.alphaStrong = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    }

    /** @param i0 &emsp; int, must be SwingConstants.HORIZONTAL or VERTICAL */
    public void setFontScaleBy(int i0) {
        if (i0 != SwingConstants.HORIZONTAL && i0 != SwingConstants.VERTICAL) {
            throw new IllegalArgumentException("FLabel$setScaleBy "
                    + "must be passed either SwingConstants.HORIZONTAL "
                    + "or SwingConstants.VERTICAL.");
        }

        this.fontScaleBy = i0;
    }

    /** @param i0 &emsp; must be Font.PLAIN, Font.BOLD, Font.ITALIC */
    public void setFontStyle(int i0) {
        if (i0 != Font.PLAIN && i0 != Font.BOLD && i0 != Font.ITALIC) {
            throw new IllegalArgumentException("FLabel$setFontStyle "
                    + "must be passed either Font.PLAIN, Font.BOLD, or Font.ITALIC.");
        }
        this.fontStyle = i0;
    }

    @Override
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

    /** @return {@link forge.Command} on click */
    public Command getCommand() {
        return this.cmdClick;
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
