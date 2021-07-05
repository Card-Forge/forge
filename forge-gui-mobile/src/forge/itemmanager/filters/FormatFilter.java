package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Utils;


public abstract class FormatFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected GameFormat format;
    private String selectedFormat;
    private boolean preventHandling = false;
    private FComboBox<Object> cbxFormats = new FComboBox<>();

    public FormatFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);

        final Localizer localizer = Localizer.getInstance();
        cbxFormats.setFont(FSkinFont.get(12));
        cbxFormats.addItem(localizer.getMessage("lblAllSetsFormats"));
        for (GameFormat format : FModel.getFormats().getFilterList()) {
            cbxFormats.addItem(format);
        }
        cbxFormats.addItem(localizer.getMessage("lblOtherFormats"));
        cbxFormats.addItem(localizer.getMessage("lblChooseSets"));
        selectedFormat = cbxFormats.getText();

        cbxFormats.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (preventHandling) { return; }

                int index = cbxFormats.getSelectedIndex();
                if (index == -1) {
                    //Do nothing when index set to -1
                }
                else if (index == 0) {
                    format = null;
                    applyChange();
                }
                else if (index == cbxFormats.getItemCount() - 2) {
                    preventHandling = true;
                    cbxFormats.setText(selectedFormat); //restore previous selection by default
                    preventHandling = false;
                    HistoricFormatSelect historicFormatSelect = new HistoricFormatSelect();
                    historicFormatSelect.setOnCloseCallBack(new Runnable(){
                        @Override
                        public void run() {
                            format = historicFormatSelect.getSelectedFormat();
                            cbxFormats.setText(format.getName());
                            applyChange();
                        }
                    });
                    Forge.openScreen(historicFormatSelect);
                }
                else if (index == cbxFormats.getItemCount() - 1) {
                    preventHandling = true;
                    cbxFormats.setText(selectedFormat); //restore previous selection by default
                    preventHandling = false;
                    Forge.openScreen(new MultiSetSelect());
                }
                else {
                    format = (GameFormat)cbxFormats.getSelectedItem();
                    applyChange();
                }
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

    private class MultiSetSelect extends FScreen {
        private final Set<CardEdition> selectedSets = new HashSet<>();
        private final FGroupList<CardEdition> lstSets = add(new FGroupList<>());

        private MultiSetSelect() {
            super("Choose Sets");

            lstSets.addGroup("Core Sets");
            lstSets.addGroup("Expansions");
            lstSets.addGroup("Starter Sets");
            lstSets.addGroup("Reprint Sets");
            lstSets.addGroup("Boxed Sets");

            lstSets.addGroup("Collector's Edition");
            lstSets.addGroup("Duel Decks");
            lstSets.addGroup("Promo Sets");
            lstSets.addGroup("Digital Sets");

            lstSets.addGroup("Draft Innovation Sets");



            lstSets.addGroup("Commander Sets");
            lstSets.addGroup("Multiplayer Sets");
            lstSets.addGroup("Other Supplemental Sets");
            lstSets.addGroup("Funny Sets");

            lstSets.addGroup("Custom Sets");

            List<CardEdition> sets = FModel.getMagicDb().getSortedEditions();
            for (CardEdition set : sets) {
                switch (set.getType()) {
                    case CORE:
                        lstSets.addItem(set, 0);
                        break;
                    case EXPANSION:
                        lstSets.addItem(set, 1);
                        break;
                    case STARTER:
                        lstSets.addItem(set, 2);
                        break;
                    case REPRINT:
                        lstSets.addItem(set, 3);
                        break;
                    case BOXED_SET:
                        lstSets.addItem(set,4);
                        break;
                    case COLLECTOR_EDITION:
                        lstSets.addItem(set, 5);
                        break;
                    case DUEL_DECK:
                        lstSets.addItem(set, 6);
                        break;
                    case PROMO:
                        lstSets.addItem(set, 7);
                        break;
                    case ONLINE:
                        lstSets.addItem(set, 8);
                        break;
                    case DRAFT:
                        lstSets.addItem(set, 9);
                        break;
                    case COMMANDER:
                        lstSets.addItem(set, 10);
                        break;
                    case MULTIPLAYER:
                        lstSets.addItem(set, 11);
                        break;
                    case OTHER:
                        lstSets.addItem(set, 12);
                        break;
                    case FUNNY:
                        lstSets.addItem(set, 13);
                        break;
                    default:  // THIRDPARTY - Custom Sets
                        lstSets.addItem(set, 14);
                        break;
                }
            }

            lstSets.setListItemRenderer(new SetRenderer());
        }

        @Override
        public void onClose(Callback<Boolean> canCloseCallback) {
            if (selectedSets.size() > 0) {
                List<String> setCodes = new ArrayList<>();
                List<CardEdition> sortedSets = new ArrayList<>(selectedSets);
                Collections.sort(sortedSets);
                for (CardEdition set : sortedSets) {
                    setCodes.add(set.getCode());
                }
                format = new GameFormat(null, setCodes, null);
                cbxFormats.setText(sortedSets.size() > 1 ? TextUtil.join(setCodes, ", ") : sortedSets.get(0).toString());
                applyChange();
            }
            super.onClose(canCloseCallback);
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            lstSets.setBounds(0, startY, width, height - startY);
        }

        private class SetRenderer extends FList.ListItemRenderer<CardEdition> {
            @Override
            public float getItemHeight() {
                return Utils.AVG_FINGER_HEIGHT;
            }

            @Override
            public boolean tap(Integer index, CardEdition value, float x, float y, int count) {
                if (selectedSets.contains(value)) {
                    if (count == 2) {
                        Forge.back(); //support double tap to confirm selection without unselecting double tapped item
                    }
                    else {
                        selectedSets.remove(value);
                    }
                }
                else {
                    selectedSets.add(value);
                    if (count == 2) {
                        Forge.back(); //support double tap to confirm selection after selecting double tapped item
                    }
                }
                return true;
            }

            @Override
            public void drawValue(Graphics g, Integer index, CardEdition value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                x += offset;
                y += offset;
                w -= 2 * offset;
                h -= 2 * offset;

                float textHeight = h;
                h *= 0.66f;

                g.drawText(value.toString(), font, foreColor, x, y, w - h - FList.PADDING, textHeight, false, Align.left, true);

                x += w - h;
                y += (textHeight - h) / 2;
                FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, foreColor, selectedSets.contains(value), x, y, h, h);
            }
        }
    }
}
