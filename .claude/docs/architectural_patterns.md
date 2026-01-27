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

### GUI Initialization Levels

Different test scenarios require different initialization levels:

**Level 1: FModel only (headless)**
```java
GuiBase.setInterface(new GuiDesktop());
FModel.initialize(null, preferences -> {
    preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
    return null;
});
```
- Works without display server
- Sufficient for: card loading, deck operations, metrics, server start/stop
- `Singletons.getControl()` returns null

**Level 2: Full Singletons (requires display)**
```java
Singletons.initializeOnce(true);  // Sets up view AND control
```
- Requires display server (Xvfb on Linux, real display on Windows)
- Required for: full game execution via standard `GuiDesktop.hostMatch()`
- Sets up `Singletons.getControl()` and `Singletons.getView()`

**Level 3: Headless Full Game (no display required)**
```java
GuiBase.setInterface(new HeadlessGuiDesktop());
FModel.initialize(null, preferences -> {
    preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
    return null;
});
```
- **No display server required** - works in CI/CD environments
- `HeadlessGuiDesktop` overrides methods that require `Singletons.getControl()`
- `NoOpGuiGame` provides stub implementation of `IGuiGame` (~80 methods)
- Sufficient for: full AI-vs-AI game execution via network infrastructure
- Log files include `-test` suffix to distinguish from production logs

**Key Insight**: `ServerGameLobby.startGame()` returns a Runnable that calls `GuiBase.getInterface().hostMatch()`. Standard `GuiDesktop.hostMatch()` requires `Singletons.getControl()`, but `HeadlessGuiDesktop.hostMatch()` bypasses this requirement.

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

## Network Synchronization (NetworkPlay Branch)

### Delta Sync Architecture

The network play system uses delta synchronization to minimize bandwidth:

**Key Components:**
- `DeltaSyncManager` - Collects and sends only changed properties
- `DeltaPacket` - Contains object deltas, new objects, removed objects, and periodic checksum
- `FullStatePacket` - Complete game state for initial sync or recovery

**Sync Flow:**
1. Server collects changes via `collectDeltas(GameView)`
2. Changes serialized to `DeltaPacket` with sequence number
3. Client applies deltas, validates checksum every 20 packets (CHECKSUM_INTERVAL)
4. On checksum mismatch, client requests full state resync

**Key Files:**
- `forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java` - Server-side delta collection
- `forge-gui/src/main/java/forge/gamemodes/net/NetworkGuiGame.java` - Client-side delta application (~850 lines)
- `forge-gui/src/main/java/forge/gamemodes/net/DeltaPacket.java` - Delta packet structure

### NetworkGuiGame Class Hierarchy

Network-specific deserialization logic is isolated into a dedicated subclass hierarchy:

```
AbstractGuiGame (core game logic, ~900 lines)
    ↑ extends
NetworkGuiGame (abstract - network deserialization, ~850 lines)
    ↑ extends
├── NetGuiGame (server-side network proxy for remote clients)
└── CMatchUI (desktop GUI - also extends NetworkGuiGame for network support)
```

**AbstractGuiGame** (`forge-gui/.../match/AbstractGuiGame.java`):
- Core local game functionality
- Player controller management
- Basic IGuiGame interface stubs for network methods

**NetworkGuiGame** (`forge-gui/.../net/NetworkGuiGame.java`):
- **Abstract class** - subclasses must implement UI-specific methods
- Network delta deserialization (`applyDelta()`, `fullStateSync()`)
- Tracker initialization for deserialized objects
- Reconnection handling
- All network-specific logic isolated here

**NetGuiGame** (`forge-gui/.../net/server/NetGuiGame.java`):
- Server-side proxy representing remote client
- Sends protocol messages to client via `GameProtocolSender`
- Extends NetworkGuiGame for consistent network handling

**Benefits of this architecture:**
- AbstractGuiGame stays free of network bloat (~40 lines of stubs vs ~850 lines)
- Network code clearly separated and maintainable
- Master branch merges have minimal conflicts in AbstractGuiGame
- CMatchUI inherits network support through NetworkGuiGame

### Client-Side Delta Application

The client applies deltas received from the server in `NetworkGuiGame.applyDelta()`:

