package forge.game.ability;

import java.util.EnumMap;

/**
 * Keys for Ability parameter maps.
 */
public enum AbilityKey {
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget"),
    Card("Card"),
    Cause("Cause"),
    Destination("Destination"),
    Player("Player"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    Origin("Origin"),
    SpellAbilityStackInstance("SpellAbilityStackInstance")
    ;


    private String key;

    AbilityKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }

    public static <V> EnumMap<AbilityKey, V> newMap() {
        return new EnumMap<>(AbilityKey.class);
    }
}
