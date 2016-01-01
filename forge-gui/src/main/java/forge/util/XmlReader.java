package forge.util;

import java.io.File;
import java.util.Collection;
import java.util.EnumMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import forge.card.CardDb;
import forge.item.PaperCard;
import forge.util.XmlWriter.IXmlWritable;

public class XmlReader {
    private Element currentElement;

    public XmlReader(String filename0) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = builder.parse(new File(filename0));
        currentElement = (Element)document.getFirstChild();
    }

    public String read(String key, String defaultValue) {
        if (currentElement.hasAttribute(key)) {
            return currentElement.getAttribute(key);
        }
        return defaultValue;
    }
    public <E extends Enum<E>> E read(String key, E defaultValue) {
        if (currentElement.hasAttribute(key)) {
            return Enum.valueOf(defaultValue.getDeclaringClass(), currentElement.getAttribute(key));
        }
        return defaultValue;
    }
    public <E extends Enum<E>> E read(String key, Class<E> enumType) {
        if (currentElement.hasAttribute(key)) {
            return Enum.valueOf(enumType, currentElement.getAttribute(key));
        }
        return null;
    }
    public int read(String key, int defaultValue) {
        if (currentElement.hasAttribute(key)) {
            return Integer.parseInt(currentElement.getAttribute(key));
        }
        return defaultValue;
    }
    public long read(String key, long defaultValue) {
        if (currentElement.hasAttribute(key)) {
            return Long.parseLong(currentElement.getAttribute(key));
        }
        return defaultValue;
    }
    public boolean read(String key, boolean defaultValue) {
        if (currentElement.hasAttribute(key)) {
            return Boolean.parseBoolean(currentElement.getAttribute(key));
        }
        return defaultValue;
    }
    public PaperCard read(String key, PaperCard defaultValue, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        return read(key, defaultValue, paperCardBuilder);
    }
    public <T extends Collection<PaperCard>> void read(String key, T collectionToLoad, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        read(key, collectionToLoad, null, paperCardBuilder);
    }
    public <T extends Collection<PaperCard>> T read(String key, Class<T> collectionType, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        return read(key, null, collectionType, paperCardBuilder);
    }
    public <T extends IXmlWritable> T read(String key, T defaultValue, Class<T> type) {
        return read(key, defaultValue, new GenericBuilder<T>(type));
    }
    public <V extends IXmlWritable, T extends Collection<V>> void read(String key, T collectionToLoad, Class<V> elementType) {
        read(key, collectionToLoad, null, new GenericBuilder<V>(elementType));
    }
    public <V extends IXmlWritable, T extends Collection<V>> T read(String key, Class<T> collectionType, Class<V> elementType) {
        return read(key, null, collectionType, new GenericBuilder<V>(elementType));
    }
    public <V extends IXmlWritable> void read(final String key, final V[] array, final Class<V> elementType) {
        parseChildElements(key, new Runnable() {
            @Override
            public void run() {
                final GenericBuilder<V> builder = new GenericBuilder<V>(elementType);
                parseChildElements(null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Integer arrayIndex = Integer.valueOf(currentElement.getTagName());
                            if (arrayIndex >= 0 && arrayIndex < array.length) {
                                V value = builder.evaluate();
                                if (value != null) {
                                    array[arrayIndex] = value;
                                }
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
    public <E extends Enum<E>, V extends IXmlWritable> void read(final String key, final EnumMap<E, V> enumMap, final Class<E> enumType, final Class<V> valueType) {
        parseChildElements(key, new Runnable() {
            @Override
            public void run() {
                final GenericBuilder<V> builder = new GenericBuilder<V>(valueType);
                parseChildElements(null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            E mapKey = Enum.valueOf(enumType, currentElement.getTagName());
                            V value = builder.evaluate();
                            if (value != null) {
                                enumMap.put(mapKey, value);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /* Private helper methods */

    private <T> T read(String key, T defaultValue, Evaluator<T> builder) {
        NodeList childNodes = currentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element element = (Element)node;
                if (element.getTagName().equals(key)) {
                    Element parentElement = currentElement;
                    currentElement = element;
                    T result = builder.evaluate();
                    currentElement = parentElement;
                    if (result != null) {
                        return result;
                    }
                    break;
                }
            }
        }
        return defaultValue;
    }

    private <V, T extends Collection<V>> T read(String key, T defaultValue, Class<T> collectionType, Evaluator<V> builder) {
        NodeList childNodes = currentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element element = (Element)node;
                if (element.getTagName().equals(key)) {
                    Element parentElement = currentElement;
                    currentElement = element;
                    V result = builder.evaluate();
                    currentElement = parentElement;
                    if (result != null) {
                        if (defaultValue == null) {
                            try {
                                defaultValue = collectionType.newInstance();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                        defaultValue.add(result);
                    }
                }
            }
        }
        return defaultValue;
    }

    private void parseChildElements(String findKey, Runnable handler) {
        Element parentElement = currentElement;
        NodeList childNodes = currentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element element = (Element)node;
                if (findKey == null || element.getTagName().equals(findKey)) {
                    currentElement = (Element)node;
                    handler.run();
                    currentElement = parentElement;
                    if (findKey != null) { return; }
                }
            }
        }
    }

    private static final PaperCardBuilder paperCardBuilder = new PaperCardBuilder();
    private static class PaperCardBuilder extends Evaluator<PaperCard> {
        private XmlReader xml;
        private CardDb cardDb;

        private void setup(XmlReader xml0, CardDb cardDb0) {
            xml = xml0;
            cardDb = cardDb0;
        }

        @Override
        public PaperCard evaluate() {
            String name = xml.read("name", "");
            String edition = xml.read("edition", "");
            int artIndex = xml.read("artIndex", 0);
            return cardDb.getCard(name, edition, artIndex);
        }
    }

    private static class GenericBuilder<T extends IXmlWritable> extends Evaluator<T> {
        private final Class<T> type;

        private GenericBuilder(Class<T> type0) {
            type = type0;
        }

        @Override
        public T evaluate() {
            try {
                return type.getDeclaredConstructor(XmlReader.class).newInstance(this);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
