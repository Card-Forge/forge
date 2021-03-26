package forge.gamemodes.quest.bazaar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import forge.gamemodes.quest.data.QuestAssets;
import forge.util.IgnoringXStream;
import forge.util.XmlUtil;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class QuestPetStorage {

    private Map<Integer, List<QuestPetController>> petsBySlot = new HashMap<>();

    private Map<String, QuestPetController> petsByName = new HashMap<>();

    /**
     * TODO: Write javadoc for Constructor.
     * 
     * @param file File
     */
    public QuestPetStorage(final File file) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(file);

            final XStream xs = new IgnoringXStream();
            // clear out existing permissions and set our own
            xs.addPermission(NoTypePermission.NONE);
            // allow some basics
            xs.addPermission(NullPermission.NULL);
            xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
            xs.allowTypeHierarchy(String.class);
            // allow any type from the same package
            xs.allowTypesByWildcard(new String[] {
                    QuestPetStorage.class.getPackage().getName()+".*"
            });

            xs.autodetectAnnotations(true);

            final NodeList xmlPets = document.getElementsByTagName("pets").item(0).getChildNodes();
            for (int iN = 0; iN < xmlPets.getLength(); iN++) {
                final Node n = xmlPets.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                final Attr att = document.createAttribute("resolves-to");
                att.setValue(QuestPetController.class.getCanonicalName());
                n.getAttributes().setNamedItem(att);
                final String sXml = XmlUtil.nodeToString(n);
                final QuestPetController petCtrl = (QuestPetController) xs.fromXML(sXml);
                this.addToMap(petCtrl);
            }

        } catch (final SAXException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param petCtrl
     */
    private void addToMap(final QuestPetController petCtrl) {
        final int iSlot = petCtrl.getSlot();
        /*
        * Refactoring this to List<QuestPetController> list = this.petsBySlot.computeIfAbsent(Integer.valueOf(iSlot), k -> new ArrayList<QuestPetController>());
        * will cause Android not to compile
        * */
        List<QuestPetController> list = this.petsBySlot.get(Integer.valueOf(iSlot));
        if (null == list) {
            list = new ArrayList<>();
            this.petsBySlot.put(Integer.valueOf(iSlot), list);
        }
        this.petsByName.put(petCtrl.getName(), petCtrl);
        list.add(petCtrl);
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param petName String
     * @return QuestPetController
     */
    public QuestPetController getPet(final String petName) {
        return this.petsByName.get(petName);
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param iSlot int
     * @param qA QuestAssets
     * @return List
     */
    public List<QuestPetController> getAvaliablePets(final int iSlot, final QuestAssets qA) {
        final List<QuestPetController> result = new ArrayList<>();
        final List<QuestPetController> allPossible = this.petsBySlot.get(Integer.valueOf(iSlot));
        if (null != allPossible) {
            for (final QuestPetController c : allPossible) {
                if (qA.getPetLevel(c.getSaveFileKey()) > 0) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * 
     * TODO: Write javadoc for this method.
     * @param iSlot int
     * @return List<QuestPetController>
     */
    public List<QuestPetController> getAllPets(final int iSlot) {
        final List<QuestPetController> result = new ArrayList<>();
        final List<QuestPetController> allPossible = this.petsBySlot.get(Integer.valueOf(iSlot));
        if (null != allPossible) {
            result.addAll(allPossible);
        }
        return result;
    }

}
