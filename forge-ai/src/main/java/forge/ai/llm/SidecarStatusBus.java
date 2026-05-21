package forge.ai.llm;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.tinylog.Logger;

/**
 * Process-wide pub/sub for "AI is waiting on the sidecar" status events.
 *
 * <p>The AI decision threads fire {@link #fireThinkingStart()} / {@link #fireThinkingEnd()}
 * around blocking calls into the LLM sidecar. UI layers (e.g. forge-gui-desktop's
 * {@code CMatchUI}) subscribe to display a transient "AI is thinking…" indicator
 * without taking a hard dependency on the sidecar protocol.</p>
 *
 * <p>Fail-soft: listener exceptions are logged and swallowed so a misbehaving UI
 * listener can never break the AI decision loop.</p>
 */
public final class SidecarStatusBus {

    public interface Listener {
        void onThinkingStart();
        void onThinkingEnd();
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private SidecarStatusBus() {}

    public static void addListener(final Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(final Listener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static void fireThinkingStart() {
        for (final Listener l : LISTENERS) {
            try {
                l.onThinkingStart();
            } catch (final Throwable t) {
                Logger.debug(t, "SidecarStatusBus: listener threw on start");
            }
        }
    }

    public static void fireThinkingEnd() {
        for (final Listener l : LISTENERS) {
            try {
                l.onThinkingEnd();
            } catch (final Throwable t) {
                Logger.debug(t, "SidecarStatusBus: listener threw on end");
            }
        }
    }
}
