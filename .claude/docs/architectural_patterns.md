# Forge Architectural Patterns

This document describes recurring architectural patterns, design decisions, and conventions used throughout the Forge codebase.

## Dependency Injection

**Pattern: Manual Constructor Injection**

Forge does not use a DI framework (no Spring, Guice, etc.). Dependencies are passed through constructors and stored as final fields.

Examples:
- `forge-game/src/main/java/forge/trackable/TrackableObject.java:22-27` - Constructor receives `id` and `tracker` dependencies
- `forge-game/src/main/java/forge/game/card/Card.java:87-99` - Multiple dependencies through constructor
- `forge-game/src/main/java/forge/game/CardTraitBase.java:39-48` - Base class receives `hostCard` dependency

## State Management

### Layered State with Maps/Tables

Game state changes are tracked using Guava's `Table<K, K, V>` and `Map<K, V>` structures, mirroring Magic's layer system for state modification.

- `forge-game/src/main/java/forge/game/card/Card.java:89` - `states = Maps.newEnumMap(CardStateName.class)`
- `forge-game/src/main/java/forge/game/card/Card.java:125` - `changedCardTypesByText = TreeBasedTable.create()` (Layer 3)
- `forge-game/src/main/java/forge/game/card/Card.java:159` - `changedCardColors = TreeBasedTable.create()`

### Trackable Observer Pattern

`TrackableObject` provides state synchronization between server and UI. Properties are tracked and changes propagated to observers.

- `forge-game/src/main/java/forge/trackable/TrackableObject.java:18-19` - Property and change tracking
- `forge-game/src/main/java/forge/game/card/CardView.java:29-69` - View objects extend TrackableObject

### Singleton Model

`FModel` serves as centralized application state holder with static access methods.

- `forge-gui/src/main/java/forge/model/FModel.java:77-80` - Private constructor, static method access

## View/Data Separation

Mutable game objects have corresponding immutable `*View` objects for safe UI exposure.

**Pattern:** Create view via static factory method, view provides read-only access.

- `forge-game/src/main/java/forge/game/card/CardView.java:32` - `public static CardView get(Card c)`
- `forge-game/src/main/java/forge/game/card/CardCollectionView.java` - Read-only collection interface
- `forge-core/src/main/java/forge/util/collect/FCollectionView.java` - Generic collection view

## Interface Design

### Marker Interfaces for Capabilities

Small interfaces define single capabilities for type-safe filtering and composition.

- `forge-core/src/main/java/forge/util/IHasName.java:7-9` - Single method interface
- `forge-core/src/main/java/forge/util/ITriggerEvent.java:3-7` - Event capability interface

### Visitor Pattern

Type-safe operations on heterogeneous objects without instanceof checks.

- `forge-core/src/main/java/forge/util/Visitor.java:3-22` - Generic visitor with `boolean visit(T object)`
- `forge-game/src/main/java/forge/game/cost/ICostVisitor.java` - Cost-specific visitor
- `forge-game/src/main/java/forge/game/event/IGameEventVisitor.java` - Event visitor

## Effect System

### Base Class Hierarchy

All spell abilities extend `SpellAbilityEffect`. Over 200+ effect implementations follow this pattern.

- `forge-game/src/main/java/forge/game/ability/SpellAbilityEffect.java:43-50` - Abstract base
  - `public abstract void resolve(SpellAbility sa)` - Resolution contract
  - `protected String getStackDescription(final SpellAbility sa)` - Stack text

Concrete implementations:
- `forge-game/src/main/java/forge/game/ability/effects/AttachEffect.java`
- `forge-game/src/main/java/forge/game/ability/effects/DamageAllEffect.java`
- Pattern: All files named `*Effect.java`

### Parallel AI Structure

Each effect type has a corresponding AI decision class in `forge-ai`.

- Effect: `forge-game/.../ability/effects/ChooseCardEffect.java`
- AI: `forge-ai/src/main/java/forge/ai/ability/ChooseCardAi.java`

Pattern: `*Effect.java` paired with `*Ai.java`

## Trait Base Class

Triggers, ReplacementEffects, and StaticAbilities share common base with parameter maps.

- `forge-game/src/main/java/forge/game/CardTraitBase.java:39` - Implements `GameObject, IHasCardView, IHasSVars`
- `forge-game/src/main/java/forge/game/CardTraitBase.java:47-48` - Parameter maps for card attributes

## Naming Conventions

