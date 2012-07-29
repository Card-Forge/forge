/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.Locale;

import javax.swing.JLabel;

/**
 * <p>
 * GlowText class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GlowText extends JLabel {

    /**
     * Instantiates a new glow text.
     */
    public GlowText() {
    }

    /** Constant <code>serialVersionUID=-2868833097364223352L</code>. */
    private static final long serialVersionUID = -2868833097364223352L;
    private int glowSize;
    private Color glowColor;
    private boolean wrap;

    /**
     * <p>
     * setGlow.
     * </p>
     * 
     * @param glowColor
     *            a {@link java.awt.Color} object.
     * @param size
     *            a int.
     * @param intensity
     *            a float.
     */
    public final void setGlow(final Color glowColor, int size, float intensity) {
        this.glowColor = glowColor;
        this.glowSize = size;
    }

    /**
     * <p>
     * Setter for the field <code>wrap</code>.
     * </p>
     * 
     * @param wrap
     *            a boolean.
     */
    public final void setWrap(final boolean wrap) {
        this.wrap = wrap;
    }

    /**
     * <p>
     * getPreferredSize.
     * </p>
     * 
     * @return a {@link java.awt.Dimension} object.
     */
    @Override
    public final Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width += glowSize;
        size.height += glowSize / 2;
        return size;
    }

    /** {@inheritDoc} */
    @Override
    public final void setText(final String text) {
        super.setText(text);
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics g) {
        if (getText().length() == 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Dimension size = getSize();
        int textX = 0, textY = 0;
        int wrapWidth = Math.max(0, wrap ? size.width - glowSize : Integer.MAX_VALUE);

        AttributedString attributedString = new AttributedString(getText());
        attributedString.addAttribute(TextAttribute.FONT, getFont());
        AttributedCharacterIterator charIterator = attributedString.getIterator();
        FontRenderContext fontContext = g2d.getFontRenderContext();

        LineBreakMeasurer measurer = new LineBreakMeasurer(charIterator, BreakIterator.getWordInstance(Locale.ENGLISH),
                fontContext);
        int lineCount = 0;
        while (measurer.getPosition() < charIterator.getEndIndex()) {
            measurer.nextLayout(wrapWidth);
            lineCount++;
            if (lineCount > 2) {
                break;
            }
        }
        charIterator.first();
        // Use char wrap if word wrap would cause more than two lines of text.
        if (lineCount > 2) {
            measurer = new LineBreakMeasurer(charIterator, BreakIterator.getCharacterInstance(Locale.ENGLISH),
                    fontContext);
        } else {
            measurer.setPosition(0);
        }
        while (measurer.getPosition() < charIterator.getEndIndex()) {
            TextLayout textLayout = measurer.nextLayout(wrapWidth);
            float ascent = textLayout.getAscent();
            textY += ascent; // Move down to baseline.

            g2d.setColor(glowColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            textLayout.draw(g2d, textX + glowSize / 2 + 1, textY + glowSize / 2 - 1);
            textLayout.draw(g2d, textX + glowSize / 2 + 1, textY + glowSize / 2 + 1);
            textLayout.draw(g2d, textX + glowSize / 2 - 1, textY + glowSize / 2 - 1);
            textLayout.draw(g2d, textX + glowSize / 2 - 1, textY + glowSize / 2 + 1);

            g2d.setColor(getForeground());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            textLayout.draw(g2d, textX + glowSize / 2, textY + glowSize / 2);

            textY += textLayout.getDescent() + textLayout.getLeading(); // Move
                                                                        // down
                                                                        // to
                                                                        // top
                                                                        // of
                                                                        // next
                                                                        // line.
        }
    }
}
