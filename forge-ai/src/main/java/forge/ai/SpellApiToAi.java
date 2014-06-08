package forge.ai;

import java.util.EnumMap;

import forge.ai.ability.*;
import forge.game.ability.ApiType;
import forge.util.ReflectionUtil;

public enum SpellApiToAi {
    Converter;
    
    private final static EnumMap<ApiType, Class<? extends SpellAbilityAi>> apiToClass = new EnumMap<>(ApiType.class);
    private final EnumMap<ApiType, SpellAbilityAi> apiToInstance = new EnumMap<>(ApiType.class);
    
    static {
        apiToClass.put(ApiType.Abandon, AlwaysPlayAi.class);
        apiToClass.put(ApiType.AddOrRemoveCounter, CountersPutOrRemoveAi.class);
        apiToClass.put(ApiType.AddPhase, AddPhaseAi.class);
        apiToClass.put(ApiType.AddTurn, AddTurnAi.class);
        apiToClass.put(ApiType.Animate, AnimateAi.class);
        apiToClass.put(ApiType.AnimateAll, AnimateAllAi.class);
        apiToClass.put(ApiType.Attach, AttachAi.class);
        apiToClass.put(ApiType.Balance, BalanceAi.class);
        apiToClass.put(ApiType.BecomesBlocked, BecomesBlockedAi.class);
        apiToClass.put(ApiType.BidLife, BidLifeAi.class);
        apiToClass.put(ApiType.Bond, BondAi.class);
        apiToClass.put(ApiType.ChangeTargets, ChangeTargetsAi.class);
        apiToClass.put(ApiType.ChangeZone, ChangeZoneAi.class);
        apiToClass.put(ApiType.ChangeZoneAll, ChangeZoneAllAi.class);
        
        apiToClass.put(ApiType.Charm, CharmAi.class);
        apiToClass.put(ApiType.ChooseCard, ChooseCardAi.class);
        apiToClass.put(ApiType.ChooseColor, ChooseColorAi.class);
        apiToClass.put(ApiType.ChooseDirection, ChooseDirectionAi.class);
        apiToClass.put(ApiType.ChooseNumber, ChooseNumberAi.class);
        apiToClass.put(ApiType.ChoosePlayer, ChoosePlayerAi.class);
        apiToClass.put(ApiType.ChooseSource, ChooseSourceAi.class);
        apiToClass.put(ApiType.ChooseType, ChooseTypeAi.class);
        apiToClass.put(ApiType.Clash, ClashAi.class);
        apiToClass.put(ApiType.Cleanup, AlwaysPlayAi.class);
        apiToClass.put(ApiType.Clone, CloneAi.class);
        apiToClass.put(ApiType.CopyPermanent, CopyPermanentAi.class);
        apiToClass.put(ApiType.CopySpellAbility, CanPlayAsDrawbackAi.class);
        apiToClass.put(ApiType.ControlPlayer, CannotPlayAi.class);
        apiToClass.put(ApiType.ControlSpell, CannotPlayAi.class);
        apiToClass.put(ApiType.Counter, CounterAi.class);
        apiToClass.put(ApiType.DamageAll, DamageAllAi.class);

        apiToClass.put(ApiType.DealDamage, DamageDealAi.class);
        apiToClass.put(ApiType.Debuff, DebuffAi.class);
        apiToClass.put(ApiType.DebuffAll, DebuffAllAi.class);
        apiToClass.put(ApiType.DeclareCombatants, CannotPlayAi.class);
        apiToClass.put(ApiType.DelayedTrigger, DelayedTriggerAi.class);
        apiToClass.put(ApiType.Destroy, DestroyAi.class);
        apiToClass.put(ApiType.DestroyAll, DestroyAllAi.class);
        apiToClass.put(ApiType.Dig, DigAi.class);
        apiToClass.put(ApiType.DigUntil, DigUntilAi.class);
        apiToClass.put(ApiType.Discard, DiscardAi.class);
        apiToClass.put(ApiType.DrainMana, DrainManaAi.class);
        apiToClass.put(ApiType.Draw, DrawAi.class);
        apiToClass.put(ApiType.EachDamage, DamageEachAi.class);
        apiToClass.put(ApiType.Effect, EffectAi.class);
        apiToClass.put(ApiType.Encode, EncodeAi.class);
        apiToClass.put(ApiType.EndTurn, EndTurnAi.class);
        apiToClass.put(ApiType.ExchangeLife, LifeExchangeAi.class);
        apiToClass.put(ApiType.ExchangeControl, ControlExchangeAi.class);
        apiToClass.put(ApiType.ExchangePower, PowerExchangeAi.class);
        apiToClass.put(ApiType.ExchangeZone, ZoneExchangeAi.class);
        apiToClass.put(ApiType.Fight, FightAi.class);
        apiToClass.put(ApiType.FlipACoin, FlipACoinAi.class);
        apiToClass.put(ApiType.Fog, FogAi.class);
        apiToClass.put(ApiType.GainControl, ControlGainAi.class);
        apiToClass.put(ApiType.GainLife, LifeGainAi.class);
        apiToClass.put(ApiType.GainOwnership, CannotPlayAi.class);
        apiToClass.put(ApiType.GenericChoice, ChooseGenericEffectAi.class);
        apiToClass.put(ApiType.LoseLife, LifeLoseAi.class);
        apiToClass.put(ApiType.LosesGame, GameLossAi.class);
        apiToClass.put(ApiType.Mana, ManaEffectAi.class);
        apiToClass.put(ApiType.ManaReflected, CannotPlayAi.class);
        apiToClass.put(ApiType.Mill, MillAi.class);
        apiToClass.put(ApiType.MoveCounter, CountersMoveAi.class);
        apiToClass.put(ApiType.MultiplePiles, CannotPlayAi.class);
        apiToClass.put(ApiType.MustAttack, MustAttackAi.class);
        apiToClass.put(ApiType.MustBlock, MustBlockAi.class);
        apiToClass.put(ApiType.NameCard, ChooseCardNameAi.class);
        apiToClass.put(ApiType.PeekAndReveal, PeekAndRevealAi.class);
        apiToClass.put(ApiType.PermanentCreature, PermanentCreatureAi.class);
        apiToClass.put(ApiType.PermanentNoncreature, PermanentNoncreatureAi.class);
        apiToClass.put(ApiType.Phases, PhasesAi.class);
        apiToClass.put(ApiType.Planeswalk, AlwaysPlayAi.class);
        apiToClass.put(ApiType.Play, PlayAi.class);
        apiToClass.put(ApiType.PlayLandVariant, CannotPlayAi.class);
        apiToClass.put(ApiType.Poison, PoisonAi.class);
        apiToClass.put(ApiType.PreventDamage, DamagePreventAi.class);
        apiToClass.put(ApiType.PreventDamageAll, DamagePreventAllAi.class);
        apiToClass.put(ApiType.Proliferate, CountersProliferateAi.class);
        apiToClass.put(ApiType.Protection, ProtectAi.class);
        apiToClass.put(ApiType.ProtectionAll, ProtectAllAi.class);
        apiToClass.put(ApiType.Pump, PumpAi.class);
        apiToClass.put(ApiType.PumpAll, PumpAllAi.class);
        apiToClass.put(ApiType.PutCounter, CountersPutAi.class);
        apiToClass.put(ApiType.PutCounterAll, CountersPutAllAi.class);
        apiToClass.put(ApiType.RearrangeTopOfLibrary, RearrangeTopOfLibraryAi.class);
        apiToClass.put(ApiType.Regenerate, RegenerateAi.class);
        apiToClass.put(ApiType.RegenerateAll, RegenerateAllAi.class);
        apiToClass.put(ApiType.RemoveCounter, CountersRemoveAi.class);
        apiToClass.put(ApiType.RemoveCounterAll, CannotPlayAi.class);
        apiToClass.put(ApiType.RemoveFromCombat, RemoveFromCombatAi.class);
        apiToClass.put(ApiType.ReorderZone, AlwaysPlayAi.class);
        apiToClass.put(ApiType.Repeat, RepeatAi.class);
        apiToClass.put(ApiType.RepeatEach, RepeatEachAi.class);
        apiToClass.put(ApiType.RestartGame, RestartGameAi.class);
        apiToClass.put(ApiType.Reveal, RevealAi.class);
        apiToClass.put(ApiType.RevealHand, RevealHandAi.class);
        apiToClass.put(ApiType.ReverseTurnOrder, AlwaysPlayAi.class);
        apiToClass.put(ApiType.RollPlanarDice, RollPlanarDiceAi.class);
        apiToClass.put(ApiType.RunSVarAbility, AlwaysPlayAi.class);
        apiToClass.put(ApiType.Sacrifice, SacrificeAi.class);
        apiToClass.put(ApiType.SacrificeAll, SacrificeAllAi.class);
        apiToClass.put(ApiType.Scry, ScryAi.class);
        apiToClass.put(ApiType.SetInMotion, AlwaysPlayAi.class);
        apiToClass.put(ApiType.SetLife, LifeSetAi.class);
        apiToClass.put(ApiType.SetState, SetStateAi.class);
        apiToClass.put(ApiType.Shuffle, ShuffleAi.class);
        apiToClass.put(ApiType.SkipTurn, SkipTurnAi.class);
        apiToClass.put(ApiType.StoreMap, StoreMapAi.class);
        apiToClass.put(ApiType.StoreSVar, StoreSVarAi.class);
        apiToClass.put(ApiType.Tap, TapAi.class);
        apiToClass.put(ApiType.TapAll, TapAllAi.class);
        apiToClass.put(ApiType.TapOrUntap, TapOrUntapAi.class);
        apiToClass.put(ApiType.TapOrUntapAll, TapOrUntapAllAi.class);
        apiToClass.put(ApiType.Token, TokenAi.class);
        apiToClass.put(ApiType.TwoPiles, TwoPilesAi.class);
        apiToClass.put(ApiType.Unattach, CannotPlayAi.class);
        apiToClass.put(ApiType.UnattachAll, UnattachAllAi.class);
        apiToClass.put(ApiType.Untap, UntapAi.class);
        apiToClass.put(ApiType.UntapAll, UntapAllAi.class);
        apiToClass.put(ApiType.Vote, VoteAi.class);
        apiToClass.put(ApiType.WinsGame, GameWinAi.class);

        apiToClass.put(ApiType.InternalEtbReplacement, CanPlayAsDrawbackAi.class);
        apiToClass.put(ApiType.InternalLegendaryRule, LegendaryRuleAi.class);
        apiToClass.put(ApiType.InternalHaunt, HauntAi.class);
        apiToClass.put(ApiType.InternalIgnoreEffect, CannotPlayAi.class);
    }
    
    public SpellAbilityAi get(ApiType api) {
        SpellAbilityAi result = apiToInstance.get(api);
        if( null == result ) {
            Class<? extends SpellAbilityAi> clz = apiToClass.get(api);
            if(null == clz) {
                System.err.println("No AI assigned for API: " + api);
                clz = CannotPlayAi.class;
            }
            result = ReflectionUtil.makeDefaultInstanceOf(clz);
            apiToInstance.put(api, result);
        }
        return result; 
    }
}
