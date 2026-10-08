package forge.animation;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FOverlay;

public class FlipOntoBattlefieldAnimation extends ForgeAnimation {
    private static final float CARD_RATIO = FCardPanel.ASPECT_RATIO;
    private static final float TOSS_TIME = 1.1f;
    private static final float SETTLE_TIME = 0.3f;
    private static final float RELEASE_TIME = TOSS_TIME + SETTLE_TIME + 0.7f; // game thread resumes here
    private static final float MAX_LINGER = 60f;        // safety cap only
    private static final float RESOLVED_GRACE = 2.5f;   // time for the script to destroy things

    private static FlipOntoBattlefieldAnimation active;
    private volatile boolean resolved = false;
    private float resolvedAt = -1f, goneAt = -1f;

    private final CardView flipped;
    private final int timesFlipped;
    private final boolean flew;
    private final Runnable onFinished;
    private boolean released = false;

    private final float sw, sh, cardW, cardH;
    private final float startX, startY, landX, landY, landAngle, spinDir;
    private final List<float[]> hitRects = new ArrayList<>(); // raw panel rects x,y,w,h
    private final List<Boolean> hitImmune = new ArrayList<>();
    private float elapsed = 0f;
    private final Overlay overlay = new Overlay();
    private volatile boolean leaveHandedOff = false;

    public FlipOntoBattlefieldAnimation(CardView flipped, CardView target, List<CardView> hit,
            List<CardView> battlefield, int timesFlipped, Runnable onFinished) {
        this.flipped = flipped;
        this.timesFlipped = timesFlipped;
        this.flew = timesFlipped > 0;
        this.onFinished = onFinished;

        sw = Forge.getScreenWidth();
        sh = Forge.getScreenHeight();

        // standard card size from the chosen card
        float[] tr = rawRect(target);
        float h = clampHeight(tr != null ? Math.max(tr[2], tr[3]) - 2 * FCardPanel.PADDING : sh * 0.2f);
        cardH = h;
        cardW = h / CARD_RATIO;
        float tcx = tr != null ? tr[0] + tr[2] / 2f : sw / 2f;
        float tcy = tr != null ? tr[1] + tr[3] / 2f : sh / 2f;

        // launch from the source card
        float[] sr = rawRect(flipped);
        startX = sr != null ? sr[0] + sr[2] / 2f : sw / 2f;
        startY = sr != null ? sr[1] + sr[3] / 2f : sh * 0.85f;

        // everything on the board except the thrown card = obstacles for a miss
        List<float[]> obstacles = new ArrayList<>();
        for (CardView cv : battlefield) {
            if (cv == null || cv.getId() == flipped.getId()) { continue; }
            float[] r = rawRect(cv);
            if (r != null) { obstacles.add(r); }
        }

        for (CardView cv : hit) {
            float[] r = rawRect(cv);
            if (r != null) {
                hitRects.add(r);
                hitImmune.add(cv.isToken());   // the script skips tokens
            }
        }

        float lx, ly;
        if (!hitRects.isEmpty()) {
            // land ON the hit card(s); between them if there are two
            float sx = 0f, sy = 0f;
            for (float[] r : hitRects) { sx += r[0] + r[2] / 2f; sy += r[1] + r[3] / 2f; }
            lx = sx / hitRects.size();
            ly = sy / hitRects.size();
            if (hitRects.size() == 1) {
                lx += (float) (Math.random() - 0.5) * cardW * 0.2f;
                ly += (float) (Math.random() - 0.5) * cardH * 0.15f;
            }
        } else {
            // miss: find open space near the target that touches no card
            float[] free = findFreeSpot(tcx, tcy, obstacles);
            lx = free[0];
            ly = free[1];
        }
        landX = clampX(lx);
        landY = clampY(ly);
        landAngle = (float) (Math.random() - 0.5) * 50f;
        spinDir = Math.random() < 0.5 ? 1f : -1f;
    }

    public static void leaveStarted(CardView cv) {
        FlipOntoBattlefieldAnimation a = active;
        if (a != null && cv != null && a.flipped.getId() == cv.getId()) { a.leaveHandedOff = true; }
    }

    public static void markResolved(CardView cv) {
        FlipOntoBattlefieldAnimation a = active;
        if (a != null && cv != null && a.flipped.getId() == cv.getId()) { a.resolved = true; }
    }

