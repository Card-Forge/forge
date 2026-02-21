package forge.game.ability;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

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
    AttachSource("AttachSource"),
    AttachTarget("AttachTarget"),
    Attacked("Attacked"),
    Attacker("Attacker"),
    Attackers("Attackers"),
    AttackingPlayer("AttackingPlayer"),
    AttackedTarget("AttackedTarget"),
    Blocker("Blocker"),
    Blockers("Blockers"),
    CanReveal("CanReveal"),
    Card("Card"),
    CardState("CardState"),
    Cards("Cards"),
    CardsFiltered("CardsFiltered"),
    CardLKI("CardLKI"),
    Cause("Cause"),
    Causer("Causer"),
    Championed("Championed"),
    ClassLevel("ClassLevel"),
    CostStack("CostStack"),
    CounterAmount("CounterAmount"),
    CounteredSA("CounteredSA"),
    CounterNum("CounterNum"),
    CounterMap("CounterMap"),
    CounterTable("CounterTable"),
    CounterType("CounterType"),
    Crew("Crew"),
    CumulativeUpkeepPaid("CumulativeUpkeepPaid"),
    CurrentCastSpells("CurrentCastSpells"),
    CurrentStormCount("CurrentStormCount"),
    Cycling("Cycling"),
    DamageAmount("DamageAmount"),
    DamageMap("DamageMap"),
    DamageSource("DamageSource"),
    DamageSources("DamageSources"),
    DamageTarget("DamageTarget"),
    DamageTargets("DamageTargets"),
    Defender("Defender"),
    Defenders("Defenders"),
    DefendingPlayer("DefendingPlayer"),
    Destination("Destination"),
    Devoured("Devoured"),
    DicePTExchanges("DicePTExchanges"),
    Discard("Discard"),
    DiscardedBefore("DiscardedBefore"),
    DividedShieldAmount("DividedShieldAmount"),
    EchoPaid("EchoPaid"),
    EffectOnly("EffectOnly"),
    Enlisted("Enlisted"),
    Exploited("Exploited"),
    Explored("Explored"),
    Explorer("Explorer"),
    ExtraTurn("ExtraTurn"),
    ETB("ETB"),
    Fighter("Fighter"),
    Fighters("Fighters"),
    FirstTime("FirstTime"),
    Fizzle("Fizzle"),
    FoundSearchingLibrary("FoundSearchingLibrary"),
    Ignore("Ignore"),
    IgnoreChosen("IgnoreChosen"),
    IsCombat("IsCombat"), // TODO confirm that this and IsCombatDamage can be merged
    IsCombatDamage("IsCombatDamage"),
    IsDamage("IsDamage"),
    IndividualCostPaymentInstance("IndividualCostPaymentInstance"),
    LastStateBattlefield("LastStateBattlefield"),
    LastStateGraveyard("LastStateGraveyard"),
    LifeAmount("LifeAmount"), //TODO confirm that this and LifeGained can be merged
    LifeGained("LifeGained"),
    LoseReason("LoseReason"),
    Map("Map"),
    Mana("Mana"),
    MergedCards("MergedCards"),
    Mode("Mode"),
    NaturalResult("NaturalResult"),
    NewCard("NewCard"),
    NewCounterAmount("NewCounterAmount"),
    NoPreventDamage("NoPreventDamage"),
    Num("Num"),
    Number("Number"),
    Object("Object"),
    Objects("Objects"),
    OpponentVotedDiff("OpponentVotedDiff"),
    OpponentVotedSame("OpponentVotedSame"),
    OtherAttackers("OtherAttackers"),
    Origin("Origin"),
    OriginalController("OriginalController"),
    OriginalDefender("OriginalDefender"),
    OriginalParams("OriginalParams"),
    PayingMana("PayingMana"),
    Phase("Phase"),
    Player("Player"),
    PreventedAmount("PreventedAmount"),
    Produced("Produced"),
    Regeneration("Regeneration"),
    ReplacementEffect("ReplacementEffect"),
    ReplacementResult("ReplacementResult"),
    ReplacementResultMap("ReplacementResultMap"),
    Result("Result"),
    RolledToVisitAttractions("RolledToVisitAttractions"),
    RoomName("RoomName"),
    Scheme("Scheme"),
    ScryBottom("ScryBottom"),
    ScryNum("ScryNum"),
    Sides("Sides"),
    Source("Source"),
    Sources("Sources"),
    SourceSA("SourceSA"),
    SpellAbility("SpellAbility"),
    SpellAbilityTargets("SpellAbilityTargets"),
    StackSa("StackSa"),
    SurveilNum("SurveilNum"),
    Target("Target"),
    Targets("Targets"),
    Token("Token"),
    TokenNum("TokenNum"),
    Valiant("Valiant"),
    Won("Won"),

    // below shared across different Replacements, don't reuse
    InternalTriggerTable("InternalTriggerTable"),
    SimultaneousETB("SimultaneousETB"); // for CR 614.13c

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
        for (AbilityKey k : values()) {
            if (k.toString().equalsIgnoreCase(s)) {
                return k;
            }
        }
        return null;
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

    public static Map<AbilityKey, Object> mapFromCard(Card card) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Card, card);
        return runParams;
    }

    public static Map<AbilityKey, Object> mapFromPlayer(Player player) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Player, player);
        return runParams;
    }

    public static Map<AbilityKey, Object> mapFromAffected(GameEntity gameEntity) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Affected, gameEntity);
        return runParams;
    }

    public static Map<AbilityKey, Object> mapFromPIMap(Map<Player, Integer> map) {
        final Map<AbilityKey, Object> runParams = newMap();

        runParams.put(Map, map);
        return runParams;
    }

    public static CardZoneTable addCardZoneTableParams(Map<AbilityKey, Object> params, SpellAbility sa) {
        CardZoneTable table = CardZoneTable.getSimultaneousInstance(sa);
        addCardZoneTableParams(params, table);
        return table;
    }
    public static void addCardZoneTableParams(Map<AbilityKey, Object> params, CardZoneTable table) {
        params.put(AbilityKey.LastStateBattlefield, table.getLastStateBattlefield());
        params.put(AbilityKey.LastStateGraveyard, table.getLastStateGraveyard());
        params.put(AbilityKey.InternalTriggerTable, table);
    }
}
