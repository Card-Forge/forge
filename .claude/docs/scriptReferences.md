# NetworkPlay Branch - Script Reference

This document catalogs all scripts and classes relevant to the NetworkPlay branch, organized by category.

---

## Core Network Infrastructure

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| NetworkGuiGame | forge.gamemodes.net | Base class for network GUI games, handles delta sync and full state sync | `forge-gui/src/main/java/forge/gamemodes/net/NetworkGuiGame.java` |
| NetGuiGame | forge.gamemodes.net.server | Server-side network GUI game implementation | `forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java` |
| FServerManager | forge.gamemodes.net.server | Manages network server lifecycle, port binding, client connections | `forge-gui/src/main/java/forge/gamemodes/net/server/FServerManager.java` |
| FGameClient | forge.gamemodes.net.client | Client-side network connection manager | `forge-gui/src/main/java/forge/gamemodes/net/client/FGameClient.java` |
| GameClientHandler | forge.gamemodes.net.client | Handles client-side protocol messages, game creation, player ID sync | `forge-gui/src/main/java/forge/gamemodes/net/client/GameClientHandler.java` |
| GameServerHandler | forge.gamemodes.net.server | Handles server-side protocol messages | `forge-gui/src/main/java/forge/gamemodes/net/server/GameServerHandler.java` |

---

## Delta Synchronization

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| DeltaSyncManager | forge.gamemodes.net.server | Manages delta packet generation, per-client checksums, sequence tracking | `forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java` |
| DeltaPacket | forge.gamemodes.net | Packet containing delta changes for game state sync | `forge-gui/src/main/java/forge/gamemodes/net/DeltaPacket.java` |
| FullStatePacket | forge.gamemodes.net | Packet containing full game state for initial sync/reconnection | `forge-gui/src/main/java/forge/gamemodes/net/FullStatePacket.java` |
| NetworkPropertySerializer | forge.gamemodes.net | Serializes trackable properties for delta transmission | `forge-gui/src/main/java/forge/gamemodes/net/NetworkPropertySerializer.java` |
| NetworkTrackableSerializer | forge.gamemodes.net | Serializes trackable objects for network transmission | `forge-gui/src/main/java/forge/gamemodes/net/NetworkTrackableSerializer.java` |
| NetworkTrackableDeserializer | forge.gamemodes.net | Deserializes trackable objects from network data | `forge-gui/src/main/java/forge/gamemodes/net/NetworkTrackableDeserializer.java` |

---

## Session Management

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| GameSession | forge.gamemodes.net.server | Manages game session state, player registration, reconnection tokens | `forge-gui/src/main/java/forge/gamemodes/net/server/GameSession.java` |
| PlayerSession | forge.gamemodes.net.server | Per-player session state, connection tracking, tokens | `forge-gui/src/main/java/forge/gamemodes/net/server/PlayerSession.java` |
| RemoteClient | forge.gamemodes.net.server | Represents a connected remote client | `forge-gui/src/main/java/forge/gamemodes/net/server/RemoteClient.java` |

---

## Protocol & Events

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| ProtocolMethod | forge.gamemodes.net | Enum of all network protocol methods | `forge-gui/src/main/java/forge/gamemodes/net/ProtocolMethod.java` |
| GameProtocolHandler | forge.gamemodes.net | Base handler for game protocol messages | `forge-gui/src/main/java/forge/gamemodes/net/GameProtocolHandler.java` |
| GameProtocolSender | forge.gamemodes.net | Sends protocol messages over network | `forge-gui/src/main/java/forge/gamemodes/net/GameProtocolSender.java` |
| NetEvent | forge.gamemodes.net.event | Base interface for network events | `forge-gui/src/main/java/forge/gamemodes/net/event/NetEvent.java` |
| LoginEvent | forge.gamemodes.net.event | Client login event | `forge-gui/src/main/java/forge/gamemodes/net/event/LoginEvent.java` |
| ReconnectRequestEvent | forge.gamemodes.net.event | Client reconnection request | `forge-gui/src/main/java/forge/gamemodes/net/event/ReconnectRequestEvent.java` |
| ReconnectRejectedEvent | forge.gamemodes.net.event | Server reconnection rejection | `forge-gui/src/main/java/forge/gamemodes/net/event/ReconnectRejectedEvent.java` |
| ReplyEvent | forge.gamemodes.net.event | Reply to a protocol request | `forge-gui/src/main/java/forge/gamemodes/net/event/ReplyEvent.java` |
| MessageEvent | forge.gamemodes.net.event | Chat message event | `forge-gui/src/main/java/forge/gamemodes/net/event/MessageEvent.java` |

