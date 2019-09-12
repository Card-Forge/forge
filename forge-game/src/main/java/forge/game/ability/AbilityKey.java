package forge.game.ability;

import forge.game.card.Card;

import java.util.EnumMap;
import java.util.Map;

/**
 * Keys for Ability parameter maps.
 */
public enum AbilityKey {
    AbilityMana("AbilityMana"),
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
    Fighter("Fighter"),
    Fizzle("Fizzle"),
    IsCombatDamage("IsCombatDamage"),
    Player("Player"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    MonstrosityAmount("MonstrosityAmount"),
    NumBlockers("NumBlockers"),
    Objects("Objects"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    Produced("Produced"),
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

    public static Map<AbilityKey, Object> mapFromCard(forge.game.card.Card card) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Card, card);
        return runParams;
    }
}