**Application Order:**
1. **New Objects** - Create objects that don't exist yet via `createObjectFromData()`
2. **Object Deltas** - Apply property changes to existing objects via `applyDeltaToObject()`
3. **Removed Objects** - Remove objects that no longer exist
4. **Checksum Validation** - Every CHECKSUM_INTERVAL (20) packets, validate state matches server

**Key Methods** (all in `NetworkGuiGame`):
- `applyDelta(DeltaPacket packet)` - Main entry point for applying deltas
- `createObjectFromData(NewObjectData data, Tracker tracker)` - Creates new CardView/PlayerView/etc.
- `applyDeltaToObject(TrackableObject obj, byte[] deltaBytes, Tracker tracker)` - Applies property changes
- `findObjectByTypeAndId(Tracker tracker, int objectType, int objectId)` - Type-specific lookup (critical!)

**Important:** When network bugs occur, check BOTH:
- Server-side: `DeltaSyncManager` - How deltas are collected and sent
- Client-side: `NetworkGuiGame` - How deltas are received and applied

Many bugs require fixes on both sides (e.g., composite delta keys needed updates in both DeltaSyncManager's tracking sets AND NetworkGuiGame's lookup methods).

### Checksum Validation

Periodic checksums detect state desynchronization:

```java
// Checksum includes: gameView.getId(), getTurn(), getPhase(), player.getId(), player.getLife()
private int computeStateChecksum(GameView gameView) {
    int hash = 17;
    hash = 31 * hash + gameView.getId();
    hash = 31 * hash + gameView.getTurn();
    // ... etc
}
```

**Important:** Checksum must be computed immediately after collecting deltas, before any other operations that could allow game state to change. See Concurrency Patterns below.

### Cross-JVM Serialization Pitfall

**Critical:** When comparing values across JVM processes (server vs client), never use identity-based methods.

**Problem Pattern:**
```java
// BAD: hashCode() on enums returns identity hash - differs between JVMs!
hash = 31 * hash + gameView.getPhase().hashCode();  // Server: 123456, Client: 789012
```

**Solution Pattern:**
```java
// GOOD: ordinal() returns enum position - consistent across all JVMs
hash = 31 * hash + gameView.getPhase().ordinal();   // Server: 1, Client: 1
```

**Why this matters:**
- Server and client are separate JVM processes
- `Object.hashCode()` (default for enums) returns identity-based hash
- Same enum constant (e.g., `PhaseType.UPKEEP`) has different identity in each JVM
- This causes checksums to always mismatch, triggering unnecessary resyncs

**Safe cross-JVM comparison methods:**
- `enum.ordinal()` - Position in declaration (0, 1, 2...)
- `enum.name()` - String name of constant
- Primitive values (`int`, `long`, etc.)
- Custom `hashCode()` implementations using only the above

**Unsafe for cross-JVM comparison:**
- `Object.hashCode()` (identity-based)
- `System.identityHashCode()`
- Object references

### Composite Delta Key Pattern

Delta packets use maps keyed by object ID, but different object types (CardView, PlayerView, StackItemView) have separate ID counters that can collide. The solution uses composite keys encoding both type and ID:

```java
// Key encoding: (type << 28) | (id & 0x0FFFFFFF)
// Upper 4 bits = object type (0-15)
// Lower 28 bits = object ID (up to 268M objects)

private static int makeDeltaKey(int type, int id) {
    return (type << 28) | (id & 0x0FFFFFFF);
}

// Type constants:
// 0 = CardView
// 1 = PlayerView
// 2 = StackItemView
// 3 = CombatView
// 4 = GameView (also uses GAMEVIEW_DELTA_KEY = Integer.MIN_VALUE)
```

**Examples:**
- CardView id=5 → key=0x00000005
- PlayerView id=5 → key=0x10000005
- StackItemView id=5 → key=0x20000005

This prevents ID collisions in `objectDeltas` and `newObjects` maps.

### Type-Specific Object Lookup Pattern

**Critical:** When looking up objects by ID, always use type-specific lookup to avoid ID collisions between different object types.

**Problem Pattern:**
```java
// BAD: Searches ALL types - CardView ID=1 might return PlayerView ID=1
TrackableObject obj = findObjectById(tracker, objectId);
```

**Solution Pattern:**
```java
// GOOD: Looks up specific type only
TrackableObject obj = findObjectByTypeAndId(tracker, objectType, objectId);
```

