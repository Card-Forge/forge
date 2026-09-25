package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;

import com.badlogic.gdx.math.Rectangle;
import forge.Forge;
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
        width = Math.max(width0, 2f);
        height = Math.max(height0, 2f);
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
        return checkFrameBuffer() == null ? null : new TextureRegion(checkFrameBuffer().getColorBufferTexture());
    }

    @Override
    public Texture getTexture() {
        return checkFrameBuffer() == null ? null : checkFrameBuffer().getColorBufferTexture();
    }

    public void clear() {
        final FrameBuffer fb = frameBuffer;
        if (fb != null) {
            frameBuffer = null;
            //must be disposed on EDT thread
            FThreads.invokeInEdtNowOrLater(fb::dispose);
        }
    }

    public FrameBuffer checkFrameBuffer() {
        try {
            if (frameBuffer == null) {
                Graphics g = Forge.getGraphics();
                SpriteBatch batch = g.getBatch();
                boolean wasScissorEnabled = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
                boolean wasDrawing = batch.isDrawing(); //don't assume - check, so we never call end() on an already-paused batch
                Matrix4 savedProjection = wasDrawing ? new Matrix4(batch.getProjectionMatrix()) : null;
                float savedRegionHeight = wasDrawing ? g.getRegionHeight() : 0f;
                Rectangle savedBounds = wasDrawing ? g.getBounds() : null;
                Rectangle savedVisibleBounds = wasDrawing ? g.getVisibleBounds() : null;
                if (wasScissorEnabled) Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
                frameBuffer = new FrameBuffer(Format.RGBA8888, (int) width, (int) height, false);

                try {
                    if (wasDrawing) batch.end();
                    frameBuffer.begin();
                    Gdx.gl.glClearColor(0, 0, 0, 0);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    Matrix4 fboMatrix = new Matrix4().setToOrtho2D(0, 0, width, height);
                    g.setBounds(width, height);
                    g.setProjectionMatrix(fboMatrix);
                    batch.begin();
                    draw(g, width, height);
                    batch.end();
                } finally {
                    frameBuffer.end();
                    if (wasDrawing) {
                        g.setProjectionMatrix(savedProjection);
                        g.setBounds(savedBounds);
                        g.setVisibleBounds(savedVisibleBounds);
                        g.setRegionHeight(savedRegionHeight);
                        batch.begin();
                    }
                    if (wasScissorEnabled) Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frameBuffer;
    }

    public void dispose() {
        if (frameBuffer != null)
            frameBuffer.dispose();
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
