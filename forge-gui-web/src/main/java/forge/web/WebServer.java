package forge.web;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.gamemodes.net.WebSocketBytes;
import forge.gamemodes.net.WebSocketTunnel;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.util.FileUtil;
import forge.util.IHasForgeLog;
import forge.util.LogExporter;
import forge.web.images.CardThumbnails;
import forge.web.images.SkinSprites;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.PageText;
import forge.web.protocol.ToBrowser.PictureReady;
import forge.web.protocol.MessageJson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

/** The HTTP and WebSocket server on the web port. It serves the page, card images, sounds, the browsers' sockets and the netplay tunnel for desktop and mobile Forge. It listens on every interface so that a player on another machine can open a link. The token in the link is new on each launch and keeps everyone else out. */
public final class WebServer implements AutoCloseable, IHasForgeLog {
    private static final String COOKIE = "forge_token";
    /** Whether a request came from a browser on this machine, not from another machine or through a tunnel. */
    private static final AttributeKey<Boolean> LOCAL = AttributeKey.valueOf("forge.local");
    private static final AttributeKey<Boolean> KEEP_ALIVE = AttributeKey.valueOf("forge.keepAlive");
    /** For content that never changes at its address, such as a card picture of one printing or a hashed script chunk. The browser keeps it permanently. */
    static final String KEEP_FOREVER = "public, max-age=31536000, immutable";
    /** For skin pictures and sound effects. They change only when the skin or sound set is changed and the server restarted, so a day's wait is fair. */
    static final String KEEP_A_DAY = "max-age=86400";
    /** The same port as Forge's own netplay. Hosting from the desktop client at the same time therefore needs {@code -Dforge.web.port}. */
    private static final int DEFAULT_PORT = 36743;
    private static final int PING_SECONDS = 30;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".js", "text/javascript; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".svg", "image/svg+xml",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".webmanifest", "application/manifest+json");
    private final EventLoopGroup group = new NioEventLoopGroup(2, new DefaultThreadFactory("WebServer", true));
    private final Endpoint endpoint;
    private final String token;
    private final Channel channel;
    private final PageBundle bundle = new PageBundle();
    /** Every open page. A closed socket leaves the group by itself. */
    private final ChannelGroup browsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public interface Endpoint {
        /** The id tells browsers apart across a reload. local says the browser is on this machine. */
        void connected(BrowserChannel channel, String clientId, boolean local);
        void disconnected(BrowserChannel channel);
        void onMessage(BrowserChannel channel, JsonObject message);
    }
    public WebServer(final Endpoint endpoint, final String token) throws InterruptedException {
        this(endpoint, token, configuredPort());
    }

    static int configuredPort() {
        return Integer.getInteger("forge.web.port", DEFAULT_PORT);
    }

