package forge.trackable;

public class TrackableProperty {
    public enum CardProp {
        Owner,
        Controller,
        Zone,
        Cloned,
        FaceDown,
        FlipCard,
        Flipped,
        SplitCard,
        Transformed,
        SetCode,
        Rarity,
        Attacking,
        Blocking,
        PhasedOut,
        Sickness,
        Tapped,
        Token,
        Counters,
        Damage,
        AssignedDamage,
        ShieldCount,
        PreventNextDamage,
        ChosenType,
        ChosenColors,
        ChosenPlayer,
        NamedCard,
        Equipping,
        EquippedBy,
        Enchanting,
        EnchantedBy,
        Fortifying,
        FortifiedBy,
        GainControlTargets,
        CloneOrigin,
        Imprinted,
        HauntedBy,
        Haunting,
        MustBlock,
        PairedWith,
        Original,
        Alternate
    }

    public enum CardStateProp {
        Name,
        Colors,
        ImageKey,
        Type,
        ManaCost,
        Power,
        Toughness,
        Loyalty,
        Text,
        ChangedColorWords,
        ChangedTypes,
        HasDeathtouch,
        HasHaste,
        HasInfect,
        HasStorm,
        HasTrample,
        FoilIndex
    }

    public enum PlayerProp {
        LobbyPlayer,
        Opponents,
        Life,
        PoisonCounters,
        MaxHandSize,
        HasUnlimitedHandSize,
        NumDrawnThisTurn,
        PreventNextDamage,
        EnchantedBy,
        Keywords,
        CommanderInfo,
        Ante,
        Battlefield,
        Command,
        Exile,
        Flashback,
        Graveyard,
        Hand,
        Library,
        Mana
    }

    public enum SpellAbilityProp {
        HostCard,
        Description,
        CanPlay,
        PromptIfOnlyPossibleAbility
    }

    public enum StackItemProp {
        Key,
        SourceTrigger,
        Text,
        SourceCard,
        Activator,
        TargetCards,
        TargetPlayers,
        SubInstance,
        Ability,
        OptionalTrigger
    }

    public enum CombatProp {
        AttackersWithDefenders,
        AttackersWithBlockers,
        BandsWithDefenders,
        BandsWithBlockers,
        AttackersWithPlannedBlockers,
        BandsWithPlannedBlockers
    }
}
