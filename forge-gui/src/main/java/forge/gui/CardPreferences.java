package forge.gui;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 
 * Preferences associated with individual cards
 *
 */
public class CardPreferences {
    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");
    private static Map<String, CardPreferences> allPrefs = new HashMap<String, CardPreferences>();

    public static CardPreferences getPrefs(String name) {
        CardPreferences prefs = allPrefs.get(name);
        if (prefs == null) {
            prefs = new CardPreferences();
            allPrefs.put(name, prefs);
        }
        return prefs;
    }

    public static void load(String filename) {
        allPrefs.clear();

        try {
            final XMLInputFactory in = XMLInputFactory.newInstance();
            final XMLEventReader reader = in.createXMLEventReader(new FileInputStream(filename));

            XMLEvent event;
            StartElement element;
            Iterator<?> attributes;
            Attribute attribute;
            String tagname;
            CardPreferences prefs;

            while (reader.hasNext()) {
                event = reader.nextEvent();

                if (event.isStartElement()) {
                    element = event.asStartElement();
                    tagname = element.getName().getLocalPart();

                    if (tagname.equals("card")) {
                        prefs = new CardPreferences();
                        attributes = element.getAttributes();

                        while (attributes.hasNext()) {
                            attribute = (Attribute) attributes.next();
                            switch (attribute.getName().toString()) {
                            case "name":
                                allPrefs.put(attribute.getValue(), prefs);
                                break;
                            case "stars":
                                prefs.starCount = Integer.parseInt(attribute.getValue());
                                break;
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

    public static void save(String filename) {
        try {
            final XMLOutputFactory out = XMLOutputFactory.newInstance();
            final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(filename));
    
            writer.add(EVENT_FACTORY.createStartDocument());
            writer.add(NEWLINE);
            writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
            writer.add(EVENT_FACTORY.createAttribute("type", "cards"));
            writer.add(NEWLINE);
    
            for (Map.Entry<String, CardPreferences> entry : allPrefs.entrySet()) {
                if (entry.getValue().starCount > 0) {
                    writer.add(TAB);
                    writer.add(EVENT_FACTORY.createStartElement("", "", "card"));
                    writer.add(EVENT_FACTORY.createAttribute("name", entry.getKey()));
                    writer.add(EVENT_FACTORY.createAttribute("stars", String.valueOf(entry.getValue().starCount)));
                    writer.add(EVENT_FACTORY.createEndElement("", "", "card"));
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

    private CardPreferences() {
    }

    public int getStarCount() {
        return this.starCount;
    }

    public void setStarCount(int starCount0) {
        this.starCount = starCount0;
    }
}