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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.time.StopWatch;

import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.control.FControl;
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

    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;
    private transient CardRulesReader rulesReader;


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
    public CardStorageReader(String cardDataDir, final boolean useZip) {

        // These read data for lightweight classes.
        this.rulesReader = new CardRulesReader();
        
        File theCardsFolder = new File(cardDataDir);

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
                System.err.printf("Error reading zip file \"%s\": %s. Defaulting to txt files in \"%s\".%n", zipFile.getAbsolutePath(), exn, theCardsFolder.getAbsolutePath());
            }
         }

        this.charset = Charset.forName(CardStorageReader.DEFAULT_CHARSET_NAME);

    } // CardReader()

    private final List<CardRules> loadCardsInRange(final List<File> files, int from, int to) {
        
        CardRulesReader rulesReader = new CardRulesReader();
        
        List<CardRules> result = new ArrayList<CardRules>();
        for(int i = from; i < to; i++) {
            File cardTxtFile = files.get(i);
            if (cardTxtFile.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION))
                result.add(this.loadCard(rulesReader, cardTxtFile));
        }
        return result;
    }
    
    
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
            result = loadAllCardsFromFolder(barProgress);
        } else {
            
            Enumeration<? extends ZipEntry> zipEnum = this.zip.entries();
            int estimatedFilesRemaining = this.zip.size();
            
            barProgress.setMaximum(estimatedFilesRemaining);
            ZipEntry entry;

            // zipEnum was initialized in the constructor.
            while (zipEnum.hasMoreElements()) {
                entry = zipEnum.nextElement();

                if (entry.isDirectory() || !entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)) {
                    barProgress.increment();
                    continue;
                }

                result.add(this.loadCard(entry));
                barProgress.increment();
            }
        } // endif

        barProgress.setPercentMode(false);

        return result;
    } // loadCardsUntilYouFind(String)

    private List<CardRules> loadAllCardsFromFolder(final FProgressBar barProgress) {
        List<CardRules> result = new ArrayList<CardRules>();
        
        StopWatch sw = new StopWatch();
        sw.start();
        final List<File> allFiles = new ArrayList<File>();
        
        fillFilesArray(allFiles, this.cardsfolder);
        long estimatedFilesRemaining = allFiles.size();
        

        final int NUMBER_OF_PARTS = 20;
        sw.split();
        if (barProgress != null) {
            barProgress.setMaximum(NUMBER_OF_PARTS);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    barProgress.setPercentMode(true);
                    barProgress.setDescription("Loading card data: ");
                }
            });
        }

        final CountDownLatch cdl = new CountDownLatch(NUMBER_OF_PARTS);
        int totalFiles = allFiles.size();
        int filesPerPart = totalFiles / NUMBER_OF_PARTS; 
        final List<Callable<List<CardRules>>> tasks = new ArrayList<Callable<List<CardRules>>>();
        for (int iPart = 0; iPart < NUMBER_OF_PARTS; iPart++) {
            final int from = iPart * filesPerPart;
            final int till = iPart == NUMBER_OF_PARTS - 1 ? totalFiles : from + filesPerPart;
            tasks.add(new Callable<List<CardRules>>() {
                @Override
                public List<CardRules> call() throws Exception{
                    List<CardRules> res = loadCardsInRange(allFiles, from, till);
                    barProgress.increment();
                    cdl.countDown();
                    return res;
                }
            });
        }
            
        try {
            final ExecutorService executor = FControl.getComputingPool(0.5f);
            final List<Future<List<CardRules>>> parts = executor.invokeAll(tasks);
            executor.shutdown();
            cdl.await();
            for(Future<List<CardRules>> pp : parts) {
                result.addAll(pp.get());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        
        long fs = sw.getSplitTime();
        sw.stop();
        
        System.out.println("Processed " + estimatedFilesRemaining + " file objects in " + (sw.getTime() - fs) + " ms, apart from that folder scan took " + fs + " ms.");
        return result;
    }

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
    protected final CardRules loadCard(CardRulesReader reader, final InputStream inputStream) {
        reader.reset();

        InputStreamReader isr = new InputStreamReader(inputStream, this.charset);
        List<String> allLines = FileUtil.readAllLines(isr, true);

        return reader.readCard(allLines);
    }

    /**
     * Load a card from a txt file.
     * 
     * @param pathToTxtFile
     *            the full or relative path to the file to load
     * 
     * @return a new Card instance
     */
    protected final CardRules loadCard(final CardRulesReader reader, final File pathToTxtFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(pathToTxtFile);
            return this.loadCard(reader, fileInputStream);
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
            return this.loadCard(rulesReader, zipInputStream);
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
