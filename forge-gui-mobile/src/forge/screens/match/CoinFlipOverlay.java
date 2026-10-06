package forge.screens.match;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FTextureRegionImage;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgeConstants;
import forge.toolbox.FOverlay;

public class CoinFlipOverlay extends FOverlay {
    private static final float FLIP_TIME = 1.5f;
    private static final float HOLD_TIME = 0.9f;
    private static final int   SPINS = 4;

    private static final Color HEADS_FALLBACK = new Color(0.95f, 0.80f, 0.25f, 1f);
    private static final Color TAILS_FALLBACK = new Color(0.75f, 0.75f, 0.80f, 1f);

    private final boolean heads;
    private final String caption;
    private final Runnable onDone;

    private Texture headsTex, tailsTex;
    private FImage headsImg, tailsImg;

    private float elapsed;
    private boolean done;
    private float w, h;

    private final boolean waitForTap;
    private boolean released;

    public CoinFlipOverlay(final boolean heads, final String caption, final boolean waitForTap, final Runnable onDone) {
        super(FSkinColor.get(Colors.CLR_OVERLAY));
        this.heads = heads;
        this.caption = caption;
        this.waitForTap = waitForTap;
        this.onDone = onDone;
        loadTextures();
    }

    private void loadTextures() {
        try {
            // adjust the folder to wherever you put the PNGs
            final String dir = ForgeConstants.RES_DIR + "skins/default/";
            final FileHandle hf = Gdx.files.absolute(dir + "coin_heads.png");
            final FileHandle tf = Gdx.files.absolute(dir + "coin_tails.png");
            if (hf.exists() && tf.exists()) {
                headsTex = new Texture(hf);
                tailsTex = new Texture(tf);
                headsTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                tailsTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                headsImg = new FTextureRegionImage(new TextureRegion(headsTex));
                tailsImg = new FTextureRegionImage(new TextureRegion(tailsTex));
            }
        } catch (Exception e) {
            headsImg = tailsImg = null; // fall back to plain squares
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
        final float eased = 1f - (1f - t) * (1f - t) * (1f - t);

        // integer turns => heads up, +0.5 turn => tails up
        final float turns = SPINS + (heads ? 0f : 0.5f);
        final float angle = eased * turns * 2f * (float) Math.PI;
        final float cos = (float) Math.cos(angle);
        final boolean showingHeads = cos >= 0f;
        final float scaleY = Math.max(0.04f, Math.abs(cos));

        final float size = Math.min(w, h) * 0.36f;
        final float lift = 4f * t * (1f - t) * h * 0.25f;
        final float cx = w / 2f;
        final float cy = h / 2f - lift;
        final float drawH = size * scaleY;
        final float x = cx - size / 2f;
        final float y = cy - drawH / 2f;

        final FImage img = showingHeads ? headsImg : tailsImg;
        if (img != null) {
            g.drawImage(img, x, y, size, drawH);
        } else {
            g.fillRect(showingHeads ? HEADS_FALLBACK : TAILS_FALLBACK, x, y, size, drawH);
        }

        if (t >= 1f) {
            final String face = Forge.getLocalizer().getMessage(heads ? "lblHeads" : "lblTails");
            final FSkinFont font = FSkinFont.get(18);
            final FSkinColor text = FSkinColor.get(Colors.CLR_TEXT);
            final float textY = cy + size / 2f + 10f;
            g.drawText(face, font, text, 0, textY, w, font.getLineHeight() * 1.5f, false, Align.center, true);
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
        done = true; // stop drawing before textures are disposed
        if (headsTex != null) headsTex.dispose();
        if (tailsTex != null) tailsTex.dispose();
        onDone.run();   // releases the latch so the game thread continues
    }

    // Every dismissal path (tap, Escape/Back, FOverlay.hideAll) goes through hide().
    @Override public void hide() {
        super.hide();
        release();
    }
}