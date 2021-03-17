package forge.itemmanager.filters;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;

import com.google.common.base.Predicate;

import forge.gui.UiCommand;
import forge.item.InventoryItem;
import forge.item.PaperToken;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;


public class TokenSearchFilter extends TextSearchFilter<PaperToken> {
    private FComboBoxWrapper<String> cbSearchMode;
    private FLabel btnName, btnType, btnText, btnCost;

    public TokenSearchFilter(ItemManager<? super PaperToken> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperToken> createCopy() {
        TokenSearchFilter copy = new TokenSearchFilter(itemManager);
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
        cbSearchMode.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                if (!txtSearch.isEmpty()) {
                    applyChange();
                }
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

        button.setCommand(new UiCommand() {
            @Override
            public void run() {
                applyChange();
            }
        });

        widget.add(button);
        return button;
    }

    @Override
    protected Predicate<PaperToken> buildPredicate() {
        // Don't filter anything out for now.
        return new Predicate<PaperToken>() {
            @Override
            public boolean apply(PaperToken paperToken) {
                return true;
            }
        };
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
