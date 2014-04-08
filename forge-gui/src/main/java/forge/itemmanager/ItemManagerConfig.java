package forge.itemmanager;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import forge.itemmanager.ItemColumnConfig.SortState;
import forge.properties.ForgeConstants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 
 * Preferences associated with individual cards
 *
 */
public enum ItemManagerConfig {
    STRING_ONLY(SColumnUtil.getStringColumn(), false, false, true,
            null, null, 1, 0),
    CARD_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, false, false,
            null, null, 4, 0),
    DECK_EDITOR(SColumnUtil.getDeckEditorDefaultColumns(), false, false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    DRAFT_PACK(SColumnUtil.getDraftPackDefaultColumns(), false, false, true,
            null, null, 4, 1),
    DRAFT_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    SEALED_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false, false,
            GroupDef.COLOR, ColumnDef.CMC, 4, 1),
    SPELL_SHOP(SColumnUtil.getSpellShopDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_INVENTORY(SColumnUtil.getQuestInventoryDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_EDITOR_POOL(SColumnUtil.getQuestEditorPoolDefaultColumns(), false, false, false,
            null, null, 4, 0),
    QUEST_DECK_EDITOR(SColumnUtil.getQuestDeckEditorDefaultColumns(), false, false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    AVATAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, false,
            null, null, 4, 0),
    SCHEME_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, false,
            null, null, 4, 0),
    PLANAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false, false,
            null, null, 4, 0),
    COMMANDER_POOL(SColumnUtil.getCatalogDefaultColumns(true), true, false, false,
            null, null, 4, 0),
    WORKSHOP_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, true, false,
            null, null, 4, 0),
    DECK_VIEWER(SColumnUtil.getDeckViewerDefaultColumns(), false, false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    CONSTRUCTED_DECKS(SColumnUtil.getDecksDefaultColumns(true, true), false, false, false,
            null, null, 3, 0),
    DRAFT_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    SEALED_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    QUEST_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false, false,
            null, null, 3, 0),
    PRECON_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false, false,
            null, null, 3, 0),
    QUEST_EVENT_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false, false,
            null, null, 3, 0);

    private Map<ColumnDef, ItemColumnConfig> cols;
    private boolean showUniqueCardsOption;

    private Prop<Boolean> uniqueCardsOnly;
    private Prop<Boolean> hideFilters;
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
        private void writeValue(final XMLEventWriter writer, String localName) throws XMLStreamException {
            if (value == null) {
                if (defaultValue != null) {
                    writer.add(EVENT_FACTORY.createAttribute(localName, ""));
                }
            }
            else if (!value.equals(defaultValue)) {
                if (value instanceof Enum) { //use Enum.name to prevent issues with toString() overrides
                    writer.add(EVENT_FACTORY.createAttribute(localName, ((Enum)value).name()));
                }
                else {
                    writer.add(EVENT_FACTORY.createAttribute(localName, String.valueOf(value)));
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

    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");

    public static void load() {
        try {
            final XMLInputFactory in = XMLInputFactory.newInstance();
            final XMLEventReader reader = in.createXMLEventReader(new FileInputStream(ForgeConstants.ITEM_VIEW_PREFS_FILE));

            XMLEvent event;
            StartElement element;
            Iterator<?> attributes;
            Attribute attr;
            ItemManagerConfig config = null;
            Map<String, String> attrMap = new HashMap<String, String>();

            while (reader.hasNext()) {
                event = reader.nextEvent();

                if (event.isStartElement()) {
                    element = event.asStartElement();
                    attrMap.clear();
                    attributes = element.getAttributes();
                    while (attributes.hasNext()) {
                        attr = (Attribute) attributes.next();
                        attrMap.put(attr.getName().toString(), attr.getValue());
                    }
                    switch (element.getName().getLocalPart()) {
                    case "config":
                        try {
                            config = Enum.valueOf(ItemManagerConfig.class, attrMap.get("name"));
                            if (attrMap.containsKey("uniqueCardsOnly")) {
                                config.uniqueCardsOnly.value = Boolean.parseBoolean(attrMap.get("uniqueCardsOnly"));
                            }
                            if (attrMap.containsKey("hideFilters")) {
                                config.hideFilters.value = Boolean.parseBoolean(attrMap.get("hideFilters"));
                            }
                            if (attrMap.containsKey("groupBy")) {
                                String value = attrMap.get("groupBy");
                                if (value.isEmpty()) {
                                    config.groupBy.value = null;
                                }
                                else {
                                    config.groupBy.value = Enum.valueOf(GroupDef.class, value);
                                }
                            }
                            if (attrMap.containsKey("pileBy")) {
                                String value = attrMap.get("pileBy");
                                if (value.isEmpty()) {
                                    config.pileBy.value = null;
                                }
                                else {
                                    config.pileBy.value = Enum.valueOf(ColumnDef.class, value);
                                }
                            }
                            if (attrMap.containsKey("imageColumnCount")) {
                                config.imageColumnCount.value = Integer.parseInt(attrMap.get("imageColumnCount"));
                            }
                            if (attrMap.containsKey("viewIndex")) {
                                config.viewIndex.value = Integer.parseInt(attrMap.get("viewIndex"));
                            }
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "col":
                        if (config == null) { break; }
                        try {
                            ItemColumnConfig colConfig = config.cols.get(Enum.valueOf(ColumnDef.class, attrMap.get("name")));
                            if (attrMap.containsKey("width")) {
                                colConfig.setPreferredWidth(Integer.parseInt(attrMap.get("width")));
                            }
                            if (attrMap.containsKey("sortPriority")) {
                                colConfig.setSortPriority(Integer.parseInt(attrMap.get("sortPriority")));
                            }
                            if (attrMap.containsKey("sortState")) {
                                colConfig.setSortState(Enum.valueOf(SortState.class, attrMap.get("sortState")));
                            }
                            if (attrMap.containsKey("index")) {
                                colConfig.setIndex(Integer.parseInt(attrMap.get("index")));
                            }
                            if (attrMap.containsKey("visible")) {
                                colConfig.setVisible(Boolean.parseBoolean(attrMap.get("visible")));
                            }
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
        catch (final FileNotFoundException e) {
            //ignore; it's ok if this file doesn't exist 
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            final XMLOutputFactory out = XMLOutputFactory.newInstance();
            final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(ForgeConstants.ITEM_VIEW_PREFS_FILE));
    
            writer.add(EVENT_FACTORY.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
            writer.add(EVENT_FACTORY.createAttribute("type", "item_view"));
            writer.add(NEWLINE);
    
            for (ItemManagerConfig config : ItemManagerConfig.values()) {
                writer.add(TAB);
                writer.add(EVENT_FACTORY.createStartElement("", "", "config"));
                writer.add(EVENT_FACTORY.createAttribute("name", config.name()));
                config.uniqueCardsOnly.writeValue(writer, "uniqueCardsOnly");
                config.hideFilters.writeValue(writer, "hideFilters");
                config.groupBy.writeValue(writer, "groupBy");
                config.pileBy.writeValue(writer, "pileBy");
                config.imageColumnCount.writeValue(writer, "imageColumnCount");
                config.viewIndex.writeValue(writer, "viewIndex");
                writer.add(NEWLINE);
                for (ItemColumnConfig colConfig : config.cols.values()) {
                    writer.add(TAB);
                    writer.add(TAB);
                    writer.add(EVENT_FACTORY.createStartElement("", "", "col"));

                    writer.add(EVENT_FACTORY.createAttribute("name", colConfig.getDef().name()));
                    if (colConfig.getPreferredWidth() != colConfig.getDefaults().getPreferredWidth()) {
                        writer.add(EVENT_FACTORY.createAttribute("width", String.valueOf(colConfig.getPreferredWidth())));
                    }
                    if (colConfig.getSortPriority() != colConfig.getDefaults().getSortPriority()) {
                        writer.add(EVENT_FACTORY.createAttribute("sortPriority", String.valueOf(colConfig.getSortPriority())));
                    }
                    if (colConfig.getSortState() != colConfig.getDefaults().getSortState()) {
                        writer.add(EVENT_FACTORY.createAttribute("sortState", String.valueOf(colConfig.getSortState())));
                    }
                    if (colConfig.getIndex() != colConfig.getDefaults().getIndex()) {
                        writer.add(EVENT_FACTORY.createAttribute("index", String.valueOf(colConfig.getIndex())));
                    }
                    if (colConfig.isVisible() != colConfig.getDefaults().isVisible()) {
                        writer.add(EVENT_FACTORY.createAttribute("visible", String.valueOf(colConfig.isVisible())));
                    }

                    writer.add(EVENT_FACTORY.createEndElement("", "", "col"));
                    writer.add(NEWLINE);
                }
                writer.add(TAB);
                writer.add(EVENT_FACTORY.createEndElement("", "", "config"));
                writer.add(NEWLINE);
            }

            writer.add(EVENT_FACTORY.createEndDocument());
            writer.flush();
            writer.close();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }
}