package forge.assets;

import java.nio.ByteBuffer;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;

import forge.gui.error.BugReporter;
import io.sentry.Sentry;

/**
 * libGDX decodes a one-channel image (a greyscale JPEG, as Scryfall serves for some mostly white cards) to an Alpha
 * pixmap, which draws its grey levels as transparency over black. This loader turns such images into RGB888.
 */
class GreyscaleTextureLoader extends TextureLoader {
    GreyscaleTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
        if (parameter == null || parameter.textureData == null) {
            Format format = parameter == null ? null : parameter.format;
            boolean genMipMaps = parameter != null && parameter.genMipMaps;
            TextureData data = TextureData.Factory.loadFromFile(file, format, genMipMaps);
            if (data instanceof FileTextureData && data.getFormat() == Format.Alpha) {
                data.prepare();
                Pixmap pixmap = toRgb(data.consumePixmap(), file.path());
                data = pixmap.getFormat() == Format.Alpha
                        ? new FileTextureData(file, pixmap, format, genMipMaps)
                        : new GreyscaleFileTextureData(file, pixmap, format, genMipMaps);
            }
            // getTextureFilter() is shared, never store per-file data in it
            TextureParameter decoded = new TextureParameter();
            decoded.textureData = data;
            decoded.texture = parameter == null ? null : parameter.texture;
            parameter = decoded;
        }
        super.loadAsync(manager, fileName, file, parameter);
    }

    /** Copies each grey level of an Alpha pixmap into R, G and B, and disposes the source. Returns the source if that fails. */
    static Pixmap toRgb(Pixmap alpha, String source) {
        if (alpha.getFormat() != Format.Alpha)
            return alpha;
        int width = alpha.getWidth();
        int height = alpha.getHeight();
        Pixmap rgb;
        try {
            rgb = new Pixmap(width, height, Format.RGB888);
        } catch (RuntimeException e) {
            String error = "Greyscale image not converted, it will draw dark: " + source;
            System.err.println(error);
            e.printStackTrace();
            if (BugReporter.isSentryEnabled()) {
                Sentry.addBreadcrumb(error);
                Sentry.captureException(e);
            }
            return alpha;
        }
        ByteBuffer src = alpha.getPixels();
        ByteBuffer dst = rgb.getPixels();
        for (int i = 0, n = width * height; i < n; i++) {
            byte grey = src.get(i);
            dst.put(i * 3, grey);
            dst.put(i * 3 + 1, grey);
            dst.put(i * 3 + 2, grey);
        }
        alpha.dispose();
        return rgb;
    }

    /** Stays a FileTextureData so ImageCache keys it by path and a lost GL context reloads and reconverts it from the file. */
    private static final class GreyscaleFileTextureData extends FileTextureData {
        GreyscaleFileTextureData(FileHandle file, Pixmap pixmap, Format format, boolean useMipMaps) {
            super(file, pixmap, format, useMipMaps);
        }

        @Override
        public Pixmap consumePixmap() {
            return toRgb(super.consumePixmap(), getFileHandle().path());
        }
    }
}
