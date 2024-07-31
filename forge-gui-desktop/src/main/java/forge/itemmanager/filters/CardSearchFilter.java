package forge.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;

import forge.gui.UiCommand;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;


public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    private FComboBoxWrapper<String> cbSearchMode;
    private FLabel btnName, btnType, btnText, btnCost;

    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardSearchFilter copy = new CardSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        copy.cbSearchMode.setSelectedIndex(this.cbSearchMode.getSelectedIndex());
        copy.btnName.setSelected(this.btnName.isSelected());
        copy.btnType.setSelected(this.btnType.isSelected());
        copy.btnText.setSelected(this.btnText.isSelected());
		copy.btnCost.setSelected(this.btnCost.isSelected());
        return copy;
    }

    @Override
    public void reset() {
        super.reset();
        this.cbSearchMode.setSelectedIndex(0);
        this.btnName.setSelected(true);
        this.btnType.setSelected(true);
		this.btnText.setSelected(true);
		this.btnCost.setSelected(false);
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        super.buildWidget(widget);

        cbSearchMode = new FComboBoxWrapper<>();
        cbSearchMode.addItem("in");
        cbSearchMode.addItem("not in");
        cbSearchMode.addTo(widget);
        cbSearchMode.addItemListener(arg0 -> {
            if (!txtSearch.isEmpty()) {
                applyChange();
            }
        });

        btnName = addButton(widget, "Name");
        btnType = addButton(widget, "Type");
        btnText = addButton(widget, "Text");
		btnCost = addButton(widget, "Cost");
		
		btnCost.setSelected(false);
		
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        final int comboBoxWidth = 61;
        final int buttonWidth = 51;

        helper.fillLine(txtSearch, FTextField.HEIGHT, comboBoxWidth + buttonWidth * 4 + 16); //leave space for combo box and buttons
        helper.include(cbSearchMode.getComponent(), comboBoxWidth, FTextField.HEIGHT);
        helper.include(btnName, buttonWidth, FTextField.HEIGHT);
        helper.include(btnType, buttonWidth, FTextField.HEIGHT);
		helper.include(btnText, buttonWidth, FTextField.HEIGHT);
		helper.include(btnCost, buttonWidth, FTextField.HEIGHT);
    }

    @SuppressWarnings("serial")
    private FLabel addButton(JPanel widget, String text) {
        FLabel button = new FLabel.Builder().text(text).hoverable().selectable().selected().build();

        button.setCommand((UiCommand) this::applyChange);

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
                btnText.isSelected(),
				btnCost.isSelected());
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
