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
    AllVotes("AllVotes"),
    Amount("Amount"),
    Attach("Attach"),
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
    Explorer("Explorer"),
    Event("Event"),
    Fighter("Fighter"),
    FirstTime("FirstTime"),
    Fizzle("Fizzle"),
    IsCombatDamage("IsCombatDamage"),
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
    OtherVoters("OtherVoters"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    OriginalDefender("OriginalDefender"),
    PayingMana("PayingMana"),
    Phase("Phase"),
    Player("Player"),
    Produced("Produced"),
    Result("Result"),
    Scheme("Scheme"),
    Source("Source"),
    Sources("Sources"),
    SourceSA("SourceSA"),
    SpellAbility("SpellAbility"),
    SpellAbilityStackInstance("SpellAbilityStackInstance"),
    SpellAbilityTargetingCards("SpellAbilityTargetingCards"),
    StackInstance("StackInstance"),
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

    /**
     * @param s A string that would be output from toString
     * @return the corresponding key if there is one or null otherwise
     */
    public static AbilityKey fromString(String s) {
        switch (s) {
            case "AbilityMana":
                return AbilityMana;
            case "Activator":
                return Activator;
            case "Affected":
                return Affected;
            case "AllVotes":
                return AllVotes;
            case "Amount":
                return Amount;
            case "Attach":
                return Attach;
            case "AttachSource":
                return AttachSource;
            case "AttachTarget":
                return AttachTarget;
            case "Attacked":
                return Attacked;
            case "Attacker":
                return Attacker;
            case "Attackers":
                return Attackers;
            case "AttackingPlayer":
                return AttackingPlayer;
            case "AttackedTarget":
                return AttackedTarget;
            case "Blocker":
                return Blocker;
            case "Blockers":
                return Blockers;
            case "CastSA":
                return CastSA;
            case "CastSACMC":
                return CastSACMC;
            case "Card":
                return Card;
            case "Cards":
                return Cards;
            case "CardLKI":
                return CardLKI;
            case "Cause":
                return Cause;
            case "Causer":
                return Causer;
            case "Championed":
                return Championed;
            case "CopySA":
                return CopySA;
            case "Cost":
                return Cost;
            case "CostStack":
                return CostStack;
            case "CounterAmount":
                return CounterAmount;
            case "CounteredSA":
                return CounteredSA;
            case "CounterType":
                return CounterType;
            case "Crew":
                return Crew;
            case "CumulativeUpkeepPaid":
                return CumulativeUpkeepPaid;
            case "CurrentCastSpells":
                return CurrentCastSpells;
            case "CurrentStormCount":
                return CurrentStormCount;
            case "DamageAmount":
                return DamageAmount;
            case "DamageSource":
                return DamageSource;
            case "DamageSources":
                return DamageSources;
            case "DamageTarget":
                return DamageTarget;
            case "DamageTargets":
                return DamageTargets;
            case "Defender":
                return Defender;
            case "Defenders":
                return Defenders;
            case "DefendingPlayer":
                return DefendingPlayer;
            case "Destination":
                return Destination;
            case "Devoured":
                return Devoured;
            case "EchoPaid":
                return EchoPaid;
            case "Exploited":
                return Exploited;
            case "Explorer":
                return Explorer;
            case "Event":
                return Event;
            case "Fighter":
                return Fighter;
            case "FirstTime":
                return FirstTime;
            case "Fizzle":
                return Fizzle;
            case "IsCombatDamage":
                return IsCombatDamage;
            case "IndividualCostPaymentInstance":
                return IndividualCostPaymentInstance;
            case "IsMadness":
                return IsMadness;
            case "LifeAmount":
                return LifeAmount;
            case "MonstrosityAmount":
                return MonstrosityAmount;
            case "NewCounterAmount":
                return NewCounterAmount;
            case "Num":
                return Num;
            case "NumBlockers":
                return NumBlockers;
            case "NumThisTurn":
                return NumThisTurn;
            case "Number":
                return Number;
            case "Object":
                return Object;
            case "Objects":
                return Objects;
            case "OtherAttackers":
                return OtherAttackers;
            case "OtherVoters":
                return OtherVoters;
            case "Origin":
                return Origin;
            case "OriginalController":
                return OriginalController;
            case "OriginalDefender":
                return OriginalDefender;
            case "PayingMana":
                return PayingMana;
            case "Phase":
                return Phase;
            case "Player":
                return Player;
            case "Produced":
                return Produced;
            case "Result":
                return Result;
            case "Scheme":
                return Scheme;
            case "Source":
                return Source;
            case "Sources":
                return Sources;
            case "SourceSA":
                return SourceSA;
            case "SpellAbility":
                return SpellAbility;
            case "SpellAbilityStackInstance":
                return SpellAbilityStackInstance;
            case "SpellAbilityTargetingCards":
                return SpellAbilityTargetingCards;
            case "StackInstance":
                return StackInstance;
            case "StackSa":
                return StackSa;
            case "StackSi":
                return StackSi;
            case "Target":
                return Target;
            case "Targets":
                return Targets;
            case "Transformer":
                return Transformer;
            case "Vehicle":
                return Vehicle;
            case "Won":
                return Won;
            default:
                return null;
        }

    }

    public static <V> EnumMap<AbilityKey, V> newMap() {
        return new EnumMap<>(AbilityKey.class);
    }

    public static <V> EnumMap<AbilityKey, V> newMap(Map<AbilityKey, V> map) {
        // The EnumMap constructor throws IllegalArgumentException if the map is empty.
        if (map.isEmpty()) {
            return newMap();
        }
        return new EnumMap<>(map);
    }

    public static Map<AbilityKey, Object> mapFromCard(forge.game.card.Card card) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Card, card);
        return runParams;
    }
}
