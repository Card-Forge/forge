package forge.gui.card;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import forge.card.CardDb;
import forge.util.TextUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import forge.item.IPaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.XmlUtil;

/** 
 * Preferences associated with individual cards
 *
 */
public class CardPreferences {
    private static Map<String, CardPreferences> allPrefs = new HashMap<>();

    public static CardPreferences getPrefs(IPaperCard card) {
        String cardName = card.getName();
        CardPreferences prefs = allPrefs.get(cardName);
        if (prefs == null) {
            prefs = new CardPreferences(cardName);
            allPrefs.put(cardName, prefs);
        }
        return prefs;
    }

    public static void load() {
        allPrefs.clear();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(new File(ForgeConstants.CARD_PREFS_FILE));
            final NodeList cards = document.getElementsByTagName("card");
            for (int i = 0; i < cards.getLength(); i++) {
                final Element el = (Element)cards.item(i);
                final CardPreferences prefs = new CardPreferences(el.getAttribute("name"));
                allPrefs.put(prefs.cardName, prefs);
                prefs.setStarCount(XmlUtil.getIntAttribute(el, "stars"));
                prefs.setPreferredArt(XmlUtil.getStringAttribute(el, "art"));
            }
        }
        catch (FileNotFoundException e) {
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

    private final String cardName;
    private int starCount;
    private String preferredArt;

    private CardPreferences(String cardName0) {
        cardName = cardName0;
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
        if (preferredArt0 == null) {
            preferredArt = null;
            return;
        }
        if (preferredArt0.equals(preferredArt)) { return; }

        try {
            String infoCardName;
            String infoSetCode;
            int infoArtIndex;
            String[] prefArtInfos =TextUtil.split(preferredArt0, CardDb.NameSetSeparator);
            if (prefArtInfos.length == 2){
                // legacy format
                infoCardName = this.cardName;
                infoSetCode = prefArtInfos[0];
                infoArtIndex = Integer.parseInt(prefArtInfos[1]);
            } else {
                infoCardName = prefArtInfos[0];
                infoSetCode = prefArtInfos[1];
                infoArtIndex = Integer.parseInt(prefArtInfos[2]);
            }
            if (!this.cardName.equals(infoCardName))  // extra sanity check
                return;
            if (FModel.getMagicDb().getCommonCards().setPreferredArt(cardName, infoSetCode, infoArtIndex))
                preferredArt = preferredArt0;
        } catch (NumberFormatException ex){
            System.err.println("ERROR with Existing Preferred Card Entry: " + preferredArt0);
        }
    }

    public void setPreferredArt(String setCode, int artIndex) {
        this.setPreferredArt(setCode + CardDb.NameSetSeparator + artIndex);
    }
}