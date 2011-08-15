package forge;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;
import forge.card.trigger.TriggerHandler;
import forge.error.ErrorViewer;
import forge.gui.MultiPhaseProgressMonitorWithETA;
import forge.properties.NewConstants;
import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.generator.FindNonDirectoriesSkipDotDirectoriesGenerator;
import net.slightlymagic.braids.util.generator.GeneratorFunctions;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * <p>CardReader class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardReader implements Runnable, NewConstants {
    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "US-ASCII";

    /** Regex that matches a single hyphen (-) or space. */
    public static final Pattern HYPHEN_OR_SPACE = Pattern.compile("[ -]");

    /** Regex for punctuation that we omit from card file names. */
    public static final Pattern PUNCTUATION_TO_ZAP = Pattern.compile("[,'\"]");

    /** Regex that matches two or more underscores (_). */
    public static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+");

    /** Special value for estimatedFilesRemaining. */
    protected static final int UNKNOWN_NUMBER_OF_FILES_REMAINING = -1;

    private Map<String, Card> mapToFill;
    private File cardsfolder;

    private ZipFile zip;
    private Charset charset;

    private Enumeration<? extends ZipEntry> zipEnum;
    private long estimatedFilesRemaining = UNKNOWN_NUMBER_OF_FILES_REMAINING;
    private Iterable<File> findNonDirsIterable;



    /**
     * This is a convenience for CardReader(cardsfolder, mapToFill, true); .
     *
     * @param theCardsFolder  indicates location of the cardsFolder
     *
     * @param theMapToFill  maps card names to Card instances; this is where we
     * place the cards once read
     *
     */
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill) {
        this(theCardsFolder, theMapToFill, true);
    }

    /**
     * <p>Constructor for CardReader.</p>
     *
     * @param theCardsFolder  indicates location of the cardsFolder
     *
     * @param theMapToFill  maps card names to Card instances; this is where we
     * place the cards once read
     *
     * @param useZip  if true, attempts to load cards from a zip file, if one exists.
     */
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill, final boolean useZip) {
        if (theMapToFill == null) {
            throw new NullPointerException("theMapToFill must not be null.");
        }
        this.mapToFill = theMapToFill;

        if (!theCardsFolder.exists()) {
            throw new RuntimeException("CardReader : constructor error -- file not found -- filename is "
                    + theCardsFolder.getAbsolutePath());
        }

        if (!theCardsFolder.isDirectory()) {
            throw new RuntimeException("CardReader : constructor error -- not a directory -- "
                    + theCardsFolder.getAbsolutePath());
        }

        this.cardsfolder = theCardsFolder;


        final File zipFile = new File(theCardsFolder, "cardsfolder.zip");

        // Prepare resources to read cards lazily.
        if (useZip && zipFile.exists()) {
            try {
                this.zip = new ZipFile(zipFile);
            } catch (Exception exn) {
                System.err.println("Error reading zip file \""
                        + zipFile.getAbsolutePath() + "\": " + exn + ". "
                        + "Defaulting to txt files in \""
                        + theCardsFolder.getAbsolutePath()
                        + "\"."
                        );
            }

        }

        if (useZip && zip != null) {
            zipEnum = zip.entries();
            estimatedFilesRemaining = zip.size();
        }

        setEncoding(DEFAULT_CHARSET_NAME);

    } //CardReader()


    /**
     * This finalizer helps assure there is no memory or thread leak with
     * findNonDirsIterable, which was created with YieldUtils.toIterable.
     *
     * @throws Throwable indirectly
     */
    protected final void finalize() throws Throwable {
        try {
            if (findNonDirsIterable != null) {
                for (@SuppressWarnings("unused") File ignored
                        : findNonDirsIterable)
                {
                    // Do nothing; just exercising the Iterable.
                }
            }
        } finally {
            super.finalize();
        }
    }


    /**
     * Reads the rest of ALL the cards into memory.  This is not lazy.
     */
    public final void run() {
        loadCardsUntilYouFind(null);
    }

    /**
     * Starts reading cards into memory until the given card is found.
     *
     * After that, we save our place in the list of cards (on disk) in case we
     * need to load more.
     *
     * @param cardName the name to find; if null, load all cards.
     *
     * @return the Card or null if it was not found.
     */
    protected final Card loadCardsUntilYouFind(final String cardName) {
        Card result = null;

        MultiPhaseProgressMonitorWithETA monitor;

        if (zip != null) {
            monitor = new MultiPhaseProgressMonitorWithETA("Forge - Loading card database from zip file", 1,
                    estimatedFilesRemaining, 1.0f);

            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (zipEnum.hasMoreElements()) {
                entry = (ZipEntry) zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(".txt")) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = loadCard(entry);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if (cardName != null && cardName.equals(result.getName())) {
                    break;
                }
            }

        } else {
            if (estimatedFilesRemaining == UNKNOWN_NUMBER_OF_FILES_REMAINING) {
                final Generator<File> findNonDirsGen = new FindNonDirectoriesSkipDotDirectoriesGenerator(cardsfolder);
                estimatedFilesRemaining = GeneratorFunctions.estimateSize(findNonDirsGen);
                findNonDirsIterable = YieldUtils.toIterable(findNonDirsGen);
            }

            monitor = new MultiPhaseProgressMonitorWithETA("Forge - Loading card database from files", 1,
                    estimatedFilesRemaining, 1.0f);

            for (File cardTxtFile : findNonDirsIterable) {
                if (!cardTxtFile.getName().endsWith(".txt")) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = loadCard(cardTxtFile);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if (cardName != null && cardName.equals(result.getName())) {
                    break;  // no thread leak here if entire card DB is loaded, or if this object is finalized.
                }

            } //endfor

        } //endif

        if (monitor != null) {
            monitor.dispose();
        }

        return result;
    } //loadCardsUntilYouFind(String)


    /**
     * <p>addTypes to an existing card.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param types a {@link java.lang.String} object.
     */
    public static void addTypes(final Card card, final String types) {
        final StringTokenizer tok = new StringTokenizer(types);
        while (tok.hasMoreTokens()) {
            card.addType(tok.nextToken());
        }
    }

    /**
     * <p>Reads a line from the given reader and handles exceptions.</p>
     *
     * @return a {@link java.lang.String} object.
     * @param reader a {@link java.io.BufferedReader} object.
     */
    public static String readLine(final BufferedReader reader) {
        //makes the checked exception, into an unchecked runtime exception
        try {
            String line = reader.readLine();
            if (line != null) {
                line = line.trim();
            }
            return line;
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("CardReader : readLine(Card) error", ex);
        }
    } //readLine(BufferedReader)

    /**
     * <p>load a card.</p>
     *
     * @param inputStream  the stream from which to load the card's information
     *
     * @return the card loaded from the stream
     */
    protected final Card loadCard(final InputStream inputStream) {
        final Card card = new Card();

        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, charset);
            reader = new BufferedReader(inputStreamReader);

            String s = readLine(reader);
            while (!"End".equals(s)) {
                if (s.startsWith("#")) {
                    //no need to do anything, this indicates a comment line
                } else if (s.startsWith("Name:")) {
                    String t = s.substring(5);
                    //System.out.println(s);
                    if (!mapToFill.containsKey(t)) {
                        card.setName(t);
                    } else {
                        break;  // this card has already been loaded.
                    }
                } else if (s.startsWith("ManaCost:")) {
                    String t = s.substring(9);
                    //System.out.println(s);
                    if (!"no cost".equals(t)) {
                        card.setManaCost(t);
                    }
                } else if (s.startsWith("Types:")) {
                    addTypes(card, s.substring(6));
                } else if (s.startsWith("Text:")) {
                    String t = s.substring(5);
                    // if (!t.equals("no text"));
                    if ("no text".equals(t)) {
                        t = "";
                    }
                    card.setText(t);
                } else if (s.startsWith("PT:")) {
                    String t = s.substring(3);
                    String[] pt = t.split("/");
                    int att = pt[0].contains("*") ? 0 : Integer.parseInt(pt[0]);
                    int def = pt[1].contains("*") ? 0 : Integer.parseInt(pt[1]);
                    card.setBaseAttackString(pt[0]);
                    card.setBaseDefenseString(pt[1]);
                    card.setBaseAttack(att);
                    card.setBaseDefense(def);
                } else if (s.startsWith("Loyalty:")) {
                    String[] splitStr = s.split(":");
                    int loyal = Integer.parseInt(splitStr[1]);
                    card.setBaseLoyalty(loyal);
                } else if (s.startsWith("K:")) {
                    String t = s.substring(2);
                    card.addIntrinsicKeyword(t);
                } else if (s.startsWith("SVar:")) {
                    String[] t = s.split(":", 3);
                    card.setSVar(t[1], t[2]);
                } else if (s.startsWith("A:")) {
                    String t = s.substring(2);
                    card.addIntrinsicAbility(t);
                } else if (s.startsWith("T:")) {
                    String t = s.substring(2);
                    card.addTrigger(TriggerHandler.parseTrigger(t, card, true));
                } else if (s.startsWith("S:")) {
                    String t = s.substring(2);
                    card.addStaticAbilityString(t);
                } else if (s.startsWith("SetInfo:")) {
                    String t = s.substring(8);
                    card.addSet(new SetInfo(t));
                }

                s = readLine(reader);
            } // while !End
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
            try {
                inputStreamReader.close();
            } catch (IOException ignored) {
            }
        }

        mapToFill.put(card.getName(), card);
        return card;
    }


    /**
     * Set the character encoding to use when loading cards.
     *
     * @param charsetName  the name of the charset, for example, "UTF-8"
     */
    public final void setEncoding(final String charsetName) {
        this.charset = Charset.forName(charsetName);
    }


    /**
     * Load a card from a txt file.
     *
     * @param pathToTxtFile  the full or relative path to the file to load
     *
     * @return a new Card instance
     */
    protected final Card loadCard(final File pathToTxtFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(pathToTxtFile);
            return loadCard(fileInputStream);
        } catch (FileNotFoundException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", pathToTxtFile.getAbsolutePath());
            throw new RuntimeException("CardReader : run error -- file exception -- filename is "
                    + pathToTxtFile.getPath(), ex);
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Load a card from an entry in a zip file.
     *
     * @param entry  to load from
     *
     * @return a new Card instance
     */
    protected final Card loadCard(final ZipEntry entry) {
        InputStream zipInputStream = null;
        try {
            zipInputStream = zip.getInputStream(entry);
            return loadCard(zipInputStream);

        } catch (IOException exn) {
            throw new RuntimeException(exn);
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }


    /**
     * Attempt to guess what the path to a given card's txt file would be.
     *
     * @param asciiCardName  the card name in canonicalized ASCII form
     *
     * @return  the likeliest path of the card's txt file, excluding
     * cardsFolder but including the subdirectory of that and the ".txt"
     * suffix.  For example, "e/elvish_warrior.txt"
     *
     * @see CardUtil#canonicalizeCardName
     */
    public final String toMostLikelyPath(final String asciiCardName) {
        String baseFileName = asciiCardName;

        /*
         * friarsol wrote: "hyphens and spaces are converted to underscores,
         * commas and apostrophes are removed (I'm not sure if there are any
         * other punctuation used)."
         *
         * @see http://www.slightlymagic.net/forum/viewtopic.php?f=52&t=4887#p63189
         */

        baseFileName = HYPHEN_OR_SPACE.matcher(baseFileName).replaceAll("_");
        baseFileName = MULTIPLE_UNDERSCORES.matcher(baseFileName).replaceAll("_");
        baseFileName = PUNCTUATION_TO_ZAP.matcher(baseFileName).replaceAll("");

        // Place the file within a single-letter subdirectory.
        StringBuffer buf = new StringBuffer(1 + 1 + baseFileName.length() + ".txt".length());
        buf.append(Character.toLowerCase(baseFileName.charAt(0)));

        // Zip file is always created with unix-style path names.
        buf.append('/');

        buf.append(baseFileName.toLowerCase(Locale.ENGLISH));
        buf.append(".txt");

        return buf.toString();
    }

    /**
     * Attempt to load a card by its canonical ASCII name.
     *
     * @param canonicalASCIIName  the canonical ASCII name of the card
     *
     * @return a new Card instance having that name, or null if not found
     */
    public final Card findCard(final String canonicalASCIIName) {
        UtilFunctions.checkNotNull("canonicalASCIIName", canonicalASCIIName);

        String cardFilePath = toMostLikelyPath(canonicalASCIIName);

        Card result = null;

        if (zip != null) {
            ZipEntry entry = zip.getEntry(cardFilePath);

            if (entry != null) {
                result = loadCard(entry);
            } else {
                //System.err.println(":Could not find \"" + cardFilePath + "\" in zip file.");
            }
        }

        if (result == null) {
            result = loadCard(new File(cardsfolder, cardFilePath));
        }

        if (result == null || !(result.getName().equals(canonicalASCIIName))) {
            //System.err.println(":Could not find \"" + cardFilePath + "\".");
            result = loadCardsUntilYouFind(canonicalASCIIName);
        }

        return result;
    }
}
