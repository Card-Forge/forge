package forge.itemmanager.filters;

import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;
import forge.util.TextUtil;

import javax.swing.*;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ListLabelFilter<T extends InventoryItem> extends ItemFilter<T> {
    private FLabel label;
    
    protected ListLabelFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    protected abstract String getCaption();
    protected abstract Iterable<String> getList();
    protected abstract String getTooltip();
    protected abstract int getCount();

    @Override
    public final boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        label = new FLabel.Builder().fontAlign(SwingConstants.LEFT).fontSize(12).build();
        updateLabel();
        widget.add(label);
    }

    protected void updateLabel() {
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append(getCaption());
        switch (getCount()) {
        case 0:
            labelBuilder.append("s: All");
            break;
        case 1:
            labelBuilder.append(": " + getList().iterator().next());
            break;
        default:
            labelBuilder.append("s: " + TextUtil.join(getList(), ", "));
            break;
        }
        label.setText(labelBuilder.toString());
        label.setToolTipText(getTooltip());
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(label, FTextField.HEIGHT);
    }
}
