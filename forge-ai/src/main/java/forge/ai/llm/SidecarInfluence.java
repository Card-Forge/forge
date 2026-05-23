package forge.ai.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.llm.RecognitionResult.HandValuation;
import forge.ai.llm.RecognitionResult.EarlyGamePlan;
import forge.ai.llm.RecognitionResult.ManaPlan;
import forge.ai.llm.RecognitionResult.OpponentHandGuess;
import forge.ai.llm.RecognitionResult.PilotingAdvice;
import forge.ai.llm.RecognitionResult.RoleAssessment;
import forge.ai.llm.RecognitionResult.TargetPriority;

/**
 * Stores the latest structured action recommendations from the LLM sidecar
 * and applies second-pass personality weighting to adjust action percentages.
 *
 * <p>This is the bridge between the asynchronous sidecar response and the
 * heuristic AI's decision points. It is thread-safe and fail-soft: if no
 * sidecar data is available, it simply returns empty results and the
 * heuristic AI continues unchanged.</p>
 *
 * <p>Personality weighting factors are defined in {@link AiProps} and adjust
 * action percentages based on the AI's configured personality profile.</p>
 */
public final class SidecarInfluence {

    /** Latest actions from the sidecar, keyed by action_type. */
    private volatile Map<String, ActionScore> latestActions = Map.of();
    /** Multiple PLAY_SPELL recommendations in priority order. */
    private volatile List<ActionScore> latestPlaySpells = List.of();
    /** Multiple PLAY_LAND recommendations in priority order. */
    private volatile List<ActionScore> latestPlayLands = List.of();
    /** Latest hand valuations, keyed by lower-case card name. */
    private volatile Map<String, HandValuation> latestHandValues = Map.of();
    /** Latest opponent-hand inferences sorted by probability desc. */
    private volatile List<OpponentHandGuess> latestOpponentHand = List.of();
    /** Latest target priorities. Key is lower-case spell name, "" for generic. */
    private volatile Map<String, TargetPriority> latestTargetPriorities = Map.of();
    /** Latest role assessment, or null. */
    private volatile RoleAssessment latestRole = null;
    /** Latest opening/rolling plan, or null. */
    private volatile EarlyGamePlan latestEarlyGamePlan = null;
    /** Latest per-action manabase plan (fetch/land/utility), or null. */
    private volatile ManaPlan latestManaPlan = null;

    /** The AI controller, used to read personality properties. */
    private final AiController ai;

    private volatile boolean enabled = false;

    /**
     * Latest in-flight /recognize future, pushed by the observer when it fires
     * a call. {@link #awaitLatest(long)} blocks on this so decision points can
     * give the LLM a budget to finish before the heuristic AI commits.
     */
    private volatile CompletableFuture<?> latestCall = CompletableFuture.completedFuture(null);

    public SidecarInfluence(final AiController ai) {
        this.ai = ai;
    }

    /** Called by the observer when it fires a /recognize call. */
    public void setLatestCall(final CompletableFuture<?> future) {
        if (future != null) {
            latestCall = future;
        }
    }

