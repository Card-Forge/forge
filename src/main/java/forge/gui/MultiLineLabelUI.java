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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.text.View;

/**
 * Label UI delegate that supports multiple lines and line wrapping. Hard line
 * breaks (<code>\n</code>) are preserved. If the dimensions of the label is too
 * small to fit all content, the string will be clipped and "..." appended to
 * the end of the visible text (similar to the default behavior of
 * <code>JLabel</code>). If used in conjunction with a {@link MultiLineLabel},
 * text alignment (horizontal and vertical) is supported. The UI delegate can be
 * used on a regular <code>JLabel</code> if text alignment isn't required. The
 * default alignment, left and vertically centered, will then be used.
 * <p/>
 * Example of usage:
 * <p/>
 * 
 * <pre>
 * JLabel myLabel = new JLabel();
 * myLabel.setUI(MultiLineLabelUI.labelUI);
 * myLabel.setText(&quot;A long label that will wrap automatically.&quot;);
 * </pre>
 * <p/>
 * <p/>
 * The line and wrapping support is implemented without using a
 * <code>View</code> to make it easy for subclasses to add custom text effects
 * by overriding {@link #paintEnabledText(JLabel, Graphics, String, int, int)}
 * and {@link #paintDisabledText(JLabel, Graphics, String, int, int)}. This
 * class is designed to be easily extended by subclasses.
 * 
 * @author Samuel Sjoberg, http://samuelsjoberg.com
 * @version 1.3.0
 */
public class MultiLineLabelUI extends BasicLabelUI implements ComponentListener {

    /**
     * Shared instance of the UI delegate.
     */
    private static LabelUI labelUI = new MultiLineLabelUI();

    /**
     * Client property key used to store the calculated wrapped lines on the
     * JLabel.
     */
    public static final String PROPERTY_KEY = "WrappedText";

    // Static references to avoid heap allocations.
    /** Constant <code>paintIconR</code>. */
    private static Rectangle paintIconR = new Rectangle();

    /** Constant <code>paintTextR</code>. */
    private static Rectangle paintTextR = new Rectangle();

    /** Constant <code>paintViewR</code>. */
    private static Rectangle paintViewR = new Rectangle();

    /** Constant <code>paintViewInsets</code>. */
    private static Insets paintViewInsets = new Insets(0, 0, 0, 0);

    /**
     * Font metrics of the JLabel being rendered.
     */
    private FontMetrics metrics;

    /**
     * Default size of the lines list.
     */
    private static int defaultSize = 4;

    /**
     * Get the shared UI instance.
     * 
     * @param c
     *            the c
     * @return a ComponentUI
     */
    public static ComponentUI createUI(final JComponent c) {
        return MultiLineLabelUI.getLabelUI();
    }

    /** {@inheritDoc} */
    @Override
    protected void uninstallDefaults(final JLabel c) {
        super.uninstallDefaults(c);
        this.clearCache(c);
    }

