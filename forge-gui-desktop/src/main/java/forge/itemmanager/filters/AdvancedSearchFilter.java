package forge.itemmanager.filters;

import forge.item.InventoryItem;
import forge.itemmanager.AdvancedSearch;
import forge.itemmanager.ItemManager;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;

import javax.swing.*;

import com.google.common.base.Predicate;


public class AdvancedSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private final AdvancedSearch.Model<T> model;
    private FLabel label;
    
    public AdvancedSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        model = new AdvancedSearch.Model<T>();
    }

    @Override
    public final boolean isEmpty() {
        return model.isEmpty();
    }

    @Override
    public void reset() {
        model.reset();
    }

    @Override
    public ItemFilter<T> createCopy() {
        return new AdvancedSearchFilter<T>(itemManager);
    }

    @Override
    protected Predicate<T> buildPredicate() {
        return model.getPredicate();
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        label = new FLabel.Builder().fontAlign(SwingConstants.LEFT).fontSize(12).build();
        model.setLabel(label);
        widget.add(label);
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(label, FTextField.HEIGHT);
    }

    public boolean edit() {
        return true;
    }

    @Override
    public boolean merge(ItemFilter<?> filter) {
        return true;
    }
}
