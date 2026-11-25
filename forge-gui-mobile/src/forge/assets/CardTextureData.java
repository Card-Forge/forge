package forge.assets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * TextureData for selected card images (iOS downloaded images and Android card art), supplied to the AssetManager via
 * {@link com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter#textureData}. The stock
 * TextureLoader honours a preset textureData and ignores the resolved FileHandle, so the AssetManager
 * owns the whole lifecycle (load/get/unload/dispose/context-loss reload) while the actual decode stays
 * a plain java.io byte read — iOS can't reliably load a libGDX FileHandle from Documents, but reading
 * bytes directly is proven. Downscales to at most {@link #MAX_CARD_TEX_DIM}px on the longest side since
 * the battlefield draws thumbnails. Re-preparable (mirrors FileTextureData) so getpixelColor's
 * prepare/consumePixmap border sampling and GL context-loss reload both re-decode from disk.
 */
final class CardTextureData implements TextureData {
    private static final int MAX_CARD_TEX_DIM = 512;

    private final String path;
    private final boolean useRgb565;
    private final boolean preserveOriginalQuality;
    private int width, height;
    private Format format = Format.RGBA8888; // real format set in prepare(); safe default pre-prepare
    private Pixmap pixmap;
    private boolean prepared;

    CardTextureData(String path) {
        this(path, false, false);
    }

    CardTextureData(String path, boolean useRgb565) {
        this(path, useRgb565, false);
    }

    CardTextureData(String path, boolean useRgb565, boolean preserveOriginalQuality) {
        this.path = path;
        this.useRgb565 = useRgb565;
        this.preserveOriginalQuality = preserveOriginalQuality;
    }

    String getPath() {
        return path;
    }

    @Override
    public TextureDataType getType() {
        return TextureDataType.Pixmap;
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public void prepare() {
        if (prepared)
            throw new GdxRuntimeException("Already prepared");
        Pixmap p = downscale(GreyscaleTextureLoader.toRgb(decodeFromDisk(path), path), useRgb565, preserveOriginalQuality);
        pixmap = p;
        width = p.getWidth();
        height = p.getHeight();
        format = p.getFormat();
        prepared = true;
    }

    @Override
    public Pixmap consumePixmap() {
        if (!prepared)
            throw new GdxRuntimeException("Call prepare() before consumePixmap()");
        // mirror FileTextureData: hand off the pixmap and un-prepare so the data can be re-prepared
        // (getpixelColor border sampling, GL context-loss reload)
        prepared = false;
        Pixmap p = pixmap;
        pixmap = null;
        return p;
    }

    @Override
    public boolean disposePixmap() {
        return true; // the CPU pixmap is freed right after GPU upload; nothing retained
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Format getFormat() {
        return format;
    }

    @Override
    public boolean useMipMaps() {
        return false;
    }

    @Override
    public boolean isManaged() {
        return true; // enables libGDX's own context-loss reload via prepare()
    }

    @Override
    public void consumeCustomData(int target) {
        throw new GdxRuntimeException("CardTextureData does not upload custom data");
    }

    // Proven iOS read: java.io bytes, not a libGDX FileHandle.
    private static Pixmap decodeFromDisk(String path) {
        File f = new File(path);
        byte[] bytes = new byte[(int) f.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            int off = 0, n;
            while (off < bytes.length && (n = fis.read(bytes, off, bytes.length - off)) > 0)
                off += n;
        } catch (IOException e) {
            throw new GdxRuntimeException("Failed reading card image: " + path, e);
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
        }
        return new Pixmap(bytes, 0, bytes.length);
    }

    // Cap the longest side at MAX_CARD_TEX_DIM. Android JPEG card art is also converted to RGB565
    // when it is already within the size limit; PNG/alpha-capable art keeps its original alpha.
    // Original-quality mode bypasses both transformations.
    // Any failure falls back to the decoded source so a format conversion cannot prevent card display.
    private static Pixmap downscale(Pixmap src, boolean useRgb565, boolean preserveOriginalQuality) {
        if (preserveOriginalQuality)
            return src;
        try {
            int w = src.getWidth();
            int h = src.getHeight();
            int maxSide = Math.max(w, h);
            if (maxSide <= MAX_CARD_TEX_DIM && !useRgb565)
                return src;
            float s = maxSide <= MAX_CARD_TEX_DIM ? 1f : (float) MAX_CARD_TEX_DIM / (float) maxSide;
            int nw = Math.max(1, Math.round(w * s));
            int nh = Math.max(1, Math.round(h * s));
            Pixmap.Format targetFormat = useRgb565 ? Pixmap.Format.RGB565 : Pixmap.Format.RGBA8888;
            Pixmap dst = new Pixmap(nw, nh, targetFormat);
            dst.setFilter(Pixmap.Filter.BiLinear);
            dst.drawPixmap(src, 0, 0, w, h, 0, 0, nw, nh);
            src.dispose();
            return dst;
        } catch (Exception e) {
            return src;
        }
    }
}
