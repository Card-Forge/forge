package forge.itemmanager.filters;

import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SItemManagerUtil.StatTypes;

public abstract class ColorFilter<T extends InventoryItem> extends StatTypeFilter<T> {
    public ColorFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected void buildWidget(Widget widget) {
        addToggleButton(widget, StatTypes.WHITE);
        addToggleButton(widget, StatTypes.BLUE);
        addToggleButton(widget, StatTypes.BLACK);
        addToggleButton(widget, StatTypes.RED);
        addToggleButton(widget, StatTypes.GREEN);
        addToggleButton(widget, StatTypes.COLORLESS);
        addToggleButton(widget, StatTypes.MULTICOLOR);
    }
}
