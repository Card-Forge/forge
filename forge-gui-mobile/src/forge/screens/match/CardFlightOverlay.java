package forge.screens.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;

import forge.Graphics;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.views.VPlayerPanel;
import forge.toolbox.FCardPanel;

/**
 * Draws short "card flies into the battlefield" animations on top of the match screen.
 * Lands launch from their spot in the hand; spells launch from where they were shown on the stack.
 * All methods are expected to be called from the render thread, so no locking is used.
 */
public final class CardFlightOverlay {
    private static final float DURATION_NANOS = 450_000_000f;
    private static final int MAX_FLIGHTS = 4;          // extra simultaneous flights are skipped
    private static final boolean SKIP_TOKENS = true;   // tokens have no hand/stack origin and are often spawned in bulk
    private static final float ARC_HEIGHT = 40f;

    private static final List<Flight> flights = new ArrayList<>(MAX_FLIGHTS);

    // last on-screen rect of each spell's thumbnail in the stack view, keyed by card id.
    // Each entry is allocated once and then updated in place by VStack every frame.
    private static final Map<Integer, Rectangle> stackRects = new HashMap<>();

    // ids of spells whose flight has launched from the stack view; VStack hides those items
    private static final Set<Integer> launchedFromStack = new HashSet<>();

    private CardFlightOverlay() { }

    public static final class Flight {
        private final CardView card;
        private final Rectangle from;  // may be null: fallback is resolved on first draw
        private final boolean fromExact; // true: 'from' is already the card's exact rect (stack thumbnail); false: it's a panel rect with padding
        private final Rectangle to;
        private final long startNanos = TimeUtils.nanoTime();
        private final float tappedAngle;

        // resolved once, on the first draw, so the per-frame path allocates nothing
        private boolean resolved;
        private float fromCx, fromCy, toCx, toCy, fromH, toH;

        private Flight(CardView card, Rectangle from, boolean fromExact, Rectangle to, float tappedAngle) {
            this.card = card; this.from = from; this.fromExact = fromExact; this.to = to; this.tappedAngle = tappedAngle;
        }

        private boolean done; // set by draw() AFTER the final frame was drawn

        /** True only once the overlay has drawn the card's final frame, so the real card never blinks out. */
        public boolean isDone() {
            return done;
        }
    }

    /**
     * Called by VStack every time it draws a spell's thumbnail, so we know where it was on screen.
     * Rect is in screen coordinates (same space as FDisplayObject.screenPos).
     */
    public static void noteStackRect(int cardId, float x, float y, float w, float h) {
        Rectangle r = stackRects.get(cardId);
        if (r == null) {
            r = new Rectangle();
            stackRects.put(cardId, r);
        }
        r.set(x, y, w, h);
    }

    /** VStack asks this so it can hide an item whose card is already flying to the battlefield. */
    public static boolean isLeavingStack(int cardId) {
        return !launchedFromStack.isEmpty() && launchedFromStack.contains(cardId);
    }

    /**
     * Origin priority: stack thumbnail (spells only, if the stack view showed it), then the card's
     * recorded spot in the open hand, then the hand tab / avatar / screen edge fallback.
     *
     * @param handFrom  the card's rect in the hand when it was played, or null
     * @param viaStack  true for anything that resolves from the stack (non-land permanents)
     * @return the flight, or null if it was skipped (cap reached / token). The caller should then just show the card.
     */
    public static Flight start(CardView card, Rectangle handFrom, Rectangle to, float tappedAngle, boolean viaStack) {
        Rectangle stackFrom = stackRects.remove(card.getId()); // always consume so it can't go stale
        Rectangle from;
        boolean exact;
        if (viaStack && stackFrom != null) {
            from = stackFrom;
            exact = true;
        } else {
            from = handFrom;
            exact = false;
        }
        if (flights.size() >= MAX_FLIGHTS) { return null; }
        if (SKIP_TOKENS && card.isToken()) { return null; }

        Flight f = new Flight(card, from, exact, to, tappedAngle);
        flights.add(f);
        if (exact) { launchedFromStack.add(card.getId()); }
        Gdx.graphics.requestRendering();
        return f;
    }

    /** Call on new game / leaving the match screen. */
    public static void clear() {
        flights.clear();
        stackRects.clear();
        launchedFromStack.clear();
    }

