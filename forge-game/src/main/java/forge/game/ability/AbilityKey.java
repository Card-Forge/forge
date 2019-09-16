package forge.game.ability;

import java.util.EnumMap;
import java.util.Map;

/**
 * Keys for Ability parameter maps.
 */
public enum AbilityKey {
    AbilityMana("AbilityMana"),
    Activator("Activator"),
    Affected("Affected"),
    Attach("Attach"),
    AllVotes("AllVotes"),
    AttachSource("AttachSource"),
    AttachTarget("AttachTarget"),
    Attacked("Attacked"),
    Attacker("Attacker"),
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget"),
    Blocker("Blocker"),
    Blockers("Blockers"),
    CastSA("CastSA"),
    CastSACMC("CastSACMC"),
    Card("Card"),
    Cards("Cards"),
    CardLKI("CardLKI"),
    Cause("Cause"),
    Causer("Causer"),
    Championed("Championed"),
    CopySA("CopySA"),
    Cost("Cost"),
    CostStack("CostStack"),
    CounterAmount("CounterAmount"),
    CounteredSA("CounteredSA"),
    CounterType("CounterType"),
    Crew("Crew"),
    CumulativeUpkeepPaid("CumulativeUpkeepPaid"),
    CurrentCastSpells("CurrentCastSpells"),
    CurrentStormCount("CurrentStormCount"),
    DamageAmount("DamageAmount"),
    DamageSource("DamageSource"),
    DamageSources("DamageSources"),
    DamageTarget("DamageTarget"),
    DamageTargets("DamageTargets"),
    Defender("Defender"),
    Defenders("Defenders"),
    DefendingPlayer("DefendingPlayer"),
    Destination("Destination"),
    Devoured("Devoured"),
    EchoPaid("EchoPaid"),
    Exploited("Exploited"),
    Event("Event"),
    Fighter("Fighter"),
    FirstTime("FirstTime"),
    Fizzle("Fizzle"),
    IsCombatDamage("IsCombatDamage"),
    PayingMana("PayingMana"),
    Phase("Phase"),
    Player("Player"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    IsMadness("IsMadness"),
    LifeAmount("LifeAmount"),
    MonstrosityAmount("MonstrosityAmount"),
    NewCounterAmount("NewCounterAmount"),
    Num("Num"), // TODO confirm that this and NumThisTurn can be merged
    NumBlockers("NumBlockers"),
    NumThisTurn("NumThisTurn"),
    Number("Number"),
    Object("Object"),
    Objects("Objects"),
    OtherAttackers("OtherAttackers"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    Produced("Produced"),
    Result("Result"),
    Scheme("Scheme"),
    Source("Source"),
    SourceSA("SourceSA"),
    SpellAbilityStackInstance("SpellAbilityStackInstance"),
    StackSa("StackSa"),
    StackSi("StackSi"),
    Target("Target"),
    Targets("Targets"),
    Transformer("Transformer"),
    Vehicle("Vehicle"),
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
