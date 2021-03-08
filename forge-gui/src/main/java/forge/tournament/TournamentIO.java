package forge.tournament;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.IgnoringXStream;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TournamentIO {
    /** Prompt in text field for new (unsaved) built gauntlets. */
    public static final String TXF_PROMPT = "[New Tournament]";
    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".dat";
    /** Prefix for quick tournament save files. */
    public static final String PREFIX_QUICK = "Quick_";
    /** Prefix for custom tournament save files. */
    public static final String PREFIX_CUSTOM = "Custom_";
    /** Regex for locked tournament save files. */
    public static final String PREFIX_LOCKED = "LOCKED_";

    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        // clear out existing permissions and set our own
        xStream.addPermission(NoTypePermission.NONE);
        // allow some basics
        xStream.addPermission(NullPermission.NULL);
        xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xStream.allowTypeHierarchy(String.class);
        // allow any type from the same package
        xStream.allowTypesByWildcard(new String[] {
                TournamentIO.class.getPackage().getName()+".*"
        });
        xStream.registerConverter(new DeckSectionToXml());
        xStream.autodetectAnnotations(true);
        return xStream;
    }

    public static File getTournamentFile(final String name) {
        return new File(ForgeConstants.TOURNAMENT_DIR.userPrefLoc, name + SUFFIX_DATA);
    }

    public static File getTournamentFile(final TournamentData gd) {
        return getTournamentFile(gd.getName());
    }

    public static File[] getTournamentFilesUnlocked(final String prefix) {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return ((prefix == null || name.startsWith(prefix)) && name.endsWith(SUFFIX_DATA));
            }
        };

        final File folder = new File(ForgeConstants.TOURNAMENT_DIR.userPrefLoc);
        return folder.listFiles(filter);
    }

    public static File[] getTournamentFilesLocked() {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return (name.startsWith(PREFIX_LOCKED) && name.endsWith(SUFFIX_DATA));
            }
        };

        final File folder = new File(ForgeConstants.TOURNAMENT_DIR.defaultLoc);
        return folder.listFiles(filter);
    }

    public static TournamentData loadTournament(final File xmlSaveFile) {
        boolean isCorrupt = false;
        try (GZIPInputStream zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));
             InputStreamReader reader = new InputStreamReader(zin)) {
            final TournamentData data = (TournamentData)TournamentIO.getSerializer(true).fromXML(reader);

            final String filename = xmlSaveFile.getName();
            data.setName(filename.substring(0, filename.length() - SUFFIX_DATA.length()));
            return data;
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final Exception e) { //if there's a non-IO exception, delete the corrupt file
            e.printStackTrace();
            isCorrupt = true;
        }

        if (isCorrupt) {
            try {
                xmlSaveFile.delete();
            } catch (final Exception e) {
                System.out.println("error delete corrupt tournament file: " + e);
            }
        }
        return null;
    }

    public static void saveTournament(final TournamentData gd0) {
        try {
            final XStream xStream = TournamentIO.getSerializer(false);
            TournamentIO.savePacked(xStream, gd0);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final XStream xStream0, final TournamentData gd0) throws IOException {
        final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(getTournamentFile(gd0)));
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
            for (final Map.Entry<PaperCard, Integer> e : (CardPool) source) {
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
                } else if ("card".equals(nodename)) { // new format
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
                throw new RuntimeException("Unsupported card found in quest save: " + name + " from edition " + set);
            }
            return card;
        }
    }
}
