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
        comboBox = new FComboBox<E>();
        allWrappers.add(this);
    }

    public FComboBoxWrapper(E[] items) {
        super();
        comboBox = new FComboBox<E>(items);
        allWrappers.add(this);
    }

    public FComboBoxWrapper(Vector<E> items) {
        super();
        comboBox = new FComboBox<E>(items);
        allWrappers.add(this);
    }

    public FComboBoxWrapper(ComboBoxModel<E> aModel) {
        super();
        comboBox = new FComboBox<E>(aModel);
        allWrappers.add(this);
    }

    public void addItem(E item) {
        comboBox.addItem(item);
    }

    public void removeItem(E item) {
        comboBox.removeItem(item);
    }

    public void removeAllItems() {
        comboBox.removeAllItems();
    }

    @SuppressWarnings("unchecked")
    public E getSelectedItem() {
        Object res = comboBox.getSelectedItem();
        return res == null ? null : (E) res;
    }

    public void setSelectedItem(Object item) {
        comboBox.setSelectedItem(item);
    }

    public int getSelectedIndex() {
        return comboBox.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        comboBox.setSelectedIndex(index);
    }

    public String getText() {
        return comboBox.getText();
    }
    public void setText(String text0) {
        comboBox.setText(text0);
    }

    public void setMaximumRowCount(int count) {
        comboBox.setMaximumRowCount(count);
    }

    public int getItemCount() {
        return comboBox.getItemCount();
    }

    public E getItemAt(int index) {
        return comboBox.getItemAt(index);
    }

    public void addActionListener(ActionListener l) {
        comboBox.addActionListener(l);
    }

    public void addItemListener(ItemListener l) {
        comboBox.addItemListener(l);
    }

    public void addKeyListener(KeyListener l) {
        comboBox.addKeyListener(l);
    }

    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        comboBox.setRenderer(aRenderer);
    }

    public void setModel(ComboBoxModel<E> aModel) {
        comboBox.setModel(aModel);
    }

    public void setTextAlignment(TextAlignment align) {
        comboBox.setTextAlignment(align);
    }

    public void setSkinFont(SkinFont skinFont) {
        comboBox.setSkinFont(skinFont);
    }

    @Override
    public boolean isVisible() {
        return comboBox.isVisible();
    }

    @Override
    public void setVisible(boolean aFlag) {
        comboBox.setVisible(aFlag);
    }

    @Override
    public boolean isEnabled() {
        return comboBox.isEnabled();
    }

    @Override
    public void setEnabled(boolean aFlag) {
        comboBox.setEnabled(aFlag);
    }

    public int getAutoSizeWidth() {
        return comboBox.getAutoSizeWidth();
    }

    public void addTo(Container container) {
        addTo(container, null);
    }
    public void addTo(Container container, Object constraints0) {
        container.add(comboBox, constraints0);
        constraints = constraints0;
    }

    //disguise as component for sake of rare places that need to access component in wrapper
    //use addTo instead if you want constraints remembered after refreshing skin
    public JComponent getComponent() {
        return comboBox;
    }

    private void refreshSkin() {
        comboBox = refreshComboBoxSkin(comboBox, constraints);
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