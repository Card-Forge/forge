package forge.gamemodes.match;

/**
 * Per-trigger user choice for optional triggered abilities.
 * Carried over the wire by {@code IGameController.notifyTriggerChoiceChanged}.
 */
public enum TriggerChoice {
    ASK,
    ALWAYS_YES,
    ALWAYS_NO
}
