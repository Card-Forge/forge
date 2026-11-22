package forge.gui.download;

import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IProgressBar;
import forge.gui.interfaces.ITextField;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Exercises {@link GuiDownloadService#run()} against a socket server that answers like a flaky image host, offline:
 * transfers cut off with and without a Content-Length, and what the download leaves on disk afterwards. The server
 * is a raw socket rather than com.sun.net.httpserver because that one always declares a length or chunks the body,
 * and the case under test is a body that simply ends when the connection closes (HttpURLConnection reports that
 * as a normal end of stream, indistinguishable from a finished transfer).
 */
@Test(groups = {"UnitTest"}, timeOut = 60000)
public class GuiDownloadServiceTest {

    /** Stands in for the image that is installed before a download runs. */
    private static final byte[] OLD_IMAGE = "image that was already installed".getBytes(StandardCharsets.UTF_8);

    @FunctionalInterface
    private interface Reply {
        void send(OutputStream out) throws IOException;
    }

    private IGuiBase previousInterface;
    private ServerSocket serverSocket;
    private String baseUrl;
    private final Map<String, Reply> routes = new ConcurrentHashMap<>();
    private File dir;

    @BeforeClass
    public void setUp() throws IOException {
        previousInterface = GuiBase.getInterface();
        GuiBase.setInterface(stub(IGuiBase.class, (name, args) -> {
            switch (name) {
            case "getUserAgent":
                return "ForgeTest";
            case "invokeInEdtNow":
            case "invokeInEdtLater":
            case "invokeInEdtAndWait":
                ((Runnable) args[0]).run(); // no event thread in a test: progress updates run where they are posted
                return null;
            default:
                return null;
            }
        }));

        serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        baseUrl = "http://127.0.0.1:" + serverSocket.getLocalPort();
        final Thread acceptor = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    final Socket socket = serverSocket.accept();
                    final Thread handler = new Thread(() -> answer(socket));
                    handler.setDaemon(true);
                    handler.start();
                } catch (IOException e) {
                    return; // server closed
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @AfterClass
    public void tearDown() throws IOException {
        serverSocket.close();
        GuiBase.setInterface(previousInterface);
    }

    @BeforeMethod
    public void createDir() throws IOException {
        routes.clear();
        dir = Files.createTempDirectory("guidownload").toFile();
    }

    @AfterMethod
    public void deleteDir() throws IOException {
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private void answer(final Socket client) {
        try (Socket socket = client) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            final String requestLine = in.readLine(); // "GET /path HTTP/1.1"
            String header;
            do {
                header = in.readLine();
            } while (header != null && !header.isEmpty());
            final Reply reply = routes.getOrDefault(requestLine.split(" ")[1], NOT_FOUND);
            final OutputStream out = socket.getOutputStream();
            reply.send(out);
            out.flush();
        } catch (IOException | RuntimeException e) {
            // the client hung up: nothing to answer
        }
    }

    private static final Reply NOT_FOUND = out -> out.write(
            "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));

    /** A 200 answer that ends when its body does; a negative declaredLength leaves out the Content-Length header. */
    private static Reply ok(final byte[] body, final long declaredLength) {
        return out -> {
            final String length = declaredLength >= 0 ? "Content-Length: " + declaredLength + "\r\n" : "";
            out.write(("HTTP/1.1 200 OK\r\n" + length + "Connection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(body);
        };
    }

    private String serve(final String path, final Reply reply) {
        routes.put(path, reply);
        return baseUrl + path;
    }

    private static byte[] image(final String format, final long seed) throws IOException {
        final Random random = new Random(seed); // noise, so that a cut anywhere lands inside the image data
        final BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static byte[] firstHalf(final byte[] image) {
        return Arrays.copyOf(image, image.length / 2);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(final Class<T> type, final BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            final Object result = handler.apply(method.getName(), args);
            if (result != null) {
                return result;
            }
            final Class<?> returned = method.getReturnType();
            if (returned == boolean.class) {
                return false;
            }
            if (returned == int.class) {
                return 0;
            }
            if (returned == long.class) {
                return 0L;
            }
            return returned == String.class ? "" : null;
        });
    }

    /** Runs a download of the given files (path to url) and returns the final progress text. */
    private String download(final Map<String, String> files, final int threads) throws InterruptedException {
        final AtomicReference<String> finalText = new AtomicReference<>("");
        final IProgressBar progressBar = stub(IProgressBar.class, (name, args) -> {
            if (name.equals("setDescription") && ((String) args[0]).contains("finished")) {
                finalText.set((String) args[0]);
            }
            return null;
        });
        final ITextField field = stub(ITextField.class, (name, args) -> null);
        final IButton button = stub(IButton.class, (name, args) -> null);
        final CountDownLatch ready = new CountDownLatch(1);

        final GuiDownloadService service = new GuiDownloadService() {
            @Override
            public String getTitle() {
                return "Test download";
            }

            @Override
            protected Map<String, String> getNeededFiles() {
                return files;
            }

            @Override
            protected int getThreadCount() {
                return threads;
            }
        };
        service.initialize(field, field, progressBar, button, () -> { }, ready::countDown, null);
        Assert.assertTrue(ready.await(10, TimeUnit.SECONDS), "the list of files was not assembled");
        service.run();
        return finalText.get();
    }

    private String download(final File dest, final String url) throws InterruptedException {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put(dest.getAbsolutePath(), url);
        return download(files, 1);
    }

    private void assertNoTempFiles() {
        final String[] leftovers = dir.list((parent, name) -> name.endsWith(".tmp"));
        Assert.assertEquals(leftovers.length, 0, "temporary files left behind: " + Arrays.toString(leftovers));
    }

    private static void assertSkipped(final String finalText, final int skipped) {
        Assert.assertTrue(finalText.contains("Skipped " + skipped + " items"), "unexpected result: " + finalText);
    }

    @DataProvider(name = "formats")
    public Object[][] formats() {
        return new Object[][] {{"jpg"}, {"png"}};
    }

    /** Every cut-off transfer: (declares its length, image format). */
    @DataProvider(name = "interruptions")
    public Object[][] interruptions() {
        return new Object[][] {{true, "jpg"}, {true, "png"}, {false, "jpg"}, {false, "png"}};
    }

    @DataProvider(name = "lengths")
    public Object[][] lengths() {
        return new Object[][] {{true}, {false}};
    }

    @Test(dataProvider = "formats")
    public void wholeDownloadWithContentLength_isSaved(final String format) throws Exception {
        final byte[] image = image(format, 1);
        final File dest = new File(dir, "card." + format);

        final String result = download(dest, serve("/card." + format, ok(image, image.length)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), image);
        assertNoTempFiles();
        assertSkipped(result, 0);
    }

    @Test(dataProvider = "formats")
    public void wholeDownloadWithoutContentLength_isSaved(final String format) throws Exception {
        final byte[] image = image(format, 1);
        final File dest = new File(dir, "card." + format);

        final String result = download(dest, serve("/card." + format, ok(image, -1)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), image);
        assertNoTempFiles();
        assertSkipped(result, 0);
    }

    @Test(dataProvider = "lengths")
    public void wholeDownload_replacesExistingImage(final boolean declareLength) throws Exception {
        final byte[] image = image("jpg", 1);
        final File dest = new File(dir, "card.jpg");
        Files.write(dest.toPath(), OLD_IMAGE);

        final String result = download(dest, serve("/card.jpg", ok(image, declareLength ? image.length : -1)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), image);
        assertNoTempFiles();
        assertSkipped(result, 0);
    }

    @Test(dataProvider = "interruptions")
    public void interruptedDownload_isNotSaved(final boolean declareLength, final String format) throws Exception {
        final byte[] image = image(format, 1);
        final File dest = new File(dir, "card." + format);

        final String result = download(dest, serve("/card." + format, ok(firstHalf(image), declareLength ? image.length : -1)));

        Assert.assertFalse(dest.exists(), "a cut-off transfer was installed as the image");
        assertNoTempFiles();
        assertSkipped(result, 1);
    }

    @Test(dataProvider = "interruptions")
    public void interruptedDownload_keepsExistingImage(final boolean declareLength, final String format) throws Exception {
        final byte[] image = image(format, 1);
        final File dest = new File(dir, "card." + format);
        Files.write(dest.toPath(), OLD_IMAGE);

        final String result = download(dest, serve("/card." + format, ok(firstHalf(image), declareLength ? image.length : -1)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), OLD_IMAGE);
        assertNoTempFiles();
        assertSkipped(result, 1);
    }

    @DataProvider(name = "notImages")
    public Object[][] notImages() {
        return new Object[][] {
            {"<html>Service unavailable</html>".getBytes(StandardCharsets.UTF_8)},
            {new byte[0]}
        };
    }

    @Test(dataProvider = "notImages")
    public void bodyThatIsNoImage_withoutContentLength_keepsExistingImage(final byte[] body) throws Exception {
        final File dest = new File(dir, "card.jpg");
        Files.write(dest.toPath(), OLD_IMAGE);

        final String result = download(dest, serve("/card.jpg", ok(body, -1)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), OLD_IMAGE);
        assertNoTempFiles();
        assertSkipped(result, 1);
    }

    @Test
    public void fileThatIsNoImage_withoutContentLength_isSaved() throws Exception {
        final byte[] prices = "Lightning Bolt=0.5".getBytes(StandardCharsets.UTF_8);
        final File dest = new File(dir, "prices.txt");

        final String result = download(dest, serve("/prices.txt", ok(prices, -1)));

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), prices);
        assertNoTempFiles();
        assertSkipped(result, 0);
    }

    @Test
    public void notFound_keepsExistingImage() throws Exception {
        final File dest = new File(dir, "card.jpg");
        Files.write(dest.toPath(), OLD_IMAGE);

        final String result = download(dest, baseUrl + "/missing.jpg"); // nothing is served there, nor at the .png retry

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), OLD_IMAGE);
        assertNoTempFiles();
        assertSkipped(result, 1);
    }

    @Test
    public void interruptedFirstUrl_fallsBackToTheNextUrl() throws Exception {
        final byte[] image = image("jpg", 1);
        final File dest = new File(dir, "card.jpg");
        final String cutOff = serve("/cut.jpg", ok(firstHalf(image), -1));
        final String whole = serve("/whole.jpg", ok(image, image.length));

        final String result = download(dest, cutOff + GuiDownloadService.URL_SEPARATOR + whole);

        Assert.assertEquals(Files.readAllBytes(dest.toPath()), image);
        assertNoTempFiles();
        assertSkipped(result, 0);
    }

    @Test
    public void parallelDownloads_installOnlyWholeImages() throws Exception {
        final int count = 12;
        final byte[][] images = new byte[count][];
        final Map<String, String> files = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            images[i] = image("jpg", i);
            final boolean cutOff = i % 2 == 1; // odd ones are cut off, alternately with and without a declared length
            final boolean declared = i % 4 < 2;
            final String url = serve("/card" + i + ".jpg", ok(cutOff ? firstHalf(images[i]) : images[i], declared ? images[i].length : -1));
            final File dest = new File(dir, "card" + i + ".jpg");
            if (cutOff && i % 4 == 1) {
                Files.write(dest.toPath(), OLD_IMAGE); // half of the failing ones have an image to protect
            }
            files.put(dest.getAbsolutePath(), url);
        }

        final String result = download(files, 8);

        for (int i = 0; i < count; i++) {
            final File dest = new File(dir, "card" + i + ".jpg");
            if (i % 2 == 0) {
                Assert.assertEquals(Files.readAllBytes(dest.toPath()), images[i], "image " + i);
            } else if (i % 4 == 1) {
                Assert.assertEquals(Files.readAllBytes(dest.toPath()), OLD_IMAGE, "existing image " + i);
            } else {
                Assert.assertFalse(dest.exists(), "cut-off image " + i + " was installed");
            }
        }
        assertNoTempFiles();
        assertSkipped(result, count / 2);
    }
}
