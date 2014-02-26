package forge.ai;

public enum AiPlayDecision {
    WillPlay, 
    CantPlaySa,
    CantPlayAi,
    CantAfford,
    CantAffordX,
    WaitForMain2,
    AnotherTime,
    MissingNeededCards,
    NeedsToPlayCriteriaNotMet,
    TargetingFailed,
    CostNotAcceptable,
    WouldDestroyLegend,
    WouldDestroyOtherPlaneswalker,
    WouldBecomeZeroToughnessCreature,
    WouldDestroyWorldEnchantment,
    BadEtbEffects,
    CurseEffects;
}