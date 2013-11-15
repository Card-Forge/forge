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
import java.io.PrintWriter;
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

import forge.FThreads;
import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.error.BugReporter;
import forge.gui.toolbox.FProgressBar;
import forge.properties.NewConstants;
import forge.util.FileUtil;

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

    final private boolean useThreadPool = FThreads.isMultiCoreSystem();
    final private int NUMBER_OF_PARTS = 25;
    
    final private CountDownLatch cdl = new CountDownLatch(NUMBER_OF_PARTS);
    final private FProgressBar barProgress;
    
    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Charset charset;


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
    public CardStorageReader(String cardDataDir, final boolean useZip, FProgressBar barProgress) {
        this.barProgress = barProgress; 
        
        // These read data for lightweight classes.
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
                result.add(this.loadCard(rulesReader, cardTxtFile));
        }
        return result;
    }
    
    private final List<CardRules> loadCardsInRangeFromZip(final List<ZipEntry> files, int from, int to) {
    
        CardRulesReader rulesReader = new CardRulesReader();
        
        List<CardRules> result = new ArrayList<CardRules>();
        for(int i = from; i < to; i++) {
            ZipEntry ze = files.get(i);
            // if (ze.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION))  // already filtered!
            result.add(this.loadCard(rulesReader, ze));
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
        
        final List<Callable<List<CardRules>>> tasks;
        long estimatedFilesRemaining;
        
        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.
        if (this.zip == null) {
            final List<File> allFiles = new ArrayList<File>();
            fillFilesArray(allFiles, this.cardsfolder);
            estimatedFilesRemaining = allFiles.size();
            tasks = makeTaskListForFiles(allFiles);
        } else {
            
            estimatedFilesRemaining = this.zip.size();
            ZipEntry entry;
            List<ZipEntry> entries = new ArrayList<ZipEntry>();
            // zipEnum was initialized in the constructor.
            Enumeration<? extends ZipEntry> zipEnum = this.zip.entries();
            while (zipEnum.hasMoreElements()) {
                entry = zipEnum.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION))
                    continue;
                entries.add(entry);
                }

            tasks = makeTaskListForZip(entries);
        } // endif

        StopWatch sw = new StopWatch();
        sw.start();
        
        List<CardRules> res = executeLoadTask(tasks);
        
        sw.stop();
        final long timeOnParse = sw.getTime();
        System.out.printf("Read cards: %s %s in %d ms (%d parts) %s%n", estimatedFilesRemaining, zip == null? "files" : "archived files", timeOnParse, NUMBER_OF_PARTS, useThreadPool ? "using thread pool" : "in same thread");
        if ( null != barProgress )
            barProgress.setPercentMode(false);
        return res;
    } // loadCardsUntilYouFind(String)

    private List<CardRules> executeLoadTask(final List<Callable<List<CardRules>>> tasks) {
        List<CardRules> result = new ArrayList<CardRules>();

        try {
            if ( useThreadPool ) {
                final ExecutorService executor = FThreads.getComputingPool(0.5f);
                final List<Future<List<CardRules>>> parts = executor.invokeAll(tasks);
                executor.shutdown();
                cdl.await();
                for(Future<List<CardRules>> pp : parts) {
                    result.addAll(pp.get());
                }
            } else {
                for(Callable<List<CardRules>> c : tasks) {
                    result.addAll(c.call());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) { // this clause comes from non-threaded branch
            throw new RuntimeException(e);
        }
        
        return result;
    }
        
    private List<Callable<List<CardRules>>> makeTaskListForZip(final List<ZipEntry> entries) {
        int totalFiles = entries.size();
        int filesPerPart = totalFiles / NUMBER_OF_PARTS; 
        final List<Callable<List<CardRules>>> tasks = new ArrayList<Callable<List<CardRules>>>();
        for (int iPart = 0; iPart < NUMBER_OF_PARTS; iPart++) {
            final int from = iPart * filesPerPart;
            final int till = iPart == NUMBER_OF_PARTS - 1 ? totalFiles : from + filesPerPart;
            tasks.add(new Callable<List<CardRules>>() {
                @Override
                public List<CardRules> call() throws Exception{
                    List<CardRules> res = loadCardsInRangeFromZip(entries, from, till);
                    if ( null != barProgress )
                        barProgress.increment();
                    cdl.countDown();
                    return res;
                }
            });
        }
        return tasks;
    }

    private List<Callable<List<CardRules>>> makeTaskListForFiles(final List<File> allFiles) {
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
                    if ( null != barProgress )
                        barProgress.increment();
                    cdl.countDown();
                    return res;
                }
            });
        }
        return tasks;
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
                if (entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION))
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
    protected final CardRules loadCard(final CardRulesReader rulesReader, final ZipEntry entry) {
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

    //utility functions to parse all cards and perform certain actions on each card
    public static void parseAllCards(String[] args) {
    	if (args.length < 2) { return; }

    	int totalParsedCount = 0;
    	final List<List<String>> output = new ArrayList<List<String>>();
    	for (int i = 1; i < args.length; i++) {
    		output.add(new ArrayList<String>());
        }
    	final List<File> allFiles = new ArrayList<File>();
    	final CardRulesReader rulesReader = new CardRulesReader();
    	final CardStorageReader reader = new CardStorageReader(NewConstants.CARD_DATA_DIR, false, null);
    	reader.fillFilesArray(allFiles, reader.cardsfolder);
    	for (File file : allFiles) {
    		rulesReader.reset();

            InputStreamReader isr;
			try {
				isr = new InputStreamReader(new FileInputStream(file), reader.charset);
	            List<String> lines = FileUtil.readAllLines(isr, true);
	            CardRules rules = rulesReader.readCard(lines);
	            
	            System.out.println();
	            System.out.print(rules.getName()); //print each card here in case it gets stuck in utility
	            
	            totalParsedCount++;
	            for (int i = 1; i < args.length; i++) {
	            	switch (args[i]) {
	            	case "updateAbilityManaSymbols":
	            		updateAbilityManaSymbols(rules, lines, file, output.get(i - 1));
	            		break;
	            	}
	            }
			} catch (FileNotFoundException ex) {
			}
        }

		System.out.println();
		System.out.println();
        System.out.print("Total cards: " + totalParsedCount);

    	for (int i = 1; i < args.length; i++) {
    		List<String> singleOutput = output.get(i - 1);
    		System.out.println();
    		System.out.println();
    		System.out.println(args[i] + ":");
    		System.out.println();
    		for (String line : singleOutput) {
        		System.out.println(line);
    		}
    		System.out.println();
            System.out.print("Total cards: " + singleOutput.size());
        }
    }
    
    private static void updateAbilityManaSymbols(CardRules rules, List<String> lines, File file, List<String> output) {
		boolean updated = false;
    	String oracleText = rules.getOracleText();
        String[] sentences = oracleText.replace(rules.getName(), "CARDNAME").split("\\.|\\\\n|\\\"|\\(|\\)");
        for (String s : sentences) {
        	int idx = s.indexOf(":");
        	if (idx != -1) {
        		s = s.substring(idx + 1);
        	}
        	if (s.isEmpty()) { continue; }
        	try {
	        	String pattern = s.replaceAll("\\{([WUBRGSXYZ]|[0-9]+)\\}", "$1[ ]\\?").replaceAll("\\{C\\}", "Chaos");
	        	if (pattern.length() != s.length()) {
	        		pattern = "Description\\$(.*)" + pattern;
	        		s = "Description\\$$1" + s;
	        		for (int i = 0; i < lines.size(); i++) {
	        			String newLine = lines.get(i).replaceAll(pattern, s);
	        			if (newLine.length() != lines.get(i).length()) {
	        				updated = true;
	            			lines.set(i, newLine);
	        			}
	        		}
	        	}
        	}
        	catch (Exception ex) {
	            output.add("<Exception (" + rules.getName() + ") " + ex.getMessage() + ">");
        	}
        }
		if (updated) {
			try {
	            PrintWriter p = new PrintWriter(file);
	            for (int i = 0; i < lines.size(); i++) {
	            	if (i < lines.size() - 1) {
	            		p.println(lines.get(i));
	            	}
	            	else {
	            		p.print(lines.get(i));
	            	}
	            }
	            p.close();
	            output.add(rules.getName());
	        } catch (final Exception ex) {
	        }
		}
    }
}
