package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.utils.LayoutHelper;


public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    private FComboBox<String> cbSearchMode;
    private FLabel btnName, btnType, btnText;

    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardSearchFilter copy = new CardSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(txtSearch.getText());
        copy.cbSearchMode.setSelectedIndex(cbSearchMode.getSelectedIndex());
        copy.btnName.setSelected(btnName.isSelected());
        copy.btnType.setSelected(btnType.isSelected());
        copy.btnText.setSelected(btnText.isSelected());
        return copy;
    }

    @Override
    public void reset() {
        super.reset();
        cbSearchMode.setSelectedIndex(0);
        btnName.setSelected(true);
        btnType.setSelected(true);
        btnText.setSelected(true);
    }

    @Override
    protected final void buildWidget(Widget widget) {
        super.buildWidget(widget);

        cbSearchMode = new FComboBox<String>();
        cbSearchMode.addItem("in");
        cbSearchMode.addItem("not in");
        cbSearchMode.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (!txtSearch.isEmpty()) {
                    applyChange();
                }
            }
        });
        widget.add(cbSearchMode);

        btnName = addButton(widget, "Name");
        btnType = addButton(widget, "Type");
        btnText = addButton(widget, "Text");
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        final float comboBoxWidth = 61;
        final float buttonWidth = 51;
        final float height = txtSearch.getHeight();

        helper.fillLine(txtSearch, height, comboBoxWidth + buttonWidth * 3 + helper.getGapX() * 4); //leave space for combo box and buttons
        helper.include(cbSearchMode, comboBoxWidth, height);
        helper.include(btnName, buttonWidth, height);
        helper.include(btnType, buttonWidth, height);
        helper.include(btnText, buttonWidth, height);
    }

    private FLabel addButton(Widget widget, String text) {
        FLabel button = new FLabel.Builder().text(text)
                .selectable().selected().command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        applyChange();
                    }
                })
                .build();
        widget.add(button);
        return button;
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildTextFilter(
                txtSearch.getText(),
                cbSearchMode.getSelectedIndex() != 0,
                btnName.isSelected(),
                btnType.isSelected(),
                btnText.isSelected());
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        //fallback to regular item text filter if item not PaperCard
        boolean result = btnName.isSelected() && SFilterUtil.buildItemTextFilter(txtSearch.getText()).apply(item);
        if (cbSearchMode.getSelectedIndex() != 0) { //invert result if needed
            result = !result;
        }
        return result;
    }
}
