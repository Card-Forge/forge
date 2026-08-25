package forge.ai;

public enum AiPlayDecision {
    // Play decision reasons
    WillPlay,
    MandatoryPlay,
    PlayToEmptyHand,
    AddBoardPresence,
    ImpactCombat,
    ResponseToStackResolve,
    Removal,
    Tempo,
    CardAdvantage,

    // Play later decisions
    WaitForCombat,
    WaitForMain2,
    WaitForEndOfTurn,
    StackNotEmpty,
    AnotherTime,

    // Don't play reasons
    CantPlaySa,
    CantPlayAi,
    CantAfford,
    CantAffordX,
    TargetingFailed,
    StopRunawayActivations,
    CostNotAcceptable,
    DoesntImpactCombat,
    DoesntImpactGame,
    TimingRestrictions,
    MissingPhaseRestrictions,
    MissingLogic,
    MissingNeededCards,
    NeedsToPlayCriteriaNotMet,
    ConditionsNotMet,
    IncreasesLifeInDanger,
    BadEtbEffects,
    CurseEffects,
    WouldBecomeZeroToughnessCreature,
    WouldDestroyLegend,
    WouldDestroyWorldEnchantment,
    HybridSimRejected;

    public boolean willingToPlay() {
        return switch (this) {
            case WillPlay, MandatoryPlay, PlayToEmptyHand, AddBoardPresence, ImpactCombat, ResponseToStackResolve, Removal, Tempo, CardAdvantage -> true;
            default -> false;
        };
    }
}