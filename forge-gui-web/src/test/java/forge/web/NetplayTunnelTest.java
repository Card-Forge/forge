package forge.web;

import com.google.gson.JsonObject;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.WebSocketTunnel;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.interfaces.ILobbyListener;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/** Tests desktop or mobile Forge joining a web host. The netplay client connects through the web port by the invite link. */
public class NetplayTunnelTest extends WebHubTest {
    private static final String GUEST_TOKEN = "link-token";
    private WebServer server;
    private final List<App> apps = new ArrayList<>();

    @BeforeClass
    public void openWebPort() throws Exception {
        server = new WebServer(new WebServer.Endpoint() {
            @Override public void connected(final BrowserChannel channel, final String clientId, final boolean local) { }
            @Override public void disconnected(final BrowserChannel channel) { }
            @Override public void onMessage(final BrowserChannel channel, final JsonObject message) { }
        }, GUEST_TOKEN, 0);
    }

    // A connected app delays the next test's table until the host stops waiting for the app's seat to become free
    @Override
    void afterDisconnecting() {
        apps.forEach(App::leave);
        apps.clear();
    }

    @AfterClass(alwaysRun = true)
    public void closeWebPort() {
        if (server != null) {
            server.close();
        }
    }

    /** A netplay client built the way desktop Forge builds one, with a screen state that a test can read. */
    private static final class App implements ILobbyListener {
        final ClientGameLobby lobby = new ClientGameLobby();
        final WebGuiGame gui = new WebGuiGame(PlayerSettings.saved());
        final TestBrowser screen = new TestBrowser();
        final CountDownLatch seated = new CountDownLatch(1);
        /** The chat lines this app has received, as "source: message". */
        final BlockingQueue<String> heard = new LinkedBlockingQueue<>();
        volatile int seat = -1;
        FGameClient client;

        @Override public void message(final String source, final String message, final ChatMessage.MessageType type) {
            heard.add(source + ": " + message);
        }
        @Override public void update(final GameLobbyData state, final int slot) {
            lobby.setLocalPlayer(slot);
            lobby.setData(state);
            if (slot >= 0) {
                seat = slot;
                seated.countDown();
            }
        }
        @Override public void close() { }
        @Override public ClientGameLobby getLobby() {
            return lobby;
        }

        void leave() {
            if (client != null) {
                client.close();
            }
            gui.close();
        }
    }

    private App join(final String name, final String link, final SslContext trust) {
        return join(name, WebSocketTunnel.of(link).uri(), trust);
    }

    private App join(final String name, final URI tunnel, final SslContext trust) {
        final App app = new App();
        apps.add(app);
        app.gui.attach(app.screen);
        app.gui.setClientLobby(app.lobby);
        app.client = new FGameClient(name, app.gui, new WebSocketTunnel(tunnel, trust));
        app.client.setDispatchExecutor(app.gui.dispatchExecutor());
        app.client.addLobbyListener(app);
        app.client.connect();
        return app;
    }

    private static boolean seatIs(final JsonObject table, final int seat, final String type, final String name) {
        if (table.getAsJsonArray("seats").size() <= seat) {
            return false;
        }
        final JsonObject s = table.getAsJsonArray("seats").get(seat).getAsJsonObject();
        return type.equals(s.get("type").getAsString()) && name.equals(s.get("name").getAsString());
    }

