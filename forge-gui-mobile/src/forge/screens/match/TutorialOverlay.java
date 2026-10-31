package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import forge.Graphics;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

/**
 * Points the "How to Play" tutorial at parts of the match screen: the rest of the screen is dimmed and the parts get a
 * gold outline. It takes no touches, so the game underneath works as usual, and it is drawn below the coach note.
 *
 * <p>It also tells the match screen once a frame to place the note again whenever something it must stay clear of has
 * moved (a highlighted part, the stack list, the phase stops list), because those move without the screen being laid
 * out again.
 */
class TutorialOverlay extends FDisplayObject {
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color GLOW = new Color(1f, 0.82f, 0.35f, 0.35f);
    private static final Color EDGE = new Color(1f, 0.82f, 0.35f, 1f);
    private static final float MIN_PIECE = 0.5f;

    private final MatchScreen screen;

    TutorialOverlay(final MatchScreen screen0) {
        screen = screen0;
    }

    @Override
    public void draw(final Graphics g) {
        screen.syncTutorialLayout();
        final List<Rectangle> targets = screen.getTutorialRects();
        if (targets.isEmpty()) {
            return;
        }
        final float pad = Utils.scale(3);
        final float w = getWidth(), h = getHeight();

        final List<Rectangle> holes = new ArrayList<>();
        for (final Rectangle r : targets) {
            final float x = Math.max(0f, r.x - pad), y = Math.max(0f, r.y - pad);
            final float x2 = Math.min(w, r.x + r.width + pad), y2 = Math.min(h, r.y + r.height + pad);
            if (x2 > x && y2 > y) {
                holes.add(new Rectangle(x, y, x2 - x, y2 - y));
            }
        }

        // Dim everything but the holes: start with the whole screen and cut each hole out of the pieces.
        List<Rectangle> pieces = new ArrayList<>();
        pieces.add(new Rectangle(0, 0, w, h));
        for (final Rectangle hole : holes) {
            final List<Rectangle> next = new ArrayList<>();
            for (final Rectangle piece : pieces) {
                subtract(piece, hole, next);
            }
            pieces = next;
        }
        for (final Rectangle piece : pieces) {
            g.fillRect(DIM, piece.x, piece.y, piece.width, piece.height);
        }

        final float edge = Math.max(2f, Utils.scale(2));
        for (final Rectangle hole : holes) {
            g.drawRect(edge * 2f, GLOW, hole.x - edge, hole.y - edge, hole.width + 2 * edge, hole.height + 2 * edge);
            g.drawRect(edge, EDGE, hole.x, hole.y, hole.width, hole.height);
        }
    }

    /** Adds to {@code out} what is left of {@code piece} once {@code hole} is cut out of it (up to four rectangles). */
    private static void subtract(final Rectangle piece, final Rectangle hole, final List<Rectangle> out) {
        if (!piece.overlaps(hole)) {
            out.add(piece);
            return;
        }
        final float left = Math.max(piece.x, hole.x), right = Math.min(piece.x + piece.width, hole.x + hole.width);
        final float top = Math.max(piece.y, hole.y), bottom = Math.min(piece.y + piece.height, hole.y + hole.height);
        add(out, piece.x, piece.y, piece.width, top - piece.y); // above
        add(out, piece.x, bottom, piece.width, piece.y + piece.height - bottom); // below
        add(out, piece.x, top, left - piece.x, bottom - top); // left
        add(out, right, top, piece.x + piece.width - right, bottom - top); // right
    }

    private static void add(final List<Rectangle> out, final float x, final float y, final float w, final float h) {
        if (w > MIN_PIECE && h > MIN_PIECE) {
            out.add(new Rectangle(x, y, w, h));
        }
    }

    // never takes a touch; the game underneath stays usable
    @Override
    public boolean press(final float x, final float y) {
        return false;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        return false;
    }
}
