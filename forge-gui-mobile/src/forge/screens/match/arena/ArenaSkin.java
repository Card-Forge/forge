package forge.screens.match.arena;

import forge.Graphics;
import forge.toolbox.FDisplayObject;

/**
 * How a view gets the Arena look: the view keeps one skin object and asks it to draw first; if the skin does not
 * draw (Arena look off, or the skin declines for this frame), the view draws its default look exactly as before.
 * Each hook mirrors the view method it stands in for. Return {@code true} only if you drew (then the view returns);
 * {@code false} falls back to the default look. See the package documentation for the full pattern.
 *
 * <pre>{@code
 * public class VAvatar extends FDisplayObject {
 *     private static final ArenaSkin<VAvatar> ARENA = new ArenaAvatarSkin();   // in forge.screens.match.arena
 *
 *     public void draw(Graphics g) {
 *         if (ArenaSkin.draw(ARENA, g, this)) { return; }
 *         ... the default look, untouched ...
 *     }
 * }
 * }</pre>
 *
 * Skins are stateless or hold only per-view animation state keyed by the view; they run on the render thread inside
 * {@code draw}, so they must not allocate per frame, block, or change game state.
 *
 * @param <V> the view this skin draws
 */
public interface ArenaSkin<V extends FDisplayObject> {
    /** Stands in for {@link FDisplayObject#draw(Graphics)} (whole view). */
    default boolean draw(final Graphics g, final V view) {
        return false;
    }

    /** Stands in for {@code FContainer.drawBackground(Graphics)}: what goes under the children. */
    default boolean drawBackground(final Graphics g, final V view) {
        return false;
    }

    /** Stands in for {@code FContainer.drawOverlay(Graphics)}: what goes over the children. */
    default boolean drawOverlay(final Graphics g, final V view) {
        return false;
    }

    /**
     * Stands in for {@code FContainer.doLayout(width, height)} when the Arena look needs a different arrangement of the
     * children. Return {@code true} if you laid them out. (Layout runs again when the look is switched; see
     * {@link ArenaTheme#addListener}.)
     */
    default boolean doLayout(final V view, final float width, final float height) {
        return false;
    }

    // ---- the calls views make: each is a field read when the Arena look is off --------------------------------------

    static <V extends FDisplayObject> boolean draw(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isActive() && skin.draw(g, view);
    }

    static <V extends FDisplayObject> boolean drawBackground(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isActive() && skin.drawBackground(g, view);
    }

    static <V extends FDisplayObject> boolean drawOverlay(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isActive() && skin.drawOverlay(g, view);
    }

    static <V extends FDisplayObject> boolean doLayout(final ArenaSkin<V> skin, final V view, final float width, final float height) {
        return skin != null && ArenaTheme.isActive() && skin.doLayout(view, width, height);
    }

    /**
     * For widgets shared with the menus (dialogs, option panes, choice lists): like {@link #draw} but only on the match
     * screen ({@link ArenaTheme#isMatchBoard()}), so the menus keep the default look.
     */
    static <V extends FDisplayObject> boolean drawOnMatch(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isMatchBoard() && skin.draw(g, view);
    }

    static <V extends FDisplayObject> boolean drawBackgroundOnMatch(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isMatchBoard() && skin.drawBackground(g, view);
    }

    static <V extends FDisplayObject> boolean drawOverlayOnMatch(final ArenaSkin<V> skin, final Graphics g, final V view) {
        return skin != null && ArenaTheme.isMatchBoard() && skin.drawOverlay(g, view);
    }
}
