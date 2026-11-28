package forge.util;

import forge.ImageKeys;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class WebpImagesTest {
    // an 8x11 lossy WebP
    private static final byte[] WEBP = Base64.getDecoder().decode(
            "UklGRjwAAABXRUJQVlA4IDAAAADwAQCdASoIAAsAAUAmJaACdLoB+AAEgwAA/u4KZ/5BcsLrka/9pZ+pZ+pZ/ioAAAA=");

    // Fails if desktop has no WebP reader
    @Test
    public void desktopReadsWebp() throws Exception {
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(WEBP));
        Assert.assertNotNull(image);
        Assert.assertEquals(image.getWidth(), 8);
        Assert.assertEquals(image.getHeight(), 11);
    }

    // Fails if the format is taken from the file name
    @Test
    public void webpIsRecognisedByItsBytes() throws Exception {
        final Path dir = Files.createTempDirectory("webp-sniff");
        try {
            final File misnamed = dir.resolve("Shock.fullborder.jpg").toFile();
            Files.write(misnamed.toPath(), WEBP);
            Assert.assertTrue(ImageUtil.isWebp(misnamed));

            final File jpeg = dir.resolve("Opt.fullborder.webp").toFile();
            Files.write(jpeg.toPath(), new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1 });
            Assert.assertFalse(ImageUtil.isWebp(jpeg));

            // a RIFF file of another kind, and one cut off before the format tag
            Assert.assertFalse(ImageUtil.isWebp("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII)));
            Assert.assertFalse(ImageUtil.isWebp("RIFF\0\0\0\0WEB".getBytes(StandardCharsets.US_ASCII)));
            Assert.assertFalse(ImageUtil.isWebp(dir.resolve("missing.jpg").toFile()));
        } finally {
            FileUtil.deleteDirectory(dir.toFile());
        }
    }

    // Fails if WebP that can hold alpha would be stored as RGB565 and lose its transparent corners
    @Test
    public void onlySimpleLossyWebpIsOpaque() throws Exception {
        final Path dir = Files.createTempDirectory("webp-alpha");
        try {
            final File lossy = dir.resolve("Shock.fullborder.webp").toFile();
            Files.write(lossy.toPath(), WEBP);
            Assert.assertTrue(ImageUtil.isOpaqueWebp(lossy));

            final File lossless = dir.resolve("Opt.fullborder.webp").toFile();
            Files.write(lossless.toPath(), "RIFF\0\0\0\0WEBPVP8L\0\0\0\0".getBytes(StandardCharsets.US_ASCII));
            Assert.assertFalse(ImageUtil.isOpaqueWebp(lossless));

            final File extended = dir.resolve("Ponder.fullborder.webp").toFile();
            Files.write(extended.toPath(), "RIFF\0\0\0\0WEBPVP8X\0\0\0\0".getBytes(StandardCharsets.US_ASCII));
            Assert.assertFalse(ImageUtil.isOpaqueWebp(extended));
        } finally {
            FileUtil.deleteDirectory(dir.toFile());
        }
    }

    // Fails if a WebP download keeps the .jpg name its path was chosen with
    @Test
    public void webpDownloadsAreNamedWebp() {
        Assert.assertEquals(ImageKeys.withExtension("cache/M21/Shock.fullborder.jpg", ".webp"), "cache/M21/Shock.fullborder.webp");
    }

    // Fails if a card cached as JPEG would be downloaded again as WebP, or if a later WebP replaced the JPEG in use
    @Test
    public void aCachedJpegIsKept() throws Exception {
        final Path dir = Files.createTempDirectory("webp-cache");
        try {
            final File asked = dir.resolve("Shock.fullborder.jpg").toFile();
            Assert.assertNull(ImageKeys.getCachedFile(asked));

            final File webp = dir.resolve("Shock.fullborder.webp").toFile();
            Files.write(webp.toPath(), WEBP);
            Assert.assertEquals(ImageKeys.getCachedFile(asked), webp);

            Files.write(asked.toPath(), new byte[] { 1 });
            Assert.assertEquals(ImageKeys.getCachedFile(asked), asked);
        } finally {
            FileUtil.deleteDirectory(dir.toFile());
        }
    }
}
