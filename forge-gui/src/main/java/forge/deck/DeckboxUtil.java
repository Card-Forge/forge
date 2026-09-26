package forge.deck;

import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helpers for local Deckbox deck folders under the user decks directory.
 */
public final class DeckboxUtil {
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
        // Prevent path traversal / nested separators in a single segment.
        value = value.replace('\\', '_').replace('/', '_');
        if (".".equals(value) || "..".equals(value)) {
            return "";
        }
        return value;
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
            if (hasLocalDecks(username, folder)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLocalDecks(final String username, final String folder) {
        final File dir = getDeckDirectory(username, folder);
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        final File[] files = dir.listFiles((d, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".dck"));
        return files != null && files.length > 0;
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
     * Compare local decks for {@code username} against the public Deckbox profile.
     * @return true when folder names and deck names match
     */
    public static boolean isInSyncWithRemote(final String username) throws IOException {
        if (!hasUsername(username)) {
            return false;
        }
        final Map<String, List<DeckboxClient.RemoteDeckRef>> remote = DeckboxClient.listFolders(username);
        final Map<String, Set<String>> expected = new LinkedHashMap<>();
        for (final Map.Entry<String, List<DeckboxClient.RemoteDeckRef>> entry : remote.entrySet()) {
            final List<DeckboxClient.RemoteDeckRef> decks = entry.getValue();
            if ((decks == null || decks.isEmpty()) && !isRootFolder(username, entry.getKey())) {
                continue;
            }
            final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            if (decks != null) {
                for (final DeckboxClient.RemoteDeckRef ref : decks) {
                    if (ref.name() != null && !ref.name().isBlank()) {
                        names.add(ref.name());
                    }
                }
            }
            expected.put(entry.getKey(), names);
        }
        expected.putIfAbsent(getRootFolderName(username), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

        final Map<String, Set<String>> local = new LinkedHashMap<>();
        for (final String folder : listLocalFolders(username)) {
            local.put(folder, listLocalDeckNames(username, folder));
        }
        if (expected.size() != local.size()) {
            return false;
        }
        for (final Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            Set<String> localNames = null;
            for (final Map.Entry<String, Set<String>> localEntry : local.entrySet()) {
                if (localEntry.getKey().equalsIgnoreCase(entry.getKey())) {
                    localNames = localEntry.getValue();
                    break;
                }
            }
            if (localNames == null || !localNames.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> listLocalDeckNames(final String username, final String folder) {
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final File dir = getDeckDirectory(username, folder);
        if (dir == null || !dir.isDirectory()) {
            return names;
        }
        final File[] files = dir.listFiles((d, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".dck"));
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
