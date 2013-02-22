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
package forge.card.cardfactory;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.SwingUtilities;

import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.error.BugReporter;
import forge.gui.toolbox.FProgressBar;
import forge.util.FileUtil;
import forge.view.FView;

/**
 * <p>
 * CardReader class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardStorageReader {

    private static final String CARD_FILE_DOT_EXTENSION = ".txt";

    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "US-ASCII";

    /** Special value for estimatedFilesRemaining. */
    private static final int UNKNOWN_NUMBER_OF_FILES_REMAINING = -1;

    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;
    private transient CardRulesReader rulesReader;

    private transient Enumeration<? extends ZipEntry> zipEnum;

    private transient long estimatedFilesRemaining = CardStorageReader.UNKNOWN_NUMBER_OF_FILES_REMAINING;


    // 8/18/11 10:56 PM


    /**
     * <p>
     * Constructor for CardReader.
     * </p>
     * 
     * @param theCardsFolder
     *            indicates location of the cardsFolder
     * @param useZip
     *            if true, attempts to load cards from a zip file, if one
     *            exists.
     */
    public CardStorageReader(final File theCardsFolder, final boolean useZip) {

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

        this.charset = Charset.forName(CardStorageReader.DEFAULT_CHARSET_NAME);

    } // CardReader()


    /**
     * Starts reading cards into memory until the given card is found.
     * 
     * After that, we save our place in the list of cards (on disk) in case we
     * need to load more.
     * 
     * @return the Card or null if it was not found.
     */
    public final List<CardRules> loadCards() {

        List<CardRules> result = new ArrayList<CardRules>();
        final FProgressBar barProgress = FView.SINGLETON_INSTANCE.getSplash().getProgressBar();

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.
        if (this.zip == null) {
            List<File> allFiles = new ArrayList<File>();
            if (this.estimatedFilesRemaining == CardStorageReader.UNKNOWN_NUMBER_OF_FILES_REMAINING) {
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
                if (!cardTxtFile.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)) {
                    barProgress.increment();
                    continue;
                }

                //System.out.println(cardTxtFile.getName());
                result.add(this.loadCard(cardTxtFile));
                barProgress.increment();

            } // endfor
        } else {
            barProgress.setMaximum((int) this.estimatedFilesRemaining);
            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (this.zipEnum.hasMoreElements()) {
                entry = this.zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)) {
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
                if (filename.startsWith(".")) {
                    continue;
                }

                fillFilesArray(allFiles, entry);
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

        return rulesReader.readCard(allLines);
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
            BugReporter.reportException(ex, "File \"%s\" exception", pathToTxtFile.getAbsolutePath());
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
}
