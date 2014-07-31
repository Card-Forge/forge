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
                prefs.preferredArt = el.getAttribute("art");
                if (prefs.preferredArt.length() == 0) {
                    prefs.preferredArt = null; //don't store empty string
                }
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
                CardPreferences prefs = entry.getValue();
                if (prefs.starCount > 0 || prefs.preferredArt != null) {
                    Element card = document.createElement("card");
                    card.setAttribute("name", entry.getKey());
                    if (prefs.starCount > 0) {
                        card.setAttribute("stars", String.valueOf(prefs.starCount));
                    }
                    if (prefs.preferredArt != null) {
                        card.setAttribute("art", prefs.preferredArt);
                    }
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
    private String preferredArt;

    private CardPreferences() {
    }

    public int getStarCount() {
        return starCount;
    }

    public void setStarCount(int starCount0) {
        starCount = starCount0;
    }

    public String getPreferredArt() {
        return preferredArt;
    }

    public void setPreferredArt(String preferredArt0) {
        preferredArt = preferredArt0;
    }
}