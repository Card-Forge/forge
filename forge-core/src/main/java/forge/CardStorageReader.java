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
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.time.StopWatch;

import com.google.common.io.Files;

import forge.card.CardRules;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.ThreadUtil;

/**
 * <p>
 * CardStorageReader class.
 * </p>
 *
 * @author Forge
 * @version $Id: CardStorageReader.java 23742 2013-11-22 16:32:56Z Max mtg $
 */

public class CardStorageReader {
    public interface ProgressObserver{
        void setOperationName(String name, boolean usePercents);
        void report(int current, int total);

        // does nothing, used when they pass null instead of an instance
        ProgressObserver emptyObserver = new ProgressObserver() {
            @Override public void setOperationName(final String name, final boolean usePercents) {}
            @Override public void report(final int current, final int total) {}
        };
    }

    private static final String CARD_FILE_DOT_EXTENSION = ".txt";
    private static final String UPCOMING = "upcoming";

    /** Default charset when loading from files. */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    private final boolean useThreadPool = ThreadUtil.isMultiCoreSystem();
    private final static int NUMBER_OF_PARTS = 25;

    private final ProgressObserver progressObserver;

    private final boolean loadingTokens;
    private transient File cardsfolder;

    private transient ZipFile zip;
    private transient Map<String, ZipEntry> zipEntriesMap;
    private final transient Charset charset;

    private final boolean loadCardsLazily;

    public CardStorageReader(final String cardDataDir, final CardStorageReader.ProgressObserver progressObserver, boolean loadCardsLazily) {
        this.progressObserver = progressObserver != null ? progressObserver : CardStorageReader.ProgressObserver.emptyObserver;
        this.cardsfolder = new File(cardDataDir);

        this.loadingTokens = cardDataDir.contains("token");

        this.loadCardsLazily = loadCardsLazily;

        // These read data for lightweight classes.
        if (!cardsfolder.exists()) {
            throw new RuntimeException("CardReader : constructor error -- " + cardsfolder.getAbsolutePath() + " file/folder not found.");
        }

        if (!cardsfolder.isDirectory()) {
            throw new RuntimeException("CardReader : constructor error -- not a directory -- " + cardsfolder.getAbsolutePath());
        }

        final File zipFile = new File(cardsfolder, "cardsfolder.zip");

        if (zipFile.exists()) {
            try {
                this.zip = new ZipFile(zipFile);
            } catch (final Exception exn) {
                System.err.printf("Error reading zip file \"%s\": %s. Defaulting to txt files in \"%s\".%n", zipFile.getAbsolutePath(), exn, cardsfolder.getAbsolutePath());
            }
        }

        this.charset = Charset.forName(CardStorageReader.DEFAULT_CHARSET_NAME);

    } // CardReader()

    private List<CardRules> loadCardsInRange(final List<File> files, final int from, final int to) {
        final CardRules.Reader rulesReader = new CardRules.Reader();

        final List<CardRules> result = new ArrayList<>();
        for(int i = from; i < to; i++) {
            final File cardTxtFile = files.get(i);
            result.add(this.loadCard(rulesReader, cardTxtFile));
        }
        return result;
    }

