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
package forge.quest.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import forge.Singletons;
import forge.card.CardEdition;
import forge.deck.DeckSection;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.quest.data.GameFormatQuest;
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
import forge.quest.QuestController;
import forge.quest.QuestMode;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.QuestAchievements;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestData;
import forge.quest.data.QuestItemCondition;
import forge.util.IgnoringXStream;
import forge.util.XmlUtil;

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
     * Gets the serializer.
     *
     * @param isIgnoring the is ignoring
     * @return the serializer
     */
    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        xStream.registerConverter(new ItemPoolToXml());
        xStream.registerConverter(new DeckSectionToXml());
        xStream.registerConverter(new GameTypeToXml());
        xStream.registerConverter(new GameFormatQuestToXml());
        xStream.registerConverter(new QuestModeToXml());
        xStream.autodetectAnnotations(true);
        xStream.alias("CardPool", ItemPool.class);
        xStream.alias("DeckSection", DeckSection.class);
        return xStream;
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

            zin.close();

            data = (QuestData) QuestDataIO.getSerializer(true).fromXML(xml.toString());

            if (data.getVersionNumber() != QuestData.CURRENT_VERSION_NUMBER) {
                try {
                    QuestDataIO.updateSaveFile(data, xml.toString(), xmlSaveFile.getName().replace(".dat", ""));
                } catch (final Exception e) {
                    forge.error.ErrorViewer.showError(e);
                }
            }

            return data;
        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }

    private static <T> void setFinalField(final Class<T> clasz, final String fieldName, final T instance,
            final Object newValue) throws IllegalAccessException, NoSuchFieldException {
        final Field field = clasz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, newValue); // no difference here (used only to set
                                       // initial lives)
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
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static void updateSaveFile(final QuestData newData, final String input, String filename) throws ParserConfigurationException, SAXException, IOException, IllegalAccessException, NoSuchFieldException {

        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(input));
        final Document document = builder.parse(is);

        final int saveVersion = newData.getVersionNumber();

        if (saveVersion < 3) {
            QuestDataIO.setFinalField(QuestData.class, "assets", newData, new QuestAssets(null));

            final int diffIdx = Integer.parseInt(document.getElementsByTagName("diffIndex").item(0).getTextContent());
            QuestDataIO.setFinalField(QuestData.class, "achievements", newData, new QuestAchievements(diffIdx));
        }

        if (saveVersion < 4) {
            QuestDataIO.setFinalField(QuestAssets.class, "inventoryItems", newData.getAssets(), new EnumMap<QuestItemType, Integer>(QuestItemType.class));
        }

        if (saveVersion < 5) {
            QuestDataIO.setFinalField(QuestAssets.class, "combatPets", newData.getAssets(), new HashMap<String, QuestItemCondition>());
        }

        final QuestAssets qS = newData.getAssets();

        switch (saveVersion) {
        // There should be a fall-through b/w the cases so that each
        // version's changes get applied progressively
        case 0:
            // First beta release with new file format,
            // inventory needs to be migrated
            QuestDataIO.setFinalField(QuestAssets.class, "inventoryItems", newData.getAssets(), new EnumMap<QuestItemType, Integer>(QuestItemType.class));
            qS.setItemLevel(QuestItemType.ESTATES, Integer.parseInt(document.getElementsByTagName("estatesLevel").item(0).getTextContent()));
            qS.setItemLevel(QuestItemType.LUCKY_COIN, Integer.parseInt(document.getElementsByTagName("luckyCoinLevel").item(0).getTextContent()));
            qS.setItemLevel(QuestItemType.SLEIGHT, Integer.parseInt(document.getElementsByTagName("sleightOfHandLevel").item(0).getTextContent()));

            final int gearLevel = Integer.parseInt(document.getElementsByTagName("gearLevel").item(0).getTextContent());
            if (gearLevel >= 1) {
                newData.getAssets().setItemLevel(QuestItemType.MAP, 1);
            }
            if (gearLevel == 2) {
                newData.getAssets().setItemLevel(QuestItemType.ZEPPELIN, 1);
            }
            // fall-through
        case 1:
            // nothing to do here, everything is managed by CardPoolToXml
            // deserializer

        case 2:
            // questdata was divided into assets and achievements
            if (StringUtils.isBlank(newData.getName())) {
                QuestDataIO.setFinalField(QuestData.class, "name", newData, filename);
            }

            final QuestAchievements qA = newData.getAchievements();
            QuestDataIO.setFinalField(QuestAchievements.class, "win", qA, Integer.parseInt(document.getElementsByTagName("win").item(0).getTextContent()));
            QuestDataIO.setFinalField(QuestAchievements.class, "lost", qA, Integer.parseInt(document.getElementsByTagName("lost").item(0).getTextContent()));

            Node nw;
            if ((nw = document.getElementsByTagName("winstreakBest").item(0)) != null) {
                QuestDataIO.setFinalField(QuestAchievements.class, "winstreakBest", qA, Integer.parseInt(nw.getTextContent()));
            }
            if ((nw = document.getElementsByTagName("winstreakCurrent").item(0)) != null) {
                QuestDataIO.setFinalField(QuestAchievements.class, "winstreakCurrent", qA, Integer.parseInt(nw.getTextContent()));
            }

            QuestDataIO.setFinalField(QuestAchievements.class, "challengesPlayed", qA, Integer.parseInt(document.getElementsByTagName("challengesPlayed").item(0).getTextContent()));

            final ArrayList<Integer> completedChallenges = new ArrayList<Integer>();
            QuestDataIO.setFinalField(QuestAchievements.class, "completedChallenges", qA, completedChallenges);

            if ((nw = document.getElementsByTagName("completedChallenges").item(0)) != null) {
                final NodeList ccs = nw.getChildNodes();
                for (int iN = 0; iN < ccs.getLength(); iN++) {
                    final Node n0 = ccs.item(iN);
                    if (n0.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    completedChallenges.add(Integer.parseInt(n0.getTextContent()));
                }
            }

            final XStream xs = QuestDataIO.getSerializer(true);

            QuestDataIO.setFinalField(QuestAssets.class, "credits", qS, Integer.parseInt(document.getElementsByTagName("credits").item(0).getTextContent()));
            QuestDataIO.setFinalField(QuestAssets.class, "cardPool", qS, QuestDataIO.readAsset(xs, document, "cardPool", ItemPool.class));
            QuestDataIO.setFinalField(QuestAssets.class, "myDecks", qS, QuestDataIO.readAsset(xs, document, "myDecks", HashMap.class));
            QuestDataIO.setFinalField(QuestAssets.class, "shopList", qS, QuestDataIO.readAsset(xs, document, "shopList", ItemPool.class));
            QuestDataIO.setFinalField(QuestAssets.class, "newCardList", qS, QuestDataIO.readAsset(xs, document, "newCardList", ItemPool.class));

        case 3:
            // QuestInventory class no longer exists - KV pairs of
            // QuestItemPair => level moved to assets
            final Node oldInventory = saveVersion > 0 ? document.getElementsByTagName("inventory").item(1) : null;
            if (null != oldInventory) {
                for (int iN = 0; iN < oldInventory.getChildNodes().getLength(); iN++) {
                    final Node _n = oldInventory.getChildNodes().item(iN);
                    if (_n.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    final Element n = (Element) _n;
                    final String name = n.getElementsByTagName("string").item(0).getTextContent();
                    final QuestItemType qType = QuestItemType.valueFromSaveKey(name);
                    int level = 0;
                    for (int iX = 0; iX < n.getChildNodes().getLength(); iX++) {
                        final Node _x = n.getChildNodes().item(iX);
                        if (_x.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        final Element x = (Element) _x;
                        if (!x.getTagName().startsWith("forge.quest.data.")) {
                            continue;
                        }
                        final String sLevel = x.getElementsByTagName("level").item(0).getTextContent();
                        if (StringUtils.isNotBlank(sLevel)) {
                            level = Integer.parseInt(sLevel);
                        }
                    }
                    qS.setItemLevel(qType, level);
                }
            }

        case 4:
            if (saveVersion > 0) {
                NodeList pets = document.getElementsByTagName("pets").item(0).getChildNodes();
                for (int i = 0; i < pets.getLength(); i++) {
                    if (pets.item(i).getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    final Element entry = (Element) pets.item(i);
                    String name = entry.getElementsByTagName("string").item(0).getTextContent();
                    String sLevel = entry.getElementsByTagName("level").item(0).getTextContent();
                    qS.setPetLevel(name, Integer.parseInt(sLevel));
                }

            }

            // pet manager will be re-engineered here

        default:
            break;
        }

        // mark the QD as the latest version
        newData.setVersionNumber(QuestData.CURRENT_VERSION_NUMBER);


    }

    @SuppressWarnings("unchecked")
    private static <T> T readAsset(final XStream xs, final Document doc, final String tagName, final Class<T> clasz)
            throws IllegalAccessException, NoSuchFieldException {
        final NodeList nn = doc.getElementsByTagName(tagName);
        final Node n = nn.item(0);

        final Attr att = doc.createAttribute("resolves-to");
        att.setValue(clasz.getCanonicalName());
        n.getAttributes().setNamedItem(att);

        final String xmlData = XmlUtil.nodeToString(n);
        return (T) xs.fromXML(xmlData);
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
            final XStream xStream = QuestDataIO.getSerializer(false);

            final File f = new File(ForgeProps.getFile(NewConstants.Quest.DATA_DIR), qd.getName());
            QuestDataIO.savePacked(f + ".dat", xStream, qd);
            // QuestDataIO.saveUnpacked(f + ".xml", xStream, qd);

        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data.");
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final String f, final XStream xStream, final QuestData qd) throws IOException {
        final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
        final GZIPOutputStream zout = new GZIPOutputStream(bout);
        xStream.toXML(qd, zout);
        zout.flush();
        zout.close();
    }

    @SuppressWarnings("unused") // used only for debug purposes
    private static void saveUnpacked(final String f, final XStream xStream, final QuestData qd) throws IOException {
        final BufferedOutputStream boutUnp = new BufferedOutputStream(new FileOutputStream(f));
        xStream.toXML(qd, boutUnp);
        boutUnp.flush();
        boutUnp.close();
    }

    private static class GameFormatQuestToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(GameFormatQuest.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {

            writer.startNode("format");
            GameFormatQuest format = (GameFormatQuest) source;
            writer.addAttribute("name", format.getName());
            writer.addAttribute("unlocksUsed", Integer.toString(format.getUnlocksUsed()));
            writer.addAttribute("canUnlock", format.canUnlockSets() ? "1" : "0");
            writer.endNode();

            for (String set : format.getAllowedSetCodes()) {
                writer.startNode("set");
                writer.addAttribute("s", set);
                writer.endNode();
            }
            for (String ban : format.getBannedCardNames()) {
                writer.startNode("ban");
                writer.addAttribute("s", ban);
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            reader.moveDown();
            String name = reader.getAttribute("name");
            String unlocksUsed = reader.getAttribute("unlocksUsed");
            boolean canUnlock = !("0".equals(reader.getAttribute("canUnlock")));
            List<String> allowedSets = new ArrayList<String>();
            List<String> bannedCards = new ArrayList<String>();
            reader.moveUp();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String nodename = reader.getNodeName();
                if (nodename.equals("ban")) {
                    bannedCards.add(reader.getAttribute("s"));
                    // System.out.println("Added + " + toBan + " to banned cards");
                }
                else if (nodename.equals("set")) {
                    allowedSets.add(reader.getAttribute("s"));
                    // System.out.println("Added + " + toSets + " to legal sets");
                }
                reader.moveUp();
            }
            GameFormatQuest res = new GameFormatQuest(name, allowedSets, bannedCards);
            try {
                if ( StringUtils.isNotEmpty(unlocksUsed)) {
                    setFinalField(GameFormatQuest.class, "unlocksUsed", res, Integer.parseInt(unlocksUsed));
                }
                setFinalField(GameFormatQuest.class, "allowUnlocks", res, canUnlock);                
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            
            return res;
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
            return GameType.smartValueOf(value, GameType.Quest); 
        }
    }

    private static class QuestModeToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(QuestMode.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            QuestMode mode = (QuestMode) source;
            String sMode = mode.toString();
            writer.setValue(sMode);
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final String value = reader.getValue();
            return QuestMode.smartValueOf(value, QuestMode.Classic);
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
            return QuestController.getPrecons().get(name);
        }

        protected BoosterPack readBooster(final HierarchicalStreamReader reader) {
            final CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
            return BoosterPack.FN_FROM_SET.apply(ed);
        }

        protected TournamentPack readTournamentPack(final HierarchicalStreamReader reader) {
            final CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
            return TournamentPack.FN_FROM_SET.apply(ed);
        }

        protected FatPack readFatPack(final HierarchicalStreamReader reader) {
            final CardEdition ed = Singletons.getModel().getEditions().get(reader.getAttribute("s"));
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
