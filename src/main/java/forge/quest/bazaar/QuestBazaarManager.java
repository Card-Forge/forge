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
package forge.quest.bazaar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

import forge.Singletons;
import forge.quest.QuestController;
import forge.quest.data.QuestAssets;
import forge.util.IgnoringXStream;
import forge.util.XmlUtil;

/**
 * <p>
 * QuestStallManager class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarManager {
    private final File xmlFile;

    public QuestBazaarManager(File xmlFile0) {
        xmlFile = xmlFile0;
    }

    public void load() {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(xmlFile);

            XStream xs = new IgnoringXStream();
            xs.autodetectAnnotations(true);

            NodeList xmlStalls = document.getElementsByTagName("stalls").item(0).getChildNodes();
            for (int iN = 0; iN < xmlStalls.getLength(); iN++) {
                Node n = xmlStalls.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) { continue; }

                Attr att = document.createAttribute("resolves-to");
                att.setValue(QuestStallDefinition.class.getCanonicalName());
                n.getAttributes().setNamedItem(att);
                QuestStallDefinition stall = (QuestStallDefinition) xs.fromXML(XmlUtil.nodeToString(n));
                stalls.put(stall.getName(), stall);
            }

            NodeList xmlQuestItems = document.getElementsByTagName("questItems").item(0).getChildNodes();
            for (int iN = 0; iN < xmlQuestItems.getLength(); iN++) {
                Node n = xmlQuestItems.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) { continue; }

                NamedNodeMap attrs = n.getAttributes();
                String sType = attrs.getNamedItem("itemType").getTextContent();
                String name = attrs.getNamedItem("name").getTextContent();
                QuestItemType qType = QuestItemType.smartValueOf(sType);
                Attr att = document.createAttribute("resolves-to");
                att.setValue(qType.getBazaarControllerClass().getCanonicalName());
                attrs.setNamedItem(att);
                QuestItemBasic ctrl = (QuestItemBasic) xs.fromXML(XmlUtil.nodeToString(n));
                items.put(name, ctrl);
            }

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /** Constant <code>stalls</code>. */
    private final Map<String, QuestStallDefinition> stalls = new TreeMap<String, QuestStallDefinition>();
    /** Constant <code>items</code>. */
    private final Map<String, SortedSet<IQuestBazaarItem>> itemsOnStalls = new TreeMap<String, SortedSet<IQuestBazaarItem>>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, IQuestBazaarItem> items = new TreeMap<String, IQuestBazaarItem>();

    /**
     * <p>
     * getStall.
     * </p>
     * 
     * @param stallName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.quest.bazaar.QuestStallDefinition} object.
     */
    public QuestStallDefinition getStall(final String stallName) {
        if (stalls.isEmpty()) {
            load();
        }

        return stalls.get(stallName);
    }

    /**
     * Retrieves all creatures and items, iterates through them,
     * and maps to appropriate merchant.
     */
    public void buildItems(final QuestController qCtrl) {
        final Map<String, IQuestBazaarItem> itemSet = new HashMap<String, IQuestBazaarItem>();

        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {

            for (QuestPetController pet : qCtrl.getPetsStorage().getAllPets(iSlot)) {
                //System.out.println("Pet: " + pet.getName());
                itemSet.put(pet.getName(), pet);
            }
        }

        itemSet.putAll(items);

        itemsOnStalls.clear();

        for (QuestStallDefinition thisStall : stalls.values()) {
            TreeSet<IQuestBazaarItem> set = new TreeSet<IQuestBazaarItem>();

            for (String itemName : thisStall.getItems()) {
                IQuestBazaarItem item = itemSet.get(itemName);
                //System.out.println(itemName);
                set.add(item);
            }
            itemsOnStalls.put(thisStall.getName(), set);
        }
    }

    /**
     * Returns <i>purchasable</i> items available for a particular stall.
     * 
     * @param stallName &emsp; {@link java.lang.String}
     * @return {@link java.util.List}.
     */
    public List<IQuestBazaarItem> getItems(final QuestController qCtrl, final String stallName) {
        buildItems(qCtrl);

        final List<IQuestBazaarItem> ret = new ArrayList<IQuestBazaarItem>();

        QuestAssets qA = Singletons.getModel().getQuest().getAssets();
        for (final IQuestBazaarItem purchasable : itemsOnStalls.get(stallName)) {
            if (purchasable.isAvailableForPurchase(qA)) {
                ret.add(purchasable);
            }
        }
        return ret;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public Set<String> getStallNames() {
        if (stalls.isEmpty()) {
            load();
        }
        return stalls.keySet();
    }

}
