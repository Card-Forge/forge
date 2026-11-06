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
    private String arenaLabel;
    private boolean primary = false;
    private final AlphaComposite disabledComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
    private final ArenaMotion.Fade hoverFade = new ArenaMotion.Fade(this::repaintSelf, 0f);
    private final ArenaMotion.Fade pressFade = new ArenaMotion.Fade(this::repaintSelf, 0f);
    private final ArenaMotion.Fade appearFade = new ArenaMotion.Fade(this::repaintSelf, 1f);
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
                if (ArenaTheme.isEnabled()) { hoverFade.to(1f, 110); }
                if (isToggled() || !isEnabled()) { return; }
                resetImg();
                repaintSelf();
            }

            @Override
            public void mouseExited(final MouseEvent evt) {
                hovered = false;
                if (ArenaTheme.isEnabled()) { hoverFade.to(0f, 180); }
                if (isToggled() || !isEnabled()) { return; }
                resetImg();
                repaintSelf();
            }

            @Override
            public void mousePressed(final MouseEvent evt) {
                if (isToggled() || !isEnabled()) { return; }
                if (ArenaTheme.isEnabled()) { pressFade.to(1f, 70); }
                imgL = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_LEFT);
                imgM = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_CENTER);
                imgR = FSkin.getIcon(FSkinProp.IMG_BTN_DOWN_RIGHT);
                repaintSelf();
            }

            @Override
            public void mouseReleased(final MouseEvent evt) {
                if (ArenaTheme.isEnabled()) { pressFade.to(0f, 150); }
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

    /** Arena: the word shown on the button in place of its text, which the game logic keeps reading ("Next" for "OK"). */
    public void setArenaLabel(final String label) {
        this.arenaLabel = label;
        repaintSelf();
    }

    /** Arena: the button has just come into view, and grows into place. */
    public void arenaAppear() {
        appearFade.set(0f);
        appearFade.to(1f, 200);
    }

    /** Marks this as the main action button; the Arena theme paints it amber. */
    public void setPrimary(final boolean b0) {
        this.primary = b0;
        repaintSelf();
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
        if (ArenaTheme.isEnabled()) {
            paintArena(g);
            return;
        }
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

    /** Two chevrons, drawn so that they sit dead centre whatever the font would have done. */
    private void paintArenaChevrons(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final float s = getHeight() * 0.19f, cx = getWidth() / 2f, cy = getHeight() / 2f;
        g.setStroke(new java.awt.BasicStroke(Math.max(2f, getHeight() * 0.075f), java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        for (int pass = 0; pass < 2; pass++) {
            g.setColor(pass == 0 ? new Color(0, 0, 0, 110) : Color.WHITE);
            final float dy = pass == 0 ? 1f : 0f;
            for (final float off : new float[] {-s * 0.62f, s * 0.62f}) {
                final java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
                p.moveTo(cx + off - s * 0.5f, cy - s + dy);
                p.lineTo(cx + off + s * 0.5f, cy + dy);
                p.lineTo(cx + off - s * 0.5f, cy + s + dy);
                g.draw(p);
            }
        }
    }

    private void paintArena(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            final boolean pressed = getModel().isPressed() && getModel().isArmed();
            final float show = appearFade.get(), press = pressFade.get();
            if (show <= 0f) {
                return;
            }
            final float scale = (0.9f + 0.1f * show) * (1f - 0.04f * press);
            if (scale != 1f) {
                g2d.translate(getWidth() / 2d, getHeight() / 2d);
                g2d.scale(scale, scale);
                g2d.translate(-getWidth() / 2d, -getHeight() / 2d);
            }
            if (show < 1f) {
                g2d.setComposite(AlphaComposite.SrcOver.derive(show));
            }
            final java.awt.Composite whole = g2d.getComposite();
            final float wholeAlpha = whole instanceof AlphaComposite a ? a.getAlpha() : 1f;
            float light = isEnabled() ? hoverFade.get() : 0f;
            // the button to press next keeps asking to be pressed, the light on it rising and falling
            if (primary && arenaLabel != null && isEnabled() && !toggle && light < 1f && !ArenaTheme.snapshotting) {
                light = Math.max(light, 0.55f * ArenaMotion.pulse(this, 2.2f));
            }
            if (light < 1f) {
                ArenaTheme.paintButton(g2d, getWidth(), getHeight(), primary, false, pressed, isFocusOwner(), toggle, isEnabled());
            }
            if (light > 0f) {
                g2d.setComposite(AlphaComposite.SrcOver.derive(wholeAlpha * light));
                ArenaTheme.paintButton(g2d, getWidth(), getHeight(), primary, true, pressed, isFocusOwner(), toggle, isEnabled());
                g2d.setComposite(whole);
            }
            final String text = arenaLabel != null ? arenaLabel : getText();
            if ("\u00BB".equals(text)) {
                paintArenaChevrons(g2d);
            } else if (text != null && !text.isEmpty()) {
                // one voice on every button: the display face, sized to the button and shrunk to fit
                float size = Math.max(12f, Math.min(getHeight() * 0.54f, 34f));
                while (size > 10f && ArenaTheme.paintLabel(g2d, text, size, 0f, 0f, false) > getWidth() - getHeight() * 0.7f) {
                    size -= 1f;
                }
                g2d.setFont(ArenaTheme.displayFont(java.awt.Font.BOLD, size));
                final FontMetrics fm = g2d.getFontMetrics();
                final float tx = (getWidth() - ArenaTheme.paintLabel(g2d, text, size, 0f, 0f, false)) / 2f;
                // centred on the capitals, not on the line box
                final int ty = Math.round((getHeight() + fm.getAscent() * 0.72f) / 2f);
                if (isEnabled()) {
                    g2d.setColor(new Color(60, 22, 0, primary ? 200 : 150));
                    ArenaTheme.paintLabel(g2d, text, size, tx, ty + 1, true);
                    if (primary) {
                        ArenaTheme.paintLabel(g2d, text, size, tx, ty + 2, true);
                    }
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(new Color(255, 255, 255, 178));
                }
                ArenaTheme.paintLabel(g2d, text, size, tx, ty, true);
            }
        } finally {
            g2d.dispose();
        }
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
