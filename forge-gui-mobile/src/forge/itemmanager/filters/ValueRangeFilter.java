package forge.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
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

        String text = "<= " + this.getCaption() + " <=";
        label = new FLabel.Builder().text(text).align(Align.center).font(ListLabelFilter.LABEL_FONT).build();
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
    protected void doWidgetLayout(float width, float height) {
        float x = 0;
        float spinnerWidth = height * 1.5f;
        lowerBound.setBounds(x, 0, spinnerWidth, height);
        x += lowerBound.getWidth();
        label.setBounds(x, 0, width - 2 * spinnerWidth, height);
        x += label.getWidth();
        upperBound.setBounds(x, 0, spinnerWidth, height);
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
}
