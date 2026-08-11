package forge.game.decision;

/** The kind of atomic player decision represented by a {@link DecisionRequest}. */
public enum DecisionType {
    PRIORITY_ACTION,
    MULLIGAN,
    MODE,
    X_VALUE,
    TARGET,
    PAYMENT,
    CARD_SELECTION,
    ATTACK,
    BLOCK,
    CONFIRMATION
}