| Pattern | Meaning | Examples |
|---------|---------|----------|
| `V*` prefix | Desktop UI view | `VAllDecks.java`, `VCardCatalog.java` |
| `*Effect` suffix | Ability effect implementation | `AttachEffect.java`, `DamageAllEffect.java` |
| `*Ai` suffix | AI decision logic | `ChooseCardAi.java`, `VentureAi.java` |
| `*Predicates` | Predicate factory class | `CardPredicates.java`, `PlayerPredicates.java` |
| `*Handler` | Event/phase management | `TriggerHandler.java`, `ReplacementHandler.java` |
| `*Factory` | Object creation | `AbilityFactory.java`, `CardFactory.java` |
| `*Manager` | Resource/state manager | `FServerManager.java`, `QuestEventDuelManager.java` |
| `*View` | Immutable view object | `CardView.java`, `PlayerView.java` |

## Event Handling

### Game Event Visitor

Events use visitor pattern for type-safe dispatch.

- `forge-game/src/main/java/forge/game/event/GameEvent.java:5` - `public abstract <T> T visit(IGameEventVisitor<T> visitor)`
- Implementations: `GameEventCardAttachment`, `GameEventCardChangeZone`, `GameEventCardCounters`

### Listener Registration

Observer/Listener interfaces for state change callbacks.

- `forge-gui/src/main/java/forge/interfaces/ILobbyListener.java:6-11` - Callback interface
- Used in: `FServerManager.java`, `HostedMatch.java`, `GameLobby.java` (31 files use addListener)

### Trigger Handler

Centralized trigger management with suppression and delayed triggers.

- `forge-game/src/main/java/forge/game/trigger/TriggerHandler.java:46-50`
  - `suppressedModes` - Set of suppressed trigger types
  - `activeTriggers` - Currently active triggers
  - `delayedTriggers` - Triggers waiting to fire

### Command Pattern

Serializable runnable commands for deferred execution.

- `forge-game/src/main/java/forge/GameCommand.java:28-39` - `extends Serializable, Runnable`
- `forge-gui/src/main/java/forge/gui/UiCommand.java:28-29` - UI variant

## Predicate Factories

Filter logic encapsulated in static factory methods returning `Predicate<T>`.

- `forge-game/src/main/java/forge/game/card/CardPredicates.java:47` - `public static Predicate<Card> isController(final Player p)`
- `forge-game/src/main/java/forge/game/card/CardPredicates.java:53` - `public static Predicate<Card> isOwner(final Player p)`
- Also: `PlayerPredicates.java`, `CardTraitPredicates.java`

## Storage Abstraction

Pluggable storage backends with reader/writer interfaces.

- `forge-core/src/main/java/forge/util/storage/IStorage.java:26-40` - Storage interface
- `forge-core/src/main/java/forge/util/storage/StorageReaderBase.java` - Abstract base
- Implementations: `StorageReaderFile.java`, `StorageReaderFolder.java`, `StorageReaderRecursiveFolderWithUserFolder.java`

## Collections

### FCollection - Hybrid List/Set

Custom collection providing both List ordering and Set uniqueness.

- `forge-core/src/main/java/forge/util/collect/FCollection.java:24-46`
  - `implements List<T>, FCollectionView<T>, Cloneable, Serializable`
  - Internal `HashSet` for uniqueness, `ArrayList` for ordering

### Guava Usage

Heavy reliance on Google Guava throughout:
- `ImmutableList.builder()` for immutable collections
- `Table<K, K, V>` for two-dimensional mappings
- `Maps.newHashMap()`, `Maps.newLinkedHashMap()`, `Maps.newTreeMap()` factory methods

## Testing Patterns

### Base Test Class with Mocking

PowerMock setup for testing code with static initialization.

- `forge-gui-desktop/src/test/java/forge/gamesimulationtests/BaseGameSimulationTest.java:27-31`
  - `@PrepareForTest(value = { FModel.class, Singletons.class, ... })`
  - Provides `runGame()` helper for game simulation tests

### Test Naming

Test classes follow `*Test.java` naming parallel to source structure.

Examples: `AbilityKeyTest.java`, `ManaCostBeingPaidTest.java`, `DamageDealAiTest.java`

## Enum-Based Type Safety

Extensive enum usage for compile-time safety:

- `forge-game/src/main/java/forge/game/ability/ApiType.java` - API types
- `forge-game/src/main/java/forge/trackable/TrackableProperty.java` - Properties
- `forge-game/src/main/java/forge/game/card/CardStateName.java` - Card states

## Builder Pattern

Complex object construction uses builder pattern.

- `forge-gui/src/main/java/forge/gamemodes/limited/LimitedDeckBuilder.java` - Base builder
- Specialized: `BoosterDeckBuilder.java`, `SealedDeckBuilder.java`, `CardThemedDeckBuilder.java`

## Serialization

Core domain objects implement `Serializable` for persistence and network transmission.

- `forge-core/src/main/java/forge/deck/DeckBase.java:26` - `implements Serializable`
- `forge-game/src/main/java/forge/trackable/TrackableObject.java:13-14`
- `forge-game/src/main/java/forge/GameCommand.java:28` - Commands are serializable
