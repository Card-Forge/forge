package forge.itemmanager;

import forge.itemmanager.ItemColumnConfig.SortState;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;
import forge.util.XmlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 
 * Preferences associated with individual cards
 *
 */
public enum ItemManagerConfig {
    STRING_ONLY(SColumnUtil.getStringColumn(), false, false, true,
            null, null, 1, 0),
    CARD_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, true, false,
            null, null, 4, 0),
    DECK_EDITOR(SColumnUtil.getDeckEditorDefaultColumns(), false, false, true,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    DRAFT_PACK(SColumnUtil.getDraftPackDefaultColumns(), false, false, true,
            null, null, 4, 1),
    DRAFT_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false, false,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    SEALED_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false, false,
            GroupDef.COLOR, ColumnDef.CMC, 4, 1),
    SPELL_SHOP(SColumnUtil.getSpellShopDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_INVENTORY(SColumnUtil.getQuestInventoryDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_EDITOR_POOL(SColumnUtil.getQuestEditorPoolDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_DECK_EDITOR(SColumnUtil.getQuestDeckEditorDefaultColumns(), false, false, false,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    QUEST_DRAFT_DECK_VIEWER(SColumnUtil.getDeckViewerDefaultColumns(), false, false, true,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    CONQUEST_AETHER(SColumnUtil.getConquestAEtherDefaultColumns(), false, false, false,
            null, null, 4, 0),
    CONQUEST_COMMANDERS(SColumnUtil.getConquestCommandersDefaultColumns(), false, false, false,
            null, null, 3, 0),
    CONQUEST_COLLECTION(SColumnUtil.getConquestCollectionDefaultColumns(), false, false, false,
            null, null, 4, 0),
    CONQUEST_DECK_EDITOR(SColumnUtil.getConquestDeckEditorDefaultColumns(), false, false, false,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    AVATAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, false,
            null, null, 4, 0),
    SCHEME_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, true,
            null, null, 4, 0),
    CONSPIRACY_DECKS(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, true,
            null, null, 4, 0),
    SCHEME_DECK_EDITOR(SColumnUtil.getCatalogDefaultColumns(true), true, false, true,
            null, null, 4, 0),
    PLANAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, true,
            null, null, 4, 0),
    PLANAR_DECK_EDITOR(SColumnUtil.getCatalogDefaultColumns(true), true, false, true,
            null, null, 4, 0),
    COMMANDER_POOL(SColumnUtil.getCatalogDefaultColumns(true), true, false, false,
            null, null, 4, 0),
    COMMANDER_SECTION(SColumnUtil.getCatalogDefaultColumns(true), true, false, true,
            null, null, 1, 1),
    WORKSHOP_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, true, false,
            null, null, 4, 0),
    DECK_VIEWER(SColumnUtil.getDeckViewerDefaultColumns(), false, false, false,
            GroupDef.DEFAULT, ColumnDef.CMC, 4, 1),
    CONSTRUCTED_DECKS(SColumnUtil.getDecksDefaultColumns(true, true), false, false, false,
            null, null, 3, 0),
    COMMANDER_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    PLANAR_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    SCHEME_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    VANGUARDS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, true,
            null, null, 3, 0),
    DRAFT_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    SEALED_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    WINSTON_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    QUEST_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    PRECON_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false, false,
            null, null, 3, 0),
    QUEST_EVENT_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false, false,
            null, null, 3, 0),
    NET_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false, false,
            null, null, 3, 0),
    SIDEBOARD(SColumnUtil.getDeckEditorDefaultColumns(), false, false, true,
            GroupDef.DEFAULT, ColumnDef.CMC, 3, 0);

    private Map<ColumnDef, ItemColumnConfig> cols;
    private boolean showUniqueCardsOption;

    private Prop<Boolean> uniqueCardsOnly;
    private Prop<Boolean> hideFilters;
    private Prop<Boolean> compactListView;
    private Prop<GroupDef> groupBy;
    private Prop<ColumnDef> pileBy;
    private Prop<Integer> imageColumnCount;
    private Prop<Integer> viewIndex;

    private ItemManagerConfig(final Map<ColumnDef, ItemColumnConfig> cols0, boolean showUniqueCardsOption0, boolean uniqueCardsOnly0, boolean hideFilters0, GroupDef groupBy0, ColumnDef pileBy0, int imageColumnCount0, int viewIndex0) {
        cols = cols0;
        for (ItemColumnConfig colConfig : cols.values()) {
            colConfig.establishDefaults();
        }
        showUniqueCardsOption = showUniqueCardsOption0;

        uniqueCardsOnly = new Prop<Boolean>(uniqueCardsOnly0);
        hideFilters = new Prop<Boolean>(hideFilters0);
        compactListView = new Prop<Boolean>(FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_LIST_ITEMS)); //use main setting to determine default
        groupBy = new Prop<GroupDef>(groupBy0);
        pileBy = new Prop<ColumnDef>(pileBy0);
        imageColumnCount = new Prop<Integer>(imageColumnCount0);
        viewIndex = new Prop<Integer>(viewIndex0);
    }

    private class Prop<T> {
        private T value;
        private T defaultValue;

        private Prop(T defaultValue0) {
            value = defaultValue0;
            defaultValue = defaultValue0;
        }

        private T getValue() {
            return value;
        }

        private void setValue(T value0) {
            if (value == value0) { return; }
            value = value0;
            save();
        }

        @SuppressWarnings("rawtypes")
        private void writeValue(final Element el, String localName) {
            if (value == null) {
                if (defaultValue != null) {
                    el.setAttribute(localName, "");
                }
            }
            else if (!value.equals(defaultValue)) {
                if (value instanceof Enum) { //use Enum.name to prevent issues with toString() overrides
                    el.setAttribute(localName, ((Enum)value).name());
                }
                else {
                    el.setAttribute(localName, String.valueOf(value));
                }
            }
        }
    }

    public Map<ColumnDef, ItemColumnConfig> getCols() {
        return cols;
    }

    public boolean getShowUniqueCardsOption() {
        return showUniqueCardsOption;
    }

    public boolean getUniqueCardsOnly() {
        return uniqueCardsOnly.getValue();
    }
    public void setUniqueCardsOnly(boolean value0) {
        uniqueCardsOnly.setValue(value0);
    }

    public boolean getHideFilters() {
        return hideFilters.getValue();
    }
    public void setHideFilters(boolean value0) {
        hideFilters.setValue(value0);
    }

    public boolean getCompactListView() {
        return compactListView.getValue();
    }
    public void setCompactListView(boolean value0) {
        compactListView.setValue(value0);
    }

    public GroupDef getGroupBy() {
        return groupBy.getValue();
    }
    public void setGroupBy(GroupDef value0) {
        groupBy.setValue(value0);
    }

    public ColumnDef getPileBy() {
        return pileBy.getValue();
    }
    public void setPileBy(ColumnDef value0) {
        pileBy.setValue(value0);
    }

    public int getImageColumnCount() {
        return imageColumnCount.getValue();
    }
    public void setImageColumnCount(int value0) {
        imageColumnCount.setValue(value0);
    }

    public int getViewIndex() {
        return viewIndex.getValue();
    }
    public void setViewIndex(int value0) {
        viewIndex.setValue(value0);
    }

    public static void load() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(new File(ForgeConstants.ITEM_VIEW_PREFS_FILE));
            final NodeList configs = document.getElementsByTagName("config");
            for (int i = 0; i < configs.getLength(); i++) {
                try { //capture enum parse errors without losing other preferences
                    final Element el = (Element)configs.item(i);
                    final ItemManagerConfig config = Enum.valueOf(ItemManagerConfig.class, el.getAttribute("name"));
                    if (el.hasAttribute("uniqueCardsOnly")) {
                        config.uniqueCardsOnly.value = Boolean.parseBoolean(el.getAttribute("uniqueCardsOnly"));
                    }
                    if (el.hasAttribute("hideFilters")) {
                        config.hideFilters.value = Boolean.parseBoolean(el.getAttribute("hideFilters"));
                    }
                    if (el.hasAttribute("compactListView")) {
                        config.compactListView.value = Boolean.parseBoolean(el.getAttribute("compactListView"));
                    }
                    if (el.hasAttribute("groupBy")) {
                        String value = el.getAttribute("groupBy");
                        if (value.isEmpty()) {
                            config.groupBy.value = null;
                        }
                        else {
                            config.groupBy.value = Enum.valueOf(GroupDef.class, value);
                        }
                    }
                    if (el.hasAttribute("pileBy")) {
                        String value = el.getAttribute("pileBy");
                        if (value.isEmpty()) {
                            config.pileBy.value = null;
                        }
                        else {
                            config.pileBy.value = Enum.valueOf(ColumnDef.class, value);
                        }
                    }
                    if (el.hasAttribute("imageColumnCount")) {
                        config.imageColumnCount.value = Integer.parseInt(el.getAttribute("imageColumnCount"));
                    }
                    if (el.hasAttribute("viewIndex")) {
                        config.viewIndex.value = Integer.parseInt(el.getAttribute("viewIndex"));
                    }
                    final NodeList cols = el.getElementsByTagName("col");
                    for (int j = 0; j < cols.getLength(); j++) {
                        try { //capture enum parse errors without losing other column preferences
                            final Element colEl = (Element)cols.item(j);
                            ItemColumnConfig colConfig = config.cols.get(Enum.valueOf(ColumnDef.class, colEl.getAttribute("name")));
                            if (colEl.hasAttribute("width")) {
                                colConfig.setPreferredWidth(Integer.parseInt(colEl.getAttribute("width")));
                            }
                            if (colEl.hasAttribute("sortPriority")) {
                                colConfig.setSortPriority(Integer.parseInt(colEl.getAttribute("sortPriority")));
                            }
                            if (colEl.hasAttribute("sortState")) {
                                colConfig.setSortState(Enum.valueOf(SortState.class, colEl.getAttribute("sortState")));
                            }
                            if (colEl.hasAttribute("index")) {
                                colConfig.setIndex(Integer.parseInt(colEl.getAttribute("index")));
                            }
                            if (colEl.hasAttribute("visible")) {
                                colConfig.setVisible(Boolean.parseBoolean(colEl.getAttribute("visible")));
                            }
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (FileNotFoundException e) {
            //ok if file not found
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("preferences");
            root.setAttribute("type", "item_view");
            document.appendChild(root);

            for (ItemManagerConfig config : ItemManagerConfig.values()) {
                Element el = document.createElement("config");
                el.setAttribute("name", config.name());
                config.uniqueCardsOnly.writeValue(el, "uniqueCardsOnly");
                config.hideFilters.writeValue(el, "hideFilters");
                config.compactListView.writeValue(el, "compactListView");
                config.groupBy.writeValue(el, "groupBy");
                config.pileBy.writeValue(el, "pileBy");
                config.imageColumnCount.writeValue(el, "imageColumnCount");
                config.viewIndex.writeValue(el, "viewIndex");
                for (ItemColumnConfig colConfig : config.cols.values()) {
                    Element colEl = document.createElement("col");
                    colEl.setAttribute("name", colConfig.getDef().name());
                    if (colConfig.getPreferredWidth() != colConfig.getDefaults().getPreferredWidth()) {
                        colEl.setAttribute("width", String.valueOf(colConfig.getPreferredWidth()));
                    }
                    if (colConfig.getSortPriority() != colConfig.getDefaults().getSortPriority()) {
                        colEl.setAttribute("sortPriority", String.valueOf(colConfig.getSortPriority()));
                    }
                    if (colConfig.getSortState() != colConfig.getDefaults().getSortState()) {
                        colEl.setAttribute("sortState", String.valueOf(colConfig.getSortState()));
                    }
                    if (colConfig.getIndex() != colConfig.getDefaults().getIndex()) {
                        colEl.setAttribute("index", String.valueOf(colConfig.getIndex()));
                    }
                    if (colConfig.isVisible() != colConfig.getDefaults().isVisible()) {
                        colEl.setAttribute("visible", String.valueOf(colConfig.isVisible()));
                    }
                    el.appendChild(colEl);
                }
                root.appendChild(el);
            }
            XmlUtil.saveDocument(document, ForgeConstants.ITEM_VIEW_PREFS_FILE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}