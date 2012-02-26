/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import forge.Singletons;
import forge.card.CardEdition;
import forge.deck.DeckSection;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.item.BoosterPack;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;

/**
 * <p>
 * QuestDataIO class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestDataIO {
    /**
     * <p>
     * Constructor for QuestDataIO.
     * </p>
     */
    public QuestDataIO() {
    }

    /**
     * <p>
     * loadData.
     * </p>
     * 
     * @param xmlSaveFile
     *            &emsp; {@link java.io.File}
     * @return {@link forge.quest.data.QuestData}
     */
    public static QuestData loadData(final File xmlSaveFile) {
        try {
            QuestData data = null;
            final String name = xmlSaveFile.getName().substring(0, xmlSaveFile.getName().length() - 4);

            if (!xmlSaveFile.exists()) {
                return new QuestData(name);
            }

            final GZIPInputStream zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));

            final StringBuilder xml = new StringBuilder();
            final char[] buf = new char[1024];
            final InputStreamReader reader = new InputStreamReader(zin);
            while (reader.ready()) {
                final int len = reader.read(buf);
                if (len == -1) {
                    break;
                } // when end of stream was reached
                xml.append(buf, 0, len);
            }

            final IgnoringXStream xStream = new IgnoringXStream();
            xStream.registerConverter(new ItemPoolToXml());
            xStream.registerConverter(new DeckSectionToXml());
            xStream.registerConverter(new GameTypeToXml());
            xStream.alias("CardPool", ItemPool.class);
            xStream.alias("DeckSection", DeckSection.class);
            data = (QuestData) xStream.fromXML(xml.toString());
            data.setName(name);

            if (data.getVersionNumber() != QuestData.CURRENT_VERSION_NUMBER) {
                QuestDataIO.updateSaveFile(data, xml.toString());
            }

            zin.close();

            return data;
        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p>
     * updateSaveFile.
     * </p>
     * 
     * @param newData
     *            a {@link forge.quest.data.QuestData} object.
     * @param input
     *            a {@link java.lang.String} object.
     */
    private static void updateSaveFile(final QuestData newData, final String input) {
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(input));
            final Document document = builder.parse(is);

            switch (newData.getVersionNumber()) {
            // There should be a fall-through b/w the cases so that each
            // version's changes get applied progressively
            case 0:
                // First beta release with new file format,
                // inventory needs to be migrated
                newData.setInventory(new QuestInventory());
                NodeList elements = document.getElementsByTagName("estatesLevel");
                newData.getInventory().setItemLevel("Estates", Integer.parseInt(elements.item(0).getTextContent()));
                elements = document.getElementsByTagName("luckyCoinLevel");
                newData.getInventory().setItemLevel("Lucky Coin", Integer.parseInt(elements.item(0).getTextContent()));
                elements = document.getElementsByTagName("sleightOfHandLevel");
                newData.getInventory().setItemLevel("Sleight", Integer.parseInt(elements.item(0).getTextContent()));
                elements = document.getElementsByTagName("gearLevel");

                final int gearLevel = Integer.parseInt(elements.item(0).getTextContent());
                if (gearLevel >= 1) {
                    newData.getInventory().setItemLevel("Map", 1);
                }
                if (gearLevel == 2) {
                    newData.getInventory().setItemLevel("Zeppelin", 1);
                }
                // fall-through
            case 1:
                // nothing to do here, everything is managed by CardPoolToXml
                // deserializer
                break;
            default:
                break;
            }

            // mark the QD as the latest version
            newData.setVersionNumber(QuestData.CURRENT_VERSION_NUMBER);

        } catch (final Exception e) {
            forge.error.ErrorViewer.showError(e);
        }
    }

    /**
     * <p>
     * saveData.
     * </p>
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public static void saveData(final QuestData qd) {
        try {
            final XStream xStream = new XStream();
            xStream.registerConverter(new ItemPoolToXml());
            xStream.registerConverter(new DeckSectionToXml());
            xStream.alias("CardPool", ItemPool.class);
            xStream.alias("DeckSection", DeckSection.class);

            final File f = new File(ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + File.separator + qd.getName()
                    + ".dat");
            final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
            final GZIPOutputStream zout = new GZIPOutputStream(bout);
            xStream.toXML(qd, zout);
            zout.flush();
            zout.close();

            // BufferedOutputStream boutUnp = new BufferedOutputStream(new
            // FileOutputStream(f + ".xml"));
            // xStream.toXML(qd, boutUnp);
            // boutUnp.flush();
            // boutUnp.close();

        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data.");
            throw new RuntimeException(ex);
        }
    }

    /**
     * Xstream subclass that ignores fields that are present in the save but not
     * in the class. This one is intended to skip fields defined in Object class
     * (but are there any fields?)
     */
    private static class IgnoringXStream extends XStream {
        private final List<String> ignoredFields = new ArrayList<String>();

        @Override
        protected MapperWrapper wrapMapper(final MapperWrapper next) {
            return new MapperWrapper(next) {
                @Override
                public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") final Class definedIn,
                        final String fieldName) {
                    if (definedIn == Object.class) {
                        IgnoringXStream.this.ignoredFields.add(fieldName);
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
        public boolean canConvert(final Class clasz) {
            return clasz.equals(GameType.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            // not used
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final String value = reader.getValue();
            return GameType.smartValueOf(value, GameType.Quest); // does not
                                                                 // matter -
                                                                 // this field
                                                                 // is
                                                                 // deprecated
                                                                 // anyway
        }

    }

    private static class ItemPoolToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(ItemPool.class);
        }

        protected void write(final CardPrinted cref, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("card");
            writer.addAttribute("c", cref.getName());
            writer.addAttribute("s", cref.getEdition());
            if (cref.isFoil()) {
                writer.addAttribute("foil", "1");
            }
            if (cref.getArtIndex() > 0) {
                writer.addAttribute("i", Integer.toString(cref.getArtIndex()));
            }
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final BoosterPack booster, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("booster");
            writer.addAttribute("s", booster.getEdition());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final FatPack fatpack, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("fpack");
            writer.addAttribute("s", fatpack.getEdition());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final TournamentPack booster, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("tpack");
            writer.addAttribute("s", booster.getEdition());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final PreconDeck deck, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("precon");
            writer.addAttribute("name", deck.getName());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            @SuppressWarnings("unchecked")
            final ItemPool<InventoryItem> pool = (ItemPool<InventoryItem>) source;
            for (final Entry<InventoryItem, Integer> e : pool) {
                final InventoryItem item = e.getKey();
                final Integer count = e.getValue();
                if (item instanceof CardPrinted) {
                    this.write((CardPrinted) item, count, writer);
                } else if (item instanceof BoosterPack) {
                    this.write((BoosterPack) item, count, writer);
                } else if (item instanceof TournamentPack) {
                    this.write((TournamentPack) item, count, writer);
                } else if (item instanceof FatPack) {
                    this.write((FatPack) item, count, writer);
                } else if (item instanceof PreconDeck) {
                    this.write((PreconDeck) item, count, writer);
                }
            }

        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final ItemPool<InventoryItem> result = new ItemPool<InventoryItem>(InventoryItem.class);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String sCnt = reader.getAttribute("n");
                final int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                final String nodename = reader.getNodeName();

                if ("string".equals(nodename)) {
                    result.add(CardDb.instance().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // new format
                    result.add(this.readCardPrinted(reader), cnt);
                } else if ("booster".equals(nodename)) {
                    result.add(this.readBooster(reader), cnt);
                } else if ("tpack".equals(nodename)) {
                    result.add(this.readTournamentPack(reader), cnt);
                } else if ("fpack".equals(nodename)) {
                    result.add(this.readFatPack(reader), cnt);
                } else if ("precon".equals(nodename)) {
                    final PreconDeck toAdd = this.readPreconDeck(reader);
                    if (null != toAdd) {
                        result.add(toAdd, cnt);
                    }
                }
                reader.moveUp();
            }
            return result;
        }

        protected PreconDeck readPreconDeck(final HierarchicalStreamReader reader) {
            String name = reader.getAttribute("name");
            if (name == null) {
                name = reader.getAttribute("s");
            }
            return QuestData.getPrecons().get(name);
        }

        protected BoosterPack readBooster(final HierarchicalStreamReader reader) {
            CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
            return BoosterPack.FN_FROM_SET.apply(ed);
        }

        protected TournamentPack readTournamentPack(final HierarchicalStreamReader reader) {
            CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
            return TournamentPack.FN_FROM_SET.apply(ed);
        }

        protected FatPack readFatPack(final HierarchicalStreamReader reader) {
            CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
            return FatPack.FN_FROM_SET.apply(ed);
        }

        protected CardPrinted readCardPrinted(final HierarchicalStreamReader reader) {
            final String name = reader.getAttribute("c");
            final String set = reader.getAttribute("s");
            final String sIndex = reader.getAttribute("i");
            final short index = StringUtils.isNumeric(sIndex) ? Short.parseShort(sIndex) : 0;
            final boolean foil = "1".equals(reader.getAttribute("foil"));
            final CardPrinted card = CardDb.instance().getCard(name, set, index);
            return foil ? CardPrinted.makeFoiled(card) : card;
        }
    }

    private static class DeckSectionToXml extends ItemPoolToXml {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(DeckSection.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            for (final Entry<CardPrinted, Integer> e : (DeckSection) source) {
                this.write(e.getKey(), e.getValue(), writer);
            }

        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final DeckSection result = new DeckSection();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String sCnt = reader.getAttribute("n");
                final int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                final String nodename = reader.getNodeName();

                if ("string".equals(nodename)) {
                    result.add(CardDb.instance().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // new format
                    result.add(this.readCardPrinted(reader), cnt);
                }
                reader.moveUp();
            }
            return result;
        }

    }
}
