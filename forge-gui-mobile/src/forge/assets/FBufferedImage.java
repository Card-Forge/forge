package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;

import forge.Graphics;
import forge.gui.FThreads;

//Special graphics object for rendering to a texture
public abstract class FBufferedImage extends FImageComplex {
    private final float width, height, opacity;
    private FrameBuffer frameBuffer;

    public FBufferedImage(float width0, float height0) {
        this(width0, height0, 1);
    }
    public FBufferedImage(float width0, float height0, float opacity0) {
        width = width0;
        height = height0;
        opacity = opacity0;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public int getRegionX() {
        return 0;
    }

    @Override
    public int getRegionY() {
        return 0;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return new TextureRegion(checkFrameBuffer().getColorBufferTexture());
    }

    @Override
    public Texture getTexture() {
        return checkFrameBuffer().getColorBufferTexture();
    }

    public void clear() {
        final FrameBuffer fb = frameBuffer;
        if (fb != null) {
            frameBuffer = null;
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() {
                    fb.dispose(); //must be disposed on EDT thread
                }
            });
        }
    }

    public FrameBuffer checkFrameBuffer() {
        if (frameBuffer == null) {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST); //prevent buffered image being clipped

            //render texture to frame buffer if needed
            frameBuffer = new FrameBuffer(Format.RGBA8888, (int)width, (int)height, false);
            frameBuffer.begin();

            //frame graphics must be given a projection matrix
            //so stuff is rendered properly to custom sized frame buffer
            Graphics frameGraphics = new Graphics();
            Matrix4 matrix = new Matrix4();
            matrix.setToOrtho2D(0, 0, width, height);
            frameGraphics.setProjectionMatrix(matrix);

            frameGraphics.begin(width, height);
            draw(frameGraphics, width, height);
            frameGraphics.end();

            frameBuffer.end();
            frameGraphics.dispose();

            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        }
        return frameBuffer;
    }

    protected abstract void draw(Graphics g, float w, float h);

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (opacity < 1) {
            g.setAlphaComposite(opacity);
        }
        g.drawFlippedImage(getTexture(), x, y, w, h); //need to draw image flipped because of how FrameBuffer works
        if (opacity < 1) {
            g.resetAlphaComposite();
        }
    }
}
