package forge.deck;


import forge.Constant;
import forge.error.ErrorViewer;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;


//reads and write Deck objects
public class DeckManager {
    private static FilenameFilter BDKFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".bdk");
        }
    };

    private static FilenameFilter DCKFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".dck");
        }
    };


    private File deckDir;
    Map<String, Deck> deckMap;
    Map<String, Deck[]> boosterMap;

    public DeckManager(String fileName) {
        this(new File(fileName));
    }

    public DeckManager(File deckDir) {
        if (deckDir == null) {
            throw new IllegalArgumentException("No deck directory specified");
        }
        try {
            this.deckDir = deckDir;

            if (deckDir.isFile()) {
                throw new IOException("Not a directory");
            }
            else {
                deckDir.mkdirs();
                if (!deckDir.isDirectory()) {
                    throw new IOException("Directory can't be created");
                }
                this.deckMap = new HashMap<String, Deck>();
                this.boosterMap = new HashMap<String, Deck[]>();
                readDir();
            }
        } catch (IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckManager : write() error, " + ex.getMessage());
        }
    }


    public boolean isUnique(String deckName) {
        return !deckMap.containsKey(deckName);
    }

    public boolean isUniqueDraft(String deckName) {
        return !boosterMap.keySet().contains(deckName);
    }

    public void addDeck(Deck deck) {
        if (deck.getDeckType().equals(Constant.GameType.Draft)) {
            throw new RuntimeException(
                    "DeckManager : addDeck() error, deck type is Draft");
        }

        deckMap.put(deck.getName(), deck);
    }

    public void deleteDeck(String deckName) {
        deckMap.remove(deckName);
    }

    public Deck[] readBoosterDeck(String deckName) {
        if (!boosterMap.containsKey(deckName)) {
            throw new RuntimeException(
                    "DeckManager : readBoosterDeck() error, deck name not found - " + deckName);
        }

        return boosterMap.get(deckName);
    }

    public void addBoosterDeck(Deck[] deck) {
        checkBoosterDeck(deck);

        boosterMap.put(deck[0].toString(), deck);
    }

    public void deleteBoosterDeck(String deckName) {
        if (!boosterMap.containsKey(deckName)) {
            throw new RuntimeException(
                    "DeckManager : deleteBoosterDeck() error, deck name not found - " + deckName);
        }

        boosterMap.remove(deckName);
    }

    private void checkBoosterDeck(Deck[] deck) {
        if (deck == null || deck.length != 8 || deck[0].getName().equals("")
                || (!deck[0].getDeckType().equals(Constant.GameType.Draft))) {
            throw new RuntimeException("DeckManager : checkBoosterDeck() error, invalid deck");
        }
    }


    public Collection<Deck> getDecks() {
        return deckMap.values();
    }

    public Map<String, Deck[]> getBoosterDecks() {
        return new HashMap<String, Deck[]>(boosterMap);
    }

    public void close() {
        writeFile();
    }


    public void readDir() {
        deckMap.clear();
        boosterMap.clear();

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

            boosterMap.put(d[0].getName(), d);
        }
    }

    private Deck readDeck(File deckFile) {

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

        String firstLine = lineIterator.next();

        //Old text-based format
        if (!firstLine.equals("[Metadata]")) {
            lineIterator.previous();
            return readDeckOld(lineIterator);
        }

        return null;

    }


    private static Deck readDeckOld(ListIterator<String> iterator) {

        String line;
        //readDeck name
        String name = iterator.next();

        //readDeck comments
        String comment = null;
        while ((line = iterator.next()) != null && !line.equals("[general]")) {
            if (comment == null) {
                comment = line;
            }
            else {
                comment += "\n" + line;
            }
        }

        //readDeck deck type
        String deckType = iterator.next();

        Deck d = new Deck(deckType);
        d.setName(name);
        d.setComment(comment);

        //go to [main]
        while ((line = iterator.next()) != null && !line.equals("[main]")) {
            System.err.println("unexpected line: " + line);
        }

        Pattern p = Pattern.compile("\\s*((\\d+)\\s+)?(.*?)\\s*");

        //readDeck main deck
        while (iterator.hasNext() && !(line = iterator.next()).equals("[sideboard]")) {
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            int count = s == null ? 1 : parseInt(s);

            for (int i = 0; i < count; i++) {
                d.addMain(m.group(3));
            }
        }

        //readDeck sideboard
        while (iterator.hasNext()) {
            Matcher m = p.matcher(line);
            m.matches();
            String s = m.group(2);
            int count = s == null ? 1 : parseInt(s);
            for (int i = 0; i < count; i++) {
                d.addSideboard(m.group(3));
            }
        }

        return d;
    }

    private String deriveFileName(String deckName) {
        //skips all but the listed characters
        return deckName.replaceAll("[^-_$#@.{[()]} a-zA-Z0-9]", "");
    }

    public void writeFile() {
        try {
            //store the files that do exist
            List<File> files = new ArrayList<File>();
            files.addAll(asList(deckDir.listFiles(DCKFileFilter)));

            //save the files and remove them from the list
            for (Deck deck : deckMap.values()) {
                File f = new File(deckDir, deriveFileName(deck.getName()) + ".dck");
                files.remove(f);
                BufferedWriter out = new BufferedWriter(new FileWriter(f));
                write(deck, out);
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
            for (Entry<String, Deck[]> e : boosterMap.entrySet()) {
                File f = new File(deckDir, deriveFileName(e.getValue()[0].getName()) + ".bdk");
                f.mkdir();
                for (int i = 0; i < e.getValue().length; i++) {
                    BufferedWriter out = new BufferedWriter(new FileWriter(new File(f, i + ".dck")));
                    write(e.getValue()[i], out);
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
            throw new RuntimeException("DeckManager : write() error, " + ex.getMessage());
        }
    }

    private void write(Deck d, BufferedWriter out) throws IOException {
        out.write(format("%s%n", d.getName()));
        if (d.getComment() != null) {
            out.write(format("%s%n", d.getComment()));
        }
        out.write(format("%s%n", "[general]"));
        out.write(format("%s%n", d.getDeckType()));
        out.write(format("%s%n", "[main]"));
        for (Entry<String, Integer> e : count(d.getMain()).entrySet()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey()));
        }
        out.write(format("%s%n", "[sideboard]"));
        for (Entry<String, Integer> e : count(d.getSideboard()).entrySet()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey()));
        }
    }

    private Map<String, Integer> count(List<String> src) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (String s : src) {
            Integer dstValue = result.get(s);
            if (dstValue == null) {
                result.put(s, 1);
            }
            else {
                result.put(s, dstValue + 1);
            }
        }
        return result;
    }

    public Deck getDeck(String deckName) {
        return deckMap.get(deckName);
    }
}