---

## Lobby Management

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| ServerGameLobby | forge.gamemodes.net.server | Server-side game lobby management | `forge-gui/src/main/java/forge/gamemodes/net/server/ServerGameLobby.java` |
| ClientGameLobby | forge.gamemodes.net.client | Client-side game lobby representation | `forge-gui/src/main/java/forge/gamemodes/net/client/ClientGameLobby.java` |
| IOnlineLobby | forge.gamemodes.net | Interface for online lobby operations | `forge-gui/src/main/java/forge/gamemodes/net/IOnlineLobby.java` |
| NetConnectUtil | forge.gamemodes.net | Utilities for network connection UI | `forge-gui/src/main/java/forge/gamemodes/net/NetConnectUtil.java` |

---

## Debugging & Logging

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| NetworkDebugLogger | forge.gamemodes.net | Centralized logging for network debugging with file output | `forge-gui/src/main/java/forge/gamemodes/net/NetworkDebugLogger.java` |
| NetworkDebugConfig | forge.gamemodes.net | Configuration for debug logging (from NetworkDebug.config) | `forge-gui/src/main/java/forge/gamemodes/net/NetworkDebugConfig.java` |
| NetworkByteTracker | forge.gamemodes.net | Tracks network bandwidth usage for analysis | `forge-gui/src/main/java/forge/gamemodes/net/NetworkByteTracker.java` |
| NetworkGameEventListener | forge.gamemodes.net | Listens for game events and logs them | `forge-gui/src/main/java/forge/gamemodes/net/NetworkGameEventListener.java` |

---

## Serialization Infrastructure

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| CompatibleObjectEncoder | forge.gamemodes.net | Netty encoder for network objects | `forge-gui/src/main/java/forge/gamemodes/net/CompatibleObjectEncoder.java` |
| CompatibleObjectDecoder | forge.gamemodes.net | Netty decoder for network objects | `forge-gui/src/main/java/forge/gamemodes/net/CompatibleObjectDecoder.java` |
| CObjectInputStream | forge.gamemodes.net | Custom object input stream for network serialization | `forge-gui/src/main/java/forge/gamemodes/net/CObjectInputStream.java` |
| CObjectOutputStream | forge.gamemodes.net | Custom object output stream for network serialization | `forge-gui/src/main/java/forge/gamemodes/net/CObjectOutputStream.java` |

---

## Test Infrastructure

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| ComprehensiveDeltaSyncTest | forge.net | Main test class for comprehensive 100-game network tests | `forge-gui-desktop/src/test/java/forge/net/ComprehensiveDeltaSyncTest.java` |
| BatchGameTest | forge.net | TestNG entry point for sequential and parallel batch tests | `forge-gui-desktop/src/test/java/forge/net/BatchGameTest.java` |
| AutomatedNetworkTest | forge.net | Individual automated network tests (2p, 3p, 4p) | `forge-gui-desktop/src/test/java/forge/net/AutomatedNetworkTest.java` |
| ComprehensiveTestExecutor | forge.net | Orchestrates comprehensive test execution | `forge-gui-desktop/src/test/java/forge/net/ComprehensiveTestExecutor.java` |
| ComprehensiveGameRunner | forge.net | Runs individual games for comprehensive tests (subprocess entry) | `forge-gui-desktop/src/test/java/forge/net/ComprehensiveGameRunner.java` |
| MultiProcessGameExecutor | forge.net | Spawns parallel game processes for batch testing | `forge-gui-desktop/src/test/java/forge/net/MultiProcessGameExecutor.java` |
| SequentialGameExecutor | forge.net | Sequential multi-game execution in single JVM | `forge-gui-desktop/src/test/java/forge/net/SequentialGameExecutor.java` |
| ConsoleNetworkTestRunner | forge.net | Standalone CLI entry point for CI/CD | `forge-gui-desktop/src/test/java/forge/net/ConsoleNetworkTestRunner.java` |
| HeadlessNetworkGuiGame | forge.net | Headless GUI game for automated testing | `forge-gui-desktop/src/test/java/forge/net/HeadlessNetworkGuiGame.java` |
| HeadlessNetworkClient | forge.net | Headless network client for automated testing | `forge-gui-desktop/src/test/java/forge/net/HeadlessNetworkClient.java` |
| HeadlessGuiDesktop | forge.net | Headless desktop GUI for testing environment | `forge-gui-desktop/src/test/java/forge/net/HeadlessGuiDesktop.java` |
| NoOpGuiGame | forge.net | No-op IGuiGame implementation for AI games | `forge-gui-desktop/src/test/java/forge/net/NoOpGuiGame.java` |
| NetworkClientTestHarness | forge.net | Test harness for network client testing | `forge-gui-desktop/src/test/java/forge/net/NetworkClientTestHarness.java` |
| AutomatedGameTestHarness | forge.net | Network game harness via ServerGameLobby | `forge-gui-desktop/src/test/java/forge/net/AutomatedGameTestHarness.java` |
| LocalGameTestHarness | forge.net | Non-network harness via LocalLobby | `forge-gui-desktop/src/test/java/forge/net/LocalGameTestHarness.java` |
| GameTestHarnessFactory | forge.net | Unified entry point for all test modes | `forge-gui-desktop/src/test/java/forge/net/GameTestHarnessFactory.java` |
| NetworkLogAnalyzer | forge.net.analysis | Analyzes network debug logs for errors and metrics | `forge-gui-desktop/src/test/java/forge/net/analysis/NetworkLogAnalyzer.java` |
| GameLogMetrics | forge.net.analysis | Per-game metrics storage | `forge-gui-desktop/src/test/java/forge/net/analysis/GameLogMetrics.java` |
| AnalysisResult | forge.net.analysis | Aggregate results and reporting | `forge-gui-desktop/src/test/java/forge/net/analysis/AnalysisResult.java` |

