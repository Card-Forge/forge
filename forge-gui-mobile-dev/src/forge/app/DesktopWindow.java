package forge.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Utils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowPosCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

/**
 * The desktop window of the libGDX client: a normal resizable window that opens maximized the first time, remembers
 * where it was and how big, and switches to borderless fullscreen on its monitor (no display mode change, so Alt+Tab
 * and other monitors keep working) and back without a restart.
 * <p>
 * All of it runs on the main thread, which on Windows and Linux is also the render thread.
 */
public final class DesktopWindow {
    /** The interface is laid out for at least this many points (1/480 of the design height) tall; see Utils.scale. */
    private static final float MIN_POINTS_TALL = 360f, MIN_POINTS_WIDE = 560f;

    private static Lwjgl3Window window;
    /** Started with width=/height= arguments (developers, scripts): keep that size and leave the saved one alone. */
    private static boolean fixedSize;
    /** The window's bounds the last time it was neither maximized, minimized nor fullscreen (client area, screen px). */
    private static int normalX, normalY, normalW, normalH;
    private static boolean fullscreen, maximizedBeforeFullscreen;
    private static GLFWWindowPosCallback posCallback;
    private static GLFWWindowSizeCallback sizeCallback;

    private DesktopWindow() {
    }

    /**
     * Sets where and how the window opens. {@code fixedWidth}/{@code fixedHeight} are the command line size (0 when not
     * given); {@code fallbackWidth}/{@code fallbackHeight} the Video Mode size, used the first time Forge leaves the
     * maximized window it starts in.
     */
    static void configure(Lwjgl3ApplicationConfiguration config, ForgePreferences prefs, boolean fullscreen0,
                          int fixedWidth, int fixedHeight, int fallbackWidth, int fallbackHeight) {
        config.setResizable(true);
        config.setAutoIconify(false);
        if (fixedWidth > 0 && fixedHeight > 0) {
            fixedSize = true;
            config.setWindowedMode(fixedWidth, fixedHeight);
            return;
        }
        Monitor monitor = Lwjgl3ApplicationConfiguration.getPrimaryMonitor();
        int[] saved = parseBounds(prefs.getPref(FPref.UI_WINDOW_BOUNDS));
        if (saved != null) {
            Monitor onIt = monitorAt(saved[0] + saved[2] / 2, saved[1] + Math.min(saved[3] / 2, 20));
            if (onIt == null) {
                saved = null; //that monitor is gone: open on the main one
            } else {
                monitor = onIt;
            }
        }
        int[] area = workArea(monitor);
        if (saved != null) {
            normalW = Math.min(saved[2], area[2]);
            normalH = Math.min(saved[3], area[3]);
            normalX = saved[0];
            normalY = saved[1];
        } else {
            normalW = Math.min(fallbackWidth, area[2] * 9 / 10);
            normalH = Math.min(fallbackHeight, area[3] * 9 / 10);
            normalX = area[0] + (area[2] - normalW) / 2;
            normalY = area[1] + (area[3] - normalH) / 2;
        }
        maximizedBeforeFullscreen = prefs.getPrefBoolean(FPref.UI_WINDOW_MAXIMIZED);
        if (fullscreen0) {
            fullscreen = true;
            int[] full = monitorBounds(monitor);
            config.setDecorated(false);
            config.setWindowedMode(full[2], full[3]);
            config.setWindowPosition(full[0], full[1]);
        } else {
            config.setWindowedMode(normalW, normalH);
            config.setWindowPosition(normalX, normalY);
            if (maximizedBeforeFullscreen) {
                config.setMaximized(true);
                config.setMaximizedMonitor(monitor);
            }
        }
    }

    /** The window exists (main thread, before Forge's create): follow its moves and sizes from now on. */
    public static void attach(Lwjgl3Window window0) {
        window = window0;
        long handle = window.getWindowHandle();
        posCallback = GLFWWindowPosCallback.create((h, x, y) -> rememberNormalBounds());
        sizeCallback = GLFWWindowSizeCallback.create((h, w, ht) -> rememberNormalBounds());
        //libGDX sets neither callback; free whatever was there before ours (nothing, normally)
        GLFWWindowPosCallback oldPos = GLFW.glfwSetWindowPosCallback(handle, posCallback);
        GLFWWindowSizeCallback oldSize = GLFW.glfwSetWindowSizeCallback(handle, sizeCallback);
        if (oldPos != null) { oldPos.free(); }
        if (oldSize != null) { oldSize.free(); }
        rememberNormalBounds();
        //Forge has been created by the time the window runs its first runnable, so the interface scale is known
        window.postRunnable(DesktopWindow::applySizeLimits);
    }

    /** Never smaller than the interface fits in (it is drawn at the scale chosen at start-up). */
    private static void applySizeLimits() {
        if (window == null || fixedSize) {
            return;
        }
        int minH = Math.round(Utils.scale(MIN_POINTS_TALL));
        int minW = Math.round(Utils.scale(MIN_POINTS_WIDE));
        int[] area = workArea(currentMonitor());
        minW = Math.max(640, Math.min(minW, area[2] * 3 / 4));
        minH = Math.max(400, Math.min(minH, area[3] * 3 / 4));
        window.setSizeLimits(minW, minH, -1, -1);
    }

