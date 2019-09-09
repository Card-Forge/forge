package forge.game.ability;

import java.util.EnumMap;

/**
 * Keys for Ability parameter maps.
 */
public enum AbilityKey {
    Affected("Affected"),
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget"),
    Card("Card"),
    CardLKI("CardLKI"),
    Cause("Cause"),
    CounteredSA("CounteredSA"),
    Destination("Destination"),
    Event("Event"),
    Fizzle("Fizzle"),
    Player("Player"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    SpellAbilityStackInstance("SpellAbilityStackInstance"),
    StackSa("StackSa"),
    StackSi("StackSi");


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
