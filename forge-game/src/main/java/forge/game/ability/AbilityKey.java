package forge.game.ability;

import java.util.EnumMap;

/**
 * Keys for Ability parameter maps.
 */
public enum AbilityKey {
    Affected("Affected"),
    Attacker("Attacker"),
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget"),
    Blocker("Blocker"),
    Blockers("Blockers"),
    Card("Card"),
    CardLKI("CardLKI"),
    Cause("Cause"),
    Causer("Causer"),
    Championed("Championed"),
    CounteredSA("CounteredSA"),
    DamageAmount("DamageAmount"),
    DamageSource("DamageSource"),
    DamageTarget("DamageTarget"),
    Defender("Defender"),
    DefendingPlayer("DefendingPlayer"),
    Destination("Destination"),
    Event("Event"),
    Fizzle("Fizzle"),
    IsCombatDamage("IsCombatDamage"),
    Player("Player"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    MonstrosityAmount("MonstrosityAmount"),
    NumBlockers("NumBlockers"),
    Objects("Objects"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    Result("Result"),
    Scheme("Scheme"),
    SpellAbilityStackInstance("SpellAbilityStackInstance"),
    StackSa("StackSa"),
    StackSi("StackSi"),
    Target("Target"),
    Won("Won");


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
