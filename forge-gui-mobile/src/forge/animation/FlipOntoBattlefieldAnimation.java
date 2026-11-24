package forge.animation;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Interpolation;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.BufferUtils;
import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.screens.match.views.VCardDisplayArea;
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
    private static final Color SHADOW = new Color(0f, 0f, 0f, 0.35f);
    private final Color flashColor = new Color();

    private Pixmap bakedPixmap;
    private TextureRegion face;
    private boolean bakeTried;
    private final Matrix4 savedProj = new Matrix4(), savedTrans = new Matrix4(), bakeProj = new Matrix4();
    private final Rectangle savedBounds = new Rectangle(), savedVisible = new Rectangle();
    private final IntBuffer vp = BufferUtils.newIntBuffer(16);
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
        cardH = Math.round(h);
        cardW = Math.round(h / CARD_RATIO);
        float tcx = tr != null ? tr[0] + tr[2] / 2f : sw / 2f;
        float tcy = tr != null ? tr[1] + tr[3] / 2f : sh / 2f;

        // launch from the source card
        float[] sr = rawRect(flipped);
        startX = sr != null ? sr[0] + sr[2] / 2f : sw / 2f;
        startY = sr != null ? sr[1] + sr[3] / 2f : sh * 0.85f;

        // real on-screen card rect of every visible permanent except the thrown card
        final List<float[]> obstacles = new ArrayList<>();
        final List<CardView> cardViews = new ArrayList<>();
        for (CardView cv : battlefield) {
            if (cv == null || cv.getId() == flipped.getId()) { continue; }
            float[] r = rawRect(cv);
            if (r == null) { continue; }
            obstacles.add(visibleCard(r, cv.isTapped()));
            cardViews.add(cv);
        }

        final float ang = (float) (Math.random() - 0.5) * 50f;
        final int wanted = flew ? hit.size() : 0;   // how many cards the game says the orb hits
        float lx, ly;
        if (wanted > 0 && tr != null && !obstacles.isEmpty()) {
            // The game rolled how many cards get hit; where the orb really lands decides which ones.
            // Try landing spots around the target and keep one that touches that many cards.
            final double rad = Math.toRadians(ang);
            final float cs = (float) Math.abs(Math.cos(rad)), sn = (float) Math.abs(Math.sin(rad));
            final float bw = (cardW * cs + cardH * sn) * 0.85f, bh = (cardW * sn + cardH * cs) * 0.85f;

            int bestDiff = Integer.MAX_VALUE;
            final List<float[]> pool = new ArrayList<>();
            for (int k = 0; k < 48; k++) {
                final double a = Math.random() * Math.PI * 2d;
                final float rr = k == 0 ? 0f : (float) Math.sqrt(Math.random()) * cardH;
                final float x = Math.round(clampX(tcx + (float) Math.cos(a) * rr));
                final float y = Math.round(clampY(tcy + (float) Math.sin(a) * rr));
                final int diff = Math.abs(touching(x, y, bw, bh, obstacles).size() - wanted);
                if (diff < bestDiff) { bestDiff = diff; pool.clear(); }
                if (diff == bestDiff) { pool.add(new float[] { x, y }); }
            }
            final float[] pick = pool.get((int) (Math.random() * pool.size()));
            lx = pick[0];
            ly = pick[1];

            // tell the game which cards the orb really touches
            final List<Integer> touched = touching(lx, ly, bw, bh, obstacles);
            try {
                hit.clear();
                for (int i : touched) { hit.add(cardViews.get(i)); }
            } catch (UnsupportedOperationException ignored) { }
            for (int i : touched) {
                hitRects.add(obstacles.get(i));
                hitImmune.add(cardViews.get(i).isToken());   // the script skips tokens
            }
        } else {
            // no usable layout (target not on screen) or a miss: keep what the game decided
            for (CardView cv : hit) {
                float[] r = rawRect(cv);
                if (r != null) {
                    hitRects.add(r);
                    hitImmune.add(cv.isToken());   // the script skips tokens
                }
            }
            final boolean hitButHidden = hitRects.isEmpty() && !hit.isEmpty();
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
            } else if (hitButHidden) {
                // it hit something we can't see: settle on the chosen target with a small scatter,
                // and don't search for free space, since a "clean miss" would contradict the game log
                lx = tcx + (float) (Math.random() - 0.5) * cardW * 0.3f;
                ly = tcy + (float) (Math.random() - 0.5) * cardH * 0.2f;
            } else {
                // real miss: find open space near the target that touches no card
                float[] free = findFreeSpot(tcx, tcy, obstacles);
                lx = free[0];
                ly = free[1];
            }
        }
        landX = Math.round(clampX(lx));
        landY = Math.round(clampY(ly));
        landAngle = ang;
        spinDir = Math.random() < 0.5 ? 1f : -1f;
    }

    // the card inside its panel: padded, and a tapped card sits sideways at the bottom of the square panel
    private static float[] visibleCard(float[] p, boolean tapped) {
        final float pad = FCardPanel.PADDING;
        final float h = p[3] - 2 * pad;
        final float w = h / CARD_RATIO;
        return tapped ? new float[] { p[0] + pad, p[1] + pad + h - w, h, w }
                : new float[] { p[0] + pad, p[1] + pad, w, h };
    }

    // indices of the cards the orb box covers by at least 5% of their area
    private static List<Integer> touching(float cx, float cy, float bw, float bh, List<float[]> rects) {
        final List<Integer> out = new ArrayList<>();
        final float l = cx - bw / 2f, r = cx + bw / 2f, t = cy - bh / 2f, b = cy + bh / 2f;
        for (int i = 0; i < rects.size(); i++) {
            final float[] o = rects.get(i);
            final float ox = Math.min(r, o[0] + o[2]) - Math.max(l, o[0]);
            final float oy = Math.min(b, o[1] + o[3]) - Math.max(t, o[1]);
            if (ox > 0f && oy > 0f && ox * oy >= 0.05f * o[2] * o[3]) { out.add(i); }
        }
        return out;
    }

    /** Renders the card face once, upright, into a texture. Falls back to direct drawing if anything fails. */
    private void bakeFace(Graphics g) {
        bakeTried = true;
        final SpriteBatch batch = g.getBatch();
        final float pxScale = Gdx.graphics.getBackBufferWidth() / sw;   // logical units -> pixels
        final int texW = Math.max(1, Math.round(cardW * pxScale));
        final int texH = Math.max(1, Math.round(cardH * pxScale));

        final FrameBuffer fb;
        try {
            fb = new FrameBuffer(Pixmap.Format.RGBA8888, texW, texH, false);
        } catch (RuntimeException e) {
            return;
        }

        final boolean wasDrawing = batch.isDrawing();
        if (wasDrawing) { batch.end(); }

        // save everything we're about to change
        savedProj.set(batch.getProjectionMatrix());
        savedTrans.set(batch.getTransformMatrix());
        savedBounds.set(g.getBounds());
        savedVisible.set(g.getVisibleBounds());
        final float savedRegionH = g.getRegionHeight();
        final int srcC = batch.getBlendSrcFunc(), dstC = batch.getBlendDstFunc();
        final int srcA = batch.getBlendSrcFuncAlpha(), dstA = batch.getBlendDstFuncAlpha();
        final boolean scissor = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
        vp.clear();
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, vp);
        final int vx = vp.get(0), vy = vp.get(1), vw = vp.get(2), vh = vp.get(3);

        Pixmap pm = null;
        boolean began = false;
        try {
            if (scissor) { Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST); }
            fb.begin();
            began = true;
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.getTransformMatrix().idt();
            g.setProjectionMatrix(bakeProj.setToOrtho2D(0, 0, cardW, cardH));
            g.setBounds(cardW, cardH);
            batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                    GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();
            CardRenderer.drawCardWithOverlays(g, flipped, 0, 0, cardW, cardH, CardStackPosition.Top, false, false, false);
            batch.end();

            pm = Pixmap.createFromFrameBuffer(0, 0, texW, texH);   // read back while the FBO is still bound
            fb.end(vx, vy, vw, vh);
            began = false;

            final Texture tex = new Texture(new PixmapTextureData(pm, null, false, false, true)); // managed
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            bakedPixmap = pm;
            face = new TextureRegion(tex);
            face.flip(false, true);
        } catch (RuntimeException e) {
            if (batch.isDrawing()) { batch.end(); }
            if (began) { fb.end(vx, vy, vw, vh); }
            if (pm != null) { pm.dispose(); }
            face = null;
            bakedPixmap = null;
        } finally {
            fb.dispose();                                          // no longer needed
            batch.setBlendFunctionSeparate(srcC, dstC, srcA, dstA);
            batch.getTransformMatrix().set(savedTrans);
            g.setProjectionMatrix(savedProj);
            g.setBounds(savedBounds);
            g.setVisibleBounds(savedVisible);
            g.setRegionHeight(savedRegionH);
            if (scissor) { Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST); }
            if (wasDrawing) { batch.begin(); }
        }
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
        overlay.show();
        super.start();
    }

    private static final float HANDOFF_WAIT = 0.3f;
    private static final float EXIT_FADE = 0.3f;
    private float exitAlpha = 1f;

    @Override
    protected boolean advance(float dt) {
        elapsed += dt;
        //System.out.println("FlipAnim end: handoff=" + leaveHandedOff + " gone=" + (goneAt >= 0f) + " t=" + elapsed);
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
        return elapsed - goneAt < HANDOFF_WAIT + EXIT_FADE;   // was 1.5f
    }

    @Override
    protected void onEnd(boolean endingAll) {
        overlay.hide();
        if (active == this) { active = null; }
        setSourceVisible(true);   // always: the panel is reused by graveyard/exile views
        try { CardAreaPanel.get(flipped).clearLeaveOrigin(); } catch (Exception ignored) {}
        release();
        if (face != null) {
            final Texture t = face.getTexture();
            final Pixmap pm = bakedPixmap;
            face = null;
            bakedPixmap = null;
            Gdx.app.postRunnable(() -> { t.dispose(); if (pm != null) pm.dispose(); });
        }
    }

    private void release() {
        if (released) { return; }
        released = true;
        if (onFinished != null) { onFinished.run(); }
    }

    private CardAreaPanel lastPanel;

    private void applyLeaveOrigin(CardAreaPanel p) {
        float pad = FCardPanel.PADDING;
        p.setLeaveOrigin(landX - cardW / 2f - pad, landY - cardH / 2f - pad,
                cardW + 2 * pad, cardH + 2 * pad, landAngle);
    }

    private void hideSource() {
        try {
            CardAreaPanel p = CardAreaPanel.get(flipped);
            if (p == null) { return; }
            if (p != lastPanel) {            // first call, or the board rebuilt the panel
                lastPanel = p;
                applyLeaveOrigin(p);
            }
            p.setVisible(false);
        } catch (Exception ignored) { }
    }

    private void setSourceVisible(boolean visible) {
        try {
            CardAreaPanel p = CardAreaPanel.get(flipped);   // look it up fresh each time
            if (p != null) { p.setVisible(visible); }
        } catch (Exception ignored) { }
    }

    // ---- drawing ----
    private void drawAnimation(Graphics g) {
        exitAlpha = goneAt < 0f ? 1f
                : 1f - Math.min(1f, Math.max(0f, (elapsed - goneAt - HANDOFF_WAIT) / EXIT_FADE));
        if (face == null && !bakeTried) { bakeFace(g); }   // must run before any rotate transform starts
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
                Color base = hitImmune.get(i) ? Color.CYAN : Color.RED;
                flashColor.set(base.r, base.g, base.b, flash);
                g.fillRect(flashColor, r[0], r[1], r[2], r[3]);
                g.drawRect(4f, base, r[0], r[1], r[2], r[3]);
            }
        }
        float pop = since < SETTLE_TIME ? 0.12f * (float) Math.sin(Math.PI * since / SETTLE_TIME) : 0f;
        drawCardAt(g, landX, landY, cardW * (1f + pop), cardH * (1f + pop), landAngle, cardH * 0.04f, false);
    }

    private void drawCardAt(Graphics g, float cx, float cy, float w, float h, float rotation, float shadow, boolean back) {
        final float a = exitAlpha;
        g.startRotateTransform(cx, cy, rotation);
        if (shadow > 0.5f) {
            g.setAlphaComposite(a);
            g.fillRect(SHADOW, cx - w / 2f + shadow, cy - h / 2f + shadow, w, h);
        }
        if (back) {
            g.setAlphaComposite(a);
            drawCardBack(g, cx - w / 2f, cy - h / 2f, w, h);
        } else if (face != null) {
            final SpriteBatch b = g.getBatch();
            b.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            g.setColorRGBA(a, a, a, a);
            g.drawImage(face, cx - w / 2f, cy - h / 2f, w, h);
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            g.setAlphaComposite(a);
            CardRenderer.drawCardWithOverlays(g, flipped, cx - w / 2f, cy - h / 2f, w, h, CardStackPosition.Top, false, false, false);
        }
        g.resetAlphaComposite();
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
            CardAreaPanel p = CardAreaPanel.peek(cv);
            if (p == null || p.getWidth() <= 0) { return null; }
            VCardDisplayArea area = p.getDisplayArea();        // forge.screens.match.views.VCardDisplayArea
            if (area == null || !area.isVisible()) { return null; }

            float x = p.localToScreenX(0), y = p.localToScreenY(0);
            float w = p.getWidth(), h = p.getHeight();
            if (x + w <= 0 || y + h <= 0 || x >= Forge.getScreenWidth() || y >= Forge.getScreenHeight()) {
                return null;                                   // scrolled completely off screen
            }
            return new float[] { x, y, w, h };
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