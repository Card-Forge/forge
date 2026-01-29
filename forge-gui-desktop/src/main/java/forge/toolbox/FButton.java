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
package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;

import forge.gui.UiCommand;
import forge.gui.framework.ILocalRepaint;
import forge.gui.interfaces.IButton;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedButton;

/**
 * The core JButton used throughout the Forge project. Follows skin font and
 * theme button styling.
 *
 */
@SuppressWarnings("serial")
public class FButton extends SkinnedButton implements ILocalRepaint, IButton {

    /** The img r. */
    private SkinImage imgL;
    private SkinImage imgM;
    private SkinImage imgR;
    private int w, h = 0;
    private boolean allImagesPresent = false;
    private boolean toggle = false;
    private boolean hovered = false;
    private boolean useHighlightMode = false; // Enable inverted color mode for yield buttons
    private boolean highlighted = false; // When in highlight mode: true = red (active), false = blue (normal)
    private final AlphaComposite disabledComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
    private KeyAdapter klEnter;

    /**
     * Instantiates a new FButton.
     */
    public FButton() {
        this("");
    }

    public FButton(final String label) {
        super(label);
        this.setOpaque(false);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(Color.red);
        this.setFocusPainted(false);
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setContentAreaFilled(false);
        this.setMargin(new Insets(0, 25, 0, 25));
        this.setFont(FSkin.getBoldFont(14));
        this.imgL = FSkin.getIcon(FSkinProp.IMG_BTN_UP_LEFT);
        this.imgM = FSkin.getIcon(FSkinProp.IMG_BTN_UP_CENTER);
        this.imgR = FSkin.getIcon(FSkinProp.IMG_BTN_UP_RIGHT);

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
            public void mouseEntered(final MouseEvent evt) {
                hovered = true;
                if (isToggled() || !isEnabled()) { return; }
                resetImg();
                repaintSelf();
            }

            @Override
            public void mouseExited(final MouseEvent evt) {
                hovered = false;
                if (isToggled() || !isEnabled()) { return; }
                resetImg();
                repaintSelf();
            }

            @Override
            public void mousePressed(final MouseEvent evt) {
                if (isToggled() || !isEnabled()) { return; }
                imgL = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_LEFT);
                imgM = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_CENTER);
                imgR = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_RIGHT);
                repaintSelf();
            }

            @Override
            public void mouseReleased(final MouseEvent evt) {
                if (isToggled() || !isEnabled()) { return; }
                resetImg();
                repaintSelf();
            }
        });

        // Focus events
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                if (isToggled()) { return; }
                resetImg();
                addKeyListener(klEnter);
                repaintSelf();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                if (isToggled()) { return; }
                resetImg();
                removeKeyListener(klEnter);
                repaintSelf();
            }
        });
    }

    private void resetImg() {
        if (hovered) {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_OVER_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_OVER_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_OVER_RIGHT);
        }
        else if (useHighlightMode) {
            // Highlight mode for yield buttons:
            // - highlighted=true: UP images (red/orange) for active yield
            // - highlighted=false: FOCUS images (blue) for normal state
            if (highlighted) {
                imgL = FSkin.getIcon(FSkinProp.IMG_BTN_UP_LEFT);
                imgM = FSkin.getIcon(FSkinProp.IMG_BTN_UP_CENTER);
                imgR = FSkin.getIcon(FSkinProp.IMG_BTN_UP_RIGHT);
            } else {
                imgL = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_LEFT);
                imgM = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_CENTER);
                imgR = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_RIGHT);
            }
        }
        else if (isFocusOwner()) {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_FOCUS_RIGHT);
        } else {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_UP_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_UP_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_UP_RIGHT);
        }
    }

    @Override
    public void setEnabled(final boolean b0) {
        if (!b0) {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_RIGHT);
        }
        else {
            resetImg();
        }

        super.setEnabled(b0);
        repaintSelf();
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
    public void setToggled(final boolean b0) {
        if (b0) {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_TOGGLE_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_TOGGLE_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_TOGGLE_RIGHT);
        }
        else if (isEnabled()) {
            resetImg();
        }
        else {
            imgL = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_LEFT);
            imgM = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_CENTER);
            imgR = FSkin.getIcon(FSkinProp.IMG_BTN_DISABLED_RIGHT);
        }
        this.toggle = b0;
        repaintSelf();
    }

    /**
     * Enable highlight mode for this button.
     * In highlight mode, button colors are inverted:
     * - Normal state uses FOCUS images (blue)
     * - Highlighted state uses UP images (red/orange)
     * Used for yield buttons.
     * @param b0 true to enable highlight mode
     */
    public void setUseHighlightMode(final boolean b0) {
        this.useHighlightMode = b0;
        if (isEnabled() && !isToggled()) {
            resetImg();
            repaintSelf();
        }
    }

    /**
     * Check if button is in highlighted state.
     * Only meaningful when useHighlightMode is true.
     * @return boolean
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Set highlighted state for the button.
     * Requires useHighlightMode to be enabled first.
     * When highlighted=false: uses FOCUS images (blue)
     * When highlighted=true: uses UP images (red/orange)
     * This is used for yield buttons to show which yield is active.
     * @param b0 true to highlight (red), false for normal (blue)
     */
    public void setHighlighted(final boolean b0) {
        this.highlighted = b0;
        if (isEnabled() && !isToggled()) {
            resetImg();
            repaintSelf();
        }
    }

    public int getAutoSizeWidth() {
        int width = 0;
        if (this.getText() != null && !this.getText().isEmpty()) {
            final FontMetrics metrics = this.getFontMetrics(this.getFont());
            width = metrics.stringWidth(this.getText());
        }
        return width;
    }

    /** Prevent button from repainting the whole screen. */
    @Override
    public void repaintSelf() {
        final Dimension d = getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        if (!allImagesPresent) {
            return;
        }

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        if (!isEnabled()) {
            g2d.setComposite(this.disabledComposite);
        }

        w = getWidth();
        h = getHeight();

        FSkin.drawImage(g2d, imgL, 0, 0, this.h, this.h);
        FSkin.drawImage(g2d, imgM, this.h, 0, this.w - (2 * this.h), this.h);
        FSkin.drawImage(g2d, imgR, this.w - this.h, 0, this.h, this.h);

        super.paintComponent(g);
    }

    @Override
    public void setCommand(final UiCommand command) {
        addActionListener(e -> command.run());
    }

    @Override
    public void setImage(final FSkinProp color) {
        setForeground(FSkin.getColor(Colors.fromSkinProp(color)));
    }

    @Override
    public void setTextColor(final int r, final int g, final int b) {
        setForeground(new Color(r, g, b));
    }
}