    // Fails if a netplay client with the invite link cannot take a seat, is missing from the table or the presence list, or is left out of the match
    @Test(timeOut = 120_000)
    public void anAppSitsDownByTheInviteLinkAndFollowsTheHostIntoTheMatch() throws Exception {
        final TestBrowser host = hostAt("invite");
        final int hostSeat = host.latestTable().get("mySeat").getAsInt();

        final App app = join("Desk", server.inviteUrl("127.0.0.1"), null);
        Assert.assertTrue(app.seated.await(60, TimeUnit.SECONDS), "the app never took a seat through the web port");
        Assert.assertNotEquals(app.seat, hostSeat, "the app was given the host's seat");
        Assert.assertNotNull(host.awaitLobby(l -> seatIs(l, app.seat, "REMOTE", "Desk")), "the host's table never showed the app arriving");
        host.awaitMatching("presence", p -> person(p, "Desk") != null, "the app was never listed among who is here");

        // A browser cannot use the name of a player on an app, just as it cannot use another browser's name
        final TestBrowser guest = connect("guest");
        sessions.onMessage(guest, message("setName", "name", "desk"));
        guest.awaitMatching("error", e -> e.get("message").getAsString().contains("already called"),
                "a browser was let play under the app's name");
        sessions.disconnected(guest);

        sessions.onMessage(host, TestBrowser.action("decks"));
        // The list can arrive before hostAt clears the messages, and the server never sends the same list to a browser twice
        final String deck = TestDecks.firstLegal(host.awaitDecks());
        final JsonObject choose = message("setSeat", "index", hostSeat);
        choose.addProperty("deck", deck);
        sessions.onMessage(host, choose);
        sessions.onMessage(host, message("ready", "ready", true));
        app.client.send(UpdateLobbyPlayerEvent.deckUpdate(TestDecks.of("Forests", "Forest", 60)));
        app.client.send(UpdateLobbyPlayerEvent.isReadyUpdate(true));
        Assert.assertNotNull(host.awaitLobby(l -> l.get("canStart").getAsBoolean()),
                "the host could not start with an app seated, ready and holding a deck");

        sessions.onMessage(host, message("start", "spectate", false));
        app.screen.awaitMatching("state", m -> m.get("full").getAsBoolean(), "the app was never shown the table of the match");
    }

    // Fails if chat between a browser and an app at the same table does not arrive, or a browser is shown its own line twice
    @Test(timeOut = 120_000)
    public void aBrowserAndAnAppReadEachOthersChat() throws Exception {
        final TestBrowser host = hostAt("invite");
        final App app = join("Desk", server.inviteUrl("127.0.0.1"), null);
        Assert.assertTrue(app.seated.await(60, TimeUnit.SECONDS), "the app never took a seat through the web port");

        sessions.onMessage(host, message("chat", "text", "hello from the browser"));
        String line;
        do {
            line = app.heard.poll(60, TimeUnit.SECONDS);
        } while (line != null && !line.endsWith("hello from the browser"));
        Assert.assertEquals(line, "Host: hello from the browser", "the app never read the browser's line");

        app.client.send(new MessageEvent("Desk", "hello from the app"));
        host.awaitMatching("chat", c -> "hello from the app".equals(c.get("text").getAsString()) && "Desk".equals(c.get("from").getAsString()),
                "the browser never read the app's line");
        Assert.assertEquals(host.all("chat").stream().filter(c -> "hello from the browser".equals(c.get("text").getAsString())).count(), 1,
                "the browser was shown its own line again from netplay");
    }

