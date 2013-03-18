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
package forge.deck.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;

import forge.ImageCache;
import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.properties.NewConstants;
import forge.util.FileSection;
import forge.util.FileSectionManual;
import forge.util.FileUtil;
import forge.util.IItemSerializer;
import forge.util.storage.StorageReaderFolder;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * This class knows how to make a file out of a deck object and vice versa.
 */
public class DeckSerializer extends StorageReaderFolder<Deck> implements IItemSerializer<Deck> {
    private final boolean moveWronglyNamedDecks;
    private static final String FILE_EXTENSION = ".dck";

    public DeckSerializer(final File deckDir0) {
        this(deckDir0, false);
    }

    public DeckSerializer(final File deckDir0, boolean moveWrongDecks) {
        super(deckDir0, Deck.FN_NAME_SELECTOR);
        moveWronglyNamedDecks = moveWrongDecks;
    }

    /** Constant <code>DCKFileFilter</code>. */
    public static final FilenameFilter DCK_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".dck");
        }
    };
    /** The Constant DCK_FILTER. */
    public static final FileFilter DCK_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".dck") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Simple Deck File .dck";
        }
    };

    /** The Constant HTML_FILTER. */
    public static final FileFilter HTML_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".html") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Proxy File .html";
        }
    };

    /**
     * <p>
     * writeDeck.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @param out
     *            a {@link java.io.BufferedWriter} object.
     * @throws java.io.IOException
     *             if any.
     */
    private static void writeDeckHtml(final Deck d, final BufferedWriter out) throws IOException {
        Template temp = null;
        final int cardBorder = 0;
        final int height = 319;
        final int width = 222;

        /* Create and adjust the configuration */
        final Configuration cfg = new Configuration();
        try {
            cfg.setClassForTemplateLoading(d.getClass(), "/");
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            /*
             * ------------------------------------------------------------------
             * -
             */
            /*
             * You usually do these for many times in the application
             * life-cycle:
             */

            /* Get or create a template */
            temp = cfg.getTemplate("proxy-template.ftl");

            /* Create a data-model */
            final Map<String, Object> root = new HashMap<String, Object>();
            root.put("title", d.getName());
            final List<String> list = new ArrayList<String>();
            for (final Entry<CardPrinted, Integer> card : d.getMain()) {
                // System.out.println(card.getSets().get(card.getSets().size() - 1).URL);
                for (int i = card.getValue().intValue(); i > 0; --i ) {
                    CardPrinted r = card.getKey();
                    String url = NewConstants.URL_PIC_DOWNLOAD + ImageCache.getDownloadUrl(r, false);
                    list.add(url);
                }
            }

            final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
            for (final Entry<CardPrinted, Integer> entry : d.getMain().getOrderedList()) {
                map.put(entry.getKey().getName(), entry.getValue());
                // System.out.println(entry.getValue() + " " +
                // entry.getKey().getName());
            }

            root.put("urls", list);
            root.put("cardBorder", cardBorder);
            root.put("height", height);
            root.put("width", width);
            root.put("cardlistWidth", width - 11);
            root.put("cardList", map);

            /* Merge data-model with template */
            temp.process(root, out);
            out.flush();
        } catch (final IOException e) {
            System.out.println(e.toString());
        } catch (final TemplateException e) {
            System.out.println(e.toString());
        }
    }

    public static void writeDeck(final Deck d, final File f) {
        FileUtil.writeFile(f, d.save());
    }

    public static void writeDeckHtml(final Deck d, final File f) {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            DeckSerializer.writeDeckHtml(d, writer);
            writer.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(final Deck unit) {
        FileUtil.writeFile(this.makeFileFor(unit), unit.save());
    }

    @Override
    public void erase(final Deck unit) {
        this.makeFileFor(unit).delete();
    }

    public File makeFileFor(final Deck deck) {
        return new File(this.getDirectory(), deck.getBestFileName() + FILE_EXTENSION);
    }

    @Override
    protected Deck read(final File file) {
        final Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(file));
        Deck result = Deck.fromSections(sections, true);

        if (moveWronglyNamedDecks) {
            adjustFileLocation(file, result);
        }
        return result;
    }

    private void adjustFileLocation(final File file, final Deck result) {
        if (result == null) {
            file.delete();
        } else {
            String destFilename = result.getBestFileName() + FILE_EXTENSION;
            if (!file.getName().equals(destFilename)) {
                file.renameTo(new File(file.getParentFile().getParentFile(), destFilename));
            }
        }
    }

    @Override
    protected FilenameFilter getFileFilter() {
        return DeckSerializer.DCK_FILE_FILTER;
    }

    public static DeckFileHeader readDeckMetadata(final Map<String, List<String>> map, final boolean canThrow) {
        if (map == null) {
            return null;
        }
        final List<String> metadata = map.get("metadata");
        if (metadata != null) {
            return new DeckFileHeader(FileSection.parse(metadata, "="));
        }
        final List<String> general = map.get("general");
        if (general != null) {
            if (canThrow) {
                throw new OldDeckFileFormatException();
            }
            final FileSectionManual fs = new FileSectionManual();
            fs.put(DeckFileHeader.NAME, StringUtils.join(map.get(""), " "));
            fs.put(DeckFileHeader.DECK_TYPE, StringUtils.join(general, " "));
            return new DeckFileHeader(fs);
        }

        return null;
    }
}
