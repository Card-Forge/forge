package forge.card.ability;


import java.util.Map;
import java.util.TreeMap;

import forge.card.ability.ai.AddPhaseAi;
import forge.card.ability.ai.AddTurnAi;
import forge.card.ability.ai.AlwaysPlayAi;
import forge.card.ability.ai.AnimateAi;
import forge.card.ability.ai.AnimateAllAi;
import forge.card.ability.ai.AttachAi;
import forge.card.ability.ai.BondAi;
import forge.card.ability.ai.CanPlayAsDrawbackAi;
import forge.card.ability.ai.CannotPlayAi;
import forge.card.ability.ai.ChangeZoneAi;
import forge.card.ability.ai.ChangeZoneAllAi;
import forge.card.ability.ai.CharmAi;
import forge.card.ability.ai.ChooseCardAi;
import forge.card.ability.ai.ChooseCardNameAi;
import forge.card.ability.ai.ChooseColorAi;
import forge.card.ability.ai.ChoosePlayerAi;
import forge.card.ability.ai.ChooseSourceAi;
import forge.card.ability.ai.ChooseTypeAi;
import forge.card.ability.ai.ClashAi;
import forge.card.ability.ai.CloneAi;
import forge.card.ability.ai.ControlExchangeAi;
import forge.card.ability.ai.ControlGainAi;
import forge.card.ability.ai.CopyPermanentAi;
import forge.card.ability.ai.CounterAi;
import forge.card.ability.ai.CountersMoveAi;
import forge.card.ability.ai.CountersProliferateAi;
import forge.card.ability.ai.CountersPutAi;
import forge.card.ability.ai.CountersPutAllAi;
import forge.card.ability.ai.CountersRemoveAi;
import forge.card.ability.ai.DamageAllAi;
import forge.card.ability.ai.DamageDealAi;
import forge.card.ability.ai.DamageEachAi;
import forge.card.ability.ai.DamagePreventAi;
import forge.card.ability.ai.DamagePreventAllAi;
import forge.card.ability.ai.DebuffAi;
import forge.card.ability.ai.DebuffAllAi;
import forge.card.ability.ai.DelayedTriggerAi;
import forge.card.ability.ai.DestroyAi;
import forge.card.ability.ai.DestroyAllAi;
import forge.card.ability.ai.DigAi;
import forge.card.ability.ai.DigUntilAi;
import forge.card.ability.ai.DiscardAi;
import forge.card.ability.ai.DrainManaAi;
import forge.card.ability.ai.DrawAi;
import forge.card.ability.ai.EffectAi;
import forge.card.ability.ai.EncodeAi;
import forge.card.ability.ai.EndTurnAi;
import forge.card.ability.ai.FightAi;
import forge.card.ability.ai.FogAi;
import forge.card.ability.ai.GameLossAi;
import forge.card.ability.ai.GameWinAi;
import forge.card.ability.ai.LifeExchangeAi;
import forge.card.ability.ai.LifeGainAi;
import forge.card.ability.ai.LifeLoseAi;
import forge.card.ability.ai.LifeSetAi;
import forge.card.ability.ai.MillAi;
import forge.card.ability.ai.MustAttackAi;
import forge.card.ability.ai.MustBlockAi;
import forge.card.ability.ai.PeekAndRevealAi;
import forge.card.ability.ai.PermanentCreatureAi;
import forge.card.ability.ai.PermanentNoncreatureAi;
import forge.card.ability.ai.PhasesAi;
import forge.card.ability.ai.PlayAi;
import forge.card.ability.ai.PoisonAi;
import forge.card.ability.ai.ProtectAi;
import forge.card.ability.ai.ProtectAllAi;
import forge.card.ability.ai.PumpAi;
import forge.card.ability.ai.PumpAllAi;
import forge.card.ability.ai.RearrangeTopOfLibraryAi;
import forge.card.ability.ai.RegenerateAi;
import forge.card.ability.ai.RegenerateAllAi;
import forge.card.ability.ai.RemoveFromCombatAi;
import forge.card.ability.ai.RepeatAi;
import forge.card.ability.ai.RepeatEachAi;
import forge.card.ability.ai.RestartGameAi;
import forge.card.ability.ai.RevealAi;
import forge.card.ability.ai.RevealHandAi;
import forge.card.ability.ai.SacrificeAi;
import forge.card.ability.ai.SacrificeAllAi;
import forge.card.ability.ai.ScryAi;
import forge.card.ability.ai.SetStateAi;
import forge.card.ability.ai.SetStateAllAi;
import forge.card.ability.ai.ShuffleAi;
import forge.card.ability.ai.StoreSVarAi;
import forge.card.ability.ai.TapAi;
import forge.card.ability.ai.TapAllAi;
import forge.card.ability.ai.TapOrUntapAi;
import forge.card.ability.ai.TapOrUntapAllAi;
import forge.card.ability.ai.TokenAi;
import forge.card.ability.ai.TwoPilesAi;
import forge.card.ability.ai.UnattachAllAi;
import forge.card.ability.ai.UntapAi;
import forge.card.ability.ai.UntapAllAi;
import forge.card.ability.effects.*;
import forge.util.ReflectionUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum ApiType {

    Abandon (AbandonEffect.class, AlwaysPlayAi.class),
    AddPhase (AddPhaseEffect.class, AddPhaseAi.class),
    AddTurn (AddTurnEffect.class, AddTurnAi.class),
    Animate (AnimateEffect.class, AnimateAi.class),
    AnimateAll (AnimateAllEffect.class, AnimateAllAi.class),
    Attach (AttachEffect.class, AttachAi.class),
    Bond (BondEffect.class, BondAi.class),
    ChangeZone(ChangeZoneEffect.class, ChangeZoneAi.class),
    ChangeZoneAll(ChangeZoneAllEffect.class, ChangeZoneAllAi.class),
    /** This is <b>Modal</b>, like 'choose one - ' or 'choose two - '. <br> Might be great to rename this api and update all scripts.*/
    Charm(CharmEffect.class, CharmAi.class),
    ChooseCard (ChooseCardEffect.class, ChooseCardAi.class),
    ChooseColor (ChooseColorEffect.class, ChooseColorAi.class),
    ChooseNumber (ChooseNumberEffect.class, CannotPlayAi.class),
    ChoosePlayer (ChoosePlayerEffect.class, ChoosePlayerAi.class),
    ChooseSource (ChooseSourceEffect.class, ChooseSourceAi.class),
    ChooseType (ChooseTypeEffect.class, ChooseTypeAi.class),
    Clash (ClashEffect.class, ClashAi.class),
    Cleanup (CleanUpEffect.class, AlwaysPlayAi.class),
    Clone (CloneEffect.class, CloneAi.class),
    CopyPermanent (CopyPermanentEffect.class, CopyPermanentAi.class),
    CopySpellAbility (CopySpellAbilityEffect.class, CanPlayAsDrawbackAi.class),
    Counter (CounterEffect.class, CounterAi.class),
    DamageAll (DamageAllEffect.class, DamageAllAi.class),
    DealDamage (DamageDealEffect.class, DamageDealAi.class),
    Debuff (DebuffEffect.class, DebuffAi.class),
    DebuffAll (DebuffAllEffect.class, DebuffAllAi.class),
    DelayedTrigger (DelayedTriggerEffect.class, DelayedTriggerAi.class),
    Destroy (DestroyEffect.class, DestroyAi.class),
    DestroyAll (DestroyAllEffect.class, DestroyAllAi.class),
    Dig (DigEffect.class, DigAi.class),
    DigUntil (DigUntilEffect.class, DigUntilAi.class),
    Discard (DiscardEffect.class, DiscardAi.class),
    DrainMana (DrainManaEffect.class, DrainManaAi.class),
    Draw (DrawEffect.class, DrawAi.class),
    EachDamage (DamageEachEffect.class, DamageEachAi.class),
    Effect (EffectEffect.class, EffectAi.class),
    Encode (EncodeEffect.class, EncodeAi.class),
    EndTurn (EndTurnEffect.class, EndTurnAi.class),
    ExchangeLife (LifeExchangeEffect.class, LifeExchangeAi.class),
    ExchangeControl (ControlExchangeEffect.class, ControlExchangeAi.class),
    Fight (FightEffect.class, FightAi.class),
    FlipACoin (FlipCoinEffect.class, AlwaysPlayAi.class),
    Fog (FogEffect.class, FogAi.class),
    GainControl (ControlGainEffect.class, ControlGainAi.class),
    GainLife (LifeGainEffect.class, LifeGainAi.class),
    GenericChoice (ChooseGenericEffect.class, CannotPlayAi.class),
    LoseLife (LifeLoseEffect.class, LifeLoseAi.class),
    LosesGame (GameLossEffect.class, GameLossAi.class),
    Mana (ManaEffect.class, CannotPlayAi.class),
    ManaReflected (ManaReflectedEffect.class, CannotPlayAi.class),
    Mill (MillEffect.class, MillAi.class),
    MoveCounter (CountersMoveEffect.class, CountersMoveAi.class),
    MustAttack (MustAttackEffect.class, MustAttackAi.class),
    MustBlock (MustBlockEffect.class, MustBlockAi.class),
    NameCard (ChooseCardNameEffect.class, ChooseCardNameAi.class),
    PeekAndReveal (PeekAndRevealEffect.class, PeekAndRevealAi.class),
    PermanentCreature (PermanentCreatureEfect.class, PermanentCreatureAi.class),
    PermanentNoncreature (PermanentNoncreatureEffect.class, PermanentNoncreatureAi.class),
    Phases (PhasesEffect.class, PhasesAi.class),
    Planeswalk(PlaneswalkEffect.class, AlwaysPlayAi.class),
    Play (PlayEffect.class, PlayAi.class),
    Poison (PoisonEffect.class, PoisonAi.class),
    PreventDamage (DamagePreventEffect.class, DamagePreventAi.class),
    PreventDamageAll (DamagePreventAllEffect.class, DamagePreventAllAi.class),
    Proliferate (CountersProliferateEffect.class, CountersProliferateAi.class),
    Protection (ProtectEffect.class, ProtectAi.class),
    ProtectionAll (ProtectAllEffect.class, ProtectAllAi.class),
    Pump (PumpEffect.class, PumpAi.class),
    PumpAll (PumpAllEffect.class, PumpAllAi.class),
    PutCounter (CountersPutEffect.class, CountersPutAi.class),
    PutCounterAll (CountersPutAllEffect.class, CountersPutAllAi.class),
    RearrangeTopOfLibrary (RearrangeTopOfLibraryEffect.class, RearrangeTopOfLibraryAi.class),
    Regenerate (RegenerateEffect.class, RegenerateAi.class),
    RegenerateAll (RegenerateAllEffect.class, RegenerateAllAi.class),
    RemoveCounter (CountersRemoveEffect.class, CountersRemoveAi.class),
    RemoveCounterAll (CountersRemoveAllEffect.class, CannotPlayAi.class),
    RemoveFromCombat (RemoveFromCombatEffect.class, RemoveFromCombatAi.class),
    Repeat (RepeatEffect.class, RepeatAi.class),
    RepeatEach (RepeatEachEffect.class, RepeatEachAi.class),
    RestartGame (RestartGameEffect.class, RestartGameAi.class),
    Reveal (RevealEffect.class, RevealAi.class),
    RevealHand (RevealHandEffect.class, RevealHandAi.class),
    RollPlanarDice (RollPlanarDiceEffect.class, CannotPlayAi.class),
    Sacrifice (SacrificeEffect.class, SacrificeAi.class),
    SacrificeAll (SacrificeAllEffect.class, SacrificeAllAi.class),
    Scry (ScryEffect.class, ScryAi.class),
    SetInMotion (SetInMotionEffect.class, AlwaysPlayAi.class),
    SetLife (LifeSetEffect.class, LifeSetAi.class),
    SetState (SetStateEffect.class, SetStateAi.class),
    SetStateAll (SetStateAllEffect.class, SetStateAllAi.class),
    Shuffle (ShuffleEffect.class, ShuffleAi.class),
    StoreSVar (StoreSVarEffect.class, StoreSVarAi.class),
    Tap (TapEffect.class, TapAi.class),
    TapAll (TapAllEffect.class, TapAllAi.class),
    TapOrUntap (TapOrUntapEffect.class, TapOrUntapAi.class),
    TapOrUntapAll (TapOrUntapAllEffect.class, TapOrUntapAllAi.class),
    Token (TokenEffect.class, TokenAi.class),
    TwoPiles (TwoPilesEffect.class, TwoPilesAi.class),
    UnattachAll (UnattachAllEffect.class, UnattachAllAi.class),
    Untap (UntapEffect.class, UntapAi.class),
    UntapAll (UntapAllEffect.class, UntapAllAi.class),
    WinsGame (GameWinEffect.class, GameWinAi.class), 
    
    
    
    InternalEtbReplacement(ETBReplacementEffect.class, CanPlayAsDrawbackAi.class);

    private final Class<? extends SpellAbilityEffect> clsEffect;
    private final Class<? extends SpellAbilityAi> clsAi;
    

    private static final Map<String, ApiType> allValues = new TreeMap<String, ApiType>(String.CASE_INSENSITIVE_ORDER);

    ApiType(Class<? extends SpellAbilityEffect> clsEf, Class<? extends SpellAbilityAi> clsAI) {
        clsEffect = clsEf;
        clsAi = clsAI;
    }

    public static ApiType smartValueOf(String value) {
        if (allValues.isEmpty())
            for(ApiType c : ApiType.values())
                allValues.put(c.toString(), c);
        
        ApiType v = allValues.get(value);
        if ( v == null )
            throw new RuntimeException("Element " + value + " not found in ApiType enum");
        return v;
    }

    public SpellAbilityEffect getSpellEffect() {
        return clsEffect == null ? null : ReflectionUtil.makeDefaultInstanceOf(clsEffect);

    }

    public SpellAbilityAi getAi() {
        return clsAi == null ? null : ReflectionUtil.makeDefaultInstanceOf(clsAi);
    }

}
