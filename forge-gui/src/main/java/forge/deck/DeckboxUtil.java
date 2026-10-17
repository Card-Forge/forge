package forge.deck;

import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Helpers for local Deckbox deck folders under the user decks directory.
 */
public final class DeckboxUtil {
    /** Display name for the current bulk-sync provider (extend when more sites gain sync). */
    public static final String SYNC_PROVIDER_NAME = DeckboxDeckUrlProvider.PROVIDER_NAME;
    /** Sidecar written after a successful sync; used to detect local deck edits. */
    private static final String SYNC_MANIFEST_FILE = ".forge-deckbox-sync";

    private DeckboxUtil() {
    }

    public static String sanitizeSegment(final String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        // Match characters illegal in Windows paths (Deckbox often uses "::" in names).
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Windows also rejects trailing dots/spaces in file/folder names.
        value = value.replaceAll("[. ]+$", "");
        if (value.isEmpty() || ".".equals(value) || "..".equals(value)) {
            return "";
        }
        return value;
    }

    /**
     * Filename stem Forge writes for a deck name ({@link forge.deck.DeckBase#getBestFileName()}).
     * Used so remote sync comparison matches on-disk .dck names.
     */
    public static String toDeckFileKey(final String deckName) {
        if (deckName == null) {
            return "";
        }
        return deckName.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    public static String getSavedUsername() {
        return FModel.getPreferences().getPref(FPref.DECKBOX_USERNAME);
    }

    public static String getSavedFolder() {
        return FModel.getPreferences().getPref(FPref.DECKBOX_SUBDIRECTORY);
    }

    public static void saveSettings(final String username, final String folder) {
        final ForgePreferences prefs = FModel.getPreferences();
        prefs.setPref(FPref.DECKBOX_USERNAME, username == null ? "" : username.trim());
        prefs.setPref(FPref.DECKBOX_SUBDIRECTORY, folder == null ? "" : folder.trim());
        prefs.save();
    }

    /** Resolve {@code decks/deckbox/{username}} or null if username is blank. */
    public static File getUserDirectory(final String username) {
        final String user = sanitizeSegment(username);
        if (user.isEmpty()) {
            return null;
        }
        return new File(ForgeConstants.DECK_DECKBOX_DIR, user);
    }

    /** Local folder used for public decks that are not in a Deckbox folder. */
    public static String getRootFolderName(final String username) {
        final String user = sanitizeSegment(username);
        if (user.isEmpty()) {
            return "";
        }
        return user + "-root";
    }

    public static boolean isRootFolder(final String username, final String folder) {
        final String root = getRootFolderName(username);
        return !root.isEmpty() && root.equalsIgnoreCase(sanitizeSegment(folder));
    }

    /** Resolve {@code decks/deckbox/{username}/{folder}} or null if username/folder blank. */
    public static File getDeckDirectory(final String username, final String folder) {
        final File userDir = getUserDirectory(username);
        if (userDir == null) {
            return null;
        }
        final String folderName = sanitizeSegment(folder);
        if (folderName.isEmpty()) {
            return null;
        }
        return new File(userDir, folderName);
    }

    public static boolean ensureUserDirectory(final String username) {
        final File dir = getUserDirectory(username);
        if (dir == null) {
            return false;
        }
        return FileUtil.ensureDirectoryExists(dir);
    }

    /**
     * Ensure the username folder and selected Deckbox folder exist under the decks path.
     * @return true if the target deck directory exists after the call
     */
    public static boolean ensureDeckDirectory(final String username, final String folder) {
        final File dir = getDeckDirectory(username, folder);
        if (dir == null) {
            return false;
        }
        return FileUtil.ensureDirectoryExists(dir);
    }

    /** Local Deckbox folder names already synced for this username. */
    public static List<String> listLocalFolders(final String username) {
        final File userDir = getUserDirectory(username);
        if (userDir == null || !userDir.isDirectory()) {
            return Collections.emptyList();
        }
        final File[] children = userDir.listFiles(File::isDirectory);
        if (children == null || children.length == 0) {
            return Collections.emptyList();
        }
        final List<String> folders = new ArrayList<>();
        for (final File child : children) {
            folders.add(child.getName());
        }
        Collections.sort(folders, String.CASE_INSENSITIVE_ORDER);
        final String rootName = getRootFolderName(username);
        if (folders.removeIf(name -> name.equalsIgnoreCase(rootName))) {
            folders.add(0, rootName);
        }
        return folders;
    }

    /** Remove previously synced local folders so the next sync matches Deckbox. */
    public static void clearLocalFolders(final String username) {
        final File userDir = getUserDirectory(username);
        if (userDir == null || !userDir.isDirectory()) {
            return;
        }
        final File[] children = userDir.listFiles();
        if (children == null) {
            return;
        }
        for (final File child : children) {
            if (child.isDirectory()) {
                FileUtil.deleteDirectory(child);
            } else if (!child.delete()) {
                child.deleteOnExit();
            }
        }
    }

    private static boolean isSynced(final String username) {
        for (final String folder : listLocalFolders(username)) {
            if (folderHasDecks(username, folder)) {
                return true;
            }
        }
        return false;
    }

    private static boolean folderHasDecks(final String username, final String folder) {
        final File[] files = listDeckFiles(username, folder);
        return files != null && files.length > 0;
    }

    private static File[] listDeckFiles(final String username, final String folder) {
        final File dir = getDeckDirectory(username, folder);
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        return dir.listFiles((d, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".dck"));
    }

    public static IStorage<Deck> getStorage(final String username, final String folder) {
        final File dir = getDeckDirectory(username, folder);
        if (dir == null) {
            return null;
        }
        FileUtil.ensureDirectoryExists(dir);
        return new StorageImmediatelySerialized<>("Deckbox decks",
                new DeckStorage(dir, ForgeConstants.DECK_BASE_DIR), true);
    }

    public static Iterable<DeckProxy> getDecks(final String username, final String folder) {
        final IStorage<Deck> storage = getStorage(username, folder);
        if (storage == null) {
            return Collections.emptyList();
        }
        return DeckProxy.getAllDecksFromStorage("Deckbox", GameType.Constructed, storage);
    }

    public static boolean hasUsername(final String username) {
        return StringUtils.isNotBlank(sanitizeSegment(username));
    }

    /** True when this username already has at least one local synced .dck file. */
    public static boolean hasLocalDecks(final String username) {
        return isSynced(username);
    }

    /** Usernames that already have at least one local synced .dck file. */
    public static List<String> listSyncedUsernames() {
        final File root = new File(ForgeConstants.DECK_DECKBOX_DIR);
        if (!root.isDirectory()) {
            return Collections.emptyList();
        }
        final File[] children = root.listFiles(File::isDirectory);
        if (children == null || children.length == 0) {
            return Collections.emptyList();
        }
        final List<String> users = new ArrayList<>();
        for (final File child : children) {
            if (isSynced(child.getName())) {
                users.add(child.getName());
            }
        }
        Collections.sort(users, String.CASE_INSENSITIVE_ORDER);
        return users;
    }

    /**
     * Compare local decks for {@code username} against the last sync content snapshot
     * and the public Deckbox profile listing.
     * @return true when post-sync content hashes and remote listing both match local files
     */
    public static boolean isInSyncWithRemote(final String username) throws IOException {
        if (!hasUsername(username)) {
            return false;
        }
        // Local edits keep the same filename — check content hashes before hitting the network.
        if (!localContentMatchesSyncManifest(username)) {
            return false;
        }
        return remoteListingMatchesLocal(username);
    }

    private static boolean remoteListingMatchesLocal(final String username) throws IOException {
        final Map<String, List<DeckboxClient.RemoteDeckRef>> remote = DeckboxClient.listFolders(username);
        final Map<String, Set<String>> expected = new LinkedHashMap<>();
        for (final Map.Entry<String, List<DeckboxClient.RemoteDeckRef>> entry : remote.entrySet()) {
            final String folderKey = sanitizeSegment(entry.getKey());
            if (folderKey.isEmpty()) {
                continue;
            }
            final List<DeckboxClient.RemoteDeckRef> decks = entry.getValue();
            if ((decks == null || decks.isEmpty()) && !isRootFolder(username, folderKey)) {
                continue;
            }
            final Set<String> names = expected.computeIfAbsent(folderKey,
                    k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
            if (decks == null) {
                continue;
            }
            final List<String> assignedNames = new ArrayList<>();
            for (final DeckboxClient.RemoteDeckRef ref : decks) {
                if (ref.name() == null || ref.name().isBlank()) {
                    continue;
                }
                // Mirror DeckUrlLoader.getDeckName collision handling within the folder.
                String deckName = ref.name().trim();
                for (final String existing : assignedNames) {
                    if (existing.equals(deckName)) {
                        deckName = deckName + " " + ref.setId();
                        break;
                    }
                }
                assignedNames.add(deckName);
                final String fileKey = toDeckFileKey(deckName);
                if (!fileKey.isEmpty()) {
                    names.add(fileKey);
                }
            }
        }
        expected.putIfAbsent(getRootFolderName(username), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

        final Map<String, Set<String>> local = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (final String folder : listLocalFolders(username)) {
            local.put(folder, listLocalDeckNames(username, folder));
        }
        if (expected.size() != local.size()) {
            return false;
        }
        for (final Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            final Set<String> localNames = local.get(entry.getKey());
            if (localNames == null || !localNames.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Snapshot SHA-256 hashes of every local synced .dck for {@code username}.
     * Called after a successful Deckbox sync so later local edits can be detected.
     */
    public static void writeSyncManifest(final String username) throws IOException {
        final File userDir = getUserDirectory(username);
        if (userDir == null) {
            throw new IOException(Localizer.getInstance().getMessage("lblDeckboxCouldNotCreateDirectory"));
        }
        FileUtil.ensureDirectoryExists(userDir);
        final Map<String, String> hashes = collectLocalDeckHashes(username);
        final List<String> lines = new ArrayList<>();
        lines.add("# Forge Deckbox sync manifest v1");
        for (final Map.Entry<String, String> entry : hashes.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        FileUtil.writeFile(getSyncManifestFile(username), lines);
    }

    private static boolean localContentMatchesSyncManifest(final String username) throws IOException {
        final File manifestFile = getSyncManifestFile(username);
        if (manifestFile == null || !manifestFile.isFile()) {
            return false;
        }
        final Map<String, String> expected = readSyncManifest(manifestFile);
        if (expected.isEmpty()) {
            return false;
        }
        final Map<String, String> actual = collectLocalDeckHashes(username);
        return expected.equals(actual);
    }

    private static File getSyncManifestFile(final String username) {
        final File userDir = getUserDirectory(username);
        if (userDir == null) {
            return null;
        }
        return new File(userDir, SYNC_MANIFEST_FILE);
    }

    private static Map<String, String> readSyncManifest(final File manifestFile) {
        final Map<String, String> hashes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (final String line : FileUtil.readFile(manifestFile)) {
            if (line == null) {
                continue;
            }
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            final int sep = trimmed.indexOf('=');
            if (sep <= 0 || sep == trimmed.length() - 1) {
                continue;
            }
            hashes.put(trimmed.substring(0, sep), trimmed.substring(sep + 1));
        }
        return hashes;
    }

    private static Map<String, String> collectLocalDeckHashes(final String username) throws IOException {
        final Map<String, String> hashes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (final String folder : listLocalFolders(username)) {
            final File[] files = listDeckFiles(username, folder);
            if (files == null) {
                continue;
            }
            for (final File file : files) {
                final String stem = file.getName().substring(0, file.getName().length() - 4);
                hashes.put(folder + "/" + stem, sha256Hex(file));
            }
        }
        return hashes;
    }

    private static String sha256Hex(final File file) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Set<String> listLocalDeckNames(final String username, final String folder) {
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final File[] files = listDeckFiles(username, folder);
        if (files == null) {
            return names;
        }
        for (final File file : files) {
            final String filename = file.getName();
            names.add(filename.substring(0, filename.length() - 4));
        }
        return names;
    }
}
