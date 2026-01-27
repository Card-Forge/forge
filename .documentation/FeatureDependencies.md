# NetworkPlay Branch: Staged PR Implementation Guide

## Feature Categories

This branch can be disaggregated into 5 independent feature categories for staged review:

| # | Feature | Lines | Complexity | Dependencies |
|---|---------|-------|------------|--------------|
| 1 | Chat Improvements | ~200 | LOW | None |
| 2 | Testing Tools | ~9,650 | LOW | None |
| 3 | New Network Protocol | ~4,500 | HIGH | None (foundational) |
| 4 | Other UI Improvements | ~500 | MEDIUM | Partial protocol dependency |
| 5 | Reconnection Support | ~1,250 | MEDIUM | Requires protocol |

---

## Feature 1: Chat Improvements (~200 lines)

**Status:** Fully Independent
**Complexity:** LOW
**Review Priority:** First (quick win)

### Description
Enhanced chat message system with player/system message distinction and improved formatting.

### File Inventory

| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `ChatMessage.java` | forge-gui | 71 | Message types (PLAYER/SYSTEM), formatting |
| `IOnlineChatInterface.java` | forge-gui | 6 | UI integration interface |
| `MessageEvent.java` | forge-gui | 34 | Network transport for chat |
| `FNetOverlay.java` (changes) | forge-gui-desktop | ~50 | System message styling (light blue) |
| `FServerManager.java` (changes) | forge-gui | ~40 | Broadcasting chat to players |

### Implementation Notes
- System messages displayed in light blue to distinguish from player chat
- Clean separation between chat transport and UI rendering
- No dependencies on other NetworkPlay features

### Review Guidance
- Focus on: Message type enum design, color choices for system messages
- Test: Send chat messages in multiplayer lobby, verify system announcements display correctly

---

## Feature 2: Testing Tools (~9,650 lines)

**Status:** Fully Independent
**Complexity:** LOW (self-contained test module)
**Review Priority:** Can be parallel with any other PR

### Description
Comprehensive test infrastructure for network validation, including 100-game automated batch testing with log analysis.

### File Inventory

#### Test Harnesses
| File | Lines | Description |
|------|-------|-------------|
| `ComprehensiveDeltaSyncTest.java` | 298 | Main 100-game test orchestrator |
| `ComprehensiveTestExecutor.java` | 230 | Batch execution manager |
| `MultiProcessGameExecutor.java` | 671 | Parallel JVM process spawning |
| `ComprehensiveGameRunner.java` | 214 | Individual game runner |

#### Network Testing
| File | Lines | Description |
|------|-------|-------------|
| `HeadlessNetworkClient.java` | 578 | Remote TCP client simulation |
| `NetworkClientTestHarness.java` | 417 | Server + client orchestration |
| `AutomatedGameTestHarness.java` | 316 | Network game framework |
| `AutomatedNetworkTest.java` | 619 | Integration test suite |

#### Log Analysis
| File | Lines | Description |
|------|-------|-------------|
| `NetworkLogAnalyzer.java` | 416 | Log parsing for metrics |
| `AnalysisResult.java` | 424 | Result aggregation |
| `GameLogMetrics.java` | 242 | Per-game metric storage |
| `DeckCountVerificationTest.java` | 109 | Deck extraction validation |

#### Support Utilities
| File | Lines | Description |
|------|-------|-------------|
| `NoOpGuiGame.java` | 693 | Headless AI game dummy |
| `HeadlessGuiDesktop.java` | ~200 | Headless desktop override |
| `HeadlessNetworkGuiGame.java` | 349 | Network-aware headless game |
| `GameEventListener.java` | 267 | Production logging |

#### Scenario Tests
| File | Lines | Description |
|------|-------|-------------|
| `MultiplayerScenario.java` | 355 | 2-4 player tests |
| `MultiplayerNetworkScenario.java` | 499 | Network multiplayer tests |
| `ReconnectionScenario.java` | 337 | Reconnection tests |

### Location
All test files located in: `forge-gui-desktop/src/test/java/forge/net/`

### Implementation Notes
- Entirely self-contained in test directory
- Does not affect production game code
- Provides validation infrastructure for all other features
- Supports headless execution for CI integration

### Review Guidance
- Focus on: Test coverage, parallel execution safety, resource cleanup
- Test: Run `ComprehensiveDeltaSyncTest` with 10-game batch to validate harness