    /**
     * Block up to {@code timeoutMs} for the most recent /recognize to settle.
     * Used at key decision points (mulligan, start of main, declare-attackers)
     * so the AI gives the sidecar a budget to deliver fresh advice before
     * committing. Returns immediately if no call is in flight or it's already
     * done.
     *
     * @return true if the call completed within the budget, false on timeout.
     */
    public boolean awaitLatest(final long timeoutMs) {
        if (timeoutMs <= 0) {
            return latestCall.isDone();
        }
        final CompletableFuture<?> f = latestCall;
        if (f.isDone()) {
            return true;
        }
        try {
            f.get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (final TimeoutException ex) {
            Logger.debug("SidecarInfluence: awaitLatest timed out after %d ms", timeoutMs);
            return false;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final Exception ex) {
            // Future never throws (fail-soft); any unexpected exception is harmless here.
            return true;
        }
    }

    /**
     * Called when a new sidecar result arrives. Stores the actions and applies
     * personality weighting (second pass).
     */
    public void updateFromResult(final RecognitionResult result) {
        if (result == null || result.piloting() == null) {
            return;
        }
        final PilotingAdvice piloting = result.piloting();
        final List<ActionScore> raw = piloting.actions();
        if (raw == null || raw.isEmpty()) {
            latestActions = Map.of();
            latestPlaySpells = List.of();
            latestPlayLands = List.of();
        } else {
            // For single-instance action types (BLOCK, ATTACK, PASS, MULLIGAN,
            // ACTIVATE_ABILITY) we keep just the best entry. For PLAY_SPELL and
            // PLAY_LAND we keep them all so the Java side can fall through if
            // the first pick is unplayable.
            final Map<String, ActionScore> weighted = new ConcurrentHashMap<>();
            final List<ActionScore> spells = new ArrayList<>();
            final List<ActionScore> lands = new ArrayList<>();
            for (final ActionScore action : raw) {
                final double adjusted = applyPersonalityWeight(action);
                final ActionScore weightedAction = new ActionScore(
                        action.actionType(),
                        action.target(),
                        action.targets(),
                        adjusted,
                        action.reasoning()
                );
                if ("PLAY_SPELL".equals(action.actionType())) {
                    spells.add(weightedAction);
                } else if ("PLAY_LAND".equals(action.actionType())) {
                    lands.add(weightedAction);
                }
                // bestAction(...) consumers see the highest-percentage entry
                // per action type; preserve current behavior by overwriting
                // only when the new score is higher.
                final ActionScore prev = weighted.get(action.actionType());
                if (prev == null || adjusted > prev.percentage()) {
                    weighted.put(action.actionType(), weightedAction);
                }
            }
            spells.sort((a, b) -> Double.compare(b.percentage(), a.percentage()));
            lands.sort((a, b) -> Double.compare(b.percentage(), a.percentage()));
            latestActions = Collections.unmodifiableMap(weighted);
            latestPlaySpells = Collections.unmodifiableList(spells);
            latestPlayLands = Collections.unmodifiableList(lands);
        }

        // Role / hand / opponent / targeting payloads (v4). All nullable on
        // older sidecars or when state is too thin to compute.
        latestRole = piloting.role();
        latestEarlyGamePlan = piloting.earlyGamePlan();
        latestManaPlan = piloting.manaPlan();
        final List<HandValuation> hvList = piloting.handValues();
        if (hvList == null || hvList.isEmpty()) {
            latestHandValues = Map.of();
        } else {
            final Map<String, HandValuation> hv = new HashMap<>();
            for (final HandValuation v : hvList) {
                if (v != null && v.card() != null) {
                    hv.put(v.card().toLowerCase(Locale.ROOT), v);
                }
            }
            latestHandValues = Collections.unmodifiableMap(hv);
        }
        final List<OpponentHandGuess> oh = piloting.opponentHand();
        if (oh == null || oh.isEmpty()) {
            latestOpponentHand = List.of();
        } else {
            final List<OpponentHandGuess> sorted = new ArrayList<>(oh);
            sorted.sort((a, b) -> Double.compare(b.probability(), a.probability()));
            latestOpponentHand = Collections.unmodifiableList(sorted);
        }
        final List<TargetPriority> tps = piloting.targetPriorities();
        if (tps == null || tps.isEmpty()) {
            latestTargetPriorities = Map.of();
        } else {
            final Map<String, TargetPriority> tpMap = new HashMap<>();
            for (final TargetPriority tp : tps) {
                if (tp == null) continue;
                final String key = tp.spell() == null ? "" : tp.spell().toLowerCase(Locale.ROOT);
                tpMap.put(key, tp);
            }
            latestTargetPriorities = Collections.unmodifiableMap(tpMap);
        }

        enabled = true;
        Logger.debug("SidecarInfluence: updated actions=%d, hand=%d, opp=%d, targets=%d, role=%s, plan=%s",
                latestActions.size(), latestHandValues.size(),
                latestOpponentHand.size(), latestTargetPriorities.size(),
                latestRole != null ? latestRole.aiRole() : "none",
                latestEarlyGamePlan != null ? latestEarlyGamePlan.decision() : "none");
    }

    /**
     * Apply personality weighting as a soft DOWN-weight only.
     *
     * <p>Historically this was a two-sided multiplier (boost favored actions,
     * dampen disfavored ones). That compounded with the
     * {@link AiProps#SIDECAR_BIAS_ATTACK}/...{@code _BLOCK}/...{@code _PASS}
     * multipliers applied at the decision sites — an aggressive AI's ATTACK
     * could get boosted twice. Bias is already the user-tunable knob per
     * profile, so personality is now restricted to DOWN-weighting
     * actions that conflict with the personality. That preserves the
     * "aggressive AI doesn't sandbag" intent without re-boosting actions the
     * profile already favors.</p>
     *
     * <p>{@link AiProps#SIDECAR_PERSONALITY_WEIGHT} = 0 disables personality
     * entirely. Higher values mean stronger dampening (cap 0.5x).</p>
     */
    private double applyPersonalityWeight(final ActionScore action) {
        final double pct = action.percentage();

        final int weight = ai.getIntProperty(AiProps.SIDECAR_PERSONALITY_WEIGHT);
        if (weight <= 0) {
            return pct;
        }
        final boolean isAggro = ai.getBoolProperty(AiProps.PLAY_AGGRO);
        // Dampening factor ranges from 1.0 (weight=0) down to 0.5 (weight=100).
        final double damp = Math.max(0.5, 1.0 - (weight / 100.0) * 0.5);

        switch (action.actionType()) {
            case "ATTACK":
                if (!isAggro) {
                    return pct * damp;
                }
                break;
            case "PASS":
                if (isAggro) {
                    return pct * damp;
                }
                break;
            case "BLOCK":
                if (isAggro) {
                    return pct * damp;
                }
                break;
            default:
                break;
        }
        return pct;
    }

    /** @return the best action of a given type, or empty if none. */
    public Optional<ActionScore> bestAction(final String actionType) {
        final ActionScore a = latestActions.get(actionType);
        return Optional.ofNullable(a);
    }

    /** @return all actions sorted by percentage descending. */
    public List<ActionScore> getActions() {
        return latestActions.values().stream()
                .sorted((a, b) -> Double.compare(b.percentage(), a.percentage()))
                .collect(Collectors.toList());
    }

    /** @return the action with the highest percentage overall, if any. */
    public Optional<ActionScore> getBestAction() {
        return getActions().stream().findFirst();
    }

    /** @return the latest action map (unmodifiable). */
    public Map<String, ActionScore> getLatestByType() {
        return latestActions;
    }

    /** @return true if the sidecar has provided any action data. */
    public boolean hasData() {
        return enabled && !latestActions.isEmpty();
    }

    // ------------------------------------------------------------------------
    // v4 board-aware accessors
    // ------------------------------------------------------------------------

    /** @return the latest role assessment, or empty if none. */
    public Optional<RoleAssessment> role() {
        return Optional.ofNullable(latestRole);
    }

    /** @return true if the sidecar says the AI should currently play the beatdown role. */
    public boolean isAiBeatdown() {
        return latestRole != null && "beatdown".equalsIgnoreCase(latestRole.aiRole());
    }

    /** @return true if the sidecar says the AI should currently play the control role. */
    public boolean isAiControl() {
        return latestRole != null && "control".equalsIgnoreCase(latestRole.aiRole());
    }

    /** @return true if the sidecar says the AI is the winning side right now. */
    public boolean isAiAhead() {
        return latestRole != null && "ai".equalsIgnoreCase(latestRole.winningSide());
    }

    /** @return the sidecar's value score for a card by name, or empty. */
    public Optional<Double> handValue(final String cardName) {
        if (cardName == null || cardName.isEmpty()) {
            return Optional.empty();
        }
        final HandValuation hv = latestHandValues.get(cardName.toLowerCase(Locale.ROOT));
        return hv == null ? Optional.empty() : Optional.of(hv.value());
    }

    /** @return all hand valuations sorted by value desc. */
    public List<HandValuation> getHandValues() {
        return latestHandValues.values().stream()
                .sorted((a, b) -> Double.compare(b.value(), a.value()))
                .collect(Collectors.toList());
    }

    /** @return latest opening/rolling early-game plan, if any. */
    public Optional<EarlyGamePlan> earlyGamePlan() {
        return Optional.ofNullable(latestEarlyGamePlan);
    }

    /** @return latest per-action manabase plan (fetch/land/utility), if any. */
    public Optional<ManaPlan> manaPlan() {
        return Optional.ofNullable(latestManaPlan);
    }

    /** @return cards the sidecar recommends returning after a mulligan keep. */
    public List<String> bottomCards() {
        return latestEarlyGamePlan == null ? List.of() : latestEarlyGamePlan.bottomCards();
    }

    /** @return inferred opponent-hand categories sorted by probability desc. */
    public List<OpponentHandGuess> opponentLikelyHas() {
        return latestOpponentHand;
    }

    /**
     * Probability that the opponent currently holds at least one card of the
     * given category (e.g. "counterspell", "removal", "wrath").
     */
    public double opponentHoldsProbability(final String category) {
        if (category == null) return 0.0;
        for (final OpponentHandGuess g : latestOpponentHand) {
            if (category.equalsIgnoreCase(g.category())) {
                return g.probability();
            }
        }
        return 0.0;
    }

    /**
     * Ordered target priority for the given spell name; falls back to the
     * generic entry when no spell-specific list exists.
     */
    public List<String> targetPriorityFor(final String spellName) {
        if (spellName != null) {
            final TargetPriority tp = latestTargetPriorities.get(spellName.toLowerCase(Locale.ROOT));
            if (tp != null && tp.targets() != null && !tp.targets().isEmpty()) {
                return tp.targets();
            }
        }
        final TargetPriority generic = latestTargetPriorities.get("");
        return generic == null ? List.of() : generic.targets();
    }

    /** @return the best PLAY_LAND card name, or empty if the sidecar gave none. */
    public Optional<String> bestPlayLandName() {
        if (!latestPlayLands.isEmpty()) {
            final String t = latestPlayLands.get(0).target();
            return Optional.ofNullable(t).filter(s -> !s.isEmpty());
        }
        final ActionScore land = latestActions.get("PLAY_LAND");
        return land == null ? Optional.empty() : Optional.ofNullable(land.target()).filter(s -> !s.isEmpty());
    }

    /** @return all PLAY_SPELL card names in priority order, possibly empty. */
    public List<String> bestPlaySpellNames() {
        if (!latestPlaySpells.isEmpty()) {
            return latestPlaySpells.stream()
                    .map(ActionScore::target)
                    .filter(t -> t != null && !t.isEmpty())
                    .collect(Collectors.toList());
        }
        final ActionScore spell = latestActions.get("PLAY_SPELL");
        if (spell == null || spell.target() == null || spell.target().isEmpty()) {
            return List.of();
        }
        return List.of(spell.target());
    }
}