    // Fails if the netplay route accepts a client whose link has the wrong token or no token
    @Test(timeOut = 60_000)
    public void anAppWithoutTheTokenIsTurnedAway() throws Exception {
        hostAt("invite");
        final String route = "ws://127.0.0.1:" + server.port() + WebSocketTunnel.PATH;
        for (final String address : List.of(route + "?token=wrong", route)) {
            Assert.assertThrows(IllegalStateException.class, () -> join("Stranger", URI.create(address), null));
        }
        // A page in a guest's browser has the token as a cookie. It must not be able to open the route with it
        final HttpClient http = HttpClient.newHttpClient();
        final URI page = URI.create("http://127.0.0.1:" + server.port() + WebSocketTunnel.PATH);
        final int byCookie = http.send(HttpRequest.newBuilder(page).header("Cookie", "forge_token=" + GUEST_TOKEN).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
        Assert.assertEquals(byCookie, 403, "the netplay route took a cookie for a token");
        final int byLink = http.send(HttpRequest.newBuilder(URI.create(page + "?token=" + GUEST_TOKEN)).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
        Assert.assertNotEquals(byLink, 403, "the netplay route turned away the token it should take");
    }

    // Fails if an old-style address is treated as an invite link, which would send a desktop host's guests to a web port
    @Test
    public void onlyAnInviteLinkIsTunnelled() {
        Assert.assertNull(WebSocketTunnel.of("localhost:36743"));
        Assert.assertNull(WebSocketTunnel.of("192.168.1.20:36743"));
        Assert.assertNull(WebSocketTunnel.of("http://192.168.1.20:36743"));
        Assert.assertNull(WebSocketTunnel.of("not a link"));
        Assert.assertEquals(String.valueOf(WebSocketTunnel.of("http://192.168.1.20:36743/?token=abc").uri()), "ws://192.168.1.20:36743/netplay?token=abc");
        Assert.assertEquals(String.valueOf(WebSocketTunnel.of(" HTTPS://play.example.com/?token=abc ").uri()), "wss://play.example.com:443/netplay?token=abc");
    }

    // Fails if an https invite link is not joined over TLS, or the host's certificate is not checked against the client's trusted certificates
    @Test(timeOut = 120_000)
    public void anHttpsLinkIsJoinedEncrypted() throws Exception {
        hostAt("invite");
        final Path store = Files.createTempFile("forge-tunnel", ".p12");
        Files.delete(store);
        final Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "host", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost", "-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", "changeit")
                .redirectErrorStream(true).start();
        final String said = new String(keytool.getInputStream().readAllBytes());
        Assert.assertEquals(keytool.waitFor(), 0, "keytool could not make a certificate: " + said);
        final KeyStore keys = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(store)) {
            keys.load(in, "changeit".toCharArray());
        }
        final KeyManagerFactory own = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        own.init(keys, "changeit".toCharArray());
        final KeyStore certs = KeyStore.getInstance("PKCS12");
        certs.load(null, null);
        certs.setCertificateEntry("host", keys.getCertificate("host"));
        final TrustManagerFactory trusting = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trusting.init(certs);

        // Acts as a proxy in front of the server would. It terminates the TLS and passes the decrypted traffic to the web port
        final SslContext front = SslContextBuilder.forServer(own).build();
        final EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            final Channel proxy = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override protected void initChannel(final Channel ch) {
                            ch.pipeline().addLast(front.newHandler(ch.alloc()), new Relay(server.port()));
                        }
                    }).bind(0).sync().channel();
            final int port = ((InetSocketAddress) proxy.localAddress()).getPort();

            // First a certificate this machine does not trust, then a trusted one issued to another name
            Assert.assertThrows(IllegalStateException.class, () -> join("Untrusting", "https://localhost:" + port + "/?token=" + GUEST_TOKEN, null));
            final SslContext trust = SslContextBuilder.forClient().trustManager(trusting).build();
            Assert.assertThrows(IllegalStateException.class, () -> join("Misnamed", "https://127.0.0.1:" + port + "/?token=" + GUEST_TOKEN, trust));

            final App app = join("Secure", "https://localhost:" + port + "/?token=" + GUEST_TOKEN, SslContextBuilder.forClient().trustManager(trusting).build());
            Assert.assertTrue(app.seated.await(60, TimeUnit.SECONDS), "the app never took a seat over TLS");
            app.leave();
            proxy.close().sync();
        } finally {
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS);
            Files.deleteIfExists(store);
        }
    }

    /** Passes bytes between a client and the web port, both ways. */
    private static final class Relay extends ChannelInboundHandlerAdapter {
        private final int port;
        private Channel onward;

        Relay(final int port) {
            this.port = port;
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            final Channel inward = ctx.channel();
            inward.config().setAutoRead(false);
            onward = new Bootstrap().group(inward.eventLoop()).channel(NioSocketChannel.class)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override public void channelRead(final ChannelHandlerContext c, final Object msg) {
                            inward.writeAndFlush(msg);
                        }
                        @Override public void channelInactive(final ChannelHandlerContext c) {
                            inward.close();
                        }
                    }).connect("127.0.0.1", port).addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            inward.config().setAutoRead(true);
                            inward.read();
                        } else {
                            inward.close();
                        }
                    }).channel();
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            onward.writeAndFlush(msg);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) {
            if (onward != null) {
                onward.close();
            }
        }
    }
}