---

## Feature 3: New Network Protocol (~4,500 lines)

**Status:** Foundational
**Complexity:** HIGH
**Review Priority:** Second (enables other features)

### Description
Delta synchronization protocol that transmits only changed game state, achieving 99.5% bandwidth reduction compared to the previous full-snapshot approach.

### File Inventory

#### Core Protocol
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `DeltaPacket.java` | forge-gui | 240 | Incremental state change container |
| `FullStatePacket.java` | forge-gui | 123 | Initial sync & reconnection payload |
| `ProtocolMethod.java` | forge-gui | 203 | RPC method registry |
| `GameProtocolHandler.java` | forge-gui | 114 | Netty message dispatch |
| `GameProtocolSender.java` | forge-gui | 31 | RPC client wrapper |

#### Serialization
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `NetworkTrackableSerializer.java` | forge-gui | 120 | Binary writer |
| `NetworkTrackableDeserializer.java` | forge-gui | ~200 | Binary reader with Tracker |
| `NetworkPropertySerializer.java` | forge-gui | 593 | Type-aware property serialization |
| `CompatibleObjectDecoder.java` | forge-gui | 47 | Version-aware deserialization |
| `CompatibleObjectEncoder.java` | forge-gui | ~50 | Version-aware serialization |

#### State Management
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `DeltaSyncManager.java` | forge-gui | 894 | Server-side state tracking |
| `NetworkGuiGame.java` | forge-gui-desktop | ~940 | Client-side deserialization |
| `NetGuiGame.java` | forge-gui | 697 | Server-side delta transmission |
| `GameServerHandler.java` | forge-gui | 67 | Server protocol handling |

#### Debugging
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `NetworkDebugLogger.java` | forge-gui | ~200 | Configurable logging |
| `NetworkDebugConfig.java` | forge-gui | ~80 | Configuration flags |

### Architecture Overview

```
Server                              Client
┌─────────────────┐                ┌─────────────────┐
│  NetGuiGame     │                │ NetworkGuiGame  │
│  (transmit)     │                │ (receive)       │
└────────┬────────┘                └────────┬────────┘
         │                                  │
         ▼                                  ▼
┌─────────────────┐                ┌─────────────────┐
│ DeltaSyncManager│   DeltaPacket  │ Tracker         │
│ (track changes) │ ─────────────► │ (reconstruct)   │
└─────────────────┘                └─────────────────┘
```

### Key Design Decisions
1. **Binary serialization**: Custom serializers for efficient wire format
2. **Property tracking**: Only changed properties transmitted per tick
3. **Object identity**: Unique IDs maintained across client/server
4. **Version tolerance**: Encoder/decoder handle protocol versioning

### Implementation Notes
- Replaces previous full GameView transmission
- Backwards compatible with existing lobby/matchmaking
- Debug logging configurable via `NetworkDebugConfig`

### Review Guidance
- Focus on: Serialization correctness, memory management, thread safety
- Test: Run 10-game network test, verify bandwidth metrics in logs
- Critical paths: `DeltaSyncManager.buildDeltaPacket()`, `NetworkGuiGame.processDeltaPacket()`

---

## Feature 4: Other UI Improvements (~500 lines)

**Status:** Partially Dependent on Protocol
**Complexity:** MEDIUM
**Review Priority:** Third (after protocol)

### Description
Connection status, error handling, and player feedback enhancements.

### File Inventory

| File | Module | Lines | Description | Depends on Protocol? |
|------|--------|-------|-------------|---------------------|
| `LobbyUpdateEvent.java` | forge-gui | 182 | Player status updates | No |
| `UpdateLobbyPlayerEvent.java` | forge-gui | 182 | Player detail updates | No |
| `InputLockUI.java` (changes) | forge-gui-desktop | ~30 | Network-aware waiting messages | No |
| `GameClientHandler.java` (changes) | forge-gui | ~100 | Multiplayer ID consistency fix | Yes |
| `NetGuiGame.java` (changes) | forge-gui | ~50 | Bandwidth metrics display | Yes |
| `ReplyPool.java` | forge-gui | ~50 | Async RPC response handling | Yes |
| `NetConnectUtil.java` | forge-gui | ~50 | Connection helper utilities | No |

### Disaggregation Options

