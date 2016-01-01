package forge.util;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import forge.item.PaperCard;

public class XmlWriter {
    private final Document document;
    private final String filename;
    private final Stack<Element> parentElements = new Stack<Element>();

    private Element currentElement;

    public XmlWriter(String filename0, String rootName0) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = builder.newDocument();
        currentElement = document.createElement(rootName0);
        document.appendChild(currentElement);
        filename = filename0;
    }

    public void startElement(String name0) {
        parentElements.add(currentElement);
        currentElement = document.createElement(name0);
    }

    public void endElement() {
        Element parentElement = parentElements.pop();
        if (parentElement == null) { return; }
        parentElement.appendChild(currentElement);
        currentElement = parentElement;
    }

    public void write(String key, String value) {
        currentElement.setAttribute(key, value);
    }
    public void write(String key, Enum<?> value) {
        write(key, value.name());
    }
    public void write(String key, int value) {
        write(key, String.valueOf(value));
    }
    public void write(String key, long value) {
        write(key, String.valueOf(value));
    }
    public void write(String key, boolean value) {
        write(key, String.valueOf(value));
    }
    public void write(String key, PaperCard value) {
        if (value == null) { return; }

        startElement(key);
        write("name", value.getName());
        write("set", value.getEdition());
        write("art", value.getArtIndex());
        endElement();
    }
    public void write(String key, HashSet<PaperCard> value) {
        startElement(key);
        for (PaperCard card : value) {
            write("card", card);
        }
        endElement();
    }
    public void write(String key, IXmlWritable value) {
        if (value == null) { return; }

        startElement(key);
        value.saveToXml(this);
        endElement();
    }
    public void write(String key, Iterable<? extends IXmlWritable> value) {
        startElement(key);
        for (IXmlWritable item : value) {
            write("item", item);
        }
        endElement();
    }
    public void write(String key, IXmlWritable[] value) {
        startElement(key);
        for (int i = 0; i < value.length; i++) {
            write(String.valueOf(i), value[i]);
        }
        endElement();
    }
    public void write(String key, EnumMap<? extends Enum<?>, ? extends IXmlWritable> value) {
        startElement(key);
        for (Entry<? extends Enum<?>, ? extends IXmlWritable> entry : value.entrySet()) {
            write(entry.getKey().name(), entry.getValue());
        }
        endElement();
    }

    public void close() throws Exception {
        XmlUtil.saveDocument(document, filename);
    }

    public interface IXmlWritable {
        void saveToXml(XmlWriter xml);
    }
}
