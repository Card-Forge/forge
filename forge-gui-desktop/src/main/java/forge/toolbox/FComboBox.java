package forge.toolbox;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.util.List;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import forge.interfaces.IComboBox;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinnedComboBox;

@SuppressWarnings("serial")
public class FComboBox<E> extends SkinnedComboBox<E> implements IComboBox<E> {
    public enum TextAlignment {
        LEFT (SwingConstants.LEFT),
        RIGHT (SwingConstants.RIGHT),
        CENTER (SwingConstants.CENTER);
        private int value;
        private TextAlignment(final int value) { this.value = value; }
        public int getInt() { return value; }
    }
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private SkinFont skinFont;

    // CTR
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

                final JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
                lblItem.setHorizontalAlignment(textAlignment.getInt());
                return lblItem;
            }
        }
    }
}
