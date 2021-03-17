/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util;

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class XmlUtil {
    public static String nodeToString(final Node node) {
        final StringWriter sw = new StringWriter();
        try {
            final Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (final TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static void saveDocument(final Document document, String filename) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(filename));
        t.transform(source, result);
    }

    public static int getIntAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Integer.parseInt(value);
            }
            catch (Exception ex) {}
        }
        return 0;
    }

    public static long getLongAttribute(final Element el, final String name) {
        final String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Long.parseLong(value);
            }
            catch (final Exception ex) {}
        }
        return 0;
    }

    public static String getStringAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            return value;
        }
        return null;
    }

    public static boolean getBooleanAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Boolean.parseBoolean(value);
            }
            catch (Exception ex) {}
        }
        return false;
    }
}
