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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
    private boolean toggle = false;
    private final FSkin skin;
    private final AlphaComposite disabledComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
    private KeyAdapter klEnter;

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

        klEnter = new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    doClick();
                }
            }
        };

        // Mouse events
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent evt) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.overLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.overCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.overRIGHT");
                }
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent evt) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled() && !FButton.this.isFocusOwner()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.upLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.upCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.upRIGHT");
                }
                else if (FButton.this.isEnabled() && FButton.this.isFocusOwner()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.focusLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.focusCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.focusRIGHT");
                }
            }

            @Override
            public void mousePressed(final java.awt.event.MouseEvent evt) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.downLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.downCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.downRIGHT");
                }
            }

            @Override
            public void mouseReleased(final java.awt.event.MouseEvent evt) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.downLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.downCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.downRIGHT");
                }
            }
        });

        // Focus events
        this.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.focusLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.focusCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.focusRIGHT");
                }

                addKeyListener(klEnter);
            }

            public void focusLost(FocusEvent e) {
                if (isToggled()) { return; }

                if (FButton.this.isEnabled()) {
                    FButton.this.imgL = FButton.this.skin.getImage("button.upLEFT");
                    FButton.this.imgM = FButton.this.skin.getImage("button.upCENTER");
                    FButton.this.imgR = FButton.this.skin.getImage("button.upRIGHT");
                }

                removeKeyListener(klEnter);
            }
        });
    }

    @Override
    public void setEnabled(boolean b0) {
        if (!b0) {
            FButton.this.imgL = FButton.this.skin.getImage("button.disabledLEFT");
            FButton.this.imgM = FButton.this.skin.getImage("button.disabledCENTER");
            FButton.this.imgR = FButton.this.skin.getImage("button.disabledRIGHT");
        }
        else {
            FButton.this.imgL = FButton.this.skin.getImage("button.upLEFT");
            FButton.this.imgM = FButton.this.skin.getImage("button.upCENTER");
            FButton.this.imgR = FButton.this.skin.getImage("button.upRIGHT");
        }

        super.setEnabled(b0);
    }

    /** 
     * Button toggle state, for a "permanently pressed" functionality, e.g. as a tab.
     * 
     * @return boolean
     */
    public boolean isToggled() {
        return toggle;
    }

    /** @param b0 &emsp; boolean. */
    public void setToggled(boolean b0) {
        if (b0) {
            FButton.this.imgL = FButton.this.skin.getImage("button.toggleLEFT");
            FButton.this.imgM = FButton.this.skin.getImage("button.toggleCENTER");
            FButton.this.imgR = FButton.this.skin.getImage("button.toggleRIGHT");
        }
        else if (isEnabled()) {
            FButton.this.imgL = FButton.this.skin.getImage("button.upLEFT");
            FButton.this.imgM = FButton.this.skin.getImage("button.upCENTER");
            FButton.this.imgR = FButton.this.skin.getImage("button.upRIGHT");
            repaint();
        }
        else {
            FButton.this.imgL = FButton.this.skin.getImage("button.disabledLEFT");
            FButton.this.imgM = FButton.this.skin.getImage("button.disabledCENTER");
            FButton.this.imgR = FButton.this.skin.getImage("button.disabledRIGHT");
            repaint();
        }
        this.toggle = b0;
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
