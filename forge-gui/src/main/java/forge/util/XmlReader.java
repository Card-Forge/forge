package forge.util;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    public <E extends Enum<E>> E read(String key, E defaultValue, Class<E> enumType) {
        if (currentElement.hasAttribute(key)) {
            return Enum.valueOf(enumType, currentElement.getAttribute(key));
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
    public PaperCard read(String key, PaperCard defaultValue) {
        //TODO
        return defaultValue;
    }
    public <T extends IXmlWritable> T read(String key, T defaultValue, Class<T> type) {
        readChildElement(key);
        /*NodeList elements = currentElement.getElementsByTagName(key);
        if (elements.getLength() > 0) {
            try {
                return type.getDeclaredConstructor(XmlReader.class).newInstance(this);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        return defaultValue;
    }

    private void readChildElement(String key) {
        
    }
}
