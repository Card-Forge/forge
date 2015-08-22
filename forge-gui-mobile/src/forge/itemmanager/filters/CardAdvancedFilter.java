package forge.itemmanager.filters;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;

import forge.assets.FSkinImage;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.menu.FTooltip;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;


public class CardAdvancedFilter extends ItemFilter<PaperCard> {
    private FiltersLabel label;
    private FLabel btnEdit;
    private boolean isAdded;

    public CardAdvancedFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardAdvancedFilter copy = new CardAdvancedFilter(itemManager);
        return copy;
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard input) {
                return true;
            }
        };
    }

    public void edit() {
        if (!isAdded) {
            isAdded = true;
            itemManager.addFilter(0, this);
        }
        else {
            itemManager.removeFilter(this);
            isAdded = false;
        }
    }

    private void updateLabel() {
        label.setText("Filters: ");
    }

    private String getTooltip() {
        return "Test2";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    protected void buildWidget(Widget widget) {
        label = new FiltersLabel();
        updateLabel();
        widget.add(label);

        btnEdit = new FLabel.ButtonBuilder().icon(FSkinImage.SEARCH).iconScaleFactor(0.9f).command(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                edit();
            }
        }).build();
        widget.add(btnEdit);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        float buttonWidth = height;
        float buttonHeight = height;
        btnEdit.setBounds(width - buttonWidth, (height - buttonHeight) / 2, buttonWidth, buttonHeight);
        label.setSize(btnEdit.getLeft() - ItemFilter.PADDING, height);
    }

    private class FiltersLabel extends FLabel {
        private FiltersLabel() {
            super(new FLabel.Builder().align(HAlignment.LEFT).font(ListLabelFilter.LABEL_FONT));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FTooltip tooltip = new FTooltip(getTooltip());
            tooltip.show(this, x, getHeight());
            return true;
        }
    }
}
