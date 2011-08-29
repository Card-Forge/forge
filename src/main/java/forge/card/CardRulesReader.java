package forge.card;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.generator.FindNonDirectoriesSkipDotDirectoriesGenerator;
import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;
import net.slightlymagic.braids.util.progress_monitor.StderrProgressMonitor;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.card.trigger.TriggerHandler;
import forge.error.ErrorViewer;
import forge.properties.NewConstants;
import forge.view.FView;


/**
 * <p>CardReader class.</p>
 *
 * Forked from forge.CardReader at rev 10010.
 * 
 * @version $Id$
 */
public class CardRulesReader
    //implements Runnable,  // NOPMD by Braids on 8/18/11 10:55 PM
    implements NewConstants
{
    private static final String CARD_FILE_DOT_EXTENSION = ".txt"; // NOPMD by Braids on 8/18/11 11:04 PM

    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "US-ASCII"; // NOPMD by Braids on 8/18/11 10:54 PM

    /** Regex that matches a single hyphen (-) or space. */
    public static final Pattern HYPHEN_OR_SPACE = Pattern.compile("[ -]");

    /** Regex for punctuation that we omit from card file names. */
    public static final Pattern PUNCTUATION_TO_ZAP = Pattern.compile("[,'\"]"); // NOPMD by Braids on 8/18/11 10:54 PM

    /** Regex that matches two or more underscores (_). */
    public static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+"); // NOPMD by Braids on 8/18/11 10:54 PM

    /** Special value for estimatedFilesRemaining. */
    protected static final int UNKNOWN_NUMBER_OF_FILES_REMAINING = -1; // NOPMD by Braids on 8/18/11 10:54 PM

    private transient Map<String, CardRules> mapToFill;
    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;

    private transient Enumeration<? extends ZipEntry> zipEnum;

    private transient long estimatedFilesRemaining = // NOPMD by Braids on 8/18/11 10:56 PM
            UNKNOWN_NUMBER_OF_FILES_REMAINING;

    private transient Iterable<File> findNonDirsIterable; // NOPMD by Braids on 8/18/11 10:56 PM



    /**
     * This is a convenience for CardReader(cardsfolder, mapToFill, true); .
     *
     * @param theCardsFolder  indicates location of the cardsFolder
     *
     * @param theMapToFill  maps card names to Card instances; this is where we
     * place the cards once read
     *
     public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill) {
        this(theCardsFolder, theMapToFill, true);
    }
     */

    /**
     * <p>Constructor for CardReader.</p>
     *
     * @param theCardsFolder  indicates location of the cardsFolder
     *
     * @param theMapToFill  maps card names to Card instances; this is where we
     * place the cards once read
     *
     * @param useZip  if true, attempts to load cards from a zip file, if one exists.
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill, final boolean useZip) {
        if (theMapToFill == null) {
            throw new NullPointerException("theMapToFill must not be null."); // NOPMD by Braids on 8/18/11 10:53 PM
        }
        this.mapToFill = theMapToFill;

        if (!theCardsFolder.exists()) {
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : constructor error -- file not found -- filename is "
                    + theCardsFolder.getAbsolutePath());
        }

        if (!theCardsFolder.isDirectory()) {
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : constructor error -- not a directory -- "
                    + theCardsFolder.getAbsolutePath());
        }

        this.cardsfolder = theCardsFolder;


        final File zipFile = new File(theCardsFolder, "cardsfolder.zip");

        // Prepare resources to read cards lazily.
        if (useZip && zipFile.exists()) {
            try {
                this.zip = new ZipFile(zipFile);
            } catch (Exception exn) {
                System.err.println("Error reading zip file \"" // NOPMD by Braids on 8/18/11 10:53 PM
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
     */


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
    public final void run() { 
        loadCardsUntilYouFind(null);
    }
     */

    /**
     * Starts reading cards into memory until the given card is found.
     *
     * After that, we save our place in the list of cards (on disk) in case we
     * need to load more.
     *
     * @param cardName the name to find; if null, load all cards.
     *
     * @return the Card or null if it was not found.
    protected final Card loadCardsUntilYouFind(final String cardName) {
        Card result = null;

        // Try to retrieve card loading progress monitor model.
        // If no progress monitor present, output results to console.
        BaseProgressMonitor monitor = null;
        final FView view = Singletons.getView();
        if (view != null) {
            monitor = view.getCardLoadingProgressMonitor();
        }
        
        if (monitor == null) {
            monitor = new StderrProgressMonitor(1, 0L);
        }

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.
        if (zip == null) {
            if (estimatedFilesRemaining == UNKNOWN_NUMBER_OF_FILES_REMAINING) {
                final Generator<File> findNonDirsGen = new FindNonDirectoriesSkipDotDirectoriesGenerator(cardsfolder);
                estimatedFilesRemaining = GeneratorFunctions.estimateSize(findNonDirsGen);
                findNonDirsIterable = YieldUtils.toIterable(findNonDirsGen);
            }

            monitor.setTotalUnitsThisPhase(estimatedFilesRemaining);

            for (File cardTxtFile : findNonDirsIterable) {
                if (!cardTxtFile.getName().endsWith(CARD_FILE_DOT_EXTENSION)) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = loadCard(cardTxtFile);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if (cardName != null && cardName.equals(result.getName())) {
                    break;  // no thread leak here if entire card DB is loaded, or if this object is finalized.
                }

            } //endfor

        } else {
            monitor.setTotalUnitsThisPhase(estimatedFilesRemaining);
            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (zipEnum.hasMoreElements()) {
                entry = (ZipEntry) zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(CARD_FILE_DOT_EXTENSION)) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = loadCard(entry);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if (cardName != null && cardName.equals(result.getName())) {
                    break;
                }
            }

        } //endif

        return result;
    } //loadCardsUntilYouFind(String)
     */


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
            throw new RuntimeException("CardReader : readLine(Card) error", ex); // NOPMD by Braids on 8/18/11 10:53 PM
        }
    } //readLine(BufferedReader)

    /**
     * <p>Load a card from an InputStream.</p>
     *
     * @param txtFileLocator  describes the location of the txt file we are
     * parsing
     * 
     * @param inputStream  the stream from which to load the card's information
     *
     * @return the card loaded from the stream
     * 
     * @throws CardParsingException  if there is something wrong with the
     * stream's contents
     */
    protected final CardRules loadCard(final String txtFileLocator, final InputStream inputStream)
        throws CardParsingException
    {
        int lineNum = 0;

        String cardName = null;
        CardType cardType = null;
        String manacost = null;
        String ptLine = null;
        String[] cardRules = null;
        Map<String, CardInSet> setsData = new TreeMap<String, CardInSet>();
        boolean removedFromAIDecks = false;
        boolean removedFromRandomDecks = false;

        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, charset);
            reader = new BufferedReader(inputStreamReader);

            while (true) {
                String line = readLine(reader);
                lineNum++;

                if (line.charAt(0) == '#') {
                    //no need to do anything, this indicates a comment line
                    continue;

                } else if (line.startsWith("Name:")) {
                    cardName = getValueAfterKey(line, "Name:");

                    if (cardName == null || cardName.isEmpty()) {
                        throw new CardParsingException(txtFileLocator, lineNum, "Card name is empty");
                    }

                } else if (line.startsWith("ManaCost:")) {
                    final String value = getValueAfterKey(line, "ManaCost:");

                    if (!"no cost".equals(value)) {
                        manacost = value;
                    }
                    else {
                        assert manacost == null;
                    }

                } else if (line.startsWith("Types:")) {
                    final String value = getValueAfterKey(line, "Types:");

                    try {
                        cardType = CardType.parse(value);
                    }
                    catch (Throwable exn) {
                        throw new CardParsingException(txtFileLocator, lineNum,
                                "In Types: " + exn.getMessage(), exn);
                    }

                } else if (line.startsWith("Oracle:")) {
                    final String value = getValueAfterKey(line, "Oracle:");
                    cardRules = value.split("\\n");

                } else if (line.startsWith("PT:")) {
                    throwCPEIfPTIsNotNull(ptLine, txtFileLocator, lineNum);
                    ptLine = getValueAfterKey(line, "PT:");

                } else if (line.startsWith("Loyalty:")) {
                    throwCPEIfPTIsNotNull(ptLine, txtFileLocator, lineNum);
                    ptLine = getValueAfterKey(line, "Loyalty:");

                } else if (line.startsWith("SVar:RemAIDeck:")) {
                    final String value = getValueAfterKey(line, "SVar:RemAIDeck:");
                    removedFromAIDecks = ("True".equalsIgnoreCase(value));

                } else if (line.startsWith("SVar:RemRandomDeck:")) {
                    final String value = getValueAfterKey(line, "SVar:RemRandomDeck:");
                    removedFromRandomDecks = ("True".equalsIgnoreCase(value));

                } else if (line.startsWith("SetInfo:")) {
                    parseSetInfoLine(txtFileLocator, lineNum, line, setsData);

                } else if ("End".equals(line)) {
                    break;
                }

            } // while true

        } finally {
            try {
                reader.close();
            } catch (IOException ignored) { // NOPMD by Braids on 8/18/11 11:08 PM
            }
            try {
                inputStreamReader.close();
            } catch (IOException ignored) { // NOPMD by Braids on 8/18/11 11:08 PM
            }
        }

        try {
            return new CardRules(cardName, cardType, manacost, ptLine, cardRules, setsData, removedFromRandomDecks,
                    removedFromAIDecks);
        }
        catch (Throwable exn) {
            throw new CardParsingException(txtFileLocator, lineNum,
                    "Error constructing CardRules instance: " + exn.toString(),
                    exn);
        }
    }

    /**
     * Parse a SetInfo line from a card txt file.
     * 
     * @param txtFileLocator  used in error messages
     * @param lineNum  used in error messages
     * @param line  must begin with "SetInfo:"
     * @param setsData  the current mapping of set names to CardInSet instances
     * 
     * @throws CardParsingException  if there is a problem parsing the line
     */
    public static void parseSetInfoLine(final String txtFileLocator, final int lineNum, final String line,
            final Map<String, CardInSet> setsData)
            throws CardParsingException
    {
        final int setCodeIx = 0;
        final int rarityIx = 1;
        final int numPicIx = 3;

        // Sample SetInfo line:
        //SetInfo:POR|Land|http://magiccards.info/scans/en/po/203.jpg|4

        final String value = line.substring("SetInfo:".length());
        final String[] pieces = value.split("\\|");

        if (pieces.length <= rarityIx) {
            throw new CardParsingException(txtFileLocator, lineNum,
                    "SetInfo line <<" + value + ">> has insufficient pieces");
        }

        final String setCode = pieces[setCodeIx];
        final String txtRarity = pieces[rarityIx];
        // pieces[2] is the magiccards.info URL for illustration #1, which we do not need.
        int numIllustrations = 1;

        if (setsData.containsKey(setCode)) {
            throw new CardParsingException(txtFileLocator, lineNum,
                    "Found multiple SetInfo lines for set code <<" + setCode + ">>");
        }

        if (pieces.length > numPicIx) {
            try {
                numIllustrations = Integer.parseInt(pieces[numPicIx]);
            }
            catch (NumberFormatException nfe) {
                throw new CardParsingException(txtFileLocator, lineNum,
                        "Fourth item of SetInfo is not an integer in <<"
                        + value + ">>");
            }

            if (numIllustrations < 1) {
                throw new CardParsingException(txtFileLocator, lineNum,
                        "Fourth item of SetInfo is not a positive integer, but"
                        + numIllustrations);
            }
        }

        CardRarity rarity = null;
        if ("Land".equals(txtRarity)) {
            rarity = CardRarity.BasicLand;
        }
        else if ("Common".equals(txtRarity)) {
            rarity = CardRarity.Common;
        }
        else if ("Uncommon".equals(txtRarity)) {
            rarity = CardRarity.Uncommon;
        }
        else if ("Rare".equals(txtRarity)) {
            rarity = CardRarity.Rare;
        }
        else if ("Mythic".equals(txtRarity)) {
            rarity = CardRarity.MythicRare;
        }
        else if ("Special".equals(txtRarity)) {
            rarity = CardRarity.Special;
        }
        else {
            throw new CardParsingException(txtFileLocator, lineNum,
                    "Unrecognized rarity string <<" + txtRarity + ">>");
        }

        CardInSet cardInSet = new CardInSet(rarity, numIllustrations);

        setsData.put(setCode, cardInSet);
    }

    /**
     * Test if ptLine is null; if it is not, throw a CardParsingException.
     * 
     * @param ptLine  the previously seen power/toughness or loyalty value, if any
     * @param txtFileLocator  describes location of the card's txt file
     * @param lineNum  the line number just read
     * 
     * @throws CardParsingException  iff ptLine is not null
     */
    public static void throwCPEIfPTIsNotNull(final String ptLine, final String txtFileLocator, final int lineNum)
            throws CardParsingException
    {
        if (ptLine != null) {
            throw new CardParsingException(txtFileLocator, lineNum,
                    "more than one PT or Loyalty is present");
        }
    }

    /**
     * Parse the value from a card.txt line.
     * 
     * Throws {@link IndexOutOfBoundsException} if fieldNameWithColon is not in line.
     * 
     * @param line  the raw line; may have newline at end
     * 
     * @param fieldNameWithColon  the field name with its colon, used to
     * identify the key
     * 
     * @return the value after the colon, with its leading and trailing
     * whitespace removed
     */
    public static String getValueAfterKey(final String line, final String fieldNameWithColon) {
        final int startIx = fieldNameWithColon.length();
        final String lineAfterColon = line.substring(startIx);
        return lineAfterColon.trim();
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
    protected final Card loadCard(final File pathToTxtFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(pathToTxtFile);
            return loadCard(fileInputStream);
        } catch (FileNotFoundException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", pathToTxtFile.getAbsolutePath());
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : run error -- file exception -- filename is "
                    + pathToTxtFile.getPath(), ex);
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException ignored) { // NOPMD by Braids on 8/18/11 11:08 PM
            }
        }
    }
     */

    /**
     * Load a card from an entry in a zip file.
     *
     * @param entry  to load from
     *
     * @return a new Card instance
    protected final Card loadCard(final ZipEntry entry) {
        InputStream zipInputStream = null;
        try {
            zipInputStream = zip.getInputStream(entry);
            return loadCard(zipInputStream);

        } catch (IOException exn) {
            throw new RuntimeException(exn); // NOPMD by Braids on 8/18/11 10:53 PM
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (IOException ignored) { // NOPMD by Braids on 8/18/11 11:08 PM
            }
        }
    }
     */


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
        final StringBuffer buf = new StringBuffer(1 + 1 + baseFileName.length() + CARD_FILE_DOT_EXTENSION.length());
        buf.append(Character.toLowerCase(baseFileName.charAt(0)));

        // Zip file is always created with unix-style path names.
        buf.append('/');

        buf.append(baseFileName.toLowerCase(Locale.ENGLISH));
        buf.append(CARD_FILE_DOT_EXTENSION);

        return buf.toString();
    }

    /**
     * Attempt to load a card by its canonical ASCII name.
     *
     * @param canonicalASCIIName  the canonical ASCII name of the card
     *
     * @return a new Card instance having that name, or null if not found
    public final Card findCard(final String canonicalASCIIName) { // NOPMD by Braids on 8/18/11 11:08 PM
        UtilFunctions.checkNotNull("canonicalASCIIName", canonicalASCIIName);

        final String cardFilePath = toMostLikelyPath(canonicalASCIIName);

        Card result = null;

        if (zip != null) {
            final ZipEntry entry = zip.getEntry(cardFilePath);

            if (entry != null) {
                result = loadCard(entry);
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
     */
}