    /** A port of 0 uses any free port, which is what a test needs. */
    WebServer(final Endpoint endpoint, final String token, final int port) throws InterruptedException {
        this.endpoint = endpoint;
        this.token = token;
        final ServerBootstrap b = new ServerBootstrap()
                .group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        // The handlers after the gate depend on who is calling, so the gate adds them at the first request
                        ch.pipeline().addLast(new HttpServerCodec(), new HttpObjectAggregator(1 << 20), new AccessGate());
                    }
                });
        channel = b.bind(port).sync().channel();
    }

    public int port() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    /** The link for this machine's own browser. */
    public String url() {
        return inviteUrl("127.0.0.1");
    }

    /** The link at whichever address a player can reach this machine by. */
    public String inviteUrl(final String address) {
        return "http://" + address + ":" + port() + "/?token=" + token;
    }

    @Override
    public void close() {
        channel.close().syncUninterruptibly();
        group.shutdownGracefully();
    }

    /** True when the socket's page came from this server, whichever address the browser reached it by. */
    private static boolean sameOrigin(final FullHttpRequest req) {
        final String origin = req.headers().get(HttpHeaderNames.ORIGIN);
        final String host = req.headers().get(HttpHeaderNames.HOST);
        if (origin == null || host == null) {
            return false;
        }
        final int slashes = origin.indexOf("//");
        return slashes >= 0 && host.equals(origin.substring(slashes + 2));
    }

    /** Whether a token is the link's. The comparison takes constant time, so the time a guess takes reveals nothing about the token. */
    private boolean admits(final String presented) {
        return presented != null && MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    /** A loopback request that no proxy forwarded. A proxy on this machine also connects from loopback, but adds these headers. */
    private static boolean fromThisMachine(final ChannelHandlerContext ctx, final FullHttpRequest req) {
        return ctx.channel().remoteAddress() instanceof InetSocketAddress a && a.getAddress() != null && a.getAddress().isLoopbackAddress()
                && !req.headers().contains("Cf-Connecting-Ip") && !req.headers().contains("X-Forwarded-For") && !req.headers().contains("Forwarded");
    }

    private static String param(final QueryStringDecoder q, final String name) {
        final List<String> values = q.parameters().get(name);
        return values == null ? null : values.get(0);
    }

    /** Whether a request carries the link's token: in its query, or as the cookie that the page load stored. */
    private boolean admitted(final FullHttpRequest req, final QueryStringDecoder q) {
        if (admits(param(q, "token"))) {
            return true;
        }
        final String header = req.headers().get(HttpHeaderNames.COOKIE);
        if (header == null) {
            return false;
        }
        for (final Cookie c : ServerCookieDecoder.STRICT.decode(header)) {
            if (COOKIE.equals(c.name()) && admits(c.value())) {
                return true;
            }
        }
        return false;
    }

    static void respond(final ChannelHandlerContext ctx, final HttpResponseStatus status, final byte[] body, final String type) {
        respond(ctx, status, body, type, null, "no-cache", null);
    }

    /** Sends one of the messages in {@link forge.web.protocol.ToBrowser} to every open page. */
    private void toEveryBrowser(final Record message) {
        browsers.writeAndFlush(new TextWebSocketFrame(MessageJson.WRITER.toJson(MessageJson.encode(message))));
    }

    static void notFound(final ChannelHandlerContext ctx) {
        respond(ctx, HttpResponseStatus.NOT_FOUND, new byte[0], "text/plain");
    }

    /** Forge's logs zipped as desktop's Help menu exports them, or null when there are none. */
    private static byte[] logsZip() throws IOException {
        final Path dir = Files.createTempDirectory("forge-web-logs");
        try {
            final File zip = LogExporter.exportLogs(dir.toFile());
            return zip == null ? null : Files.readAllBytes(zip.toPath());
        } finally {
            FileUtil.deleteDirectory(dir.toFile());
        }
    }

    private static void respondOrNotFound(final ChannelHandlerContext ctx, final byte[] body, final String type, final String ifNoneMatch) {
        if (body == null) {
            notFound(ctx);
        } else {
            respondUnlessUnchanged(ctx, body, type, null, KEEP_A_DAY, ifNoneMatch);
        }
    }

    /** Answers with the body, or with an empty 304 when the browser already holds the same body. */
    private static void respondUnlessUnchanged(final ChannelHandlerContext ctx, final byte[] body, final String type, final String cookieToken,
            final String cacheControl, final String ifNoneMatch) {
        final String etag = etag(body);
        final boolean unchanged = etag.equals(ifNoneMatch);
        respond(ctx, unchanged ? HttpResponseStatus.NOT_MODIFIED : HttpResponseStatus.OK, unchanged ? new byte[0] : body,
                type, cookieToken, cacheControl, etag);
    }

    /** Answers a request. A cookie, when given, is the token that the page was loaded with. */
    static void respond(final ChannelHandlerContext ctx, final HttpResponseStatus status, final byte[] body, final String type,
            final String cookieToken, final String cacheControl, final String etag) {
        final FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(body));
        resp.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, type)
                .set(HttpHeaderNames.CACHE_CONTROL, cacheControl);
        // A 304 has no body. A length on it would say that the body the browser already has is empty
        if (status != HttpResponseStatus.NOT_MODIFIED) {
            resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, body.length);
        }
        if (etag != null) {
            resp.headers().set(HttpHeaderNames.ETAG, etag);
        }
        if (cookieToken != null) {
            final DefaultCookie cookie = new DefaultCookie(COOKIE, cookieToken);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSameSite(CookieHeaderNames.SameSite.Strict);
            resp.headers().set(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        }
        send(ctx, resp);
    }

    static void redirect(final ChannelHandlerContext ctx, final String location) {
        final FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        resp.headers()
                .set(HttpHeaderNames.LOCATION, location)
                .setInt(HttpHeaderNames.CONTENT_LENGTH, 0)
                .set(HttpHeaderNames.CACHE_CONTROL, "no-store");
        send(ctx, resp);
    }

    /** The connection stays open when the browser asked for that, because a board of forty pictures would otherwise need forty handshakes. */
    static void send(final ChannelHandlerContext ctx, final FullHttpResponse resp) {
        final boolean keepAlive = Boolean.TRUE.equals(ctx.channel().attr(KEEP_ALIVE).get());
        resp.headers().set(HttpHeaderNames.CONNECTION, keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
        final ChannelFuture sent = ctx.writeAndFlush(resp);
        if (!keepAlive) {
            sent.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String contentType(final String resource) {
        final int dot = resource.lastIndexOf('.');
        return dot < 0 ? "application/octet-stream" : CONTENT_TYPES.getOrDefault(resource.substring(dot), "application/octet-stream");
    }

    private static String etag(final byte[] body) {
        final CRC32 crc = new CRC32();
        crc.update(body);
        return "\"" + Long.toHexString(crc.getValue()) + "-" + Integer.toHexString(body.length) + "\"";
    }

    private final class AccessGate extends ChannelInboundHandlerAdapter {
        private boolean routed;

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            if (msg instanceof FullHttpRequest req) {
                final QueryStringDecoder q = new QueryStringDecoder(req.uri());
                final boolean socket = "/ws".equals(q.path());
                final boolean netplay = WebSocketTunnel.PATH.equals(q.path());
                // A netplay client is not a page and sends no origin to check, so only its link's token lets it in, never a cookie
                final boolean admitted = netplay ? admits(param(q, "token")) : admitted(req, q);
                // The token keeps other pages out. The origin check stops them from opening a socket with a stolen cookie
                if (!admitted || (socket && !sameOrigin(req)) || (netplay && !HostingServer.isHosting())) {
                    req.release();
                    respond(ctx, HttpResponseStatus.FORBIDDEN, new byte[0], "text/plain");
                    return;
                }
                ctx.channel().attr(LOCAL).set(fromThisMachine(ctx, req));
                ctx.channel().attr(KEEP_ALIVE).set(HttpUtil.isKeepAlive(req));
                if (!routed) {
                    routed = true;
                    route(ctx, netplay);
                }
            }
            ctx.fireChannelRead(msg);
        }

        private void route(final ChannelHandlerContext ctx, final boolean netplay) {
            if (netplay) {
                // Desktop or mobile Forge. The netplay server then handles this connection like one it accepted itself
                ctx.pipeline().addLast(
                        new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
                                .websocketPath(WebSocketTunnel.PATH).checkStartsWith(true)
                                .maxFramePayloadLength(WebSocketTunnel.FRAME_BYTES).build()),
                        new WebSocketBytes());
                FServerManager.getInstance().adopt(ctx.channel());
                return;
            }
            ctx.pipeline().addLast(
                    // State and deck lists are JSON that repeats its field names, so compression matters to a guest over the internet
                    new WebSocketServerCompressionHandler(),
                    new TextCompressor(),
                    new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
                            .websocketPath("/ws").checkStartsWith(true).maxFramePayloadLength(1 << 22)
                            .allowExtensions(true).build()),
                    new StaticFiles(),
                    new BrowserSocket(endpoint, browsers));
        }
    }

    /**
     * Compresses the page's text for a guest over the internet. Pictures and sound are compressed already, so they are sent as they are.
     * Netty's socket handshake removes this from the pipeline, so it never sees socket frames.
     */
    private static final class TextCompressor extends HttpContentCompressor {
        @Override
        protected Result beginEncode(final HttpResponse response, final String acceptEncoding) throws Exception {
            final String type = response.headers().get(HttpHeaderNames.CONTENT_TYPE, "");
            return type.startsWith("text/") || type.contains("json") || type.startsWith("image/svg") ? super.beginEncode(response, acceptEncoding) : null;
        }
    }

    private final class StaticFiles extends SimpleChannelInboundHandler<FullHttpRequest> {
        /** The path the browser last asked for, so a failure message can name the file. */
        private String asked;

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            if (cause instanceof IOException) {
                netLog.warn("Could not send a browser {}: {}", asked, cause.toString());
            } else {
                netLog.warn(cause, "Could not send a browser {}", asked);
            }
            // Otherwise the browser would wait for a request that is never answered
            if (ctx.channel().isActive()) {
                respond(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new byte[0], "text/plain");
            }
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) throws IOException {
            final QueryStringDecoder q = new QueryStringDecoder(req.uri());
            final String path = q.path();
            final String ifNoneMatch = req.headers().get(HttpHeaderNames.IF_NONE_MATCH);
            asked = path;
            if ("/avatar".equals(path) || "/sleeve".equals(path)) {
                final String index = param(q, "i");
                final Integer i = index == null ? null : Ints.tryParse(index);
                respondOrNotFound(ctx, i == null ? null : SkinSprites.png("/avatar".equals(path), i), "image/png", ifNoneMatch);
                return;
            }
            if ("/sleeveart".equals(path)) {
                final String key = param(q, "key");
                if (key == null) {
                    notFound(ctx);
                } else {
                    ImageReplies.serveSleeveArt(ctx, key, "1".equals(param(q, "auto")), () -> toEveryBrowser(new PictureReady(key, true)));
                }
                return;
            }
            if ("/mana".equals(path)) {
                final String symbol = param(q, "s");
                respondOrNotFound(ctx, symbol == null ? null : SkinSprites.manaPng(symbol), "image/png", ifNoneMatch);
                return;
            }
            if ("/ability".equals(path)) {
                final String icon = param(q, "k");
                respondOrNotFound(ctx, icon == null ? null : SkinSprites.abilityPng(icon), "image/png", ifNoneMatch);
                return;
            }
            if ("/coin".equals(path)) {
                respondOrNotFound(ctx, SkinSprites.coinPng(!"tails".equals(param(q, "side"))), "image/png", ifNoneMatch);
                return;
            }
            if ("/sound".equals(path) || "/music".equals(path)) {
                AudioReplies.serve(ctx, "/sound".equals(path), param(q, "name"), param(q, "track"), req.headers().get(HttpHeaderNames.RANGE), ifNoneMatch);
                return;
            }
            if ("/img".equals(path)) {
                final String key = param(q, "key");
                if (key == null) {
                    notFound(ctx);
                } else {
                    ImageReplies.serveImage(ctx, key, String.valueOf(CardThumbnails.WIDTH).equals(param(q, "w")),
                            () -> toEveryBrowser(new PictureReady(key, false)));
                }
                return;
            }
            if ("/logs".equals(path)) {
                // The logs are this machine's files, and they name the players who connected, so only a browser on this machine gets them
                if (!Boolean.TRUE.equals(ctx.channel().attr(LOCAL).get())) {
                    respond(ctx, HttpResponseStatus.FORBIDDEN, new byte[0], "text/plain");
                } else {
                    final byte[] zip = logsZip();
                    if (zip == null) {
                        notFound(ctx);
                    } else {
                        respond(ctx, HttpResponseStatus.OK, zip, "application/zip");
                    }
                }
                return;
            }
            if ("/text".equals(path)) {
                respondUnlessUnchanged(ctx, PageText.json(), "application/json; charset=utf-8", null, "no-cache", ifNoneMatch);
                return;
            }
            final String resource = "/".equals(path) ? "index.html" : path.substring(1);
            final byte[] body = resource.contains("..") ? null : "index.html".equals(resource) ? bundle.page()
                    : PageBundle.STYLES.equals(resource) ? bundle.styles() : PageBundle.readResource(resource);
            final String type = contentType(resource);
            if (body == null) {
                notFound(ctx);
                return;
            }
            // The response sets the link's token as a cookie, so the page's later requests carry it
            final String token = param(q, "token");
            // A chunk's name and the page's version parameter are file hashes, so a changed file gets a new address and the old one never goes stale
            final boolean hashed = resource.startsWith("js/chunks/") || q.parameters().containsKey("v");
            respondUnlessUnchanged(ctx, body, type, admits(token) ? token : null, hashed ? KEEP_FOREVER : "no-cache", ifNoneMatch);
        }
    }

    private static final class BrowserSocket extends SimpleChannelInboundHandler<WebSocketFrame> {
        private final Endpoint endpoint;
        private final ChannelGroup browsers;
        private BrowserChannel browser;
        private ScheduledFuture<?> pings;

        BrowserSocket(final Endpoint endpoint, final ChannelGroup browsers) {
            this.endpoint = endpoint;
            this.browsers = browsers;
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete done) {
                final Channel ch = ctx.channel();
                browsers.add(ch);
                browser = new BrowserChannel() {
                    @Override
                    public void send(final JsonObject message) {
                        ch.writeAndFlush(new TextWebSocketFrame(MessageJson.WRITER.toJson(message)));
                    }

                    @Override
                    public void close() {
                        ch.writeAndFlush(new CloseWebSocketFrame(CLOSED_BY_SERVER, null)).addListener(ChannelFutureListener.CLOSE);
                    }
                };
                final String id = param(new QueryStringDecoder(done.requestUri()), "client");
                endpoint.connected(browser, id == null ? "" : id, Boolean.TRUE.equals(ch.attr(LOCAL).get()));
                // A proxy between the server and the browser drops a socket that stays silent too long, as it does while a player thinks
                pings = ctx.executor().scheduleAtFixedRate(() -> ch.writeAndFlush(new PingWebSocketFrame()),
                        PING_SECONDS, PING_SECONDS, TimeUnit.SECONDS);
            } else {
                ctx.fireUserEventTriggered(evt);
            }
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame text && browser != null) {
                try {
                    endpoint.onMessage(browser, JsonParser.parseString(text.text()).getAsJsonObject());
                } catch (final RuntimeException e) {
                    netLog.warn(e, "Bad browser message");
                }
            }
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) {
            if (pings != null) {
                pings.cancel(false);
            }
            if (browser != null) {
                endpoint.disconnected(browser);
            }
            ctx.fireChannelInactive();
        }
    }
}
