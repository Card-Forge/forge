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
package forge.gamemodes.quest.io;

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
import java.util.HashSet;
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
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEventDraft;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.bazaar.QuestItemType;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.gamemodes.quest.data.QuestAchievements;
import forge.gamemodes.quest.data.QuestAssets;
import forge.gamemodes.quest.data.QuestData;
import forge.gamemodes.quest.data.QuestEventDraftContainer;
import forge.gamemodes.quest.data.QuestItemCondition;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gamemodes.quest.data.StarRating;
import forge.item.BoosterBox;
import forge.item.BoosterPack;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.item.SealedProduct;
import forge.item.TournamentPack;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.IgnoringXStream;
import forge.util.ItemPool;
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
    static {
        //ensure save directory exists if this class is used
        FileUtil.ensureDirectoryExists(ForgeConstants.QUEST_SAVE_DIR);
    }

    /**
     * Gets the serializer.
     *
     * @param isIgnoring the is ignoring
     * @return the serializer
     */
    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        // clear out existing permissions and set our own
        xStream.addPermission(NoTypePermission.NONE);
        // allow some basics
        xStream.addPermission(NullPermission.NULL);
        xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xStream.allowTypeHierarchy(String.class);
        xStream.allowTypeHierarchy(QuestData.class);
        xStream.allowTypeHierarchy(HashMap.class);
        xStream.allowTypeHierarchy(Deck.class);
        xStream.allowTypeHierarchy(DeckGroup.class);
        xStream.allowTypeHierarchy(EnumMap.class);
        xStream.allowTypeHierarchy(QuestItemType.class);
        // allow any type from the same package
        xStream.allowTypesByWildcard(new String[] {
                QuestDataIO.class.getPackage().getName()+".*",
                "forge.gamemodes.quest.data.*"
        });
        xStream.registerConverter(new ItemPoolToXml());
        xStream.registerConverter(new DeckToXml());
        xStream.registerConverter(new DraftTournamentToXml());
        xStream.registerConverter(new GameFormatQuestToXml());
        xStream.registerConverter(new QuestModeToXml());
        xStream.autodetectAnnotations(true);
        xStream.alias("CardPool", ItemPool.class);
        xStream.alias("DeckSection", CardPool.class);

        // alias for renamed quest data
        xStream.aliasPackage("forge.quest", "forge.gamemodes.quest");
        xStream.alias("forge.quest.data.item.QuestItemType", QuestItemType.class);

        return xStream;
    }

    /**
     * <p>
     * loadData.
     * </p>
     *
     * @param xmlSaveFile
     *            &emsp; {@link java.io.File}
     * @return {@link forge.gamemodes.quest.data.QuestData}
     */
    public static QuestData loadData(final File xmlSaveFile) throws IOException {
        QuestData data;
        final StringBuilder xml = new StringBuilder();

        try (GZIPInputStream zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));
             InputStreamReader reader = new InputStreamReader(zin)) {
            final char[] buf = new char[1024];
            while (reader.ready()) {
                final int len = reader.read(buf);
                if (len == -1) {
                    break;
                } // when end of stream was reached
                xml.append(buf, 0, len);
            }
        }

        String bigXML = xml.toString();
        try {
            data = (QuestData) QuestDataIO.getSerializer(true).fromXML(bigXML);
        } catch(Exception ex) {
            // Attempt to auto restore?
            throw new IOException(ex);
        }

        if (data.getVersionNumber() != QuestData.CURRENT_VERSION_NUMBER) {
            try {
                QuestDataIO.updateSaveFile(data, bigXML, xmlSaveFile.getName().replace(".dat", ""));
            }
            catch (final Exception e) {
                throw new IOException(e);
            }
        }

        return data;
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
     *            a {@link forge.gamemodes.quest.data.QuestData} object.
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

        if (saveVersion < 6) {
            QuestDataIO.setFinalField(QuestData.class, "petSlots", newData, new HashMap<>());
        }

        if (saveVersion < 8) {
            // Active Charm replaced by matchLength field
        	//QuestDataIO.setFinalField(QuestData.class, "isCharmActive", newData, false);
        }

        if (saveVersion < 10) {
            QuestDataIO.setFinalField(QuestData.class, "matchLength", newData, 3);
        }

        if (saveVersion < 11) {
            // clear player star ratings on cards - no card has been rated yet
            QuestDataIO.setFinalField(QuestData.class, "Ratings", newData, new HashSet<StarRating>());
            newData.Ratings.clear();
        }
        if (saveVersion < 12) {
            // Current Deck moved from preferences to quest data - it should not be global for all quests!!!
            QuestDataIO.setFinalField(QuestData.class, "currentDeck", newData, FModel.getQuestPreferences().getPref(QPref.CURRENT_DECK));
        }
        if(saveVersion < 13){
            //Update for quest DeckConstructionRules
            //Add a DeckConstructionRules set to Default.
            QuestDataIO.setFinalField(QuestData.class, "deckConstructionRules", newData, DeckConstructionRules.Default);
        }
        if (saveVersion < 14) {
            // Migrate DraftTournaments to use new Tournament class
        }

        final QuestAssets qS = newData.getAssets();
        final QuestAchievements qA = newData.getAchievements();

        switch (saveVersion) {
        // There should be a fall-through between the cases so that each
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
            //$FALL-THROUGH$
        case 1:
            // nothing to do here, everything is managed by CardPoolToXml
            // deserializer

        case 2:
            // questdata was divided into assets and achievements
            if (StringUtils.isBlank(newData.getName())) {
                QuestDataIO.setFinalField(QuestData.class, "name", newData, filename);
            }

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

            final List<Integer> completedChallenges = new ArrayList<>();
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

            //$FALL-THROUGH$
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
                        if (!x.getTagName().startsWith("forge.gamemodes.quest.data.")) {
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

            //$FALL-THROUGH$
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
            //$FALL-THROUGH$
        case 5:
        case 6:
            // have to convert completed challenges list members to strings.
            for(int i = qA.getLockedChallenges().size()-1; i >= 0; i-- ) {
                Object lc = qA.getLockedChallenges().get(i);
                if (lc != null) {
                    qA.getLockedChallenges().set(i, lc.toString());
                }
            }
            for(int i = qA.getCurrentChallenges().size()-1; i >= 0; i-- ) {
                Object lc = qA.getCurrentChallenges().get(i);
                if (lc != null) {
                    qA.getCurrentChallenges().set(i, lc.toString());
                }
            }

            //$FALL-THROUGH$
        case 7:
        case 8:
            QuestDataIO.setFinalField(QuestAssets.class, "draftDecks", qS, new HashMap<String, DeckGroup>());
            break;
        }

        // mark the QD as the latest version
        newData.setVersionNumber(QuestData.CURRENT_VERSION_NUMBER);


    }

    @SuppressWarnings("unchecked")
    private static <T> T readAsset(final XStream xs, final Document doc, final String tagName, final Class<T> clasz) {
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
     *            a {@link forge.gamemodes.quest.data.QuestData} object.
     */
    public static synchronized void saveData(final QuestData qd) {
        try {
            final XStream xStream = QuestDataIO.getSerializer(false);

            final File f = new File(ForgeConstants.QUEST_SAVE_DIR, qd.getName());
            //Copy the save file in case the save fails
            FileUtil.copyFile(f + ".dat", f + ".dat.bak");
            QuestDataIO.savePacked(f + ".dat", xStream, qd);
            //QuestDataIO.saveUnpacked(f + ".xml", xStream, qd);
        }
        catch (final Exception ex) {
            //BugReporter.reportException(ex, "Error saving Quest Data.");
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
            List<String> allowedSets = new ArrayList<>();
            List<String> bannedCards = new ArrayList<>();
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
                if (StringUtils.isNotEmpty(unlocksUsed)) {
                    setFinalField(GameFormatQuest.class, "unlocksUsed", res, Integer.parseInt(unlocksUsed));
                }
                setFinalField(GameFormatQuest.class, "allowUnlocks", res, canUnlock);
            } catch (NumberFormatException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            return res;
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

    private static class DraftTournamentToXml implements Converter {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class type) {
            return type.equals(QuestEventDraftContainer.class);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            QuestEventDraftContainer drafts = (QuestEventDraftContainer) source;

            for (QuestEventDraft draft : drafts) {
                writer.startNode("draft");

                writer.startNode("title");
                writer.setValue(draft.getTitle());
                writer.endNode();

                writer.startNode("packs");
                StringBuilder output = new StringBuilder();
                for (int i = 0; i < draft.getBoosterConfiguration().length; i++) {
                    output.append(draft.getBoosterConfiguration()[i]);
                    if (i != draft.getBoosterConfiguration().length - 1) {
                        output.append("/");
                    }
                }
                writer.setValue(output.toString());
                writer.endNode();

                writer.startNode("entryFee");
                writer.setValue(String.valueOf(draft.getEntryFee()));
                writer.endNode();

                writer.startNode("block");
                writer.setValue(draft.getBlock());
                writer.endNode();

                writer.startNode("standings");
                int i = 0;
                for (String standing : draft.getStandings()) {
                    writer.startNode("s" + i++);
                    writer.setValue(standing);
                    writer.endNode();
                }
                writer.endNode();

                // TODO Save bracket instead of standings
                //writer.startNode("bracket");
                //draft.getBracket().exportToXML(writer);
                //writer.endNode();

                writer.startNode("aiNames");
                i = 0;
                for (String name : draft.getAINames()) {
                    writer.startNode("ain" + i++);
                    writer.setValue(name);
                    writer.endNode();
                }
                writer.endNode();

                writer.startNode("aiIcons");
                i = 0;
                for (int icon : draft.getAIIcons()) {
                    writer.startNode("aii" + i++);
                    writer.setValue(icon + "");
                    writer.endNode();
                }
                writer.endNode();

                writer.startNode("started");
                writer.setValue("" + draft.isStarted());
                writer.endNode();

                writer.startNode("age");
                writer.setValue("" + draft.getAge());
                writer.endNode();

                writer.endNode();

            }

        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {

            QuestEventDraftContainer output = new QuestEventDraftContainer();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                // TODO Add Tournament
                String draftName = null;
                String boosterConfiguration = null;
                int entryFee = 1500;
                int age = 15;
                String block = null;
                String[] standings = null;
                String[] aiNames = new String[7];
                int[] aiIcons = new int[7];
                boolean started = false;

                while (reader.hasMoreChildren()) {

                    reader.moveDown();

                    switch (reader.getNodeName()) {
                        case "title":
                            draftName = reader.getValue();
                            break;
                        case "packs":
                            boosterConfiguration = reader.getValue();
                            break;
                        case "entryFee":
                            entryFee = Integer.parseInt(reader.getValue());
                            break;
                        case "block":
                            block = reader.getValue();
                            break;
                        case "standings":
                            // TODO Leaving for older quest datas, but will convert immediately to bracket
                            standings = new String[15];
                            int i = 0;
                            while (reader.hasMoreChildren()) {
                                reader.moveDown();
                                standings[i++] = reader.getValue();
                                reader.moveUp();
                            }
                            break;
                        case "bracket":
                            // TODO For newer quest datas, that store brackets
                            break;
                        case "aiNames":
                            int ii = 0;
                            while (reader.hasMoreChildren()) {
                                reader.moveDown();
                                aiNames[ii++] = reader.getValue();
                                reader.moveUp();
                            }
                            break;
                        case "aiIcons":
                            int iii = 0;
                            while (reader.hasMoreChildren()) {
                                reader.moveDown();
                                aiIcons[iii++] = Integer.parseInt(reader.getValue());
                                reader.moveUp();
                            }
                            break;
                        case "started":
                            started = Boolean.parseBoolean(reader.getValue());
                            break;
                        case "age":
                            age = Integer.parseInt(reader.getValue());
                            break;
                    }

                    reader.moveUp();

                }

                QuestEventDraft draft = new QuestEventDraft(draftName);
                draft.setBoosterConfiguration(boosterConfiguration);
                draft.setEntryFee(entryFee);
                draft.setBlock(block);
                // TODO Stop setting standings once Bracket is loading from IO
                draft.setStandings(standings);
                if (standings != null) {
                    draft.setBracket(QuestEventDraft.createBracketFromStandings(standings, aiNames, aiIcons));
                }
                draft.setAINames(aiNames);
                draft.setAIIcons(aiIcons);
                draft.setStarted(started);
                draft.setAge(age);

                output.add(draft);

                reader.moveUp();
            }

            return output;
        }
    }

    public static class DeckToXml extends ItemPoolToXml {

        /* (non-Javadoc)
         * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class type) {
            return type.equals(Deck.class);
        }

        /* (non-Javadoc)
         * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
         */
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Deck d = (Deck)source;
            writer.startNode("name");
            writer.setValue(d.getName());
            writer.endNode();

            for( Entry<DeckSection, CardPool> ds : d ) {
                writer.startNode(ds.getKey().toString());
                for (final Entry<PaperCard, Integer> e : ds.getValue()) {
                    this.write(e.getKey(), e.getValue(), writer);
                }
                writer.endNode();
            }
        }

        /* (non-Javadoc)
         * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
         */
        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            reader.moveDown(); // <name> tag MUST be at first position at all times
            String deckName = reader.getValue();
            reader.moveUp();

            final Deck result = new Deck(deckName);

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                DeckSection section = DeckSection.smartValueOf(reader.getNodeName());
                if ( null == section )
                    throw new RuntimeException("Quest deck has unknown section: " + reader.getNodeName());

                CardPool pool = result.getOrCreate(section);
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    final String sCnt = reader.getAttribute("n");
                    final int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                    final String nodename = reader.getNodeName();
                    if ("string".equals(nodename)) {
                        pool.add(FModel.getMagicDb().getCommonCards().getCard(reader.getValue()));
                    } else if ("card".equals(nodename)) { // new format
                        PaperCard pc = this.readCardPrinted(reader);
                        if (pc != null) {
                            pool.add(pc, cnt);
                        }
                    }
                    reader.moveUp();
                }
                reader.moveUp();
            }

            return result;
        }
    }

    public static class ItemPoolToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(ItemPool.class);
        }

        protected void write(final PaperCard cref, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("card");
            writer.addAttribute("c", cref.getName());
            writer.addAttribute("s", cref.getEdition());
            if (cref.isFoil()) {
                writer.addAttribute("foil", "1");
            }
            writer.addAttribute("i", Integer.toString(cref.getArtIndex()));
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final BoosterPack booster, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("booster");
            if (booster.getEdition().equals("?")) {
                writer.addAttribute("s", booster.getName().substring(0, booster.getName().indexOf(booster.getItemType()) - 1));
            } else {
                writer.addAttribute("s", booster.getEdition());
            }
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final FatPack fatpack, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("fpack");
            writer.addAttribute("s", fatpack.getEdition());
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        protected void write(final BoosterBox boosterbox, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("bbox");
            writer.addAttribute("s", boosterbox.getEdition());
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
                if (item instanceof PaperCard) {
                    this.write((PaperCard) item, count, writer);
                } else if (item instanceof BoosterPack) {
                    this.write((BoosterPack) item, count, writer);
                } else if (item instanceof TournamentPack) {
                    this.write((TournamentPack) item, count, writer);
                } else if (item instanceof FatPack) {
                    this.write((FatPack) item, count, writer);
                } else if (item instanceof BoosterBox) {
                    this.write((BoosterBox) item, count, writer);
                } else if (item instanceof PreconDeck) {
                    this.write((PreconDeck) item, count, writer);
                }
            }

        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final ItemPool<InventoryItem> result = new ItemPool<>(InventoryItem.class);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String sCnt = reader.getAttribute("n");
                final int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                final String nodename = reader.getNodeName();

                if ("string".equals(nodename)) {
                    result.add(FModel.getMagicDb().getCommonCards().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // new format
                    result.add(this.readCardPrinted(reader), cnt);
                } else if ("booster".equals(nodename)) {
                    result.add(this.readBooster(reader), cnt);
                } else if ("tpack".equals(nodename)) {
                    result.add(this.readTournamentPack(reader), cnt);
                } else if ("fpack".equals(nodename)) {
                    result.add(this.readFatPack(reader), cnt);
                } else if ("bbox".equals(nodename)) {
                    result.add(this.readBoosterBox(reader), cnt);
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
            String s = reader.getAttribute("s");
            if (SealedProduct.specialSets.contains(s) || s.equals("?")) {
                return BoosterPack.FN_FROM_COLOR.apply(s);
            } else {
                final CardEdition ed = FModel.getMagicDb().getEditions().get(s);
                return BoosterPack.FN_FROM_SET.apply(ed);
            }
        }

        protected TournamentPack readTournamentPack(final HierarchicalStreamReader reader) {
            final CardEdition ed = FModel.getMagicDb().getEditions().get(reader.getAttribute("s"));
            return TournamentPack.FN_FROM_SET.apply(ed);
        }

        protected FatPack readFatPack(final HierarchicalStreamReader reader) {
            final CardEdition ed = FModel.getMagicDb().getEditions().get(reader.getAttribute("s"));
            return FatPack.FN_FROM_SET.apply(ed);
        }

        protected BoosterBox readBoosterBox(final HierarchicalStreamReader reader) {
            final CardEdition ed = FModel.getMagicDb().getEditions().get(reader.getAttribute("s"));
            return BoosterBox.FN_FROM_SET.apply(ed);
        }

        protected PaperCard readCardPrinted(final HierarchicalStreamReader reader) {
            final String name = reader.getAttribute("c");
            final String set = reader.getAttribute("s");
            final String sIndex = reader.getAttribute("i");
            final short index = StringUtils.isNumeric(sIndex) ? Short.parseShort(sIndex) : 0;
            final boolean foil = "1".equals(reader.getAttribute("foil"));
            PaperCard card = FModel.getMagicDb().getOrLoadCommonCard(name, set, index, foil);
            if (null == card) {
                System.err.println("Warning: Unsupported card found in quest save: " + name + " from edition " + set +". It will be removed from the quest save.");
            }
            return card;
        }
    }
}
