package forge.deck.io;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import forge.deck.DeckProxy;
import forge.properties.ForgeConstants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 
 * Preferences associated with individual decks
 *
 */
public class DeckPreferences {
    private static String currentDeck;
    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");
    private static Map<String, DeckPreferences> allPrefs = new HashMap<String, DeckPreferences>();

    public static String getCurrentDeck() {
        return currentDeck;
    }

    public static void setCurrentDeck(String currentDeck0) {
        if (currentDeck != null && currentDeck.equals(currentDeck0)) { return; }
        currentDeck = currentDeck0;
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
            final XMLInputFactory in = XMLInputFactory.newInstance();
            final XMLEventReader reader = in.createXMLEventReader(new FileInputStream(ForgeConstants.DECK_PREFS_FILE));

            XMLEvent event;
            StartElement element;
            Iterator<?> attributes;
            Attribute attribute;
            String tagname;
            DeckPreferences prefs;

            while (reader.hasNext()) {
                event = reader.nextEvent();

                if (event.isStartElement()) {
                    element = event.asStartElement();
                    tagname = element.getName().getLocalPart();

                    if (tagname.equals("deck")) {
                        prefs = new DeckPreferences();
                        attributes = element.getAttributes();

                        while (attributes.hasNext()) {
                            attribute = (Attribute) attributes.next();
                            switch (attribute.getName().toString()) {
                            case "key":
                                allPrefs.put(attribute.getValue(), prefs);
                                break;
                            case "stars":
                                prefs.starCount = Integer.parseInt(attribute.getValue());
                                break;
                            }
                        }
                    }
                    else if (tagname.equals("preferences")) {
                        attributes = element.getAttributes();
                        while (attributes.hasNext()) {
                            attribute = (Attribute) attributes.next();
                            switch (attribute.getName().toString()) {
                            case "currentDeck":
                                currentDeck = attribute.getValue();
                            }
                        }
                    }
                }
            }
        }
        catch (final FileNotFoundException e) {
            /* ignore; it's ok if this file doesn't exist */
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            final XMLOutputFactory out = XMLOutputFactory.newInstance();
            final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(ForgeConstants.DECK_PREFS_FILE));

            writer.add(EVENT_FACTORY.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
            writer.add(EVENT_FACTORY.createAttribute("type", "decks"));
            writer.add(EVENT_FACTORY.createAttribute("currentDeck", currentDeck));
            writer.add(NEWLINE);

            for (Map.Entry<String, DeckPreferences> entry : allPrefs.entrySet()) {
                if (entry.getValue().starCount > 0) {
                    writer.add(TAB);
                    writer.add(EVENT_FACTORY.createStartElement("", "", "deck"));
                    writer.add(EVENT_FACTORY.createAttribute("key", entry.getKey()));
                    writer.add(EVENT_FACTORY.createAttribute("stars", String.valueOf(entry.getValue().starCount)));
                    writer.add(EVENT_FACTORY.createEndElement("", "", "deck"));
                    writer.add(NEWLINE);
                }
            }

            writer.add(EVENT_FACTORY.createEndDocument());
            writer.flush();
            writer.close();
        }
        catch (final Exception e) {
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