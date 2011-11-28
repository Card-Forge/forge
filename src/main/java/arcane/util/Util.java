/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package arcane.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

/**
 * <p>
 * Util class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Util {
    /**
     * Constant.
     * <code>isMac=System.getProperty("os.name").toLowerCase().indexOf("mac") != -1</code>.
     */
    public static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    /**
     * Constant.
     * <code>isWindows=System.getProperty("os.name").toLowerCase().indexOf("windows") == -1</code>
     */
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("windows") == -1;

    /** Constant <code>threadPool</code>. */
    private static ThreadPoolExecutor threadPool;
    /** Constant <code>threadCount</code>. */
    private static int threadCount;

    static {
        threadPool = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    public Thread newThread(final Runnable runnable) {
                        threadCount++;
                        Thread thread = new Thread(runnable, "Util" + threadCount);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        threadPool.prestartAllCoreThreads();
    }

    /**
     * <p>
     * broadcast.
     * </p>
     *
     * @param data an array of byte.
     * @param port a int.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void broadcast(final byte[] data, final int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        broadcast(socket, data, port, NetworkInterface.getNetworkInterfaces());
        socket.close();
    }

    /**
     * <p>
     * broadcast.
     * </p>
     * 
     * @param socket
     *            a {@link java.net.DatagramSocket} object.
     * @param data
     *            an array of byte.
     * @param port
     *            a int.
     * @param ifaces
     *            a {@link java.util.Enumeration} object.
     * @throws java.io.IOException
     *             if any.
     */
    private static void broadcast(final DatagramSocket socket,
            final byte[] data, final int port, final Enumeration<NetworkInterface> ifaces)
            throws IOException {
        for (NetworkInterface iface : Collections.list(ifaces)) {
            for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                if (!address.isSiteLocalAddress()) {
                    continue;
                }
                // Java 1.5 doesn't support getting the subnet mask, so try the
                // two most common.
                byte[] ip = address.getAddress();
                ip[3] = -1; // 255.255.255.0
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), port));
                ip[2] = -1; // 255.255.0.0
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), port));
            }
        }
    }

    /**
     * <p>
     * invokeAndWait.
     * </p>
     *
     * @param runnable a {@link java.lang.Runnable} object.
     * @throws Exception the exception
     */
    public static void invokeAndWait(final Runnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (Exception ex) {
            throw new Exception("Error invoking runnable in UI thread.", ex);
        }
    }
}
