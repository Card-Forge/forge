package forge.gamemodes.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Host-side relay tunnel. Establishes a persistent outbound connection to the
 * VPS relay port (the "tunnel"), then listens for "new joiner" signals.
 * On each signal, opens a new outbound connection to the VPS relay port
 * (the "data" connection) and a new local connection to the game server
 * (localhost:gamePort), and bridges them bidirectionally.
 *
 * This allows joiners behind NAT to connect to the VPS relay, which bridges
 * them to the host's game server — no port forwarding needed on either side.
 */
public class RelayTunnel {

    private static final byte MAGIC_HOST_TUNNEL = 0x01;
    private static final byte MAGIC_HOST_DATA = 0x02;

    private final String relayHost;
    private final int relayPort;
    private final int gamePort;
    private final ExecutorService executor;
    private volatile boolean running;
    private Socket tunnelSocket;

    public RelayTunnel(String relayHost, int relayPort, int gamePort) {
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.gamePort = gamePort;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "RelayTunnel");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start the tunnel: connect to the VPS relay port and send the host
     * tunnel magic byte. Then listen for "new joiner" signals on the tunnel.
     * Reconnects automatically if the tunnel drops.
     */
    public void start() throws IOException {
        running = true;
        connectTunnel();
    }

    /**
     * Connect (or reconnect) to the VPS relay port and listen for signals.
     */
    private void connectTunnel() {
        while (running) {
            try {
                tunnelSocket = new Socket();
                tunnelSocket.connect(new InetSocketAddress(relayHost, relayPort), 10000);
                tunnelSocket.setTcpNoDelay(true);
                tunnelSocket.setKeepAlive(true);

                OutputStream out = tunnelSocket.getOutputStream();
                out.write(MAGIC_HOST_TUNNEL);
                out.flush();

                System.out.println("RelayTunnel: connected to " + relayHost + ":" + relayPort);

                InputStream in = tunnelSocket.getInputStream();
                byte[] buf = new byte[1];
                while (running) {
                    int n = in.read(buf);
                    if (n <= 0) {
                        break;
                    }
                    if (buf[0] == MAGIC_HOST_TUNNEL) {
                        executor.submit(this::createDataConnection);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("RelayTunnel: tunnel dropped, reconnecting in 3s: " + e.getMessage());
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) { return; }
                }
            }
        }
        System.out.println("RelayTunnel: tunnel closed");
    }

    /**
     * Create a per-joiner data connection: connect to VPS relay port (with
     * magic 0x02), connect to localhost:gamePort, and bridge them.
     */
    private void createDataConnection() {
        try {
            // Connect to VPS relay port with data magic
            Socket dataConn = new Socket();
            dataConn.connect(new InetSocketAddress(relayHost, relayPort), 10000);
            dataConn.setTcpNoDelay(true);
            OutputStream dataOut = dataConn.getOutputStream();
            dataOut.write(MAGIC_HOST_DATA);
            dataOut.flush();

            // Connect to local game server
            Socket gameConn = new Socket();
            gameConn.connect(new InetSocketAddress("127.0.0.1", gamePort), 5000);
            gameConn.setTcpNoDelay(true);

            System.out.println("RelayTunnel: data connection established, bridging to localhost:" + gamePort);

            // Bridge dataConn ↔ gameConn bidirectionally
            Thread t1 = new Thread(() -> pipe(dataConn, gameConn), "RelayData->Game");
            Thread t2 = new Thread(() -> pipe(gameConn, dataConn), "RelayGame->Data");
            t1.start();
            t2.start();

            // Wait for either direction to close
            t1.join();
            t2.join();

            System.out.println("RelayTunnel: data connection closed");
        } catch (Exception e) {
            System.err.println("RelayTunnel: failed to create data connection: " + e.getMessage());
        }
    }

    /**
     * Copy all bytes from 'from' to 'to', then close both.
     */
    private void pipe(Socket from, Socket to) {
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            // Connection closed — expected
        } finally {
            try { from.close(); } catch (IOException ignored) {}
            try { to.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Stop the tunnel and clean up.
     */
    public void stop() {
        running = false;
        try {
            if (tunnelSocket != null) tunnelSocket.close();
        } catch (IOException ignored) {}
        executor.shutdownNow();
        System.out.println("RelayTunnel: stopped");
    }
}
