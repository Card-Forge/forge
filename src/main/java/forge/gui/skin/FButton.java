package forge.gui.skin;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JButton;

import forge.AllZone;

/**
 * The core JButton used throughout the Forge project. Follows skin font and
 * theme button styling.
 * 
 */
@SuppressWarnings("serial")
public class FButton extends JButton {

    /** The img r. */
    private Image imgL;
    private Image imgM;
    private Image imgR;
    private int w, h = 0;
    private boolean allImagesPresent = false;
    private final FSkin skin;
    private final AlphaComposite disabledComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);

    /**
     * Instantiates a new f button.
     * 
     * @param msg
     *            the msg
     */
    public FButton(final String msg) {
        super(msg);
        this.skin = AllZone.getSkin();
        this.setOpaque(false);
        this.setForeground(this.skin.getTxt1a());
        this.setBackground(Color.red);
        this.setContentAreaFilled(false);
        this.setMargin(new Insets(0, 25, 0, 25));
        this.setFont(this.skin.getFont1().deriveFont(Font.BOLD, 15));
        this.imgL = this.skin.getBtnLup().getImage();
        this.imgM = this.skin.getBtnMup().getImage();
        this.imgR = this.skin.getBtnRup().getImage();

        if ((this.imgL != null) && (this.imgM != null) && (this.imgR != null)) {
            this.allImagesPresent = true;
        }

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getBtnLover().getImage();
                    FButton.this.imgM = FButton.this.skin.getBtnMover().getImage();
                    FButton.this.imgR = FButton.this.skin.getBtnRover().getImage();
                }
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getBtnLup().getImage();
                    FButton.this.imgM = FButton.this.skin.getBtnMup().getImage();
                    FButton.this.imgR = FButton.this.skin.getBtnRup().getImage();
                }
            }

            @Override
            public void mousePressed(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getBtnLdown().getImage();
                    FButton.this.imgM = FButton.this.skin.getBtnMdown().getImage();
                    FButton.this.imgR = FButton.this.skin.getBtnRdown().getImage();
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(final Graphics g) {
        if (!this.allImagesPresent) {
            return;
        }

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        if (!this.isEnabled()) {
            g2d.setComposite(this.disabledComposite);
        }

        this.w = this.getWidth();
        this.h = this.getHeight();

        g2d.drawImage(this.imgL, 0, 0, this.h, this.h, null);
        g2d.drawImage(this.imgM, this.h, 0, this.w - (2 * this.h), this.h, null);
        g2d.drawImage(this.imgR, this.w - this.h, 0, this.h, this.h, null);

        super.paintComponent(g);
    }
}
