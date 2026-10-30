package forge.toolbox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import forge.gui.interfaces.IComboBox;
import forge.gui.util.ListHeading;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinnedComboBox;

@SuppressWarnings("serial")
public class FComboBox<E> extends SkinnedComboBox<E> implements IComboBox<E> {
    public enum TextAlignment {
        LEFT (SwingConstants.LEFT),
        RIGHT (SwingConstants.RIGHT),
        CENTER (SwingConstants.CENTER);
        private int value;
        TextAlignment(final int value) { this.value = value; }
        public int getInt() { return value; }
    }
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private SkinFont skinFont;
    private boolean grouped;

    public FComboBox() {
        super();
        initialize();
    }
    public FComboBox(final ComboBoxModel<E> model) {
        super(model);
        initialize();
    }
    public FComboBox(final E[] items) {
        super(items);
        initialize();
    }
    public FComboBox(final List<E> items) {
        this(new Vector<>(items));
    }
    public FComboBox(final Vector<E> items) {
        super(items);
        initialize();
    }

    private void initialize() {
        setUI(new FComboBoxUI());
        setBorder(getDefaultBorder());
        grouped = hasHeadings();
        // A grouped list is short enough to show whole, and scrolling would hide its headings
        if (grouped) {
            setMaximumRowCount(getItemCount());
        }
    }

    private boolean hasHeadings() {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i) instanceof ListHeading) {
                return true;
            }
        }
        return false;
    }

    private static Border getDefaultBorder() {
        return UIManager.getBorder("ComboBox.border");
    }

    public String getText() {
        final Object selectedItem = getSelectedItem();
        if (selectedItem == null) {
            return "";
        }
        return selectedItem.toString();
    }
    public void setText(final String text0) {
        setSelectedItem(null);
        dataModel.setSelectedItem(text0); //use this to get around inability to set selected item that's not in items
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(final TextAlignment align) {
        textAlignment = align;
    }

    public SkinFont getSkinFont() {
        return this.skinFont;
    }

    public void setSkinFont(final SkinFont skinFont0) {
        this.skinFont = skinFont0;
        this.setFont(skinFont0);
    }

    public int getAutoSizeWidth() {
        int maxWidth = 0;
        final FontMetrics metrics = this.getFontMetrics(this.getFont());
        for (int i = 0; i < this.getItemCount(); i++) {
            final int width = metrics.stringWidth(this.getItemAt(i).toString());
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth + 28; //leave room for arrow and padding
    }

    @SuppressWarnings("unchecked")
    @Override
    public E getSelectedItem() {
        return (E)super.getSelectedItem();
    }

    @Override
    public void setSelectedItem(final Object item) {
        if (item instanceof ListHeading) {
            return;
        }
        super.setSelectedItem(item);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2d = (Graphics2D)g;
        g2d.setPaint(getForeground());
        int shapeWidth = 8;
        int shapeHeight = 8;
        int x = getWidth() - shapeWidth - 6;
        int y = getHeight() / 2 - 1;
        if (getHeight() > 26) { //increase arrow size if taller combo box
            shapeWidth += 2;
            shapeHeight += 2;
            x -= 4;
            y--;
        }
        final int[] xPoints = {x, x + shapeWidth, x + (shapeWidth / 2)};
        final int[] yPoints = {y, y, y + (shapeHeight / 2)};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private class FComboBoxUI extends BasicComboBoxUI {
        @Override
        protected LayoutManager createLayoutManager() {
            return super.createLayoutManager();
        }

        @Override
        protected ComboPopup createPopup() {
            final ComboPopup p = super.createPopup();
            final JComponent c = (JComponent)p;
            c.setBorder(getDefaultBorder());
            return p;
        }

        @Override
        public void paintCurrentValue(final Graphics g, final Rectangle bounds, final boolean hasFocus) {
            if (comboBox.isEnabled()) {
                super.paintCurrentValue(g, bounds, hasFocus);
                return;
            }
            // The look and feel's disabled colours leave the value unreadable on Forge's skins
            final Component c = comboBox.getRenderer().getListCellRendererComponent(
                    listBox, comboBox.getSelectedItem(), -1, false, false);
            final Color text = comboBox.getForeground();
            c.setFont(comboBox.getFont());
            c.setForeground(new Color(text.getRed(), text.getGreen(), text.getBlue(), 150));
            c.setBackground(comboBox.getBackground());
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
        }

        @Override
        protected JButton createArrowButton() {
            return new JButton() { //return button that takes up no space
                @Override
                public int getWidth() {
                    return 0;
                }
            };
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected ListCellRenderer createRenderer() {
            return new CustomCellRenderer<>();
        }

        @SuppressWarnings("hiding")
        private class CustomCellRenderer<E> implements ListCellRenderer<E> {
            private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

            @Override
            public Component getListCellRendererComponent(
                    final JList<? extends E> list, final E value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {

                final Color text = list.getForeground();
                if (value instanceof ListHeading) {
                    final JLabel lblHeading = (JLabel) defaultRenderer.getListCellRendererComponent(
                            list, value, index, false, false);
                    lblHeading.setForeground(new Color(text.getRed(), text.getGreen(), text.getBlue(), 160));
                    lblHeading.setFont(list.getFont().deriveFont(Font.PLAIN, list.getFont().getSize2D() - 3));
                    final Border padding = new EmptyBorder(8, 6, 2, 6);
                    lblHeading.setBorder(index <= 0 ? padding : new CompoundBorder(
                            new MatteBorder(1, 0, 0, 0, new Color(text.getRed(), text.getGreen(), text.getBlue(), 70)), padding));
                    return lblHeading;
                }

                final JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                final boolean indented = grouped && index > 0 && isUnderHeading(list, index);
                lblItem.setBorder(new EmptyBorder(4, indented ? 18 : 3, 4, 3));
                if (index >= 0 && grouped) {
                    // Plain and a step smaller, so the headings read as labels above the choices
                    lblItem.setFont(list.getFont().deriveFont(Font.PLAIN, list.getFont().getSize2D() - 1));
                    lblItem.setHorizontalTextPosition(SwingConstants.LEADING);
                    lblItem.setIconTextGap(10);
                    if (value == FComboBox.this.getSelectedItem()) {
                        lblItem.setIcon(new CheckIcon(lblItem.getForeground()));
                    }
                }
                lblItem.setHorizontalAlignment(textAlignment.getInt());
                return lblItem;
            }

            private boolean isUnderHeading(final JList<? extends E> list, final int index) {
                for (int i = index - 1; i >= 0; i--) {
                    if (list.getModel().getElementAt(i) instanceof ListHeading) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private static final class CheckIcon implements Icon {
        private final Color color;

        private CheckIcon(final Color color0) {
            color = color0;
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawPolyline(new int[] {x + 1, x + 4, x + 10}, new int[] {y + 6, y + 9, y + 2}, 3);
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}
