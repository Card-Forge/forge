package forge.card.abilityfactory;

import java.lang.reflect.Constructor;
import forge.card.abilityfactory.ai.*;
import forge.card.abilityfactory.effects.*;

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
    Encode (EncodeEffect.class, CannotPlayAi.class),
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
    ManaBurn (ManaBurnEffect.class, CannotPlayAi.class),
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
    WinsGame (GameWinEffect.class, GameWinAi.class);

    ApiType(Class<? extends SpellEffect> clsEf, Class<? extends SpellAiLogic> clsAI) {
        clsEffect = clsEf;
        clsAi = clsAI;
    }

    private final Class<? extends SpellEffect> clsEffect;
    private final Class<? extends SpellAiLogic> clsAi;

    public static ApiType smartValueOf(String value) {

        final String valToCompate = value.trim();
        for (final ApiType v : ApiType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in ApiType enum");
    }

    public SpellEffect getSpellEffect() {
        if (null == clsEffect) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Constructor<? extends SpellEffect>[] cc = (Constructor<? extends SpellEffect>[]) clsEffect.getConstructors();
        for (Constructor<? extends SpellEffect> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp.length == 0) {
                try {
                    SpellEffect res = c.newInstance();
                    return res;
                } catch (Exception e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No default constructor found in class " + clsEffect.getName());
    }

    public SpellAiLogic getAi() {
        if (null == clsAi) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Constructor<? extends SpellAiLogic>[] cc = (Constructor<? extends SpellAiLogic>[]) clsAi.getConstructors();
        for (Constructor<? extends SpellAiLogic> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp.length == 0) {
                try {
                    SpellAiLogic res = c.newInstance();
                    return res;
                } catch (Exception e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No default constructor found in class " + clsEffect.getName());
    }

}
