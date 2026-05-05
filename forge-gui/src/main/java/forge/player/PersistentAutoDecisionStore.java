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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * File-backed singleton for INSTALL-tier auto-yield keys and trigger decisions.
 * Shared across all local controllers on this install of Forge.
 *
 * Key stability: entries are keyed by SpellAbility.yieldKey() (which for
 * triggered abilities is host-name + ": " + Trigger.toString()) and are not
 * guaranteed stable across Forge versions or card text edits. Stale entries
 * silently fail to match — acceptable; users can re-add as needed.
 *
 * File schema (auto-yields.dat) — additive, backwards-compatible with old yield-only files:
 *   yield.<urlencoded-key>=true
 *   trigger.accept.<urlencoded-key>=true
 *   trigger.decline.<urlencoded-key>=true
 */
public class PersistentAutoDecisionStore {
    private static final String YIELD_PREFIX = "yield.";
    private static final String TRIGGER_ACCEPT_PREFIX = "trigger.accept.";
    private static final String TRIGGER_DECLINE_PREFIX = "trigger.decline.";

    private static volatile PersistentAutoDecisionStore instance;

    public static PersistentAutoDecisionStore get() {
        PersistentAutoDecisionStore local = instance;
        if (local != null) return local;
        synchronized (PersistentAutoDecisionStore.class) {
            if (instance == null) {
                instance = new PersistentAutoDecisionStore(Paths.get(ForgeConstants.USER_DIR, "auto-yields.dat"));
            }
            return instance;
        }
    }

    private final Path persistFile;
    private final Set<String> yields = new HashSet<>();
    private final Map<String, AutoYieldStore.TriggerDecision> triggerDecisions = new HashMap<>();

    PersistentAutoDecisionStore(Path persistFile) {
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

    public synchronized AutoYieldStore.TriggerDecision getTriggerDecision(String key) {
        AutoYieldStore.TriggerDecision d = triggerDecisions.get(key);
        return d == null ? AutoYieldStore.TriggerDecision.ASK : d;
    }

    public synchronized void setTriggerDecision(String key, AutoYieldStore.TriggerDecision decision) {
        boolean changed;
        if (decision == AutoYieldStore.TriggerDecision.ASK) {
            changed = triggerDecisions.remove(key) != null;
        } else {
            changed = decision != triggerDecisions.put(key, decision);
        }
        if (changed) save();
    }

    public synchronized Iterable<Map.Entry<String, AutoYieldStore.TriggerDecision>> getAutoTriggers() {
        return new HashMap<>(triggerDecisions).entrySet();
    }

    private void load() {
        if (persistFile == null || !Files.exists(persistFile)) return;
        try (BufferedReader r = Files.newBufferedReader(persistFile)) {
            Properties p = new Properties();
            p.load(r);
            for (String name : p.stringPropertyNames()) {
                String value = p.getProperty(name);
                if (name.startsWith(YIELD_PREFIX) && "true".equals(value)) {
                    yields.add(URLDecoder.decode(name.substring(YIELD_PREFIX.length()), "UTF-8"));
                } else if (name.startsWith(TRIGGER_ACCEPT_PREFIX) && "true".equals(value)) {
                    triggerDecisions.put(URLDecoder.decode(name.substring(TRIGGER_ACCEPT_PREFIX.length()), "UTF-8"),
                            AutoYieldStore.TriggerDecision.ACCEPT);
                } else if (name.startsWith(TRIGGER_DECLINE_PREFIX) && "true".equals(value)) {
                    triggerDecisions.put(URLDecoder.decode(name.substring(TRIGGER_DECLINE_PREFIX.length()), "UTF-8"),
                            AutoYieldStore.TriggerDecision.DECLINE);
                }
            }
        } catch (IOException ignored) {
            // Stale/corrupt file — treat as empty; next save overwrites.
        }
    }

    private void save() {
        if (persistFile == null) return;
        try {
            Properties p = new Properties();
            for (String key : yields) {
                p.setProperty(YIELD_PREFIX + URLEncoder.encode(key, "UTF-8"), "true");
            }
            for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : triggerDecisions.entrySet()) {
                String prefix = e.getValue() == AutoYieldStore.TriggerDecision.ACCEPT
                        ? TRIGGER_ACCEPT_PREFIX : TRIGGER_DECLINE_PREFIX;
                p.setProperty(prefix + URLEncoder.encode(e.getKey(), "UTF-8"), "true");
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
