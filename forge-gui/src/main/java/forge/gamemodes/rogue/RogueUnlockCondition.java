package forge.gamemodes.rogue;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses and evaluates unlock conditions for Rogue Commander decks.
 *
 * Syntax: Key$ Value | Key$ Value | ...
 *
 * Supported conditions:
 * - Default$ True/False - Always unlocked or always locked
 * - WinWithCommander$ Name - Unlock after winning a run with specified commander
 * - Any RogueStats conditionKey$ N - Unlock when stat value reaches N
 */
public class RogueUnlockCondition {

    // Non-stat condition keys (not backed by RogueStats enum)
    private static final String KEY_DEFAULT = "Default";
    private static final String KEY_WIN_WITH_COMMANDER = "WinWithCommander";
    private final Map<String, String> conditions;
    private final String rawCondition;

    /**
     * Parse an unlock condition string.
     * @param conditionString The condition string (e.g., "Default$ False" or "RunsWon$ 3 | WinWithCommander$ Meria")
     */
    public RogueUnlockCondition(String conditionString) {
        this.rawCondition = conditionString;
        this.conditions = new HashMap<>();

        if (conditionString == null || conditionString.trim().isEmpty()) {
            // No condition means always unlocked
            conditions.put(KEY_DEFAULT, "True");
            return;
        }

        // Parse pipe-separated conditions
        String[] parts = conditionString.split("\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            int dollarIndex = trimmed.indexOf('$');
            if (dollarIndex > 0) {
                String key = trimmed.substring(0, dollarIndex).trim();
                String value = trimmed.substring(dollarIndex + 1).trim();
                conditions.put(key, value);
            }
        }
    }

    /**
     * Evaluate whether this condition is met based on current meta progress.
     * For conditions with multiple parts, ALL conditions must be met (AND logic).
     * @return true if the commander should be unlocked
     */
    public boolean evaluate() {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();

        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!evaluateSingleCondition(key, value, progress)) {
                return false; // Any failed condition means locked
            }
        }

        return true; // All conditions passed
    }

    /**
     * Evaluate a single condition key-value pair.
     * Stat-backed conditions use progression-eligible values; special conditions handled explicitly.
     */
    private boolean evaluateSingleCondition(String key, String value, RogueMetaProgress progress) {
        if (KEY_DEFAULT.equals(key)) return Boolean.parseBoolean(value);
        if (KEY_WIN_WITH_COMMANDER.equals(key)) return progress.hasWonWithCommander(value);
        // All other keys are stat-backed (RunsStarted, MatchesWon, MaxLife, etc.)
        return progress.getProgressionStatValue(key) >= parseIntSafe(value);
    }

    /**
     * Check if this condition references a specific key.
     */
    public boolean hasCondition(String key) {
        return conditions.containsKey(key);
    }

    /**
     * Safely parse an integer, returning 0 on failure.
     */
    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number in unlock condition: " + value);
            return Integer.MAX_VALUE; // Impossible to meet on parse error
        }
    }

    /**
     * Get a human-readable description of the unlock condition.
     * @return Description for display in UI
     */
    public String getDescription() {
        if (conditions.containsKey(KEY_DEFAULT)) {
            return Boolean.parseBoolean(conditions.get(KEY_DEFAULT))
                ? "Always available"
                : "Locked";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(getConditionDescription(entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Get description for a single condition.
     */
    private String getConditionDescription(String key, String value) {
        // Non-stat conditions
        if (KEY_WIN_WITH_COMMANDER.equals(key)) return "Win a Run with " + value;

        // Stat-backed conditions — look up by enum key
        RogueStats stat = RogueStats.fromKey(key);
        if (stat != null) return stat.getUnlockDescription(value);

        return key + ": " + value;
    }

    /**
     * Check if this is a default unlock (always available or always locked).
     */
    public boolean isDefault() {
        return conditions.size() == 1 && conditions.containsKey(KEY_DEFAULT);
    }

    /**
     * Check if this is always locked (Default$ False with no other conditions).
     */
    public boolean isAlwaysLocked() {
        return isDefault() && !Boolean.parseBoolean(conditions.get(KEY_DEFAULT));
    }

    /**
     * Get the raw condition string.
     */
    public String getRawCondition() {
        return rawCondition;
    }

    @Override
    public String toString() {
        return rawCondition != null ? rawCondition : "Default$ True";
    }
}