    /** {@inheritDoc} */
    @Override
    protected void installListeners(final JLabel c) {
        super.installListeners(c);
        c.addComponentListener(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void uninstallListeners(final JLabel c) {
        super.uninstallListeners(c);
        c.removeComponentListener(this);
    }

    /**
     * Clear the wrapped line cache.
     * 
     * @param l
     *            the label containing a cached value
     */
    protected void clearCache(final JLabel l) {
        l.putClientProperty(MultiLineLabelUI.PROPERTY_KEY, null);
    }

    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
        super.propertyChange(e);
        final String name = e.getPropertyName();
        if (name.equals("text") || "font".equals(name)) {
            this.clearCache((JLabel) e.getSource());
        }
    }

    /**
     * Calculate the paint rectangles for the icon and text for the passed
     * label.
     * 
     * @param l
     *            a label
     * @param fm
     *            the font metrics to use, or <code>null</code> to get the font
     *            metrics from the label
     * @param width
     *            label width
     * @param height
     *            label height
     */
    protected void updateLayout(final JLabel l, FontMetrics fm, final int width, final int height) {
        if (fm == null) {
            fm = l.getFontMetrics(l.getFont());
        }
        this.metrics = fm;

        final String text = l.getText();
        final Icon icon = l.getIcon();
        final Insets insets = l.getInsets(MultiLineLabelUI.paintViewInsets);

        MultiLineLabelUI.paintViewR.x = insets.left;
        MultiLineLabelUI.paintViewR.y = insets.top;
        MultiLineLabelUI.paintViewR.width = width - (insets.left + insets.right);
        MultiLineLabelUI.paintViewR.height = height - (insets.top + insets.bottom);

        MultiLineLabelUI.paintIconR.x = 0;
        MultiLineLabelUI.paintIconR.y = 0;
        MultiLineLabelUI.paintIconR.width = 0;
        MultiLineLabelUI.paintIconR.height = 0;
        MultiLineLabelUI.paintTextR.x = 0;
        MultiLineLabelUI.paintTextR.y = 0;
        MultiLineLabelUI.paintTextR.width = 0;
        MultiLineLabelUI.paintTextR.height = 0;

        this.layoutCL(l, fm, text, icon, MultiLineLabelUI.paintViewR, MultiLineLabelUI.paintIconR,
                MultiLineLabelUI.paintTextR);
    }

    /**
     * <p>
     * prepareGraphics.
     * </p>
     * 
     * @param g
     *            a {@link java.awt.Graphics} object.
     */
    protected void prepareGraphics(final Graphics g) {
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics g, final JComponent c) {

        // parent's update method fills the background
        this.prepareGraphics(g);

        final JLabel label = (JLabel) c;
        final String text = label.getText();
        final Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

        if ((icon == null) && (text == null)) {
            return;
        }

        final FontMetrics fm = g.getFontMetrics();

        this.updateLayout(label, fm, c.getWidth(), c.getHeight());

        if (icon != null) {
            icon.paintIcon(c, g, MultiLineLabelUI.paintIconR.x, MultiLineLabelUI.paintIconR.y);
        }

        if (text != null) {
            final View v = (View) c.getClientProperty("html");
            if (v != null) {
                // HTML view disables multi-line painting.
                v.paint(g, MultiLineLabelUI.paintTextR);
            } else {
                // Paint the multi line text
                this.paintTextLines(g, label, fm);
            }
        }
    }

    /**
     * Paint the wrapped text lines.
     * 
     * @param g
     *            graphics component to paint on
     * @param label
     *            the label being painted
     * @param fm
     *            font metrics for current font
     */
    protected void paintTextLines(final Graphics g, final JLabel label, final FontMetrics fm) {
        final List<String> lines = this.getTextLines(label);

        // Available component height to paint on.
        final int height = this.getAvailableHeight(label);

        int textHeight = lines.size() * fm.getHeight();
        while (textHeight > height) {
            // Remove one line until no. of visible lines is found.
            textHeight -= fm.getHeight();
        }
        MultiLineLabelUI.paintTextR.height = Math.min(textHeight, height);
        MultiLineLabelUI.paintTextR.y = this.alignmentY(label, fm, MultiLineLabelUI.paintTextR);

        final int textX = MultiLineLabelUI.paintTextR.x;
        int textY = MultiLineLabelUI.paintTextR.y;

        for (final Iterator<String> it = lines.iterator(); it.hasNext()
                && MultiLineLabelUI.paintTextR.contains(textX, textY + MultiLineLabelUI.getAscent(fm)); textY += fm
                .getHeight()) {

            String text = it.next().trim();

            if (it.hasNext()
                    && !MultiLineLabelUI.paintTextR.contains(textX,
                            textY + fm.getHeight() + MultiLineLabelUI.getAscent(fm))) {
                // The last visible row, add a clip indication.
                text = this.clip(text, fm, MultiLineLabelUI.paintTextR);
            }

            final int x = this.alignmentX(label, fm, text, MultiLineLabelUI.paintTextR);

            if (label.isEnabled()) {
                this.paintEnabledText(label, g, text, x, textY);
            } else {
                this.paintDisabledText(label, g, text, x, textY);
            }
        }
    }

    /**
     * Returns the available height to paint text on. This is the height of the
     * passed component with insets subtracted.
     * 
     * @param l
     *            a component
     * @return the available height
     */
    protected int getAvailableHeight(final JLabel l) {
        l.getInsets(MultiLineLabelUI.paintViewInsets);
        return l.getHeight() - MultiLineLabelUI.paintViewInsets.top - MultiLineLabelUI.paintViewInsets.bottom;
    }

    /**
     * Add a clip indication to the string. It is important that the string
     * length does not exceed the length or the original string.
     * 
     * @param text
     *            the to be painted
     * @param fm
     *            font metrics
     * @param bounds
     *            the text bounds
     * @return the clipped string
     */
    protected String clip(final String text, final FontMetrics fm, final Rectangle bounds) {
        // Fast and lazy way to insert a clip indication is to simply replace
        // the last characters in the string with the clip indication.
        // A better way would be to use metrics and calculate how many (if any)
        // characters that need to be replaced.
        if (text.length() < 3) {
            return "...";
        }
        return text.substring(0, text.length() - 3) + "...";
    }

    /**
     * Establish the vertical text alignment. The default alignment is to center
     * the text in the label.
     * 
     * @param label
     *            the label to paint
     * @param fm
     *            font metrics
     * @param bounds
     *            the text bounds rectangle
     * @return the vertical text alignment, defaults to CENTER.
     */
    protected int alignmentY(final JLabel label, final FontMetrics fm, final Rectangle bounds) {
        final int height = this.getAvailableHeight(label);
        final int textHeight = bounds.height;

        if (label instanceof MultiLineLabel) {
            final int align = ((MultiLineLabel) label).getVerticalTextAlignment();
            switch (align) {
            case SwingConstants.TOP:
                return MultiLineLabelUI.getAscent(fm) + MultiLineLabelUI.paintViewInsets.top;
            case SwingConstants.BOTTOM:
                return (((MultiLineLabelUI.getAscent(fm) + height) - MultiLineLabelUI.paintViewInsets.top) + MultiLineLabelUI.paintViewInsets.bottom)
                        - textHeight;
            default:
            }
        }

        // Center alignment
        final int textY = MultiLineLabelUI.paintViewInsets.top + ((height - textHeight) / 2)
                + MultiLineLabelUI.getAscent(fm);
        return Math.max(textY, MultiLineLabelUI.getAscent(fm) + MultiLineLabelUI.paintViewInsets.top);
    }

    /**
     * <p>
     * getAscent.
     * </p>
     * 
     * @param fm
     *            a {@link java.awt.FontMetrics} object.
     * @return a int.
     */
    private static int getAscent(final FontMetrics fm) {
        return fm.getAscent() + fm.getLeading();
    }

    /**
     * Establish the horizontal text alignment. The default alignment is left
     * aligned text.
     * 
     * @param label
     *            the label to paint
     * @param fm
     *            font metrics
     * @param s
     *            the string to paint
     * @param bounds
     *            the text bounds rectangle
     * @return the x-coordinate to use when painting for proper alignment
     */
    protected int alignmentX(final JLabel label, final FontMetrics fm, final String s, final Rectangle bounds) {
        if (label instanceof MultiLineLabel) {
            final int align = ((MultiLineLabel) label).getHorizontalTextAlignment();
            switch (align) {
            case SwingConstants.RIGHT:
                return (bounds.x + MultiLineLabelUI.paintViewR.width) - fm.stringWidth(s);
            case SwingConstants.CENTER:
                return (bounds.x + (MultiLineLabelUI.paintViewR.width / 2)) - (fm.stringWidth(s) / 2);
            default:
                return bounds.x;
            }
        }
        return bounds.x;
    }

    /**
     * Check the given string to see if it should be rendered as HTML. Code
     * based on implementation found in
     * <code>BasicHTML.isHTMLString(String)</code> in future JDKs.
     * 
     * @param s
     *            the string
     * @return <code>true</code> if string is HTML, otherwise <code>false</code>
     */
    private static boolean isHTMLString(final String s) {
        if (s != null) {
            if ((s.length() >= 6) && (s.charAt(0) == '<') && (s.charAt(5) == '>')) {
                final String tag = s.substring(1, 5);
                return tag.equalsIgnoreCase("html");
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize(final JComponent c) {
        final Dimension d = super.getPreferredSize(c);
        final JLabel label = (JLabel) c;

        if (MultiLineLabelUI.isHTMLString(label.getText())) {
            return d; // HTML overrides everything and we don't need to process
        }

        // Width calculated by super is OK. The preferred width is the width of
        // the unwrapped content as long as it does not exceed the width of the
        // parent container.

        if (c.getParent() != null) {
            // Ensure that preferred width never exceeds the available width
            // (including its border insets) of the parent container.
            final Insets insets = c.getParent().getInsets();
            final Dimension size = c.getParent().getSize();
            if (size.width > 0) {
                // If width isn't set component shouldn't adjust.
                d.width = size.width - insets.left - insets.right;
            }
        }

        this.updateLayout(label, null, d.width, d.height);

        // The preferred height is either the preferred height of the text
        // lines, or the height of the icon.
        d.height = Math.max(d.height, this.getPreferredHeight(label));

        return d;
    }

    /**
     * The preferred height of the label is the height of the lines with added
     * top and bottom insets.
     * 
     * @param label
     *            the label
     * @return the preferred height of the wrapped lines.
     */
    protected int getPreferredHeight(final JLabel label) {
        final int numOfLines = this.getTextLines(label).size();
        final Insets insets = label.getInsets(MultiLineLabelUI.paintViewInsets);
        return (numOfLines * this.metrics.getHeight()) + insets.top + insets.bottom;
    }

    /**
     * Get the lines of text contained in the text label. The prepared lines is
     * cached as a client property, accessible via {@link #PROPERTY_KEY}.
     * 
     * @param l
     *            the label
     * @return the text lines of the label.
     */
    @SuppressWarnings("unchecked")
    protected List<String> getTextLines(final JLabel l) {
        List<String> lines = (List<String>) l.getClientProperty(MultiLineLabelUI.PROPERTY_KEY);
        if (lines == null) {
            lines = this.prepareLines(l);
            l.putClientProperty(MultiLineLabelUI.PROPERTY_KEY, lines);
        }
        return lines;
    }

    /** {@inheritDoc} */
    @Override
    public void componentHidden(final ComponentEvent e) {
        // Don't care
    }

    /** {@inheritDoc} */
    @Override
    public void componentMoved(final ComponentEvent e) {
        // Don't care
    }

    /** {@inheritDoc} */
    @Override
    public void componentResized(final ComponentEvent e) {
        this.clearCache((JLabel) e.getSource());
    }

    /** {@inheritDoc} */
    @Override
    public void componentShown(final ComponentEvent e) {
        // Don't care
    }

    /**
     * Prepare the text lines for rendering. The lines are wrapped to fit in the
     * current available space for text. Explicit line breaks are preserved.
     * 
     * @param l
     *            the label to render
     * @return a list of text lines to render
     */
    protected List<String> prepareLines(final JLabel l) {
        final List<String> lines = new ArrayList<String>(MultiLineLabelUI.defaultSize);
        final String text = l.getText();
        if (text == null) {
            return null; // Null guard
        }
        final PlainDocument doc = new PlainDocument();
        try {
            doc.insertString(0, text, null);
        } catch (final BadLocationException e) {
            return null;
        }
        final Element root = doc.getDefaultRootElement();
        for (int i = 0, j = root.getElementCount(); i < j; i++) {
            this.wrap(lines, root.getElement(i));
        }
        return lines;
    }

    /**
     * If necessary, wrap the text into multiple lines.
     * 
     * @param lines
     *            line array in which to store the wrapped lines
     * @param elem
     *            the document element containing the text content
     */
    protected void wrap(final List<String> lines, final Element elem) {
        final int p1 = elem.getEndOffset();
        final Document doc = elem.getDocument();
        for (int p0 = elem.getStartOffset(); p0 < p1;) {
            final int p = this.calculateBreakPosition(doc, p0, p1);
            try {
                lines.add(doc.getText(p0, p - p0));
            } catch (final BadLocationException e) {
                throw new Error("Can't get line text. p0=" + p0 + " p=" + p);
            }
            p0 = (p == p0) ? p1 : p;
        }
    }

    /**
     * Calculate the position on which to break (wrap) the line.
     * 
     * @param doc
     *            the document
     * @param p0
     *            start position
     * @param p1
     *            end position
     * @return the actual end position, will be <code>p1</code> if content does
     *         not need to wrap, otherwise it will be less than <code>p1</code>.
     */
    protected int calculateBreakPosition(final Document doc, final int p0, final int p1) {
        final Segment segment = SegmentCache.getSegment();
        try {
            doc.getText(p0, p1 - p0, segment);
        } catch (final BadLocationException e) {
            throw new Error("Can't get line text");
        }

        final int width = MultiLineLabelUI.paintTextR.width;
        final int p = p0 + Utilities.getBreakLocation(segment, this.metrics, 0, width, null, p0);
        SegmentCache.releaseSegment(segment);
        return p;
    }

    /**
     * Gets the label ui.
     * 
     * @return the labelUI
     */
    public static LabelUI getLabelUI() {
        return MultiLineLabelUI.labelUI;
    }

    /**
     * Sets the label ui.
     * 
     * @param labelUI
     *            the new label ui
     */
    public static void setLabelUI(final LabelUI labelUI) {
        MultiLineLabelUI.labelUI = labelUI;
    }

    /**
     * Static singleton {@link Segment} cache.
     * 
     * @author Samuel Sjoberg
     * @see javax.swing.text.SegmentCache
     */
    protected static final class SegmentCache {

        /**
         * Reused segments.
         */
        private final ArrayList<Segment> segments = new ArrayList<Segment>(2);

        /**
         * Singleton instance.
         */
        private static SegmentCache cache = new SegmentCache();

        /**
         * Private constructor.
         */
        private SegmentCache() {
        }

        /**
         * Returns a <code>Segment</code>. When done, the <code>Segment</code>
         * should be recycled by invoking {@link #releaseSegment(Segment)}.
         * 
         * @return a <code>Segment</code>.
         */
        public static Segment getSegment() {
            final int size = SegmentCache.cache.segments.size();
            if (size > 0) {
                return SegmentCache.cache.segments.remove(size - 1);
            }
            return new Segment();
        }

        /**
         * Releases a <code>Segment</code>. A segment should not be used after
         * it is released, and a segment should never be released more than
         * once.
         * 
         * @param segment
         *            the segment
         */
        public static void releaseSegment(final Segment segment) {
            segment.array = null;
            segment.count = 0;
            SegmentCache.cache.segments.add(segment);
        }
    }
}
