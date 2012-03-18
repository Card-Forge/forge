package forge.quest.bazaar;

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

import forge.quest.data.QuestAssets;
import forge.util.IgnoringXStream;
import forge.util.XmlUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestPetStorage {
    
    Map<Integer, List<QuestPetController>> petsBySlot = new HashMap<Integer, List<QuestPetController>>();
    Map<String, QuestPetController> petsByName = new HashMap<String, QuestPetController>();
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param file
     */
    public QuestPetStorage(File file) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(file);

            XStream xs = new IgnoringXStream();
            xs.autodetectAnnotations(true);
            
            NodeList xmlPets = document.getElementsByTagName("pets").item(0).getChildNodes();
            for (int iN = 0; iN < xmlPets.getLength(); iN++) {
                Node n = xmlPets.item(iN);
                if (n.getNodeType() != Node.ELEMENT_NODE) { continue; }

                Attr att = document.createAttribute("resolves-to");
                att.setValue(QuestPetController.class.getCanonicalName());
                n.getAttributes().setNamedItem(att);
                String sXml = XmlUtil.nodeToString(n);
                QuestPetController petCtrl = (QuestPetController) xs.fromXML(sXml);
                addToMap(petCtrl);
            }

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param petCtrl
     */
    private void addToMap(QuestPetController petCtrl) {
        int iSlot = petCtrl.getSlot();
        List<QuestPetController> list = petsBySlot.get(Integer.valueOf(iSlot));
        if ( null == list ) {
            list = new ArrayList<QuestPetController>();
            petsBySlot.put(Integer.valueOf(iSlot), list);
        }
        petsByName.put(petCtrl.getName(), petCtrl);
        list.add(petCtrl);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param petName
     * @return
     */
    public QuestPetController getPet(String petName) {
        return petsByName.get(petName);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param i
     * @param qA
     * @return
     */
    public List<QuestPetController> getAvaliablePets(int iSlot, QuestAssets qA) {
        List<QuestPetController> result = new ArrayList<QuestPetController>();
        List<QuestPetController> allPossible = petsBySlot.get(Integer.valueOf(iSlot));
        if ( null != allPossible ) for(QuestPetController c : allPossible) if( qA.getPetLevel(c.getSaveFileKey()) > 0 ) result.add(c); 
        return result;
    }

    public List<QuestPetController> getAllPets(int iSlot) {
        List<QuestPetController> result = new ArrayList<QuestPetController>();
        List<QuestPetController> allPossible = petsBySlot.get(Integer.valueOf(iSlot));
        if ( null != allPossible ) for(QuestPetController c : allPossible) result.add(c);
        return result;
    }
    
}
