package forge.net;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.util.collect.FCollectionView;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test that runs a full AI vs AI game over the real TCP network stack.
 * Uses 10-card basic land decks for fast CI execution.
 *
 * What this test catches:
 * - Serialization failures (NotSerializableException on new/changed game objects)
 * - Protocol mismatches (wrong method name/args in ProtocolMethod enum)
 * - Unhandled exceptions in Netty pipeline (beforeCall, channelRead)
 * - EDT/Netty deadlocks (game hangs -> 60s timeout fails)
 * - Silent client disconnection (server finishes but client assertions fail)
 * - Send errors in the Netty pipeline (caught by RemoteClient)
 */
public class TestTrueNetworkTraffic {

    @BeforeClass
    public void setUp() {
        TestUtils.ensureFModelInitialized();
    }

    @Test(timeOut = 60000, description = "True network traffic test with remote client")
    public void testTrueNetworkTraffic() throws Exception {
        System.out.println("[TestTrueNetworkTraffic] Starting true network traffic test...");

        FServerManager server = FServerManager.getInstance();
        FGameClient client = null;

        try {
            // Create minimal decks
            Deck deck1 = TestDeckLoader.createMinimalDeck("Mountain", 10);
            Deck deck2 = TestDeckLoader.createMinimalDeck("Forest", 10);

            // Allocate a free port
            int port = PortAllocator.allocatePort();
            System.out.println("[TestTrueNetworkTraffic] Using port: " + port);

            // Start server
            server.startServer(port);
            Assert.assertTrue(server.isHosting(), "Server should be hosting");

            // Create lobby and configure server side
            ServerGameLobby lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // No-op lobby listener
            server.setLobbyListener(new ILobbyListener() {
                @Override public void message(String source, String message) {
                    System.out.println("[Server] " + (source != null ? source + ": " : "") + message);
                }
                @Override public void update(GameLobbyData state, int slot) { }
                @Override public void close() { }
                @Override public ClientGameLobby getLobby() { return null; }
            });

            // No-op lobby update listener
            lobby.setListener(new IUpdateable() {
                @Override public void update(boolean fullUpdate) { }
                @Override public void update(int slot, LobbySlotType type) { }
            });

            // Configure slots
            LobbySlot slot0 = lobby.getSlot(0);
            slot0.setType(LobbySlotType.AI);
            slot0.setDeck(deck1);
            slot0.setIsReady(true);

            // Slot 1 stays OPEN; becomes REMOTE when client connects
            LobbySlot slot1 = lobby.getSlot(1);
            slot1.setDeck(deck2);

            // Connect client
            HeadlessNetworkGuiGame clientGui = new HeadlessNetworkGuiGame();
            client = new FGameClient("ignored", "0", clientGui, "localhost", port);

            // ClientGameLobby required by GameClientHandler.beforeCall
            ClientGameLobby clientLobby = new ClientGameLobby();
            clientLobby.setListener(new IUpdateable() {
                @Override public void update(boolean fullUpdate) { }
                @Override public void update(int slot, LobbySlotType type) { }
            });

            client.addLobbyListener(new ILobbyListener() {
                @Override public void message(String source, String message) {
                    System.out.println("[Client] " + (source != null ? source + ": " : "") + message);
                }
                @Override public void update(GameLobbyData state, int slot) {
                    clientLobby.setLocalPlayer(slot);
                    clientLobby.setData(state);
                }
                @Override public void close() { }
                @Override public ClientGameLobby getLobby() { return clientLobby; }
            });

            client.connect();

            // Wait for slot 1 to become REMOTE (client fully registered on server)
            long waitStart = System.currentTimeMillis();
            while (slot1.getType() != LobbySlotType.REMOTE) {
                if (System.currentTimeMillis() - waitStart > 10_000) {
                    Assert.fail("Client did not register within 10 seconds. Slot 1 type: " + slot1.getType());
                }
                Thread.sleep(100);
            }
            System.out.println("[TestTrueNetworkTraffic] Client registered, slot 1 type: " + slot1.getType());

            // Mark slot 1 ready
            slot1.setIsReady(true);

            // Start the game
            Runnable startGameRunnable = lobby.startGame();
            Assert.assertNotNull(startGameRunnable, "startGame() should return a Runnable");
            startGameRunnable.run();

            // Get HostedMatch and Game
            HostedMatch hostedMatch = lobby.getHostedMatch();
            Assert.assertNotNull(hostedMatch, "HostedMatch should exist after startGame");

            Game game = hostedMatch.getGame();
            Assert.assertNotNull(game, "Game should exist after startGame");

            // Convert remote player to AI
            System.out.println("[TestTrueNetworkTraffic] Converting remote player (slot 1) to AI");
            server.convertToAI(1, slot1.getName());

            // Poll for game completion
            long startTime = System.currentTimeMillis();
            long timeout = 45_000;
            while (!game.isGameOver()) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    Assert.fail("Game did not complete within " + (timeout / 1000) + " seconds. " +
                        "Turn: " + game.getPhaseHandler().getTurn());
                }
                Thread.sleep(500);
            }

