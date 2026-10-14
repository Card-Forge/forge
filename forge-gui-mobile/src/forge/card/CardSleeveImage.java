package forge.card;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkin;

/**
 * A deck sleeve rendered from a card's art (cover-cropped). It reports the built-in sleeve aspect
 * ratio so it matches the standard sleeves wherever an FImage's intrinsic size drives layout (e.g.
 * the lobby sleeve label); the actual draw cover-crops the art into whatever bounds it is given,
 * reusing {@link CardAvatarImage}'s rendering, then frames it like the built-in sleeves.
 */
public class CardSleeveImage implements FImage {
    // Matches the dark frame on the built-in sleeve sprites, ~4% of the short edge
    private static final Color BORDER = new Color(38 / 255f, 37 / 255f, 38 / 255f, 1f);
    private static final float BORDER_FRACTION = 0.04f;

    private final CardAvatarImage art;

    public CardSleeveImage(final String imageKey) {
        this(imageKey, 500);
    }
    public CardSleeveImage(final String imageKey, final int cropOffset) {
        this.art = new CardAvatarImage(imageKey, cropOffset);
    }

    @Override
    public float getWidth() {
        final TextureRegion s = FSkin.getSleeves().get(0);
        return s != null ? s.getRegionWidth() : 360f;
    }

    @Override
    public float getHeight() {
        final TextureRegion s = FSkin.getSleeves().get(0);
        return s != null ? s.getRegionHeight() : 500f;
    }

    @Override
    public void draw(final Graphics g, final float x, final float y, final float w, final float h) {
        art.draw(g, x, y, w, h);
        final float bw = Math.min(w, h) * BORDER_FRACTION;
        g.fillRect(BORDER, x, y, w, bw);
        g.fillRect(BORDER, x, y + h - bw, w, bw);
        g.fillRect(BORDER, x, y, bw, h);
        g.fillRect(BORDER, x + w - bw, y, bw, h);
    }
}
