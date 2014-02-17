package forge.gui.toolbox.itemmanager;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.google.common.base.Function;

import forge.gui.toolbox.itemmanager.views.ColumnDef;
import forge.gui.toolbox.itemmanager.views.GroupDef;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.ItemColumnConfig;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.item.InventoryItem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/** 
 * Preferences associated with individual cards
 *
 */
public enum ItemManagerConfig {
    STRING_ONLY(SColumnUtil.getStringColumn(), false, false,
            null, null, 1, 0),
    CARD_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, false,
            null, null, 4, 0),
    DECK_EDITOR(SColumnUtil.getDeckEditorDefaultColumns(), false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    DRAFT_PACK(SColumnUtil.getDraftPackDefaultColumns(), false, false,
            null, null, 4, 1),
    DRAFT_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    SEALED_POOL(SColumnUtil.getCatalogDefaultColumns(false), false, false,
            GroupDef.COLOR, ColumnDef.CMC, 4, 1),
    SPELL_SHOP(SColumnUtil.getSpellShopDefaultColumns(), false, false,
            null, null, 4, 0),
    QUEST_INVENTORY(SColumnUtil.getQuestInventoryDefaultColumns(), false, false,
            null, null, 4, 0),
    QUEST_EDITOR_POOL(SColumnUtil.getQuestEditorPoolDefaultColumns(), false, false,
            null, null, 4, 0),
    QUEST_DECK_EDITOR(SColumnUtil.getQuestDeckEditorDefaultColumns(), false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    AVATAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false,
            null, null, 4, 0),
    SCHEME_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false,
            null, null, 4, 0),
    PLANAR_POOL(SColumnUtil.getSpecialCardPoolDefaultColumns(), true, false,
            null, null, 4, 0),
    COMMANDER_POOL(SColumnUtil.getCatalogDefaultColumns(true), true, false,
            null, null, 4, 0),
    WORKSHOP_CATALOG(SColumnUtil.getCatalogDefaultColumns(true), true, true,
            null, null, 4, 0),
    DECK_VIEWER(SColumnUtil.getDeckViewerDefaultColumns(), false, false,
            GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 4, 1),
    CONSTRUCTED_DECKS(SColumnUtil.getDecksDefaultColumns(true, true), false, false,
            null, null, 3, 0),
    DRAFT_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false,
            null, null, 3, 0),
    SEALED_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false,
            null, null, 3, 0),
    QUEST_DECKS(SColumnUtil.getDecksDefaultColumns(true, false), false, false,
            null, null, 3, 0),
    PRECON_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false,
            null, null, 3, 0),
    QUEST_EVENT_DECKS(SColumnUtil.getDecksDefaultColumns(false, false), false, false,
            null, null, 3, 0);

    private Map<ColumnDef, ItemColumnConfig> cols;
    private boolean showUniqueCardsOption;

    private Prop<Boolean> uniqueCardsOnly;
    private Prop<GroupDef> groupBy;
    private Prop<ColumnDef> pileBy;
    private Prop<Integer> imageColumnCount;
    private Prop<Integer> viewIndex;

    private ItemManagerConfig(final Map<ColumnDef, ItemColumnConfig> cols0, boolean showUniqueCardsOption0, boolean uniqueCardsOnly0, GroupDef groupBy0, ColumnDef pileBy0, int imageColumnCount0, int viewIndex0) {
        cols = cols0;
        showUniqueCardsOption = showUniqueCardsOption0;

        uniqueCardsOnly = new Prop<Boolean>(uniqueCardsOnly0);
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
    }

    public Map<ColumnDef, ItemColumnConfig> getCols() {
        return cols;
    }

    public void addColOverride(Map<ColumnDef, ItemColumn> colOverrides, ColumnDef colDef) {
        ItemColumnConfig colConfig = cols.get(colDef);
        addColOverride(colOverrides, colDef, colConfig.getFnSort(), colConfig.getFnDisplay());
    }
    public void addColOverride(Map<ColumnDef, ItemColumn> colOverrides, ColumnDef colDef,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        colOverrides.put(colDef, new ItemColumn(cols.get(colDef), fnSort0, fnDisplay0));
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

    private static String filename;
    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");

    public static void load(String filename0) {
        filename = filename0;

        /*try {
            final XMLInputFactory in = XMLInputFactory.newInstance();
            final XMLEventReader reader = in.createXMLEventReader(new FileInputStream(filename));

            XMLEvent event;
            StartElement element;
            Iterator<?> attributes;
            Attribute attribute;
            ItemManagerConfig config;

            while (reader.hasNext()) {
                event = reader.nextEvent();

                if (event.isStartElement()) {
                    element = event.asStartElement();
                    try {
                        config = Enum.valueOf(ItemManagerConfig.class, element.getName().getLocalPart());

                        attributes = element.getAttributes();

                        while (attributes.hasNext()) {
                            attribute = (Attribute) attributes.next();
                            switch (attribute.getName().toString()) {
                            case "name":
                                break;
                            case "stars":
                                break;
                            }
                        }
                    }
                    catch (Exception ex) {
                        config = null;
                    }
                }
            }
        }
        catch (final FileNotFoundException e) {
             ignore; it's ok if this file doesn't exist 
        }
        catch (final Exception e) {
            e.printStackTrace();
        }*/
    }

    public static void save() {
        /*try {
            final XMLOutputFactory out = XMLOutputFactory.newInstance();
            final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(filename));
    
            writer.add(EVENT_FACTORY.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
            writer.add(EVENT_FACTORY.createAttribute("type", "cards"));
            writer.add(NEWLINE);
    
            for (ItemManagerConfig config : ItemManagerConfig.values()) {
                writer.add(TAB);
                writer.add(EVENT_FACTORY.createStartElement("", "", config.name()));
                writer.add(EVENT_FACTORY.createAttribute("name", "TODO"));
                writer.add(EVENT_FACTORY.createEndElement("", "", "card"));
                writer.add(NEWLINE);
            }
    
            writer.add(EVENT_FACTORY.createEndDocument());
            writer.flush();
            writer.close();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }*/
    }
}