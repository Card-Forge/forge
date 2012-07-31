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
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.SwingUtilities;


import forge.card.CardManaCost;
import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.card.EditionInfo;
import forge.card.mana.ManaCostParser;
import forge.card.replacement.ReplacementHandler;
import forge.card.trigger.TriggerHandler;
import forge.error.ErrorViewer;
import forge.gui.toolbox.FProgressBar;
import forge.util.FileUtil;
import forge.view.SplashFrame;

/**
 * <p>
 * CardReader class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardReader {

    // PM
    private static final String CARD_FILE_DOT_EXTENSION = ".txt";
    // Braids on
    // 8/18/11
    // 11:04 PM

    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "US-ASCII";
    // Braids on
    // 8/18/11
    // 10:54 PM

    /** Regex that matches a single hyphen (-) or space. */
    public static final Pattern HYPHEN_OR_SPACE = Pattern.compile("[ -]");

    /** Regex for punctuation that we omit from card file names. */
    public static final Pattern PUNCTUATION_TO_ZAP = Pattern.compile("[,'\"]");
    // by
    // Braids
    // on
    // 8/18/11
    // 10:54
    // PM

    /** Regex that matches two or more underscores (_). */
    public static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+");
    // by
    // Braids
    // on
    // 8/18/11
    // 10:54
    // PM

    /** Special value for estimatedFilesRemaining. */
    protected static final int UNKNOWN_NUMBER_OF_FILES_REMAINING = -1;
    // by
    // Braids
    // on
    // 8/18/11
    // 10:54
    // PM

    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;
    private transient CardRulesReader rulesReader;

    private transient Enumeration<? extends ZipEntry> zipEnum;

    private transient long estimatedFilesRemaining = CardReader.// 8/18/11 10:56
                                                                // PM
    UNKNOWN_NUMBER_OF_FILES_REMAINING;


    // 8/18/11 10:56 PM


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
    public CardReader(final File theCardsFolder, final boolean useZip) {

        // These read data for lightweight classes.
        this.rulesReader = new CardRulesReader();

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
            } catch (final Exception exn) {
                System.err.println("Error reading zip file \""
                        // Braids on
                        // 8/18/11 10:53
                        // PM
                        + zipFile.getAbsolutePath() + "\": " + exn + ". " + "Defaulting to txt files in \""
                        + theCardsFolder.getAbsolutePath() + "\".");
            }

            if (this.zip != null) {
                this.zipEnum = this.zip.entries();
                this.estimatedFilesRemaining = this.zip.size();
            }
        }

        this.setEncoding(CardReader.DEFAULT_CHARSET_NAME);

    } // CardReader()


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
    public final List<CardRules> loadCards() {
        
        List<CardRules> result = new ArrayList<CardRules>();
        final FProgressBar barProgress = SplashFrame.PROGRESS_BAR;

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.
        if (this.zip == null) {
            List<File> allFiles = new ArrayList<File>();
            if (this.estimatedFilesRemaining == CardReader.UNKNOWN_NUMBER_OF_FILES_REMAINING) {
                fillFilesArray(allFiles, this.cardsfolder);
                this.estimatedFilesRemaining = allFiles.size();

            }

            if (barProgress != null) {
                barProgress.setMaximum((int) this.estimatedFilesRemaining);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        barProgress.setDescription("Loading card data: ");
                    }
                });
            }

            for (final File cardTxtFile : allFiles) {
                if (!cardTxtFile.getName().endsWith(CardReader.CARD_FILE_DOT_EXTENSION)) {
                    barProgress.increment();
                    continue;
                }

                result.add(this.loadCard(cardTxtFile));
                barProgress.increment();

            } // endfor
        } else {
            barProgress.setMaximum((int) this.estimatedFilesRemaining);
            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (this.zipEnum.hasMoreElements()) {
                entry = this.zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(CardReader.CARD_FILE_DOT_EXTENSION)) {
                    barProgress.increment();
                    continue;
                }

                result.add(this.loadCard(entry));
                barProgress.increment();
            }
        } // endif

        return result;
    } // loadCardsUntilYouFind(String)

    /**
     * TODO: Write javadoc for this method.
     * @param allFiles
     * @param cardsfolder2
     */
    private void fillFilesArray(List<File> allFiles, File startDir) {
        String[] list = startDir.list();
            for (String filename : list) {
                File entry = new File(startDir, filename);

                if (!entry.isDirectory()) {
                    allFiles.add(entry);
                    continue;
                }
                if (filename.startsWith(".")) continue;
                
                fillFilesArray(allFiles, entry);
            }
    }

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
     * load a card.
     * </p>
     * 
     * @param inputStream
     *            the stream from which to load the card's information
     * 
     * @return the card loaded from the stream
     */
    protected final CardRules loadCard(final InputStream inputStream) {
        this.rulesReader.reset();

        InputStreamReader isr = new InputStreamReader(inputStream, this.charset);
        List<String> allLines = FileUtil.readAllLines(isr, true);
        
        CardReader.loadCard(allLines, this.rulesReader);
        return this.rulesReader.getCard();
        
    }

    /**
     * Returns the card lines - read from input stream and trimmed from spaces
     *
     * @param lines are input lines
     * @param rulesReader is used to fill CardPrinted characteristics
     * @param mapToFill is used to eliminate duplicates
     * @return the card
     */
    public static void loadCard(final Iterable<String> lines, final CardRulesReader rulesReader) {

        for (String line : lines) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            
            rulesReader.parseLine(line);
        }
    }

    public static Card readCard(final Iterable<String> lines)
    {
        final Card card = new Card();

        for (String line : lines) {
           
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            
            parseCardLine(card, line);
        } // while !End
        
        if (card.isInAlternateState()) {
            card.setState(CardCharactersticName.Original);
        }
        
        return card;
    }
    
    
    private static void parseCardLine(Card card, String line) {
        char firstCh = line.charAt(0);
        
        switch(firstCh) { // this is a simple state machine to gain some performance 
        case 'A':
            if (line.equals("ALTERNATE")) {
                CardCharactersticName mode;
                if (card.isFlipCard()) {
                    mode = CardCharactersticName.Flipped;
                } else if (card.isDoubleFaced()) {
                    mode = CardCharactersticName.Transformed;
                } else {
                    mode = card.isTransformable();
                }
                card.addAlternateState(mode);
                card.setState(mode);
            } else if (line.startsWith("A:")) {
                card.addIntrinsicAbility(line.substring(2));
            } else if (line.startsWith("AlternateMode")) {
                //System.out.println(card.getName());
                final CardCharactersticName value = CardCharactersticName.smartValueOf(line.substring("AlternateMode:".length()));
                if (value == CardCharactersticName.Flipped) {
                    card.setFlipCard(true);
                } else if (value == CardCharactersticName.Transformed) {
                    card.setDoubleFaced(true);
                } else {
                    card.setTransformable(value);
                }
            }
            break;
            
        case 'C': 
            if (line.startsWith("Colors")) {
                final String value = line.substring(7);
                final ArrayList<CardColor> newCols = new ArrayList<CardColor>();
                final CardColor newCol = new CardColor(card);
                for (final String col : value.split(",")) {
                    newCol.addToCardColor(col);
                }
                newCol.fixColorless();
                newCols.add(newCol);

                card.setColor(newCols);
                card.setCardColorsOverridden(true);
            }
            break;
            
        case 'K':
            if (line.startsWith("K:")) {
                final String value = line.substring(2);
                card.addIntrinsicKeyword(value);
            }
            break;
            
        case 'L':
            if (line.startsWith("Loyalty")) {
                final String[] splitStr = line.split(":");
                final int loyal = Integer.parseInt(splitStr[1]);
                card.setBaseLoyalty(loyal);
            }
            break;

        case 'M':
            if (line.startsWith("ManaCost")) {
                final String value = line.substring(9);
                // System.out.println(s);
                if (!"no cost".equals(value)) {
                    card.setManaCost(new CardManaCost(new ManaCostParser(value)));
                }
            }
            break;                    
            
        case 'N':
            if (line.startsWith("Name")) {
                final String value = line.substring(5);
                card.setName(value);
            }
            break;
            
        case 'P': 
            if (line.startsWith("PT")) {
                final String value = line.substring(3);
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
            }
            break;
            
        case 'R':
            if (line.startsWith("R:")) {
                card.addReplacementEffect(ReplacementHandler.parseReplacement(line.substring(2), card));
            }
            break;
            
        case 'S': 
            if (line.startsWith("S:")) {
                card.addStaticAbilityString(line.substring(2));
            } else if (line.startsWith("SVar")) {
                final String[] value = line.split(":", 3);
                card.setSVar(value[1], value[2]);
            } else if (line.startsWith("SetInfo")) {
                final String value = line.substring(8);
                card.addSet(new EditionInfo(value));
                // 8/18/11 11:08 PM
            }
            break;
            
        case 'T':
            if (line.startsWith("Types")) {
                CardReader.addTypes(card, line.substring(6));
            } else if (line.startsWith("Text")) {
                String value = line.substring(5);
                // if (!t.equals("no text"));
                if ("no text".equals(value)) {
                    value = "";
                }
                card.setText(value);
            } else if (line.startsWith("T:")) {
                card.addTrigger(TriggerHandler.parseTrigger(line.substring(2), card, true));
            }
            break;

    }
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
    protected final CardRules loadCard(final File pathToTxtFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(pathToTxtFile);
            return this.loadCard(fileInputStream);
        } catch (final FileNotFoundException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", pathToTxtFile.getAbsolutePath());
            throw new RuntimeException("CardReader : run error -- file exception -- filename is "
                    + pathToTxtFile.getPath(), ex);
        } finally {
            try {
                fileInputStream.close();
            } catch (final IOException ignored) {
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
    protected final CardRules loadCard(final ZipEntry entry) {
        InputStream zipInputStream = null;
        try {
            zipInputStream = this.zip.getInputStream(entry);
            return this.loadCard(zipInputStream);
        } catch (final IOException exn) {
            throw new RuntimeException(exn);
            // PM
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (final IOException ignored) {
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
}
