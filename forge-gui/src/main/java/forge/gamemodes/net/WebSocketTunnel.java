package forge.gamemodes.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

/** The client end of a netplay connection that runs through a WebSocket on a web server's port. */
public final class WebSocketTunnel {
    /** The path on a web server's port that accepts netplay clients over a WebSocket. */
    public static final String PATH = "/netplay";
    /** The largest WebSocket frame accepted. Each netplay message is one frame, so this is just above the decoder's message limit. */
    public static final int FRAME_BYTES = 10 << 20;
    private static final int OPEN_SECONDS = 15;

    private final URI uri;
    private final SslContext trust;

    /**
     * A wss address uses TLS. The server's certificate is checked against trust, or against the system's trusted
     * certificates when trust is null.
     */
    public WebSocketTunnel(final URI uri, final SslContext trust) {
        this.uri = uri;
        this.trust = trust;
    }

    /**
     * Returns the tunnel for a web server's invite link, which is an http or https link with a token.
     * Returns null for any other text.
     */
    public static WebSocketTunnel of(final String address) {
        final URI link;
        try {
            link = URI.create(address.trim());
        } catch (final IllegalArgumentException e) {
            return null;
        }
        final boolean secure = "https".equalsIgnoreCase(link.getScheme());
        if ((!secure && !"http".equalsIgnoreCase(link.getScheme())) || link.getHost() == null
                || link.getRawQuery() == null || !link.getRawQuery().matches("(.*&)?token=.+")) {
            return null;
        }
        final int port = link.getPort() < 0 ? (secure ? 443 : 80) : link.getPort();
        return new WebSocketTunnel(URI.create((secure ? "wss" : "ws") + "://" + link.getHost() + ":" + port + PATH + "?" + link.getRawQuery()), null);
    }

    public URI uri() {
        return uri;
    }

    public SocketAddress address() {
        return InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort());
    }

    /** Adds the handlers that carry the netplay byte stream over the WebSocket. The netplay handlers go after them. */
    public void addHandlers(final Channel ch) throws SSLException {
        if ("wss".equals(uri.getScheme())) {
            final SslContext ssl = trust != null ? trust : SslContextBuilder.forClient().build();
            final SslHandler tls = ssl.newHandler(ch.alloc(), uri.getHost(), uri.getPort());
            // Makes TLS check that the certificate names this host. By default it only checks that the certificate is trusted
            final SSLParameters named = tls.engine().getSSLParameters();
            named.setEndpointIdentificationAlgorithm("HTTPS");
            tls.engine().setSSLParameters(named);
            ch.pipeline().addLast(tls);
        }
        ch.pipeline().addLast(
                new HttpClientCodec(),
                new HttpObjectAggregator(8192),
                new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(uri,
                        WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), FRAME_BYTES)),
                new WebSocketBytes());
    }

    /**
     * Waits for the WebSocket handshake on a connected channel, and closes the channel if it fails. Any caller can
     * connect to the web port, so the handshake is where the server accepts or refuses this client.
     */
    public void awaitOpen(final Channel ch) throws InterruptedException {
        final ChannelFuture opened = ch.pipeline().get(WebSocketBytes.class).opened();
        if (!opened.await(OPEN_SECONDS, TimeUnit.SECONDS) || !opened.isSuccess()) {
            ch.close();
            throw new IllegalStateException("The host did not let this client in", opened.cause());
        }
    }
}
