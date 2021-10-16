package forge.adventure.libgdxgui.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
import forge.adventure.libgdxgui.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.adventure.libgdxgui.itemmanager.ItemManager;
import forge.adventure.libgdxgui.menu.FTooltip;
import forge.adventure.libgdxgui.toolbox.FLabel;
import forge.util.TextUtil;


public abstract class ListLabelFilter<T extends InventoryItem> extends ItemFilter<T> {
    public static final FSkinFont LABEL_FONT = FSkinFont.get(12);

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
            labelBuilder.append(": ").append(getList().iterator().next());
            break;
        default:
            labelBuilder.append("s: ").append(TextUtil.join(getList(), ", "));
            break;
        }
        label.setText(labelBuilder.toString());
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        label.setSize(width, height);
    }

    private class ListLabel extends FLabel {
        private ListLabel() {
            super(new FLabel.Builder().align(Align.left).font(LABEL_FONT));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FTooltip tooltip = new FTooltip(getTooltip());
            tooltip.show(this, x, getHeight());
            return true;
        }
    }
}
