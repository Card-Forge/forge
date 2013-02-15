package forge.gui.framework;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
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
import forge.properties.NewConstants;
import forge.view.FView;


/**
 * Handles layout saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SLayoutIO {
    /** Each cell must save these elements of its display. */
    private enum Property {
        x,
        y,
        w,
        h,
        doc
    }

    private static String fileDefault = null;
    private static String filePreferred = null;
    private static final XMLEventFactory EF = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EF.createDTD("\n");
    private static final XMLEvent TAB = EF.createDTD("\t");

    /**
     * Gets preferred layout file corresponding to current state of UI.
     * @return {@link java.lang.String}
     */
    public static String getFilePreferred() {
        return filePreferred;
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

    /**
     * Publicly-accessible load method, to neatly handle exception handling.
     * @param f0 &emsp; {@link java.io.File}
     */
    public static void loadLayout(final File f0) {
        try { load(f0); }
        catch (final Exception e) { e.printStackTrace(); }
    }

    private synchronized static void save(final File f0) throws Exception {
        final String fWriteTo;
        SLayoutIO.setFilesForState();

        if (f0 == null) {
            fWriteTo = filePreferred;
        }
        else {
            fWriteTo = f0.getPath();
        }

        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(fWriteTo));
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
        final JPanel pnl = FView.SINGLETON_INSTANCE.getPnlContent();
        double x0, y0, w0, h0;

        writer.add(EF.createStartDocument());
        writer.add(NEWLINE);
        writer.add(EF.createStartElement("", "", "layout"));
        writer.add(NEWLINE);

        for (final DragCell cell : cells) {
            x0 = ((double) Math.round(((double) cell.getX() / (double) pnl.getWidth()) * 100000)) / 100000;
            y0 = ((double) Math.round(((double) cell.getY() / (double) pnl.getHeight()) * 100000)) / 100000;
            w0 = ((double) Math.round(((double) cell.getW() / (double) pnl.getWidth()) * 100000)) / 100000;
            h0 = ((double) Math.round(((double) cell.getH() / (double) pnl.getHeight()) * 100000)) / 100000;

            //cell.setRoughBounds(x, y, w, h);

            writer.add(TAB);
            writer.add(EF.createStartElement("", "", "cell"));
            writer.add(EF.createAttribute(Property.x.toString(), String.valueOf(x0)));
            writer.add(EF.createAttribute(Property.y.toString(), String.valueOf(y0)));
            writer.add(EF.createAttribute(Property.w.toString(), String.valueOf(w0)));
            writer.add(EF.createAttribute(Property.h.toString(), String.valueOf(h0)));
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

    private static void load(final File f) throws Exception {
        final FView view = FView.SINGLETON_INSTANCE;
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        SLayoutIO.setFilesForState();

        view.getPnlInsets().removeAll();
        view.getPnlInsets().setLayout(new BorderLayout());
        view.getPnlInsets().add(view.getPnlContent(), BorderLayout.CENTER);
        view.getPnlInsets().setBorder(new EmptyBorder(
                SLayoutConstants.BORDER_T, SLayoutConstants.BORDER_T, 0, 0));

        final XMLEventReader reader;
        if (f != null && f.exists()) {
            reader = inputFactory.createXMLEventReader(new FileInputStream(f));
        }
        else if (new File(filePreferred).exists()) {
            reader = inputFactory.createXMLEventReader(new FileInputStream(filePreferred));
        }
        else {
            reader = inputFactory.createXMLEventReader(new FileInputStream(fileDefault));
        }

        view.removeAllDragCells();
        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;
        DragCell cell = null;
        double x0 = 0, y0 = 0, w0 = 0, h0 = 0;

        while (reader.hasNext()) {
            event = reader.nextEvent();

            if (event.isStartElement()) {
                element = event.asStartElement();

                if (element.getName().getLocalPart().equals("cell")) {
                    attributes = element.getAttributes();
                    while (attributes.hasNext()) {
                        attribute = (Attribute) attributes.next();
                        if (attribute.getName().toString().equals(Property.x.toString())) {
                            x0 = Double.valueOf(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(Property.y.toString())) {
                            y0 = Double.valueOf(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(Property.w.toString())) {
                            w0 = Double.valueOf(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(Property.h.toString())) {
                            h0 = Double.valueOf(attribute.getValue());
                        }
                    }

                    cell = new DragCell();
                    cell.setRoughBounds(x0, y0, w0, h0);
                    FView.SINGLETON_INSTANCE.addDragCell(cell);
                }
                else if (element.getName().getLocalPart().equals("doc")) {
                    event = reader.nextEvent();
                    try {
                        cell.addDoc(EDocID.valueOf(event.asCharacters().getData()).getDoc());
                    } catch (IllegalArgumentException e) { /* ignore; just don't add invalid element */ }
                }
            }
        }

        // Rough bounds are all in place; resize the window.
        SResizingUtil.resizeWindow();
    }

    private static void createNode(final XMLEventWriter writer0, final Property name0,
            final String val0) throws XMLStreamException {

        writer0.add(TAB);
        writer0.add(TAB);
        writer0.add(EF.createStartElement("", "", name0.toString()));
        writer0.add(EF.createCharacters(val0));
        writer0.add(EF.createEndElement("", "", name0.toString()));
        writer0.add(NEWLINE);
    }

    /**
     * Updates preferred / default layout addresses particular to each UI state.
     * Always called before a load or a save, to ensure file addresses are correct.
     */
    private static void setFilesForState() {
        final String dir = NewConstants.LAYOUT_DIR;

        switch(FControl.SINGLETON_INSTANCE.getState()) {
            case HOME_SCREEN:
                fileDefault = dir + "home_default.xml";
                filePreferred = dir + "home_preferred.xml";
                break;
            case MATCH_SCREEN:
                fileDefault = dir + "match_default.xml";
                filePreferred = dir + "match_preferred.xml";
                break;
            case DECK_EDITOR_CONSTRUCTED:
            case DECK_EDITOR_LIMITED:
            case DECK_EDITOR_QUEST:
            case DRAFTING_PROCESS:
            case QUEST_CARD_SHOP:
                fileDefault = dir + "editor_default.xml";
                filePreferred = dir + "editor_preferred.xml";
                break;
            case QUEST_BAZAAR:
                fileDefault = dir + "bazaar_default.xml";
                filePreferred = dir + "bazaar_preferred.xml";
                break;
            default:
                throw new IllegalStateException("Layout load failed; UI state unknown.");
        }
    }
}
