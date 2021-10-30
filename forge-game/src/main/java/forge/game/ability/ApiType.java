package forge.game.ability;


import java.util.HashMap;
import java.util.Map;

import forge.game.ability.effects.*;
import forge.util.ReflectionUtil;

/**
 * TODO: Write javadoc for this type.
 *
 */
public enum ApiType {
    Abandon (AbandonEffect.class),
    ActivateAbility (ActivateAbilityEffect.class),
    AddOrRemoveCounter (CountersPutOrRemoveEffect.class),
    AddPhase (AddPhaseEffect.class),
    AddTurn (AddTurnEffect.class),
    Amass (AmassEffect.class),
    Animate (AnimateEffect.class),
    AnimateAll (AnimateAllEffect.class),
    Attach (AttachEffect.class),
    Ascend (AscendEffect.class),
    AssignGroup (AssignGroupEffect.class),
    Balance (BalanceEffect.class),
    BecomeMonarch (BecomeMonarchEffect.class),
    BecomesBlocked (BecomesBlockedEffect.class),
    BidLife (BidLifeEffect.class),
    Block (BlockEffect.class),
    Bond (BondEffect.class),
    Branch (BranchEffect.class),
    Camouflage (CamouflageEffect.class),
    ChangeCombatants (ChangeCombatantsEffect.class),
    ChangeTargets (ChangeTargetsEffect.class),
    ChangeText (ChangeTextEffect.class),
    ChangeX (ChangeXEffect.class),
    ChangeZone (ChangeZoneEffect.class),
    ChangeZoneAll (ChangeZoneAllEffect.class),
    Charm (CharmEffect.class),
    ChooseCard (ChooseCardEffect.class),
    ChooseColor (ChooseColorEffect.class),
    ChooseDirection (ChooseDirectionEffect.class),
    ChooseEntity (ChooseEntityEffect.class),
    ChooseEvenOdd (ChooseEvenOddEffect.class),
    ChooseNumber (ChooseNumberEffect.class),
    ChoosePlayer (ChoosePlayerEffect.class),
    ChooseSource (ChooseSourceEffect.class),
    ChooseType (ChooseTypeEffect.class),
    Clash (ClashEffect.class),
    ClassLevelUp (ClassLevelUpEffect.class),
    Cleanup (CleanUpEffect.class),
    Clone (CloneEffect.class),
    CompanionChoose (ChooseCompanionEffect.class),
    CopyPermanent (CopyPermanentEffect.class),
    CopySpellAbility (CopySpellAbilityEffect.class),
    ControlSpell (ControlSpellEffect.class),
    ControlPlayer (ControlPlayerEffect.class),
    Counter (CounterEffect.class),
    DamageAll (DamageAllEffect.class),
    DealDamage (DamageDealEffect.class),
    DayTime (DayTimeEffect.class),
    Debuff (DebuffEffect.class),
    DeclareCombatants (DeclareCombatantsEffect.class),
    DelayedTrigger (DelayedTriggerEffect.class),
    Destroy (DestroyEffect.class),
    DestroyAll (DestroyAllEffect.class),
    Dig (DigEffect.class),
    DigMultiple (DigMultipleEffect.class),
    DigUntil (DigUntilEffect.class),
    Discard (DiscardEffect.class),
    DrainMana (DrainManaEffect.class),
    Draw (DrawEffect.class),
    EachDamage (DamageEachEffect.class),
    Effect (EffectEffect.class),
    Encode (EncodeEffect.class),
    EndCombatPhase (EndCombatPhaseEffect.class),
    EndTurn (EndTurnEffect.class),
    ExchangeLife (LifeExchangeEffect.class),
    ExchangeLifeVariant (LifeExchangeVariantEffect.class),
    ExchangeControl (ControlExchangeEffect.class),
    ExchangeControlVariant (ControlExchangeVariantEffect.class),
    ExchangePower (PowerExchangeEffect.class),
    ExchangeZone (ZoneExchangeEffect.class),
    Explore (ExploreEffect.class),
    Fight (FightEffect.class),
    FlipACoin (FlipCoinEffect.class),
    FlipOntoBattlefield (FlipOntoBattlefieldEffect.class),
    Fog (FogEffect.class),
    GainControl (ControlGainEffect.class),
    GainControlVariant (ControlGainVariantEffect.class),
    GainLife (LifeGainEffect.class),
    GainOwnership (OwnershipGainEffect.class),
    GameDrawn (GameDrawEffect.class),
    GenericChoice (ChooseGenericEffect.class),
    Goad (GoadEffect.class),
    Haunt (HauntEffect.class),
    Investigate (InvestigateEffect.class),
    ImmediateTrigger (ImmediateTriggerEffect.class),
    Learn (LearnEffect.class),
    LookAt (LookAtEffect.class),
    LoseLife (LifeLoseEffect.class),
    LosesGame (GameLossEffect.class),
    MakeCard (MakeCardEffect.class),
    Mana (ManaEffect.class),
    ManaReflected (ManaReflectedEffect.class),
    Manifest (ManifestEffect.class),
    Meld (MeldEffect.class),
    Mill (MillEffect.class),
    MoveCounter (CountersMoveEffect.class),
    MultiplePiles (MultiplePilesEffect.class),
    MultiplyCounter (CountersMultiplyEffect.class),
    MustAttack (MustAttackEffect.class),
    MustBlock (MustBlockEffect.class),
    Mutate (MutateEffect.class),
    NameCard (ChooseCardNameEffect.class),
    NoteCounters (CountersNoteEffect.class),
    PeekAndReveal (PeekAndRevealEffect.class),
    PermanentCreature (PermanentCreatureEffect.class),
    PermanentNoncreature (PermanentNoncreatureEffect.class),
    Phases (PhasesEffect.class),
    Planeswalk (PlaneswalkEffect.class),
    Play (PlayEffect.class),
    PlayLandVariant (PlayLandVariantEffect.class),
    Poison (PoisonEffect.class),
    PreventDamage (DamagePreventEffect.class),
    PreventDamageAll (DamagePreventAllEffect.class),
    Proliferate (CountersProliferateEffect.class),
    Protection (ProtectEffect.class),
    ProtectionAll (ProtectAllEffect.class),
    Pump (PumpEffect.class),
    PumpAll (PumpAllEffect.class),
    PutCounter (CountersPutEffect.class),
    PutCounterAll (CountersPutAllEffect.class),
    RearrangeTopOfLibrary (RearrangeTopOfLibraryEffect.class),
    Regenerate (RegenerateEffect.class),
    RegenerateAll (RegenerateAllEffect.class),
    Regeneration (RegenerationEffect.class),
    RemoveCounter (CountersRemoveEffect.class),
    RemoveCounterAll (CountersRemoveAllEffect.class),
    RemoveFromCombat (RemoveFromCombatEffect.class),
    RemoveFromGame (RemoveFromGameEffect.class),
    RemoveFromMatch (RemoveFromMatchEffect.class),
    ReorderZone (ReorderZoneEffect.class),
    Repeat (RepeatEffect.class),
    RepeatEach (RepeatEachEffect.class),
    ReplaceEffect (ReplaceEffect.class),
    ReplaceMana (ReplaceManaEffect.class),
    ReplaceDamage (ReplaceDamageEffect.class),
    ReplaceToken (ReplaceTokenEffect.class),
    ReplaceSplitDamage (ReplaceSplitDamageEffect.class),
    RestartGame (RestartGameEffect.class),
    Reveal (RevealEffect.class),
    RevealHand (RevealHandEffect.class),
    ReverseTurnOrder (ReverseTurnOrderEffect.class),
    RollDice (RollDiceEffect.class),
    RollPlanarDice (RollPlanarDiceEffect.class),
    RunChaos (RunChaosEffect.class),
    Sacrifice (SacrificeEffect.class),
    SacrificeAll (SacrificeAllEffect.class),
    Scry (ScryEffect.class),
    SetInMotion (SetInMotionEffect.class),
    SetLife (LifeSetEffect.class),
    SetState (SetStateEffect.class),
    Shuffle (ShuffleEffect.class),
    SkipPhase (SkipPhaseEffect.class),
    SkipTurn (SkipTurnEffect.class),
    StoreSVar (StoreSVarEffect.class),
    Subgame (SubgameEffect.class),
    Surveil (SurveilEffect.class),
    SwitchBlock (SwitchBlockEffect.class),
    Tap (TapEffect.class),
    TapAll (TapAllEffect.class),
    TapOrUntap (TapOrUntapEffect.class),
    TapOrUntapAll (TapOrUntapAllEffect.class),
    Token (TokenEffect.class, false),
    TwoPiles (TwoPilesEffect.class),
    Unattach (UnattachEffect.class),
    UnattachAll (UnattachAllEffect.class),
    Untap (UntapEffect.class),
    UntapAll (UntapAllEffect.class),
    Venture (VentureEffect.class),
    Vote (VoteEffect.class),
    WinsGame (GameWinEffect.class),


