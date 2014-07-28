package forge;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;

//Special graphics object for rendering to a texture
public class TextureRenderer extends Graphics {
    private final float width, height;
    private final FrameBuffer frameBuffer;

    public TextureRenderer(float width0, float height0) {
        width = width0;
        height = height0;
        frameBuffer = new FrameBuffer(Format.RGB565, (int)width, (int)height, false);
        frameBuffer.begin();

        //batch and shapeRenderer must be given a projection matrix
        //so they're rendered properly to custom sized frame buffer
        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(matrix);
        shapeRenderer.setProjectionMatrix(matrix);

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
        end();
        fb.end();

        Texture texture = fb.getColorBufferTexture();
        dispose(); //dispose after generating texture
        return texture;
    }
}