**Implementation:**
```java
private TrackableObject findObjectByTypeAndId(Tracker tracker, int objectType, int objectId) {
    switch (objectType) {
        case DeltaPacket.TYPE_CARD_VIEW:
            return tracker.getObj(TrackableTypes.CardViewType, objectId);
        case DeltaPacket.TYPE_PLAYER_VIEW:
            return tracker.getObj(TrackableTypes.PlayerViewType, objectId);
        // ... other types
    }
}
```

**Why this matters:** CardView, PlayerView, StackItemView, etc. all have separate ID counters starting from 0/1. Without type-specific lookup:
- CardView ID=1 and PlayerView ID=1 are different objects
- Generic lookup might find PlayerView when CardView was intended
- Applying CardView properties to PlayerView causes ClassCastException

### TrackableObject Change Tracking

View objects extend `TrackableObject` which tracks property changes:

- `hasChanges()` - Check if any properties changed since last sync
- `getChangedProps()` - Get set of changed property names
- Properties marked dirty when setters called
- Changes cleared after serialization for delta sync

### Per-Client Change Tracking (Multi-Client Bug Fix)

**Critical for 3-4 player games:** Multiple remote clients share the same `GameView` object on the server. Each client has its own `NetGuiGame` with its own `DeltaSyncManager`.

**Problem Pattern:**
```java
// BAD: First client clears changes, subsequent clients see nothing
// Client 1's NetGuiGame:
collectDeltas(gameView);     // Sees hasChanges() = true
sendDeltas();
clearAllChanges(gameView);   // Clears global change flags on shared GameView

// Client 2's NetGuiGame (called immediately after):
collectDeltas(gameView);     // Sees hasChanges() = false - changes already cleared!
// Client 2 receives no updates, causes checksum mismatch
```

**Solution Pattern:**
```java
// GOOD: Per-client tracking via property checksums
// Each DeltaSyncManager maintains:
Map<Integer, Map<Integer, Integer>> lastSentPropertyChecksums;
// Key: composite delta key (type + id)
// Value: map of property ordinal to checksum of last-sent value

// Instead of relying on hasChanges():
private byte[] serializeChangesPerClient(int deltaKey, TrackableObject obj) {
    Map<Integer, Integer> lastChecksums = lastSentPropertyChecksums.get(deltaKey);
    // Compare current property values to last-sent checksums
    // Send only properties that differ FOR THIS CLIENT
    // Update lastSentPropertyChecksums after sending
}
```

**Key Implementation Points:**
1. `DeltaSyncManager.lastSentPropertyChecksums` - Per-client tracking map
2. `recordPropertyChecksums()` - Called in `markObjectsAsSent()` during initial sync
3. `serializeChangesPerClient()` - Compares current vs last-sent, independent of global `hasChanges()`
4. `clearAllChanges()` is NO LONGER called in `NetGuiGame.updateGameView()` - each client tracks independently

**Why this matters:**
- 2-player games: Only 1 remote client, so `clearAllChanges()` doesn't affect others
- 3-4 player games: Multiple clients share GameView; first client's clear breaks others
- Symptom: Clients stuck at Turn 0, Hand=0, checksum always matches initial state

**Files:**
- `DeltaSyncManager.java` - `lastSentPropertyChecksums`, `serializeChangesPerClient()`
- `NetGuiGame.java` - Removed `clearAllChanges()` call from `updateGameView()`

## Concurrency Patterns

### Game Thread vs Network Thread

Network play involves multiple threads:
- **Game Thread (Game-0)** - Runs game logic, calls `chooseSpellAbilityToPlay()`, blocks on player input
- **Network Thread** - Handles message sending/receiving, delta sync
- **UI Thread** - Updates display based on game state

### Input Blocking with CountDownLatch

Human player input uses `CountDownLatch` to block game thread:

```java
// InputSyncronizedBase pattern
cdlDone = new CountDownLatch(1);
cdlDone.await();  // Blocks until input received or stop() called
```

**Critical:** When converting player to AI, must:
1. Replace controller FIRST
2. THEN clear inputs (release latch)

Wrong order causes race condition where game thread calls old controller after latch release.

### Race Condition: Checksum Computation

**Problem Pattern:**
```java
// BAD: State can change between collecting and checksumming
collectDeltas(gameView);        // Collect changes
doBookkeeping();                // Other operations - game state may change here!
checksum = computeChecksum();   // Checksum reflects changed state, not collected deltas
```

