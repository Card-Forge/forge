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
package forge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.generator.FindNonDirectoriesSkipDotDirectoriesGenerator;
import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import net.slightlymagic.braids.util.progress_monitor.StderrProgressMonitor;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.card.trigger.TriggerHandler;
import forge.error.ErrorViewer;
import forge.view.FView;

/**
 * <p>
 * CardReader class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardReader implements Runnable {
    // NOPMD by Braids on 8/18/11 10:55
    // PM
    private static final String CARD_FILE_DOT_EXTENSION = ".txt"; // NOPMD by
                                                                  // Braids on
                                                                  // 8/18/11
                                                                  // 11:04 PM

    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "US-ASCII"; // NOPMD by
                                                                  // Braids on
                                                                  // 8/18/11
                                                                  // 10:54 PM

    /** Regex that matches a single hyphen (-) or space. */
    public static final Pattern HYPHEN_OR_SPACE = Pattern.compile("[ -]");

    /** Regex for punctuation that we omit from card file names. */
    public static final Pattern PUNCTUATION_TO_ZAP = Pattern.compile("[,'\"]"); // NOPMD
                                                                                // by
                                                                                // Braids
                                                                                // on
                                                                                // 8/18/11
                                                                                // 10:54
                                                                                // PM

    /** Regex that matches two or more underscores (_). */
    public static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+"); // NOPMD
                                                                               // by
                                                                               // Braids
                                                                               // on
                                                                               // 8/18/11
                                                                               // 10:54
                                                                               // PM

    /** Special value for estimatedFilesRemaining. */
    protected static final int UNKNOWN_NUMBER_OF_FILES_REMAINING = -1; // NOPMD
                                                                       // by
                                                                       // Braids
                                                                       // on
                                                                       // 8/18/11
                                                                       // 10:54
                                                                       // PM

    private transient Map<String, Card> mapToFill;
    private transient List<CardRules> listRulesToFill;
    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;
    private transient CardRulesReader rulesReader;

    private transient Enumeration<? extends ZipEntry> zipEnum;

    private transient long estimatedFilesRemaining = // NOPMD by Braids on
    CardReader.// 8/18/11 10:56 PM
    UNKNOWN_NUMBER_OF_FILES_REMAINING;

    private transient Iterable<File> findNonDirsIterable; // NOPMD by Braids on
                                                          // 8/18/11 10:56 PM

    /**
     * Instantiates a new card reader.
     * 
     * @param theCardsFolder
     *            the the cards folder
     * @param theMapToFill
     *            the the map to fill
     */
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill) {
        this(theCardsFolder, theMapToFill, null, true);
    }

    /**
     * This is a convenience for CardReader(cardsfolder, mapToFill, true); .
     * 
     * @param theCardsFolder
     *            indicates location of the cardsFolder
     * @param theMapToFill
     *            maps card names to Card instances; this is where we place the
     *            cards once read
     * @param listRules2Fill
     *            List<CardRules>
     */
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill,
            final List<CardRules> listRules2Fill) {
        this(theCardsFolder, theMapToFill, listRules2Fill, true);
    }

    /**
     * <p>
     * Constructor for CardReader.
     * </p>
     * 
     * @param theCardsFolder
     *            indicates location of the cardsFolder
     * 
     * @param theMapToFill
     *            maps card names to Card instances; this is where we place the
     *            cards once read
     * @param listRules2Fill
     *            List<CardRules>
     * @param useZip
     *            if true, attempts to load cards from a zip file, if one
     *            exists.
     */
    public CardReader(final File theCardsFolder, final Map<String, Card> theMapToFill,
            final List<CardRules> listRules2Fill, final boolean useZip) {
        if (theMapToFill == null) {
            throw new NullPointerException("theMapToFill must not be null."); // NOPMD
                                                                              // by
                                                                              // Braids
                                                                              // on
                                                                              // 8/18/11
                                                                              // 10:53
                                                                              // PM
        }
        this.mapToFill = theMapToFill;
        // These read data for lightweight classes.
        this.listRulesToFill = listRules2Fill == null ? new ArrayList<CardRules>() : listRules2Fill;
        this.rulesReader = new CardRulesReader();

        if (!theCardsFolder.exists()) {
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : constructor error -- file not found -- filename is "
                            + theCardsFolder.getAbsolutePath());
        }

        if (!theCardsFolder.isDirectory()) {
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : constructor error -- not a directory -- " + theCardsFolder.getAbsolutePath());
        }

        this.cardsfolder = theCardsFolder;

        final File zipFile = new File(theCardsFolder, "cardsfolder.zip");

        // Prepare resources to read cards lazily.
        if (useZip && zipFile.exists()) {
            try {
                this.zip = new ZipFile(zipFile);
            } catch (final Exception exn) {
                System.err.println("Error reading zip file \"" // NOPMD by
                                                               // Braids on
                                                               // 8/18/11 10:53
                                                               // PM
                        + zipFile.getAbsolutePath()
                        + "\": "
                        + exn
                        + ". "
                        + "Defaulting to txt files in \""
                        + theCardsFolder.getAbsolutePath() + "\".");
            }

        }

        if (useZip && (this.zip != null)) {
            this.zipEnum = this.zip.entries();
            this.estimatedFilesRemaining = this.zip.size();
        }

        this.setEncoding(CardReader.DEFAULT_CHARSET_NAME);

    } // CardReader()

    /**
     * This finalizer helps assure there is no memory or thread leak with
     * findNonDirsIterable, which was created with YieldUtils.toIterable.
     * 
     * @throws Throwable
     *             indirectly
     */
    @Override
    protected final void finalize() throws Throwable {
        try {
            if (this.findNonDirsIterable != null) {
                for (@SuppressWarnings("unused")
                final// Do nothing; just exercising the Iterable.
                File ignored : this.findNonDirsIterable) {
                }
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Reads the rest of ALL the cards into memory. This is not lazy.
     */
    @Override
    public final void run() {
        this.loadCardsUntilYouFind(null);
    }

    /**
     * Starts reading cards into memory until the given card is found.
     * 
     * After that, we save our place in the list of cards (on disk) in case we
     * need to load more.
     * 
     * @param cardName
     *            the name to find; if null, load all cards.
     * 
     * @return the Card or null if it was not found.
     */
    protected final Card loadCardsUntilYouFind(final String cardName) {
        Card result = null;

        // Try to retrieve card loading progress monitor model.
        // If no progress monitor present, output results to console.
        BraidsProgressMonitor monitor = null;
        final FView view = Singletons.getView();
        if (view != null) {
            monitor = view.getCardLoadingProgressMonitor();
        }

        if (monitor == null) {
            monitor = new StderrProgressMonitor(1, 0L);
        }

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.
        if (this.zip == null) {
            if (this.estimatedFilesRemaining == CardReader.UNKNOWN_NUMBER_OF_FILES_REMAINING) {
                final Generator<File> findNonDirsGen = new FindNonDirectoriesSkipDotDirectoriesGenerator(
                        this.cardsfolder);
                this.estimatedFilesRemaining = GeneratorFunctions.estimateSize(findNonDirsGen);
                this.findNonDirsIterable = YieldUtils.toIterable(findNonDirsGen);
            }

            if (monitor != null) {
                monitor.setTotalUnitsThisPhase(this.estimatedFilesRemaining);
            }

            for (final File cardTxtFile : this.findNonDirsIterable) {
                if (!cardTxtFile.getName().endsWith(CardReader.CARD_FILE_DOT_EXTENSION)) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = this.loadCard(cardTxtFile);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if ((cardName != null) && cardName.equals(result.getName())) {
                    break; // no thread leak here if entire card DB is loaded,
                           // or if this object is finalized.
                }

            } // endfor

        } else {
            monitor.setTotalUnitsThisPhase(this.estimatedFilesRemaining);
            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (this.zipEnum.hasMoreElements()) {
                entry = this.zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(CardReader.CARD_FILE_DOT_EXTENSION)) {
                    monitor.incrementUnitsCompletedThisPhase(1L);
                    continue;
                }

                result = this.loadCard(entry);
                monitor.incrementUnitsCompletedThisPhase(1L);

                if ((cardName != null) && cardName.equals(result.getName())) {
                    break;
                }
            }

        } // endif

        return result;
    } // loadCardsUntilYouFind(String)

    /**
     * <p>
     * addTypes to an existing card.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param types
     *            a {@link java.lang.String} object.
     */
    public static void addTypes(final Card card, final String types) {
        final StringTokenizer tok = new StringTokenizer(types);
        while (tok.hasMoreTokens()) {
            card.addType(tok.nextToken());
        }
    }

    /**
     * <p>
     * Reads a line from the given reader and handles exceptions.
     * </p>
     * 
     * @param reader
     *            a {@link java.io.BufferedReader} object.
     * @return a {@link java.lang.String} object.
     */
    public static String readLine(final BufferedReader reader) {
        // makes the checked exception, into an unchecked runtime exception
        try {
            String line = reader.readLine();
            if (line != null) {
                line = line.trim();
            }
            return line;
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("CardReader : readLine(Card) error", ex); // NOPMD
                                                                                 // by
                                                                                 // Braids
                                                                                 // on
                                                                                 // 8/18/11
                                                                                 // 10:53
                                                                                 // PM
        }
    } // readLine(BufferedReader)

    /**
     * <p>
     * load a card.
     * </p>
     * 
     * @param inputStream
     *            the stream from which to load the card's information
     * 
     * @return the card loaded from the stream
     */
    protected final Card loadCard(final InputStream inputStream) {
        final Card card = new Card();
        this.rulesReader.reset();

        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, this.charset);
            reader = new BufferedReader(inputStreamReader);

            String line = CardReader.readLine(reader);
            while (!"End".equals(line)) {
                this.rulesReader.parseLine(line);
                if (line.isEmpty()) {
                    // Ignore empty lines.
                } else if (line.charAt(0) == '#') { // NOPMD by Braids on
                                                    // 8/18/11 10:59 PM
                    // no need to do anything, this indicates a comment line
                } else if (line.startsWith("Name:")) {
                    final String value = line.substring(5);
                    // System.out.println(s);
                    if (this.mapToFill.containsKey(value)) {
                        break; // this card has already been loaded.
                    } else {
                        card.setName(value);
                    }
                } else if (line.startsWith("ManaCost:")) {
                    final String value = line.substring(9);
                    // System.out.println(s);
                    if (!"no cost".equals(value)) {
                        card.setManaCost(value);
                    }
                } else if (line.startsWith("Types:")) {
                    CardReader.addTypes(card, line.substring("Types:".length()));
                } else if (line.startsWith("Text:")) {
                    String value = line.substring("Text:".length());
                    // if (!t.equals("no text"));
                    if ("no text".equals(value)) {
                        value = "";
                    }
                    card.setText(value);
                } else if (line.startsWith("PT:")) {
                    final String value = line.substring("PT:".length());
                    final String[] powTough = value.split("/");
                    int att;
                    if (powTough[0].contains("*")) {
                        att = 0;
                    } else {
                        att = Integer.parseInt(powTough[0]);
                    }

                    int def;
                    if (powTough[1].contains("*")) {
                        def = 0;
                    } else {
                        def = Integer.parseInt(powTough[1]);
                    }

                    card.setBaseAttackString(powTough[0]);
                    card.setBaseDefenseString(powTough[1]);
                    card.setBaseAttack(att);
                    card.setBaseDefense(def);
                } else if (line.startsWith("Loyalty:")) {
                    final String[] splitStr = line.split(":");
                    final int loyal = Integer.parseInt(splitStr[1]);
                    card.setBaseLoyalty(loyal);
                } else if (line.startsWith("K:")) {
                    final String value = line.substring(2);
                    card.addIntrinsicKeyword(value);
                } else if (line.startsWith("SVar:")) {
                    final String[] value = line.split(":", 3);
                    card.setSVar(value[1], value[2]);
                } else if (line.startsWith("A:")) {
                    final String value = line.substring(2);
                    card.addIntrinsicAbility(value);
                } else if (line.startsWith("T:")) {
                    final String value = line.substring(2);
                    card.addTrigger(TriggerHandler.parseTrigger(value, card, true));
                } else if (line.startsWith("S:")) {
                    final String value = line.substring(2);
                    card.addStaticAbilityString(value);
                } else if (line.startsWith("SetInfo:")) {
                    final String value = line.substring("SetInfo:".length());
                    card.addSet(new SetInfo(value)); // NOPMD by Braids on
                                                     // 8/18/11 11:08 PM
                } else if (line.equals("ALTERNATE")) {
                    String mode;
                    if (card.isFlip()) {
                        mode = "Flipped";
                    } else {
                        mode = "Transformed";
                    }
                    card.addAlternateState(mode);
                    card.setState(mode);
                } else if (line.startsWith("AlternateMode:")) {
                    final String value = line.substring("AlternateMode:".length());
                    if (value.equalsIgnoreCase("Flip")) {
                        card.setFlip(true);
                    } else {
                        card.setDoubleFaced(true);
                    }
                } else if (line.startsWith("Colors:")) {
                    final String value = line.substring("Colors:".length());
                    final ArrayList<CardColor> newCols = new ArrayList<CardColor>();
                    for (final String col : value.split(",")) {
                        final CardColor newCol = new CardColor(card);
                        newCol.addToCardColor(col);
                        newCols.add(newCol);
                    }

                    card.setColor(newCols);
                    card.setCardColorsOverridden(true);
                }

                line = CardReader.readLine(reader);
            } // while !End
        } finally {
            try {
                reader.close();
            } catch (final IOException ignored) { // NOPMD by Braids on 8/18/11
                                                  // 11:08
                // PM
            }
            try {
                inputStreamReader.close();
            } catch (final IOException ignored) { // NOPMD by Braids on 8/18/11
                                                  // 11:08
                // PM
            }
        }

        if (card.isInAlternateState()) {
            card.setState("Original");
        }

        this.listRulesToFill.add(this.rulesReader.getCard());
        this.mapToFill.put(card.getName(), card);
        return card;
    }

    /**
     * Set the character encoding to use when loading cards.
     * 
     * @param charsetName
     *            the name of the charset, for example, "UTF-8"
     */
    public final void setEncoding(final String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

    /**
     * Load a card from a txt file.
     * 
     * @param pathToTxtFile
     *            the full or relative path to the file to load
     * 
     * @return a new Card instance
     */
    protected final Card loadCard(final File pathToTxtFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(pathToTxtFile);
            return this.loadCard(fileInputStream);
        } catch (final FileNotFoundException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", pathToTxtFile.getAbsolutePath());
            throw new RuntimeException(// NOPMD by Braids on 8/18/11 10:53 PM
                    "CardReader : run error -- file exception -- filename is " + pathToTxtFile.getPath(), ex);
        } finally {
            try {
                fileInputStream.close();
            } catch (final IOException ignored) { // NOPMD by Braids on 8/18/11
                                                  // 11:08
                // PM
            }
        }
    }

    /**
     * Load a card from an entry in a zip file.
     * 
     * @param entry
     *            to load from
     * 
     * @return a new Card instance
     */
    protected final Card loadCard(final ZipEntry entry) {
        InputStream zipInputStream = null;
        try {
            zipInputStream = this.zip.getInputStream(entry);
            return this.loadCard(zipInputStream);

        } catch (final IOException exn) {
            throw new RuntimeException(exn); // NOPMD by Braids on 8/18/11 10:53
                                             // PM
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (final IOException ignored) { // NOPMD by Braids on 8/18/11
                                                  // 11:08
                // PM
            }
        }
    }

    /**
     * Attempt to guess what the path to a given card's txt file would be.
     * 
     * @param asciiCardName
     *            the card name in canonicalized ASCII form
     * 
     * @return the likeliest path of the card's txt file, excluding cardsFolder
     *         but including the subdirectory of that and the ".txt" suffix. For
     *         example, "e/elvish_warrior.txt"
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
         * @see
         * http://www.slightlymagic.net/forum/viewtopic.php?f=52&t=4887#p63189
         */

        baseFileName = CardReader.HYPHEN_OR_SPACE.matcher(baseFileName).replaceAll("_");
        baseFileName = CardReader.MULTIPLE_UNDERSCORES.matcher(baseFileName).replaceAll("_");
        baseFileName = CardReader.PUNCTUATION_TO_ZAP.matcher(baseFileName).replaceAll("");

        // Place the file within a single-letter subdirectory.
        final StringBuffer buf = new StringBuffer(1 + 1 + baseFileName.length()
                + CardReader.CARD_FILE_DOT_EXTENSION.length());
        buf.append(Character.toLowerCase(baseFileName.charAt(0)));

        // Zip file is always created with unix-style path names.
        buf.append('/');

        buf.append(baseFileName.toLowerCase(Locale.ENGLISH));
        buf.append(CardReader.CARD_FILE_DOT_EXTENSION);

        return buf.toString();
    }

    /**
     * Attempt to load a card by its canonical ASCII name.
     * 
     * @param canonicalASCIIName
     *            the canonical ASCII name of the card
     * 
     * @return a new Card instance having that name, or null if not found
     */
    public final Card findCard(final String canonicalASCIIName) { // NOPMD by
                                                                  // Braids on
                                                                  // 8/18/11
                                                                  // 11:08 PM
        UtilFunctions.checkNotNull("canonicalASCIIName", canonicalASCIIName);

        final String cardFilePath = this.toMostLikelyPath(canonicalASCIIName);

        Card result = null;

        if (this.zip != null) {
            final ZipEntry entry = this.zip.getEntry(cardFilePath);

            if (entry != null) {
                result = this.loadCard(entry);
            }
        }

        if (result == null) {
            result = this.loadCard(new File(this.cardsfolder, cardFilePath));
        }

        if ((result == null) || !(result.getName().equals(canonicalASCIIName))) {
            // System.err.println(":Could not find \"" + cardFilePath + "\".");
            result = this.loadCardsUntilYouFind(canonicalASCIIName);
        }

        return result;
    }
}