    // Screen rect of the player's Hand tab icon, or null if it isn't laid out/visible
    private static Rectangle handTabRect(VPlayerPanel pp) {
        for (VPlayerPanel.InfoTab tab : pp.getTabs()) {
            if (tab instanceof VPlayerPanel.InfoTabZone && ((VPlayerPanel.InfoTabZone) tab).zoneType == ZoneType.Hand) {
                Rectangle r = tab.screenPos;
                return (r != null && r.width > 0 && r.height > 0) ? r : null;
            }
        }
        return null;
    }

    /** Resolves start/end geometry once per flight. Allocates a few small objects, but only once. */
    private static void resolve(Flight f, PlayerView bottomPlayer, float screenH) {
        float pad = FCardPanel.PADDING;

        // destination: card rect inside the panel's padding
        float toH = f.to.height - 2 * pad;
        float toW = toH / FCardPanel.ASPECT_RATIO;
        float toX = f.to.x + pad;
        float toY = f.to.y + pad;

        float toCx = toX + toW / 2;
        float toCy = toY + toH / 2;
        if (f.tappedAngle != 0f) { // a tapped card sits bottom-aligned in the square panel
            toCx = toX + toH / 2;
            toCy = toY + toH - toW / 2;
        }

        float fromCx, fromCy, fromH;
        if (f.from != null && f.fromExact) {
            // stack thumbnail: already card-shaped, no padding to strip
            fromH = f.from.height;
            fromCx = f.from.x + f.from.width / 2;
            fromCy = f.from.y + f.from.height / 2;
        } else if (f.from != null) {
            // hand panel rect (lands): strip the panel padding
            fromH = f.from.height - 2 * pad;
            float fromW = fromH / FCardPanel.ASPECT_RATIO;
            fromCx = f.from.x + pad + fromW / 2;
            fromCy = f.from.y + pad + fromH / 2;
        } else {
            // nothing recorded (stack hidden, effects, AI): launch from the controller's Hand tab, then avatar, then screen edge
            Rectangle src = null;
            VPlayerPanel pp = MatchScreen.getPlayerPanel(f.card.getController());
            if (pp != null) {
                src = handTabRect(pp);
                if (src == null && pp.getAvatar() != null && pp.getAvatar().screenPos.width > 0) {
                    src = pp.getAvatar().screenPos;
                }
            }
            if (src != null) {
                fromH = toH * 0.6f; // start smaller, grows into place
                fromCx = src.x + src.width / 2;
                fromCy = src.y + src.height / 2;
            } else {
                boolean mine = f.card.getController() == bottomPlayer;
                fromH = toH;
                fromCx = toX + toW / 2;
                fromCy = (mine ? screenH : -toH) + toH / 2;
            }
        }

        f.fromCx = fromCx; f.fromCy = fromCy; f.fromH = fromH;
        f.toCx = toCx; f.toCy = toCy; f.toH = toH;
        f.resolved = true;
    }

    public static void draw(Graphics g, PlayerView bottomPlayer, float screenH) {
        if (flights.isEmpty()) { return; }

        final long now = TimeUtils.nanoTime();
        for (int i = 0; i < flights.size(); ) {      // index loop: no Iterator allocation, oldest drawn first
            Flight f = flights.get(i);
            float t = (now - f.startNanos) / DURATION_NANOS;
            final boolean last = t >= 1f;
            if (last) { t = 1f; } // still draw the final pose once; the real card takes over next frame

            if (!f.resolved) { resolve(f, bottomPlayer, screenH); }

            float e = Interpolation.fastSlow.apply(t);
            float h = f.fromH + (f.toH - f.fromH) * e;
            float w = h / FCardPanel.ASPECT_RATIO;   // always a real card shape

            float cx = f.fromCx + (f.toCx - f.fromCx) * e;
            float cy = f.fromCy + (f.toCy - f.fromCy) * e - ARC_HEIGHT * MathUtils.sin(MathUtils.PI * t); // little arc
            float angle = 360f * e + f.tappedAngle * e; // spin, then settle into the tapped pose

            g.startRotateTransform(cx, cy, angle);
            CardRenderer.drawCard(g, f.card, cx - w / 2, cy - h / 2, w, h,
                    CardStackPosition.Top, false, false, false, true);
            g.endTransform();

            if (last) {
                f.done = true;
                if (f.fromExact) { launchedFromStack.remove(f.card.getId()); }
                flights.remove(i);
                Gdx.graphics.requestRendering(); // one more frame so the real card gets drawn
            } else {
                i++;
            }
        }

        if (!flights.isEmpty()) {
            Gdx.graphics.requestRendering(); // keep frames coming only while flights are active
        }
    }
}