package forge.screens.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.views.VPlayerPanel;
import forge.toolbox.FCardPanel;
import forge.util.collect.FCollectionView;

/**
 * Draws short card animations on top of the match screen:
 *  - ENTER: a card flies into the battlefield (lands from the hand, spells from the stack view).
 *  - LEAVE: a card that left the battlefield. To the graveyard (or gone) it glitches/shakes with a red flash,
 *           then shrinks and fades toward the owner's zone tab. To exile/hand/library it just flies there.
 * Nothing here spins; a tapped card keeps its tapped pose and straightens as it leaves.
 * All methods are expected to be called from the render thread, so no locking is used.
 */
public final class CardFlightOverlay {
    private static final float ENTER_NANOS = 450_000_000f;
    private static final float LEAVE_GLITCH_NANOS = 700_000_000f; // graveyard: shake, then fly
    private static final float LEAVE_MOVE_NANOS = 420_000_000f;   // other zones: just fly
    private static final float GLITCH_FRACTION = 0.3f;            // share of the glitch flight spent shaking in place
    private static final boolean LEAVE_SHAKE = false; // shake in place before a destroyed card flies off
    private static final boolean LEAVE_TINT = false;  // flash a color over a destroyed card (see TINT below)
    private static final int MAX_FLIGHTS = 4;          // simultaneous ENTER flights
    private static final int MAX_LEAVE_FLIGHTS = 12;   // simultaneous LEAVE flights (board wipes); extra cards just disappear
    private static final long LEAVE_STAGGER_NANOS = 35_000_000L;     // delay between cards of one wipe
    private static final long LEAVE_BATCH_GAP_NANOS = 150_000_000L;  // departures closer together than this belong to one wipe          // extra simultaneous flights are skipped
    private static final boolean TOKENS_POPUP = true; // tokens always use the popup style (unless animations are off)
    private static final float ARC_HEIGHT = 40f;

    // zones a card can be sent to from the battlefield, in the order they are checked
    private static final ZoneType[] LEAVE_ZONES = { ZoneType.Graveyard, ZoneType.Exile, ZoneType.Hand, ZoneType.Library };
    // tint used when LEAVE_TINT is on; alpha is set per frame. Try (0f, 0f, 0f) for a dark "dim" instead of red.
    private static final Color RED = new Color(1f, 0.1f, 0.1f, 1f);

    private static final List<Flight> flights = new ArrayList<>(MAX_FLIGHTS);

    // last on-screen rect of each spell's thumbnail in the stack view, keyed by card id.
    // Each entry is allocated once and then updated in place by VStack every frame.
    private static final Map<Integer, Rectangle> stackRects = new HashMap<>();

    // ids of spells whose flight has launched from the stack view; VStack hides those items
    private static final Set<Integer> launchedFromStack = new HashSet<>();

    private static long lastLeaveStartNanos;
    private static int leaveBatchIndex;

    private CardFlightOverlay() { }
    public enum Style { OFF, ROTATE, SLIDE, POPUP }

