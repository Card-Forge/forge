package forge.screens.match.arena;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;

import forge.Classic;
import forge.Forge;
import forge.Graphics;
import forge.screens.FScreen;
import forge.screens.match.MatchScreen;
import forge.toolbox.FDisplayObject;

/**
 * What lets the Arena look change without a cut (port of the Swing {@code ArenaCurtain}, on the GPU). A picture of
 * how things looked a moment ago is laid over the window and fades off, so whatever changed beneath it (another
 * screen, a dialog that has gone, a new game) comes up through it rather than appearing all at once. What is beneath
 * stays live: anything moving there keeps moving while the picture fades.
 *
 * <p>The picture is the whole frame rendered again, at that moment, into an offscreen framebuffer: the current screen
 * and its overlays exactly as the last frame showed them (a curtain still fading included). Drawing it each frame is
 * one full-screen textured quad. Capturing costs one extra render of the frame, once.</p>
 *
 * <p>Wired in already: switching to, from or between match screens (Forge.setCurrentScreen) fades when the Arena look
 * is on. Views call {@link #fadeOver}, {@link #fadeOut(FDisplayObject, int)} or {@link #cover()}/{@link #release(int)}
 * themselves for changes inside the match (a dialog closing, the end screen, the log), <b>before</b> making the change,
 * on the render thread, outside {@code draw()} (input handlers, game-event callbacks posted to the render thread,
 * tween callbacks are all fine). Called while a frame is being drawn they do nothing and return false.</p>
 */
public final class ArenaCurtain {
    private ArenaCurtain() { }

    private static FrameBuffer[] fbos = new FrameBuffer[2];
    private static int current = -1; // index of the fbo holding the picture shown, -1 if none
    private static TextureRegion picture;
    /** Where the picture goes, in Forge coordinates; null for the whole window. */
    private static Rectangle region;
    private static float shown;
    private static boolean holding;
    private static long holdUntil;
    private static ArenaMotion.Tween tween;
    private static int fbWidth, fbHeight;
    private static boolean failed;

    // ---- the API views use ------------------------------------------------------------------------------------------
    /**
     * Photographs the window as it is and holds the picture over it, opaque, until {@link #release}, or for
     * {@link ArenaMotion#HOLD_MOST_MS} whatever happens. For a change made over several steps (Next Game: the new
     * board is laid out over a few frames). True if a curtain this put up now covers the window.
     */
    public static boolean cover() {
        if (!ArenaTheme.isActive() || holding || !capture(null)) {
            return false;
        }
        holding = true;
        holdUntil = System.currentTimeMillis() + ArenaMotion.HOLD_MOST_MS;
        cancelTween();
        shown = 1f;
        ArenaMotion.hold(ArenaCurtain.class);
        return true;
    }

    /** Lets the held curtain fade over {@code millis}, after the frame now being prepared has been shown once. */
    public static void release(final int millis) {
        if (!holding) {
            return;
        }
        holding = false;
        ArenaMotion.release(ArenaCurtain.class);
        startFade(millis, 0);
    }

    /** Photographs the whole window and fades the picture off over {@code millis}. True if a curtain is now up. */
    public static boolean fadeOver(final int millis) {
        return fadeOver(millis, 0);
    }

    /** {@link #fadeOver(int)} after holding the picture for {@code delayMillis} (time for what is beneath to settle). */
    public static boolean fadeOver(final int millis, final int delayMillis) {
        if (!ArenaTheme.isActive() || holding || !capture(null)) {
            return false;
        }
        startFade(millis, delayMillis);
        return true;
    }

    /**
     * For something about to go from the window (a dialog, a browser): a picture of where it is now, fading away in its
     * place. Call it before hiding the thing. Its rectangle is taken from its last drawn screen position. For an
     * overlay that dims the whole window, use {@link #fadeOver} instead so the dimming fades too.
     */
    public static boolean fadeOut(final FDisplayObject what, final int millis) {
        if (what == null) {
            return false;
        }
        final Rectangle r = what.screenPos;
        return fadeOut(r.x, r.y, r.width, r.height, millis);
    }

    /** A picture of this part of the window (Forge coordinates of the whole screen) fading away in its place. */
    public static boolean fadeOut(final float x, final float y, final float w, final float h, final int millis) {
        if (!ArenaTheme.isActive() || holding || w <= 0 || h <= 0 || !capture(new Rectangle(x, y, w, h))) {
            return false;
        }
        startFade(millis, 0);
        return true;
    }

    /** Whether a curtain is over the window (held or fading). */
    public static boolean isUp() {
        return current >= 0 && (holding || shown > 0f);
    }

    /** Whether a curtain is being held. Input may want to wait while it is. */
    public static boolean isHeld() {
        return holding;
    }

