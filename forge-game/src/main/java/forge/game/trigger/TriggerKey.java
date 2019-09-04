package forge.game.trigger;

/**
 * Keys for Trigger parameter maps.
 */
public enum TriggerKey {
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget");

    private String key;

    TriggerKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
