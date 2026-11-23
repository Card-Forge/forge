package forge.toolbox;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.tinylog.Logger;

import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;

/**
 * Resolves the UI scale of the display the component (or graphics) is currently drawn on.
 *
 * <p>The result is read at call time rather than cached globally: a window can be moved to another
 * output, or the compositor can change its scale, without a restart. AWT is only touched by the
 * resolution methods, never by a static initializer, so console modes remain headless-safe.</p>
 */
public final class FUiScale {
    private FUiScale() {}

    private static final Object LOCK = new Object();
    private static final Map<Window, Float> WINDOW_SCALES = new WeakHashMap<>();
    private static final Set<Window> PENDING_REPAINTS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final AtomicBoolean REPAINT_SCHEDULED = new AtomicBoolean();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Boolean> NOTIFYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Scale observed without a window attached (Graphics, detached components, headless probes).
    private static Float contextFreeScale;

    // Test-only override; null selects the real graphics configuration. The test lives in this
    // package, so no public API is added for it.
    private static volatile Supplier<Float> testProvider;

    /** Resolves the scale for the output {@code c} is currently displayed on. */
    public static float of(final Component c) {
        float scale;
        try {
            GraphicsConfiguration gc = c == null ? null : c.getGraphicsConfiguration();
            scale = resolve(gc == null ? null : gc.getDefaultTransform());
        } catch (final Throwable t) {
            scale = resolve(null);
        }
        Window window = c instanceof Window ? (Window) c : (c == null ? null : SwingUtilities.getWindowAncestor(c));
        trackScaleChange(window, scale);
        return scale;
    }

    /** Resolves the scale from the device configuration of the graphics being painted. */
    public static float of(final Graphics g) {
        float scale;
        try {
            GraphicsConfiguration gc = g instanceof Graphics2D ? ((Graphics2D) g).getDeviceConfiguration() : null;
            scale = resolve(gc == null ? null : gc.getDefaultTransform());
        } catch (final Throwable t) {
            scale = resolve(null);
        }
        trackScaleChange(null, scale);
        return scale;
    }

    /** The startup/fallback scale, also used whenever no usable transform is available. */
    public static float fallback() {
        IGuiBase gui = GuiBase.getInterface();
        return gui == null ? 1f : gui.getScreenScale();
    }

    public static void addScaleChangeListener(final Runnable r) {
        if (r != null) {
            LISTENERS.addIfAbsent(r);
        }
    }

    public static void removeScaleChangeListener(final Runnable r) {
        if (r != null) {
            LISTENERS.remove(r);
        }
    }

    private static float resolve(final AffineTransform transform) {
        Supplier<Float> provider = testProvider;
        if (provider != null) {
            try {
                Float supplied = provider.get();
                if (isValid(supplied)) {
                    return supplied;
                }
            } catch (final RuntimeException e) {
                // Fall through to the regular fallback for a broken provider.
            }
            return fallback();
        }
        if (transform == null) {
            return fallback();
        }
        float scaleX = (float) transform.getScaleX();
        return isValid(scaleX) ? scaleX : fallback();
    }

    private static boolean isValid(final Float value) {
        return value != null && Float.isFinite(value) && value > 0;
    }

    private static void trackScaleChange(final Window window, final float scale) {
        boolean changed;
        synchronized (LOCK) {
            Float previous = window == null ? contextFreeScale : WINDOW_SCALES.get(window);
            changed = previous == null || Float.floatToIntBits(previous) != Float.floatToIntBits(scale);
            if (!changed) {
                return;
            }
            if (window == null) {
                contextFreeScale = scale;
            } else {
                WINDOW_SCALES.put(window, scale);
                PENDING_REPAINTS.add(window);
            }
            // A listener that calls back into of() must not recurse into another notification.
            if (NOTIFYING.get()) {
                changed = false;
            }
        }
        if (changed && (window != null || !LISTENERS.isEmpty()) && REPAINT_SCHEDULED.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(FUiScale::flushScaleChanges);
        }
    }

    private static void flushScaleChanges() {
        final List<Window> windows;
        synchronized (LOCK) {
            windows = new ArrayList<>(PENDING_REPAINTS);
            PENDING_REPAINTS.clear();
            // Allow a change occurring during repaint/notification to schedule a fresh pass.
            REPAINT_SCHEDULED.set(false);
        }
        for (Window window : windows) {
            try {
                window.revalidate();
                window.repaint();
            } catch (final Throwable t) {
                Logger.error(t, "Failed to repaint after a UI scale change");
            }
        }
        NOTIFYING.set(Boolean.TRUE);
        try {
            for (Runnable listener : LISTENERS) {
                try {
                    listener.run();
                } catch (final Throwable t) {
                    Logger.error(t, "UI scale change listener failed");
                }
            }
        } finally {
            NOTIFYING.set(Boolean.FALSE);
        }
    }

    // Test-only hooks; no AWT display or application startup is needed to exercise resolution.
    static void setScaleProviderForTest(final Supplier<Float> provider) {
        testProvider = provider;
    }

    static void checkScaleChange() {
        float scale = testProvider == null ? fallback() : resolve(null);
        trackScaleChange(null, scale);
    }

    static void resetForTest() {
        testProvider = null;
        synchronized (LOCK) {
            WINDOW_SCALES.clear();
            PENDING_REPAINTS.clear();
            contextFreeScale = null;
            REPAINT_SCHEDULED.set(false);
        }
    }
}
