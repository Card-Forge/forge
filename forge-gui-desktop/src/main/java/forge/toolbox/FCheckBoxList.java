package forge.toolbox;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * A list of FCheckBox items using Forge skin properties.
 * Call setListData() with an array of FCheckBox items to populate.
 * 
 * based on code at http://www.devx.com/tips/Tip/5342
 */
@SuppressWarnings("serial")
public class FCheckBoxList<E> extends JList<E> {
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public FCheckBoxList(boolean keepSelectionWhenFocusLost) {
        setCellRenderer(new CellRenderer<E>());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    FCheckBox checkbox = (FCheckBox)getModel().getElementAt(index);
                    if (checkbox.isEnabled()) {
                        checkbox.setSelected(!checkbox.isSelected());
                        repaint();
                    }
                }
            }
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    FCheckBox item = (FCheckBox)getSelectedValue();
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
                public void focusLost(FocusEvent arg0) {
                    lastSelectedIdx = Math.max(0, getSelectedIndex());
                    clearSelection();
                }
                
                @Override
                public void focusGained(FocusEvent arg0) {
                    if (-1 == getSelectedIndex()) {
                        setSelectedIndex(lastSelectedIdx);
                    }
                }
            });
        }

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    protected class CellRenderer<E1> implements ListCellRenderer<E1> {
        public Component getListCellRendererComponent(JList<? extends E1> list, E1 value, int index, boolean isSelected, boolean cellHasFocus) {
            FCheckBox checkbox = (FCheckBox)value;
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
