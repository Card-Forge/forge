package forge.gui.deckeditor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import forge.deck.DeckBase;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.SColumnUtil.SortState;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.item.InventoryItem;
import forge.properties.NewConstants;

/** 
 * Handles editor preferences saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SEditorIO {
    /** Used in the XML IO to extract properties from PREFS file. */
    private enum ColumnProperty { /** */
        enumval, /** */
        identifier, /** */
        index, /** */
        show, /** */
        sortpriority, /** */
        sortstate, /** */
        width
    }

    /** Preferences (must match with PREFS file). */
    public enum EditorPreference { /** */
        stats_deck, /** */
        stats_catalog
    }

    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");

    private static final Map<EditorPreference, Boolean> PREFS
        = new HashMap<EditorPreference, Boolean>();

    private static final Map<ColumnName, TableColumnInfo<InventoryItem>> COLS
        = new TreeMap<ColumnName, TableColumnInfo<InventoryItem>>();

    /**
     * Retrieve a preference from the editor preference map.
     * 
     * @param name0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.EditorPreference}
     * @return TableColumnInfo<InventoryItem>
     */
    public static boolean getPref(final EditorPreference name0) {
        return PREFS.get(name0);
    }

    /**
     * Set a preference in the editor preference map.
     * 
     * @param name0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.EditorPreference}
     * @param val0 &emsp; boolean
     */
    public static void setPref(final EditorPreference name0, final boolean val0) {
        PREFS.put(name0, val0);
    }

    /**
     * Retrieve a custom column.
     * 
     * @param name0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.CatalogColumnName}
     * @return TableColumnInfo<InventoryItem>
     */
    public static TableColumnInfo<InventoryItem> getColumn(final ColumnName name0) {
        return COLS.get(name0);
    }

    /**
     * Saves the current deck, with various prompts depending on the
     * current save environment.
     * 
     * @return boolean, true if success
     */
    @SuppressWarnings("unchecked")
    public static boolean saveDeck() {
        final DeckController<DeckBase> controller = (DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();
        final String name = VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().getText();

        // Warn if no name
        if (name.equals("[New Deck]") || name.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Please name your deck using the 'Title' box.",
                    "Save Error!",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Confirm if overwrite
        else if (controller.fileExists(name)) {
            final int m = JOptionPane.showConfirmDialog(null,
                    "There is already a deck named '" + name + "'. Overwrite?",
                    "Overwrite Deck?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (m == JOptionPane.YES_OPTION) { controller.save(); }
            VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Current Deck");
        }
        // Confirm if a new deck will be created
        else {
            final int m = JOptionPane.showConfirmDialog(null,
                    "This will create a new deck named '" + name + "'. Continue?",
                    "Create Deck?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (m == JOptionPane.YES_OPTION) { controller.saveAs(name); }
            VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Current Deck");
        }

        return true;
    }

    /**
     * Prompts to save changes if necessary.
     * 
     * @return boolean, true if success
     */
    @SuppressWarnings("unchecked")
    public static boolean confirmSaveChanges() {
        if (!((DeckController<DeckBase>) CDeckEditorUI
                .SINGLETON_INSTANCE.getCurrentEditorController().getDeckController()).isSaved()) {
            final int choice = JOptionPane.showConfirmDialog(null,
                    "Save changes to current deck?",
                    "Save Changes?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.CANCEL_OPTION) { return false; }

            if (choice == JOptionPane.YES_OPTION && !saveDeck()) { return false; }
        }

        return true;
    }

    /** Publicly-accessible save method, to neatly handle exception handling. */
    public static void savePreferences() {
        try { save(); }
        catch (final Exception e) { e.printStackTrace(); }
    }

    /** Publicly-accessible load method, to neatly handle exception handling. */
    public static void loadPreferences() {
        try { load(); }
        catch (final Exception e) { e.printStackTrace(); }
    }

    /**
     * 
     * TODO: Write javadoc for this method.
     * 
     * @param <TItem> extends InventoryItem
     * @param <TModel> extends DeckBase
     */
    private static void save() throws Exception {
        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(NewConstants.PREFERENCES_FILE_EDITOR));

        writer.add(EVENT_FACTORY.createStartDocument());
        writer.add(NEWLINE);
        writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
        writer.add(EVENT_FACTORY.createAttribute("type", "editor"));
        writer.add(NEWLINE);

        for (final EditorPreference p : PREFS.keySet()) {
            writer.add(TAB);
            writer.add(EVENT_FACTORY.createStartElement("", "", "pref"));
            writer.add(EVENT_FACTORY.createAttribute(
                    "name", p.toString()));
            writer.add(EVENT_FACTORY.createAttribute(
                    "value", PREFS.get(p).toString()));
            writer.add(EVENT_FACTORY.createEndElement("", "", "pref"));
            writer.add(NEWLINE);
        }

        for (final ColumnName c : COLS.keySet()) {
            // If column is not in view, retain previous model index for the next time
            // that the column will be in the view.
            int index = SColumnUtil.getColumnViewIndex(c);
            if (index == -1) {
                index = COLS.get(c).getModelIndex();
            }

            writer.add(TAB);
            writer.add(EVENT_FACTORY.createStartElement("", "", "col"));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.enumval.toString(), COLS.get(c).getEnumValue()));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.identifier.toString(), COLS.get(c).getIdentifier().toString()));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.index.toString(), String.valueOf(index)));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.show.toString(), String.valueOf(COLS.get(c).isShowing())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.sortpriority.toString(), String.valueOf(COLS.get(c).getSortPriority())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.sortstate.toString(), String.valueOf(COLS.get(c).getSortState())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.width.toString(), String.valueOf(COLS.get(c).getWidth())));
            writer.add(EVENT_FACTORY.createEndElement("", "", "col"));
            writer.add(NEWLINE);
        }

        writer.add(EVENT_FACTORY.createEndDocument());
        writer.flush();
        writer.close();
    }

    private static void load() throws Exception {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        final String fileAddress = NewConstants.PREFERENCES_FILE_EDITOR;
        final XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(fileAddress));

        PREFS.clear();
        COLS.clear();

        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;
        EditorPreference pref;
        TableColumnInfo<InventoryItem> tempcol;
        String tagname;

        while (reader.hasNext()) {
            event = reader.nextEvent();

            if (event.isStartElement()) {
                element = event.asStartElement();
                tagname = element.getName().getLocalPart();

                // Assemble preferences
                if (tagname.equals("pref")) {
                    // Retrieve name of pref
                    attributes = element.getAttributes();
                    pref = EditorPreference.valueOf(((Attribute) attributes.next()).getValue());

                    // Add to map
                    PREFS.put(pref, Boolean.valueOf(((Attribute) attributes.next()).getValue()));
                }
                // Assemble columns
                else if (tagname.equals("col")) {
                    attributes = element.getAttributes();
                    tempcol = new TableColumnInfo<InventoryItem>();

                    while (attributes.hasNext()) {
                        attribute = (Attribute) attributes.next();
                        if (attribute.getName().toString().equals(ColumnProperty.enumval.toString())) {
                            COLS.put(ColumnName.valueOf(attribute.getValue()), tempcol);
                            tempcol.setEnumValue(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.identifier.toString())) {
                            tempcol.setIdentifier(attribute.getValue());
                            tempcol.setHeaderValue(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.width.toString())) {
                            tempcol.setPreferredWidth(Integer.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.show.toString())) {
                            tempcol.setShowing(Boolean.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.sortpriority.toString())) {
                            tempcol.setSortPriority(Integer.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.sortstate.toString())) {
                            tempcol.setSortState(SortState.valueOf(attribute.getValue().toString()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.index.toString())) {
                            tempcol.setModelIndex(Integer.valueOf(attribute.getValue()));
                        }
                    }
                }
            }
        }

        SColumnUtil.attachSortAndDisplayFunctions();
    }
}
