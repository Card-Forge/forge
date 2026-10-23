package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.model.FModel;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.util.TextUtil;


public abstract class FormatFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected GameFormat format;
    private String selectedFormat;
    private boolean preventHandling = false;
    private final FComboBox<Object> cbxFormats = new FComboBox<>();

    public FormatFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);

        cbxFormats.setFont(FSkinFont.get(12));
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblAllSetsFormats"));
        for (GameFormat format : FModel.getFormats().getFilterList()) {
            cbxFormats.addItem(format);
        }
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblOtherFormats"));
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblChooseSets"));

        selectedFormat = cbxFormats.getText();

        cbxFormats.setChangedHandler(e -> {
            if (preventHandling) {
                return;
            }

            int index = cbxFormats.getSelectedIndex();
            if (index == -1) {
                //Do nothing when index set to -1
            } else if (index == 0) {
                format = null;
                applyChange();
            } else if (index == cbxFormats.getItemCount() - 2) {
                preventHandling = true;
                cbxFormats.setText(selectedFormat); //restore previous selection by default
                preventHandling = false;
                ArchivedFormatSelect archivedFormatSelect = new ArchivedFormatSelect();
                archivedFormatSelect.setOnCloseCallBack(() -> {
                    format = archivedFormatSelect.getSelectedFormat();
                    cbxFormats.setText(format.getName());
                    applyChange();
                });
                Forge.openScreen(archivedFormatSelect);
            } else if (index == cbxFormats.getItemCount() - 1) {
                preventHandling = true;
                cbxFormats.setText(selectedFormat); //restore previous selection by default
                preventHandling = false;
                Forge.openScreen(new MultiSetSelect("Choose Sets", Collections.emptyList(), sets -> {
                    if (sets.isEmpty())
                        return;
                    List<String> setCodes = new ArrayList<>();
                    for (CardEdition set : sets) {
                        setCodes.add(set.getCode());
                    }
                    format = new GameFormat(null, setCodes, null);
                    cbxFormats.setText(sets.size() > 1 ? TextUtil.join(setCodes, ", ") : sets.get(0).toString());
                    applyChange();
                }));
            } else {
                format = (GameFormat) cbxFormats.getSelectedItem();
                applyChange();
            }
        });
    }

    @Override
    protected void applyChange() {
        selectedFormat = cbxFormats.getText(); //backup current text
        super.applyChange();
    }

    @Override
    public void reset() {
        preventHandling = true;
        cbxFormats.setSelectedIndex(0);
        preventHandling = false;
        format = null;
    }

    @Override
    public FDisplayObject getMainComponent() {
        return cbxFormats;
    }

    @Override
    public boolean isEmpty() {
        return format == null;
    }

    @Override
    protected void buildWidget(Widget widget) {
        widget.add(cbxFormats);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        cbxFormats.setSize(width, height);
    }
}
