package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FLabel;
import forge.toolbox.FSpinner;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;
import forge.util.ComparableOp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;


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
        return lowerBound.getValue().equals(minValue()) && upperBound.getValue().equals(maxValue());
    }

    @Override
    public void reset() {
        lowerBound.setValue(minValue());
        upperBound.setValue(maxValue());
    }

    @Override
    public Component getMainComponent() {
        return ((JSpinner.DefaultEditor)lowerBound.getEditor()).getTextField();
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        lowerBound = addSpinner(widget, true);

        String text = " <= " + this.getCaption() + " <= ";
        label = new FLabel.Builder().text(text).fontSize(12).build();
        widget.add(label);

        upperBound = addSpinner(widget, false);

        lowerBound.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (Integer.parseInt(upperBound.getValue().toString()) <
                        Integer.parseInt(lowerBound.getValue().toString()))
                {
                    upperBound.setValue(lowerBound.getValue());
                }
                applyChange();
            }
        });

        upperBound.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (Integer.parseInt(lowerBound.getValue().toString()) >
                        Integer.parseInt(upperBound.getValue().toString()))
                {
                    lowerBound.setValue(upperBound.getValue());
                }
                applyChange();
            }
        });
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.include(lowerBound, 45, FTextField.HEIGHT);
        helper.include(label, 125, 26);
        helper.include(upperBound, 45, FTextField.HEIGHT);
    }

    private FSpinner addSpinner(JPanel widget, boolean lowerBound) {
        FSpinner spinner = new FSpinner.Builder()
            .minValue(this.minValue())
            .maxValue(this.maxValue())
            .initialValue(lowerBound ? this.minValue() : this.maxValue())
            .build();
        spinner.setFocusable(false); //only the spinner text field should be focusable, not the up/down widget
        widget.add(spinner);
        return spinner;
    }

    protected Predicate<CardRules> getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField field) {
        int lowerValue = Integer.parseInt(lowerBound.getValue().toString());
        int upperValue = Integer.parseInt(upperBound.getValue().toString());
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