    public static Style style() {
        String v = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_PLAY_ANIMATION_OPTIONS);
        if (v == null) { return Style.ROTATE; }
        switch (v.trim().toLowerCase()) {
            case "off": case "false": return Style.OFF;
            case "slide": return Style.SLIDE;
            case "popup": return Style.POPUP;
            default: return Style.ROTATE;
        }
    }
    public static final class Flight {
        private final CardView card;
        private final Rectangle from;    // ENTER: hand/stack origin or null. LEAVE: the card's last panel rect on the field
        private final boolean fromExact; // ENTER only: true if 'from' is already the card's exact rect (stack thumbnail)
        private final Rectangle to;      // ENTER only: destination panel rect
        private final boolean leave;
        private final long startNanos = TimeUtils.nanoTime();
        private final float tappedAngle;

        private float durationNanos;
        private long delayNanos;   // LEAVE: waits this long (holding the card in place) before moving, to stagger a wipe
        private boolean done;      // set by draw() AFTER the final frame was drawn
        private boolean resolved;  // geometry computed (once, on the first draw)
        private boolean jitter;    // LEAVE: shake in place before flying
        private boolean tint;      // LEAVE: color flash over the card
        private boolean cancelled; // LEAVE: turned out not to be a departure (e.g. control change)
        private float fromCx, fromCy, toCx, toCy, fromH, toH;
        private Style style = Style.ROTATE;

        private Flight(CardView card, Rectangle from, boolean fromExact, Rectangle to, float tappedAngle, boolean leave) {
            this.card = card; this.from = from; this.fromExact = fromExact; this.to = to;
            this.tappedAngle = tappedAngle; this.leave = leave;
            this.durationNanos = leave ? LEAVE_GLITCH_NANOS : ENTER_NANOS;
        }

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
     * Starts an ENTER flight.
     * Origin priority: stack thumbnail (spells only, if the stack view showed it), then the card's
     * recorded spot in the open hand, then the hand tab / avatar / screen edge fallback.
     *
     * @param handFrom  the card's rect in the hand when it was played, or null
     * @param viaStack  true for anything that resolves from the stack (non-land permanents)
     * @return the flight, or null if it was skipped (cap reached / token). The caller should then just show the card.
     */
    public static Flight start(CardView card, Rectangle handFrom, Rectangle to, float tappedAngle, boolean viaStack) {
        Rectangle stackFrom = stackRects.remove(card.getId()); // always consume so it can't go stale
        Style s = style();
        if (s == Style.OFF) { return null; }
        if (countFlights(false) >= MAX_FLIGHTS) { return null; }
        if (TOKENS_POPUP && card.isToken()) { s = Style.POPUP; } // tokens have no hand/stack origin

        Rectangle from;
        boolean exact;
        if (s == Style.POPUP) { // popup doesn't travel, so no origin is needed
            from = null;
            exact = false;
        } else if (viaStack && stackFrom != null) {
            from = stackFrom;
            exact = true;
        } else {
            from = handFrom;
            exact = false;
        }

        Flight f = new Flight(card, from, exact, to, tappedAngle, false);
        f.style = s;
        flights.add(f);
        if (exact) { launchedFromStack.add(card.getId()); }
        Gdx.graphics.requestRendering();
        return f;
    }

    /**
     * Starts a LEAVE animation for a card that just left the battlefield row.
     * The destination zone is looked up on the first draw, once the zone views have caught up.
     *
     * @param fromPanel   the card panel's last screen rect on the field (with its padding)
     * @param tappedAngle the card's tapped angle on the field, or 0
     */
    public static void startLeave(CardView card, Rectangle fromPanel, float tappedAngle) {
        if (countFlights(true) >= MAX_LEAVE_FLIGHTS) { return; }

        // cards that leave within a short time of each other are one wipe: start them one after another
        long now = TimeUtils.nanoTime();
        if (now - lastLeaveStartNanos > LEAVE_BATCH_GAP_NANOS) { leaveBatchIndex = 0; }
        lastLeaveStartNanos = now;

        Flight f = new Flight(card, fromPanel, false, null, tappedAngle, true);
        f.delayNanos = leaveBatchIndex * LEAVE_STAGGER_NANOS;
        leaveBatchIndex++;
        flights.add(f);
        Gdx.graphics.requestRendering();
    }

    private static int countFlights(boolean leave) {
        int n = 0;
        for (int i = 0; i < flights.size(); i++) {
            if (flights.get(i).leave == leave) { n++; }
        }
        return n;
    }

    /** Call on new game / leaving the match screen. */
    public static void clear() {
        // a panel hides its real card until its flight is done, so release them before dropping the list
        for (int i = 0; i < flights.size(); i++) {
            flights.get(i).done = true;
        }
        flights.clear();
        stackRects.clear();
        launchedFromStack.clear();
    }

    // Screen rect of one of a player's zone tab icons, or null if it isn't laid out/visible
    private static Rectangle zoneTabRect(VPlayerPanel pp, ZoneType zone) {
        for (VPlayerPanel.InfoTab tab : pp.getTabs()) {
            if (tab instanceof VPlayerPanel.InfoTabZone && ((VPlayerPanel.InfoTabZone) tab).zoneType == zone) {
                Rectangle r = tab.screenPos;
                return (r != null && r.width > 0 && r.height > 0) ? r : null;
            }
        }
        return null;
    }

    // Which of the owner's zones holds this card now? null if none (e.g. a token that ceased to exist).
    private static ZoneType zoneOf(CardView card) {
        PlayerView owner = card.getOwner();
        if (owner == null) { return null; }
        for (ZoneType z : LEAVE_ZONES) {
            FCollectionView<CardView> cards = owner.getCards(z);
            if (cards == null) { continue; }
            for (CardView c : cards) {
                if (c.getId() == card.getId()) { return z; }
            }
        }
        return null;
    }

    // true if the card is still on somebody's battlefield (a control change is not a departure)
    private static boolean isOnBattlefield(CardView card) {
        FCollectionView<PlayerView> players = MatchController.instance.getGameView().getPlayers();
        if (players == null) { return false; }
        for (PlayerView p : players) {
            FCollectionView<CardView> bf = p.getCards(ZoneType.Battlefield);
            if (bf == null) { continue; }
            for (CardView c : bf) {
                if (c.getId() == card.getId()) { return true; }
            }
        }
        return false;
    }

    /** Resolves start/end geometry of an ENTER flight once. Allocates a few small objects, but only once. */
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
            // hand panel rect: strip the panel padding
            fromH = f.from.height - 2 * pad;
            float fromW = fromH / FCardPanel.ASPECT_RATIO;
            fromCx = f.from.x + pad + fromW / 2;
            fromCy = f.from.y + pad + fromH / 2;
        } else {
            // nothing recorded (stack hidden, effects, AI): launch from the controller's Hand tab, then avatar, then screen edge
            Rectangle src = null;
            VPlayerPanel pp = MatchScreen.getPlayerPanel(f.card.getController());
            if (pp != null) {
                src = zoneTabRect(pp, ZoneType.Hand);
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

    /** Resolves a LEAVE flight once: where the card was, which zone it went to, and where that zone's tab is. */
    private static void resolveLeave(Flight f) {
        float pad = FCardPanel.PADDING;
        float h = f.from.height - 2 * pad;
        float w = h / FCardPanel.ASPECT_RATIO;
        float x = f.from.x + pad;
        float y = f.from.y + pad;
        float cx = x + w / 2;
        float cy = y + h / 2;
        if (f.tappedAngle != 0f) { // tapped cards sit bottom-aligned in the square panel
            cx = x + h / 2;
            cy = y + h - w / 2;
        }
        f.fromCx = cx; f.fromCy = cy; f.fromH = h;

        ZoneType zone = zoneOf(f.card);
        if (zone == null && isOnBattlefield(f.card)) { // e.g. control change: it didn't leave, it moved rows
            f.cancelled = true;
            f.resolved = true;
            return;
        }

        Rectangle dest = null;
        if (zone != null) {
            PlayerView owner = f.card.getOwner();
            VPlayerPanel pp = owner == null ? null : MatchScreen.getPlayerPanel(owner);
            if (pp != null) {
                dest = zoneTabRect(pp, zone);
                if (dest == null && pp.getAvatar() != null && pp.getAvatar().screenPos.width > 0) {
                    dest = pp.getAvatar().screenPos;
                }
            }
        }

        // graveyard, or gone entirely (tokens): glitch. Exile / hand / library: plain flight.
        boolean destroyed = zone == ZoneType.Graveyard || zone == null;
        f.jitter = LEAVE_SHAKE && destroyed;
        f.tint = LEAVE_TINT && destroyed;
        f.durationNanos = f.jitter ? LEAVE_GLITCH_NANOS : LEAVE_MOVE_NANOS;

        if (dest != null) {
            f.toCx = dest.x + dest.width / 2;
            f.toCy = dest.y + dest.height / 2;
            f.toH = h * 0.3f;
        } else { // nowhere to fly to: shrink and fade in place
            f.toCx = cx;
            f.toCy = cy;
            f.toH = h * 0.7f;
        }
        f.resolved = true;
    }

    public static void draw(Graphics g, PlayerView bottomPlayer, float screenH) {
        if (flights.isEmpty()) { return; }

        final long now = TimeUtils.nanoTime();
        for (int i = 0; i < flights.size(); ) {      // index loop: no Iterator allocation, oldest drawn first
            Flight f = flights.get(i);

            if (!f.resolved) {
                if (f.leave) { resolveLeave(f); } else { resolve(f, bottomPlayer, screenH); }
            }
            if (f.cancelled) {
                f.done = true;
                flights.remove(i);
                continue;
            }

            float t = (now - f.startNanos - f.delayNanos) / f.durationNanos;
            if (t < 0f) { t = 0f; } // still waiting for its turn: hold the card at its starting spot
            final boolean last = t >= 1f;
            if (last) { t = 1f; } // still draw the final pose once; the real card takes over next frame

            if (f.leave) { drawLeave(g, f, t); } else { drawEnter(g, f, t); }

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

    private static void drawEnter(Graphics g, Flight f, float t) {
        float cx, cy, h, angle;
        float alpha = 1f;

        switch (f.style) {
            case POPUP: { // appears in place, scales up with a small overshoot and fades in
                float e = Interpolation.swingOut.apply(t);
                h = f.toH * (0.3f + 0.7f * e);
                cx = f.toCx;
                cy = f.toCy;
                angle = f.tappedAngle;
                alpha = Math.min(1f, t * 3f);
                break;
            }
            case SLIDE: { // straight line, no spin, no arc; settles into the tapped pose
                float e = Interpolation.fastSlow.apply(t);
                h = f.fromH + (f.toH - f.fromH) * e;
                cx = f.fromCx + (f.toCx - f.fromCx) * e;
                cy = f.fromCy + (f.toCy - f.fromCy) * e;
                angle = f.tappedAngle * e;
                break;
            }
            default: { // ROTATE (the original behaviour)
                float e = Interpolation.fastSlow.apply(t);
                h = f.fromH + (f.toH - f.fromH) * e;
                cx = f.fromCx + (f.toCx - f.fromCx) * e;
                cy = f.fromCy + (f.toCy - f.fromCy) * e - ARC_HEIGHT * MathUtils.sin(MathUtils.PI * t);
                angle = 360f * e + f.tappedAngle * e;
                break;
            }
        }

        float w = h / FCardPanel.ASPECT_RATIO;
        if (alpha < 1f) { g.setAlphaComposite(alpha); }
        g.startRotateTransform(cx, cy, angle);
        CardRenderer.drawCard(g, f.card, cx - w / 2, cy - h / 2, w, h,
                CardStackPosition.Top, false, false, false, true);
        g.endTransform();
        if (alpha < 1f) { g.resetAlphaComposite(); }
    }

    private static void drawLeave(Graphics g, Flight f, float t) {
        final float split = f.jitter ? GLITCH_FRACTION : 0f;
        float cx, cy, h, angle;
        float alpha = 1f;
        float redAlpha = 0f;

        if (t < split) {
            // glitch phase: shake in place with a flickering red tint (new random offset every frame)
            float amp = f.fromH * 0.035f;
            cx = f.fromCx + MathUtils.random(-amp, amp);
            cy = f.fromCy + MathUtils.random(-amp, amp);
            h = f.fromH * (1f + MathUtils.random(-0.02f, 0.04f));
            angle = f.tappedAngle;
            if (f.tint) { redAlpha = MathUtils.randomBoolean(0.7f) ? 0.55f : 0.15f; }
        } else {
            // flight phase: accelerate toward the zone tab, shrinking and fading, straightening if tapped
            float m = split >= 1f ? 1f : (t - split) / (1f - split);
            float e = Interpolation.pow2In.apply(m);
            cx = f.fromCx + (f.toCx - f.fromCx) * e;
            cy = f.fromCy + (f.toCy - f.fromCy) * e;
            h = f.fromH + (f.toH - f.fromH) * e;
            angle = f.tappedAngle * (1f - e);
            alpha = 1f - e;
            if (f.tint) { redAlpha = 0.45f * (1f - m); }
        }
        if (alpha <= 0.01f) { return; }

        float w = h / FCardPanel.ASPECT_RATIO;
        if (alpha < 1f) { g.setAlphaComposite(alpha); }
        if (angle != 0f) { g.startRotateTransform(cx, cy, angle); }

        CardRenderer.drawCard(g, f.card, cx - w / 2, cy - h / 2, w, h,
                CardStackPosition.Top, false, false, false, true);
        if (redAlpha > 0f) {
            RED.a = redAlpha;
            g.fillRect(RED, cx - w / 2, cy - h / 2, w, h);
        }

        if (angle != 0f) { g.endTransform(); }
        if (alpha < 1f) { g.resetAlphaComposite(); }
    }
}