package forge.gui.framework;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import forge.control.FControl;
import forge.control.FControl.Screens;
import forge.properties.FileLocation;
import forge.properties.NewConstants;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;
import forge.view.FView;


/**
 * Handles layout saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SLayoutIO {
    /** Each cell must save these elements of its display. */
    private static class Property {
        public final static String x = "x"; 
        public final static String y = "y";
        public final static String w = "w";
        public final static String h = "h";
        public final static String doc = "doc";
    }

    private static final XMLEventFactory EF = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EF.createDTD("\n");
    private static final XMLEvent TAB = EF.createDTD("\t");

    /**
     * Gets preferred layout file corresponding to current state of UI.
     * @return {@link java.lang.String}
     */
    public static String getFilePreferred() {
        return SLayoutIO.getFileForState(FControl.SINGLETON_INSTANCE.getState()).userPrefLoc;
    }

    /** Publicly-accessible save method, to neatly handle exception handling.
     * @param f0 file to save layout to, if null, saves to filePreferred
     * 
     * 
     */
    public static void saveLayout(final File f0) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("This operation should be independent of the EDT.");
        }

        try { save(f0); }
        catch (final Exception e) { e.printStackTrace(); }

    }

    private synchronized static void save(final File f0) throws Exception {
        final String fWriteTo;
        FileLocation file = SLayoutIO.getFileForState(FControl.SINGLETON_INSTANCE.getState());

        if (f0 == null) {
            if (null == file) {
                return;
            }
            fWriteTo = file.userPrefLoc;
        }
        else {
            fWriteTo = f0.getPath();
        }

        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(fWriteTo));
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();

        writer.add(EF.createStartDocument());
        writer.add(NEWLINE);
        writer.add(EF.createStartElement("", "", "layout"));
        writer.add(NEWLINE);

        for (final DragCell cell : cells) {
            cell.updateRoughBounds();
            RectangleOfDouble bounds = cell.getRoughBounds();
            
            writer.add(TAB);
            writer.add(EF.createStartElement("", "", "cell"));
            writer.add(EF.createAttribute(Property.x.toString(), String.valueOf(Math.rint(bounds.getX() * 100000) / 100000)));
            writer.add(EF.createAttribute(Property.y.toString(), String.valueOf(Math.rint(bounds.getY() * 100000) / 100000)));
            writer.add(EF.createAttribute(Property.w.toString(), String.valueOf(Math.rint(bounds.getW() * 100000) / 100000)));
            writer.add(EF.createAttribute(Property.h.toString(), String.valueOf(Math.rint(bounds.getH() * 100000) / 100000)));
            writer.add(NEWLINE);

            for (final IVDoc<? extends ICDoc> vDoc : cell.getDocs()) {
                createNode(writer, Property.doc, vDoc.getDocumentID().toString());
            }

            writer.add(TAB);
            writer.add(EF.createEndElement("", "", "cell"));
            writer.add(NEWLINE);
        }

        writer.add(EF.createEndDocument());
        writer.flush();
        writer.close();
    }

    public static void loadLayout(final File f) {
        final FView view = FView.SINGLETON_INSTANCE;
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        FileLocation file = SLayoutIO.getFileForState(FControl.SINGLETON_INSTANCE.getState());

        view.getPnlInsets().removeAll();
        view.getPnlInsets().setLayout(new BorderLayout());
        view.getPnlInsets().add(view.getPnlContent(), BorderLayout.CENTER);
        view.getPnlInsets().setBorder(new EmptyBorder(SLayoutConstants.BORDER_T, SLayoutConstants.BORDER_T, 0, 0));

        view.removeAllDragCells();
        
        // Read a model for new layout
        MapOfLists<RectangleOfDouble, EDocID> model = null;
        try {
            FileInputStream fis = null;
            if (f != null && f.exists()) 
                fis = new FileInputStream(f);
            else {
                File userSetting = new File(file.userPrefLoc);
                fis = userSetting.exists() ? new FileInputStream(userSetting) : new FileInputStream(file.defaultLoc);
            }
    
            model = readLayout(inputFactory.createXMLEventReader(fis));
        } catch (final Exception e) { e.printStackTrace(); }
        
        // Apply new layout
        DragCell cell = null;
        for(Entry<RectangleOfDouble, Collection<EDocID>> kv : model.entrySet()) {
            cell = new DragCell();
            cell.setRoughBounds(kv.getKey());
            FView.SINGLETON_INSTANCE.addDragCell(cell); 
            for(EDocID edoc : kv.getValue()) {
                try {
//                    System.out.println(String.format("adding doc %s -> %s",  edoc, edoc.getDoc()));
                    cell.addDoc(edoc.getDoc());
                } catch (IllegalArgumentException e) {
                    System.err.println("Failed to get doc for " + edoc); 
                }
                
            }
            
        }

        // Rough bounds are all in place; resize the window.
        SResizingUtil.resizeWindow();
    }

    private static MapOfLists<RectangleOfDouble, EDocID> readLayout(final XMLEventReader reader)
            throws XMLStreamException {
        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;
        double x0 = 0, y0 = 0, w0 = 0, h0 = 0;
        
        MapOfLists<RectangleOfDouble, EDocID> model = new HashMapOfLists<RectangleOfDouble, EDocID>(CollectionSuppliers.<EDocID>arrayLists());
        
        RectangleOfDouble currentKey = null;
        while (null != reader && reader.hasNext()) {
            event = reader.nextEvent();

            if (event.isStartElement()) {
                element = event.asStartElement();

                if (element.getName().getLocalPart().equals("cell")) {
                    attributes = element.getAttributes();
                    while (attributes.hasNext()) {
                        attribute = (Attribute) attributes.next();
                        double val = Double.parseDouble(attribute.getValue());
                        String atrName = attribute.getName().toString();

                        if (atrName.equals(Property.x))      x0 = val;
                        else if (atrName.equals(Property.y)) y0 = val;
                        else if (atrName.equals(Property.w)) w0 = val;
                        else if (atrName.equals(Property.h)) h0 = val;
                    }
                    currentKey = new RectangleOfDouble(x0, y0, w0, h0);
                }
                else if (element.getName().getLocalPart().equals(Property.doc)) {
                    event = reader.nextEvent();
                    model.add(currentKey, EDocID.valueOf(event.asCharacters().getData()));
                }
            }
        }
        return model;
    }

    private static void createNode(final XMLEventWriter writer0, final String propertyName, final String value) throws XMLStreamException {
        writer0.add(TAB);
        writer0.add(TAB);
        writer0.add(EF.createStartElement("", "", propertyName));
        writer0.add(EF.createCharacters(value));
        writer0.add(EF.createEndElement("", "", propertyName));
        writer0.add(NEWLINE);
    }

    /**
     * Updates preferred / default layout addresses particular to each UI state.
     * Always called before a load or a save, to ensure file addresses are correct.
     * @return 
     */
    private static FileLocation getFileForState(Screens state) {
        switch(state) {
            case HOME_SCREEN:
                return NewConstants.HOME_LAYOUT_FILE;
            case MATCH_SCREEN:
                return NewConstants.MATCH_LAYOUT_FILE;

            case DECK_EDITOR_CONSTRUCTED:
            case DECK_EDITOR_LIMITED:
            case DECK_EDITOR_QUEST:
            case DRAFTING_PROCESS:
            case QUEST_CARD_SHOP:
                return NewConstants.EDITOR_LAYOUT_FILE;

            case QUEST_BAZAAR:
                return null;

            default:
                throw new IllegalStateException("Layout load failed; UI state unknown.");
        }
    }
}
 