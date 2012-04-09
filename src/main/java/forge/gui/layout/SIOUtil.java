package forge.gui.layout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;


/** Package-private class for handling layout saving and loading. */
final class SIOUtil {
    /** Each cell must save these elements of its display. */
    private enum Element {
        x,
        y,
        w,
        h,
        doc
    };

    private static final String FILE = "layout_default.xml";
    private static final XMLEventFactory EF = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EF.createDTD("\n");
    private static final XMLEvent TAB = EF.createDTD("\t");

    /** Publicly-accessible save method, to neatly handle exception handling. */
    public static void saveLayout() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("This operation should be independent of the EDT.");
        }

       // try { save(); }
       // catch (final Exception e) { e.printStackTrace(); }
    }

    /** Publicly-accessible load method, to neatly handle exception handling. */
    public static void loadLayout() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("This operation should be independent of the EDT.");
        }

        // try { load(); }
        // catch (final Exception e) { e.printStackTrace(); }
        // TODO save layout after resize / rearrange
        // TODO layout save
        int todo = 5;
    }

    private static void save() throws Exception {
        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(FILE));
        final List<DragCell> cells = FViewNew.SINGLETON_INSTANCE.getDragCells();

        writer.add(EF.createStartDocument());
        writer.add(EF.createStartElement("", "", "layout"));

        for (final DragCell cell : cells) {
            writer.add(EF.createStartElement("", "", "cell"));

            createNode(writer, Element.x, String.valueOf(cell.getRoughX()));
            createNode(writer, Element.y, String.valueOf(cell.getRoughY()));
            createNode(writer, Element.w, String.valueOf(cell.getRoughW()));
            createNode(writer, Element.h, String.valueOf(cell.getRoughH()));

            for (final IVDoc vDoc : cell.getDocs()) {
                createNode(writer, Element.doc, vDoc.getDocumentID().toString());
            }

            writer.add(EF.createEndElement("", "", "cell"));
        }

        writer.add(EF.createEndDocument());
        writer.flush();
        writer.close();
    }

    private static void load() throws Exception {
        final FViewNew view = FViewNew.SINGLETON_INSTANCE;
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        final XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(FILE));

        view.removeAllDragCells();
        /*while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                /*StartElement startElement = event.asStartElement();

                if (startElement.getName().getLocalPart() == (ITEM)) {

                }*

                //System.err.println(event.isStartElement());
            }
        }*/
    }

    private static void createNode(final XMLEventWriter writer0, final Element name0,
            final String val0) throws XMLStreamException {

        writer0.add(TAB);
        writer0.add(EF.createStartElement("", "", name0.toString()));
        writer0.add(EF.createCharacters(val0));
        writer0.add(EF.createEndElement("", "", name0.toString()));
        writer0.add(NEWLINE);
    }
}
