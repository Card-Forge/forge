package forge.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;
import forge.FrameRate;

import java.nio.ByteBuffer;

public class ScreenUtil implements Disposable {
    public static ScreenUtil instance;
    private TextureRegion screenTextureRegion = null;
    private Texture screenshotTexture = null;
    private final int THUMB_WIDTH = 256;
    private final int THUMB_HEIGHT = 144;
    ByteBuffer pixels;
    int bufferSize, width, height;
    private static boolean isInitialized = false;
    private volatile boolean pendingScreenshot = false;
    private static boolean firstCapture = true;

    private ScreenUtil() {
        width = Forge.getScreenWidth();
        height = Forge.getScreenHeight();
        bufferSize = width * height * 4;
        pixels = BufferUtils.newByteBuffer(bufferSize);
    }

    public static ScreenUtil getInstance() {
        return instance == null ? instance = new ScreenUtil() : instance;
    }

    public void initScreenshotBuffer() {
        if (isInitialized) return;
        screenshotTexture = new Texture(width, height, Pixmap.Format.RGB565);
        screenTextureRegion = new TextureRegion(screenshotTexture);
        screenTextureRegion.flip(false, true);
        isInitialized = true;
    }

    public TextureRegion takeScreenshot() {
        if (!isInitialized) {
            initScreenshotBuffer();
        }
        FrameRate.hideFPSCountdown = Forge.isMobileAdventureMode ? 1 : 2;
        pendingScreenshot = true;
        return screenTextureRegion;
    }

    public void onRenderFrame() {
        if (!pendingScreenshot) return;
        // Only copy when hideFPSCountdown has reached 0 (FPS is hidden) or firstCapture
        if (firstCapture || FrameRate.hideFPSCountdown <= 0) {
            Gdx.app.postRunnable(() -> {
                Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, screenshotTexture.getTextureObjectHandle());
                Gdx.gl20.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
                Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
                pendingScreenshot = false;
                firstCapture = false;
            });
        }
    }

    public TextureRegion getLastScreenTexture() {
        return screenTextureRegion;
    }

    public Pixmap getThumbnailPreview() {
        Pixmap pixmap = new Pixmap(THUMB_WIDTH, THUMB_HEIGHT, Pixmap.Format.RGBA8888);
        pixels.clear();
        // Read full framebuffer into a ByteBuffer
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(0, 0, width, height, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
        pixels.rewind();

        // Downscale manually (nearest-neighbor for speed)
        for (int y = 0; y < THUMB_HEIGHT; y++) {
            for (int x = 0; x < THUMB_WIDTH; x++) {
                int srcX = x * width / THUMB_WIDTH;
                int srcY = y * height / THUMB_HEIGHT;

                int index = (srcY * width + srcX) * 4;
                int r = pixels.get(index) & 0xFF;
                int g = pixels.get(index + 1) & 0xFF;
                int b = pixels.get(index + 2) & 0xFF;
                int a = pixels.get(index + 3) & 0xFF;

                pixmap.drawPixel(x, THUMB_HEIGHT - 1 - y, (r << 24) | (g << 16) | (b << 8) | a);
            }
        }
        updateLastPreview(pixmap, 0.15f);
        return pixmap;
    }

    private void updateLastPreview(Pixmap original, float scaleFactor) {
        // Calculate tiny target dimensions
        int targetWidth = Math.max(1, Math.round(original.getWidth() * scaleFactor));
        int targetHeight = Math.max(1, Math.round(original.getHeight() * scaleFactor));
        int cropWidth = (int) (original.getWidth() * 0.66);
        int cropHeight = (int) (original.getHeight() * 0.66);
        int startX = (original.getWidth() - cropWidth) / 2;
        int startY = (original.getHeight() - cropHeight) / 2;

        // Create a small, lightweight Pixmap
        Pixmap smallPixmap = new Pixmap(targetWidth, targetHeight, original.getFormat());
        // BiLinear filter is used to achieve a cheap blur on smallPixmap
        smallPixmap.setFilter(Pixmap.Filter.BiLinear);
        // Draw cropped Pixmap into the small Pixmap (CPU hardware downsampling)
        smallPixmap.drawPixmap(original,
            startX, startY, cropWidth, cropHeight,
            0, 0, targetWidth, targetHeight);

        try { // try to reuse lastPreview texture and draw the smallPixmap to save texture VRAM
            if (Forge.lastPreview != null)
                Forge.lastPreview.draw(smallPixmap, 0, 0);
            else
                Forge.lastPreview = new Texture(smallPixmap);
        } catch (Exception e) {
            // fallback if you can't draw the smallPixmap
            if (Forge.lastPreview != null)
                Forge.lastPreview.dispose();
            Forge.lastPreview = new Texture(smallPixmap);
        } finally {
            Forge.lastPreview.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        smallPixmap.dispose();
    }

    @Override
    public void dispose() {
        Forge.safeDispose(screenshotTexture);
    }
}
