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
    private final EnumMap<Tier, Map<String, TriggerDecision>> triggerDecisionsByTier = new EnumMap<>(Tier.class);
    private boolean disabled;
    private boolean triggerDecisionsDisabled;

    public AutoYieldStore() {
        for (Tier t : Tier.values()) {
            yieldsByTier.put(t, Sets.newHashSet());
            triggerDecisionsByTier.put(t, Maps.newHashMap());
        }
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
    public boolean isTriggerDecisionsDisabled() { return triggerDecisionsDisabled; }
    public void setTriggerDecisionsDisabled(boolean disabled) { this.triggerDecisionsDisabled = disabled; }

    public TriggerDecision getTriggerDecision(Tier tier, String key) {
        TriggerDecision d = triggerDecisionsByTier.get(tier).get(key);
        return d == null ? TriggerDecision.ASK : d;
    }

    public void setTriggerDecision(Tier tier, String key, TriggerDecision decision) {
        if (decision == TriggerDecision.ASK) triggerDecisionsByTier.get(tier).remove(key);
        else triggerDecisionsByTier.get(tier).put(key, decision);
    }

    public Iterable<Map.Entry<String, TriggerDecision>> getAutoTriggers(Tier tier) {
        return triggerDecisionsByTier.get(tier).entrySet();
    }

    public void onGameEnd(boolean matchOver) {
        yieldsByTier.get(Tier.GAME).clear();
        triggerDecisionsByTier.get(Tier.GAME).clear();
        if (matchOver) {
            yieldsByTier.get(Tier.MATCH).clear();
            triggerDecisionsByTier.get(Tier.MATCH).clear();
        }
    }

    /** Wipe all yields, trigger decisions, and the disabled flags — used to reseed the cache from a client snapshot. */
    public void clear() {
        for (Set<String> set : yieldsByTier.values()) set.clear();
        for (Map<String, TriggerDecision> map : triggerDecisionsByTier.values()) map.clear();
        disabled = false;
        triggerDecisionsDisabled = false;
    }

    /** Strips the "Card (id=N): " prefix to derive the ability-scope key, or returns the input unchanged. */
    public static String abilitySuffix(String rawKey) {
        return rawKey.contains("): ") ? rawKey.substring(rawKey.indexOf("): ") + 3) : rawKey;
    }
}
