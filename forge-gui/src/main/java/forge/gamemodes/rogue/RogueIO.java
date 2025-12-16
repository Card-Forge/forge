package forge.gamemodes.rogue;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.error.BugReporter;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.IgnoringXStream;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles saving and loading of Rogue Commander run data using XStream serialization.
 */
public class RogueIO {

    /** Suffix for all rogue run data files */
    public static final String SUFFIX_DATA = ".dat";
    /** Prefix for locked/default rogue runs */
    public static final String PREFIX_LOCKED = "LOCKED_";
    /** Save directory for rogue runs */
    public static final String ROGUE_SAVE_DIR =
        ForgeConstants.USER_DIR + "rogue" + ForgeConstants.PATH_SEPARATOR;

    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        // Clear out existing permissions and set our own
        xStream.addPermission(NoTypePermission.NONE);
        // Allow some basics
        xStream.addPermission(NullPermission.NULL);
        xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xStream.allowTypeHierarchy(String.class);
        xStream.allowTypeHierarchy(EnumMap.class);
        xStream.allowTypeHierarchy(ArrayList.class);
        xStream.allowTypeHierarchy(CardPool.class);
        xStream.allowTypeHierarchy(SortedSet.class);
        xStream.allowTypeHierarchy(Deck.class);
        xStream.allowTypeHierarchy(TreeMap.class);
        xStream.allowTypeHierarchy(List.class);
        xStream.allowTypeHierarchy(DeckSection.class);
        // Bypass invalid reference to allow loading earlier saves
        xStream.ignoreUnknownElements();
        xStream.setMode(XStream.NO_REFERENCES);
        xStream.omitField(Deck.class, "unplayableAI");
        // Allow any type from the same package
        xStream.allowTypesByWildcard(new String[] {
                RogueIO.class.getPackage().getName() + ".*",
                String.class.getPackage().getName() + ".*"
        });
        xStream.registerConverter(new DeckSectionToXml());
        xStream.autodetectAnnotations(true);

        return xStream;
    }

    public static File getRogueFile(final String name) {
        ensureRogueDirectoryExists();
        return new File(ROGUE_SAVE_DIR, name + SUFFIX_DATA);
    }

    public static File getRogueFile(final RogueRun rd) {
        return getRogueFile(rd.getName());
    }

    private static void ensureRogueDirectoryExists() {
        File dir = new File(ROGUE_SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static File[] getRogueFiles() {
        ensureRogueDirectoryExists();
        final FilenameFilter filter = (dir, name) -> name.endsWith(SUFFIX_DATA) && !name.startsWith(PREFIX_LOCKED);
        final File folder = new File(ROGUE_SAVE_DIR);
        return folder.listFiles(filter);
    }

    public static List<RogueRun> loadAllRuns() {
        List<RogueRun> runs = new ArrayList<>();
        File[] files = getRogueFiles();
        if (files != null) {
            for (File file : files) {
                RogueRun run = loadRun(file);
                if (run != null) {
                    runs.add(run);
                }
            }
        }
        return runs;
    }

    public static RogueRun loadRun(final File xmlSaveFile) {
        boolean isCorrupt = false;
        try (GZIPInputStream zin = new GZIPInputStream(Files.newInputStream(xmlSaveFile.toPath()));
             InputStreamReader reader = new InputStreamReader(zin)) {
            final RogueRun data = (RogueRun) RogueIO.getSerializer(true).fromXML(reader);

            final String filename = xmlSaveFile.getName();
            data.setName(filename.substring(0, filename.length() - SUFFIX_DATA.length()));
            return data;
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ConversionException e) {
            BugReporter.reportException(e);
        } catch (final Exception e) { // If there's a non-IO exception, delete the corrupt file
            e.printStackTrace();
            isCorrupt = true;
        }

        if (isCorrupt) {
            try {
                xmlSaveFile.delete();
            } catch (final Exception e) {
                System.out.println("Error deleting corrupt rogue run file: " + e);
            }
        }
        return null;
    }

    public static RogueRun loadRun(final String filename) {
        return loadRun(getRogueFile(filename));
    }

    public static void saveRun(final RogueRun rd) {
        try {
            final XStream xStream = RogueIO.getSerializer(false);
            RogueIO.savePacked(xStream, rd);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final XStream xStream0, final RogueRun rd) throws IOException {
        ensureRogueDirectoryExists();
        try (final BufferedOutputStream bout = new BufferedOutputStream(Files.newOutputStream(getRogueFile(rd).toPath()));
             final GZIPOutputStream zout = new GZIPOutputStream(bout)) {
            xStream0.toXML(rd, zout);
            zout.flush();
        }
    }

    public static void deleteRun(final String name) {
        File file = getRogueFile(name);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void deleteRun(final RogueRun rd) {
        deleteRun(rd.getName());
    }

    // Converter for CardPool serialization (same as Gauntlet)
    private static class DeckSectionToXml implements Converter {
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(final Class clasz) {
            return clasz.equals(CardPool.class);
        }

        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
            for (final Entry<PaperCard, Integer> e : (CardPool) source) {
                writeCardPrinted(e.getKey(), e.getValue(), writer);
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
                    result.add(FModel.getMagicDb().getCommonCards().getCard(reader.getValue()));
                } else if ("card".equals(nodename)) { // New format
                    result.add(readCardPrinted(reader), cnt);
                }
                reader.moveUp();
            }

            return result;
        }

        private static void writeCardPrinted(final PaperCard cref, final Integer count, final HierarchicalStreamWriter writer) {
            writer.startNode("card");
            writer.addAttribute("c", cref.getName());
            writer.addAttribute("s", cref.getEdition());
            if (cref.isFoil()) {
                writer.addAttribute("foil", "1");
            }
            writer.addAttribute("i", Integer.toString(cref.getArtIndex()));
            writer.addAttribute("n", count.toString());
            writer.endNode();
        }

        private static PaperCard readCardPrinted(final HierarchicalStreamReader reader) {
            final String name = reader.getAttribute("c");
            final String set = reader.getAttribute("s");
            final String sIndex = reader.getAttribute("i");
            final short index = StringUtils.isNumeric(sIndex) ? Short.parseShort(sIndex) : 0;
            final boolean foil = "1".equals(reader.getAttribute("foil"));
            PaperCard card = FModel.getMagicDb().getOrLoadCommonCard(name, set, index, foil);
            if (null == card) {
                // Get replacement card from any edition if it exists
                card = FModel.getMagicDb().getCommonCards().getCard(name);
                if (card == null) {
                    System.err.println("Warning: Unsupported card found in rogue save: " + name + " from edition " + set + ". It will be removed from the save.");
                }
            }
            return card;
        }
    }
}
