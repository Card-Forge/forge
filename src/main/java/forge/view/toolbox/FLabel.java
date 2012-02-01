package forge.view.toolbox;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

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
    private boolean scaleAuto;
    private double fontScaleFactor = 0.6;
    private double iconScaleFactor = 0.8;
    private double aspectRatio;
    private Image img = null;
    private int w, h;
    private int fontStyle = Font.PLAIN;

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
        this.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        this.setVerticalTextPosition(SwingConstants.CENTER);
        this.setVerticalAlignment(SwingConstants.CENTER);

        this.cadResize = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                switch (fontStyle) {
                    case Font.BOLD:
                        setFont(skin.getBoldFont((int) (getHeight() * fontScaleFactor)));
                        break;
                    case Font.ITALIC:
                        setFont(skin.getItalicFont((int) (getHeight() * fontScaleFactor)));
                        break;
                    default:
                        setFont(skin.getFont((int) (getHeight() * fontScaleFactor)));
                }

                if (img == null) { return; }
                aspectRatio = img.getWidth(null) / img.getHeight(null);
                h = (int) (getHeight() * iconScaleFactor);
                w = (int) (h * aspectRatio * iconScaleFactor);
                if (w == 0 || h == 0) { return; }

                FLabel.super.setIcon(new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
            }
        };

        this.setScaleAuto(true);
    }

    /** @param b0 &emsp; {@link java.lang.boolean} */
    public void setScaleAuto(final boolean b0) {
        this.scaleAuto = b0;
        if (scaleAuto) {
            this.addComponentListener(cadResize);
        }
        else {
            this.removeComponentListener(cadResize);
        }
    }

    /** 
     * Sets whether bold or italic font should be used for this label.
     * 
     * @param i0 &emsp; Font.BOLD or Font.ITALIC
     */
    public void setFontStyle(int i0) {
        this.fontStyle = i0;
    }

    /** @param d0 &emsp; Scale factor for font size relative to label height, percent. */
    public void setFontScaleFactor(final double d0) {
        this.fontScaleFactor = d0;
    }

    /** @param d0 &emsp; Scale factor for icon size relative to label height, percent. */
    public void setIconScaleFactor(final double d0) {
        this.iconScaleFactor = d0;
    }

    /** @return {@link java.lang.boolean} */
    public boolean isScaleAuto() {
        return this.scaleAuto;
    }

    @Override
    public void setIcon(final Icon i0) {
        if (scaleAuto) {
            // Setting the icon in the usual way leads to scaling problems.
            // So, only the image is saved, and scaled along with the font
            // in the resize adapter.
            if (i0 == null) { return; }
            this.img = ((ImageIcon) i0).getImage();
        }
        else {
            super.setIcon(i0);
        }
    }
}
