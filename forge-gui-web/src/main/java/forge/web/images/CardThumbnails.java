package forge.web.images;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Makes card images the browser cannot use as stored. It shrinks them with a proper filter, or paints them as a foil. A browser that shrinks a full scan on a rotated or animated layer breaks up the text. */
public final class CardThumbnails {
    /** About twice the widest size a board card is drawn at, so it stays sharp on a screen at twice normal scale. */
    public static final int WIDTH = 256;
    private static final int MOST_KEPT = 600;
    private static final float JPEG_QUALITY = 0.88f;

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2,
            new ThreadFactoryBuilder().setNameFormat("CardThumbnails").setDaemon(true).build());
    /** Each card's thumbnail, while it is being made or after. Browsers that ask at the same time share one render. */
    private static final Map<String, CompletableFuture<byte[]>> KEPT = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, CompletableFuture<byte[]>> eldest) {
            return size() > MOST_KEPT;
        }
    });

    private CardThumbnails() {
    }

    /** The image in its own format. It is shrunk to WIDTH when small is true, and painted as a foil when foil is a seed above 0. An image that needs neither is returned unchanged. */
    public static CompletableFuture<byte[]> of(final File file, final boolean small, final int foil) {
        final String key = file.getAbsolutePath() + ':' + file.lastModified() + ':' + small + ':' + foil;
        final CompletableFuture<byte[]> made = KEPT.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> {
            try {
                return render(file, small, foil);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }, WORKERS));
        // A failure is not kept, so the next request tries again
        made.whenComplete((bytes, failed) -> {
            if (failed != null) {
                KEPT.remove(key, made);
            }
        });
        return made;
    }

    private static byte[] render(final File file, final boolean small, final int foil) throws IOException {
        final BufferedImage source = ImageIO.read(file);
        if (source == null) {
            throw new IOException("Not an image: " + file);
        }
        final boolean shrink = small && source.getWidth() > WIDTH;
        if (!shrink && foil == 0) {
            return Files.readAllBytes(file.toPath());
        }
        final boolean png = file.getName().toLowerCase(Locale.ROOT).endsWith(".png");
        BufferedImage image = source;
        // Halving the size each step with bilinear sampling reads every source pixel. One large step would skip pixels
        while (shrink && image.getWidth() > WIDTH) {
            final int w = Math.max(WIDTH, image.getWidth() / 2);
            final int h = Math.max(1, Math.round(image.getHeight() * (float) w / image.getWidth()));
            image = scaled(image, w, h, png);
        }
        if (foil > 0) {
            Foil.paint(image, foil);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (png) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        final ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static BufferedImage scaled(final BufferedImage from, final int w, final int h, final boolean alpha) {
        final BufferedImage to = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(from, 0, 0, w, h, null);
        g.dispose();
        return to;
    }
}
