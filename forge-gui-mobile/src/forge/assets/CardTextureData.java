package forge.assets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.apache.commons.lang3.StringUtils;

import forge.Forge;
import forge.util.ImageUtil;

/**
 * TextureData for card images and for WebP images on every platform,
 * supplied to the AssetManager via {@link com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter#textureData}. The stock
 * TextureLoader honours a preset textureData and ignores the resolved FileHandle, so the AssetManager
 * owns the whole lifecycle (load/get/unload/dispose/context-loss reload) while the actual decode stays
 * a plain java.io byte read — iOS can't reliably load a libGDX FileHandle from Documents, but reading
 * bytes directly is proven. WebP images are decoded by the platform, straight into the pixmap. iOS downloaded
 * images and Android card art are downscaled to at most {@link #MAX_CARD_TEX_DIM}px on the longest side since the battlefield draws thumbnails. Re-preparable (mirrors FileTextureData) so GL context-loss
 * reload re-decodes from disk. The border colour is sampled while decoding, so ImageCache doesn't decode again.
 */
final class CardTextureData implements TextureData {
    private static final int MAX_CARD_TEX_DIM = 512;

    private final String path;
    // a mipmap min filter samples black from a texture uploaded without mipmaps
    private final boolean useMipMaps;
    private final boolean downscale;
    private final boolean rgb565IfOpaque;
    private volatile String borderColor;
    private int width, height;
    private Format format = Format.RGBA8888; // real format set in prepare(); safe default pre-prepare
    private Pixmap pixmap;
    private boolean prepared;

    CardTextureData(String path, boolean useMipMaps, boolean downscale, boolean rgb565IfOpaque) {
        this.path = path;
        this.useMipMaps = useMipMaps;
        this.downscale = downscale;
        this.rgb565IfOpaque = rgb565IfOpaque;
    }

    String getPath() {
        return path;
    }

    String getBorderColor() {
        return borderColor;
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
        Pixmap p = GreyscaleTextureLoader.toRgb(decodeFromDisk(path), path);
        if (downscale)
            p = downscale(p, rgb565IfOpaque && isOpaque(path));
        borderColor = ImageCache.sampleBorderColor(p, path);
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
        // (GL context-loss reload)
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
        return useMipMaps;
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
        if (ImageUtil.isWebp(f))
            return decodeWebp(f);
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

    private static Pixmap decodeWebp(File f) {
        Pixmap[] pixmap = new Pixmap[1];
        try {
            // a duplicate, so the platform's writes don't move the position GL uploads from
            Forge.getDeviceAdapter().decodeWebp(f, (w, h) -> (pixmap[0] = new Pixmap(w, h, Format.RGBA8888)).getPixels().duplicate());
        } catch (IOException | RuntimeException e) {
            if (pixmap[0] != null)
                pixmap[0].dispose();
            throw new GdxRuntimeException("Failed decoding card image: " + f, e);
        }
        return pixmap[0];
    }

    // JPEG and simple lossy WebP have no alpha channel, so RGB565 loses nothing they hold
    private static boolean isOpaque(String path) {
        return StringUtils.endsWithIgnoreCase(path, ".jpg") || StringUtils.endsWithIgnoreCase(path, ".jpeg")
                || ImageUtil.isOpaqueWebp(new File(path));
    }

    // Cap the longest side at MAX_CARD_TEX_DIM. Opaque Android card art is also converted to RGB565
    // when it is already within the size limit; PNG/alpha-capable art keeps its original alpha.
    // Any failure falls back to the decoded source so a format conversion cannot prevent card display.
    private static Pixmap downscale(Pixmap src, boolean useRgb565) {
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
