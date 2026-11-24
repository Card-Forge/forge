package forge.net;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.client.FGameClient.ReconnectState;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.interfaces.ILobbyListener;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** When a netplay client tries to reconnect: only to a seat it held, and never after it was closed on purpose. */
public class ClientReconnectTest {
    private FServerManager server;
    private ServerGameLobby lobby;
    private int port;
    private final List<FGameClient> clients = new ArrayList<>();

    private static final class Listener implements ILobbyListener {
        final CountDownLatch seated = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        private final ClientGameLobby clientLobby = new ClientGameLobby();

        @Override public void update(final GameLobbyData state, final int slot) {
            if (slot >= 0) {
                seated.countDown();
            }
        }
        @Override public void message(final String source, final String message, final ChatMessage.MessageType type) { }
        @Override public void close() {
            closed.countDown();
        }
        @Override public ClientGameLobby getLobby() {
            return clientLobby;
        }
    }

    @BeforeMethod
    public void startServer() {
        TestUtils.ensureFModelInitialized();
        port = PortAllocator.allocatePort();
        server = FServerManager.getInstance();
        server.startServer(port);
        lobby = new ServerGameLobby();
        server.setLobby(lobby);
        server.setLobbyListener(new Listener());
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() {
        clients.forEach(FGameClient::close);
        clients.clear();
        if (HostingServer.isHosting()) {
            server.stopServer();
        }
    }

    private Listener join(final String name) {
        final Listener listener = new Listener();
        final FGameClient client = new FGameClient(name, new HeadlessNetworkGuiGame(), "localhost", port);
        client.addLobbyListener(listener);
        clients.add(client);
        client.connect();
        return listener;
    }

    private static void await(final BooleanSupplier condition, final String why) throws InterruptedException {
        for (int i = 0; i < 1000 && !condition.getAsBoolean(); i++) {
            Thread.sleep(10);
        }
        Assert.assertTrue(condition.getAsBoolean(), why);
    }

    // Fails if a client the server refused starts reconnecting, or its listeners are not told the connection closed
    @Test(timeOut = 30_000)
    public void aRefusedClientDoesNotReconnect() throws Exception {
        lobby.getSlot(1).setType(LobbySlotType.AI);
        final Listener refused = join("Refused");
        Assert.assertTrue(refused.closed.await(10, TimeUnit.SECONDS), "the refused client was not told its connection closed");
        Assert.assertEquals(clients.get(0).getReconnectState(), ReconnectState.CONNECTED);
    }

    // Fails if a seated client whose connection drops does not try to reconnect, or tells its listeners the connection closed
    @Test(timeOut = 30_000)
    public void aSeatedClientReconnectsWhenItsConnectionDrops() throws Exception {
        final Listener seated = join("Seated");
        Assert.assertTrue(seated.seated.await(10, TimeUnit.SECONDS), "the client was never seated");
        server.stopServer();
        await(() -> clients.get(0).getReconnectState() == ReconnectState.RECONNECTING, "the seated client did not try to reconnect");
        Assert.assertFalse(seated.closed.await(500, TimeUnit.MILLISECONDS), "the listeners were told the connection closed while it reconnects");
    }

    // Fails if closing a seated client on purpose starts a reconnect
    @Test(timeOut = 30_000)
    public void aClosedClientDoesNotReconnect() throws Exception {
        final Listener leaving = join("Leaving");
        Assert.assertTrue(leaving.seated.await(10, TimeUnit.SECONDS), "the client was never seated");
        clients.get(0).close();
        // The server frees the seat only after the client's side of the connection has closed
        await(() -> lobby.getSlot(1).getType() == LobbySlotType.OPEN, "the server never freed the seat");
        Assert.assertEquals(clients.get(0).getReconnectState(), ReconnectState.CONNECTED);
    }
}
