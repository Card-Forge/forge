# Architecture reference

Reference documentation for Forge architecture (currently GUI and network infrastructure only).

For code review guidelines (general principles, code style, network patterns, testing), see [Guidelines.md](Guidelines.md).

## Contents

- [GUI Architecture](#gui-architecture)
  - [Inheritance Hierarchy](#inheritance-hierarchy)
  - [Layer Responsibilities](#layer-responsibilities)
  - [Where Does My Code Go?](#where-does-my-code-go--decision-checklist)
  - [Red Flags](#red-flags--signs-youre-in-the-wrong-layer)
- [Network Architecture](#network-architecture)
  - [Server Lifecycle](#server-lifecycle)
  - [GUI Creation and the guis Map](#gui-creation-and-the-guis-map)
  - [Protocol Pipeline](#protocol-pipeline)
  - [Client Side](#client-side)

---

## GUI Architecture

This section covers the **in-match GUI** — the classes responsible for displaying and controlling an active game. It does not cover the full application UI (menus, lobby screens, deck editors, etc.).

### Inheritance Hierarchy

```
IGuiGame (interface, forge-gui)
  +-- AbstractGuiGame (abstract, forge-gui; also implements IMayViewCards)
       |-- CMatchUI (forge-gui-desktop) -- Swing desktop implementation
       |-- NetGuiGame (forge-gui) -- server-side network proxy
       +-- MatchController (forge-gui-mobile) -- libgdx mobile implementation
```

### Layer Responsibilities

##### `IGuiGame` — Interface Contract (forge-gui)
Defines the method signatures that any GUI implementation must provide. This is the
contract the game engine programs against. Changes here affect all platforms. No default
methods — every method must be implemented (or stubbed) by the concrete class.

##### `AbstractGuiGame` — Shared Game-UI State (forge-gui)
Implements `IGuiGame` and `IMayViewCards`. Platform-agnostic state management and
convenience methods. Contains:
- Player tracking (current player, local players, game controllers)
- Game state flags (pause, speed, daytime)
- Card visibility rules (`mayView`, `mayFlip`)
- UI state tracking (highlighted cards, selectable cards)
- Auto-pass / auto-yield state management
- Await-next-input timer mechanism (`awaitNextInput`/`cancelAwaitNextInput`)
- Choice/input convenience wrappers (`one()`, `many()`, `getInteger()`, etc.)
- Concede/spectator logic
- No-op stubs for optional interface methods (`refreshField`, `refreshCardDetails`, etc.)
- No-op stubs for optional network methods overridden in subclasses

**What does NOT belong here:** Anything that manages Swing/libgdx components, implements
platform-specific rendering, or uses platform-specific APIs. Simple text messages and
lightweight UI logic that is identical across all platforms *may* live here to avoid
duplication (prefer one implementation over two identical copies in subclasses). But if
the display differs per platform or uses platform APIs, it belongs in a subclass.

##### `CMatchUI` — Desktop Match Screen (forge-gui-desktop)
The Swing-based desktop implementation. Extends `AbstractGuiGame`. This is where desktop-specific display logic,
Swing component management, and screen coordination belong. Implements `ICDoc`
(controller) and `IMenuProvider`. Owns references to desktop panel controllers (`CAntes`,
`CCombat`, `CDependencies`, `CDetailPicture`, `CDev`, `CDock`, `CLog`, `CPrompt`,
`CStack`).

##### `MatchController` — LibGDX Match Screen (forge-gui-mobile)
The libgdx-based implementation. Extends `AbstractGuiGame`. Uses the singleton pattern
(`MatchController.instance`). Despite the module name, this is not just a mobile port — it
is a fully featured LibGDX implementation that also runs on desktop (via `forge-gui-mobile-dev`)
and aims for feature parity with the Swing UI. It is also the exclusive home of Adventure Mode
and Planar Conquest. LibGDX-specific display and interaction logic belongs here.

##### `V*` Views (forge-gui-desktop: `forge.screens.match.views`)
Pure Swing UI components (`VField`, `VHand`, `VPrompt`, `VStack`, etc.). Each panel
view implements `IVDoc<C*>` and defines how a panel *looks* — layout, Swing components,
rendering. Views hold a reference to their corresponding controller.

Note: `VMatchUI` is the top-level match screen view and implements `IVTopLevelUI` (not
`IVDoc`), so it follows a different pattern from the per-panel views.

##### `C*` Controllers (forge-gui-desktop: `forge.screens.match.controllers`)
Per-panel controllers (`CField`, `CHand`, `CPrompt`, `CLog`, etc.). Each implements
`ICDoc` and manages the behavior of its corresponding `V*` view. Controllers hold a
reference to `CMatchUI` and their `V*` view.

Exception: `CDetailPicture` is a composite controller that manages `CDetail` and
`CPicture` together. It does not itself implement `ICDoc`.

### Where Does My Code Go? — Decision Checklist

Before adding or modifying GUI code, work through this checklist top-to-bottom.
The first matching rule wins:

1. **Does it define a new capability the game engine needs from the UI?**
   Add the method signature to `IGuiGame`. Provide a concrete implementation in
   `AbstractGuiGame` if the logic is shared across platforms, otherwise leave it
   unimplemented there so each platform subclass (`CMatchUI`, `MatchController`) must
   provide its own.

2. **Is it shared game-UI state that both desktop and mobile need identically?**
   (e.g., tracking which cards are selectable, auto-yield flags, player controller mappings)
   `AbstractGuiGame`.

3. **Is it a convenience wrapper that delegates to abstract methods?**
   (e.g., `one()` calls `getChoices()`, `confirm()` calls overloaded `confirm()`)
   `AbstractGuiGame` — this is the template method pattern already used there.

4. **Does it involve network protocol or tracker synchronization?**
   The network-aware subclasses `CMatchUI`/`NetGuiGame`, or `AbstractGuiGame` with
   no-op stubs that subclasses override.

5. **Does it use platform-specific APIs, or does the display differ between desktop
   and mobile?**
   `CMatchUI` (desktop) or `MatchController` (mobile). However, simple text messages
   and lightweight UI logic that is identical across all platforms may live in
   `AbstractGuiGame` to avoid duplication — see rule #2.

6. **Does it coordinate multiple desktop panels or manage screen-level concerns?**
   (e.g., targeting overlay, floating zones, keyboard shortcuts, menus)
   `CMatchUI`.

7. **Does it control the behavior of a specific desktop UI panel?**
   The corresponding `C*` controller (e.g., `CPrompt`, `CField`, `CLog`).

8. **Does it define how a desktop panel looks — layout, Swing components, rendering?**
   The corresponding `V*` view (e.g., `VPrompt`, `VField`, `VLog`).

### Red Flags — Signs You're in the Wrong Layer

Some of these anti-patterns already exist in the codebase as technical debt. Do not add new instances of them.

- **Adding `javax.swing.*` or `java.awt.*` imports to anything in `forge-gui/`.**
  The `forge-gui` module is shared across platforms. Swing imports mean desktop-specific
  code that belongs in `forge-gui-desktop`.

- **Adding platform-specific display logic to `AbstractGuiGame`.**
  Code that uses Swing/libgdx APIs or differs between desktop and mobile belongs in
  `CMatchUI` or `MatchController`. Simple text messages identical across platforms are
  acceptable in `AbstractGuiGame` to avoid duplication.

- **Checking `GuiBase.getInterface().isLibgdxPort()` in `AbstractGuiGame` to branch
  on platform.**
  Platform-specific branches should be handled by overriding methods in the appropriate
  subclass, not by runtime platform checks in the shared base. (The `isLibgdxPort()`
  checks in `setCurrentPlayer()` and `mayView()` already violate this — do not extend
  the pattern.)

- **Putting game-state logic (auto-yield decisions, controller management) in a `V*`
  view class.**
  Views are for layout and rendering. State logic goes in the corresponding `C*`
  controller or `CMatchUI`.

- **Moving local-only logic into a shared layer without checking network and side-effect implications.**
  When refactoring code from a subclass into a shared class (`AbstractGuiGame`), check two things: (1) whether the shared layer serializes state changes over the network — what was a harmless local update may become constant network traffic; (2) whether methods in the shared class have side effects that interfere with the new logic (e.g., a display update method that cancels the timer calling it). Trace the full call chain within the class.

---

## Network Architecture

This section covers the network play infrastructure.

### Server Lifecycle

`FServerManager` is the server-side singleton that manages the Netty server, client
connections, and the bridge between the game engine and remote clients.

- **Startup:** `FServerManager.listen()` binds a Netty server socket. When a client
  connects, `channelActive()` fires and a `RemoteClient` is created, keyed by `Channel`
  in the `clients` map (`Map<Channel, RemoteClient>`).
- **Login:** The client sends a `LoginEvent` with username, avatar, and sleeve index.
  `handleLogin()` calls `ServerGameLobby.connectPlayer()` which fills the first `OPEN`
  slot, changing it to `REMOTE`.
- **RemoteClient:** Wraps a Netty `Channel` with the client's username and slot index.
  The slot index is assigned at login and identifies the player for the lifetime of the
  connection. `RemoteClient` implements `IToClient` for sending messages.

### GUI Creation and the guis Map

When a game starts, `GameLobby.startGame()` builds the **guis map**
(`Map<RegisteredPlayer, IGuiGame>`) — the authoritative mapping from player to GUI:

- For each lobby slot, `ServerGameLobby.getGui(index)` delegates to
  `FServerManager.getGui(index)`.
- `LOCAL` slots get a new `CMatchUI` (desktop) or `MatchController` (mobile).
- `REMOTE` slots get a `new NetGuiGame(client, index)` — finds the `RemoteClient` matching
  the slot index.
- `AI` slots get no GUI entry.

The guis map is stored in `HostedMatch.startMatch()` and never rebuilt during the game.
`HostedMatch.getGuiForPlayer(Player)` looks up by `player.getRegisteredPlayer()` — this
uses **identity equality** (Object default), so the `RegisteredPlayer` instance must be
the same object used as the key, not a copy.

`NetGuiGame` is the server-side proxy: every `IGuiGame` method call is serialized via
`GameProtocolSender` and sent to the remote client over the Netty channel. It stores its
`slotIndex` for reliable identification (player names are unreliable — see the
stable identifiers guideline in [Guidelines.md](Guidelines.md)).

### Protocol Pipeline

Messages flow through the Netty pipeline as serialized Java objects:

```
Server (game thread)                          Client
  |                                             |
  |-- NetGuiGame.send(method, args)             |
  |    +-- GameProtocolSender                   |
  |         +-- RemoteClient.send()             |
  |              +-- Channel.writeAndFlush()    |
  |                   -- [Netty wire] -->       |
  |                                     GameProtocolHandler.channelRead()
  |                                       |-- beforeCall() [IO thread]
  |                                       +-- method.invoke() [EDT]
```

**Critical threading detail:** `GameProtocolHandler.channelRead()` calls `beforeCall()`
synchronously on the Netty IO thread, then dispatches the actual GUI method to the EDT
via `FThreads.invokeInEdtNowOrLater()`. This means `beforeCall()` for message N+1 can
execute before the EDT has processed message N's GUI method. Code in `beforeCall()` must
not read state that is set by a previous message's EDT-dispatched method — use fields set
within `beforeCall()` itself instead.

Replies (for `sendAndWait` calls like `getChoices`, `confirm`) flow back through the
`ReplyPool`. The server blocks on `ReplyPool.get()` until the client responds.

### Client Side

- **`FGameClient`:** Connects to the server, sends `LoginEvent`, manages the Netty
  channel. Holds references to the local `IGuiGame` and lobby listeners.
- **`GameClientHandler`:** Extends `GameProtocolHandler<IGuiGame>`. Its `beforeCall()`
  builds local state that the client needs for display: on `openView`, it creates a local
  `Match`, `Game`, and `Tracker` from lobby data. These local objects exist only so the
  client's `CMatchUI` can function — the server remains the source of truth.
- **Lobby:** `ClientGameLobby` mirrors the server lobby state. The server sends
  `LobbyUpdateEvent`s to keep it in sync. Slot data (names, decks, avatars) is used by
  `GameClientHandler` to create `RegisteredPlayer` objects for the local Match.

---

## Further Reading

- **[AGENTS.md](../../AGENTS.md)** - AI coding agent configuration (project overview, build commands)
- **[Guidelines.md](Guidelines.md)** - Code review guidelines distilled from PR feedback
