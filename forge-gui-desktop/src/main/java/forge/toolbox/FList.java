package forge.toolbox;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import forge.Singletons;
import forge.toolbox.FSkin.SkinnedList;

/**
 * A JList object using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FList<E> extends SkinnedList<E> {
    private static final EmptyBorder itemBorder = new EmptyBorder(4, 3, 4, 3);

    public FList() {
        super();
        initialize();
    }

    /**
     * A JList object using Forge skin properties.
     * This constructor assumes list contents are null and will be set later.
     * This constructor is used for applying a list model at instantiation.
     * @param model0 &emsp; {@link javax.swing.ListModel}
     */
    public FList(final ListModel<E> model0) {
        super(model0);
        initialize();
    }

    private void initialize() {
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setSelectionForeground(this.getSkin().getForeground());
        this.setFont(FSkin.getFont(12));
        this.setCellRenderer(new ComplexCellRenderer<E>());

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent arg0) {
                updateSelectionBackground();
            }

            @Override
            public void focusLost(final FocusEvent arg0) {
                updateSelectionBackground();
            }
        });
        updateSelectionBackground();
    }

    private void updateSelectionBackground() {
        this.setSelectionBackground(FSkin.getColor(hasFocus() ? FSkin.Colors.CLR_ACTIVE : FSkin.Colors.CLR_INACTIVE));
    }

    public int getAutoSizeWidth() {
        final FontMetrics metrics = this.getFontMetrics(this.getFont());
        int width = 0;
        for (int i = 0; i < this.getModel().getSize(); i++) {
            final int itemWidth = metrics.stringWidth(this.getModel().getElementAt(i).toString());
            if (itemWidth > width) {
                width = itemWidth;
            }
        }
        width += itemBorder.getBorderInsets().left + itemBorder.getBorderInsets().right; //account for item border insets

        final int minWidth = 150;
        if (width < minWidth) {
            width = minWidth;
        }
        else {
            final int maxWidth = Singletons.getView().getFrame().getWidth() - 50;
            if (width > maxWidth) {
                width = maxWidth;
            }
        }
        return width;
    }

    public int getCount() {
        return getModel().getSize();
    }

    private class ComplexCellRenderer<E1> implements ListCellRenderer<E1> {
        private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(final JList<? extends E1> lst0, final E1 val0, final int i0,
                final boolean isSelected, final boolean cellHasFocus) {

            final JLabel lblItem = (JLabel) defaultRenderer.getListCellRendererComponent(
                    lst0, val0, i0, isSelected, cellHasFocus);
            lblItem.setBorder(itemBorder);
            return lblItem;
        }
    }
}