    DamageResolve (DamageResolveEffect.class),
    ChangeZoneResolve (ChangeZoneResolveEffect.class),
    InternalEtbReplacement (ETBReplacementEffect.class),
    InternalLegendaryRule (CharmEffect.class),
    InternalIgnoreEffect (CharmEffect.class);


    private final SpellAbilityEffect instanceEffect;
    private final Class<? extends SpellAbilityEffect> clsEffect;

    private static final Map<String, ApiType> allValues = new HashMap<>();

    static {
    	for (ApiType t : ApiType.values()) {
    		allValues.put(t.name().toLowerCase(), t);
    	}
    }

    ApiType(Class<? extends SpellAbilityEffect> clsEf) { this(clsEf, true); }
    ApiType(Class<? extends SpellAbilityEffect> clsEf, final boolean isStateLess) {
        clsEffect = clsEf;
        instanceEffect = isStateLess ? ReflectionUtil.makeDefaultInstanceOf(clsEf) : null;
    }

    public static ApiType smartValueOf(String value) {
        ApiType v = allValues.get(value.toLowerCase());
        if ( v == null )
            throw new RuntimeException("Element " + value + " not found in ApiType enum");
        return v;
    }

    public SpellAbilityEffect getSpellEffect() {
        return instanceEffect != null ? instanceEffect : ReflectionUtil.makeDefaultInstanceOf(clsEffect);
    }
}
