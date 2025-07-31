package forge.ai;

public enum AiPlayDecision {
    // Play decision reasons
    WillPlay,
    MandatoryPlay,
    PlayToEmptyHand,
    ImpactCombat,
    ResponseToStackResolve,
    AddBoardPresence,
    Removal,
    Tempo,
    CardAdvantage,

    // Play later decisions
    WaitForCombat,
    WaitForMain2,
    WaitForEndOfTurn,
    StackNotEmpty,
    AnotherTime,

    // Don't play decision reasons
    CantPlaySa,
    CantPlayAi,
    CantAfford,
    CantAffordX,
    DoesntImpactCombat,
    DoesntImpactGame,
    MissingLogic,
    MissingNeededCards,
    TimingRestrictions,
    MissingPhaseRestrictions,
    ConditionsNotMet,
    NeedsToPlayCriteriaNotMet,
    StopRunawayActivations,
    TargetingFailed,
    CostNotAcceptable,
    LifeInDanger,
    WouldDestroyLegend,
    WouldDestroyOtherPlaneswalker,
    WouldBecomeZeroToughnessCreature,
    WouldDestroyWorldEnchantment,
    BadEtbEffects,
    CurseEffects;

    public boolean willingToPlay() {
        return switch (this) {
            case WillPlay, MandatoryPlay, PlayToEmptyHand, AddBoardPresence, ImpactCombat, ResponseToStackResolve, Removal, Tempo, CardAdvantage -> true;
            default -> false;
        };
    }
}