package forge.gui.toolbox;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import forge.gui.MouseUtil;
import forge.gui.MouseUtil.MouseCursor;

@SuppressWarnings("serial")
public class FComboBox<E> extends JComboBox<E> {

    public enum TextAlignment {
        LEFT (SwingConstants.LEFT),
        RIGHT (SwingConstants.RIGHT),
        CENTER (SwingConstants.CENTER);
        private int value;
        private TextAlignment(int value) { this.value = value; }
        public int getInt() { return value; }
    }
    private TextAlignment textAlignment = TextAlignment.LEFT;

    private boolean showComboButton = true;
    private MouseAdapter mouseListener = null;

    // CTR
    public FComboBox() {
        super();
        initialize();
    }
    public FComboBox(ComboBoxModel<E> model) {
        super(model);
        initialize();
    }
    public FComboBox(E[] items) {
        super(items);
        initialize();
    }

    private void initialize() {
        setUI(new FComboBoxUI());
        setBorder(getDefaultBorder());
        setMouseListener();
    }

    private Border getDefaultBorder() {
        return UIManager.getBorder("ComboBox.border");
    }

    private Color getBorderColor() {
        Border thisBorder = getDefaultBorder();
        if (thisBorder != null && thisBorder instanceof LineBorder) {
            return ((LineBorder)thisBorder).getLineColor();
        } else {
            return getForeground();
        }
    }

    public void setButtonVisible(boolean isVisible) {
        showComboButton = isVisible;
        initialize();
    }

    private void setMouseListener() {
        if (mouseListener != null) {
            removeMouseListener(mouseListener);
        }
        if (!showComboButton) {
            mouseListener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setMouseCursor(Cursor.HAND_CURSOR);
                    setBorderHighlight(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    setMouseCursor(Cursor.DEFAULT_CURSOR);
                    setBorderHighlight(false);
                }
            };
            addMouseListener(mouseListener);
        }
    }

    private void setBorderHighlight(boolean highlight) {
        if (isEnabled()) {
            if (highlight) {
                setBorder(BorderFactory.createLineBorder(getForeground(), 2));
            } else {
                setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
            }
        }
    }

    private void setMouseCursor(int cursor) {
        // Testing...
        //SwingUtilities.getWindowAncestor(this).setCursor(Cursor.getPredefinedCursor(cursor));
        // Forge...
        MouseUtil.setMouseCursor(MouseCursor.fromInt(cursor));
    }

    public void setTextAlignment(TextAlignment align) {
        textAlignment = align;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setPaint(getForeground());
        int shapeWidth = 10;
        int shapeHeight = 10;
        int x = getWidth() - shapeWidth - 8;
        int y = getHeight() / 2 - 2;
        int[] xPoints = {x, x + shapeWidth, x + (shapeWidth/2)};
        int[] yPoints = {y, y, y + (shapeHeight/2)};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private class FComboBoxUI extends BasicComboBoxUI {

        @Override
        protected LayoutManager createLayoutManager() {
            if (!showComboButton) {
                return new BorderLayout();
            } else {
                return super.createLayoutManager();
            }
        }

        @Override
        protected ComboPopup createPopup() {
            ComboPopup p = super.createPopup();
            JComponent c = (JComponent)p;
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

            private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends E> list, E value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                lblItem.setBorder(new EmptyBorder(4, 3, 4, 3));
                lblItem.setHorizontalAlignment(textAlignment.getInt());
                return lblItem;
            }
        }
    }

}
