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

import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;


public class XmlUtil {

    /**
     * Node to string.
     *
     * @param node the node
     * @return the string
     */
    public static String nodeToString(final Node node) {
        final StringWriter sw = new StringWriter();
        try {
            final Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (final TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static void read(String filename, XmlTagReader reader) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xml = factory.newPullParser();
            xml.setInput(new InputStreamReader(new FileInputStream(filename)));

            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                case XmlPullParser.START_TAG:
                    reader.tagStart(xml);
                    break;
                case XmlPullParser.END_TAG:
                    reader.tagEnd(xml);
                    break;
                }
                eventType = xml.next();
            }
        }
        catch (final FileNotFoundException e) {
            /* ignore if file doesn't exist */
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static abstract class XmlTagReader {
        public abstract void tagStart(XmlPullParser xml);
        public void tagEnd(XmlPullParser xml) {
            //not required to override but can be
        }
    }

    public static void write(String filename, XmlTagWriter writer) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filename);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
            XmlSerializer xml = factory.newSerializer();
            xml.setOutput(outputStream, null);
            xml.startDocument(null, true);
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            writer.writeTags(new XmlWriter(xml));
            xml.endDocument();
            xml.flush();
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            if (outputStream != null) {
                try {
                    outputStream.close();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            try {
                new File(filename).delete(); //delete file if exception
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public static class XmlWriter {
        private XmlSerializer xml;
        private Stack<String> tagnames;

        private XmlWriter(XmlSerializer xml0) {
            xml = xml0;
        }

        public void startTag(String tagname) {
            startTag(tagname, null);
        }
        public void startTag(String tagname, Map<String, String> attrs) {
            try {
                xml.startTag("", tagname);
                if (attrs != null) {
                    for (Entry<String, String> attr : attrs.entrySet()) {
                        xml.attribute("", attr.getKey(), attr.getValue());
                    }
                }
                tagnames.push(tagname);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void writeTag(String tagname, Map<String, String> attrs) {
            writeTag(tagname, attrs, null);
        }
        public void writeTag(String tagname, String value) {
            writeTag(tagname, null, value);
        }
        public void writeTag(String tagname, Map<String, String> attrs, String value) {
            try {
                xml.startTag("", tagname);
                if (attrs != null) {
                    for (Entry<String, String> attr : attrs.entrySet()) {
                        xml.attribute("", attr.getKey(), attr.getValue());
                    }
                }
                if (value != null) {
                    xml.text(value);
                }
                xml.endTag("", tagname);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void endTag() {
            try {
                xml.endTag("", tagnames.pop());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static abstract class XmlTagWriter {
        public abstract void writeTags(XmlWriter xml);
    }
}
