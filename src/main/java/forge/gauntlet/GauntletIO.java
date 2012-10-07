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

import forge.deck.DeckSection;
import forge.error.ErrorViewer;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.IgnoringXStream;

/** */
public class GauntletIO {
    /** Directory for storing gauntlet data files. */
    public static final String DIR_GAUNTLETS = "res/gauntlet/";
    /** Prompt in text field for new (unsaved) built gauntlets. */
    public static final String TXF_PROMPT = "[New Gauntlet]";
    /** Prefix for quick gauntlet save files. */
    public static final String PREFIX_QUICK = "Quick_";
    /** Regex for quick gauntlet save files. */
    public static final String REGEX_QUICK = "^" + GauntletIO.PREFIX_QUICK + "[0-9]+\\.dat$";
    /** Regex for locked gauntlet save files. */
    public static final String REGEX_LOCKED = "^LOCKED_.+\\.dat$";

    /**
     * Gets the serializer.
     *
     * @param isIgnoring the is ignoring
     * @return the serializer
     */
    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        xStream.registerConverter(new DeckSectionToXml());
        xStream.autodetectAnnotations(true);
        return xStream;
    }


    /** @return File[] */
    public static File[] getGauntletFilesUnlocked() {
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.matches(GauntletIO.REGEX_LOCKED);
            }
        };

        File folder = new File(GauntletIO.DIR_GAUNTLETS);
        return folder.listFiles(filter);
    }

    /** @return File[] */
    public static File[] getGauntletFilesQuick() {
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches(GauntletIO.REGEX_QUICK);
            }
        };

        File folder = new File(GauntletIO.DIR_GAUNTLETS);
        return folder.listFiles(filter);
    }

    /** @return File[] */
    public static File[] getGauntletFilesLocked() {
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches(GauntletIO.REGEX_LOCKED);
            }
        };

        File folder = new File(GauntletIO.DIR_GAUNTLETS);
        return folder.listFiles(filter);
    }

    /**
     * <p>
     * loadData.
     * </p>
     * 
     * @param xmlSaveFile
     *            &emsp; {@link java.io.File}
     * @return {@link forge.gauntlet.GauntletData}
     */
    public static GauntletData loadGauntlet(final File xmlSaveFile) {
        try {
            GauntletData data = null;

            final GZIPInputStream zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));

            final StringBuilder xml = new StringBuilder();
            final char[] buf = new char[1024];
            final InputStreamReader reader = new InputStreamReader(zin);
            while (reader.ready()) {
                final int len = reader.read(buf);
                if (len == -1) {
                    break;
                } // when end of stream was reached
                xml.append(buf, 0, len);
            }

            zin.close();
            data = (GauntletData) GauntletIO.getSerializer(true).fromXML(xml.toString());

            return data;
        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error loading Gauntlet Data");
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p>
     * saveData.
     * </p>
     * 
     * @param gd0
     *            a {@link forge.gauntlet.GauntletData} object.
     */
    public static void saveGauntlet(final GauntletData gd0) {
        try {
            final XStream xStream = GauntletIO.getSerializer(false);
            GauntletIO.savePacked(xStream, gd0);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex, "Error saving Gauntlet Data.");
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final XStream xStream0, final GauntletData gd0) throws IOException {
        final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(gd0.getActiveFile()));
        final GZIPOutputStream zout = new GZIPOutputStream(bout);
        xStream0.toXML(gd0, zout);
        zout.flush();
        zout.close();
    }

    private static class DeckSectionToXml implements Converter {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(DeckSection.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            for (final Entry<CardPrinted, Integer> e : (DeckSection) source) {
                this.writeCardPrinted(e.getKey(), e.getValue(), writer);
            }

        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final DeckSection result = new DeckSection();
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

        /** */
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
