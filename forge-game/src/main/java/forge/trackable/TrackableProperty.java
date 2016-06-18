package forge.trackable;

import forge.card.CardRarity;
import forge.game.Direction;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableTypes.TrackableType;

public enum TrackableProperty {
    //Shared
    Text(TrackableTypes.StringType),
    PreventNextDamage(TrackableTypes.IntegerType),
    EnchantedBy(TrackableTypes.CardViewCollectionType),
    Counters(TrackableTypes.CounterMapType),

    //Card 
    Owner(TrackableTypes.PlayerViewType),
    Controller(TrackableTypes.PlayerViewType),
    Zone(TrackableTypes.EnumType(ZoneType.class)),
    Cloned(TrackableTypes.BooleanType),
    FlipCard(TrackableTypes.BooleanType),
    SplitCard(TrackableTypes.BooleanType),
    Attacking(TrackableTypes.BooleanType),
    Blocking(TrackableTypes.BooleanType),
    PhasedOut(TrackableTypes.BooleanType),
    Sickness(TrackableTypes.BooleanType),
    Tapped(TrackableTypes.BooleanType),
    Token(TrackableTypes.BooleanType),
    IsCommander(TrackableTypes.BooleanType),
    Damage(TrackableTypes.IntegerType),
    AssignedDamage(TrackableTypes.IntegerType),
    ShieldCount(TrackableTypes.IntegerType),
    ChosenType(TrackableTypes.StringType),
    ChosenColors(TrackableTypes.StringListType),
    ChosenCards(TrackableTypes.CardViewCollectionType),
    ChosenPlayer(TrackableTypes.PlayerViewType),
    ChosenDirection(TrackableTypes.EnumType(Direction.class)),
    Remembered(TrackableTypes.StringType),
    NamedCard(TrackableTypes.StringType),
    PlayerMayLook(TrackableTypes.PlayerViewCollectionType, false),
    PlayerMayLookTemp(TrackableTypes.PlayerViewCollectionType, false),
    Equipping(TrackableTypes.CardViewType),
    EquippedBy(TrackableTypes.CardViewCollectionType),
    Enchanting(TrackableTypes.GameEntityViewType),
    Fortifying(TrackableTypes.CardViewType),
    FortifiedBy(TrackableTypes.CardViewCollectionType),
    EncodedCards(TrackableTypes.CardViewCollectionType),
    GainControlTargets(TrackableTypes.CardViewCollectionType),
    CloneOrigin(TrackableTypes.CardViewType),
    Cloner(TrackableTypes.StringType),
    ImprintedCards(TrackableTypes.CardViewCollectionType),
    HauntedBy(TrackableTypes.CardViewCollectionType),
    Haunting(TrackableTypes.CardViewType),
    MustBlockCards(TrackableTypes.CardViewCollectionType),
    PairedWith(TrackableTypes.CardViewType),
    CurrentState(TrackableTypes.CardStateViewType, false), //can't respect freeze, otherwise card constructor can crash
    AlternateState(TrackableTypes.CardStateViewType),
    HiddenId(TrackableTypes.IntegerType),

    //Card State
    Name(TrackableTypes.StringType),
    Colors(TrackableTypes.ColorSetType),
    ImageKey(TrackableTypes.StringType),
    Type(TrackableTypes.CardTypeViewType),
    ManaCost(TrackableTypes.ManaCostType),
    SetCode(TrackableTypes.StringType),
    Rarity(TrackableTypes.EnumType(CardRarity.class)),
    OracleText(TrackableTypes.StringType),
    RulesText(TrackableTypes.StringType),
    Power(TrackableTypes.IntegerType),
    Toughness(TrackableTypes.IntegerType),
    Loyalty(TrackableTypes.IntegerType),
    ChangedColorWords(TrackableTypes.StringMapType),
    ChangedTypes(TrackableTypes.StringMapType),
    HasDeathtouch(TrackableTypes.BooleanType),
    HasHaste(TrackableTypes.BooleanType),
    HasInfect(TrackableTypes.BooleanType),
    HasStorm(TrackableTypes.BooleanType),
    HasTrample(TrackableTypes.BooleanType),
    YouMayLook(TrackableTypes.BooleanType),
    OpponentMayLook(TrackableTypes.BooleanType),
    BlockAdditional(TrackableTypes.IntegerType),
    AbilityText(TrackableTypes.StringType),
    NonAbilityText(TrackableTypes.StringType),
    FoilIndex(TrackableTypes.IntegerType),