    @Override
    public void start() {
        active = this;
        hideSource();
        try {
            float pad = FCardPanel.PADDING;
            CardAreaPanel.get(flipped).setLeaveOrigin(landX - cardW / 2f - pad, landY - cardH / 2f - pad,
                    cardW + 2 * pad, cardH + 2 * pad, landAngle);
        } catch (Exception ignored) {}
        overlay.show();
        super.start();
    }

    @Override
    protected boolean advance(float dt) {
        elapsed += dt;
        if (leaveHandedOff) { return false; }
        hideSource();   // re-hide every frame: the panel can be rebuilt when the board re-lays out
        if (!released && elapsed >= RELEASE_TIME) { release(); }
        if (!released) { return true; }

        if (goneAt < 0f) {
            if (flipped.getZone() != ZoneType.Battlefield) {
                goneAt = elapsed;                       // destroyed: fade the landed copy out
            } else {
                if (resolved && resolvedAt < 0f) { resolvedAt = elapsed; }
                if (resolvedAt >= 0f && elapsed - resolvedAt > RESOLVED_GRACE) { return false; } // survived: hand back
                return elapsed < MAX_LINGER;
            }
        }
        return elapsed - goneAt < 1.5f;
    }

    @Override
    protected void onEnd(boolean endingAll) {
        overlay.hide();
        if (active == this) { active = null; }
        setSourceVisible(true);   // always: the panel is reused by graveyard/exile views
        try { CardAreaPanel.get(flipped).clearLeaveOrigin(); } catch (Exception ignored) {}
        release();
    }

    private void release() {
        if (released) { return; }
        released = true;
        if (onFinished != null) { onFinished.run(); }
    }

    private void hideSource() { setSourceVisible(false); }

    private void setSourceVisible(boolean visible) {
        try {
            CardAreaPanel p = CardAreaPanel.get(flipped);   // look it up fresh each time
            if (p != null) { p.setVisible(visible); }
        } catch (Exception ignored) { }
    }

    // ---- drawing ----
    private void drawAnimation(Graphics g) {
        if (elapsed < TOSS_TIME) {
            drawToss(g, elapsed / TOSS_TIME);
        } else {
            drawResting(g, elapsed - TOSS_TIME);
        }
    }

    private void drawToss(Graphics g, float t) {
        float u = Interpolation.pow2Out.apply(t);
        float x = startX + (landX - startX) * u;
        float y = startY + (landY - startY) * u;

        float height = (float) Math.sin(Math.PI * t) * (flew ? 1f : 0.8f);
        float scale = 1f + 0.3f * height;   // was 0.6f

        float flipAngle = (float) (2d * Math.PI * timesFlipped * Interpolation.sine.apply(t));
        float c = (float) Math.cos(flipAngle);
        float tumble = flew ? Math.max(0.06f, Math.abs(c)) : 1f;
        boolean showBack = flew && c < 0f;                       // far side of the flip = card back

        float rotation = landAngle + spinDir * (flew ? 240f : 90f) * (1f - u);
        drawCardAt(g, x, y, cardW * scale, cardH * scale * tumble, rotation, height * cardH * 0.3f, showBack);
    }

    private void drawResting(Graphics g, float since) {
        // hit cards: red flash outline only - no second copy of the card
        float flash = 0.55f * Math.max(0f, 1f - since / 1.0f) * (0.5f + 0.5f * (float) Math.cos(since * 14f));
        if (flash > 0.01f) {
            for (int i = 0; i < hitRects.size(); i++) {
                float[] r = hitRects.get(i);
                Color c = hitImmune.get(i) ? Color.CYAN : Color.RED;
                g.fillRect(FSkinColor.getStandardColor(c).alphaColor(flash), r[0], r[1], r[2], r[3]);
                g.drawRect(4f, c, r[0], r[1], r[2], r[3]);
            }
        }
        float pop = since < SETTLE_TIME ? 0.12f * (float) Math.sin(Math.PI * since / SETTLE_TIME) : 0f;
        drawCardAt(g, landX, landY, cardW * (1f + pop), cardH * (1f + pop), landAngle, cardH * 0.04f, false);
    }

