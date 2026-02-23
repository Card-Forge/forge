package forge.game;

import java.util.EnumSet;
import java.util.Set;

public enum GameLogVerbosity {
    LOW("Low",
        EnumSet.of(GameLogEntryType.GAME_OUTCOME, GameLogEntryType.MATCH_RESULTS,
                   GameLogEntryType.TURN, GameLogEntryType.MULLIGAN,
                   GameLogEntryType.ANTE, GameLogEntryType.DAMAGE)),
    MEDIUM("Medium",
        EnumSet.of(GameLogEntryType.GAME_OUTCOME, GameLogEntryType.MATCH_RESULTS,
                   GameLogEntryType.TURN, GameLogEntryType.MULLIGAN,
                   GameLogEntryType.ANTE, GameLogEntryType.DAMAGE,
                   GameLogEntryType.ZONE_CHANGE, GameLogEntryType.LAND,
                   GameLogEntryType.DISCARD, GameLogEntryType.COMBAT,
                   GameLogEntryType.STACK_ADD, GameLogEntryType.STACK_RESOLVE)),
    HIGH("High",
        EnumSet.allOf(GameLogEntryType.class)),
    CUSTOM("Custom",
        EnumSet.noneOf(GameLogEntryType.class));

    private final String caption;
    private final Set<GameLogEntryType> includedTypes;

    GameLogVerbosity(String caption, Set<GameLogEntryType> includedTypes) {
        this.caption = caption;
        this.includedTypes = includedTypes;
    }

    public Set<GameLogEntryType> getIncludedTypes() {
        return includedTypes;
    }

    /** Parse from either enum name ("HIGH") or caption ("High"). */
    public static GameLogVerbosity fromString(String value) {
        for (GameLogVerbosity v : values()) {
            if (v.name().equalsIgnoreCase(value) || v.caption.equals(value)) {
                return v;
            }
        }
        return MEDIUM; // safe fallback
    }

    @Override
    public String toString() {
        return caption;
    }
}
