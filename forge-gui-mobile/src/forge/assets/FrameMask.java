package forge.assets;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;

/**
 * Antialiased corner masks for the card frame. libgdx's ShapeRenderer has no antialiasing, so shapes built from
 * triangles have jagged, broken looking edges. A frame box is drawn from textured quads instead: a corner mask in each
 * corner and plain rectangles in between, tinted with the color of the box. The masks are made once, in code, so no
 * image files are needed, and they are mipmapped so a card of any size gets smooth edges.
 * <p>
 * Both masks are one quarter of a disc, with its center in the bottom right corner of the texture, so the top left
 * corner is the outside of the shape. Mirroring the texture coordinates gives the other three corners. The "fill" mask is
 * opaque inside of the disc (the corner of a box), the "wedge" mask is opaque outside of it (what a box cuts away from
 * a texture behind it).
 */
public final class FrameMask {
    private static final int SIZE = 128; //a power of two, so the texture can have mipmaps on every device
    private static final int SAMPLES = 8; //samples per pixel and axis, for the coverage of the pixels on the edge
    private static Texture fill, wedge;

    private FrameMask() {
    }

    /** Opaque inside of the quarter disc: the rounded corner of a filled box. */
    public static Texture fill() {
        if (fill == null) {
            fill = build(false);
        }
        return fill;
    }

    /** Opaque outside of the quarter disc: the corner that a rounded box cuts away. */
    public static Texture wedge() {
        if (wedge == null) {
            wedge = build(true);
        }
        return wedge;
    }

    /**
     * The alpha of every pixel (row by row, 0 to 255) of a quarter disc with a radius of size pixels, whose center is
     * the bottom right corner. The edge pixels get the fraction of their area that is inside of the disc.
     */
    public static int[] coverage(int size, int samples, boolean inverse) {
        final int[] alpha = new int[size * size];
        final float radiusSquared = (float) size * size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int inside = 0;
                for (int sy = 0; sy < samples; sy++) {
                    final float dy = size - (y + (sy + 0.5f) / samples);
                    for (int sx = 0; sx < samples; sx++) {
                        final float dx = size - (x + (sx + 0.5f) / samples);
                        if (dx * dx + dy * dy <= radiusSquared) {
                            inside++;
                        }
                    }
                }
                final int a = Math.round(255f * inside / (samples * samples));
                alpha[y * size + x] = inverse ? 255 - a : a;
            }
        }
        return alpha;
    }

    private static Texture build(boolean inverse) {
        final int[] alpha = coverage(SIZE, SAMPLES, inverse);
        final Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixmap.drawPixel(x, y, 0xFFFFFF00 | alpha[y * SIZE + x]); //white, so the tint of the batch is the color
            }
        }
        // managed, so the texture comes back after the GL context is lost; the pixmap has to stay for that
        final Texture texture = new Texture(new PixmapTextureData(pixmap, null, true, false, true));
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }
}