**Solution Pattern:**
```java
// GOOD: Checksum computed immediately after collection
collectDeltas(gameView);        // Collect changes
checksum = computeChecksum();   // Checksum matches collected state
doBookkeeping();                // Now safe to do other operations
```

### Controller Replacement Race Condition

**Problem:** When replacing a human controller with AI:
```java
// BAD: Game thread may call old controller after latch release
clearInputs();                          // Releases game thread
createAiController();                   // Game thread running, may call old controller!
player.setController(aiController);     // Too late - old controller already called
```

**Solution:**
```java
// GOOD: Replace controller before releasing game thread
createAiController();
player.setController(aiController);     // New calls go to AI
clearInputs();                          // Now safe to release game thread
```

### Thread-Safe Collections

Use concurrent collections for cross-thread access:
- `ConcurrentHashMap` for shared maps
- `AtomicLong` for sequence numbers (see `DeltaSyncManager.sequenceNumber`)

## Automated Network Testing Infrastructure

Test infrastructure for automated network play testing. All files in `forge-gui-desktop/src/test/java/forge/net/`.

### Component Overview

| Component | Purpose |
|-----------|---------|
| `HeadlessGuiDesktop` | Extends `GuiDesktop`, bypasses `Singletons.getControl()` for headless execution |
| `NoOpGuiGame` | No-op `IGuiGame` implementation (~80 methods) for AI spectating |
| `TestDeckLoader` | Loads quest precon decks via existing `DeckSerializer` |
| `AutomatedGameTestHarness` | Orchestrates network game execution with `FServerManager` |
| `NetworkAIPlayerFactory` | Configures AI players in lobby slots |
| `GameTestMetrics` | Collects metrics from `NetworkByteTracker` |
| `ConsoleNetworkTestRunner` | CLI entry point with `--games N` argument |
| `BatchGameTest` | TestNG entry point for sequential and parallel batch tests |
| `AutomatedNetworkTest` | TestNG test class validating infrastructure |
| `MultiplayerNetworkScenario` | 3-4 player games with actual remote HeadlessNetworkClient connections |
| `ComprehensiveGameRunner` | Standalone runner for 2-4 player games, used by multi-process execution |
| `ComprehensiveTestExecutor` | Orchestrates 100+ games with configurable player count distribution |

### Usage Pattern

```java
// Component tests (headless - FModel.initialize only)
TestDeckLoader.getRandomPrecon();           // Load deck
GameTestMetrics metrics = new GameTestMetrics();  // Track results
FServerManager.getInstance().startServer(port);   // Server operations

// Full game tests (uses HeadlessGuiDesktop - no display server required)
AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
GameTestMetrics result = harness.runBasicTwoPlayerGame();
```

### Test Command

```bash
mvn -pl forge-gui-desktop -am test -Dtest=forge.net.AutomatedNetworkTest
```

### Async Game Execution Pattern

**Critical Discovery**: Games run asynchronously in a thread pool, not synchronously.

When `lobby.startGame().run()` is called:
1. `HostedMatch.startGame()` calls `game.getAction().invoke(proc)`
2. `invoke()` checks if on game thread; if not, calls `ThreadUtil.invokeInGameThread(proc)`
3. `invokeInGameThread()` submits to `ExecutorService` and **returns immediately**
4. Game runs in background thread while `run()` returns

**Problem**: Test harness returns before game completes, showing 0 turns.

**Solution**: Poll for game completion:
```java
// Wait for match to have outcomes (game finished)
while (waitedMs < maxWaitSeconds * 1000) {
    Match match = hostedMatch.getMatch();
    if (match != null) {
        Collection<GameOutcome> outcomes = match.getOutcomes();
        if (outcomes != null && !outcomes.isEmpty()) {
            break;  // Game completed
        }
    }
    Thread.sleep(100);
    waitedMs += 100;
}
```

### Key API Patterns Discovered

| Pattern | Correct Usage |
|---------|---------------|
| Configure AI player | `slot.setType(LobbySlotType.AI)` (no `addAIPlayer()` method) |
| Set lobby on server | `server.setLobby(lobby)` (no getter, must keep reference) |
| Start game | `Runnable start = lobby.startGame(); if (start != null) start.run();` |
| Wait for completion | Poll `match.getOutcomes()` until non-empty (games run async!) |
| Avoid port conflicts | Use `AtomicInteger` port counter, increment per test |

