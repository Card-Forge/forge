package forge.screens.match;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;

import forge.Graphics;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.views.VPlayerPanel;
import forge.toolbox.FCardPanel;

public final class CardFlightOverlay {
    private static final long DURATION_MS = 450;
    private static final List<Flight> flights = new ArrayList<>();

    private CardFlightOverlay() { }

    public static final class Flight {
        private final CardView card;
        private final Rectangle from; // may be null (AI play): fallback is computed when drawing
        private final Rectangle to;
        private final long start = System.currentTimeMillis();
        private final float tappedAngle;

        private Flight(CardView card, Rectangle from, Rectangle to, float tappedAngle) {
            this.card = card; this.from = from; this.to = to; this.tappedAngle = tappedAngle;
        }

        public boolean isDone() {
            return System.currentTimeMillis() - start >= DURATION_MS;
        }
    }
    public static synchronized Flight start(CardView card, Rectangle from, Rectangle to, float tappedAngle) {
        Flight f = new Flight(card, from, to, tappedAngle);
        flights.add(f);
        Gdx.graphics.requestRendering();
        return f;
    }
    // Screen rect of the player's Hand tab icon, or null if it isn't laid out/visible
    private static Rectangle handTabRect(VPlayerPanel pp) {
        for (VPlayerPanel.InfoTab tab : pp.getTabs()) {
            if (tab instanceof VPlayerPanel.InfoTabZone z && z.zoneType == ZoneType.Hand) {
                Rectangle r = tab.screenPos;
                return (r != null && r.width > 0 && r.height > 0) ? r : null;
            }
        }
        return null;
    }
    // Converts a panel's bounds into the rect the card is actually drawn in (card aspect ratio)
    private static Rectangle cardRect(Rectangle panel) {
        float pad = FCardPanel.PADDING;
        float h = panel.height - 2 * pad;
        float w = h / FCardPanel.ASPECT_RATIO;
        // the panel draws the card at the top-left inside its padding
        return new Rectangle(panel.x + pad, panel.y + pad, w, h);
    }
    public static synchronized void draw(Graphics g, PlayerView bottomPlayer, float screenH) {
        if (flights.isEmpty()) { return; }
        Iterator<Flight> it = flights.iterator();
        while (it.hasNext()) {
            Flight f = it.next();
            float t = Math.min((System.currentTimeMillis() - f.start) / (float) DURATION_MS, 1f);
            if (t >= 1f) { it.remove(); continue; }

            Rectangle to = cardRect(f.to);
            Rectangle from;
            if (f.from != null) {
                from = cardRect(f.from);
            } else { // no tap recorded (AI play, effects): launch from the controller's Hand tab
                from = null;
                VPlayerPanel pp = MatchScreen.getPlayerPanel(f.card.getController());
                Rectangle src = pp == null ? null : handTabRect(pp);
                if (src == null && pp != null && pp.getAvatar() != null && pp.getAvatar().screenPos.width > 0) {
                    src = pp.getAvatar().screenPos; // fallback: avatar
                }
                if (src != null) {
                    float sh = to.height * 0.6f; // start smaller, grows into place
                    float sw = sh / FCardPanel.ASPECT_RATIO;
                    from = new Rectangle(src.x + src.width / 2 - sw / 2, src.y + src.height / 2 - sh / 2, sw, sh);
                }
                if (from == null) { // last resort
                    boolean mine = f.card.getController() == bottomPlayer;
                    from = new Rectangle(to.x, mine ? screenH : -to.height, to.width, to.height);
                }
            }

            float e = Interpolation.fastSlow.apply(t);
            float h = from.height + (to.height - from.height) * e;
            float w = h / FCardPanel.ASPECT_RATIO;           // always a real card shape

            // where the card's center ends up: a tapped card sits bottom-aligned in the square panel
            float toCx = to.x + to.width / 2;
            float toCy = to.y + to.height / 2;
            if (f.tappedAngle != 0f) {
                toCx = to.x + to.height / 2;
                toCy = to.y + to.height - to.width / 2;
            }
            float fromCx = from.x + from.width / 2;
            float fromCy = from.y + from.height / 2;

            float cx = fromCx + (toCx - fromCx) * e;
            float cy = fromCy + (toCy - fromCy) * e - 40 * (float) Math.sin(Math.PI * t); // little arc
            float angle = 360f * e + f.tappedAngle * e; // spin, then settle into the tapped pose

            g.startRotateTransform(cx, cy, angle);
            CardRenderer.drawCard(g, f.card, cx - w / 2, cy - h / 2, w, h,
                    CardStackPosition.Top, false, false, false, true);
            g.endTransform();
        }
        Gdx.graphics.requestRendering(); // keep frames coming while flights are active
    }
}