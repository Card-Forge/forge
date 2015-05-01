package forge.interfaces;

public interface IComboBox<E> extends IComponent {
    void setSelectedItem(E item);
    void setSelectedIndex(int index);
    void addItem(E item);
    void removeAllItems();
    int getSelectedIndex();
    E getSelectedItem();
}
