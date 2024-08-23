package forge.gamemodes.gauntlet;

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

public class GauntletIO {
    /** Prompt in text field for new (unsaved) built gauntlets. */
    public static final String TXF_PROMPT = "[New Gauntlet]";
    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".dat";
    /** Prefix for quick gauntlet save files. */
    public static final String PREFIX_QUICK = "Quick_";
    /** Prefix for custom gauntlet save files. */
    public static final String PREFIX_CUSTOM = "Custom_";
    /** Prefix for commander gauntlet save files. */
    public static final String PREFIX_COMMANDER = "Commander_";
    /** Regex for locked gauntlet save files. */
    public static final String PREFIX_LOCKED = "LOCKED_";

    protected static XStream getSerializer(final boolean isIgnoring) {
        final XStream xStream = isIgnoring ? new IgnoringXStream() : new XStream();
        // clear out existing permissions and set our own
        xStream.addPermission(NoTypePermission.NONE);
        // allow some basics
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
        //bypass invalid reference to allow loading earlier saves of xstream
        xStream.ignoreUnknownElements();
        xStream.setMode(XStream.NO_REFERENCES);
        xStream.omitField(Deck.class, "unplayableAI");
        // allow any type from the same package
        xStream.allowTypesByWildcard(new String[] {
                GauntletIO.class.getPackage().getName()+".*",
                String.class.getPackage().getName()+".*"
        });
        xStream.registerConverter(new DeckSectionToXml());
        xStream.autodetectAnnotations(true);

        // Alias for renamed
        xStream.aliasPackage("forge.gauntlet", GauntletIO.class.getPackage().getName());
        return xStream;
    }

    public static File getGauntletFile(final String name) {
        return new File(ForgeConstants.GAUNTLET_DIR.userPrefLoc, name + SUFFIX_DATA);
    }

    public static File getGauntletFile(final GauntletData gd) {
        return getGauntletFile(gd.getName());
    }

    public static File[] getGauntletFilesUnlocked(final String prefix) {
        final FilenameFilter filter = (dir, name) -> ((prefix == null || name.startsWith(prefix)) && name.endsWith(SUFFIX_DATA));

        final File folder = new File(ForgeConstants.GAUNTLET_DIR.userPrefLoc);
        return folder.listFiles(filter);
    }

    public static File[] getGauntletFilesLocked() {
        final FilenameFilter filter = (dir, name) -> (name.startsWith(PREFIX_LOCKED) && name.endsWith(SUFFIX_DATA));

        final File folder = new File(ForgeConstants.GAUNTLET_DIR.defaultLoc);
        return folder.listFiles(filter);
    }

    public static GauntletData loadGauntlet(final File xmlSaveFile) {
        boolean isCorrupt = false;
        try (GZIPInputStream zin = new GZIPInputStream(Files.newInputStream(xmlSaveFile.toPath()));
             InputStreamReader reader = new InputStreamReader(zin)) {
            final GauntletData data = (GauntletData)GauntletIO.getSerializer(true).fromXML(reader);

            final String filename = xmlSaveFile.getName();
            data.setName(filename.substring(0, filename.length() - SUFFIX_DATA.length()));
            return data;
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ConversionException e) {
            BugReporter.reportException(e);
        } catch (final Exception e) { //if there's a non-IO exception, delete the corrupt file
            e.printStackTrace();
            isCorrupt = true;
        }

        if (isCorrupt) {
            try {
                xmlSaveFile.delete();
            } catch (final Exception e) {
                System.out.println("error delete corrupt gauntlet file: " + e);
            }
        }
        return null;
    }

    public static void saveGauntlet(final GauntletData gd0) {
        try {
            final XStream xStream = GauntletIO.getSerializer(false);
            GauntletIO.savePacked(xStream, gd0);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void savePacked(final XStream xStream0, final GauntletData gd0) throws IOException {
        try(final BufferedOutputStream bout = new BufferedOutputStream(Files.newOutputStream(getGauntletFile(gd0).toPath()));
            final GZIPOutputStream zout = new GZIPOutputStream(bout)) {
            xStream0.toXML(gd0, zout);
            zout.flush();
        }
    }

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
                //get same replacement card from any edition due to Edition updates/changes if it exists (ie  Ugin, the Ineffable from edition PSLD, Mana Crypt from edition FS, Timeless Lotus from edition ???).
                card = FModel.getMagicDb().getCommonCards().getCard(name);
                if (card == null) //if the card is not found, notify via log instead of crashing
                    System.err.println("Warning: Unsupported card found in gauntlet save: " + name + " from edition " + set +". It will be removed from the gauntlet save.");
                //throw new RuntimeException("Unsupported card found in gauntlet save: " + name + " from edition " + set);
            }
            return card;
        }
    }
}