---

## Test Scenarios

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| MultiplayerScenario | forge.net.scenarios | 3-4 player games with local AI | `forge-gui-desktop/src/test/java/forge/net/scenarios/MultiplayerScenario.java` |
| MultiplayerNetworkScenario | forge.net.scenarios | 3-4 player games with remote HeadlessNetworkClient connections | `forge-gui-desktop/src/test/java/forge/net/scenarios/MultiplayerNetworkScenario.java` |
| ReconnectionScenario | forge.net.scenarios | Tests disconnect/AI takeover functionality | `forge-gui-desktop/src/test/java/forge/net/scenarios/ReconnectionScenario.java` |

---

## Utility Classes

| Class | Namespace | Description | Location |
|-------|-----------|-------------|----------|
| TestDeckLoader | forge.net | Loads random precon decks for testing | `forge-gui-desktop/src/test/java/forge/net/TestDeckLoader.java` |
| PortAllocator | forge.net | Allocates unique ports for parallel tests | `forge-gui-desktop/src/test/java/forge/net/PortAllocator.java` |
| GameTestMetrics | forge.net | Collects metrics from test runs | `forge-gui-desktop/src/test/java/forge/net/GameTestMetrics.java` |
| GameTestMode | forge.net | Enum: LOCAL, NETWORK_LOCAL, NETWORK_REMOTE | `forge-gui-desktop/src/test/java/forge/net/GameTestMode.java` |
| TestConfiguration | forge.net | System properties configuration loader | `forge-gui-desktop/src/test/java/forge/net/TestConfiguration.java` |
| GameEventListener | forge.net | Game event logging interface for tests | `forge-gui-desktop/src/test/java/forge/net/GameEventListener.java` |
| NetworkAIPlayerFactory | forge.net | AI player creation for tests | `forge-gui-desktop/src/test/java/forge/net/NetworkAIPlayerFactory.java` |
| ReplyPool | forge.gamemodes.net | Manages async reply tracking | `forge-gui/src/main/java/forge/gamemodes/net/ReplyPool.java` |
| ChatMessage | forge.gamemodes.net | Represents a chat message | `forge-gui/src/main/java/forge/gamemodes/net/ChatMessage.java` |

---

## Key Files for Bug Fixes

When debugging network issues, start with these files:

1. **Checksum/Desync issues**: `DeltaSyncManager.java`, `NetworkGuiGame.java`
2. **Player ID mismatches**: `GameClientHandler.java`, `Game.java`
3. **Connection issues**: `FServerManager.java`, `FGameClient.java`
4. **Reconnection issues**: `GameSession.java`, `PlayerSession.java`
5. **Protocol issues**: `ProtocolMethod.java`, `GameProtocolHandler.java`
6. **Logging/debugging**: `NetworkDebugLogger.java`, `NetworkLogAnalyzer.java`