    private List<CardRules> loadCardsInRangeFromZip(final List<ZipEntry> files, final int from, final int to) {
        final CardRules.Reader rulesReader = new CardRules.Reader();

        final List<CardRules> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            final ZipEntry ze = files.get(i);
            // if (ze.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION))  // already filtered!
            result.add(this.loadCard(rulesReader, ze));
        }
        return result;
    }

    // Note: This is custom coded for efficiency, since it allows
    // to do the relevant transformation in a single pass with just
    // a single char array allocation.
    private String transformName(String cardName) {
        char[] chars = new char[cardName.length()];
        int charIndex = 0;
        for (int i = 0; i < cardName.length(); i++) {
            char c = Character.toLowerCase(cardName.charAt(i));
            if (c == '\'') {
                continue;
            }
            if (c < 'a' || c > 'z') {
                if (charIndex > 0 && chars[charIndex - 1] == '_') {
                    continue;
                }
                c = '_';
            }
            chars[charIndex++] = c;
        }
        if (chars[charIndex - 1] == '_') {
            charIndex--;
        }
        return new String(chars, 0, charIndex);
    }
    
    private ZipEntry findZipEntryForCard(String transformedName) {
        if (zip == null) {
            return null;
        }

        if (zipEntriesMap == null) {
            zipEntriesMap = new HashMap<>();
            for (ZipEntry entry : getZipEntries()) {
                zipEntriesMap.put(entry.getName(), entry);
            }
        }

        transformedName = transformedName.charAt(0) + "/" + transformedName;
        ZipEntry entry = zipEntriesMap.get(transformedName + CardStorageReader.CARD_FILE_DOT_EXTENSION);
        if (entry == null) {
            // Double faced cards file naming convention currently has both names - so try to prefix match.
            // TODO: Consider changing the naming convention for DFCs.
            for (String fileName : zipEntriesMap.keySet()) {
                if (fileName.startsWith(transformedName)) {
                    entry = zipEntriesMap.get(fileName);
                    break;
                }
            }
        }
        return entry;
    }
    
    private File findFileForCard(String transformedName) {
        String folder = cardsfolder.getAbsolutePath() + "/" + transformedName.charAt(0);
        File file = new File(folder + "/" + transformedName + CardStorageReader.CARD_FILE_DOT_EXTENSION);
        if (!file.exists()) {
            file = null;
            // Double faced cards file naming convention currently has both names - so try to prefix match.
            // TODO: Consider changing the naming convention for DFCs.
            String[] fileNames = new File(folder).list();
            if (fileNames != null) {
                for (String fileName : new File(folder).list()) {
                    if (fileName.startsWith(transformedName)) {
                        file = new File(folder, fileName);
                        break;
                    }
                }
            }
        }
        return file;
    }

    public final CardRules attemptToLoadCard(String cardName, String setCode) {
        String transformedName = transformName(cardName);
        CardRules rules = null;

        // TODO: Should CardRules.Reader object be cached?
        ZipEntry entry = findZipEntryForCard(transformedName);
        if (entry != null) {
            rules = loadCard(new CardRules.Reader(), entry);
        } else {
            File file = findFileForCard(transformedName);
            if (file != null) {
                rules = loadCard(new CardRules.Reader(), file);
            }
        }

        return rules;
    }

    public final Iterable<CardRules> loadCards() {
        final Localizer localizer = Localizer.getInstance();

        progressObserver.setOperationName(localizer.getMessage("splash.loading.examining-cards"), true);

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.

        final Set<CardRules> result = new TreeSet<>(new Comparator<CardRules>() {
            @Override
            public int compare(final CardRules o1, final CardRules o2) {
                if (loadingTokens) {
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getNormalizedName(), o2.getNormalizedName());
                }
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
            }
        });

        if (loadCardsLazily) {
            return result;
        }
 
        final List<File> allFiles = collectCardFiles(new ArrayList<>(), this.cardsfolder);
        if (!allFiles.isEmpty()) {
            int fileParts = zip == null ? NUMBER_OF_PARTS : 1 + NUMBER_OF_PARTS / 3;
            if (allFiles.size() < fileParts * 100) {
                fileParts = Math.max(1, allFiles.size() / 100); // to avoid creation of many threads for a dozen of files
            }
            final CountDownLatch cdlFiles = new CountDownLatch(fileParts);
            final List<Callable<List<CardRules>>> taskFiles = makeTaskListForFiles(allFiles, cdlFiles);
            progressObserver.setOperationName(localizer.getMessage("splash.loading.cards-folders"), true);
            progressObserver.report(0, taskFiles.size());
            final StopWatch sw = new StopWatch();
            sw.start();
            executeLoadTask(result, taskFiles, cdlFiles);
            sw.stop();
            final long timeOnParse = sw.getTime();
            System.out.printf("Read cards: %s files in %d ms (%d parts) %s%n", allFiles.size(), timeOnParse, taskFiles.size(), useThreadPool ? "using thread pool" : "in same thread");
        }

        if (this.zip != null) {
            final CountDownLatch cdlZip = new CountDownLatch(NUMBER_OF_PARTS);
            List<Callable<List<CardRules>>> taskZip;
            taskZip = makeTaskListForZip(getZipEntries(), cdlZip);
            progressObserver.setOperationName(localizer.getMessage("splash.loading.cards-archive"), true);
            progressObserver.report(0, taskZip.size());
            final StopWatch sw = new StopWatch();
            sw.start();
            executeLoadTask(result, taskZip, cdlZip);
            sw.stop();
            final long timeOnParse = sw.getTime();
            System.out.printf("Read cards: %s archived files in %d ms (%d parts) %s%n", this.zip.size(), timeOnParse, taskZip.size(), useThreadPool ? "using thread pool" : "in same thread");
        }

        return result;
    }

    private List<ZipEntry> getZipEntries() {
        ZipEntry entry;
        final List<ZipEntry> entries = new ArrayList<>();
        // zipEnum was initialized in the constructor.
        final Enumeration<? extends ZipEntry> zipEnum = this.zip.entries();
        while (zipEnum.hasMoreElements()) {
            entry = zipEnum.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)) {
                continue;
            }
            entries.add(entry);
        }
        return entries;
    }

    private void executeLoadTask(final Collection<CardRules> result, final List<Callable<List<CardRules>>> tasks, final CountDownLatch cdl) {
        try {
            if ( useThreadPool ) {
                final ExecutorService executor = ThreadUtil.getComputingPool(0.5f);
                final List<Future<List<CardRules>>> parts = executor.invokeAll(tasks);
                executor.shutdown();
                cdl.await();
                for (final Future<List<CardRules>> pp : parts) {
                    result.addAll(pp.get());
                }
            } else {
                for (final Callable<List<CardRules>> c : tasks) {
                    result.addAll(c.call());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (final Exception e) { // this clause comes from non-threaded branch
            throw new RuntimeException(e);
        }
    }

    private List<Callable<List<CardRules>>> makeTaskListForZip(final List<ZipEntry> entries, final CountDownLatch cdl) {
        final int totalFiles = entries.size();
        final int maxParts = (int) cdl.getCount();
        final int filesPerPart = totalFiles / maxParts;
        final List<Callable<List<CardRules>>> tasks = new ArrayList<>();
        for (int iPart = 0; iPart < maxParts; iPart++) {
            final int from = iPart * filesPerPart;
            final int till = iPart == maxParts - 1 ? totalFiles : from + filesPerPart;
            tasks.add(new Callable<List<CardRules>>() {
                @Override
                public List<CardRules> call() throws Exception{
                    final List<CardRules> res = loadCardsInRangeFromZip(entries, from, till);
                    cdl.countDown();
                    progressObserver.report(maxParts - (int)cdl.getCount(), maxParts);
                    return res;
                }
            });
        }
        return tasks;
    }

    private List<Callable<List<CardRules>>> makeTaskListForFiles(final List<File> allFiles, final CountDownLatch cdl) {
        final int totalFiles = allFiles.size();
        final int maxParts = (int) cdl.getCount();
        final int filesPerPart = totalFiles / maxParts;
        final List<Callable<List<CardRules>>> tasks = new ArrayList<>();
        for (int iPart = 0; iPart < maxParts; iPart++) {
            final int from = iPart * filesPerPart;
            final int till = iPart == maxParts - 1 ? totalFiles : from + filesPerPart;
            tasks.add(new Callable<List<CardRules>>() {
                @Override
                public List<CardRules> call() throws Exception{
                    final List<CardRules> res = loadCardsInRange(allFiles, from, till);
                    cdl.countDown();
                    progressObserver.report(maxParts - (int)cdl.getCount(), maxParts);
                    return res;
                }
            });
        }
        return tasks;
    }

    public static List<File> collectCardFiles(final List<File> accumulator, final File startDir) {
        final String[] list = startDir.list();
        for (final String filename : list) {
            final File entry = new File(startDir, filename);

            if (!entry.isDirectory()) {
                if (entry.getName().endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)) {
                    accumulator.add(entry);
                }
                continue;
            }
            if (filename.startsWith(".")) {
                continue;
            }

            if (filename.equalsIgnoreCase(CardStorageReader.UPCOMING) && !BuildInfo.isDevelopmentVersion()) {
                // If upcoming folder exits, only load these cards on development builds
                continue;
            }

            collectCardFiles(accumulator, entry);
        }
        return accumulator;
    }

    private List<String> readScript(final InputStream inputStream) {
        return FileUtil.readAllLines(new InputStreamReader(inputStream, this.charset), true);
    }

    /**
     * Load a card from a txt file.
     *
     * @return a new Card instance
     */
    protected final CardRules loadCard(final CardRules.Reader reader, final File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            reader.reset();
            final List<String> lines = readScript(fileInputStream);
            return reader.readCard(lines, Files.getNameWithoutExtension(file.getName()));
        } catch (final FileNotFoundException ex) {
            throw new RuntimeException("CardReader : run error -- file not found: " + file.getPath(), ex);
        } catch (final Exception ex) {
            System.out.println("Error loading cardscript " + file.getName() + ". Please close Forge and resolve this.");
            throw ex;
        } finally {
            try {
                assert fileInputStream != null;
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
    protected final CardRules loadCard(final CardRules.Reader rulesReader, final ZipEntry entry) {
        InputStream zipInputStream = null;
        try {
            zipInputStream = this.zip.getInputStream(entry);
            rulesReader.reset();

            return rulesReader.readCard(readScript(zipInputStream), Files.getNameWithoutExtension(entry.getName()));
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
