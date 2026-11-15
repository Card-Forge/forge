package forge.web;

import com.google.gson.JsonObject;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.web.images.CardThumbnails;
import forge.web.images.Foil;
import forge.web.images.ImageFiles;
import forge.web.protocol.BrowserChannel;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServerTest {
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<JsonObject> received = new CopyOnWriteArrayList<>();
    private volatile BrowserChannel connected;
    private volatile boolean connectedLocal;
    private WebServer server;

    private String origin() {
        return "http://127.0.0.1:" + server.port();
    }

    @BeforeClass
    public void setUp() throws Exception {
        WebTestSupport.initModel();
        server = new WebServer(new WebServer.Endpoint() {
            @Override public void connected(final BrowserChannel channel, final String clientId, final boolean local) {
                connectedLocal = local;
                connected = channel;
            }
            @Override public void disconnected(final BrowserChannel channel) { }
            @Override public void onMessage(final BrowserChannel channel, final JsonObject message) { received.add(message); }
        }, "secret", 0);
    }

    @AfterClass
    public void tearDown() {
        server.close();
    }

    private HttpResponse<String> get(final String pathAndQuery, final String cookie) throws Exception {
        final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(origin() + pathAndQuery));
        if (cookie != null) {
            b.header("Cookie", cookie);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    // Fails if a request without the access token is served instead of refused
    @Test
    public void requestWithoutTokenIsForbidden() throws Exception {
        Assert.assertEquals(get("/", null).statusCode(), 403);
    }

    // Fails if the access token in the address does not serve the page, or does not set a token cookie that scripts cannot read and other sites cannot send
    @Test
    public void tokenServesThePageAndSetsTheCookie() throws Exception {
        final HttpResponse<String> r = get("/?token=secret", null);
        Assert.assertEquals(r.statusCode(), 200);
        Assert.assertTrue(r.body().contains("<title>"));
        final String cookie = r.headers().firstValue("set-cookie").orElse("");
        Assert.assertTrue(cookie.contains("forge_token=secret"), cookie);
        Assert.assertTrue(cookie.toLowerCase().contains("; httponly"), cookie);
        Assert.assertTrue(cookie.contains("SameSite=Strict"), cookie);
    }

    // Fails if a request that a proxy forwarded counts as this machine and is given the logs
    @Test
    public void theLogsAreRefusedThroughAProxy() throws Exception {
        final HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(origin() + "/logs?token=secret"))
                .header("X-Forwarded-For", "203.0.113.7").build(), HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(r.statusCode(), 403);
    }

    // Fails if a proxied socket, such as one through a Cloudflare tunnel, counts as a browser on this machine, or a direct one does not
    @Test
    public void theSocketSaysWhetherItIsOnThisMachine() throws Exception {
        final WebSocket proxied = openSocket(origin(), new CopyOnWriteArrayList<>(), "Cf-Connecting-Ip");
        awaitConnected();
        Assert.assertFalse(connectedLocal, "a socket through a tunnel counted as this machine");
        proxied.sendClose(WebSocket.NORMAL_CLOSURE, "").get(5, TimeUnit.SECONDS);

        final WebSocket direct = openSocket(origin(), new CopyOnWriteArrayList<>(), null);
        awaitConnected();
        Assert.assertTrue(connectedLocal, "a socket from this machine did not count as this machine");
        direct.sendClose(WebSocket.NORMAL_CLOSURE, "").get(5, TimeUnit.SECONDS);
    }

    private void awaitConnected() {
        WebHubTest.awaitTrue(() -> connected != null, "the socket never reached the endpoint");
    }

    // Fails if an image key that climbs out of its folder with ".." is accepted, or a normal key with two dots inside a name is refused
    @Test
    public void imageKeysThatClimbOutAreRefused() {
        for (final String key : new String[] {"i:../x", "i:a/../../x", "t:..\\x", "../x.jpg", "c:..|set", "b:..", "i:.."}) {
            Assert.assertFalse(ImageFiles.safeKey(key), key);
        }
        for (final String key : new String[] {"c:Lightning Bolt|M10", "t:w_1_1_soldier", "i:mana/w", "c:Who..What|UNH"}) {
            Assert.assertTrue(ImageFiles.safeKey(key), key);
        }
    }

    // Fails if a marked key no longer names the foil printing, so the server cannot find its picture, or its seed differs from the browser's. foil.test.ts checks the same seed
    @Test
    public void aFoilsImageKeyNamesItsPrintingAndItsStreaks() {
        final PaperCard plain = FModel.getMagicDb().getCommonCards().getCard("Shivan Dragon", "M14");
        final String key = Foil.key(plain.getFoiled());
        Assert.assertEquals(key, "c:Shivan Dragon+|M14|1");
        final PaperCard named = ImageUtil.getPaperCardFromImageKey(key);
        Assert.assertTrue(named.isFoil());
        Assert.assertEquals(named.getCardImageKey(), plain.getCardImageKey());
        Assert.assertEquals(Foil.seed(key), 28);
        Assert.assertEquals(Foil.seed(Foil.key(plain)), 0);
    }

    // Fails if a missing card image or a path that climbs out of the web folder gets anything but a 404
    @Test
    public void missingImageAndPathTraversalAre404() throws Exception {
        // With online fetching on, a missing image gets a reply only after the fetcher gives up. That case is not tested here
        final ForgePreferences prefs = FModel.getPreferences();
        final boolean online = prefs.getPrefBoolean(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);
        prefs.setPref(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER, false);
        try {
            Assert.assertEquals(get("/img?key=c:NoSuchCard%7CXXX&token=secret", null).statusCode(), 404);
        } finally {
            prefs.setPref(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER, online);
        }
        // This class is on the classpath one folder above the page's files, so it would be served without the guard
        Assert.assertEquals(get("/..%2Fforge%2Fweb%2FWebServer.class?token=secret", null).statusCode(), 404);
    }

    // Fails if the board's size is not shrunk to the thumbnail width, the full size is shrunk, or a browser can ask for a size of its choosing
    @Test
    public void aCardImageIsShrunkOnlyToTheBoardsSize() throws Exception {
        final File file = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, "web_test_thumbnail.jpg");
        file.getParentFile().mkdirs();
        ImageIO.write(new BufferedImage(600, 840, BufferedImage.TYPE_INT_RGB), "jpg", file);
        try {
            Assert.assertEquals(imageWidth("&w=" + CardThumbnails.WIDTH), CardThumbnails.WIDTH);
            Assert.assertEquals(imageWidth(""), 600);
            Assert.assertEquals(imageWidth("&w=10"), 600);
        } finally {
            file.delete();
        }
    }

    private int imageWidth(final String size) throws Exception {
        final HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(origin() + "/img?key=t:web_test_thumbnail" + size + "&token=secret")).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        Assert.assertEquals(r.statusCode(), 200);
        return ImageIO.read(new ByteArrayInputStream(r.body())).getWidth();
    }

    // Fails if the page's text is sent again when the browser holds it, if text is sent uncompressed, or if a picture is compressed a second time
    @Test
    public void textIsCompressedAndRevalidatedAndPicturesAreSentAsTheyAre() throws Exception {
        final HttpResponse<byte[]> text = http.send(HttpRequest.newBuilder(URI.create(origin() + "/text?token=secret"))
                .header("Accept-Encoding", "gzip").build(), HttpResponse.BodyHandlers.ofByteArray());
        Assert.assertEquals(text.headers().firstValue("content-encoding").orElse(""), "gzip");
        final String etag = text.headers().firstValue("etag").orElseThrow();
        final HttpResponse<byte[]> held = http.send(HttpRequest.newBuilder(URI.create(origin() + "/text?token=secret"))
                .header("If-None-Match", etag).build(), HttpResponse.BodyHandlers.ofByteArray());
        Assert.assertEquals(held.statusCode(), 304);
        final HttpResponse<byte[]> icon = http.send(HttpRequest.newBuilder(URI.create(origin() + "/coin?side=heads&token=secret"))
                .header("Accept-Encoding", "gzip").build(), HttpResponse.BodyHandlers.ofByteArray());
        Assert.assertEquals(icon.statusCode(), 200);
        Assert.assertTrue(icon.headers().firstValue("content-encoding").isEmpty());
    }

    // Fails if the page links more than one stylesheet, the joined one has no version or is not kept, or it leaves out the first or last stylesheet
    @Test
    public void thePageLinksOneVersionedStylesheetThatIsKept() throws Exception {
        final String page = get("/?token=secret", null).body();
        Assert.assertEquals(page.split("rel=\"stylesheet\"", -1).length - 1, 1, "the page links more than one stylesheet");
        final Matcher versioned = Pattern.compile("href=\"(css/page\\.css\\?v=[0-9a-f]+)\"").matcher(page);
        Assert.assertTrue(versioned.find(), "the stylesheet has no version");
        final HttpResponse<String> styles = get("/" + versioned.group(1) + "&token=secret", null);
        Assert.assertTrue(styles.headers().firstValue("cache-control").orElse("").contains("immutable"));
        Assert.assertTrue(styles.body().contains(get("/css/theme.css?token=secret", null).body()));
        Assert.assertTrue(styles.body().contains(get("/css/portrait.css?token=secret", null).body()));
    }

    /** forwarded names a header that a proxy adds, sent with a made-up address, or is null for a direct socket. */
    private WebSocket openSocket(final String origin, final List<String> texts, final String forwarded) throws Exception {
        connected = null;
        final WebSocket.Builder b = http.newWebSocketBuilder().header("Origin", origin);
        if (forwarded != null) {
            b.header(forwarded, "203.0.113.7");
        }
        return b
                .buildAsync(URI.create("ws://127.0.0.1:" + server.port() + "/ws?token=secret"), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(final WebSocket ws, final CharSequence data, final boolean last) {
                        texts.add(data.toString());
                        ws.request(1);
                        return null;
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    // Fails if a socket message from the browser does not reach the endpoint, or one from the server does not reach the browser
    @Test
    public void socketCarriesMessagesBothWays() throws Exception {
        final List<String> texts = new CopyOnWriteArrayList<>();
        final WebSocket ws = openSocket(origin(), texts, null);
        ws.sendText("{\"t\":\"ping\"}", true).get(5, TimeUnit.SECONDS);
        WebHubTest.awaitTrue(() -> !received.isEmpty(), "the endpoint never got the browser's message");
        Assert.assertEquals(received.get(0).get("t").getAsString(), "ping");
        connected.send(TestBrowser.action("pong"));
        WebHubTest.awaitTrue(() -> !texts.isEmpty(), "the browser never got the server's message");
        Assert.assertTrue(texts.get(0).contains("pong"));
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(5, TimeUnit.SECONDS);
    }

    // Fails if a socket opened by a page from another site is accepted
    @Test(expectedExceptions = ExecutionException.class)
    public void socketFromAnotherOriginIsRefused() throws Exception {
        openSocket("http://evil.example", new CopyOnWriteArrayList<>(), null);
    }
}
