package forge;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

//Special graphics object for rendering to a texture
public class TextureRenderer extends Graphics {
    private final float width, height;
    private final FrameBuffer frameBuffer;

    public TextureRenderer(float width0, float height0) {
        width = width0;
        height = height0;
        frameBuffer = new FrameBuffer(Format.RGB565, (int)width, (int)height, false);
        frameBuffer.begin();
        begin(width, height);
    }

    public Texture finish() {
        end();
        frameBuffer.end();

        //draw buffered texture to another frame buffer to flip it to the proper orientation
        FrameBuffer fb = new FrameBuffer(Format.RGB565, (int)width, (int)height, false);
        fb.begin();
        begin(width, height);
        drawImage(frameBuffer.getColorBufferTexture(), 0, 0, width, height);
        //frameBuffer.dispose(); //avoid holding on to the first frame buffer
        end();
        fb.end();

        Texture texture = fb.getColorBufferTexture();
        dispose(); //dispose after generating texture
        return texture;
    }
}
