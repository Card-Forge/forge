package forge.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for allocating available network ports for testing.
 *
 * Helps avoid "Address already in use: bind" errors when tests run
 * in rapid succession or in parallel by:
 * 1. Checking port availability before returning
 * 2. Using wider port spacing between allocations
 * 3. Supporting multiple retries with backoff
 */
public class PortAllocator {

    // Start at a high port to avoid conflicts with system services
    private static final int BASE_PORT = 50000;

    // Space ports apart to reduce conflicts between parallel tests
    private static final int PORT_SPACING = 10;

    // Global counter for port allocation
    private static final AtomicInteger portCounter = new AtomicInteger(BASE_PORT);

    // Maximum number of attempts to find an available port
    private static final int MAX_ATTEMPTS = 100;

    /**
     * Allocate an available port for testing.
     *
     * This method tries multiple ports until finding one that's available,
     * helping avoid "Address already in use" errors in rapid test execution.
     *
     * @return an available port number
     * @throws RuntimeException if no available port could be found
     */
    public static int allocatePort() {
        return allocatePort(MAX_ATTEMPTS);
    }

    /**
     * Allocate an available port for testing with a specified maximum number of attempts.
     *
     * @param maxAttempts maximum number of ports to try
     * @return an available port number
     * @throws RuntimeException if no available port could be found
     */
    public static int allocatePort(int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Get the next port with spacing
            int port = portCounter.getAndAdd(PORT_SPACING);

            // Wrap around if we exceed the valid port range
            if (port > 65000) {
                portCounter.set(BASE_PORT);
                port = portCounter.getAndAdd(PORT_SPACING);
            }

            if (isPortAvailable(port)) {
                return port;
            }
        }

        throw new RuntimeException("Could not find an available port after " + maxAttempts + " attempts");
    }

    /**
     * Check if a port is available by attempting to bind to it.
     *
     * @param port the port number to check
     * @return true if the port is available, false otherwise
     */
    public static boolean isPortAvailable(int port) {
        if (port < 1 || port > 65535) {
            return false;
        }

        try (ServerSocket socket = new ServerSocket()) {
            // Enable address reuse to speed up port availability after close
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", port));
            return true;
        } catch (IOException e) {
            // Port is in use or otherwise unavailable
            return false;
        }
    }

}