    private void drawCardAt(Graphics g, float cx, float cy, float w, float h, float rotation, float shadow, boolean back) {
        g.startRotateTransform(cx, cy, rotation);
        if (shadow > 0.5f) {
            g.fillRect(FSkinColor.getStandardColor(Color.BLACK).alphaColor(0.35f),
                    cx - w / 2f + shadow, cy - h / 2f + shadow, w, h);
        }
        if (back) {
            drawCardBack(g, cx - w / 2f, cy - h / 2f, w, h);
        } else {
            CardRenderer.drawCardWithOverlays(g, flipped, cx - w / 2f, cy - h / 2f, w, h, CardStackPosition.Top, false, false, false);
        }
        g.endTransform();
    }

    private void drawCardBack(Graphics g, float x, float y, float w, float h) {
        FImage sleeves = MatchController.getPlayerSleeve(flipped.getOwner());
        if (sleeves != null) {
            float pad = w * 0.04f;
            g.drawImage(sleeves, x + pad, y + pad, w - 2 * pad, h - 2 * pad);
        }
    }

    // ---- geometry ----
    private float[] findFreeSpot(float tcx, float tcy, List<float[]> obstacles) {
        float bestX = tcx, bestY = tcy, bestOverlap = Float.MAX_VALUE;
        double start = Math.random() * Math.PI * 2d;
        for (float r = 1.0f; r <= 3.0f; r += 0.4f) {
            for (int k = 0; k < 12; k++) {
                double a = start + k * Math.PI / 6d;
                float x = clampX(tcx + (float) Math.cos(a) * cardH * r);
                float y = clampY(tcy + (float) Math.sin(a) * cardH * r);
                float ov = overlapArea(x, y, obstacles);
                if (ov <= 0f) { return new float[] { x, y }; }
                if (ov < bestOverlap) { bestOverlap = ov; bestX = x; bestY = y; }
            }
        }
        float bestD = Float.MAX_VALUE, gx = -1f, gy = -1f, step = cardW * 0.5f;
        for (float y = cardH / 2f; y <= sh - cardH / 2f; y += step) {
            for (float x = cardW / 2f; x <= sw - cardW / 2f; x += step) {
                if (overlapArea(x, y, obstacles) > 0f) { continue; }
                float d = (x - tcx) * (x - tcx) + (y - tcy) * (y - tcy);
                if (d < bestD) { bestD = d; gx = x; gy = y; }
            }
        }
        if (gx >= 0f) { return new float[] { gx, gy }; }

        return new float[] { bestX, bestY };
    }

    // landed card is slightly crooked, so test with a 10% bigger box
    private float overlapArea(float cx, float cy, List<float[]> obstacles) {
        float w = cardW * 1.1f, h = cardH * 1.1f;
        float l = cx - w / 2f, r = cx + w / 2f, t = cy - h / 2f, b = cy + h / 2f;
        float total = 0f;
        for (float[] o : obstacles) {
            float ox = Math.min(r, o[0] + o[2]) - Math.max(l, o[0]);
            float oy = Math.min(b, o[1] + o[3]) - Math.max(t, o[1]);
            if (ox > 0f && oy > 0f) { total += ox * oy; }
        }
        return total;
    }

    private float clampX(float x) { return Math.min(Math.max(x, cardW / 2f), sw - cardW / 2f); }
    private float clampY(float y) { return Math.min(Math.max(y, cardH / 2f), sh - cardH / 2f); }
    private float clampHeight(float h) { return Math.min(Math.max(h, sh * 0.14f), sh * 0.26f); }

    // x, y, w, h of the card's panel in screen coordinates, or null
    private static float[] rawRect(CardView cv) {
        if (cv == null) { return null; }
        try {
            CardAreaPanel p = CardAreaPanel.get(cv);
            if (p != null) {
                return new float[] { p.localToScreenX(0), p.localToScreenY(0), p.getWidth(), p.getHeight() };
            }
        } catch (Exception ignored) { }
        return null;
    }

    private class Overlay extends FOverlay {
        @Override
        protected void doLayout(float width, float height) { }

        @Override
        protected void drawBackground(Graphics g) { }

        @Override
        protected void drawOverlay(Graphics g) {
            drawAnimation(g);
        }
    }
}