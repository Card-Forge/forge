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

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.Locale;

/**
 * <p>
 * GlowText class.
 * </p>
 * 
 * @author Forge
 * @version $Id: OutlinedLabel.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class OutlinedLabel extends JLabel {

    /**
     * Instantiates a new glow text.
     */
    public OutlinedLabel() {
    }

    /** Constant <code>serialVersionUID=-2868833097364223352L</code>. */
    private static final long serialVersionUID = -2868833097364223352L;
    private Color outlineColor;
    private final static int outlineSize = 1; 
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
    public final void setGlow(final Color glowColor) {
        this.outlineColor = glowColor;
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
        size.width += outlineSize * 2;
        size.height += outlineSize * 2;
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

        Dimension size = getSize();
//
//        if( size.width < 50 ) {
//            g.setColor(Color.cyan);
//            g.drawRect(0, 0, size.width-1, size.height-1);
//        }
        
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int textX = outlineSize, textY = 0;
        int wrapWidth = Math.max(0, wrap ? size.width - outlineSize * 2 : Integer.MAX_VALUE);

        final String text = getText();
        AttributedString attributedString = new AttributedString(text);
        if (!StringUtils.isEmpty(text)) {
            attributedString.addAttribute(TextAttribute.FONT, getFont());
        }
        AttributedCharacterIterator charIterator = attributedString.getIterator();
        FontRenderContext fontContext = g2d.getFontRenderContext();

        LineBreakMeasurer measurer = new LineBreakMeasurer(charIterator, BreakIterator.getWordInstance(Locale.ENGLISH), fontContext);
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
            measurer = new LineBreakMeasurer(charIterator, BreakIterator.getCharacterInstance(Locale.ENGLISH), fontContext);
        } else {
            measurer.setPosition(0);
        }
        while (measurer.getPosition() < charIterator.getEndIndex()) {
            TextLayout textLayout = measurer.nextLayout(wrapWidth);
            float ascent = textLayout.getAscent();
            textY += ascent; // Move down to baseline.

            g2d.setColor(outlineColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            
            
            textLayout.draw(g2d, textX + outlineSize, textY - outlineSize);
            textLayout.draw(g2d, textX + outlineSize, textY + outlineSize);
            textLayout.draw(g2d, textX - outlineSize, textY - outlineSize);
            textLayout.draw(g2d, textX - outlineSize, textY + outlineSize);

            g2d.setColor(getForeground());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            textLayout.draw(g2d, textX, textY);

            // Move down to top of next line.
            textY += textLayout.getDescent() + textLayout.getLeading();
        }
    }
}
