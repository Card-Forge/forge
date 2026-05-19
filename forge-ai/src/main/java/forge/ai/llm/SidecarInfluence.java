package forge.ai.llm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import forge.ai.AiController;
import forge.ai.AiProps;

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

    /** The AI controller, used to read personality properties. */
    private final AiController ai;

    private volatile boolean enabled = false;

    public SidecarInfluence(final AiController ai) {
        this.ai = ai;
    }

    /**
     * Called when a new sidecar result arrives. Stores the actions and applies
     * personality weighting (second pass).
     */
    public void updateFromResult(final RecognitionResult result) {
        if (result == null || result.piloting() == null) {
            return;
        }
        final List<ActionScore> raw = result.piloting().actions();
        if (raw == null || raw.isEmpty()) {
            latestActions = Map.of();
            return;
        }

        final Map<String, ActionScore> weighted = new ConcurrentHashMap<>();
        for (final ActionScore action : raw) {
            final double adjusted = applyPersonalityWeight(action);
            final ActionScore weightedAction = new ActionScore(
                    action.actionType(),
                    action.target(),
                    action.targets(),
                    adjusted,
                    action.reasoning()
            );
            weighted.put(action.actionType(), weightedAction);
        }
        latestActions = Collections.unmodifiableMap(weighted);
        enabled = true;

        Logger.debug("SidecarInfluence: updated with %d weighted actions", weighted.size());
    }

    /**
     * Apply second-pass personality weighting from AiProps.
     * Aggressive profiles boost ATTACK/PLAY_SPELL; defensive ones boost BLOCK/PASS.
     */
    private double applyPersonalityWeight(final ActionScore action) {
        double pct = action.percentage();

        final boolean isAggro = ai.getBoolProperty(AiProps.PLAY_AGGRO);
        final int weight = ai.getIntProperty(AiProps.SIDECAR_PERSONALITY_WEIGHT);

        if (weight <= 0) {
            return pct; // personality weighting disabled
        }

        final double factor = 1.0 + (weight / 100.0 - 1.0) * 0.5; // normalize to 0.5x-1.5x

        switch (action.actionType()) {
            case "PLAY_SPELL":
            case "ACTIVATE_ABILITY":
                if (isAggro) {
                    pct *= Math.min(factor * 1.2, 1.5);
                }
                break;
            case "ATTACK":
                if (isAggro) {
                    pct *= Math.min(factor * 1.3, 1.6);
                } else {
                    pct *= 0.85;
                }
                break;
            case "BLOCK":
                if (!isAggro) {
                    pct *= Math.min(factor * 1.15, 1.4);
                } else {
                    pct *= 0.8;
                }
                break;
            case "PASS":
                if (isAggro) {
                    pct *= 0.7;
                } else {
                    pct *= Math.min(factor * 1.1, 1.3);
                }
                break;
            default:
                break;
        }

        return Math.max(0.0, Math.min(100.0, pct));
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
}
