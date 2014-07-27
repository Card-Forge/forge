package forge;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

//Special graphics object for rendering to a texture
public class TextureRenderer extends Graphics {
    private final FrameBuffer frameBuffer;

    public TextureRenderer(float width, float height) {
        frameBuffer = new FrameBuffer(Format.RGB565, (int)width, (int)height, false);
        frameBuffer.begin();
        begin(width, height);
    }

    public Texture finish() {
        end();

        Texture texture = frameBuffer.getColorBufferTexture();

        dispose(); //dispose after generating texture
        frameBuffer.dispose();

        return texture;
    }
}
