package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FSpinner;
import forge.util.ComparableOp;
import forge.util.LayoutHelper;


public abstract class ValueRangeFilter<T extends InventoryItem> extends ItemFilter<T> {
    private FLabel label;
    private FSpinner lowerBound, upperBound;

    protected ValueRangeFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    protected abstract String getCaption();

    protected int minValue() {
        return 0;
    }

    protected int maxValue() {
        return 20;
    }

    @Override
    public final boolean isEmpty() {
        return lowerBound.getValue() == minValue() && upperBound.getValue() == maxValue();
    }

    @Override
    public void reset() {
        lowerBound.setValue(minValue());
        upperBound.setValue(maxValue());
    }

    @Override
    public FDisplayObject getMainComponent() {
        return lowerBound;
    }

    @Override
    protected final void buildWidget(Widget widget) {
        lowerBound = addSpinner(widget, true);

        String text = " <= " + this.getCaption() + " <= ";
        label = new FLabel.Builder().text(text).font(ListLabelFilter.LABEL_FONT).build();
        widget.add(label);

        upperBound = addSpinner(widget, false);

        lowerBound.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (upperBound.getValue() < lowerBound.getValue()) {
                    upperBound.setValue(lowerBound.getValue());
                }
                applyChange();
            }
        });

        upperBound.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (lowerBound.getValue() > upperBound.getValue()) {
                    lowerBound.setValue(upperBound.getValue());
                }
                applyChange();
            }
        });
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        float height = helper.getParentHeight();
        helper.include(lowerBound, 45, height);
        helper.include(label, 125, height);
        helper.include(upperBound, 45, height);
    }

    private FSpinner addSpinner(Widget widget, boolean lowerBound) {
        FSpinner spinner = new FSpinner(minValue(), maxValue(), lowerBound ? this.minValue() : this.maxValue());
        widget.add(spinner);
        return spinner;
    }

    protected Predicate<CardRules> getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField field) {
        int lowerValue = lowerBound.getValue();
        int upperValue = upperBound.getValue();
        boolean hasMin = lowerValue != minValue();
        boolean hasMax = upperValue != maxValue();

        Predicate<CardRules> pLower = hasMin ? new CardRulesPredicates.LeafNumber(field, ComparableOp.GT_OR_EQUAL, lowerValue) : null;
        Predicate<CardRules> pUpper = hasMax ? new CardRulesPredicates.LeafNumber(field, ComparableOp.LT_OR_EQUAL, upperValue) : null;

        return SFilterUtil.optimizedAnd(pLower, pUpper);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(ItemFilter<?> filter) {
        return true;
    }
}
