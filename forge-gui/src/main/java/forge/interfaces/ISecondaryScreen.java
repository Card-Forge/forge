package forge.interfaces;

/**
 * A second physical display rendered from the main GL thread and context
 * (e.g. the bottom screen of a dual-screen handheld such as the AYN Thor).
 * <p>
 * All methods except {@link #setTouchListener} are called on the GL thread.
 */
public interface ISecondaryScreen {
    /** True when the display exists and its surface is ready to draw to. */
    boolean isAvailable();

    /** Width of the drawable area in pixels. */
    int getWidth();

    /** Height of the drawable area in pixels. */
    int getHeight();

    /**
     * Redirects GL output to the secondary display and sets the viewport to its full size.
     * @return false if the frame should be skipped (surface lost, display removed...)
     */
    boolean beginFrame();

    /** Presents the secondary frame and restores GL output to the primary display. */
    void endFrame();

    /** Receives touch/mouse input that happened on the secondary display, in its pixel coordinates (y down). */
    void setTouchListener(TouchListener listener);

    interface TouchListener {
        void touchDown(int x, int y, int pointer);
        void touchDragged(int x, int y, int pointer);
        void touchUp(int x, int y, int pointer);
    }
}
