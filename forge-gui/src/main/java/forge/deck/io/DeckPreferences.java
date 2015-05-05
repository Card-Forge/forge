package forge.deck.io;

import forge.deck.DeckProxy;
import forge.properties.ForgeConstants;
import forge.util.XmlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 
 * Preferences associated with individual decks
 *
 */
public class DeckPreferences {
    private static String currentDeck = "", draftDeck = "", sealedDeck = "", commanderDeck = "", planarDeck = "", schemeDeck = "";
    private static Map<String, DeckPreferences> allPrefs = new HashMap<String, DeckPreferences>();

    public static String getCurrentDeck() {
        return currentDeck;
    }
    public static void setCurrentDeck(String currentDeck0) {
        if (currentDeck.equals(currentDeck0)) { return; }
        currentDeck = currentDeck0;
        save();
    }

    public static String getDraftDeck() {
        return draftDeck;
    }
    public static void setDraftDeck(String draftDeck0) {
        if (draftDeck.equals(draftDeck0)) { return; }
        draftDeck = draftDeck0;
        save();
    }

    public static String getSealedDeck() {
        return sealedDeck;
    }
    public static void setSealedDeck(String sealedDeck0) {
        if (sealedDeck.equals(sealedDeck0)) { return; }
        sealedDeck = sealedDeck0;
        save();
    }

    public static String getCommanderDeck() {
        return commanderDeck;
    }
    public static void setCommanderDeck(String commanderDeck0) {
        if (commanderDeck.equals(commanderDeck0)) { return; }
        commanderDeck = commanderDeck0;
        save();
    }

    public static String getPlanarDeck() {
        return planarDeck;
    }
    public static void setPlanarDeck(String planarDeck0) {
        if (planarDeck.equals(planarDeck0)) { return; }
        planarDeck = planarDeck0;
        save();
    }

    public static String getSchemeDeck() {
        return schemeDeck;
    }
    public static void setSchemeDeck(String schemeDeck0) {
        if (schemeDeck.equals(schemeDeck0)) { return; }
        schemeDeck = schemeDeck0;
        save();
    }

    public static DeckPreferences getPrefs(DeckProxy deck) {
        String key = deck.getUniqueKey();
        DeckPreferences prefs = allPrefs.get(key);
        if (prefs == null) {
            prefs = new DeckPreferences();
            allPrefs.put(key, prefs);
        }
        return prefs;
    }

    public static void load() {
        allPrefs.clear();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(new File(ForgeConstants.DECK_PREFS_FILE));

            final Element root = (Element)document.getElementsByTagName("preferences").item(0);
            currentDeck = root.getAttribute("currentDeck");
            draftDeck = root.getAttribute("draftDeck");
            sealedDeck = root.getAttribute("sealedDeck");
            commanderDeck = root.getAttribute("commanderDeck");
            planarDeck = root.getAttribute("planarDeck");
            schemeDeck = root.getAttribute("schemeDeck");

            final NodeList cards = document.getElementsByTagName("deck");
            for (int i = 0; i < cards.getLength(); i++) {
                final DeckPreferences prefs = new DeckPreferences();
                final Element el = (Element)cards.item(i);
                allPrefs.put(el.getAttribute("key"), prefs);
                prefs.starCount = Integer.parseInt(el.getAttribute("stars"));
            }
        }
        catch (FileNotFoundException e) {
            //ok if file not found
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("preferences");
            root.setAttribute("type", "decks");
            root.setAttribute("currentDeck", currentDeck);
            root.setAttribute("draftDeck", draftDeck);
            root.setAttribute("sealedDeck", sealedDeck);
            root.setAttribute("commanderDeck", commanderDeck);
            root.setAttribute("planarDeck", planarDeck);
            root.setAttribute("schemeDeck", schemeDeck);
            document.appendChild(root);

            for (Map.Entry<String, DeckPreferences> entry : allPrefs.entrySet()) {
                if (entry.getValue().starCount > 0) {
                    Element deck = document.createElement("deck");
                    deck.setAttribute("key", entry.getKey());
                    deck.setAttribute("stars", String.valueOf(entry.getValue().starCount));
                    root.appendChild(deck);
                }
            }
            XmlUtil.saveDocument(document, ForgeConstants.DECK_PREFS_FILE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int starCount;

    private DeckPreferences() {
    }

    public int getStarCount() {
        return this.starCount;
    }

    public void setStarCount(int starCount0) {
        if (this.starCount == starCount0) { return; }
        this.starCount = starCount0;
        save();
    }
}