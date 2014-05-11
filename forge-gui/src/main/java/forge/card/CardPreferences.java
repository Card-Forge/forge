package forge.card;

import forge.item.IPaperCard;
import forge.properties.ForgeConstants;
import forge.util.XmlUtil;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 
 * Preferences associated with individual cards
 *
 */
public class CardPreferences {
    private static Map<String, CardPreferences> allPrefs = new HashMap<String, CardPreferences>();

    public static CardPreferences getPrefs(IPaperCard card) {
        String key = card.getName(); //TODO: Consider include art/set in key
        CardPreferences prefs = allPrefs.get(key);
        if (prefs == null) {
            prefs = new CardPreferences();
            allPrefs.put(key, prefs);
        }
        return prefs;
    }

    public static void load() {
        allPrefs.clear();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(ForgeConstants.CARD_PREFS_FILE);
            final NodeList cards = document.getElementsByTagName("card");
            for (int i = 0; i < cards.getLength(); i++) {
                final CardPreferences prefs = new CardPreferences();
                final Element el = (Element)cards.item(i);
                allPrefs.put(el.getAttribute("name"), prefs);
                prefs.starCount = Integer.parseInt(el.getAttribute("stars"));
            }
        }
        catch (FileNotFoundException e) {
            //ok if file not found
        }
        catch (MalformedURLException e) {
            //ok if file not found
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("preferences");
            root.setAttribute("type", "cards");
            document.appendChild(root);

            for (Map.Entry<String, CardPreferences> entry : allPrefs.entrySet()) {
                if (entry.getValue().starCount > 0) {
                    Element card = document.createElement("card");
                    card.setAttribute("name", entry.getKey());
                    card.setAttribute("stars", String.valueOf(entry.getValue().starCount));
                    root.appendChild(card);
                }
            }
            XmlUtil.saveDocument(document, ForgeConstants.CARD_PREFS_FILE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int starCount;

    private CardPreferences() {
    }

    public int getStarCount() {
        return this.starCount;
    }

    public void setStarCount(int starCount0) {
        this.starCount = starCount0;
    }
}