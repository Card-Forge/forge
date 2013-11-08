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
package forge.gui;

import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A {@link JLabel} with support for multi-line text that wraps when the line
 * doesn't fit in the available width. Multi-line text support is handled by the
 * {@link MultiLineLabelUI}, the default UI delegate of this component. The text
 * in the label can be horizontally and vertically aligned, relative to the
 * bounds of the component.
 * 
 * @author Samuel Sjoberg, http://samuelsjoberg.com
 * @version 1.0.0
 */
public class MultiLineLabel extends JLabel {

    /**
     * Default serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Horizontal text alignment.
     */
    private int halign = SwingConstants.LEFT;

    /**
     * Vertical text alignment.
     */
    private int valign = SwingConstants.CENTER;

    /**
     * Cache to save heap allocations.
     */
    private Rectangle bounds;

    /**
     * Creates a new empty label.
     */
    public MultiLineLabel() {
        super();
        this.setUI(MultiLineLabelUI.getLabelUI());
    }

    /**
     * Creates a new label with <code>text</code> value.
     * 
     * @param text
     *            the value of the label
     */
    public MultiLineLabel(final String text) {
        this();
        this.setText(text);
    }

    /**
     * {@inheritDoc}
     * 
     * @return a {@link java.awt.Rectangle} object.
     */
    @Override
    public Rectangle getBounds() {
        if (this.bounds == null) {
            this.bounds = new Rectangle();
        }
        return super.getBounds(this.bounds);
    }

    /**
     * Set the vertical text alignment.
     * 
     * @param alignment
     *            vertical alignment
     */
    public void setVerticalTextAlignment(final int alignment) {
        this.firePropertyChange("verticalTextAlignment", this.valign, alignment);
        this.valign = alignment;
    }

    /**
     * Set the horizontal text alignment.
     * 
     * @param alignment
     *            horizontal alignment
     */
    public void setHorizontalTextAlignment(final int alignment) {
        this.firePropertyChange("horizontalTextAlignment", this.halign, alignment);
        this.halign = alignment;
    }

    /**
     * Get the vertical text alignment.
     * 
     * @return vertical text alignment
     */
    public int getVerticalTextAlignment() {
        return this.valign;
    }

    /**
     * Get the horizontal text alignment.
     * 
     * @return horizontal text alignment
     */
    public int getHorizontalTextAlignment() {
        return this.halign;
    }
}
