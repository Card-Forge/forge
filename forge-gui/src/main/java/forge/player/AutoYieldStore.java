package forge.player;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class AutoYieldStore {
    public enum Tier { GAME, MATCH, SESSION }
    public enum TriggerDecision { ASK, ACCEPT, DECLINE }

    private final EnumMap<Tier, Set<String>> yieldsByTier = new EnumMap<>(Tier.class);
    private final Map<Integer, TriggerDecision> triggerDecisions = Maps.newTreeMap();
    private boolean disabled;

    public AutoYieldStore() {
        for (Tier t : Tier.values()) yieldsByTier.put(t, Sets.newHashSet());
    }

    public boolean shouldYield(Tier tier, String key) {
        return !disabled && yieldsByTier.get(tier).contains(key);
    }

    public void setYield(Tier tier, String key, boolean autoYield) {
        if (autoYield) yieldsByTier.get(tier).add(key);
        else yieldsByTier.get(tier).remove(key);
    }

    public Iterable<String> getYields(Tier tier) { return yieldsByTier.get(tier); }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }

    public TriggerDecision getTriggerDecision(int triggerId) {
        TriggerDecision d = triggerDecisions.get(triggerId);
        return d == null ? TriggerDecision.ASK : d;
    }

    public void setTriggerDecision(int triggerId, TriggerDecision decision) {
        if (decision == TriggerDecision.ASK) triggerDecisions.remove(triggerId);
        else triggerDecisions.put(triggerId, decision);
    }

    public void onGameEnd() { yieldsByTier.get(Tier.GAME).clear(); }

    public void onMatchEnd() {
        yieldsByTier.get(Tier.MATCH).clear();
        triggerDecisions.clear();
    }

    /** Strips the "Card (id=N): " prefix to derive the ability-scope key, or returns the input unchanged. */
    public static String abilitySuffix(String rawKey) {
        return rawKey.contains("): ") ? rawKey.substring(rawKey.indexOf("): ") + 3) : rawKey;
    }
}