**Independent Components** (can be submitted separately):
- `LobbyUpdateEvent.java` / `UpdateLobbyPlayerEvent.java` - Lobby UI enhancements
- `InputLockUI.java` changes - Better waiting state messages
- `NetConnectUtil.java` - Connection utilities

**Protocol-Dependent Components** (submit with or after protocol):
- `GameClientHandler.java` player ID sync fix
- `NetGuiGame.java` bandwidth tracking
- `ReplyPool.java` async RPC handling

### Review Guidance
- Focus on: UI responsiveness, error message clarity, connection state handling
- Test: Join/leave lobby repeatedly, verify player list updates correctly

---

## Feature 5: Reconnection Support (~1,250 lines)

**Status:** Depends on Protocol
**Complexity:** MEDIUM
**Review Priority:** Fourth (requires protocol infrastructure)

### Description
Enables players to rejoin games within 5-minute timeout. If timeout expires, AI takes over the disconnected player's position.

### File Inventory

#### Session Management
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `GameSession.java` | forge-gui | 215 | Session state management |
| `PlayerSession.java` | forge-gui | 160 | Per-player tokens & state |

#### Events
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `ReconnectRequestEvent.java` | forge-gui | 46 | Client reconnection request |
| `ReconnectRejectedEvent.java` | forge-gui | 35 | Server rejection response |

#### Core Changes
| File | Module | Lines | Description |
|------|--------|-------|-------------|
| `FServerManager.java` (changes) | forge-gui | ~350 | Session lifecycle, AI takeover, timeout |
| `FGameClient.java` (changes) | forge-gui | ~100 | Session credential storage |
| `GameClientHandler.java` (changes) | forge-gui | ~120 | Reconnection flow handling |
| `ServerGameLobby.java` (changes) | forge-gui | ~5 | Session creation on game start |
| `ProtocolMethod.java` (additions) | forge-gui | ~10 | reconnectRequest, gamePaused, etc. |
| `NetworkGuiGame.java` (additions) | forge-gui-desktop | ~60 | gamePaused, gameResumed, reconnectAccepted |

### Why Protocol Is Required

Reconnection depends on protocol infrastructure because:

1. **FullStatePacket**: Reconnecting client receives complete game state via `FullStatePacket`
2. **Tracker Initialization**: Client must create `Tracker` to register objects for delta updates
3. **NetworkGuiGame**: Deserialization logic lives in protocol's client-side handler

### Reconnection Flow

```
Client Disconnect          Server                     Reconnecting Client
       │                     │                              │
       X──── disconnect ────►│                              │
       │                     │◄─── 5 min timeout starts ───│
       │                     │                              │
       │                     │◄─── ReconnectRequestEvent ──│
       │                     │                              │
       │                     │── validate session token ───►│
       │                     │                              │
       │                     │─── FullStatePacket ─────────►│
       │                     │                              │
       │                     │─── resume delta sync ───────►│
```

### Implementation Notes
- Session tokens stored client-side for reconnection
- Server pauses game and notifies remaining players
- AI takeover configurable (default: 5 minutes)
- Clean session cleanup on game end

### Review Guidance
- Focus on: Token security, timeout handling, AI takeover correctness
- Test: Disconnect client mid-game, reconnect within timeout, verify state sync
- Edge cases: Reconnect during combat, during stack resolution, after game end

---

## Dependency Diagram

```
┌─────────────────────┐
│  Chat Improvements  │─────────────────────────────► INDEPENDENT
│      (~200 lines)   │
└─────────────────────┘

┌─────────────────────┐
│   Testing Tools     │─────────────────────────────► INDEPENDENT
│    (~9,650 lines)   │
└─────────────────────┘

┌─────────────────────┐
│  Network Protocol   │─────────────────────────────► FOUNDATIONAL
│    (~4,500 lines)   │
└──────────┬──────────┘
           │
           ├──────────────────────────────────────────────────────┐
           │                                                      │
           ▼                                                      ▼
┌─────────────────────┐                              ┌─────────────────────┐
│ UI Improvements     │                              │ Reconnection Support│
│ (partial, ~200 ln)  │                              │    (~1,250 lines)   │
│ - Player ID sync    │                              │                     │
│ - Bandwidth metrics │                              │                     │
└─────────────────────┘                              └─────────────────────┘
```