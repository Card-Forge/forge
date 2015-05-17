package forge.toolbox;

import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;

import com.google.common.collect.ObjectArrays;

import forge.interfaces.IComboBox;
import forge.toolbox.FComboBox.TextAlignment;
import forge.toolbox.FSkin.SkinFont;

/**
 * Wrapper for combo box with extra logic (either FComboBoxWrapper or FComboBoxPanel should be used instead of FComboBox so skinning works)
 *
 */
public class FComboBoxWrapper<E> implements IComboBox<E> {

    private static final List<FComboBoxWrapper<?>> allWrappers = new ArrayList<FComboBoxWrapper<?>>();

    public static void refreshAllSkins() {
        for (final FComboBoxWrapper<?> wrapper : allWrappers) {
            wrapper.refreshSkin();
        }
    }

    private FComboBox<E> comboBox;
    private ActionListener[] suppressedActionListeners = null;
    private Object constraints;

    public FComboBoxWrapper() {
        super();
        comboBox = new FComboBox<E>();
        allWrappers.add(this);
    }

    @Override
    public void addItem(final E item) {
        comboBox.addItem(item);
    }

    public void removeItem(final E item) {
        comboBox.removeItem(item);
    }

    @Override
    public void removeAllItems() {
        comboBox.removeAllItems();
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getSelectedItem() {
        final Object res = comboBox.getSelectedItem();
        return res == null ? null : (E) res;
    }

    @Override
    public void setSelectedItem(final Object item) {
        comboBox.setSelectedItem(item);
    }

    @Override
    public int getSelectedIndex() {
        return comboBox.getSelectedIndex();
    }

    @Override
    public void setSelectedIndex(final int index) {
        comboBox.setSelectedIndex(index);
    }

    public String getText() {
        return comboBox.getText();
    }
    public void setText(final String text0) {
        comboBox.setText(text0);
    }

    public void setMaximumRowCount(final int count) {
        comboBox.setMaximumRowCount(count);
    }

    public int getItemCount() {
        return comboBox.getItemCount();
    }

    public E getItemAt(final int index) {
        return comboBox.getItemAt(index);
    }

    public void addActionListener(final ActionListener l) {
        comboBox.addActionListener(l);
    }

    public void addItemListener(final ItemListener l) {
        comboBox.addItemListener(l);
    }

    public void suppressActionListeners() {
        final ActionListener[] listeners = comboBox.getActionListeners();
        for (final ActionListener al : listeners) {
            comboBox.removeActionListener(al);
        }
        suppressedActionListeners = suppressedActionListeners == null
                ? listeners
                        : ObjectArrays.concat(suppressedActionListeners, listeners, ActionListener.class);
    }
    public void unsuppressActionListeners() {
        if (suppressedActionListeners != null) {
            for (final ActionListener al : suppressedActionListeners) {
                comboBox.addActionListener(al);
            }
            suppressedActionListeners = null;
        }
    }

    public void addKeyListener(final KeyListener l) {
        comboBox.addKeyListener(l);
    }

    public void setRenderer(final ListCellRenderer<? super E> aRenderer) {
        comboBox.setRenderer(aRenderer);
    }

    public void setModel(final ComboBoxModel<E> aModel) {
        comboBox.setModel(aModel);
    }

    public void setTextAlignment(final TextAlignment align) {
        comboBox.setTextAlignment(align);
    }

    public void setSkinFont(final SkinFont skinFont) {
        comboBox.setSkinFont(skinFont);
    }

    @Override
    public boolean isVisible() {
        return comboBox.isVisible();
    }

    @Override
    public void setVisible(final boolean aFlag) {
        comboBox.setVisible(aFlag);
    }

    @Override
    public boolean isEnabled() {
        return comboBox.isEnabled();
    }

    @Override
    public void setEnabled(final boolean aFlag) {
        comboBox.setEnabled(aFlag);
    }

    public int getAutoSizeWidth() {
        return comboBox.getAutoSizeWidth();
    }

    public void addTo(final Container container) {
        addTo(container, null);
    }
    public void addTo(final Container container, final Object constraints0) {
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
    public static <E> FComboBox<E> refreshComboBoxSkin(final FComboBox<E> comboBox) {
        return refreshComboBoxSkin(comboBox, null);
    }
    public static <E> FComboBox<E> refreshComboBoxSkin(final FComboBox<E> comboBox, final Object constraints) {
        //find index of combo box within parent
        final Container parent = comboBox.getParent();
        if (parent == null) { return comboBox; }

        int index;
        for (index = 0; index < parent.getComponentCount(); index++) {
            if (parent.getComponent(index) == comboBox) {
                break;
            }
        }

        //create copy of combo box
        final FComboBox<E> newComboBox = new FComboBox<E>();
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            newComboBox.addItem(comboBox.getItemAt(i));
        }
        newComboBox.setSelectedIndex(comboBox.getSelectedIndex());

        ActionListener[] actionListeners = newComboBox.getActionListeners();
        for (final ActionListener l : actionListeners) {
            newComboBox.removeActionListener(l); //remove default action listeners to prevent duplicates
        }
        actionListeners = comboBox.getActionListeners();
        for (final ActionListener l : actionListeners) {
            newComboBox.addActionListener(l);
        }

        ItemListener[] itemListeners = newComboBox.getItemListeners();
        for (final ItemListener l : itemListeners) {
            newComboBox.removeItemListener(l); //remove default item listener to prevent duplicates
        }
        itemListeners = comboBox.getItemListeners();
        for (final ItemListener l : itemListeners) {
            newComboBox.addItemListener(l);
        }

        KeyListener[] keyListeners = newComboBox.getKeyListeners();
        for (final KeyListener l : keyListeners) {
            newComboBox.removeKeyListener(l); //remove default key listeners to prevent duplicates
        }
        keyListeners = comboBox.getKeyListeners();
        for (final KeyListener l : keyListeners) {
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