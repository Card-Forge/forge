package forge.itemmanager.filters;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.menu.FTooltip;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.util.TextUtil;
import forge.utils.LayoutHelper;


public abstract class ListLabelFilter<T extends InventoryItem> extends ItemFilter<T> {
    private ListLabel label;

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
    protected final void buildWidget(Widget widget) {
        label = new ListLabel();
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
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(label, FTextField.getDefaultHeight());
    }

    private class ListLabel extends FLabel {
        private ListLabel() {
            super(new FLabel.Builder().align(HAlignment.LEFT).fontSize(12));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FTooltip tooltip = new FTooltip(getTooltip());
            tooltip.show(this, x, y);
            return true;
        }
    }
}