See `.claude/docs/automated_testing_plan.md` for detailed implementation plan and status.

### Test Logging Convention

Test infrastructure uses `NetworkDebugLogger` for game/network operation logging, which writes to both console and log files with `-test` suffix.

**Use `NetworkDebugLogger` for:**
- Game execution progress (`[ReconnectionScenario] Starting game...`)
- Server lifecycle (`[MultiplayerScenario] Server started on port 57000`)
- Game results (`[AutomatedGameTestHarness] Game lasted 12 turns`)
- Errors during game execution

**Use `System.out` for:**
- CLI test runner output (test summaries, pass/fail counts)
- User-facing messages in `main()` methods
- Test configuration display

**Example:**
```java
// In scenario execute() method - use NetworkDebugLogger
NetworkDebugLogger.log("[MultiplayerScenario] Starting %d-player game", playerCount);
NetworkDebugLogger.error("[MultiplayerScenario] Error: " + e.getMessage(), e);

// In CLI runner main() - use System.out for user-facing output
System.out.println("Test Summary");
System.out.println("Passed: " + passed);
```

## Common Java Pitfalls

### Java Record Accessor Syntax

Java records use method accessors, not field access:

```java
// WRONG: Field access syntax
GameOutcome outcome = event.result;    // Compile error

// CORRECT: Method accessor syntax
GameOutcome outcome = event.result();  // Records use method calls
```

This applies to all record components. If a record is defined as `record GameEvent(GameOutcome result, Player winner)`, access uses `event.result()` and `event.winner()`.

### Collection vs List Return Types

Many Forge APIs return `Collection<T>` rather than `List<T>`. When you need a `List`:

```java
// WRONG: Type mismatch
List<GameOutcome> outcomes = match.getOutcomes();  // Returns Collection, not List

// CORRECT: Wrap in ArrayList
List<GameOutcome> outcomes = new ArrayList<>(match.getOutcomes());
```

Always check the return type of getter methods, especially in match/game outcome contexts.

### RegisteredPlayer vs Player Name Access

`GameOutcome.getWinningPlayer()` returns `RegisteredPlayer`, not `Player`. To get the player name:

```java
// WRONG: RegisteredPlayer doesn't have getName()
String winner = game.getOutcome().getWinningPlayer().getName();  // Compile error

// CORRECT: Access through LobbyPlayer
String winner = game.getOutcome().getWinningPlayer().getPlayer().getName();
```

**Class hierarchy:**
- `RegisteredPlayer` - Deck and game configuration for a player slot
  - `getPlayer()` → `LobbyPlayer` - The actual player identity
    - `getName()` → `String` - Player's display name

### Timestamp Precision in File Filtering

When filtering files by creation time using timestamps extracted from filenames, beware of precision mismatch:

```java
// WRONG: LocalDateTime.now() has nanosecond precision, filenames have second precision
LocalDateTime testStartTime = LocalDateTime.now();  // 13:27:18.192405500
// Filename: network-debug-run20260125-132718-... extracts to 13:27:18.000000000
// Filter: !logTime.isBefore(afterTime) → 13:27:18.000 < 13:27:18.192 → EXCLUDED!

// CORRECT: Truncate to match filename precision
LocalDateTime testStartTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
// Now: 13:27:18.000000000, filename: 13:27:18.000000000 → INCLUDED
```

**Why this matters:**
- Log filenames use `yyyyMMdd-HHmmss` format (second precision)
- `LocalDateTime.now()` captures nanoseconds
- Same-second files get excluded because `.000` < `.192`
- Symptom: "Found 100 files, Filtered to 0 logs"

### Log Cleanup Configuration for Testing

`NetworkDebugConfig` controls automatic log cleanup. For testing, cleanup is **disabled by default**:

```java
// In NetworkDebugConfig.java
private static final int DEFAULT_MAX_LOG_FILES = 0;  // 0 = no limit
private static final boolean DEFAULT_LOG_CLEANUP_ENABLED = false;
```

**Why disabled for testing:**
- Comprehensive tests generate 100+ log files
- Cleanup runs when new logs are created
- With limit=20, running quick test after comprehensive test deletes 80+ logs
- Java's `Files.delete()` bypasses Recycle Bin - logs are permanently lost

**To enable cleanup in production:** Set in `NetworkDebug.config`:
```
debug.logger.cleanup.enabled=true
debug.logger.max.logs=20
```
