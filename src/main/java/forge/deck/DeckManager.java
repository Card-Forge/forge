package forge.deck;


import forge.Card;
import forge.Constant;
import forge.card.CardPrinted;
import forge.error.ErrorViewer;
import forge.quest.data.QuestUtil;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;


//reads and writeDeck Deck objects
/**
 * <p>DeckManager class.</p>
 *
 * @author Forge
 * @version $Id: DeckManager.java 10146 2011-09-01 18:11:00Z Max mtg $
 */
public class DeckManager {
    /** Constant <code>BDKFileFilter</code> */
    private static FilenameFilter BDKFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".bdk");
        }
    };

    /** Constant <code>DCKFileFilter</code> */
    private static FilenameFilter DCKFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".dck");
        }
    };


    private File deckDir;
    Map<String, Deck> deckMap;
    Map<String, Deck[]> draftMap;

    /**
     * <p>Constructor for DeckManager.</p>
     *
     * @param deckDir a {@link java.io.File} object.
     */
    public DeckManager(File deckDir) {
        if (deckDir == null) {
            throw new IllegalArgumentException("No deck directory specified");
        }
        try {
            this.deckDir = deckDir;

            if (deckDir.isFile()) {
                throw new IOException("Not a directory");
            } else {
                deckDir.mkdirs();
                if (!deckDir.isDirectory()) {
                    throw new IOException("Directory can't be created");
                }
                this.deckMap = new HashMap<String, Deck>();
                this.draftMap = new HashMap<String, Deck[]>();
                readAllDecks();
            }
        } catch (IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckManager : writeDeck() error, " + ex.getMessage());
        }
    }


    /**
     * <p>isUnique.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isUnique(String deckName) {
        return !deckMap.containsKey(deckName);
    }

    /**
     * <p>isUniqueDraft.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isUniqueDraft(String deckName) {
        return !draftMap.keySet().contains(deckName);
    }

    /**
     * <p>getDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck getDeck(String deckName) {
        return deckMap.get(deckName);
    }


    /**
     * <p>addDeck.</p>
     *
     * @param deck a {@link forge.deck.Deck} object.
     */
    public void addDeck(Deck deck) {
        if (deck.getDeckType().equals(Constant.GameType.Draft)) {
            throw new RuntimeException(
                    "DeckManager : addDeck() error, deck type is Draft");
        }

        deckMap.put(deck.getName(), deck);
    }

    /**
     * <p>deleteDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    public void deleteDeck(String deckName) {
        deckMap.remove(deckName);
    }

    /**
     * <p>getDraftDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDraftDeck(String deckName) {
        if (!draftMap.containsKey(deckName)) {
            throw new RuntimeException(
                    "DeckManager : getDraftDeck() error, deck name not found - " + deckName);
        }

        return draftMap.get(deckName);
    }

    /**
     * <p>addDraftDeck.</p>
     *
     * @param deck an array of {@link forge.deck.Deck} objects.
     */
    public void addDraftDeck(Deck[] deck) {
        checkDraftDeck(deck);

        draftMap.put(deck[0].toString(), deck);
    }

    /**
     * <p>deleteDraftDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    public void deleteDraftDeck(String deckName) {
        if (!draftMap.containsKey(deckName)) {
            throw new RuntimeException(
                    "DeckManager : deleteDraftDeck() error, deck name not found - " + deckName);
        }

        draftMap.remove(deckName);
    }

    /**
     * <p>checkDraftDeck.</p>
     *
     * @param deck an array of {@link forge.deck.Deck} objects.
     */
    private void checkDraftDeck(Deck[] deck) {
        if (deck == null || deck.length != 8 || deck[0].getName().equals("")
                || (!deck[0].getDeckType().equals(Constant.GameType.Draft))) {
            throw new RuntimeException("DeckManager : checkDraftDeck() error, invalid deck");
        }
    }


    /**
     * <p>getDecks.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<Deck> getDecks() {
        return deckMap.values();
    }

    /**
     * <p>getDraftDecks.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, Deck[]> getDraftDecks() {
        return new HashMap<String, Deck[]>(draftMap);
    }

    /**
     * <p>close.</p>
     */
    public void close() {
        writeAllDecks();
    }


    /**
     * <p>readAllDecks.</p>
     */
    public void readAllDecks() {
        deckMap.clear();
        draftMap.clear();

        File[] files;

        files = deckDir.listFiles(DCKFileFilter);
        for (File file : files) {
            Deck newDeck = readDeck(file);
            deckMap.put(newDeck.getName(), newDeck);
        }

        files = deckDir.listFiles(BDKFileFilter);
        for (File file : files) {
            Deck[] d = new Deck[8];

            for (int i = 0; i < d.length; i++) {
                d[i] = readDeck(new File(file, i + ".dck"));
            }

            draftMap.put(d[0].getName(), d);
        }
    }

    /**
     * <p>readDeck.</p>
     *
     * @param deckFile a {@link java.io.File} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public static Deck readDeck(File deckFile) {

        List<String> lines = new LinkedList<String>();

        try {
            BufferedReader r = new BufferedReader(new FileReader(deckFile));

            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }

            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ListIterator<String> lineIterator = lines.listIterator();

        String line = lineIterator.next();

        //Old text-based format
        if (!line.equals("[metadata]")) {
            lineIterator.previous();
            return readDeckOld(lineIterator);
        }

        Deck d = new Deck();

        //read metadata
        while (!(line = lineIterator.next()).equals("[main]")) {
            String[] linedata = line.split("=", 2);
            d.addMetaData(linedata[0], linedata[1]);
        }

        addCardList(lineIterator, d);

        return d;

    }

    /**
     * <p>readDeckOld.</p>
     *
     * @param iterator a {@link java.util.ListIterator} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private static Deck readDeckOld(ListIterator<String> iterator) {

        String line;
        //readDeck name
        String name = iterator.next();

        //readDeck comments
        String comment = null;
        while ((line = iterator.next()) != null && !line.equals("[general]")) {
            if (comment == null) {
                comment = line;
            } else {
                comment += "\n" + line;
            }
        }

        //readDeck deck type
        String deckType = iterator.next();

        Deck d = new Deck();
        d.setName(name);
        d.setComment(comment);
        d.setDeckType(deckType);

        //go to [main]
        while ((line = iterator.next()) != null && !line.equals("[main]")) {
            System.err.println("unexpected line: " + line);
        }

        addCardList(iterator, d);

        return d;
    }

    /**
     * <p>addCardList.</p>
     *
     * @param lineIterator a {@link java.util.ListIterator} object.
     * @param d a {@link forge.deck.Deck} object.
     */
    private static void addCardList(ListIterator<String> lineIterator, Deck d) {
        String line;

        Pattern p = Pattern.compile("\\s*((\\d+)\\s+)?(.*?)\\s*");

        //readDeck main deck
        while (lineIterator.hasNext() && !(line = lineIterator.next()).equals("[sideboard]")) {
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            int count = s == null ? 1 : parseInt(s);

            for (int i = 0; i < count; i++) {
                d.addMain(m.group(3));
            }
        }

        //readDeck sideboard
        while (lineIterator.hasNext() && !(line = lineIterator.next()).equals("[human_extra_cards]")) {
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) { continue; }

            int count = s == null ? 1 : parseInt(s);
            for (int i = 0; i < count; i++) {
                d.addSideboard(cardName);
            }
        }
        
        // readDeck human extras
        while (lineIterator.hasNext() && !(line = lineIterator.next()).equals("[ai_extra_cards]")) {
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) { continue; }
            
            int count = s == null ? 1 : parseInt(s);
            for (int i = 0; i < count; i++) {
                if(cardName.substring(0,5).equals("TOKEN")) {
                    System.out.println("DeckManager: Token ignored ("+cardName+")");
                    // Build token below, but of type Card, not CardPrinted, 
                    // so can't be added to deck.
                    //Card c = QuestUtil.extraCardBuilder(cardName);                    
                }
                else {
                    d.addHumanExtraCards(cardName);
                }
            }
        }
        
        // readDeck AI extras
        while (lineIterator.hasNext()) {
            line = lineIterator.next();
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) { continue; }

            int count = s == null ? 1 : parseInt(s);
            for (int i = 0; i < count; i++) {
                if(cardName.substring(0,5).equals("TOKEN")) {
                    System.out.println("DeckManager: Token ignored ("+cardName+")");
                    // Build token below, but of type Card, not CardPrinted, 
                    // so can't be added to deck.
                    //Card c = QuestUtil.extraCardBuilder(cardName);                    
                }
                else {
                    d.addAIExtraCards(cardName);
                }
            }
        }
    }

    /**
     * <p>deriveFileName.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    private String deriveFileName(String deckName) {
        //skips all but the listed characters
        return deckName.replaceAll("[^-_$#@.{[()]} a-zA-Z0-9]", "");
    }

    /**
     * <p>writeAllDecks.</p>
     */
    public void writeAllDecks() {
        try {
            //store the files that do exist
            List<File> files = new ArrayList<File>();
            files.addAll(asList(deckDir.listFiles(DCKFileFilter)));

            //save the files and remove them from the list
            for (Deck deck : deckMap.values()) {
                File f = new File(deckDir, deriveFileName(deck.getName()) + ".dck");
                files.remove(f);
                BufferedWriter out = new BufferedWriter(new FileWriter(f));
                writeDeck(deck, out);
                out.close();
            }
            //delete the files that were not written out: the decks that were deleted
            for (File file : files) {
                file.delete();
            }

            //store the files that do exist
            files.clear();
            files.addAll(asList(deckDir.listFiles(BDKFileFilter)));

            //save the files and remove them from the list
            for (Entry<String, Deck[]> e : draftMap.entrySet()) {
                File f = new File(deckDir, deriveFileName(e.getValue()[0].getName()) + ".bdk");
                f.mkdir();
                for (int i = 0; i < e.getValue().length; i++) {
                    BufferedWriter out = new BufferedWriter(new FileWriter(new File(f, i + ".dck")));
                    writeDeck(e.getValue()[i], out);
                    out.close();
                }
            }
            /*
            //delete the files that were not written out: the decks that were deleted
            for(File file:files) {
                for(int i = 0; i < 8; i++)
                    new File(file, i + ".dck").delete();
                file.delete();
            }
            */
        } catch (IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckManager : writeDeck() error, " + ex.getMessage());
        }
    }

    /**
     * <p>writeDeck.</p>
     *
     * @param d a {@link forge.deck.Deck} object.
     * @param out a {@link java.io.BufferedWriter} object.
     * @throws java.io.IOException if any.
     */
    private static void writeDeck(Deck d, BufferedWriter out) throws IOException {
        out.write("[metadata]\n");

        for (Entry<String, String> entry : d.getMetadata()) {
            if (entry.getValue() != null)
                out.write(format("%s=%s%n", entry.getKey(), entry.getValue().replaceAll("\n", "")));
        }

        out.write(format("%s%n", "[main]"));
        for (Entry<CardPrinted, Integer> e : d.getMain()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey().getName()));
        }
        out.write(format("%s%n", "[sideboard]"));
        for (Entry<CardPrinted, Integer> e : d.getSideboard()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey().getName()));
        }
    }

    /**
     * <p>count.</p>
     *
     * @param src a {@link java.util.List} object.
     * @return a {@link java.util.Map} object.
     */
    private static Map<String, Integer> count(List<String> src) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (String s : src) {
            Integer dstValue = result.get(s);
            if (dstValue == null) {
                result.put(s, 1);
            } else {
                result.put(s, dstValue + 1);
            }
        }
        return result;
    }

    /**
     * <p>writeDeck.</p>
     *
     * @param d a {@link forge.deck.Deck} object.
     * @param f a {@link java.io.File} object.
     */
    public static void writeDeck(Deck d, File f) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writeDeck(d, writer);

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
