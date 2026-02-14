package forge.gui.framework;

import forge.Singletons;
import forge.gui.FThreads;
import forge.gui.SOverlayUtils;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.FileLocation;
import forge.localinstance.properties.ForgeConstants;
import forge.toolbox.FAbsolutePositioner;
import forge.toolbox.SaveOpenDialog;
import forge.toolbox.SaveOpenDialog.Filetypes;
import forge.util.ThreadUtil;
import forge.view.FFrame;
import forge.view.FView;

import javax.swing.border.EmptyBorder;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import java.awt.*;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles layout saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SLayoutIO {
    private static class Property {
        public final static String x = "x"; 
        public final static String y = "y";
        public final static String w = "w";
        public final static String h = "h";
        public final static String sel = "sel";
        public final static String doc = "doc";
        public final static String max = "max";
        public final static String fs = "fs";
    }

    private static final XMLEventFactory EF = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EF.createDTD("\n");
    private static final XMLEvent TAB = EF.createDTD("\t");

    private final static AtomicBoolean saveWindowRequested = new AtomicBoolean(false);


    public static void saveLayout() {
        final SaveOpenDialog dlgSave = new SaveOpenDialog();
        final FileLocation layoutFile = Singletons.getControl().getCurrentScreen().getLayoutFile();
        final File defFile = layoutFile != null ? new File(layoutFile.userPrefLoc) : null;
        final File saveFile = dlgSave.SaveDialog(defFile, Filetypes.LAYOUT);
        if (saveFile != null) {
            SLayoutIO.saveLayout(saveFile);
        }
    }

    public static void openLayout() {
        SOverlayUtils.genericOverlay();

        final SaveOpenDialog dlgOpen = new SaveOpenDialog();
        final FileLocation layoutFile = Singletons.getControl().getCurrentScreen().getLayoutFile();
        final File defFile = layoutFile != null ? new File(layoutFile.userPrefLoc) : null;
        final File loadFile = dlgOpen.OpenDialog(defFile, Filetypes.LAYOUT);

        if (loadFile != null) {
            FView.SINGLETON_INSTANCE.getPnlContent().removeAll();
            // let it redraw everything first

            FThreads.invokeInEdtLater(() -> {
                SLayoutIO.loadLayout(loadFile);
                Singletons.getControl().getCurrentScreen().getView().populate();
                SLayoutIO.saveLayout(null);
                SOverlayUtils.hideOverlay();
            });
        }
    }

    public static void revertLayout() {
        SOverlayUtils.genericOverlay();
        FView.SINGLETON_INSTANCE.getPnlContent().removeAll();

        FThreads.invokeInEdtLater(() -> {
            SLayoutIO.loadLayout(null);
            Singletons.getControl().getCurrentScreen().getView().populate();
            SOverlayUtils.hideOverlay();
        });
    }

    public static void saveWindowLayout() {
        if (saveWindowRequested.getAndSet(true)) { return; }
        ThreadUtil.delay(500, () -> {
            finishSaveWindowLayout();
            saveWindowRequested.set(false);
        });
    }
    
    private synchronized static void finishSaveWindowLayout() {
        final FFrame window = FView.SINGLETON_INSTANCE.getFrame();
        if (window.isMinimized()) { return; } //don't update saved layout if minimized
        
        final Rectangle normalBounds = window.getNormalBounds();
        
        final FileLocation file = ForgeConstants.WINDOW_LAYOUT_FILE;
        final String fWriteTo = file.userPrefLoc;
        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        XMLEventWriter writer = null;
        try (FileOutputStream fos = new FileOutputStream(fWriteTo)) {
            writer = out.createXMLEventWriter(fos);

            writer.add(EF.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EF.createStartElement("", "", "layout"));
            writer.add(EF.createAttribute(Property.x, String.valueOf(normalBounds.x)));
            writer.add(EF.createAttribute(Property.y, String.valueOf(normalBounds.y)));
            writer.add(EF.createAttribute(Property.w, String.valueOf(normalBounds.width)));
            writer.add(EF.createAttribute(Property.h, String.valueOf(normalBounds.height)));
            writer.add(EF.createAttribute(Property.max, window.isMaximized() ? "1" : "0"));
            writer.add(EF.createAttribute(Property.fs, window.isFullScreen() ? "1" : "0"));
            writer.add(EF.createEndElement("", "", "layout"));
            writer.flush();
            writer.add(EF.createEndDocument());
        } catch (XMLStreamException | IOException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (XMLStreamException ignored) {}
            }
        }
    }
    
    public static void loadWindowLayout() {
        final FFrame window = FView.SINGLETON_INSTANCE.getFrame();
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        final FileLocation file = ForgeConstants.WINDOW_LAYOUT_FILE;
        boolean usedCustomPrefsFile = false;
        FileInputStream fis = null;
        try {
            File userSetting = new File(file.userPrefLoc);
            if (userSetting.exists()) {
                usedCustomPrefsFile = true;
                fis = new FileInputStream(userSetting);
            }
            else {
                fis = new FileInputStream(file.defaultLoc);
            }

            XMLEventReader reader = null;
            try {
                XMLEvent event;
                StartElement element;
                Iterator<?> attributes;
                Attribute attribute;
                reader = inputFactory.createXMLEventReader(fis);

                while (reader != null && reader.hasNext()) {
                    event = reader.nextEvent();

                    if (event.isStartElement()) {
                        element = event.asStartElement();

                        if (element.getName().getLocalPart().equals("layout")) {
                            attributes = element.getAttributes();
                            Dimension minSize = window.getMinimumSize();
                            int x = 0, y = 0, w = minSize.width, h = minSize.height;
                            boolean max = false, fs = false;
                            while (attributes.hasNext()) {
                                attribute = (Attribute) attributes.next();
                                switch (attribute.getName().toString()) {
                                    case Property.x:   x =   Integer.parseInt(attribute.getValue()); break;                                        
                                    case Property.y:   y =   Integer.parseInt(attribute.getValue()); break;
                                    case Property.w:   w =   Integer.parseInt(attribute.getValue()); break;
                                    case Property.h:   h =   Integer.parseInt(attribute.getValue()); break;
                                    case Property.max: max = attribute.getValue().equals("1"); break;
                                    case Property.fs:  fs =  attribute.getValue().equals("1"); break;
                                }
                            }

                            //ensure the window is accessible
                            int centerX = x + w / 2;
                            int centerY = y + h / 2;
                            Rectangle screenBounds = SDisplayUtil.getScreenBoundsForPoint(new Point(centerX, centerY)); 
                            if (centerX < screenBounds.x) {
                                x = screenBounds.x;
                            }
                            else if (centerX > screenBounds.x + screenBounds.width) {
                                x = screenBounds.x + screenBounds.width - w;
                                if (x < screenBounds.x) {
                                    x = screenBounds.x;
                                }
                            }
                            if (centerY < screenBounds.y) {
                                y = screenBounds.y;
                            }
                            else if (centerY > screenBounds.y + screenBounds.height) {
                                y = screenBounds.y + screenBounds.height - h;
                                if (y < screenBounds.y) {
                                    y = screenBounds.y;
                                }
                            }
                            
                            window.setWindowLayout(x, y, w, h, max, fs);
                        }
                    }
                }
            }
            catch (final Exception e) {
                try {
                    if (reader != null) { reader.close(); }
                }
                catch (final XMLStreamException x) {
                    e.printStackTrace();
                }
                e.printStackTrace();
                if (usedCustomPrefsFile) {
                    throw new InvalidLayoutFileException();
                }
                else {
                    throw new RuntimeException(e);
                }
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (fis != null ) {
                try {
                    fis.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    private final static AtomicBoolean saveRequested = new AtomicBoolean(false);
    /** Publicly-accessible save method, to neatly handle exception handling.
     * @param f0 file to save layout to, if null, saves to filePreferred
     * 
     * 
     */
    public static void saveLayout(final File f0) {
        if( saveRequested.getAndSet(true) ) return; 
        ThreadUtil.delay(100, () -> {
            save(f0);
            saveRequested.set(false);
        });
    }

    private synchronized static void save(final File f0) {
        final String fWriteTo;
        FileLocation file = Singletons.getControl().getCurrentScreen().getLayoutFile();

        if (f0 == null) {
            if (file == null) {
                return;
            }
            fWriteTo = file.userPrefLoc;
        }
        else {
            fWriteTo = f0.getPath();
        }

        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        XMLEventWriter writer = null;
        try(FileOutputStream fos = new FileOutputStream(fWriteTo);) {
            String layoutSerial = getLayoutSerial(file.defaultLoc);
            writer = out.createXMLEventWriter(fos);
            final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();

            writer.add(EF.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EF.createStartElement("", "", "layout"));
            writer.add(EF.createAttribute("serial", layoutSerial));
            writer.add(NEWLINE);

            for (final DragCell cell : cells) {
                cell.updateRoughBounds();
                RectangleOfDouble bounds = cell.getRoughBounds();
                
                writer.add(TAB);
                writer.add(EF.createStartElement("", "", "cell"));
                writer.add(EF.createAttribute(Property.x, String.valueOf(Math.rint(bounds.getX() * 100000) / 100000)));
                writer.add(EF.createAttribute(Property.y, String.valueOf(Math.rint(bounds.getY() * 100000) / 100000)));
                writer.add(EF.createAttribute(Property.w, String.valueOf(Math.rint(bounds.getW() * 100000) / 100000)));
                writer.add(EF.createAttribute(Property.h, String.valueOf(Math.rint(bounds.getH() * 100000) / 100000)));
                if (cell.getSelected() != null) {
                    writer.add(EF.createAttribute(Property.sel, cell.getSelected().getDocumentID().toString()));
                }
                writer.add(NEWLINE);

                for (final IVDoc<? extends ICDoc> vDoc : cell.getDocs()) {
                    createNode(writer, Property.doc, vDoc.getDocumentID().toString());
                }

                writer.add(TAB);
                writer.add(EF.createEndElement("", "", "cell"));
                writer.add(NEWLINE);
            }
            writer.flush(); 
            writer.add(EF.createEndDocument());
        } catch (XMLStreamException | IOException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        } finally {
            if ( writer != null )
                try { writer.close(); } catch (XMLStreamException e) {}
        }
    }

    private static String getLayoutSerial(String layoutFileName) {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        FileInputStream fis = null;
        XMLEventReader reader = null;
        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;

        try {
            fis = new FileInputStream(layoutFileName);

            reader = inputFactory.createXMLEventReader(fis);
            while (null != reader && reader.hasNext()) {
                event = reader.nextEvent();

                if (event.isStartElement()) {
                    element = event.asStartElement();

                    if (element.getName().getLocalPart().equals("layout")) {
                        attributes = element.getAttributes();
                        while (attributes.hasNext()) {
                            attribute = (Attribute) attributes.next();
                            String atrName = attribute.getName().toString();
                            if (atrName.equals("serial")) {
                                return attribute.getValue();
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) { // I don't care what happened inside, the layout is wrong
            e.printStackTrace();
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (fis != null) {
                    fis.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    public static void loadLayout(final File f) {
        final FView view = FView.SINGLETON_INSTANCE;
        String defaultLayoutSerial = "";
        String userLayoutSerial = "";
        boolean resetLayout = false;
        FScreen screen = Singletons.getControl().getCurrentScreen();
        FAbsolutePositioner.SINGLETON_INSTANCE.hideAll();
        view.getPnlInsets().removeAll();
        view.getPnlInsets().setLayout(new BorderLayout());
        view.getPnlInsets().add(view.getPnlContent(), BorderLayout.CENTER);
        view.getPnlInsets().setBorder(new EmptyBorder(SLayoutConstants.BORDER_T, SLayoutConstants.BORDER_T, 0, 0));
        view.removeAllDragCells();

        FileLocation file = screen.getLayoutFile();
        if (file != null) {
            // Read a model for new layout
            ListMultimap<LayoutInfo, EDocID> model = null;
            boolean usedCustomPrefsFile = false;
            FileInputStream fis = null;

            try {
                if (f != null && f.exists()) {
                    fis = new FileInputStream(f);
                }
                else {
                    File userSetting = new File(file.userPrefLoc);
                    if (userSetting.exists()) {
                        defaultLayoutSerial = getLayoutSerial(file.defaultLoc);
                        userLayoutSerial = getLayoutSerial(file.userPrefLoc);
                        if (defaultLayoutSerial.compareTo(userLayoutSerial) > 0) {
                            // prompt the user that their saved layout is older
                            resetLayout = SOptionPane.showConfirmDialog(
                                    String.format("Your %s layout file is from an older template.",
                                            screen.getTabCaption()
                                    ),
                                    "Reset Layout?",
                                    "Reset",
                                    "Keep");
                        }
                        if (resetLayout) {
                            // delete the old layout file
                            screen.deleteLayoutFile();
                            fis = new FileInputStream(file.defaultLoc);
                        } else {
                            fis = new FileInputStream(userSetting);
                            usedCustomPrefsFile = true;
                        }

                    }
                    else {
                        fis = new FileInputStream(file.defaultLoc);
                    }
                }

                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader xer = null;
                try {
                    xer = inputFactory.createXMLEventReader(fis);
                    model = readLayout(xer);
                } catch (final Exception e) { // I don't care what happened inside, the layout is wrong
                    try {
                        if (xer != null) { xer.close(); }
                    }
                    catch (final XMLStreamException x) {
                        x.printStackTrace();
                    }
                    e.printStackTrace();
                    if (usedCustomPrefsFile) { // the one we can safely delete
                        throw new InvalidLayoutFileException();
                    }
                    else {
                        throw new RuntimeException(e);
                    }
                }

            }
            catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                }
                catch (IOException e) {
                        e.printStackTrace();
                }
            }
    
            // Apply new layout
            for (Entry<LayoutInfo, Collection<EDocID>> kv : model.asMap().entrySet()) {
                LayoutInfo layoutInfo = kv.getKey();
                DragCell cell = new DragCell();
                cell.setRoughBounds(layoutInfo.bounds());
                FView.SINGLETON_INSTANCE.addDragCell(cell); 
                for(EDocID edoc : kv.getValue()) {
                    try {
                        //System.out.println(String.format("adding doc %s -> %s",  edoc, edoc.getDoc()));
                        if (edoc.getDoc() != null ) {
                            cell.addDoc(edoc.getDoc());
                        }
                    }
                    catch (IllegalArgumentException e) {
                        System.err.println("Failed to get doc for " + edoc); 
                    }
                }
                if (layoutInfo.selectedId() != null) {
                    cell.setSelected(layoutInfo.selectedId().getDoc());
                }
            }
        }

        // Rough bounds are all in place; resize the window.
        SResizingUtil.resizeWindow();
    }

    private static record LayoutInfo(RectangleOfDouble bounds, EDocID selectedId)
    {
    }

    private static ListMultimap<LayoutInfo, EDocID> readLayout(final XMLEventReader reader) throws XMLStreamException
    {
        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;
        EDocID selectedId = null;
        double x0 = 0, y0 = 0, w0 = 0, h0 = 0;

        ListMultimap<LayoutInfo, EDocID> model = MultimapBuilder.hashKeys().arrayListValues().build();
        
        LayoutInfo currentKey = null;
        while (null != reader && reader.hasNext()) {
            event = reader.nextEvent();

            if (event.isStartElement()) {
                element = event.asStartElement();

                if (element.getName().getLocalPart().equals("cell")) {
                    attributes = element.getAttributes();
                    while (attributes.hasNext()) {
                        attribute = (Attribute) attributes.next();
                        String atrName = attribute.getName().toString();

                        if (atrName.equals(Property.x))      x0 = Double.parseDouble(attribute.getValue());
                        else if (atrName.equals(Property.y)) y0 = Double.parseDouble(attribute.getValue());
                        else if (atrName.equals(Property.w)) w0 = Double.parseDouble(attribute.getValue());
                        else if (atrName.equals(Property.h)) h0 = Double.parseDouble(attribute.getValue());
                        else if (atrName.equals(Property.sel)) selectedId = EDocID.valueOf(attribute.getValue());
                    }
                    currentKey = new LayoutInfo(new RectangleOfDouble(x0, y0, w0, h0), selectedId);
                }
                else if (element.getName().getLocalPart().equals(Property.doc)) {
                    event = reader.nextEvent();
                    model.put(currentKey, EDocID.valueOf(event.asCharacters().getData()));
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
}
 