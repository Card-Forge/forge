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
package forge.gamemodes.quest.bazaar;

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
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.QuestAssets;
import forge.model.FModel;
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

    public QuestBazaarManager(final File xmlFile0) {
        xmlFile = xmlFile0;
    }

    public void load() {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(xmlFile);

            final XStream xs = new IgnoringXStream();
            // clear out existing permissions and set our own
            xs.addPermission(NoTypePermission.NONE);
            // allow some basics
            xs.addPermission(NullPermission.NULL);
            xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
            xs.allowTypeHierarchy(String.class);
            // allow any type from the same package
            xs.allowTypesByWildcard(new String[] {
                    QuestBazaarManager.class.getPackage().getName()+".*"
            });
            xs.autodetectAnnotations(true);

            final NodeList xmlStalls = document.getElementsByTagName("stalls").item(0).getChildNodes();
            for (int iN = 0; iN < xmlStalls.getLength(); iN++) {
                final Node n = xmlStalls.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) { continue; }

                final Attr att = document.createAttribute("resolves-to");
                att.setValue(QuestStallDefinition.class.getCanonicalName());
                n.getAttributes().setNamedItem(att);
                final QuestStallDefinition stall = (QuestStallDefinition) xs.fromXML(XmlUtil.nodeToString(n));
                stalls.put(stall.getName(), stall);
            }

            final NodeList xmlQuestItems = document.getElementsByTagName("questItems").item(0).getChildNodes();
            for (int iN = 0; iN < xmlQuestItems.getLength(); iN++) {
                final Node n = xmlQuestItems.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) { continue; }

                final NamedNodeMap attrs = n.getAttributes();
                final String sType = attrs.getNamedItem("itemType").getTextContent();
                final String name = attrs.getNamedItem("name").getTextContent();
                final QuestItemType qType = QuestItemType.smartValueOf(sType);
                final Attr att = document.createAttribute("resolves-to");
                att.setValue(qType.getBazaarControllerClass().getCanonicalName());
                attrs.setNamedItem(att);
                final QuestItemBasic ctrl = (QuestItemBasic) xs.fromXML(XmlUtil.nodeToString(n));
                items.put(name, ctrl);
            }

        } catch (final SAXException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /** Constant <code>stalls</code>. */
    private final Map<String, QuestStallDefinition> stalls = new TreeMap<>();
    /** Constant <code>items</code>. */
    private final Map<String, SortedSet<IQuestBazaarItem>> itemsOnStalls = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, IQuestBazaarItem> items = new TreeMap<>();

    /**
     * <p>
     * getStall.
     * </p>
     *
     * @param stallName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.gamemodes.quest.bazaar.QuestStallDefinition} object.
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
        final Map<String, IQuestBazaarItem> itemSet = new HashMap<>();

        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {

            for (final QuestPetController pet : qCtrl.getPetsStorage().getAllPets(iSlot)) {
                itemSet.put(pet.getName(), pet);
            }
        }

        itemSet.putAll(items);

        itemsOnStalls.clear();

        for (final QuestStallDefinition thisStall : stalls.values()) {
            final SortedSet<IQuestBazaarItem> set = new TreeSet<>();

            for (final String itemName : thisStall.getItems()) {
                final IQuestBazaarItem item = itemSet.get(itemName);
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

        final List<IQuestBazaarItem> ret = new ArrayList<>();

        final QuestAssets qA = FModel.getQuest().getAssets();
        for (final IQuestBazaarItem purchasable : itemsOnStalls.get(stallName)) {
            if (purchasable.isAvailableForPurchase(qA, qCtrl)) {
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