            // Server-side assertions
            int turnCount = game.getPhaseHandler().getTurn();
            System.out.println("[TestTrueNetworkTraffic] Game completed in " + turnCount + " turns");
            Assert.assertTrue(turnCount > 0, "Game should have at least one turn");

            // Client-side assertions — protocol lifecycle
            Assert.assertTrue(clientGui.isOpenViewCalled(),
                "Client should have received openView over the wire");
            Assert.assertTrue(clientGui.getSetGameViewCount() > 0,
                "Client should have received setGameView updates (count: " + clientGui.getSetGameViewCount() + ")");

            // Client GameView state assertions
            GameView clientGameView = clientGui.getGameView();
            Assert.assertNotNull(clientGameView.getGameLog(),
                "Client GameView.getGameLog() should be initialized by GameClientHandler");
            Assert.assertEquals(clientGameView.getPlayers().size(), 2,
                "Client GameView should have 2 players");

            // Zone consistency — CardViews in zone collections must have matching Zone property
            ZoneType[] zonesToCheck = {ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Library};
            for (PlayerView pv : clientGameView.getPlayers()) {
                for (ZoneType zone : zonesToCheck) {
                    FCollectionView<CardView> cards = pv.getCards(zone);
                    if (cards == null) continue;
                    for (CardView cv : cards) {
                        Assert.assertEquals(cv.getZone(), zone,
                            "CardView id=" + cv.getId() + " in " + pv.getName() + "'s " + zone +
                            " has stale zone: " + cv.getZone());
                    }
                }
            }

            // Card visibility — cards in public zones must be visible to all players
            Iterable<PlayerView> allPlayers = clientGameView.getPlayers();
            ZoneType[] publicZones = {ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile};
            for (PlayerView pv : clientGameView.getPlayers()) {
                for (ZoneType zone : publicZones) {
                    FCollectionView<CardView> cards = pv.getCards(zone);
                    if (cards == null) continue;
                    for (CardView cv : cards) {
                        Assert.assertTrue(cv.canBeShownToAny(allPlayers),
                            "CardView id=" + cv.getId() + " in public zone " + zone +
                            " should be visible but canBeShownToAny returned false");
                    }
                }
            }

            // Pipeline error assertions
            int sendErrors = server.getTotalSendErrors();
            Assert.assertEquals(sendErrors, 0,
                "Server encountered " + sendErrors + " send error(s) during the game. " +
                "Check test output for stack traces.");

            System.out.println("[TestTrueNetworkTraffic] Test PASSED: game completed in " + turnCount +
                " turns, client received " + clientGui.getSetGameViewCount() + " setGameView updates" +
                ", 0 send errors");

        } finally {
            // Cleanup
            if (client != null) {
                client.close();
            }
            if (server.isHosting()) {
                server.stopServer();
            }
        }
    }

}
