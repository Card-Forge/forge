package forge.interfaces;

public interface IComboBox<E> {
    boolean isEnabled();
    void setEnabled(boolean b0);
    boolean isVisible();
    void setVisible(boolean b0);
    void setSelectedItem(E item);
    void setSelectedIndex(int index);
    void addItem(E item);
    void removeAllItems();
    int getSelectedIndex();
    E getSelectedItem();
}
