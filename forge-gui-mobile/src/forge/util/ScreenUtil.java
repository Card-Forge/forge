package forge.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import forge.gui.FThreads;

public class ScreenUtil implements Disposable {
    public static ScreenUtil instance;
    private TextureRegion lastScreenTexture;

    private ScreenUtil() {
    }

    public static ScreenUtil getInstance() {
        return instance == null ? instance = new ScreenUtil() : instance;
    }

    public TextureRegion takeScreenshot() {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (lastScreenTexture != null)
                lastScreenTexture.getTexture().dispose();

            int width = Gdx.graphics.getBackBufferWidth();
            int height = Gdx.graphics.getBackBufferHeight();
            Texture texture = new Texture(width, height, Pixmap.Format.RGB888);
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

    @Override
    public void dispose() {
        if (lastScreenTexture != null)
            lastScreenTexture.getTexture().dispose();
    }
}
