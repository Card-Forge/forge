package forge.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;
import forge.gui.FThreads;

import java.nio.ByteBuffer;

public class ScreenUtil implements Disposable {
    public static ScreenUtil instance;
    private TextureRegion lastScreenTexture;
    private final int THUMB_WIDTH = 256;
    private final int THUMB_HEIGHT = 144;
    private ScreenUtil() {
    }

    public static ScreenUtil getInstance() {
        return instance == null ? instance = new ScreenUtil() : instance;
    }

    public TextureRegion takeScreenshot() {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (lastScreenTexture != null)
                lastScreenTexture.getTexture().dispose();

            int width = Forge.getScreenWidth();
            int height = Forge.getScreenHeight();
            Texture texture = new Texture(width, height, Pixmap.Format.RGB565);
            lastScreenTexture = new TextureRegion(texture);
            lastScreenTexture.flip(false, true);
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle());
            Gdx.gl20.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        });
        return lastScreenTexture;
    }

    public TextureRegion getLastScreenTexture() {
        return lastScreenTexture;
    }

    public Pixmap getThumbnailPreview() {

        Pixmap pixmap = new Pixmap(THUMB_WIDTH, THUMB_HEIGHT, Pixmap.Format.RGBA8888);

        // Read full framebuffer into a ByteBuffer
        int fbW = Forge.getScreenWidth();
        int fbH = Forge.getScreenHeight();
        ByteBuffer pixels = BufferUtils.newByteBuffer(fbW * fbH * 4);

        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(0, 0, fbW, fbH, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

        // Downscale manually (nearest-neighbor for speed)
        for (int y = 0; y < THUMB_HEIGHT; y++) {
            for (int x = 0; x < THUMB_WIDTH; x++) {
                int srcX = x * fbW / THUMB_WIDTH;
                int srcY = y * fbH / THUMB_HEIGHT;

                int index = (srcY * fbW + srcX) * 4;
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

        // Draw full-res Pixmap into the small Pixmap (CPU hardware downsampling)
        smallPixmap.setFilter(Pixmap.Filter.BiLinear);
        smallPixmap.drawPixmap(original,
            startX, startY, cropWidth, cropHeight,
            0, 0, targetWidth, targetHeight);

        if (Forge.lastPreview != null)
            Forge.lastPreview.dispose();
        Forge.lastPreview = new Texture(smallPixmap);
        Forge.lastPreview.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        smallPixmap.dispose();
    }

    @Override
    public void dispose() {
        if (lastScreenTexture != null)
            Forge.safeDispose(lastScreenTexture.getTexture());
    }
}
