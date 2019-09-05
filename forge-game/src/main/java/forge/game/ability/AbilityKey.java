package forge.game.ability;

/**
 * Keys for Trigger parameter maps.
 */
public enum AbilityKey {
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget");

    private String key;

    AbilityKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
