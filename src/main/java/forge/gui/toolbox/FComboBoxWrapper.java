package forge.gui.toolbox;

import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;

/** 
 * Wrapper for combo box with extra logic (either FComboBoxWrapper or FComboBoxPanel should be used instead of JComboBox so skinning works)
 *
 */
public class FComboBoxWrapper<E> {

    private static ArrayList<FComboBoxWrapper<?>> allWrappers = new ArrayList<FComboBoxWrapper<?>>();

    public static void refreshAllSkins() {
        for (FComboBoxWrapper<?> wrapper : allWrappers) {
            wrapper.refreshSkin();
        }
    }

    private JComboBox<E> comboBox;
    private Object constraints;
    
    public FComboBoxWrapper() {
        super();
        this.comboBox = new JComboBox<E>();
        allWrappers.add(this);
    }
    
    public FComboBoxWrapper(E[] items) {
        super();
        this.comboBox = new JComboBox<E>(items);
        allWrappers.add(this);
    }
    
    public FComboBoxWrapper(Vector<E> items) {
        super();
        this.comboBox = new JComboBox<E>(items);
        allWrappers.add(this);
    }
    
    public FComboBoxWrapper(ComboBoxModel<E> aModel) {
        super();
        this.comboBox = new JComboBox<E>(aModel);
        allWrappers.add(this);
    }
    
    public void addItem(E item) {
        this.comboBox.addItem(item);
    }
    
    public void removeItem(E item) {
        this.comboBox.removeItem(item);
    }
    
    public void removeAllItems() {
        this.comboBox.removeAllItems();
    }
    
    @SuppressWarnings("unchecked")
    public E getSelectedItem() {
        Object res = this.comboBox.getSelectedItem();
        return res == null ? null : (E) res;
    }
    
    public void setSelectedItem(Object item) {
        this.comboBox.setSelectedItem(item);
    }
    
    public int getSelectedIndex() {
        return this.comboBox.getSelectedIndex();
    }
    
    public void setSelectedIndex(int index) {
        this.comboBox.setSelectedIndex(index);
    }
    
    public int getItemCount() {
        return this.comboBox.getItemCount();
    }
    
    public E getItemAt(int index) {
        return this.comboBox.getItemAt(index);
    }
    
    public void addActionListener(ActionListener l) {
        this.comboBox.addActionListener(l);
    }
    
    public void addItemListener(ItemListener l) {
        this.comboBox.addItemListener(l);
    }
    
    public void addKeyListener(KeyListener l) {
        this.comboBox.addKeyListener(l);
    }
    
    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        this.comboBox.setRenderer(aRenderer);
    }
    
    public void setVisible(boolean aFlag) {
        this.comboBox.setVisible(aFlag);
    }
    
    public void setEnabled(boolean aFlag) {
        this.comboBox.setEnabled(aFlag);
    }
    
    public void addTo(Container container) {
        addTo(container, null);
    }
    
    public void addTo(Container container, Object constraints0) {
        container.add(this.comboBox, constraints0);
        this.constraints = constraints0;
    }
    
    private void refreshSkin() {
        this.comboBox = refreshComboBoxSkin(this.comboBox, this.constraints);
    }
    
    //refresh combo box skin by replacing it with a copy of itself
    //TODO: Figure out if there's a better way, as calling updateUI doesn't seem to work
    public static <E> JComboBox<E> refreshComboBoxSkin(JComboBox<E> comboBox) {
        return refreshComboBoxSkin(comboBox, null);
    }
    public static <E> JComboBox<E> refreshComboBoxSkin(JComboBox<E> comboBox, Object constraints) {
        //find index of combo box within parent
        Container parent = comboBox.getParent();
        if (parent == null) { return comboBox; }

        int index;
        for (index = 0; index < parent.getComponentCount(); index++) {
            if (parent.getComponent(index) == comboBox) {
                break;
            }
        }
        
        //create copy of combo box
        JComboBox<E> newComboBox = new JComboBox<E>();
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            newComboBox.addItem(comboBox.getItemAt(i));
        }
        newComboBox.setSelectedIndex(comboBox.getSelectedIndex());
        
        ActionListener[] actionListeners = newComboBox.getActionListeners();
        for (ActionListener l : actionListeners) {
            newComboBox.removeActionListener(l); //remove default action listeners to prevent duplicates
        }
        actionListeners = comboBox.getActionListeners();
        for (ActionListener l : actionListeners) {
            newComboBox.addActionListener(l);
        }

        ItemListener[] itemListeners = newComboBox.getItemListeners();
        for (ItemListener l : itemListeners) {
            newComboBox.removeItemListener(l); //remove default item listener to prevent duplicates
        }
        itemListeners = comboBox.getItemListeners();
        for (ItemListener l : itemListeners) {
            newComboBox.addItemListener(l);
        }

        KeyListener[] keyListeners = newComboBox.getKeyListeners();
        for (KeyListener l : keyListeners) {
            newComboBox.removeKeyListener(l); //remove default key listeners to prevent duplicates
        }
        keyListeners = comboBox.getKeyListeners();
        for (KeyListener l : keyListeners) {
            newComboBox.addKeyListener(l);
        }

        newComboBox.setRenderer(comboBox.getRenderer());
        
        //replace combo box with its copy so skin updated
        parent.remove(index);
        parent.add(newComboBox, constraints, index);
        return newComboBox;
    }
}