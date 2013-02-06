package forge.gui.toolbox;

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
public class FCheckBoxList extends JList {
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public FCheckBoxList(boolean keepSelectionWhenFocusLost) {
        setCellRenderer(new CellRenderer());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    FCheckBox checkbox = (FCheckBox) getModel().getElementAt(index);
                    checkbox.setSelected(!checkbox.isSelected());
                    repaint();
                }
            }
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    FCheckBox item = (FCheckBox)getSelectedValue();
                    if (null == item) {
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
    
    protected class CellRenderer implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            FCheckBox checkbox = (FCheckBox) value;
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }
    }
}
