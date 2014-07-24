package forge.itemmanager.filters;

import forge.assets.FSkinFont;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.model.FModel;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

import java.util.HashSet;
import java.util.Set;


public abstract class FormatFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected final Set<GameFormat> formats = new HashSet<GameFormat>();
    private FComboBox<Object> cbxFormats = new FComboBox<Object>();

    public FormatFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);

        cbxFormats.setFont(FSkinFont.get(12));
        cbxFormats.addItem("All Sets/Formats");
        for (GameFormat format : FModel.getFormats().getOrderedList()) {
            cbxFormats.addItem(format);
        }
        cbxFormats.addItem("Choose Sets...");
        cbxFormats.setSelectedIndex(0);

        cbxFormats.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbxFormats.getSelectedIndex() == 0) {
                    formats.clear();
                    applyChange();
                }
                else if (cbxFormats.getSelectedIndex() < cbxFormats.getItemCount() - 1) {
                    formats.clear();
                    formats.add((GameFormat)cbxFormats.getSelectedItem());
                    applyChange();
                }
                else {
                    //TODO: Open screen to select one or more sets and/or formats
                }
            }
        });
    }

    @Override
    public void reset() {
        this.formats.clear();
    }

    public static <T extends InventoryItem> boolean canAddFormat(GameFormat format, FormatFilter<T> existingFilter) {
        return existingFilter == null || !existingFilter.formats.contains(format);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean merge(ItemFilter<?> filter) {
        FormatFilter<T> formatFilter = (FormatFilter<T>)filter;
        this.formats.addAll(formatFilter.formats);
        return true;
    }

    @Override
    public boolean isEmpty() {
        return formats.isEmpty();
    }

    @Override
    protected void buildWidget(Widget widget) {
        widget.add(cbxFormats);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        cbxFormats.setSize(width, height);
    }
}
