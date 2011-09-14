package forge.quest.data;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;

import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.item.Booster;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
                if (len == -1) { break; } // when end of stream was reached
                xml.append(buf, 0, len);
            }

            IgnoringXStream xStream = new IgnoringXStream();
            xStream.registerConverter(new CardPoolToXml());
            xStream.registerConverter(new GameTypeToXml());
            xStream.alias("CardPool", ItemPool.class);
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
                    // fall-through
                case 1:
                    // nothing to do here, everything is managed by CardPoolToXml deserializer
                    break;
                default:
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
    public static void saveData(final QuestData qd) {
        try {
            XStream xStream = new XStream();
            xStream.registerConverter(new CardPoolToXml());
            xStream.alias("CardPool", ItemPool.class);

            File f = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
            GZIPOutputStream zout = new GZIPOutputStream(bout);
            xStream.toXML(qd, zout);
            zout.flush();
            zout.close();

            //BufferedOutputStream boutUnp = new BufferedOutputStream(new FileOutputStream(f + ".xml"));
            //xStream.toXML(qd, boutUnp);
            //boutUnp.flush();
            //boutUnp.close();

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

    
    private static class GameTypeToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class clasz) {
            return clasz.equals(GameType.class);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            // not used
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String value = reader.getValue();
            return GameType.smartValueOf(value);
        }
        
    }
    
    private static class CardPoolToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class clasz) {
            return clasz.equals(ItemPool.class);
        }

        private void write(CardPrinted cref, Integer count, HierarchicalStreamWriter writer)
        {
            writer.startNode("card");
            writer.addAttribute("c", cref.getName());
            writer.addAttribute("s", cref.getSet());
            if (cref.isFoil()) { writer.addAttribute("foil", "1"); }
            if (cref.getArtIndex() > 0) { writer.addAttribute("i", Integer.toString(cref.getArtIndex())); }
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        private void write(Booster booster, Integer count, HierarchicalStreamWriter writer)
        {
            writer.startNode("booster");
            writer.addAttribute("s", booster.getSet());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }
        
        
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            @SuppressWarnings("unchecked")
            ItemPool<InventoryItem> pool = (ItemPool<InventoryItem>) source;
            for (Entry<InventoryItem, Integer> e : pool) {
                InventoryItem item = e.getKey(); 
                Integer count = e.getValue();
                if (item instanceof CardPrinted) {
                    write((CardPrinted) item, count, writer);
                } else if (item instanceof Booster) {
                    write((Booster) item, count, writer);
                } 
            }
            
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            ItemPool<InventoryItem> result = new ItemPool<InventoryItem>(InventoryItem.class);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                String sCnt = reader.getAttribute("n");
                int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                String nodename = reader.getNodeName();

                if("string".equals(nodename)) {
                    result.add(CardDb.instance().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // new format
                    result.add(readCardPrinted(reader), cnt);
                } else if ("booster".equals(nodename)) {
                    result.add(readBooster(reader), cnt);
                }
                reader.moveUp();
            }
            return result;
        }
        
        private Booster readBooster(final HierarchicalStreamReader reader)
        {
            String set = reader.getAttribute("s");
            return new Booster(set);
        }

        private CardPrinted readCardPrinted(final HierarchicalStreamReader reader)
        {
            String name = reader.getAttribute("c");
            String set = reader.getAttribute("s");
            String sIndex = reader.getAttribute("i");
            short index = StringUtils.isNumeric(sIndex) ? Short.parseShort(sIndex) : 0;
            boolean foil = "1".equals(reader.getAttribute("foil"));
            CardPrinted card = CardDb.instance().getCard(name, set, index);
            return foil ? CardPrinted.makeFoiled(card) : card;
        }
    }
}