    /** Takes any curtain down at once. */
    public static void clear() {
        cancelTween();
        if (holding) {
            holding = false;
            ArenaMotion.release(ArenaCurtain.class);
        }
        shown = 0f;
        current = -1;
        picture = null;
        region = null;
    }

    // ---- wiring (Forge / Classic) ----------------------------------------------------------------------------------
    /**
     * Called by Forge.setCurrentScreen before the screen changes: one match screen into another, a menu into a match
     * or a match back into the menus fades when the Arena look is on. Other screen changes are left alone.
     */
    public static void onScreenChange(final FScreen from, final FScreen to) {
        if (from == null || to == null || from == to || !ArenaTheme.isActive()) {
            return;
        }
        if (!(from instanceof MatchScreen) && !(to instanceof MatchScreen)) {
            return;
        }
        if (holding) {
            return; // a held curtain already covers the change; whoever holds it lets it go
        }
        // into a match the new board loads its card images over its first frames: let it settle under the picture
        final boolean intoMatch = to instanceof MatchScreen;
        fadeOver(intoMatch ? ArenaMotion.SCREEN_FADE_LONG_MS : ArenaMotion.SCREEN_FADE_MS, intoMatch ? 120 : 0);
    }

    /** Draws the curtain over the finished frame. Called by Classic.render after the screen and its overlays. */
    public static void draw(final Graphics g) {
        if (current < 0 || picture == null) {
            return;
        }
        if (holding && System.currentTimeMillis() > holdUntil) {
            release(420); // held too long: whatever was meant to happen beneath, fade now
        }
        if (!holding && shown <= 0f) {
            return;
        }
        final float a = holding ? 1f : shown;
        if (region == null) {
            g.drawImageFaded(picture, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), a);
        } else {
            g.drawImageFaded(picture, region.x, region.y, region.width, region.height, a);
        }
        if (!holding) {
            ArenaMotion.requestFrame();
        }
    }

    // ---- internals --------------------------------------------------------------------------------------------------
    private static void startFade(final int millis, final int delayMillis) {
        cancelTween();
        shown = 1f;
        tween = ArenaMotion.play(Math.max(1, millis), Math.max(0, delayMillis), ArenaMotion.IN_OUT, t -> shown = 1f - t, () -> {
            shown = 0f;
            current = -1;
            picture = null;
            region = null;
            tween = null;
        });
    }

    private static void cancelTween() {
        if (tween != null) {
            tween.cancel();
            tween = null;
        }
    }

    /** Renders the whole frame as it looks now (curtain included) into a spare framebuffer; that becomes the picture. */
    private static boolean capture(final Rectangle part) {
        if (failed || Gdx.graphics == null) {
            return false;
        }
        final Graphics g = Forge.getGraphics();
        if (g == null || g.getBatch().isDrawing()) {
            return false; // mid-frame: cannot render the frame again now
        }
        final int target = current == 0 ? 1 : 0;
        try {
            final FrameBuffer fbo = framebuffer(target);
            if (fbo == null) {
                return false;
            }
            fbo.begin();
            try {
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                Classic.getInstance().renderScene(Forge.getDisplayedScreen(), true);
            } finally {
                fbo.end();
            }
            final Texture tex = fbo.getColorBufferTexture();
            final TextureRegion pic;
            if (part == null) {
                pic = new TextureRegion(tex);
            } else {
                // the framebuffer's rows run bottom up; Forge's y runs top down
                final float sx = fbWidth / (float) Forge.getScreenWidth(), sy = fbHeight / (float) Forge.getScreenHeight();
                final int px = Math.round(part.x * sx), pw = Math.max(1, Math.round(part.width * sx));
                final int ph = Math.max(1, Math.round(part.height * sy));
                final int py = Math.round((Forge.getScreenHeight() - part.y - part.height) * sy);
                pic = new TextureRegion(tex, px, py, pw, ph);
            }
            pic.flip(false, true);
            picture = pic;
            region = part;
            current = target;
            return true;
        } catch (final RuntimeException e) {
            // an effect, not worth breaking the screen change for
            e.printStackTrace();
            failed = true;
            return false;
        }
    }

    private static FrameBuffer framebuffer(final int index) {
        final int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        if (w != fbWidth || h != fbHeight) {
            for (int i = 0; i < fbos.length; i++) {
                if (fbos[i] != null && i != current) {
                    fbos[i].dispose();
                    fbos[i] = null;
                }
            }
            fbWidth = w;
            fbHeight = h;
        }
        FrameBuffer fbo = fbos[index];
        if (fbo == null || fbo.getWidth() != w || fbo.getHeight() != h) {
            if (fbo != null) {
                fbo.dispose();
            }
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
            fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fbos[index] = fbo;
        }
        return fbo;
    }
}
