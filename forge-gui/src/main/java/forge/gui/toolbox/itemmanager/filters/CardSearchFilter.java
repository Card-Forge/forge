package forge.gui.toolbox.itemmanager.filters;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;

import com.google.common.base.Predicate;

import forge.Command;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FTextField;
import forge.gui.toolbox.LayoutHelper;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.item.InventoryItem;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    private FComboBoxWrapper<String> cbSearchMode;
    private FLabel btnName, btnType, btnText;

    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardSearchFilter copy = new CardSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        copy.cbSearchMode.setSelectedIndex(this.cbSearchMode.getSelectedIndex());
        copy.btnName.setSelected(this.btnName.getSelected());
        copy.btnType.setSelected(this.btnType.getSelected());
        copy.btnText.setSelected(this.btnText.getSelected());
        return copy;
    }

    @Override
    public void reset() {
        super.reset();
        this.cbSearchMode.setSelectedIndex(0);
        this.btnName.setSelected(true);
        this.btnType.setSelected(true);
        this.btnText.setSelected(true);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        return false;
    }

    @Override
    protected final void buildWidget(JPanel widget) {
        super.buildWidget(widget);

        cbSearchMode = new FComboBoxWrapper<String>();
        cbSearchMode.addItem("in");
        cbSearchMode.addItem("not in");
        widget.add(cbSearchMode.getComponent());
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
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        final int comboBoxWidth = 61;
        final int buttonWidth = 51;

        helper.fillLine(txtSearch, FTextField.HEIGHT, comboBoxWidth + buttonWidth * 3 + 12); //leave space for combo box and buttons
        helper.include(cbSearchMode.getComponent(), comboBoxWidth, FTextField.HEIGHT);
        helper.include(btnName, buttonWidth, FTextField.HEIGHT);
        helper.include(btnType, buttonWidth, FTextField.HEIGHT);
        helper.include(btnText, buttonWidth, FTextField.HEIGHT);
    }

    @SuppressWarnings("serial")
    private FLabel addButton(JPanel widget, String text) {
        FLabel button = new FLabel.Builder().text(text).hoverable().selectable().selected().build();

        button.setCommand(new Command() {
            @Override
            public void run() {
                applyChange();
            }
        });

        widget.add(button);
        return button;
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildTextFilter(
                txtSearch.getText(),
                cbSearchMode.getSelectedIndex() != 0,
                btnName.getSelected(),
                btnType.getSelected(),
                btnText.getSelected());
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        //fallback to regular item text filter if item not PaperCard
        boolean result = btnName.getSelected() && SFilterUtil.buildItemTextFilter(txtSearch.getText()).apply(item);
        if (cbSearchMode.getSelectedIndex() != 0) { //invert result if needed
            result = !result;
        }
        return result;
    }
}
