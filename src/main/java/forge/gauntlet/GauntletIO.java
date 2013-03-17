package forge.gauntlet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import forge.deck.CardPool;
import forge.error.BugReporter;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.NewConstants;
import forge.util.IgnoringXStream;

public class GauntletIO {
    /** Prompt in text field for new (unsaved) built gauntlets. */
    public static final String TXF_PROMPT = "[New Gauntlet]";
    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".dat"; 
    /** Prefix for quick gauntlet save files. */
    public static final String PREFIX_QUICK = "Quick_";
    /** Regex for locked gauntlet save files. */
    public static final String PREFIX_LOCKED = "LOCKED_";

    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        xStream.registerConverter(new DeckSectionToXml());
        xStream.autodetectAnnotations(true);
        return xStream;
    }

    public static File getGauntletFile(String name) {
        return new File(NewConstants.GAUNTLET_DIR.userPrefLoc, name + SUFFIX_DATA);
    }

    public static File getGauntletFile(GauntletData gd) {
        return getGauntletFile(gd.getName());
    }
    
    public static File[] getGauntletFilesUnlocked() {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(SUFFIX_DATA));
            }
        };

        File folder = new File(NewConstants.GAUNTLET_DIR.userPrefLoc);
        return folder.listFiles(filter);
    }

    public static File[] getGauntletFilesQuick() {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.startsWith(PREFIX_QUICK) && name.endsWith(SUFFIX_DATA));
            }
        };

        File folder = new File(NewConstants.GAUNTLET_DIR.userPrefLoc);
        return folder.listFiles(filter);
    }

    public static File[] getGauntletFilesLocked() {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.startsWith(PREFIX_LOCKED) && name.endsWith(SUFFIX_DATA));
            }
        };

        File folder = new File(NewConstants.GAUNTLET_DIR.defaultLoc);
        return folder.listFiles(filter);
    }

    public static GauntletData loadGauntlet(final File xmlSaveFile) {
        GZIPInputStream zin = null;
        try {
            zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));
            InputStreamReader reader = new InputStreamReader(zin);

            GauntletData data = (GauntletData)GauntletIO.getSerializer(true).fromXML(reader);

            String filename = xmlSaveFile.getName();
            data.setName(filename.substring(0, filename.length() - SUFFIX_DATA.length()));
            
            return data;
        } catch (final Exception ex) {
            BugReporter.reportException(ex, "Error loading Gauntlet Data");
            throw new RuntimeException(ex);
        } finally {
            if (null != zin) {
                try { zin.close(); }
                catch (IOException e) { System.out.println("error closing gauntlet data reader: " + e); }
            }
        }
    }

    public static void saveGauntlet(final GauntletData gd0) {
        try {
            final XStream xStream = GauntletIO.getSerializer(false);
            GauntletIO.savePacked(xStream, gd0);
        } catch (final Exception ex) {
            BugReporter.reportException(ex, "Error saving Gauntlet Data.");
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final XStream xStream0, final GauntletData gd0) throws IOException {
        final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(getGauntletFile(gd0)));
        final GZIPOutputStream zout = new GZIPOutputStream(bout);
        xStream0.toXML(gd0, zout);
        zout.flush();
        zout.close();
    }

    private static class DeckSectionToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(CardPool.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            for (final Entry<CardPrinted, Integer> e : (CardPool) source) {
                this.writeCardPrinted(e.getKey(), e.getValue(), writer);
            }
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final CardPool result = new CardPool();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String sCnt = reader.getAttribute("n");
                final int cnt = StringUtils.isNumeric(sCnt) ? Integer.parseInt(sCnt) : 1;
                final String nodename = reader.getNodeName();

                if ("string".equals(nodename)) {
                    result.add(CardDb.instance().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // new format
                    result.add(this.readCardPrinted(reader), cnt);
                }
                reader.moveUp();
            }
            
            return result;
        }

        private void writeCardPrinted(final CardPrinted cref, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("card");
            writer.addAttribute("c", cref.getName());
            writer.addAttribute("s", cref.getEdition());
            if (cref.isFoil()) {
                writer.addAttribute("foil", "1");
            }
            if (cref.getArtIndex() > 0) {
                writer.addAttribute("i", Integer.toString(cref.getArtIndex()));
            }
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        private CardPrinted readCardPrinted(final HierarchicalStreamReader reader) {
            final String name = reader.getAttribute("c");
            final String set = reader.getAttribute("s");
            final String sIndex = reader.getAttribute("i");
            final short index = StringUtils.isNumeric(sIndex) ? Short.parseShort(sIndex) : 0;
            final boolean foil = "1".equals(reader.getAttribute("foil"));
            final CardPrinted card = CardDb.instance().getCard(name, set, index);
            return foil ? CardPrinted.makeFoiled(card) : card;
        }
    }
}
