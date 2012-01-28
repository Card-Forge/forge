package forge.deck;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.PlayerType;
import forge.game.GameType;
import forge.gui.deckeditor.TableSorter;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;
import forge.util.SectionUtil;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckIO {

    private static final String NAME = "Name";
    private static final String DECK_TYPE = "Deck Type";
    private static final String COMMENT = "Comment";
    private static final String PLAYER = "Player";
    private static final String CSTM_POOL = "Custom Pool";
    /** Constant <code>BDKFileFilter</code>. */
    static FilenameFilter bdkFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".bdk");
        }
    };
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
            return "Simple Deck File .html";
        }
    };
    /**
     * <p>
     * readDeck.
     * </p>
     * 
     * @param deckFile
     *            a {@link java.io.File} object.
     * @return a {@link forge.deck.Deck} object.
     */
    
    public static Deck readDeck(final File deckFile) {
        return readDeck(FileUtil.readFile(deckFile)); 
    }
    
    public static Deck readDeck(final List<String> deckFileLines) {
        final Map<String, List<String>> sections = SectionUtil.parseSections(deckFileLines);
        if (sections.isEmpty()) {
            return null;
        }
    
        final Deck d = new Deck();
    
        final String firstLine = deckFileLines.get(0);
        if (!firstLine.startsWith("[") || firstLine.equalsIgnoreCase("[general]")) {
            readDeckOldMetadata(deckFileLines.iterator(), d);
        } else {
            readDeckMetadata(sections.get("metadata"), d);
        }
        d.setMain(readCardList(sections.get("main")));
        d.setSideboard(readCardList(sections.get("sideboard")));
    
        return d;
    }

    private static void readDeckMetadata(final Iterable<String> lines, final Deck d) {
        if (lines == null) {
            return;
        }
        final Iterator<String> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            final String line = lineIterator.next();
    
            final String[] linedata = line.split("=", 2);
            final String field = linedata[0].toLowerCase();
            String value = "";
    
            if (linedata.length > 1) {
                value = linedata[1];
            }
    
            if (NAME.equalsIgnoreCase(field)) {
                d.setName(value);
    
            } else if (COMMENT.equalsIgnoreCase(field)) {
                d.setComment(value);
    
            } else if (DECK_TYPE.equalsIgnoreCase(field)) {
                d.setDeckType(GameType.smartValueOf(value));
    
            } else if (CSTM_POOL.equalsIgnoreCase(field)) {
                d.setCustomPool(value.equalsIgnoreCase("true"));
    
            } else if (PLAYER.equalsIgnoreCase(field)) {
                if ("human".equalsIgnoreCase(value)) {
                    d.setPlayerType(PlayerType.HUMAN);
    
                } else {
                    d.setPlayerType(PlayerType.COMPUTER);
                }
            }
        }
    }

    /**
     * <p>
     * readDeckOld.
     * </p>
     * 
     * @param iterator
     *            a {@link java.util.ListIterator} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private static void readDeckOldMetadata(final Iterator<String> iterator, final Deck d) {
    
        String line;
        // readDeck name
        final String name = iterator.next();
    
        // readDeck comments
        String comment = null;
        while (iterator.hasNext()) {
            line = iterator.next();
            if ((line != null) && !line.equals("[general")) {
                if (comment == null) {
                    comment = line;
                } else {
                    comment += "\n" + line;
                }
            }
        }
    
        // readDeck deck type
    
        final GameType deckType = iterator.hasNext() ? GameType.smartValueOf(iterator.next()) : GameType.Constructed;
    
        d.setName(name);
        d.setComment(comment);
        d.setDeckType(deckType);
    }

    // Precondition: iterator should point at the first line of cards list
    private static List<String> readCardList(final Iterable<String> lines) {
        final List<String> result = new ArrayList<String>();
        final Pattern p = Pattern.compile("((\\d+)\\s+)?(.*?)");
    
        if (lines == null) {
            return result;
        }
    
        final Iterator<String> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            final String line = lineIterator.next();
            if (line.startsWith("[")) {
                break;
            } // there comes another section
    
            final Matcher m = p.matcher(line.trim());
            m.matches();
            final String sCnt = m.group(2);
            final String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) {
                continue;
            }
    
            final int count = sCnt == null ? 1 : Integer.parseInt(sCnt);
            for (int i = 0; i < count; i++) {
                result.add(cardName);
            }
        }
        return result;
    }

    private static String deriveFileName(final String deckName) {
        // skips all but the listed characters
        return deckName.replaceAll("[^-_$#@.{[()]} a-zA-Z0-9]", "");
    }

    // only accepts numbers, letters or dashes up to 20 characters in length
    /**
     * 
     * Clean deck name.
     * 
     * @param in
     *            a String
     * @return a String
     */
    public static String cleanDeckName(final String in) {
        final char[] c = in.toCharArray();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; (i < c.length) && (i < 20); i++) {
            if (Character.isLetterOrDigit(c[i]) || (c[i] == '-')) {
                sb.append(c[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 
     * Make file name.
     * 
     * @param deckName
     *            a String
     * @param deckType
     *            a GameType
     * @return a File
     */
    public static File makeFileName(final String deckName, final GameType deckType) {
        final File path = ForgeProps.getFile(NewConstants.NEW_DECKS);
        if (deckType == GameType.Draft) {
            return new File(path, deriveFileName(deckName) + ".bdk");
        } else {
            return new File(path, deriveFileName(deckName) + ".dck");
        }
    }

    /**
     * 
     * Make file name.
     * 
     * @param deck
     *            a Deck
     * @return a File
     */
    public static File makeFileName(final Deck deck) {
        return makeFileName(deck.getName(), deck.getDeckType());
    }

    /**
     * 
     * Write draft Decks.
     * 
     * @param drafts
     *            a Deck[]
     */
    public static void writeDraftDecks(final Deck[] drafts) {
        final File f = makeFileName(drafts[0]);
        f.mkdir();
        for (int i = 0; i < drafts.length; i++) {
            FileUtil.writeFile(new File(f, i + ".dck"), serializeDeck(drafts[i]));
        }
    }

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
    private static List<String> serializeDeck(final Deck d) {
        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));
    
        out.add(String.format("%s=%s", NAME, d.getName().replaceAll("\n", "")));
        out.add(String.format("%s=%s", DECK_TYPE, d.getDeckType()));
        // these are optional
        if (d.getComment() != null) {
            out.add(String.format("%s=%s", COMMENT, d.getComment().replaceAll("\n", "")));
        }
        if (d.getPlayerType() != null) {
            out.add(String.format("%s=%s", PLAYER, d.getPlayerType()));
        }
    
        if (d.isCustomPool()) {
            out.add(String.format("%s=%s", CSTM_POOL, "true"));
        }
    
        out.add(String.format("%s", "[main]"));
        out.addAll(writeCardPool(d.getMain()));
    
        out.add(String.format("%s", "[sideboard]"));
        out.addAll(writeCardPool(d.getSideboard()));
        return out;
    }

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
            for (final Card card : d.getMain().toForgeCardList().toArray()) {
                // System.out.println(card.getSets().get(card.getSets().size() -
                // 1).URL);
                list.add(card.getSets().get(card.getSets().size() - 1).getUrl());
            }
            /*
             * List<String> nameList = new ArrayList<String>(); for (Card card :
             * d.getMain().toForgeCardList().toArray()) {
             * //System.out.println(card.getSets().get(card.getSets().size() -
             * 1).URL); nameList.add(card.getName()); }
             */
    
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
            // root.put("nameList", nameList);
            root.put("cardList", map);
    
            /* Merge data-model with template */
            // StringWriter sw = new StringWriter();
            temp.process(root, out);
            out.flush();
        } catch (final IOException e) {
            System.out.println(e.toString());
        } catch (final TemplateException e) {
            System.out.println(e.toString());
        }
    }

    private static List<String> writeCardPool(final ItemPoolView<CardPrinted> pool) {
        final List<Entry<CardPrinted, Integer>> main2sort = pool.getOrderedList();
        Collections.sort(main2sort, TableSorter.BY_NAME_THEN_SET);
        final List<String> out = new ArrayList<String>();
        for (final Entry<CardPrinted, Integer> e : main2sort) {
            final CardPrinted card = e.getKey();
            final boolean hasBadSetInfo = "???".equals(card.getSet()) || StringUtils.isBlank(card.getSet());
            if (hasBadSetInfo) {
                out.add(String.format("%d %s", e.getValue(), card.getName()));
            } else {
                out.add(String.format("%d %s|%s", e.getValue(), card.getName(), card.getSet()));
            }
        }
        return out;
    }

    /**
     * <p>
     * writeDeck.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @param f
     *            a {@link java.io.File} object.
     */
    public static void writeDeck(final Deck d, final File f) {
        FileUtil.writeFile(f, serializeDeck(d));
    }

    /**
     * <p>
     * Write deck to HTML.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @param f
     *            a {@link java.io.File} object.
     */
    public static void writeDeckHtml(final Deck d, final File f) {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writeDeckHtml(d, writer);
            writer.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * readAllDecks.
     * </p>
     * @return 
     */
    public final static Map<String, Deck> readAllDecks(File deckDir) {
        Map<String, Deck> result = new HashMap<String, Deck>();
        final List<String> decksThatFailedToLoad = new ArrayList<String>();
        File[] files = deckDir.listFiles(DeckIO.DCK_FILE_FILTER);
        for (final File file : files) {
            try {
                final Deck newDeck = DeckIO.readDeck(file);
                result.put(newDeck.getName(), newDeck);
            } catch (final NoSuchElementException ex) {
                final String message = String.format("%s failed to load because ---- %s", file.getName(),
                        ex.getMessage());
                decksThatFailedToLoad.add(message);
            }
        }
    
        if (!decksThatFailedToLoad.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    StringUtils.join(decksThatFailedToLoad, System.getProperty("line.separator")),
                    "Some of your decks were not loaded.", JOptionPane.WARNING_MESSAGE);
        }
        
        return result;
    }

    public final static Map<String, Deck[]> readAllDraftDecks(File deckDir)
    {
        Map<String, Deck[]> result = new HashMap<String, Deck[]>();
        File[] files = deckDir.listFiles(DeckIO.bdkFileFilter);
        for (final File file : files) {
            final Deck[] d = new Deck[8];
    
            boolean gotError = false;
            for (int i = 0; i < d.length; i++) {
                d[i] = DeckIO.readDeck(new File(file, i + ".dck"));
                if (d[i] == null) {
                    gotError = true;
                    break;
                }
            }
    
            if (!gotError) {
                result.put(d[0].getName(), d);
            }
        }
        return result;
    }

}
