# Code review guidelines

These guidelines are distilled from PR review feedback. Following them avoids the most common causes of PR rejection.

For architecture reference (GUI hierarchy, layer responsibilities, network infrastructure), see [Architecture.md](Architecture.md).

## Contents

- [General Principles](#general-principles)
- [Code Style](#code-style)
- [Architecture](#architecture)
- [Domain-Specific (MTG Rules)](#domain-specific-mtg-rules)
- [Network-Specific Guidelines](#network-specific-guidelines)
  - [Design](#design)
  - [Implementation](#implementation)
  - [Verification](#verification)
- [Testing](#testing)

## General Principles

- **Keep it simple / avoid over-engineering:** Simplest approach that works. Don't introduce new classes, abstractions, or helper methods called from one place when existing infrastructure suffices. Inline logic at the call site. Prefer modifying 3 files over creating 10. Don't introduce callback interfaces or delegate patterns when the caller can access the data directly (e.g., `matchUI.getGameView().getPlayers().size()` is better than adding a new `getPlayerCount()` to a callback interface).
- **Minimal diff:** Prefer small, focused changes over large refactors. The fewer lines changed, the easier to review and less risk of introducing bugs. Do not make cosmetic fixes (whitespace, formatting, style) to code that isn't otherwise being changed for functional reasons — it creates diff noise and draws reviewer scrutiny to unrelated code.
- **Search before creating / avoid duplication:** Before implementing new functionality, search the codebase for existing mechanisms that solve the same or a similar problem. Enhance what exists rather than creating parallel systems. **Exception:** More specific guidelines (particularly in [Network-Specific Guidelines](#network-specific-guidelines)) may require intentional duplication — e.g., computing timers or derived state independently on each side of a client-server boundary to avoid network traffic.
- **Trace execution contexts:** Enumerate where the code runs: local single-player, local multiplayer, network host, network client, AI. Verify correctness in each.
- **Trace callers before modifying:** Before changing a method's behavior, search for all call sites and understand the contexts they run in. A method called from one place is safe to change; a method called from network callbacks, UI threads, and game logic simultaneously needs careful consideration. For network code, trace the full path: who sends, what serializes it, what deserializes it, who receives.
- **Check for dead code:** Remove dead code *caused by your change* in the same commit — don't hunt pre-existing dead code.
- **Don't write workarounds — flag them:** If existing code doesn't match its contracts, flag the inconsistency rather than adding defensive code.
- **Preserve existing guards when changing types:** Don't silently remove guards when refactoring types. Query the data at the use-site if reachable; otherwise carry it forward as an additional field.

## Code Style

- **Add @Override annotations:** When implementing interface methods, always add `@Override` annotation.
- **Wrap parseInt/parseLong in try-catch:** System property parsing should handle `NumberFormatException` gracefully with fallback to defaults.
- **Meaningful toString():** Classes used in logging/debugging should override `toString()` rather than inheriting from Object.
- **Intuitive naming:** Names of files, classes, and methods should communicate purpose without needing to read the implementation. Prefer `hasNetGame()` over `checkNet()`, `cancelAwaitNextInput()` over `resetTimer()`, `NetGuiGame` over `NetGG`. Names should describe *what* they answer or do, not *how* — a reader skimming a call site or class hierarchy should understand the intent immediately.
- **`this` in anonymous classes/lambdas:** Inside anonymous inner classes (e.g., `TimerTask`, `Runnable`) and lambdas nested within them, `this` refers to the anonymous class, not the enclosing class. Use `EnclosingClass.this` (e.g., `AbstractGuiGame.this`) when passing the outer instance.
- **Inline comments should be durable:** Comments must make sense to any contributor reading the code in isolation. Don't reference conversations, branch names, internal documents, or session-specific debugging labels (e.g., `// Path A`, `// this was the bug`). When giving examples in comments, keep them generic so they don't go stale.
- **Use semantic actions, not UI simulation:** When triggering game actions programmatically (e.g., from keyboard shortcuts or automated logic), call the semantic method (`passPriority()`, `concede()`) rather than simulating a UI click (`btnOK.doClick()`). UI simulation can trigger unintended side effects because the button's current meaning depends on context.
- **View-type overloads should delegate, not duplicate:** When both an engine-type overload (e.g., `updateZone(Player, ZoneType)`) and a view-type overload (e.g., `updateZone(PlayerView, ZoneType)`) exist, the engine-type one must delegate to the view-type one — not duplicate its logic. The view-type overload is the canonical implementation; the engine-type one is a convenience wrapper that converts and forwards.
- **Localize user-facing strings:** Use `Localizer.getInstance().getMessage()` for all user-facing text. Add new keys to `forge-gui/res/languages/en-US.properties`. Never hardcode English strings in Java code that will be displayed to the user.
- **Null-guard changed contracts:** When tightening a condition, trace downstream callers that assumed non-null.
- **Check hotkey conflicts:** When assigning keyboard shortcuts, search for `VK_F[key]` and `getKeyStroke` in the codebase to ensure no conflicts with hardcoded menu accelerators (e.g., F1=Help, F11=Fullscreen), and check default shortcut preferences in `ForgePreferences` for collisions.

## Architecture

- **Gate expensive work behind checks:** Expensive operations (iterating all cards, getting all abilities) should only be performed when actually needed, not proactively or on every update cycle. If a feature is preference-gated, the computation must also be gated.
- **Preference-gated features:** Gate the **entire modified flow** behind a single check at the top. When OFF, the original code path must execute unchanged — same loop bounds, same assignments, same paths. Common mistake: changing the original logic (e.g., reducing a loop range) and relying on the new feature code to compensate, then gating only the new code — this leaves the original flow broken when OFF.
- **Don't expand interfaces for trivial access:** Check whether data is reachable through existing object graphs before adding methods to `IGuiGame` or similar interfaces.
- **Keep engine clean:** GUI-specific logic (UI hints, styling) belongs in View classes, not in forge-game engine classes like Player.java or PhaseHandler.java.
- **Fix bugs at the closest layer:** Errors and bug fixes should be solved in the closest layer that is effective. For example, a network serialization issue should be fixed in the network layer, not by adding guards in the game engine. A card rules bug belongs in forge-game, not worked around in forge-gui. Fixing at the source keeps the codebase clean and avoids defensive code proliferating through unrelated layers.
- **Check platform counterparts:** When fixing a bug in `CMatchUI` (desktop), check `MatchController` (mobile) for the same issue, and vice versa.
- **Platform-neutral code for platform-neutral features:** If a feature is intended to work across platforms (desktop and mobile), implement the *state and logic* in shared code (e.g., `AbstractGuiGame`, `forge-gui`) rather than in platform-specific classes. Display that uses platform-specific APIs (Swing components, libgdx widgets) belongs in platform subclasses (`CMatchUI`, `MatchController`). However, simple text messages and lightweight UI logic that is identical across platforms may live in `AbstractGuiGame` to avoid duplication — prefer one implementation in the base class over two identical copies in subclasses. **Code smell:** If you find yourself writing the same algorithm with the same state fields in both `CMatchUI` and `MatchController`, that's a strong signal it belongs in `AbstractGuiGame` instead.
- **Check for mobile GUI:** Desktop-only features in platform-neutral code (e.g., `PlayerControllerHuman`, event handlers) should check `GuiBase.getInterface().isLibgdxPort()` and return early for mobile. However, in `AbstractGuiGame` and its subclasses, prefer subclass overrides over runtime platform checks — see [Architecture Red Flags](Architecture.md#red-flags--signs-youre-in-the-wrong-layer).
- **Zone transitions involve two players, not one:** When a card changes zones, the "from" zone and "to" zone may belong to different players (e.g., a stolen creature dying moves from the controller's battlefield to the owner's graveyard). Don't assume the card's owner or current controller is the correct player for both zones — use each zone's actual player reference.
- **Reuse `TrackableProperty` constants across view types:** Each `TrackableObject` instance has its own property map, so different view classes (e.g., `SpellAbilityView` and `StackItemView`) can share the same `TrackableProperty` enum constant when the semantics and type match. Don't create prefixed duplicates (e.g., `SA_Foo`) when an unprefixed constant (`Foo`) already exists with the same `TrackableType`. The enum name is just a key — it doesn't imply ownership by a particular view class.

## Network-Specific Guidelines

### Design

- **Isolate network code:** Network-specific functionality should be in dedicated classes (`NetGuiGame`, `NetGameController`) rather than added to core classes like `AbstractGuiGame`. Keep core game classes free of network dependencies so they remain usable in non-network contexts.
- **Account for client-server asymmetry:** A network match has three execution contexts, not two: (1) the **host's local GUI** (`CMatchUI`) — sees full game state but displays locally; (2) the **server-side proxy** (`NetGuiGame`) — serializes `IGuiGame` calls over the wire to the remote client; (3) the **remote client** (`CMatchUI` receiving protocol calls) — has only a proxy view of game state. When branching on network status, verify the behavior is correct in all three contexts. The host's local GUI is the most commonly forgotten — it participates in the match alongside `NetGuiGame` but is a separate `IGuiGame` instance.
- **Design from the client's perspective first** — the client is the constrained side. Ask: "What does the client need to know, and how will it receive that information?" If a feature requires data the client doesn't have, the server must explicitly provide it (via protocol messages or lobby initialization). Don't assume that because something is reachable on the server, it's also reachable on the client.
- **Use stable identifiers for player lookup:** Slot indices or GUI type, not `Player.getName()` (names get deduplicated).
- **Distinguish events from continuous state:** One-time transitions → single network event. Continuous state (timers, animations) → compute independently on each side. Never stream tick-by-tick updates. This intentional duplication overrides [Search before creating](#general-principles) — independent client/server implementations is correct when sharing would create recurring network traffic.
- **Prefer forwarding game events over adding protocol methods:** Forward `GameEvent` records for client-side processing via `IGameEventVisitor`. Avoid new per-feature `ProtocolMethod` entries.

### Implementation

- **Preserve message ordering:** Follow the same message order as `HostedMatch.startGame()`. Out-of-order messages cause silent failures.
- **Serialization compatibility:** Changes to objects serialized over the network (anything in `TrackableProperty`, lobby messages) must maintain backwards compatibility or include version-aware migration logic.
- **Thread safety:** Network callbacks execute on Netty threads, not the game thread. Access to shared state (e.g., `gameControllers`, `gameView`, tracker collections) from network callbacks must be synchronized or delegated to the game thread via `FThreads.invokeInEdtAndWait()` or equivalent.
- **Use `GuiBase.isNetworkplay(game)` for network detection:** There is only one signature — `isNetworkplay(IGuiGame game)`. When a game reference is available, pass it; the method delegates to `game.isNetGame()` for a per-instance answer. When no game is available, pass `null`; the method falls back to `IGuiBase.hasNetGame()` which iterates registered game instances. **Important:** `isNetGame()` must return `true` for *all* game instances in a network match — both the server-side proxy (`NetGuiGame`) and the host's local GUI (`CMatchUI`/`MatchController`). The host's local GUI gets its flag set via `FServerManager.getGui()` calling `setNetGame()`. If adding a new code path gated on `isNetGame()`, test it from the host's perspective, not just the remote client's.
- **Test serialization of new objects:** When adding or modifying objects that may be transmitted over the network (game state, card data, player info), verify they are serializable. Netty will throw `NotSerializableException` at runtime for non-serializable objects — these are easy to miss in local testing but break network play immediately. Run at least one network game after changing data model classes.

## Testing

- **Only write tests that catch real problems:** No wiring tests. Tests should catch integration issues, regressions, or non-obvious edge cases.
- **Headless CI compatibility:** Test classes must not depend on GUI components (`FOptionPane`, `JOptionPane`, etc.) that fail in headless CI environments. Use headless alternatives or skip GUI-dependent tests in CI.
- **Gate slow tests:** Stress and integration tests should be gated so they are skipped during CI's default `mvn clean test`. Only run them explicitly for local validation.
- **Always-run tests:** Unit tests (deck loader, game result, configuration parsing) must NOT be gated — they should always pass in CI without extra flags.