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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
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
     * Instantiates a new FButton.
     */
    public FButton() {
        this("");
    }

    /**
     * Instantiates a new FButton.
     * 
     * @param msg
     *            the msg
     */
    public FButton(final String msg) {
        super(msg);
        this.skin = AllZone.getSkin();
        this.setOpaque(false);
        this.setForeground(this.skin.getColor("text"));
        this.setBackground(Color.red);
        this.setFocusPainted(false);
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setContentAreaFilled(false);
        this.setMargin(new Insets(0, 25, 0, 25));
        this.setFont(this.skin.getFont1().deriveFont(Font.BOLD, 15));
        this.imgL = skin.getImage("button.upLEFT");
        this.imgM = skin.getImage("button.upCENTER");
        this.imgR = skin.getImage("button.upRIGHT");

        if ((this.imgL != null) && (this.imgM != null) && (this.imgR != null)) {
            this.allImagesPresent = true;
        }

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.overLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.overCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.overRIGHT");
                }
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.upLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.upCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.upRIGHT");
                }
            }

            @Override
            public void mousePressed(final java.awt.event.MouseEvent evt) {
                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.downLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.downCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.downRIGHT");
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
