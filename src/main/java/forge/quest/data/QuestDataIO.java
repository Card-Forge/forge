package forge.quest.data;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>QuestDataIO class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestDataIO {
    /**
     * <p>Constructor for QuestDataIO.</p>
     */
    public QuestDataIO() {
    }

    /**
     * <p>loadData.</p>
     *
     * @return a {@link forge.quest.data.QuestData} object.
     */
    public static QuestData loadData() {
        try {
            //read file "questData"
            QuestData data = null;

            File xmlSaveFile = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);

            GZIPInputStream zin =
                    new GZIPInputStream(new FileInputStream(xmlSaveFile));

            StringBuilder xml = new StringBuilder();
            char[] buf = new char[1024];
            InputStreamReader reader = new InputStreamReader(zin);
            while (reader.ready()) {
                int len = reader.read(buf);
                xml.append(buf, 0, len);
            }

            IgnoringXStream xStream = new IgnoringXStream();
            data = (QuestData) xStream.fromXML(xml.toString());

            if (data.versionNumber != QuestData.CURRENT_VERSION_NUMBER) {
                updateSaveFile(data, xml.toString());
            }

            zin.close();

            return data;
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p>updateSaveFile.</p>
     *
     * @param newData a {@link forge.quest.data.QuestData} object.
     * @param input   a {@link java.lang.String} object.
     */
    private static void updateSaveFile(
            final QuestData newData, final String input) {
        try {
            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(input));
            Document document = builder.parse(is);


            switch (newData.versionNumber) {
            //There should be a fall-through b/w the cases so that each
            // version's changes get applied progressively
                case 0:
                // First beta release with new file format,
                // inventory needs to be migrated
                    newData.inventory = new QuestInventory();
                    NodeList elements = document.getElementsByTagName("estatesLevel");
                    newData.getInventory().setItemLevel("Estates", Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("luckyCoinLevel");
                    newData.getInventory().setItemLevel("Lucky Coin", Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("sleightOfHandLevel");
                    newData.getInventory().setItemLevel("Sleight", Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("gearLevel");

                    int gearLevel = Integer.parseInt(elements.item(0).getTextContent());
                    if (gearLevel >= 1) {
                        newData.inventory.setItemLevel("Map", 1);
                    }
                    if (gearLevel == 2) {
                        newData.inventory.setItemLevel("Zeppelin", 1);
                    }
                    break;
            }

            //mark the QD as the latest version
            newData.versionNumber = QuestData.CURRENT_VERSION_NUMBER;

        } catch (Exception e) {
            forge.error.ErrorViewer.showError(e);
        }
    }

    /**
     * <p>saveData.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     */
    public static void saveData(QuestData qd) {
        try {
            File f = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
            GZIPOutputStream zout = new GZIPOutputStream(bout);

            XStream xStream = new XStream();
            xStream.toXML(qd, zout);

            zout.flush();
            zout.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data.");
            throw new RuntimeException(ex);
        }
    }

    /**
     * Xstream subclass that ignores fields that are present in the save but not in the class.
     */
    private static class IgnoringXStream extends XStream {
        List<String> ignoredFields = new ArrayList<String>();

        @Override
        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new MapperWrapper(next) {
                @Override
                public boolean shouldSerializeMember(
                        @SuppressWarnings("rawtypes") Class definedIn,
                        String fieldName) {
                    if (definedIn == Object.class) {
                        ignoredFields.add(fieldName);
                        return false;
                    }
                    return super.shouldSerializeMember(definedIn, fieldName);
                }
            };
        }
    }
}
