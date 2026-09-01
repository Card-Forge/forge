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

import com.google.common.io.Files;
import forge.card.CardRules;
import forge.card.ICardFace;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    private transient NavigableMap<String, ZipEntry> zipEntriesByBaseName;
    private transient NavigableMap<String, File> cardFilesByBaseName;
    private transient NavigableMap<String, ZipEntry> zipEntriesByCardName;
    private transient NavigableMap<String, File> cardFilesByCardName;
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

    boolean isLoadingCardsLazily() {
        return loadCardsLazily;
    }

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
            if ((c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                if (charIndex > 0 && chars[charIndex - 1] == '_') {
                    continue;
                }
                // Comma separator in numbers: "Borrowing 100,000 Arrows"
                if ((c == ',') && (charIndex > 0) && (chars[charIndex-1] >= '0' || chars[charIndex-1] <= '9'))
                    continue;
                c = '_';
            }
            chars[charIndex++] = c;
        }
        if (charIndex == 0) {
            return "";
        }
        if (chars[charIndex - 1] == '_') {
            charIndex--;
        }
        final int from = charIndex > 0 && chars[0] == '_' ? 1 : 0;
        return new String(chars, from, charIndex - from);
    }

    private synchronized NavigableMap<String, ZipEntry> getZipEntriesByBaseName() {
        if (zipEntriesByBaseName == null) {
            final TreeMap<String, ZipEntry> index = new TreeMap<>();
            for (ZipEntry entry : getZipEntries()) {
                index.put(transformName(baseName(entry.getName())), entry);
            }
            zipEntriesByBaseName = index;
        }
        return zipEntriesByBaseName;
    }

    private synchronized NavigableMap<String, File> getCardFilesByBaseName() {
        if (cardFilesByBaseName == null) {
            final TreeMap<String, File> index = new TreeMap<>();
            for (File file : collectCardFiles(new ArrayList<>(), cardsfolder)) {
                index.put(transformName(baseName(file.getName())), file);
            }
            cardFilesByBaseName = index;
        }
        return cardFilesByBaseName;
    }

    private static String baseName(String path) {
        final int slash = path.lastIndexOf('/');
        final String name = slash < 0 ? path : path.substring(slash + 1);
        return name.endsWith(CardStorageReader.CARD_FILE_DOT_EXTENSION)
                ? name.substring(0, name.length() - CardStorageReader.CARD_FILE_DOT_EXTENSION.length())
                : name;
    }

    private static <T> List<T> findCandidates(NavigableMap<String, T> index, String transformedName) {
        final List<T> candidates = new ArrayList<>();
        final T exact = index.get(transformedName);
        if (exact != null) {
            candidates.add(exact);
        }
        final String prefix = transformedName + "_";
        for (Map.Entry<String, T> e : index.tailMap(prefix, true).entrySet()) {
            if (!e.getKey().startsWith(prefix)) {
                break;
            }
            candidates.add(e.getValue());
        }
        return candidates;
    }

    private boolean rulesMatchName(CardRules rules, String transformedName) {
        if (transformedName.equals(transformName(StringUtils.stripAccents(rules.getPreInitName())))) {
            return true;
        }
        for (ICardFace face : rules.getAllFaces()) {
            if (face != null && transformedName.equals(transformName(StringUtils.stripAccents(face.getName())))) {
                return true;
            }
        }
        for (String faceName : rules.getPlaceholderFaceNames()) {
            if (transformedName.equals(transformName(StringUtils.stripAccents(faceName)))) {
                return true;
            }
        }
        return false;
    }

    public final CardRules attemptToLoadCard(String cardName) {
        final String transformedName = transformName(StringUtils.stripAccents(cardName));
        if (transformedName.isEmpty()) {
            return null;
        }

        final CardRules.Reader rulesReader = new CardRules.Reader();
        if (zip != null) {
            for (ZipEntry entry : findCandidates(getZipEntriesByBaseName(), transformedName)) {
                final CardRules rules = loadCard(rulesReader, entry);
                if (rulesMatchName(rules, transformedName)) {
                    return rules;
                }
            }
        }
        for (File file : findCandidates(getCardFilesByBaseName(), transformedName)) {
            final CardRules rules = loadCard(rulesReader, file);
            if (rulesMatchName(rules, transformedName)) {
                return rules;
            }
        }
        if (zip != null) {
            for (ZipEntry entry : findFaceCandidates(getZipEntriesByBaseName(), transformedName)) {
                final CardRules rules = loadCard(rulesReader, entry);
                if (rulesMatchName(rules, transformedName)) {
                    return rules;
                }
            }
        }
        for (File file : findFaceCandidates(getCardFilesByBaseName(), transformedName)) {
            final CardRules rules = loadCard(rulesReader, file);
            if (rulesMatchName(rules, transformedName)) {
                return rules;
            }
        }
        if (zip != null) {
            final ZipEntry entry = getZipEntriesByCardName().get(transformedName);
            if (entry != null) {
                return loadCard(rulesReader, entry);
            }
        }
        final File file = getCardFilesByCardName().get(transformedName);
        if (file != null) {
            return loadCard(rulesReader, file);
        }
        return null;
    }

    private static <T> List<T> findFaceCandidates(NavigableMap<String, T> index, String transformedName) {
        final String marker = "_" + transformedName;
        final List<T> candidates = new ArrayList<>();
        for (Map.Entry<String, T> e : index.entrySet()) {
            final String key = e.getKey();
            if (!key.startsWith(transformedName) && key.contains(marker)) {
                candidates.add(e.getValue());
            }
        }
        return candidates;
    }

    // Last-resort index for names that never appear in filenames
    private synchronized NavigableMap<String, ZipEntry> getZipEntriesByCardName() {
        if (zipEntriesByCardName == null) {
            zipEntriesByCardName = buildCardNameIndex(getZipEntriesByBaseName().values(), this::loadCard);
        }
        return zipEntriesByCardName;
    }

    private synchronized NavigableMap<String, File> getCardFilesByCardName() {
        if (cardFilesByCardName == null) {
            cardFilesByCardName = buildCardNameIndex(getCardFilesByBaseName().values(), this::loadCard);
        }
        return cardFilesByCardName;
    }

    private <T> NavigableMap<String, T> buildCardNameIndex(Collection<T> sources, BiFunction<CardRules.Reader, T, CardRules> loader) {
        final StopWatch sw = new StopWatch();
        sw.start();
        final NavigableMap<String, T> index = new TreeMap<>();
        final CardRules.Reader rulesReader = new CardRules.Reader();
        for (T source : sources) {
            final CardRules rules;
            try {
                rules = loader.apply(rulesReader, source);
            } catch (RuntimeException e) {
                continue; // a script that cannot be parsed cannot satisfy a lookup either
            }
            if (rules == null) {
                continue;
            }
            for (String name : allNamesOf(rules)) {
                final String key = transformName(StringUtils.stripAccents(name));
                if (!key.isEmpty()) {
                    index.putIfAbsent(key, source);
                }
            }
        }
        sw.stop();
        System.out.printf("Lazy card database: indexed %d card names from %d files in %d ms%n", index.size(), sources.size(), sw.getTime());
        return index;
    }

    private static List<String> allNamesOf(CardRules rules) {
        final List<String> names = new ArrayList<>();
        names.add(rules.getPreInitName());
        for (ICardFace face : rules.getAllFaces()) {
            if (face != null) {
                names.add(face.getName());
            }
        }
        final Collection<String> placeholderFaces = rules.getPlaceholderFaceNames();
        names.addAll(placeholderFaces);
        // getDisplayNameForVariant needs concrete faces, which placeholders lack.
        if (placeholderFaces.isEmpty() && rules.getSupportedFunctionalVariants() != null) {
            for (String variant : rules.getSupportedFunctionalVariants()) {
                names.add(rules.getDisplayNameForVariant(variant));
            }
        }
        return names;
    }

    public final Iterable<CardRules> loadCards() {
        if (loadCardsLazily) {
            return Collections.emptyList();
        }
        return readAllCards();
    }

    public final Iterable<CardRules> readAllCards() {
        final Localizer localizer = Localizer.getInstance();

        progressObserver.setOperationName(localizer.getMessage("splash.loading.examining-cards"), true);

        // Iterate through txt files or zip archive.
        // Report relevant numbers to progress monitor model.

        final Set<CardRules> result;
        result = new TreeSet<>(Comparator.comparing(CardRules::getNormalizedName, String.CASE_INSENSITIVE_ORDER));

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
            final long timeOnParse = sw.getTime(TimeUnit.SECONDS);
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
            final long timeOnParse = sw.getTime(TimeUnit.SECONDS);
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
            if (useThreadPool) {
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
        } catch (InterruptedException e) {
            // Propagate so callers don't cache a partially-loaded card set as if it were complete.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
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
            tasks.add(() -> {
                final List<CardRules> res = loadCardsInRangeFromZip(entries, from, till);
                cdl.countDown();
                progressObserver.report(maxParts - (int)cdl.getCount(), maxParts);
                return res;
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
            tasks.add(() -> {
                try {
                    final List<CardRules> res = loadCardsInRange(allFiles, from, till);
                    return res;
                } catch (Exception ex) {
                    throw ex;
                } finally {
                    // make sure to continue loading when using multiple threads
                    cdl.countDown();
                    progressObserver.report(maxParts - (int)cdl.getCount(), maxParts);
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
        try (InputStream fileInputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            reader.reset();
            final List<String> lines = readScript(fileInputStream);
            CardRules rules = reader.readCard(lines, Files.getNameWithoutExtension(file.getName()));
            rules.setPath(file.getPath());
            return rules;
        } catch (final FileNotFoundException ex) {
            throw new RuntimeException("CardReader : run error -- file not found: " + file.getPath(), ex);
        } catch (final Exception ex) {
            throw new RuntimeException("Error loading cardscript " + file.getName() + ". Please close Forge and resolve this.", ex);
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
        try (InputStream zipInputStream = this.zip.getInputStream(entry)) {
            rulesReader.reset();
            CardRules rules = rulesReader.readCard(readScript(zipInputStream), Files.getNameWithoutExtension(entry.getName()));
            rules.setPath(entry.getName());
            return rules;
        } catch (final IOException exn) {
            throw new RuntimeException(exn);
        }
    }

}