    private static void rememberNormalBounds() {
        if (window == null || fullscreen) {
            return;
        }
        long handle = window.getWindowHandle();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE
                || GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
            return;
        }
        int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetWindowSize(handle, w, h);
        if (w[0] > 0 && h[0] > 0) {
            normalX = x[0];
            normalY = y[0];
            normalW = w[0];
            normalH = h[0];
        }
    }

    static boolean isFullscreen() {
        return fullscreen;
    }

    /** Borderless fullscreen on the monitor the window is on, or back to the window as it was (maximized or not). */
    static void setFullscreen(boolean on) {
        if (window == null || on == fullscreen) {
            return;
        }
        long handle = window.getWindowHandle();
        if (on) {
            rememberNormalBounds();
            Monitor monitor = currentMonitor();
            maximizedBeforeFullscreen = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
            fullscreen = true;
            if (maximizedBeforeFullscreen) {
                GLFW.glfwRestoreWindow(handle);
            }
            int[] full = monitorBounds(monitor);
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(handle, 0L, full[0], full[1], full[2], full[3], GLFW.GLFW_DONT_CARE);
        } else {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            int[] area = workArea(currentMonitor());
            if (normalW <= 0 || normalH <= 0 || monitorAt(normalX + normalW / 2, normalY + Math.min(normalH / 2, 20)) == null) {
                normalW = area[2] * 3 / 4;
                normalH = area[3] * 3 / 4;
                normalX = area[0] + (area[2] - normalW) / 2;
                normalY = area[1] + (area[3] - normalH) / 2;
            }
            GLFW.glfwSetWindowMonitor(handle, 0L, normalX, normalY, normalW, normalH, GLFW.GLFW_DONT_CARE);
            fullscreen = false;
            if (maximizedBeforeFullscreen) {
                GLFW.glfwMaximizeWindow(handle);
            }
        }
        Gdx.graphics.requestRendering();
    }

    /** A plain window of this size (as big as the work area at most), centred on the monitor it is on. */
    static void setWindowSize(int width, int height) {
        if (window == null) {
            return;
        }
        if (fullscreen) {
            maximizedBeforeFullscreen = false;
            setFullscreen(false);
        }
        long handle = window.getWindowHandle();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE) {
            GLFW.glfwRestoreWindow(handle);
        }
        int[] area = workArea(currentMonitor());
        int w = Math.min(width, area[2]), h = Math.min(height, area[3] - 40); //room for the title bar
        GLFW.glfwSetWindowSize(handle, w, h);
        window.setPosition(area[0] + (area[2] - w) / 2, area[1] + Math.max(30, (area[3] - h) / 2));
        Gdx.graphics.requestRendering();
    }

    /** Width and height of the monitor the window is on. */
    static int[] getMonitorSize() {
        int[] full = monitorBounds(window == null ? Lwjgl3ApplicationConfiguration.getPrimaryMonitor() : currentMonitor());
        return new int[] { full[2], full[3] };
    }

    /** Writes how the window is now (maximized, fullscreen, its normal bounds) to the preferences: Forge is closing. */
    static void save() {
        if (window == null || fixedSize) {
            return;
        }
        try {
            rememberNormalBounds();
            boolean maximized = fullscreen ? maximizedBeforeFullscreen
                    : GLFW.glfwGetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
            ForgePreferences prefs = FModel.getPreferences();
            if (prefs == null) {
                return; //closing during start-up: nothing loaded to save into
            }
            if (normalW > 0 && normalH > 0) {
                prefs.setPref(FPref.UI_WINDOW_BOUNDS, normalX + "," + normalY + "," + normalW + "," + normalH);
            }
            prefs.setPref(FPref.UI_WINDOW_MAXIMIZED, maximized);
            prefs.save();
        } catch (RuntimeException e) {
            e.printStackTrace(); //never stand in the way of closing
        }
    }

    private static int[] parseBounds(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String[] p = s.split(",");
        if (p.length != 4) {
            return null;
        }
        try {
            int[] b = new int[4];
            for (int i = 0; i < 4; i++) {
                b[i] = Integer.parseInt(p[i].trim());
            }
            return b[2] >= 200 && b[3] >= 150 ? b : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The monitor the window's centre is on (the primary one if it is somehow on none). */
    private static Monitor currentMonitor() {
        if (window == null) {
            return Lwjgl3ApplicationConfiguration.getPrimaryMonitor();
        }
        long handle = window.getWindowHandle();
        int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetWindowSize(handle, w, h);
        Monitor m = monitorAt(x[0] + w[0] / 2, y[0] + h[0] / 2);
        if (m == null && Gdx.graphics instanceof Lwjgl3Graphics) {
            m = Gdx.graphics.getMonitor();
        }
        return m != null ? m : Lwjgl3ApplicationConfiguration.getPrimaryMonitor();
    }

    private static Monitor monitorAt(int x, int y) {
        for (Monitor m : Lwjgl3ApplicationConfiguration.getMonitors()) {
            int[] b = monitorBounds(m);
            if (x >= b[0] && y >= b[1] && x < b[0] + b[2] && y < b[1] + b[3]) {
                return m;
            }
        }
        return null;
    }

    /** x, y, width, height of the whole monitor in screen coordinates. */
    private static int[] monitorBounds(Monitor m) {
        DisplayMode mode = Lwjgl3ApplicationConfiguration.getDisplayMode(m);
        return new int[] { m.virtualX, m.virtualY, mode.width, mode.height };
    }

    /** x, y, width, height of the monitor without the taskbar and docks. */
    private static int[] workArea(Monitor m) {
        if (m instanceof Lwjgl3Graphics.Lwjgl3Monitor) {
            int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
            GLFW.glfwGetMonitorWorkarea(((Lwjgl3Graphics.Lwjgl3Monitor) m).getMonitorHandle(), x, y, w, h);
            if (w[0] > 0 && h[0] > 0) {
                return new int[] { x[0], y[0], w[0], h[0] };
            }
        }
        return monitorBounds(m);
    }
}
