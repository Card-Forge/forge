package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.util.Utils;

/**
 * The one selection box of the match screen: a dashed outline whose dashes slide around the edge ("marching ants").
 * Anything the player can pick (cards in any zone, players, stack entries) and the cursor boxes use it, so they all
 * read the same. The two colours of the dashes alternate, which keeps the box visible on light and dark backgrounds.
 * <p>
 * Chosen things are not drawn with this box: they keep a solid {@link #CHOSEN_COLOR} outline, so "can be picked"
 * and "is picked" never look alike.
 * <p>
 * Drawing a box asks for the next frame, so the animation keeps itself going while a box is on screen and stops
 * (with no continuous rendering to switch off) once nothing draws one any more.
 */
public final class FSelectionBox {
    /** Solid outline of things already chosen in the current prompt (cards, players and stack entries alike). */
    public static final Color CHOSEN_COLOR = Color.MAGENTA; // some card have gold or yelllow so make this distinct

    private static final float DASH = Utils.scale(5);
    private static final long CYCLE_MS = 700; // time for the dashes to slide by one full pattern
    private static final float DEFAULT_THICKNESS = Utils.scale(2);

    // one phase and one theme colour per rendered frame: every box of the frame moves in step, and the clock, the skin
    // lookup and the render request are done once per frame instead of once per box
    private static long frameId = -1;
    private static float framePhase;
    private static Color frameColor;

    private FSelectionBox() {
    }

    /** Solid outline of something already chosen; never thinner than the dashed box so the two read as a pair. */
    public static void drawChosen(final Graphics g, final float thickness, final float x, final float y, final float w, final float h) {
        g.drawRect(Math.max(thickness, DEFAULT_THICKNESS), CHOSEN_COLOR, x, y, w, h);
    }

    /** Colour of the box around things that can be picked. */
    public static Color validColor() {
        syncFrame();
        return frameColor;
    }

    /** Box around something that can be picked, in the theme's active colour. */
    public static void draw(final Graphics g, final float x, final float y, final float w, final float h) {
        draw(g, validColor(), DEFAULT_THICKNESS, x, y, w, h);
    }

    /** Same box with its own line thickness, for things drawn at a very different size (avatars). */
    public static void draw(final Graphics g, final float thickness, final float x, final float y, final float w, final float h) {
        draw(g, validColor(), thickness, x, y, w, h);
    }

    /** Same box in another colour (cursor boxes). The dashes alternate between {@code color} and {@link #CHOSEN_COLOR}. */
    public static void draw(final Graphics g, final Color color, final float thickness, final float x, final float y, final float w, final float h) {
        syncFrame();
        g.drawAlternatingDashedRect(thickness, color, CHOSEN_COLOR, x, y, w, h, DASH, framePhase);
    }

    private static void syncFrame() {
        final long frame = Gdx.graphics.getFrameId();
        if (frame == frameId) {
            return;
        }
        frameId = frame;
        framePhase = (System.currentTimeMillis() % CYCLE_MS) / (float) CYCLE_MS * 2 * DASH; // seamless when the cycle wraps
        frameColor = (Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_THEME) : FSkinColor.get(Colors.CLR_THEME)).getColor();
        Gdx.graphics.requestRendering(); // next frame, so the dashes keep moving
    }
}
