package forge.toolbox;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A list of FCheckBox items using Forge skin properties.
 * Call setListData() with an array of FCheckBox items to populate.
 *
 * based on code at http://www.devx.com/tips/Tip/5342
 */
@SuppressWarnings("serial")
public class FCheckBoxList<E> extends JList<E> {
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public FCheckBoxList(final boolean keepSelectionWhenFocusLost) {
        setCellRenderer(new CellRenderer<E>());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                final int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    final FCheckBox checkbox = (FCheckBox)getModel().getElementAt(index);
                    if (checkbox.isEnabled()) {
                        checkbox.setSelected(!checkbox.isSelected());
                        repaint();
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    final FCheckBox item = (FCheckBox)getSelectedValue();
                    if (null == item || !item.isEnabled()) {
                        return;
                    }

                    item.setSelected(!item.isSelected());
                    repaint();
                }
            }
        });

        if (!keepSelectionWhenFocusLost) {
            addFocusListener(new FocusListener() {
                int lastSelectedIdx;

                @Override
                public void focusLost(final FocusEvent arg0) {
                    lastSelectedIdx = Math.max(0, getSelectedIndex());
                    clearSelection();
                }

                @Override
                public void focusGained(final FocusEvent arg0) {
                    if (-1 == getSelectedIndex()) {
                        setSelectedIndex(lastSelectedIdx);
                    }
                }
            });
        }

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    protected class CellRenderer<E1> implements ListCellRenderer<E1> {
        @Override
        public Component getListCellRendererComponent(final JList<? extends E1> list, final E1 value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            final FCheckBox checkbox = (FCheckBox)value;
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }
    }
}
