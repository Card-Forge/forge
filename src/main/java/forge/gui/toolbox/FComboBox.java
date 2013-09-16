package forge.gui.toolbox;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;

/** 
 * Wrapper for combo box with extra logic (should be used instead of JComboBox)
 *
 */
@SuppressWarnings("serial")
public class FComboBox<E> extends JComponent {

    private JComboBox<E> innerComboBox;
    private static ArrayList<FComboBox<?>> comboBoxes = new ArrayList<FComboBox<?>>();

    public static void refreshAllSkins() {
        for (FComboBox<?> comboBox : comboBoxes) {
            comboBox.refreshSkin();
        }
    }
    
    public FComboBox() {
        super();
        this.setOpaque(false);
        innerComboBox = new JComboBox<E>();
        this.add(innerComboBox);
        comboBoxes.add(this);
    }
    
    public void addItem(E item) {
        this.innerComboBox.addItem(item);
    }
    
    public void removeItem(E item) {
        this.innerComboBox.removeItem(item);
    }
    
    public void removeAllItems() {
        this.innerComboBox.removeAllItems();
    }
    
    public Object getSelectedItem() {
        return this.innerComboBox.getSelectedItem();
    }
    
    public void setSelectedItem(Object item) {
        this.innerComboBox.setSelectedItem(item);
    }
    
    public int getSelectedIndex() {
        return this.innerComboBox.getSelectedIndex();
    }
    
    public void setSelectedIndex(int index) {
        this.innerComboBox.setSelectedIndex(index);
    }
    
    public int getItemCount() {
        return this.innerComboBox.getItemCount();
    }
    
    public E getItemAt(int index) {
        return this.innerComboBox.getItemAt(index);
    }
    
    public void addActionListener(ActionListener l) {
        this.innerComboBox.addActionListener(l);
    }
    
    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        this.innerComboBox.setRenderer(aRenderer);
    }
    
    public void refreshSkin() {
        //clone inner combo box
        JComboBox<E> newInnerComboBox = new JComboBox<E>();
        for (int i = 0; i < this.getItemCount(); i++) {
            newInnerComboBox.addItem(this.getItemAt(i));
        }
        newInnerComboBox.setSelectedIndex(this.getSelectedIndex());
        ActionListener[] listeners = this.innerComboBox.getActionListeners();
        for (ActionListener l : listeners) {
            newInnerComboBox.addActionListener(l);
        }
        newInnerComboBox.setRenderer(this.innerComboBox.getRenderer());
        
        //replace inner combo box with its clone
        this.remove(innerComboBox);
        this.innerComboBox = newInnerComboBox;
        this.add(innerComboBox);
    }
}
