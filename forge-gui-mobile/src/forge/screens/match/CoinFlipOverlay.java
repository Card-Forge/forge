package forge.screens.match;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.gui.FThreads;
import forge.toolbox.FOverlay;


public class CoinFlipOverlay extends FOverlay {
    private static final float FLIP_TIME = 1.5f;
    private static final float HOLD_TIME = 1.2f;
    private static final int   SPINS = 3;

    private static final Color HEADS_FALLBACK = new Color(0.95f, 0.80f, 0.25f, 1f);
    private static final Color TAILS_FALLBACK = new Color(0.75f, 0.75f, 0.80f, 1f);

    private final boolean heads;
    private final String caption;
    private final Runnable onDone;

    private Coin3D coin;
    private boolean coinFailed;

    private float elapsed;
    private boolean done;
    private float w, h;

    private final boolean waitForTap;
    private boolean released;

    private final Matrix4 savedProjection = new Matrix4();
    private final Matrix4 projection = new Matrix4();

    public CoinFlipOverlay(final boolean heads, final String caption, final boolean waitForTap, final Runnable onDone) {
        super(FSkinColor.get(Colors.CLR_OVERLAY));
        this.heads = heads;
        this.caption = caption;
        this.waitForTap = waitForTap;
        this.onDone = onDone;
    }

    /** Built on the first frame, when the overlay size (and so the render size) is known. */
    private void createCoin(final float diameter) {
        try {
            Texture headsTex = null, tailsTex = null;
            try {
                headsTex = Forge.getAssets().getCoinHead();
                tailsTex = Forge.getAssets().getCoinTail();
            } catch (Exception e) {
                headsTex = tailsTex = null; // plain metal faces
            }
            final float pxScale = w > 0 ? Gdx.graphics.getBackBufferWidth() / w : 1f;
            final float box = diameter / (2f * Coin3D.R / Coin3D.FRAME);
            final int fb = MathUtils.clamp(Math.round(box * pxScale), 256, 1024);
            coin = new Coin3D(headsTex, tailsTex, fb);
        } catch (RuntimeException e) {
            Gdx.app.error("CoinFlipOverlay", "failed to create 3D coin", e);
            coinFailed = true;
        }
    }

    @Override
    protected void doLayout(final float width, final float height) {
        w = width;
        h = height;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        if (elapsed < FLIP_TIME) {
            elapsed = FLIP_TIME;   // first tap: skip to result
        } else {
            finish();              // second tap: dismiss
        }
        return true;
    }

    @Override
    public void drawOverlay(final Graphics g) {
        if (done) {
            return;
        }
        elapsed += Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        final float t = Math.min(1f, elapsed / FLIP_TIME);
        final float settle = Math.max(0f, elapsed - FLIP_TIME);

        final float size = Math.min(w, h) * 0.36f;          // on-screen coin diameter
        final float lift = 4f * t * (1f - t) * h * 0.25f;
        final float cx = w / 2f;
        final float cy = h / 2f - lift;

        if (coin == null && !coinFailed) {
            createCoin(size);
        }
        if (coin != null) {
            // integer turns => heads up, +0.5 turn => tails up
            coin.pose(t, settle, SPINS + (heads ? 0f : 0.5f));
            drawCoin(cx, cy, size);
        } else {
            // 3D failed: flat fallback so the game still gets its result
            final float scaleY = Math.max(0.04f, Math.abs((float) Math.cos(t * (SPINS + (heads ? 0f : 0.5f)) * 2f * Math.PI)));
            g.fillRect(heads ? HEADS_FALLBACK : TAILS_FALLBACK, cx - size / 2f, cy - size * scaleY / 2f, size, size * scaleY);
        }

        if (t >= 1f) {
            final FSkinFont font = FSkinFont.get(18);
            final FSkinColor text = FSkinColor.get(Colors.CLR_TEXT);
            final float textY = cy + size / 2f + 10f;
            g.drawText(caption, font, text, 0, textY + font.getLineHeight() * 1.6f,
                    w, font.getLineHeight() * 3f, true, Align.center, false);
            if (waitForTap) {
                g.drawText(Forge.getLocalizer().getMessageorUseDefault("lblTapToContinue", "Tap to continue"), FSkinFont.get(12), text,
                        0, h - FSkinFont.get(12).getLineHeight() * 3f, w, FSkinFont.get(12).getLineHeight() * 2f,
                        false, Align.center, true);
            } else if (elapsed >= FLIP_TIME + HOLD_TIME) {
                finish();
            }
        }
        Gdx.graphics.requestRendering();
    }

    /**
     * Renders the coin into its FrameBuffer (which needs Forge's batch closed for a moment) and then draws
     * the finished picture through the same batch with its own pixel projection.
     */
    private void drawCoin(final float cx, final float cy, final float diameter) {
        final Batch b = Forge.getGraphics().getBatch();
        final boolean wasDrawing = b.isDrawing();
        if (wasDrawing) {
            b.end();
        }
        coin.render();
        if (wasDrawing) {
            b.begin();
        }

        final float bw = Gdx.graphics.getBackBufferWidth();
        final float bh = Gdx.graphics.getBackBufferHeight();
        final float sx = bw / w, sy = bh / h;
        final float box = diameter / (2f * Coin3D.R / Coin3D.FRAME); // the 3D scene is a square around the coin
        final float px = (cx - box / 2f) * sx;
        final float pw = box * sx, ph = box * sy;
        final float py = bh - (cy + box / 2f) * sy;                  // Forge's y points down, the batch's up

        savedProjection.set(b.getProjectionMatrix());
        final float oldColor = b.getPackedColor();
        projection.setToOrtho2D(0, 0, bw, bh);
        b.setProjectionMatrix(projection);
        if (!b.isDrawing()) {
            b.begin();
        }
        b.setColor(1f, 1f, 1f, 1f);
        b.draw(coin.getRegion(), px, py, pw, ph);
        if (!wasDrawing) {
            b.end();
        }
        b.setPackedColor(oldColor);
        b.setProjectionMatrix(savedProjection);
    }

    private void finish() {
        if (done) {
            return;
        }
        done = true;
        FThreads.invokeInEdtLater(this::hide);
    }

    private void release() {
        if (released) {
            return;
        }
        released = true;
        done = true; // stop drawing before the coin is disposed
        if (coin != null) {
            coin.dispose();
            coin = null;
        }
        onDone.run();   // releases the latch so the game thread continues
    }

    // Every dismissal path (tap, Escape/Back, FOverlay.hideAll) goes through hide().
    @Override public void hide() {
        super.hide();
        release();
    }
}