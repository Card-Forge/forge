package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Align;

import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.gui.error.BugReporter;
import forge.interfaces.ISecondaryScreen;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.util.Utils;

/**
 * Renders the second physical display (e.g. the AYN Thor bottom screen) from the main GL thread.
 * The current {@link FScreen} supplies the content via {@link FScreen#getSecondaryRoot()};
 * screens that have nothing to show there get an idle panel with controller hints.
 */
public final class SecondaryDisplay {
    private static OrthographicCamera camera;
    private static int width, height;
    private static FContainer lastRoot;
    private static final IdlePanel idlePanel = new IdlePanel();

    private SecondaryDisplay() { }

    static ISecondaryScreen getScreen() {
        return Forge.getDeviceAdapter() == null ? null : Forge.getDeviceAdapter().getSecondaryScreen();
    }

    /** True when a secondary display exists and dual-screen mode is enabled in preferences. */
    public static boolean isEnabled() {
        ISecondaryScreen s = getScreen();
        if (s == null) {
            return false;
        }
        try {
            return FModel.getPreferences() == null || FModel.getPreferences().getPrefBoolean(FPref.UI_DUAL_SCREEN);
        } catch (Exception e) {
            return true; //preferences not loaded yet
        }
    }

    /** True when the secondary display is enabled and its surface can be drawn to right now. */
    public static boolean isActive() {
        return isEnabled() && getScreen().isAvailable();
    }

    public static int getWidth() {
        ISecondaryScreen s = getScreen();
        return s == null ? 0 : s.getWidth();
    }

    public static int getHeight() {
        ISecondaryScreen s = getScreen();
        return s == null ? 0 : s.getHeight();
    }

    /** The container currently shown on the secondary display (for input hit-testing), or null. */
    public static FContainer getCurrentRoot() {
        return lastRoot;
    }

    /** Called from {@link Forge#render()} after the primary display was drawn. */
    static void render(FContainer primaryScreen) {
        ISecondaryScreen s = getScreen();
        if (s == null || !isEnabled() || !s.isAvailable()) {
            lastRoot = null;
            return;
        }
        if (!s.beginFrame()) {
            return;
        }
        try {
            int w = s.getWidth(), h = s.getHeight();
            boolean resized = w != width || h != height || camera == null;
            if (resized) {
                width = w;
                height = h;
                camera = new OrthographicCamera(w, h);
                camera.position.set(w / 2f, h / 2f, 0);
                camera.update();
            }
            FContainer root = null;
            if (primaryScreen instanceof FScreen) {
                root = ((FScreen) primaryScreen).getSecondaryRoot();
            }
            if (root == null) {
                root = idlePanel;
            }
            if (root != lastRoot || resized || root.getWidth() != w || root.getHeight() != h) {
                root.setSize(w, h);
                lastRoot = root;
            }
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Classic.getInstance().renderSecondary(root, camera, w, h);
        } catch (Exception e) {
            BugReporter.reportException(e);
        } finally {
            s.endFrame();
        }
    }

    /** Shown on the secondary display when the current screen has nothing for it. */
    private static final class IdlePanel extends FContainer {
        private static final Color TEXT = new Color(1f, 1f, 1f, 0.85f);

        @Override
        protected void doLayout(float width, float height) {
        }

        @Override
        protected void drawBackground(Graphics g) {
            float w = getWidth(), h = getHeight();
            g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
            FSkinFont title = FSkinFont.get(28);
            FSkinFont body = FSkinFont.get(14);
            float pad = Utils.scale(12);
            g.drawText("Forge", title, TEXT, 0, h * 0.18f, w, title.getLineHeight(), false, Align.center, true);
            String hints = Forge.hasGamepad()
                    ? "D-pad: move    A: select    B: back\nL1 / R1: switch    Start: menu"
                    : "Touch the top screen to navigate";
            g.drawText(hints, body, TEXT, pad, h * 0.45f, w - 2 * pad, h * 0.4f, true, Align.center, false);
        }
    }
}
