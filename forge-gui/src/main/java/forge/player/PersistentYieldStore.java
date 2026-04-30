package forge.player;

import forge.localinstance.properties.ForgeConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * File-backed singleton holding INSTALL-tier auto-yield keys. Shared across all
 * local controllers on this install of Forge. Trigger accept/decline decisions
 * are not persisted: trigger IDs are not stable across games.
 *
 * Key stability: ability keys derive from SpellAbility.toUnsuppressedString() and
 * are not guaranteed stable across Forge versions or card text edits. A stale
 * persisted key silently fails to match — acceptable; users can re-add as needed.
 */
public class PersistentYieldStore {
    private static final String YIELD_PREFIX = "yield.";
    private static volatile PersistentYieldStore instance;

    public static PersistentYieldStore get() {
        PersistentYieldStore local = instance;
        if (local != null) return local;
        synchronized (PersistentYieldStore.class) {
            if (instance == null) {
                instance = new PersistentYieldStore(Paths.get(ForgeConstants.USER_DIR, "auto-yields.dat"));
            }
            return instance;
        }
    }

    private final Path persistFile;
    private final Set<String> yields = new HashSet<>();

    PersistentYieldStore(Path persistFile) {
        this.persistFile = persistFile;
        load();
    }

    public synchronized boolean contains(String key) { return yields.contains(key); }

    public synchronized void setYield(String key, boolean autoYield) {
        boolean changed = autoYield ? yields.add(key) : yields.remove(key);
        if (changed) save();
    }

    public synchronized Iterable<String> getYields() {
        return Collections.unmodifiableSet(new HashSet<>(yields));
    }

    private void load() {
        if (persistFile == null || !Files.exists(persistFile)) return;
        try (BufferedReader r = Files.newBufferedReader(persistFile)) {
            Properties p = new Properties();
            p.load(r);
            for (String name : p.stringPropertyNames()) {
                if (name.startsWith(YIELD_PREFIX) && "true".equals(p.getProperty(name))) {
                    yields.add(URLDecoder.decode(name.substring(YIELD_PREFIX.length()), "UTF-8"));
                }
            }
        } catch (IOException ignored) {
            // UnsupportedEncodingException is unreachable ("UTF-8" is always supported).
            // IOException: stale or corrupt file — treat as empty; next save overwrites.
        }
    }

    private void save() {
        if (persistFile == null) return;
        try {
            Properties p = new Properties();
            for (String key : yields) {
                p.setProperty(YIELD_PREFIX + URLEncoder.encode(key, "UTF-8"), "true");
            }
            Path parent = persistFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter w = Files.newBufferedWriter(persistFile)) {
                p.store(w, "Forge auto-yield persistent store");
            }
        } catch (IOException ignored) {
            // Best-effort persistence; in-memory state remains correct for this session.
        }
    }
}
