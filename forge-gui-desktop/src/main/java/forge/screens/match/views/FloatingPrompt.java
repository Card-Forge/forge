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
package forge.screens.match.views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.Timer;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.toolbox.FSkin;

/**
 * The prompt shown in the middle of the screen whenever the game is waiting on
 * the local player to act.
 * <p>
 * Deliberately not closable: the prompt carries the only controls for
 * responding to the game, so dismissing it would strand the match. The user may
 * move and resize it, and that position is remembered for the rest of the
 * session and across runs.
 */
@SuppressWarnings("serial")
public class FloatingPrompt extends FloatingMatchWindow {
    private static final int DEFAULT_WIDTH = 460;
    private static final int DEFAULT_HEIGHT = 190;

    /** Thickness of the pulsing border, and how often it steps. */
    private static final int PULSE_THICKNESS = 3;
    private static final int PULSE_INTERVAL_MS = 40;
    private static final int PULSE_PERIOD_MS = 1400;

    private Timer flashTimer;
    private Timer pulseTimer;
    private long pulseStart;

    public FloatingPrompt() {
        super(FPref.PROMPT_WINDOW_LOC, true);
        setMinimumSize(new Dimension(260, 120));
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Pulses the window's border for as long as the game is waiting on the
     * local player, so it reads as "your move" at a glance.
     */
    public void setPulsing(final boolean pulsing) {
        if (pulsing == (pulseTimer != null)) { return; }
        if (pulsing) {
            pulseStart = System.currentTimeMillis();
            pulseTimer = new Timer(PULSE_INTERVAL_MS, e -> repaintBorder());
            pulseTimer.start();
        } else {
            pulseTimer.stop();
            pulseTimer = null;
        }
        repaintBorder();
    }

    /** Repaints only the frame, so the pulse doesn't redraw the prompt's contents. */
    private void repaintBorder() {
        if (!isVisible()) { return; }
        final int t = PULSE_THICKNESS;
        repaint(0, 0, getWidth(), t);
        repaint(0, getHeight() - t, getWidth(), t);
        repaint(0, 0, t, getHeight());
        repaint(getWidth() - t, 0, t, getHeight());
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        if (pulseTimer == null) { return; }

        final double phase = (System.currentTimeMillis() - pulseStart) % PULSE_PERIOD_MS / (double) PULSE_PERIOD_MS;
        final int alpha = (int) Math.round(70 + 185 * (0.5 - 0.5 * Math.cos(phase * 2 * Math.PI)));

        final Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(PULSE_THICKNESS));
        g2d.setColor(new Color(VPrompt.ACCENT.getRed(), VPrompt.ACCENT.getGreen(), VPrompt.ACCENT.getBlue(), alpha));
        final int inset = PULSE_THICKNESS / 2;
        g2d.drawRect(inset, inset, getWidth() - PULSE_THICKNESS, getHeight() - PULSE_THICKNESS);
        g2d.dispose();
    }

    /**
     * ESC must not close this window — {@link VPrompt} maps ESC to the Cancel
     * button, which is a game action, not a dismissal.
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            return false; //let the focused button's key adapter handle it
        }
        return super.dispatchKeyEvent(e);
    }

    /**
     * Pulses the window to draw the eye — the floating equivalent of
     * {@link forge.gui.framework.SDisplayUtil#remind}, which can't be used here
     * because it works on a docked cell.
     */
    public void flash(final int steps, final int delay) {
        if (!isVisible() || flashTimer != null) { return; }
        toFront();

        final JPanel body = getContentPanel();
        final Color base = body.getBackground();
        final Color hot = FSkin.getColor(FSkin.Colors.CLR_ACTIVE).getColor();
        final boolean wasOpaque = body.isOpaque();
        body.setOpaque(true);

        flashTimer = new Timer(delay, null);
        final int[] step = {0};
        flashTimer.addActionListener(e -> {
            final int i = step[0]++;
            if (i >= steps * 2) {
                flashTimer.stop();
                flashTimer = null;
                body.setBackground(base);
                body.setOpaque(wasOpaque);
                body.repaint();
                return;
            }
            body.setBackground(i % 2 == 0 ? hot : base);
            body.repaint();
        });
        flashTimer.start();
    }
}
