package forge.itemmanager.filters;

import forge.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public abstract class ComboBoxFilter<T extends InventoryItem, V> extends ItemFilter<T> {
    protected V filterValue;
    private boolean preventHandling = false;
    private FComboBox<Object> comboBox = new FComboBox<Object>() {
        @SuppressWarnings("unchecked")
        @Override
        protected String getDisplayText(Object item) {
            if (item instanceof String) {
                return (String)item;
            }
            return ComboBoxFilter.this.getDisplayText((V)item);
        }
    };

    protected ComboBoxFilter(String allText, Iterable<V> values, ItemManager<? super T> itemManager0) {
        this(allText, itemManager0);
        for (V value : values) {
            comboBox.addItem(value);
        }
    }
    protected ComboBoxFilter(String allText, V[] values, ItemManager<? super T> itemManager0) {
        this(allText, itemManager0);
        for (V value : values) {
            comboBox.addItem(value);
        }
    }
    private ComboBoxFilter(String allText, ItemManager<? super T> itemManager0) {
        super(itemManager0);

        comboBox.setFont(FSkinFont.get(12));
        comboBox.addItem(allText);
        comboBox.setChangedHandler(new FEventHandler() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleEvent(FEvent e) {
                if (preventHandling) { return; }

                int index = comboBox.getSelectedIndex();
                if (index == -1) {
                    //Do nothing when index set to -1
                }
                else if (index == 0) {
                    filterValue = null;
                    applyChange();
                }
                else {
                    filterValue = (V)comboBox.getSelectedItem();
                    applyChange();
                }
            }
        });
    }

    protected String getDisplayText(V value) {
        return value.toString();
    }

    @Override
    public void reset() {
        preventHandling = true;
        comboBox.setSelectedIndex(0);
        preventHandling = false;
        filterValue = null;
    }

    @Override
    public FDisplayObject getMainComponent() {
        return comboBox;
    }

    @Override
    public boolean isEmpty() {
        return filterValue == null;
    }

    @Override
    protected void buildWidget(Widget widget) {
        widget.add(comboBox);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        comboBox.setSize(width, height);
    }
}
