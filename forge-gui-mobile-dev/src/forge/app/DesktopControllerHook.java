package forge.app;

import com.badlogic.gdx.Gdx;
import forge.Forge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Debug-only controller simulator for the desktop build, for testing gamepad navigation without a gamepad.
 * <p>
 * Enabled with {@code -Dforge.controllerhook=PORT}. Listens on 127.0.0.1 only. Each line received is one button name
 * ({@code A B X Y L1 R1 L2 R2 UP DOWN LEFT RIGHT START SELECT}); it is pressed through the same code path as a real
 * controller button and the hook answers {@code ok} once the press was handled (or {@code error ...}).
 */
public final class DesktopControllerHook {
    private DesktopControllerHook() { }

    public static void startIfEnabled() {
        String portProp = System.getProperty("forge.controllerhook");
        if (portProp == null || portProp.isEmpty()) {
            return;
        }
        final int port;
        try {
            port = Integer.parseInt(portProp.trim());
        } catch (NumberFormatException e) {
            System.err.println("Controller hook: bad port " + portProp);
            return;
        }
        Thread thread = new Thread(() -> serve(port), "controller-hook");
        thread.setDaemon(true);
        thread.start();
    }

    private static void serve(int port) {
        try (ServerSocket server = new ServerSocket(port, 4, InetAddress.getLoopbackAddress())) {
            System.out.println("Controller hook listening on 127.0.0.1:" + port);
            while (true) {
                try (Socket socket = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        out.println(press(line.trim()));
                    }
                } catch (Exception e) {
                    System.err.println("Controller hook connection error: " + e);
                }
            }
        } catch (Exception e) {
            System.err.println("Controller hook stopped: " + e);
        }
    }

    private static String press(String name) {
        if (name.isEmpty()) {
            return "error empty";
        }
        if ("DUMP".equalsIgnoreCase(name)) { //debug: print the focusable widgets to the log
            Gdx.app.postRunnable(() -> System.out.println("[controller-hook] " + Forge.describeControllerFocus()));
            return "ok";
        }
        final Forge.PadButton button;
        try {
            button = Forge.PadButton.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return "error unknown button " + name;
        }
        if (Gdx.app == null) {
            return "error app not started";
        }
        CountDownLatch done = new CountDownLatch(1);
        final String[] result = { "ok" };
        Gdx.app.postRunnable(() -> {
            try {
                Forge.handlePadButton(button);
            } catch (Exception e) {
                result[0] = "error " + e;
                e.printStackTrace();
            } finally {
                Gdx.graphics.requestRendering();
                done.countDown();
            }
        });
        try {
            if (!done.await(5, TimeUnit.SECONDS)) {
                return "error timeout";
            }
        } catch (InterruptedException e) {
            return "error interrupted";
        }
        return result[0];
    }
}