    //Player
    IsAI(TrackableTypes.BooleanType),
    LobbyPlayerName(TrackableTypes.StringType),
    AvatarIndex(TrackableTypes.IntegerType),
    AvatarCardImageKey(TrackableTypes.StringType),
    Opponents(TrackableTypes.PlayerViewCollectionType),
    Life(TrackableTypes.IntegerType),
    PoisonCounters(TrackableTypes.IntegerType),
    MaxHandSize(TrackableTypes.IntegerType),
    HasUnlimitedHandSize(TrackableTypes.BooleanType),
    NumDrawnThisTurn(TrackableTypes.IntegerType),
    Keywords(TrackableTypes.KeywordCollectionViewType, false),
    Commander(TrackableTypes.CardViewType),
    CommanderDamage(TrackableTypes.IntegerMapType),
    MindSlaveMaster(TrackableTypes.PlayerViewType),
    Ante(TrackableTypes.CardViewCollectionType, false),
    Battlefield(TrackableTypes.CardViewCollectionType, false), //zones can't respect freeze, otherwise cards that die from state based effects won't have that reflected in the UI
    Command(TrackableTypes.CardViewCollectionType, false),
    Exile(TrackableTypes.CardViewCollectionType, false),
    Flashback(TrackableTypes.CardViewCollectionType, false),
    Graveyard(TrackableTypes.CardViewCollectionType, false),
    Hand(TrackableTypes.CardViewCollectionType, false),
    Library(TrackableTypes.CardViewCollectionType, false),
    Mana(TrackableTypes.ManaMapType, false),

    //SpellAbility
    HostCard(TrackableTypes.CardViewType),
    Description(TrackableTypes.StringType),
    CanPlay(TrackableTypes.BooleanType),
    PromptIfOnlyPossibleAbility(TrackableTypes.BooleanType),

    //StackItem
    Key(TrackableTypes.StringType),
    SourceTrigger(TrackableTypes.IntegerType),
    SourceCard(TrackableTypes.CardViewType),
    ActivatingPlayer(TrackableTypes.PlayerViewType),
    TargetCards(TrackableTypes.CardViewCollectionType),
    TargetPlayers(TrackableTypes.PlayerViewCollectionType),
    SubInstance(TrackableTypes.StackItemViewType),
    Ability(TrackableTypes.BooleanType),
    OptionalTrigger(TrackableTypes.BooleanType),

    //Combat
    AttackersWithDefenders(TrackableTypes.GenericMapType, false),
    AttackersWithBlockers(TrackableTypes.GenericMapType, false),
    BandsWithDefenders(TrackableTypes.GenericMapType, false),
    BandsWithBlockers(TrackableTypes.GenericMapType, false),
    AttackersWithPlannedBlockers(TrackableTypes.GenericMapType, false),
    BandsWithPlannedBlockers(TrackableTypes.GenericMapType, false),

    //Game
    Players(TrackableTypes.PlayerViewCollectionType),
    GameType(TrackableTypes.EnumType(GameType.class)),
    Title(TrackableTypes.StringType),
    Turn(TrackableTypes.IntegerType),
    WinningPlayerName(TrackableTypes.StringType),
    WinningTeam(TrackableTypes.IntegerType),
    MatchOver(TrackableTypes.BooleanType),
    NumGamesInMatch(TrackableTypes.IntegerType),
    NumPlayedGamesInMatch(TrackableTypes.IntegerType),
    Stack(TrackableTypes.StackItemViewListType),
    StormCount(TrackableTypes.IntegerType),
    GameOver(TrackableTypes.BooleanType),
    PoisonCountersToLose(TrackableTypes.IntegerType),
    GameLog(TrackableTypes.StringType),
    PlayerTurn(TrackableTypes.PlayerViewType),
    Phase(TrackableTypes.EnumType(PhaseType.class));

    private final TrackableType<?> type;
    private final boolean respectFreeze;

    private TrackableProperty(TrackableType<?> type0) {
        this(type0, true);
    }
    private TrackableProperty(TrackableType<?> type0, boolean respectFreeze0) {
        type = type0;
        respectFreeze = respectFreeze0;
    }

    public boolean respectFreeze() {
        return respectFreeze;
    }

    @SuppressWarnings("unchecked")
    public <T> void updateObjLookup(T newObj) {
        ((TrackableType<T>)type).updateObjLookup(newObj);
    }

    public void copyChangedProps(TrackableObject from, TrackableObject to) {
        type.copyChangedProps(from, to, this);
    }

    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
        return ((TrackableType<T>)type).getDefaultValue();
    }
    @SuppressWarnings("unchecked")
    public <T> T deserialize(TrackableDeserializer td, T oldValue) {
        return ((TrackableType<T>)type).deserialize(td, oldValue);
    }
    @SuppressWarnings("unchecked")
    public <T> void serialize(TrackableSerializer ts, T value) {
        ((TrackableType<T>)type).serialize(ts, value);
    }

    //cache array of all properties to allow quick lookup by ordinal,
    //which reduces the size and improves performance of serialization
    //we don't need to worry about the values changing since we will ensure
    //both players are on the same version of Forge before allowing them to connect
    private static TrackableProperty[] props = values();
    public static int serialize(TrackableProperty prop) {
        return prop.ordinal();
    }
    public static TrackableProperty deserialize(int ordinal) {
        return props[ordinal];
    }
}
