package forge.app;

import forge.interfaces.ISecondaryScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Desktop stand-in for a dual-screen handheld's bottom display: a second GLFW window that shares the main
 * window's GL context. Each frame the shared context objects are drawn into it by switching the current window,
 * which mirrors how the Android implementation switches EGL surfaces.
 * <p>
 * Enable with {@code -Dforge.dualscreen=sim}; size with {@code -Dforge.dualscreen.size=WIDTHxHEIGHT}
 * (default 620x540, half the AYN Thor's 1240x1080 bottom screen).
 */
public class DesktopSecondaryScreen implements ISecondaryScreen {
    private final int width, height;
    private long window;
    private long primaryWindow;
    private GLCapabilities capabilities;
    private GLCapabilities primaryCapabilities;
    private boolean failed;
    private boolean mouseDown;
    private volatile TouchListener touchListener;

    public static DesktopSecondaryScreen fromSystemProperties() {
        if (!"sim".equalsIgnoreCase(System.getProperty("forge.dualscreen"))) {
            return null;
        }
        int w = 620, h = 540;
        String size = System.getProperty("forge.dualscreen.size");
        if (size != null) {
            String[] parts = size.toLowerCase().split("x");
            try {
                w = Integer.parseInt(parts[0].trim());
                h = Integer.parseInt(parts[1].trim());
            } catch (Exception e) {
                System.err.println("Ignoring bad forge.dualscreen.size: " + size);
            }
        }
        return new DesktopSecondaryScreen(w, h);
    }

    public DesktopSecondaryScreen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean isAvailable() {
        return !failed;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean beginFrame() {
        if (failed) {
            return false;
        }
        primaryWindow = GLFW.glfwGetCurrentContext();
        primaryCapabilities = GL.getCapabilities();
        if (window == 0 && !createWindow()) {
            return false;
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.setCapabilities(capabilities);
        int[] fbw = new int[1], fbh = new int[1];
        GLFW.glfwGetFramebufferSize(window, fbw, fbh);
        GL11.glViewport(0, 0, fbw[0], fbh[0]);
        return true;
    }

    @Override
    public void endFrame() {
        if (window != 0) {
            GLFW.glfwSwapBuffers(window);
        }
        GLFW.glfwMakeContextCurrent(primaryWindow);
        GL.setCapabilities(primaryCapabilities);
    }

    @Override
    public void setTouchListener(TouchListener listener) {
        touchListener = listener;
    }

    private boolean createWindow() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(width, height, "Forge - bottom screen", 0, primaryWindow);
        if (window == 0) {
            System.err.println("Dual-screen sim: could not create the bottom-screen window");
            failed = true;
            GLFW.glfwMakeContextCurrent(primaryWindow);
            return false;
        }
        //place it under the main window
        int[] px = new int[1], py = new int[1], pw = new int[1], ph = new int[1];
        GLFW.glfwGetWindowPos(primaryWindow, px, py);
        GLFW.glfwGetWindowSize(primaryWindow, pw, ph);
        int winX = px[0] + Math.max(0, (pw[0] - width) / 2), winY = py[0] + ph[0] + 32;
        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor != 0) {
            int[] ax = new int[1], ay = new int[1], aw = new int[1], ah = new int[1];
            GLFW.glfwGetMonitorWorkarea(monitor, ax, ay, aw, ah);
            if (winY + height > ay[0] + ah[0]) { //no room below: put it beside the main window instead
                winX = px[0] + pw[0] + 16;
                winY = py[0];
                if (winX + width > ax[0] + aw[0]) {
                    winX = ax[0] + aw[0] - width;
                }
            }
        }
        GLFW.glfwSetWindowPos(window, winX, winY);
        GLFW.glfwShowWindow(window);
        GLFW.glfwFocusWindow(primaryWindow); //keep keyboard/controller focus on the main window

        GLFW.glfwMakeContextCurrent(window);
        capabilities = GL.createCapabilities();
        GLFW.glfwSwapInterval(0); //the main window already waits for vsync
        int[] ww = new int[1], wh = new int[1], fw = new int[1], fh = new int[1];
        GLFW.glfwGetWindowSize(window, ww, wh);
        GLFW.glfwGetFramebufferSize(window, fw, fh);
        System.out.println("Dual-screen sim: window " + ww[0] + "x" + wh[0] + ", framebuffer " + fw[0] + "x" + fh[0]);

        GLFW.glfwSetWindowCloseCallback(window, w -> GLFW.glfwSetWindowShouldClose(w, false));
        GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return;
            }
            TouchListener l = touchListener;
            int[] pos = cursorPos();
            if (action == GLFW.GLFW_PRESS) {
                mouseDown = true;
                if (l != null) l.touchDown(pos[0], pos[1], 0);
            } else if (action == GLFW.GLFW_RELEASE && mouseDown) {
                mouseDown = false;
                if (l != null) l.touchUp(pos[0], pos[1], 0);
            }
        });
        GLFW.glfwSetCursorPosCallback(window, (w, x, y) -> {
            TouchListener l = touchListener;
            if (mouseDown && l != null) {
                l.touchDragged((int) x, (int) y, 0);
            }
        });
        return true;
    }

    private int[] cursorPos() {
        double[] x = new double[1], y = new double[1];
        GLFW.glfwGetCursorPos(window, x, y);
        return new int[] { (int) x[0], (int) y[0] };
    }
}
