package forge.toolbox;

import forge.interfaces.IComboBox;
import forge.toolbox.FComboBox.TextAlignment;
import forge.toolbox.FSkin.SkinFont;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Vector;

/** 
 * Wrapper for combo box with extra logic (either FComboBoxWrapper or FComboBoxPanel should be used instead of FComboBox so skinning works)
 *
 */
public class FComboBoxWrapper<E> implements IComboBox<E> {

    private static final ArrayList<FComboBoxWrapper<?>> allWrappers = new ArrayList<FComboBoxWrapper<?>>();

    public static void refreshAllSkins() {
        for (FComboBoxWrapper<?> wrapper : allWrappers) {
            wrapper.refreshSkin();
        }
    }

    private FComboBox<E> comboBox;
    private Object constraints;

    public FComboBoxWrapper() {
        super();
        this.comboBox = new FComboBox<E>();
        allWrappers.add(this);
    }

    public FComboBoxWrapper(E[] items) {
        super();
        this.comboBox = new FComboBox<E>(items);
        allWrappers.add(this);
    }

    public FComboBoxWrapper(Vector<E> items) {
        super();
        this.comboBox = new FComboBox<E>(items);
        allWrappers.add(this);
    }

    public FComboBoxWrapper(ComboBoxModel<E> aModel) {
        super();
        this.comboBox = new FComboBox<E>(aModel);
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

    public void setMaximumRowCount(int count) {
        this.comboBox.setMaximumRowCount(count);
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

    public void setModel(ComboBoxModel<E> aModel) {
        this.comboBox.setModel(aModel);
    }

    public void setTextAlignment(TextAlignment align) {
        this.comboBox.setTextAlignment(align);
    }

    public void setSkinFont(SkinFont skinFont) {
        this.comboBox.setSkinFont(skinFont);
    }

    @Override
    public boolean isVisible() {
        return this.comboBox.isVisible();
    }

    @Override
    public void setVisible(boolean aFlag) {
        this.comboBox.setVisible(aFlag);
    }

    @Override
    public boolean isEnabled() {
        return this.comboBox.isEnabled();
    }

    @Override
    public void setEnabled(boolean aFlag) {
        this.comboBox.setEnabled(aFlag);
    }

    public int getAutoSizeWidth() {
        return this.comboBox.getAutoSizeWidth();
    }

    public void addTo(Container container) {
        this.addTo(container, null);
    }
    public void addTo(Container container, Object constraints0) {
        container.add(this.comboBox, constraints0);
        this.constraints = constraints0;
    }

    //disguise as component for sake of rare places that need to access component in wrapper
    //use addTo instead if you want constraints remembered after refreshing skin
    public JComponent getComponent() {
        return this.comboBox;
    }

    private void refreshSkin() {
        this.comboBox = refreshComboBoxSkin(this.comboBox, this.constraints);
    }

    //refresh combo box skin by replacing it with a copy of itself
    //TODO: Figure out if there's a better way, as calling updateUI doesn't seem to work
    public static <E> FComboBox<E> refreshComboBoxSkin(FComboBox<E> comboBox) {
        return refreshComboBoxSkin(comboBox, null);
    }
    public static <E> FComboBox<E> refreshComboBoxSkin(FComboBox<E> comboBox, Object constraints) {
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
        FComboBox<E> newComboBox = new FComboBox<E>();
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

        newComboBox.setEnabled(comboBox.isEnabled());
        newComboBox.setRenderer(comboBox.getRenderer());
        newComboBox.setTextAlignment(comboBox.getTextAlignment());
        if (comboBox.getSkinFont() != null) {
            newComboBox.setSkinFont(comboBox.getSkinFont());
        }

        //replace combo box with its copy so skin updated
        parent.remove(index);
        parent.add(newComboBox, constraints, index);
        return newComboBox;
    }
}