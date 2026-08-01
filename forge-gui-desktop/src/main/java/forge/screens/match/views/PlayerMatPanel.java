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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import forge.PlayerMat;
import forge.toolbox.FSkin.SkinnedPanel;

/**
 * The surface drawn under a player's battlefield.
 * <p>
 * Sits behind the (fully transparent) battlefield scroll pane, so cards and
 * overlays paint on top of it. Currently renders one of the {@link PlayerMat}
 * preset colours; the paint routine is deliberately isolated here so mat images
 * can be added later without touching {@link VField}.
 */
@SuppressWarnings("serial")
public class PlayerMatPanel extends SkinnedPanel {
    /** Lift applied to the middle of the mat so it doesn't read as a flat slab. */
    private static final float SHEEN_ALPHA = 0.10f;
    /** Darkening towards the edges. */
    private static final float VIGNETTE_ALPHA = 0.38f;

    private PlayerMat mat = PlayerMat.SLATE;

    public PlayerMatPanel() {
        // Stays non-opaque even when a mat is drawn: paintComponent covers every
        // pixel itself, and claiming opacity without guaranteeing that in every
        // state is what produces repaint artifacts.
        setOpaque(false);
    }

    public void setMat(final PlayerMat mat0) {
        final PlayerMat next = mat0 == null ? PlayerMat.SLATE : mat0;
        if (next == mat) { return; }
        mat = next;
        repaint();
    }

    public PlayerMat getMat() {
        return mat;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        final int w = getWidth();
        final int h = getHeight();
        if (mat.isTransparent() || w <= 0 || h <= 0) {
            super.paintComponent(g);
            return;
        }

        final Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Color base = new Color(mat.getRgb());
        g2d.setColor(base);
        g2d.fillRect(0, 0, w, h);

        // A soft centre sheen plus an edge vignette give the surface some depth,
        // which is what separates "a mat" from "a coloured rectangle".
        final float radius = Math.max(w, h) * 0.75f;
        final Point2D centre = new Point2D.Float(w / 2f, h / 2f);
        g2d.setPaint(new RadialGradientPaint(centre, radius,
                new float[] {0f, 1f},
                new Color[] {new Color(1f, 1f, 1f, SHEEN_ALPHA), new Color(1f, 1f, 1f, 0f)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2d.fillRect(0, 0, w, h);

        g2d.setPaint(new RadialGradientPaint(centre, radius,
                new float[] {0.55f, 1f},
                new Color[] {new Color(0f, 0f, 0f, 0f), new Color(0f, 0f, 0f, VIGNETTE_ALPHA)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2d.fillRect(0, 0, w, h);

        // Thin lighter edge, like the stitching on a real playmat.
        g2d.setColor(new Color(1f, 1f, 1f, 0.12f));
        g2d.drawRect(0, 0, w - 1, h - 1);

        g2d.dispose();
    }
}
