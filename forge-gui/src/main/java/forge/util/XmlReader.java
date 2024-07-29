package forge.util;

import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

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
    public PaperCard read(String key, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        return parseChildElements(key, paperCardBuilder);
    }
    public <T extends Collection<PaperCard>> void read(String key, T collectionToLoad, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        parseChildElements(key, collectionToLoad, null, paperCardBuilder);
    }
    public <T extends Collection<PaperCard>> T read(String key, Class<T> collectionType, CardDb cardDb) {
        paperCardBuilder.setup(this, cardDb);
        return parseChildElements(key, null, collectionType, paperCardBuilder);
    }
    public <T extends IXmlWritable> T read(String key, Class<T> type) {
        return parseChildElements(key, new GenericBuilder<>(type));
    }
    public <V extends IXmlWritable, T extends Collection<V>> void read(String key, T collectionToLoad, Class<V> elementType) {
        parseChildElements(key, collectionToLoad, null, new GenericBuilder<>(elementType));
    }
    public <V extends IXmlWritable, T extends Collection<V>> T read(String key, Class<T> collectionType, Class<V> elementType) {
        return parseChildElements(key, null, collectionType, new GenericBuilder<>(elementType));
    }
    public <V extends IXmlWritable> void read(final String key, final V[] array, final Class<V> elementType) {
        parseChildElements(key, new Evaluator<Void>() {
            @Override
            public Void evaluate() {
                final GenericBuilder<V> builder = new GenericBuilder<>(elementType);
                return parseChildElements(null, new Evaluator<Void>() {
                    @Override
                    public Void evaluate() {
                        try {
                            int arrayIndex = Integer.parseInt(currentElement.getTagName().substring(1)); //trim "i" prefix
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
                        return null;
                    }
                });
            }
        });
    }
    public <V extends IXmlWritable> void read(final String key, final Map<String, V> map, final Class<V> valueType) {
        parseChildElements(key, new Evaluator<Void>() {
            @Override
            public Void evaluate() {
                final GenericBuilder<V> builder = new GenericBuilder<>(valueType);
                return parseChildElements(null, new Evaluator<Void>() {
                    @Override
                    public Void evaluate() {
                        try {
                            String key = currentElement.getTagName();
                            V value = builder.evaluate();
                            if (value != null) {
                                map.put(key, value);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });
            }
        });
    }
    public <E extends Enum<E>, V extends IXmlWritable> void read(final String key, final EnumMap<E, V> enumMap, final Class<E> enumType, final Class<V> valueType) {
        parseChildElements(key, new Evaluator<Void>() {
            @Override
            public Void evaluate() {
                final GenericBuilder<V> builder = new GenericBuilder<>(valueType);
                return parseChildElements(null, new Evaluator<Void>() {
                    @Override
                    public Void evaluate() {
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
                        return null;
                    }
                });
            }
        });
    }

    /* Private helper methods */

    private <T> T parseChildElements(String findKey, Evaluator<T> handler) {
        Element parentElement = currentElement;
        NodeList childNodes = currentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element element = (Element)node;
                if (findKey == null || element.getTagName().equals(findKey)) {
                    currentElement = (Element)node;
                    T result = handler.evaluate();
                    currentElement = parentElement;
                    if (findKey != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private <V, T extends Collection<V>> T parseChildElements(String key, final T collectionToLoad, final Class<T> collectionType, final Evaluator<V> builder) {
        T result = parseChildElements(key, new Evaluator<T>() {
            @Override
            public T evaluate() {
                final T result;
                if (collectionToLoad == null) {
                    try {
                        result = collectionType.getDeclaredConstructor().newInstance();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                else {
                    result = collectionToLoad;
                }
                parseChildElements(null, new Evaluator<V>() {
                    @Override
                    public V evaluate() {
                        V value = builder.evaluate();
                        if (value != null) {
                            result.add(value);
                        }
                        return value;
                    }
                });
                return result;
            }
        });
        if (result == null) {
            result = collectionToLoad;
        }
        return result;
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
            String setCode = xml.read("set", "");
            int artIndex = xml.read("art", 0);
            PaperCard card = cardDb.getCard(name, setCode, artIndex);
            if (card == null) { //if can't find card within given set, try just using name
                card = cardDb.getCard(name);
            }
            return card;
        }
    }

    private class GenericBuilder<T extends IXmlWritable> extends Evaluator<T> {
        private final Class<T> type;

        private GenericBuilder(Class<T> type0) {
            type = type0;
        }

        @Override
        public T evaluate() {
            try {
                return type.getDeclaredConstructor(XmlReader.class).newInstance(XmlReader.this);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
