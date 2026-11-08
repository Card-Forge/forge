package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.util.Config;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.toolbox.FDisplayObject;
import forge.util.ShaderUtil;
import forge.util.TextBounds;
import forge.util.Utils;

import java.util.Arrays;

public class Graphics implements Disposable {
    private static final int GL_BLEND = GL20.GL_BLEND;
    private static final int GL_LINE_SMOOTH = 2848; //create constant here since not in GL20

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Vector3 tmp = new Vector3();
    // scratch colors for alpha-composited draws (avoids allocating a Color per draw call)
    private final Color fadeA = new Color();
    private final Color fadeB = new Color();
    private float regionHeight;
    private Rectangle bounds = new Rectangle();
    private Rectangle visibleBounds = new Rectangle();
    private int failedClipCount;
    private boolean clipImbalanceReported;
    private float alphaComposite = 1;
    private int transformCount = 0;
    private boolean isDisposed = false;
    private static final float[] arrowVertices = new float[14];
    private static final int CURVE_SEGMENTS = 30;
    // arrow batching (see drawArrowBatch)
    private boolean arrowBatchActive;
    private int arrowPhase; // 0 = fills, 1 = primary lines, 2 = secondary lines
    private boolean arrowNeedPhase2;
    private boolean lineSmoothOn;
    private float lineWidthNow = 1;
    // scratch geometry for the curved arrow/pointer currently being drawn (screen coordinates)
    private boolean caCurved, caHead;
    private float caSx1, caSy1, caSx2, caSy2, caLX, caLY, caRX, caRY;
    // scratch geometry for drawArrow
    private float arTx1, arTy1, arTx2, arTy2;
    private final float[] curvePts = new float[(CURVE_SEGMENTS + 1) * 2]; // screen-space bezier points, reused by the fill and stroke passes
    // arrowhead wing directions never change, so compute them once (same expressions as before => identical values)
    private static final float WING_SPREAD = (float) Math.toRadians(35);
    private static final float WING_COS_L = (float) Math.cos(Math.PI - WING_SPREAD);
    private static final float WING_SIN_L = (float) Math.sin(Math.PI - WING_SPREAD);
    private static final float WING_COS_R = (float) Math.cos(Math.PI + WING_SPREAD);
    private static final float WING_SIN_R = (float) Math.sin(Math.PI + WING_SPREAD);
    private static final Vector2 vectorAngleHelper1 = new Vector2();
    private static final Vector2 vectorAngleHelper2 = new Vector2();
    private static final Vector2 vectorAngleHelper3 = new Vector2();
    private static final TextBounds textBounds = new TextBounds();
    private static final Rectangle tmpBounds = new Rectangle();
    private int clipDepth = 0;
    // per-instance (ScissorStack keeps the Rectangle references, so instances must not share slots); grows on demand
    private Rectangle[] clipPool = new Rectangle[16];
    // results of fitText(), shared by the passes of one (outlined) text draw
    private FSkinFont fitFont;
    private boolean fitNeedClip;
    private float fitHeight;
    private static final Color LINING_DARK = Color.valueOf("#171717");
    private static final Color LINING_LIGHT = Color.valueOf("#fffffd");

    public Graphics(final int spriteCapacity) {
        batch = new SpriteBatch(spriteCapacity);
    }

    public void begin(float regionWidth0, float regionHeight0) {
        batch.begin();
        setBounds(regionWidth0, regionHeight0);
    }

    public void setBounds(float regionWidth0, float regionHeight0) {
        bounds.set(0, 0, regionWidth0, regionHeight0);
        regionHeight = regionHeight0;
        visibleBounds.set(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void setRegionHeight(float regionHeight0) {
        regionHeight = regionHeight0;
    }

    public float getRegionHeight() {
        return regionHeight;
    }

    public void setBounds(Rectangle bounds0) {
        bounds.set(bounds0.x, bounds0.y, bounds0.width, bounds0.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setVisibleBounds(Rectangle visibleBounds0) {
        visibleBounds.set(visibleBounds0.x, visibleBounds0.y, visibleBounds0.width, visibleBounds0.height);
    }

    public Rectangle getVisibleBounds() {
        return visibleBounds;
    }

    public void end() {
        // GdxRuntimeException: No buffer allocated! is thrown when when batch is already disposed
        if (isDisposed)
            return;
        if (batch.isDrawing()) {
            batch.end();
        }
        if (shapeRenderer.getCurrentType() != null) {
            shapeRenderer.end();
        }
        // the Arena shaders are bound lazily and left bound while drawing; never carry them past the frame
        final ShaderProgram bound = batch.getShader();
        if (bound != null && (bound == arenaShader || bound == arenaTiltShader)) {
            batch.setShader(null);
        }
        resetClipState();
    }

    // A frame must not leave a clip behind. If an exception skipped an endClip, whatever is left would otherwise stay on
    // libGDX's static scissor stack and keep the depth counter (and the pool) growing frame after frame.
    private void resetClipState() {
        if (clipDepth == 0 && failedClipCount == 0) {
            return;
        }
        if (!clipImbalanceReported) {
            clipImbalanceReported = true;
            System.err.println("Graphics: unbalanced startClip/endClip at the end of a frame (depth=" + clipDepth + ", failed=" + failedClipCount + "), resetting");
        }
        while (clipDepth > 0) {
            try {
                ScissorStack.popScissors();
            } catch (RuntimeException e) {
                break; // nothing left to pop
            }
            clipDepth--;
        }
        clipDepth = 0;
        failedClipCount = 0;
    }

    @Override
    public void dispose() {
        if (isDisposed) {
            return;
        }
        isDisposed = true;
        Forge.safeDispose(batch, shapeRenderer);
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public boolean startClip() {
        return startClip(0, 0, bounds.width, bounds.height);
    }

    public boolean startClip(float x, float y, float w, float h) {
        batch.flush(); // must flush batch to prevent other things not rendering

        // reusable but different approach
        final int activePoolIdx = clipDepth;
        if (activePoolIdx >= clipPool.length) {
            clipPool = Arrays.copyOf(clipPool, clipPool.length * 2);
        }
        Rectangle activeClip = clipPool[activePoolIdx];
        if (activeClip == null) {
            activeClip = clipPool[activePoolIdx] = new Rectangle();
        }

        // Advance our depth pointer before processing layout math
        clipDepth++;

        activeClip.set(adjustX(x), adjustY(y, h), w, h);

        if (transformCount != 0) { // transform position if needed
            tmp.set(activeClip.x, activeClip.y, 0);
            tmp.mul(batch.getTransformMatrix());
            float minX = tmp.x;
            float maxX = minX;
            float minY = tmp.y;
            float maxY = minY;

            tmp.set(activeClip.x + activeClip.width, activeClip.y, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) {
                minX = tmp.x;
            } else if (tmp.x > maxX) {
                maxX = tmp.x;
            }
            if (tmp.y < minY) {
                minY = tmp.y;
            } else if (tmp.y > maxY) {
                maxY = tmp.y;
            }

            tmp.set(activeClip.x + activeClip.width, activeClip.y + activeClip.height, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) {
                minX = tmp.x;
            } else if (tmp.x > maxX) {
                maxX = tmp.x;
            }
            if (tmp.y < minY) {
                minY = tmp.y;
            } else if (tmp.y > maxY) {
                maxY = tmp.y;
            }

            tmp.set(activeClip.x, activeClip.y + activeClip.height, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) {
                minX = tmp.x;
            } else if (tmp.x > maxX) {
                maxX = tmp.x;
            }
            if (tmp.y < minY) {
                minY = tmp.y;
            } else if (tmp.y > maxY) {
                maxY = tmp.y;
            }

            activeClip.set(minX, minY, maxX - minX, maxY - minY);
        }

        if (!ScissorStack.pushScissors(activeClip)) {
            failedClipCount++; // tracked failed clips to prevent calling popScissors on endClip
            clipDepth--; // ScissorStack did not keep this slot, release it so the depth doesn't drift
            return false;
        }
        return true;
    }

    public void endClip() {
        if (failedClipCount == 0) {
            if (clipDepth == 0) {
                return; // nothing open (e.g. the frame was reset mid-clip), don't touch ScissorStack
            }
            batch.flush(); // must flush batch to ensure stuff rendered during clip respects that clip
            ScissorStack.popScissors();
            clipDepth--;
        } else {
            failedClipCount--;
        }
    }

    public void draw(FDisplayObject displayObj) {
        if (displayObj == null || displayObj.getWidth() <= 0 || displayObj.getHeight() <= 0) {
            return;
        }

        final float oldX = bounds.x, oldY = bounds.y, oldW = bounds.width, oldH = bounds.height;
        bounds.set(oldX + displayObj.getLeft(), oldY + displayObj.getTop(), displayObj.getWidth(), displayObj.getHeight());

        if (transformCount != 0) {
            updateScreenPosForRotation(displayObj);
        } else {
            displayObj.screenPos.set(bounds);
        }

        Rectangle intersection = Utils.getIntersection(bounds, visibleBounds, tmpBounds);
        if (intersection != null) { //avoid drawing object if it's not within visible region
            final float backupX = visibleBounds.x, backupY = visibleBounds.y, backupW = visibleBounds.width, backupH = visibleBounds.height;
            visibleBounds.set(intersection.x, intersection.y, intersection.width, intersection.height);

            if (displayObj.getRotate90()) { //use top-right corner of bounds as pivot point
                startRotateTransform(displayObj.getWidth(), 0, -90);
                updateScreenPosForRotation(displayObj);
            } else if (displayObj.getRotate180()) { //use center of bounds as pivot point
                startRotateTransform(displayObj.getWidth() / 2, displayObj.getHeight() / 2, 180);
                //screen position won't change for this object from a 180 degree rotation
            }

            displayObj.draw(this);

            if (displayObj.getRotate90() || displayObj.getRotate180()) {
                endTransform();
            }

            visibleBounds.set(backupX, backupY, backupW, backupH);
        }

        bounds.set(oldX, oldY, oldW, oldH);
    }

    private void updateScreenPosForRotation(FDisplayObject displayObj) {
        tmp.set(bounds.x, regionHeight - bounds.y, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        float minX = tmp.x;
        float maxX = minX;
        float minY = tmp.y;
        float maxY = minY;
        tmp.set(bounds.x + bounds.width, regionHeight - bounds.y, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        if (tmp.x < minX) {
            minX = tmp.x;
        } else if (tmp.x > maxX) {
            maxX = tmp.x;
        }
        if (tmp.y < minY) {
            minY = tmp.y;
        } else if (tmp.y > maxY) {
            maxY = tmp.y;
        }
        tmp.set(bounds.x + bounds.width, regionHeight - bounds.y - bounds.height, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        if (tmp.x < minX) {
            minX = tmp.x;
        } else if (tmp.x > maxX) {
            maxX = tmp.x;
        }
        if (tmp.y < minY) {
            minY = tmp.y;
        } else if (tmp.y > maxY) {
            maxY = tmp.y;
        }
        tmp.set(bounds.x, regionHeight - bounds.y - bounds.height, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        if (tmp.x < minX) {
            minX = tmp.x;
        } else if (tmp.x > maxX) {
            maxX = tmp.x;
        }
        if (tmp.y < minY) {
            minY = tmp.y;
        } else if (tmp.y > maxY) {
            maxY = tmp.y;
        }

        displayObj.screenPos.set(minX, minY, maxX - minX, maxY - minY);
    }

    public void drawLine(float thickness, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
        drawLine(thickness, skinColor.getColor(), x1, y1, x2, y2);
    }

    public void drawLine(float thickness, Color color, float x1, float y1, float x2, float y2) {
        batch.end(); //must pause batch while rendering shapes

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        boolean needSmoothing = (x1 != x2 && y1 != y2);
        if (color.a < 1 || needSmoothing) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        if (needSmoothing) {
            Gdx.gl.glEnable(GL_LINE_SMOOTH);
        }

        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
        endShape();

        if (needSmoothing) {
            Gdx.gl.glDisable(GL_LINE_SMOOTH);
        }
        if (color.a < 1 || needSmoothing) {
            Gdx.gl.glDisable(GL_BLEND);
        }
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        batch.begin();
    }

    public void drawLinePointer(float arrowThickness, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
        drawLinePointer(arrowThickness, skinColor.getColor(), x1, y1, x2, y2);
    }

    public void drawLinePointer(float thickness, Color color, float x1, float y1, float x2, float y2) {
        if (arrowBatchActive) { // inside drawArrowBatch: emit only this pass's part
            if (alphaComposite < 1) {
                color = fade(color, fadeA);
            }
            boolean smooth = (x1 != x2 && y1 != y2);
            switch (arrowPhase) {
                case 0:
                    arrowNeedPhase2 = true;
                    shapeRenderer.setColor(color);
                    shapeRenderer.circle(adjustX(x2), adjustY(y2, 0), thickness);
                    shapeRenderer.setColor(Color.WHITE);
                    shapeRenderer.circle(adjustX(x2), adjustY(y2, 0), thickness / 2);
                    break;
                case 1:
                    setLineStateBatched(smooth, thickness);
                    shapeRenderer.setColor(color);
                    shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
                    break;
                default:
                    // same width the un-batched version ends up with for its white line
                    float lt = thickness / 3;
                    setLineStateBatched(smooth, lt > 1 ? lt : thickness);
                    shapeRenderer.setColor(Color.WHITE);
                    shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
                    break;
            }
            return;
        }
        boolean wasDrawing = batch.isDrawing(); // false inside a shape session, where the batch is already paused
        if (wasDrawing) {
            batch.end();
        } //must pause batch while rendering shapes
        float ct = thickness / 2;
        float lt = thickness / 3;

        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        boolean needSmoothing = (x1 != x2 && y1 != y2);
        if (color.a < 1 || needSmoothing) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        if (needSmoothing) {
            Gdx.gl.glEnable(GL_LINE_SMOOTH);
        }
        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(adjustX(x2), adjustY(y2, 0), thickness);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(adjustX(x2), adjustY(y2, 0), ct);
        endShape();

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
        endShape();

        if (lt > 1) {
            Gdx.gl.glLineWidth(lt);
        }
        startShape(ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
        endShape();

        if (needSmoothing) {
            Gdx.gl.glDisable(GL_LINE_SMOOTH);
        }
        if (color.a < 1 || needSmoothing) {
            Gdx.gl.glDisable(GL_BLEND);
        }
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        if (wasDrawing) {
            batch.begin();
        }
    }

    public void drawArrow(float borderThickness, float arrowThickness, float arrowSize, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
        drawArrow(borderThickness, arrowThickness, arrowSize, skinColor.getColor(), x1, y1, x2, y2);
    }

    public void drawArrow(float borderThickness, float arrowThickness, float arrowSize, Color color, float x1, float y1, float x2, float y2) {
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }

        vectorAngleHelper1.set(x2 - x1, y2 - y1);
        float angle = vectorAngleHelper1.angleRad();
        float perpRotation = (float)(Math.PI * 0.5f);
        float arrowHeadRotation = (float)(Math.PI * 0.8f);
        float arrowTipAngle = (float)(Math.PI - arrowHeadRotation);
        float halfThickness = arrowThickness / 2;

        int index = 0;

        vectorAngleHelper2.set(x2 + arrowSize * (float) Math.cos(angle + arrowHeadRotation), y2 + arrowSize * (float) Math.sin(angle + arrowHeadRotation));
        vectorAngleHelper3.set(x2 + arrowSize * (float) Math.cos(angle - arrowHeadRotation), y2 + arrowSize * (float) Math.sin(angle - arrowHeadRotation));

        float arrowCornerLen = (vectorAngleHelper2.dst(vectorAngleHelper3) - arrowThickness) / 2;
        float arrowHeadLen = arrowSize * (float) Math.cos(arrowTipAngle);

        index = addVertex(vectorAngleHelper2.x, vectorAngleHelper2.y, arrowVertices, index);
        index = addVertex(x2, y2, arrowVertices, index);
        index = addVertex(vectorAngleHelper3.x, vectorAngleHelper3.y, arrowVertices, index);
        index = addVertex(vectorAngleHelper3.x + arrowCornerLen * (float) Math.cos(angle + perpRotation), vectorAngleHelper3.y + arrowCornerLen * (float) Math.sin(angle + perpRotation), arrowVertices, index);
        index = addVertex(x1 + halfThickness * (float) Math.cos(angle - perpRotation), y1 + halfThickness * (float) Math.sin(angle - perpRotation), arrowVertices, index);
        index = addVertex(x1 + halfThickness * (float) Math.cos(angle + perpRotation), y1 + halfThickness * (float) Math.sin(angle + perpRotation), arrowVertices, index);
        index = addVertex(vectorAngleHelper2.x + arrowCornerLen * (float) Math.cos(angle - perpRotation), vectorAngleHelper2.y + arrowCornerLen * (float) Math.sin(angle - perpRotation), arrowVertices, index);

        // arrow tail endpoints (screen coordinates)
        arTx1 = adjustX(x1);
        arTy1 = adjustY(y1, 0);
        arTx2 = adjustX(x2 - arrowHeadLen * (float) Math.cos(angle));
        arTy2 = adjustY(y2 - arrowHeadLen * (float) Math.sin(angle), 0);

        if (arrowBatchActive) { // inside drawArrowBatch: emit only this pass's part
            if (arrowPhase == 0) {
                arrowFill(color, arrowThickness);
            } else if (arrowPhase == 1) {
                setLineStateBatched(true, borderThickness);
                arrowBorder();
            }
            return;
        }

        boolean wasDrawing = batch.isDrawing(); // false inside a shape session, where the batch is already paused
        if (wasDrawing) {
            batch.end(); // must pause batch while rendering shapes
        }
        Gdx.gl.glEnable(GL_BLEND);
        Gdx.gl.glEnable(GL_LINE_SMOOTH);

        // draw arrow tail and head
        startShape(ShapeType.Filled);
        arrowFill(color, arrowThickness);
        endShape();

        // draw border around arrow
        if (borderThickness > 1) {
            Gdx.gl.glLineWidth(borderThickness);
        }
        startShape(ShapeType.Line);
        arrowBorder();
        endShape();
        if (borderThickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        Gdx.gl.glDisable(GL_BLEND);
        Gdx.gl.glDisable(GL_LINE_SMOOTH);

        if (wasDrawing) {
            batch.begin();
        }
    }

    private void arrowFill(Color color, float arrowThickness) {
        shapeRenderer.setColor(color);
        shapeRenderer.rectLine(arTx1, arTy1, arTx2, arTy2, arrowThickness);
        shapeRenderer.triangle(arrowVertices[0], arrowVertices[1], arrowVertices[2], arrowVertices[3], arrowVertices[4], arrowVertices[5]);
    }

    private void arrowBorder() {
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.polygon(arrowVertices);
    }

    public void drawCurvedArrow(float thickness, Color fillColor, Color strokeColor, float x1, float y1, float x2, float y2, boolean drawPointer) {
        drawCurvedArrow(thickness, fillColor, strokeColor, x1, y1, x2, y2, drawPointer, 0f);
    }

    /**
     * @param bulge if non-zero, always draws a curve (even between close points) whose control point sits this far
     *              to the side of the midpoint; positive bulges to the left of the direction of travel on screen
     */
    public void drawCurvedArrow(float thickness, Color fillColor, Color strokeColor, float x1, float y1, float x2, float y2, boolean drawPointer, float bulge) {
        if (alphaComposite < 1) {
            fillColor = fade(fillColor, fadeA);
            strokeColor = fade(strokeColor, fadeB);
        }
        float lt = thickness / 3;
        boolean needSmoothing = (x1 != x2 && y1 != y2);
        prepCurved(thickness, x1, y1, x2, y2, !drawPointer, bulge);

        if (arrowBatchActive) { // inside drawArrowBatch: emit only this pass's part
            if (arrowPhase == 0) {
                curvedArrowCircles(fillColor, strokeColor, thickness, drawPointer);
                curvedBodyFill(fillColor, thickness);
                if (caHead) curvedHeadFill(fillColor, thickness);
            } else if (arrowPhase == 1) {
                setLineStateBatched(needSmoothing, lt);
                curvedBodyLine(strokeColor);
                if (caHead) curvedHeadLine(strokeColor);
            }
            return;
        }

        boolean wasDrawing = batch.isDrawing(); // false inside a shape session, where the batch is already paused
        if (wasDrawing) {
            batch.end();
        }
        boolean blend = fillColor.a < 1 || needSmoothing;
        if (blend) {
            Gdx.gl.glEnable(GL_BLEND);
        }
        // line smoothing/width only affect the Line passes, so set them once instead of once per stroke pass
        if (needSmoothing) Gdx.gl.glEnable(GL_LINE_SMOOTH);
        if (lt > 1) Gdx.gl.glLineWidth(lt);

        startShape(ShapeType.Filled);
        curvedArrowCircles(fillColor, strokeColor, thickness, drawPointer);
        curvedBodyFill(fillColor, thickness);
        endShape();

        startShape(ShapeType.Line);
        curvedBodyLine(strokeColor);
        endShape();

        if (caHead) {
            startShape(ShapeType.Filled);
            curvedHeadFill(fillColor, thickness);
            endShape();

            startShape(ShapeType.Line);
            curvedHeadLine(strokeColor);
            endShape();
        }

        if (needSmoothing) Gdx.gl.glDisable(GL_LINE_SMOOTH);
        if (lt > 1) Gdx.gl.glLineWidth(1);
        if (blend) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        if (wasDrawing) {
            batch.begin();
        }
    }

    public void drawCurvedLinePointer(float thickness, Color fillColor, Color strokeColor, float x1, float y1, float x2, float y2) {
        if (alphaComposite < 1) {
            fillColor = fade(fillColor, fadeA);
            strokeColor = fade(strokeColor, fadeB);
        }
        float lt = thickness / 3;
        boolean needSmoothing = (x1 != x2 && y1 != y2);
        prepCurved(thickness, x1, y1, x2, y2, false, 0f);

        if (arrowBatchActive) { // inside drawArrowBatch: emit only this pass's part
            if (arrowPhase == 0) {
                curvedPointerCircles(fillColor, strokeColor, thickness);
                curvedBodyFill(fillColor, thickness);
            } else if (arrowPhase == 1) {
                setLineStateBatched(needSmoothing, lt);
                curvedBodyLine(strokeColor);
            }
            return;
        }

        boolean wasDrawing = batch.isDrawing(); // false inside a shape session, where the batch is already paused
        if (wasDrawing) {
            batch.end();
        }
        boolean blend = fillColor.a < 1 || needSmoothing;
        if (blend) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        // line smoothing/width only affect the Line pass, so set them once
        if (needSmoothing) Gdx.gl.glEnable(GL_LINE_SMOOTH);
        if (lt > 1) Gdx.gl.glLineWidth(lt);

        startShape(ShapeType.Filled);
        curvedPointerCircles(fillColor, strokeColor, thickness);
        curvedBodyFill(fillColor, thickness);
        endShape();

        startShape(ShapeType.Line);
        curvedBodyLine(strokeColor);
        endShape();

        if (needSmoothing) Gdx.gl.glDisable(GL_LINE_SMOOTH);
        if (lt > 1) Gdx.gl.glLineWidth(1);
        if (blend) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        if (wasDrawing) {
            batch.begin();
        }
    }

    /**
     * Optional: pauses the sprite batch once so a run of consecutive shape draws (arrows, pointers, lines...) doesn't
     * pause/resume it for every single call. Nothing that uses the batch (images, text) may be drawn until
     * endShapeSession() is called. Always pair with try/finally:
     *   g.beginShapeSession();
     *   try { ...draw arrows... } finally { g.endShapeSession(); }
     */
    public void beginShapeSession() {
        if (batch.isDrawing()) {
            batch.end();
        }
    }

    public void endShapeSession() {
        if (!isDisposed && !batch.isDrawing()) {
            batch.begin();
        }
    }

    //quadratic bezier sampled into curvePts as screen coordinates (each point computed exactly once)
    private void buildCurve(float x1, float y1, float cx, float cy, float x2, float y2) {
        curvePts[0] = adjustX(x1);
        curvePts[1] = adjustY(y1, 0);
        for (int i = 1; i <= CURVE_SEGMENTS; i++) {
            float t = i / (float) CURVE_SEGMENTS;
            float bx = (1 - t) * (1 - t) * x1 + 2 * (1 - t) * t * cx + t * t * x2;
            float by = (1 - t) * (1 - t) * y1 + 2 * (1 - t) * t * cy + t * t * y2;
            curvePts[i * 2] = adjustX(bx);
            curvePts[i * 2 + 1] = adjustY(by, 0);
        }
    }

    /**
     * Draws a run of arrows/pointers (drawArrow, drawLinePointer, drawCurvedArrow, drawCurvedLinePointer) with only
     * 2-3 shape passes in total instead of 2-4 passes PER arrow. The runnable is executed once per pass and must only
     * draw arrows (no images/text, no state changes); it should be cheap and free of side effects because it runs 2-3 times.
     * Output differs from individual calls only where arrows overlap: all fills are drawn first, then all outlines.
     */
    public void drawArrowBatch(Runnable arrows) {
        if (arrowBatchActive) { // nested: just draw into the running pass
            arrows.run();
            return;
        }
        boolean wasDrawing = batch.isDrawing();
        if (wasDrawing) {
            batch.end(); //must pause batch while rendering shapes
        }
        arrowBatchActive = true;
        arrowNeedPhase2 = false;
        lineSmoothOn = false;
        lineWidthNow = 1;
        Gdx.gl.glEnable(GL_BLEND); // harmless for opaque colors, required for alpha and smoothed lines
        try {
            for (int phase = 0; phase <= 2; phase++) {
                if (phase == 2 && !arrowNeedPhase2) {
                    break;
                }
                arrowPhase = phase;
                startShape(phase == 0 ? ShapeType.Filled : ShapeType.Line);
                try {
                    arrows.run();
                } finally {
                    endShape();
                }
            }
        } finally {
            arrowBatchActive = false;
            if (lineSmoothOn) {
                Gdx.gl.glDisable(GL_LINE_SMOOTH);
                lineSmoothOn = false;
            }
            if (lineWidthNow != 1) {
                Gdx.gl.glLineWidth(1);
                lineWidthNow = 1;
            }
            Gdx.gl.glDisable(GL_BLEND);
            if (wasDrawing) {
                batch.begin();
            }
        }
    }

    //line smoothing/width can't change mid-pass, so pending lines are flushed first if the state differs
    private void setLineStateBatched(boolean smooth, float width) {
        float w = width > 1 ? width : 1;
        if (smooth == lineSmoothOn && w == lineWidthNow) {
            return;
        }
        shapeRenderer.flush();
        if (smooth != lineSmoothOn) {
            if (smooth) {
                Gdx.gl.glEnable(GL_LINE_SMOOTH);
            } else {
                Gdx.gl.glDisable(GL_LINE_SMOOTH);
            }
            lineSmoothOn = smooth;
        }
        if (w != lineWidthNow) {
            Gdx.gl.glLineWidth(w);
            lineWidthNow = w;
        }
    }

    //geometry of a curved arrow/pointer (straight when short), computed once into the ca* fields and curvePts
    private void prepCurved(float thickness, float x1, float y1, float x2, float y2, boolean wantHead, float bulge) {
        caSx1 = adjustX(x1);
        caSy1 = adjustY(y1, 0);
        caSx2 = adjustX(x2);
        caSy2 = adjustY(y2, 0);
        caHead = false;

        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        // Point just before the tip for direction
        float beforeTipX = x1, beforeTipY = y1;

        if (length < 120f && bulge == 0f) {
            // Straight line
            caCurved = false;
            float backScale = Math.max(0.1f, 10f / length);
            beforeTipX = x2 - dx * backScale;
            beforeTipY = y2 - dy * backScale;
        } else {
            // Curved Bezier
            caCurved = true;
            float midX = (x1 + x2) / 2f;
            float midY = (y1 + y2) / 2f;
            float px = -dy / length, py = dx / length;
            float curveStrength = bulge != 0f ? bulge : 50f;
            float cx = midX + px * curveStrength;
            float cy = midY + py * curveStrength;

            // Sample at t=0.95 for approach vector
            float tBefore = 0.95f;
            beforeTipX = (1 - tBefore) * (1 - tBefore) * x1 + 2 * (1 - tBefore) * tBefore * cx + tBefore * tBefore * x2;
            beforeTipY = (1 - tBefore) * (1 - tBefore) * y1 + 2 * (1 - tBefore) * tBefore * cy + tBefore * tBefore * y2;

            buildCurve(x1, y1, cx, cy, x2, y2);
        }

        if (wantHead) {
            // --- Arrowhead at (x2,y2) ---
            float tipX = caSx2;
            float tipY = caSy2;
            float adjBeforeX = adjustX(beforeTipX);
            float adjBeforeY = adjustY(beforeTipY, 0);

            float headingX = tipX - adjBeforeX;
            float headingY = tipY - adjBeforeY;
            float headingLen = (float) Math.sqrt(headingX * headingX + headingY * headingY);

            if (headingLen > 0) {
                float nx = headingX / headingLen;
                float ny = headingY / headingLen;

                float arrowLength = thickness * 2.2f;

                float leftDirX = nx * WING_COS_L - ny * WING_SIN_L;
                float leftDirY = nx * WING_SIN_L + ny * WING_COS_L;
                float rightDirX = nx * WING_COS_R - ny * WING_SIN_R;
                float rightDirY = nx * WING_SIN_R + ny * WING_COS_R;

                caLX = tipX + leftDirX * arrowLength;
                caLY = tipY + leftDirY * arrowLength;
                caRX = tipX + rightDirX * arrowLength;
                caRY = tipY + rightDirY * arrowLength;
                caHead = true;
            }
        }
    }

    // ---- emitters: each assumes the right shape pass is already open ----
    private void curvedArrowCircles(Color fillColor, Color strokeColor, float thickness, boolean drawPointer) {
        shapeRenderer.setColor(fillColor);
        shapeRenderer.circle(caSx1, caSy1, thickness);
        if (drawPointer)
            shapeRenderer.circle(caSx2, caSy2, thickness);
        shapeRenderer.setColor(strokeColor);
        shapeRenderer.circle(caSx1, caSy1, thickness / 2);
        if (drawPointer)
            shapeRenderer.circle(caSx2, caSy2, thickness / 2);
    }

    private void curvedPointerCircles(Color fillColor, Color strokeColor, float thickness) {
        shapeRenderer.setColor(fillColor);
        shapeRenderer.circle(caSx2, caSy2, thickness * 1.2f);
        shapeRenderer.setColor(strokeColor);
        shapeRenderer.circle(caSx2, caSy2, thickness / 2);
    }

    private void curvedBodyFill(Color fillColor, float thickness) {
        shapeRenderer.setColor(fillColor);
        if (caCurved) {
            for (int i = 1; i <= CURVE_SEGMENTS; i++) {
                shapeRenderer.rectLine(curvePts[i * 2 - 2], curvePts[i * 2 - 1], curvePts[i * 2], curvePts[i * 2 + 1], thickness);
            }
        } else {
            shapeRenderer.rectLine(caSx1, caSy1, caSx2, caSy2, thickness);
        }
    }

    private void curvedBodyLine(Color strokeColor) {
        shapeRenderer.setColor(strokeColor);
        if (caCurved) {
            for (int i = 1; i <= CURVE_SEGMENTS; i++) {
                shapeRenderer.line(curvePts[i * 2 - 2], curvePts[i * 2 - 1], curvePts[i * 2], curvePts[i * 2 + 1]);
            }
        } else {
            shapeRenderer.line(caSx1, caSy1, caSx2, caSy2);
        }
    }

    private void curvedHeadFill(Color fillColor, float thickness) {
        shapeRenderer.setColor(fillColor);
        shapeRenderer.rectLine(caSx2, caSy2, caLX, caLY, thickness);
        shapeRenderer.rectLine(caSx2, caSy2, caRX, caRY, thickness);
    }

    private void curvedHeadLine(Color strokeColor) {
        shapeRenderer.setColor(strokeColor);
        shapeRenderer.line(caSx2, caSy2, caLX, caLY);
        shapeRenderer.line(caSx2, caSy2, caRX, caRY);
    }

    private int addVertex(float x, float y, float[] vertices, int index) {
        vertices[index] = adjustX(x);
        vertices[index + 1] = adjustY(y, 0);
        return index + 2;
    }

    public void drawfillBorder(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
        batch.end(); //must pause batch while rendering shapes
        drawRoundRectShapes(thickness, color, x, y, w, h, cornerRadius);
        fillRoundRectShapes(color, x, y, w, h, cornerRadius);
        batch.begin();
    }

    public void drawRoundRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h, float cornerRadius) {
        drawRoundRect(thickness, skinColor.getColor(), x, y, w, h, cornerRadius);
    }

    public void drawRoundRect(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
        batch.end(); //must pause batch while rendering shapes
        drawRoundRectShapes(thickness, color, x, y, w, h, cornerRadius);
        batch.begin();
    }

    //shape work only: caller must have paused the batch and resumes it afterwards
    private void drawRoundRectShapes(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1 || cornerRadius > 0) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        if (cornerRadius > 0) {
            Gdx.gl.glEnable(GL_LINE_SMOOTH);
        }

        //adjust width/height so rectangle covers equivalent filled area
        w = Math.round(w + 1);
        h = Math.round(h + 1);

        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);

        shapeRenderer.arc(adjustX(x) + cornerRadius, adjustY(y + cornerRadius, 0), cornerRadius, 90f, 90f);
        shapeRenderer.arc(adjustX(x) + w - cornerRadius, adjustY(y + cornerRadius, 0), cornerRadius, 0f, 90f);
        shapeRenderer.arc(adjustX(x) + w - cornerRadius, adjustY(y + h - cornerRadius, 0), cornerRadius, 270, 90f);
        shapeRenderer.arc(adjustX(x) + cornerRadius, adjustY(y + h - cornerRadius, 0), cornerRadius, 180, 90f);
        //the pieces must not overlap: translucent colours would show the overlap as a brighter square
        shapeRenderer.rect(adjustX(x), adjustY(y + cornerRadius, h - cornerRadius * 2), cornerRadius, h - cornerRadius * 2);
        shapeRenderer.rect(adjustX(x + w - cornerRadius), adjustY(y + cornerRadius, h - cornerRadius * 2), cornerRadius, h - cornerRadius * 2);
        shapeRenderer.rect(adjustX(x + cornerRadius), adjustY(y, h), w - cornerRadius * 2, h);

        endShape();

        if (cornerRadius > 0) {
            Gdx.gl.glDisable(GL_LINE_SMOOTH);
        }
        if (color.a < 1 || cornerRadius > 0) {
            Gdx.gl.glDisable(GL_BLEND);
        }
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }
    }

    public void fillRoundRect(FSkinColor skinColor, float x, float y, float w, float h, float cornerRadius) {
        fillRoundRect(skinColor.getColor(), x, y, w, h, cornerRadius);
    }

    public void fillRoundRect(Color color, float x, float y, float w, float h, float cornerRadius) {
        batch.end(); //must pause batch while rendering shapes
        fillRoundRectShapes(color, x, y, w, h, cornerRadius);
        batch.begin();
    }

    //shape work only: caller must have paused the batch and resumes it afterwards
    private void fillRoundRectShapes(Color color, float x, float y, float w, float h, float cornerRadius) {
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.arc(adjustX(x) + cornerRadius, adjustY(y + cornerRadius, 0), cornerRadius, 90f, 90f);
        shapeRenderer.arc(adjustX(x) + w - cornerRadius, adjustY(y + cornerRadius, 0), cornerRadius, 0f, 90f);
        shapeRenderer.arc(adjustX(x) + w - cornerRadius, adjustY(y + h - cornerRadius, 0), cornerRadius, 270, 90f);
        shapeRenderer.arc(adjustX(x) + cornerRadius, adjustY(y + h - cornerRadius, 0), cornerRadius, 180, 90f);
        shapeRenderer.rect(adjustX(x), adjustY(y + cornerRadius, h - cornerRadius * 2), w, h - cornerRadius * 2);
        shapeRenderer.rect(adjustX(x + cornerRadius), adjustY(y, h), w - cornerRadius * 2, h);
        endShape();
        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }
    }

    // ---- rectangles through the SpriteBatch -------------------------------------------------------------------------
    // fillRect / fillGradientRect / drawRect / drawRectLines used to go through ShapeRenderer, which has to pause the batch for
    // every single call (flush, shader switch, shape pass, shader switch). A card frame drawn in code makes around ten of them
    // per card. They are now solid quads drawn through the batch with a 1x1 white texture: draw order is preserved, nothing
    // is paused, and consecutive rects merge into one draw call. Fills and gradients cover exactly the same pixels as before.
    // Outlines used to be GL lines (smoothed, centered on the edge); the quads cover the same footprint with hard edges.
    // FORCED ON by default. -Dforge.batchedRects=true turns it on; -Dforge.batchedOutlines=false then keeps outlines on ShapeRenderer.
    private static final boolean BATCHED_RECTS = true;// Boolean.parseBoolean(System.getProperty("forge.batchedRects", "false"));
    private static final boolean BATCHED_RECT_OUTLINES = true;// BATCHED_RECTS && !"false".equalsIgnoreCase(System.getProperty("forge.batchedOutlines"));
    // ONE shared 1x1 texture for the whole app instead of one per Graphics instance, so it can never pile up
    private static Pixmap whitePixmap;
    private static Texture whiteTexture;
    private final float[] quadVerts = new float[20];

    private Texture getWhiteTexture() {
        if (whiteTexture == null) {
            whitePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            whitePixmap.setColor(1f, 1f, 1f, 1f);
            whitePixmap.fill();
            // managed, so it is restored if the GL context is ever lost
            whiteTexture = new Texture(new PixmapTextureData(whitePixmap, null, false, false, true));
            whiteTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        return whiteTexture;
    }

    // One solid quad. (l, b) is the bottom-left corner in screen space; colors are packed floats per corner in the order
    // bottom-left, top-left, top-right, bottom-right. Vertex colors are used as they are (the batch color is not applied).
    private void batchQuad(float l, float b, float w, float h, float cBL, float cTL, float cTR, float cBR) {
        final float[] v = quadVerts;
        final float r = l + w, t = b + h;
        v[0] = l;  v[1] = b;  v[2] = cBL;  v[3] = 0.5f;  v[4] = 0.5f;
        v[5] = l;  v[6] = t;  v[7] = cTL;  v[8] = 0.5f;  v[9] = 0.5f;
        v[10] = r; v[11] = t; v[12] = cTR; v[13] = 0.5f; v[14] = 0.5f;
        v[15] = r; v[16] = b; v[17] = cBR; v[18] = 0.5f; v[19] = 0.5f;
        batch.draw(getWhiteTexture(), v, 0, 20);
    }

    public void drawRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h) {
        drawRect(thickness, skinColor.getColor(), x, y, w, h);
    }

    public void drawRect(float thickness, Color color, float x, float y, float w, float h) {
        if (BATCHED_RECT_OUTLINES && batch.isDrawing()) {
            if (alphaComposite < 1) {
                color = fade(color, fadeA);
            }
            final float t = Math.max(thickness, 1f); // GL lines were never thinner than 1
            final float half = t / 2f;
            final float c = color.toFloatBits();
            final float l = adjustX(x), b = adjustY(y, h);
            // the four edges, each centered on the rectangle's edge like the GL lines were
            batchQuad(l, b - half, w, t, c, c, c, c);
            batchQuad(l, b + h - half, w, t, c, c, c, c);
            batchQuad(l - half, b, t, h, c, c, c, c);
            batchQuad(l + w - half, b, t, h, c, c, c, c);
            return;
        }
        batch.end(); //must pause batch while rendering shapes

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        Gdx.gl.glEnable(GL_BLEND);
        Gdx.gl.glEnable(GL_LINE_SMOOTH); //must be smooth to ensure edges aren't missed

        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
        endShape();

        Gdx.gl.glDisable(GL_LINE_SMOOTH);
        Gdx.gl.glDisable(GL_BLEND);
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        batch.begin();
    }

    /**
     * Rectangle outline made of dashes, drawn inside the given rectangle. Changing {@code phase} (same units as the
     * other lengths) slides the dashes along the outline, which gives "marching ants" when it grows over time.
     * Built from filled rects, so it batches like every other rect and follows active transforms.
     */
    public void drawDashedRect(float thickness, Color color, float x, float y, float w, float h, float dash, float gap, float phase) {
        final float period = dash + gap;
        if (dash <= 0 || period <= 0 || w <= 0 || h <= 0) {
            return;
        }
        final float t = Math.min(Math.max(thickness, 1f), Math.min(w, h) / 2f);
        final float perimeter = 2 * (w + h);
        // start one period early so a dash that straddles the starting corner is still drawn
        final float offset = ((phase % period) + period) % period;
        for (float s = offset - period; s < perimeter; s += period) {
            final float a = Math.max(s, 0f), b = Math.min(s + dash, perimeter);
            if (b > a) {
                dashSegment(color, t, x, y, w, h, a, b);
            }
        }
    }

    /**
     * Outline of equal dashes that alternate between two colours: the same picture as two {@link #drawDashedRect} calls
     * half a pattern apart. When opaque, the first colour is drawn as one solid frame and only the second colour's dashes
     * go over it, which halves the rects drawn (this runs for every selectable card, every frame). Translucent drawing
     * keeps the two passes so the colours do not blend where they would overlap.
     */
    public void drawAlternatingDashedRect(float thickness, Color colorA, Color colorB, float x, float y, float w, float h, float dash, float phase) {
        if (alphaComposite < 1) {
            drawDashedRect(thickness, colorA, x, y, w, h, dash, dash, phase);
            drawDashedRect(thickness, colorB, x, y, w, h, dash, dash, phase + dash);
            return;
        }
        if (dash <= 0 || w <= 0 || h <= 0) {
            return;
        }
        final float t = Math.min(Math.max(thickness, 1f), Math.min(w, h) / 2f);
        fillRect(colorA, x, y, w, t); // top
        fillRect(colorA, x, y + h - t, w, t); // bottom
        fillRect(colorA, x, y + t, t, h - 2 * t); // left
        fillRect(colorA, x + w - t, y + t, t, h - 2 * t); // right
        drawDashedRect(thickness, colorB, x, y, w, h, dash, dash, phase + dash);
    }

    // draws the part of an outline (clockwise from the top left corner) between distances a and b, split per edge
    private void dashSegment(Color color, float t, float x, float y, float w, float h, float a, float b) {
        float edgeStart = 0;
        for (int edge = 0; edge < 4; edge++) {
            if (edgeStart >= b) {
                break; // the rest of the outline lies beyond this dash
            }
            final float len = edge % 2 == 0 ? w : h;
            final float lo = Math.max(a, edgeStart), hi = Math.min(b, edgeStart + len);
            if (hi > lo) {
                final float u0 = lo - edgeStart, u1 = hi - edgeStart;
                switch (edge) {
                    case 0 -> fillRect(color, x + u0, y, u1 - u0, t); // top, left to right
                    case 1 -> fillRect(color, x + w - t, y + u0, t, u1 - u0); // right, downwards
                    case 2 -> fillRect(color, x + w - u1, y + h - t, u1 - u0, t); // bottom, right to left
                    default -> fillRect(color, x, y + h - u1, t, u1 - u0); // left, upwards
                }
            }
            edgeStart += len;
        }
    }

    public void drawRectLines(float thickness, Color color, float x, float y, float w, float h) {
        if (BATCHED_RECT_OUTLINES && batch.isDrawing()) {
            if (alphaComposite < 1) {
                color = fade(color, fadeA);
            }
            final float t = Math.max(thickness, 1f);
            final float half = thickness / 2f; // the edges were inset by thickness / 2, as in the line version below
            final float qh = t / 2f;
            final float c = color.toFloatBits();
            final float x0 = adjustX(x), x1 = adjustX(x + w);
            final float yTop = adjustY(y, 0), yBottom = adjustY(y + h, 0);
            batchQuad(x0, yTop - qh, x1 - x0, t, c, c, c, c);
            batchQuad(x0, yBottom - qh, x1 - x0, t, c, c, c, c);
            final float lx = adjustX(x + half), rx = adjustX(x + w - half);
            final float yv0 = adjustY(y + h - half, 0), yv1 = adjustY(y + half, 0);
            batchQuad(lx - qh, yv0, t, yv1 - yv0, c, c, c, c);
            batchQuad(rx - qh, yv0, t, yv1 - yv0, c, c, c, c);
            return;
        }
        // all four edges are axis aligned (no smoothing needed) so they share one batch pause and one shape pass
        batch.end(); //must pause batch while rendering shapes

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        float half = thickness / 2f;
        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.line(adjustX(x), adjustY(y, 0), adjustX(x + w), adjustY(y, 0));
        shapeRenderer.line(adjustX(x + half), adjustY(y + half, 0), adjustX(x + half), adjustY(y + h - half, 0));
        shapeRenderer.line(adjustX(x), adjustY(y + h, 0), adjustX(x + w), adjustY(y + h, 0));
        shapeRenderer.line(adjustX(x + w - half), adjustY(y + half, 0), adjustX(x + w - half), adjustY(y + h - half, 0));
        endShape();

        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        batch.begin();
    }

    public void fillRect(FSkinColor skinColor, float x, float y, float w, float h) {
        fillRect(skinColor.getColor(), x, y, w, h);
    }

    public void fillRect(Color color, float x, float y, float w, float h) {
        if (BATCHED_RECTS && batch.isDrawing()) {
            if (alphaComposite < 1) {
                color = fade(color, fadeA);
            }
            final float c = color.toFloatBits();
            batchQuad(adjustX(x), adjustY(y, h), w, h, c, c, c, c);
            return;
        }
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
        endShape();

        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        batch.begin();
    }

    public void drawCircle(float thickness, FSkinColor skinColor, float x, float y, float radius) {
        drawCircle(thickness, skinColor.getColor(), x, y, radius);
    }

    public void drawCircle(float thickness, Color color, float x, float y, float radius) {
        batch.end(); //must pause batch while rendering shapes

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        Gdx.gl.glEnable(GL_BLEND);
        Gdx.gl.glEnable(GL_LINE_SMOOTH);

        startShape(ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius);
        endShape();

        Gdx.gl.glDisable(GL_LINE_SMOOTH);
        Gdx.gl.glDisable(GL_BLEND);
        if (thickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        batch.begin();
    }

    public void fillCircle(FSkinColor skinColor, float x, float y, float radius) {
        fillCircle(skinColor.getColor(), x, y, radius);
    }

    public void fillCircle(Color color, float x, float y, float radius) {
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius); //TODO: Make smoother
        endShape();

        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        batch.begin();
    }

    public void fillTriangle(FSkinColor skinColor, float x1, float y1, float x2, float y2, float x3, float y3) {
        fillTriangle(skinColor.getColor(), x1, y1, x2, y2, x3, y3);
    }

    public void fillTriangle(Color color, float x1, float y1, float x2, float y2, float x3, float y3) {
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color = fade(color, fadeA);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.triangle(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0), adjustX(x3), adjustY(y3, 0));
        endShape();

        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        batch.begin();
    }

    public void fillGradientRect(FSkinColor skinColor1, FSkinColor skinColor2, boolean vertical, float x, float y, float w, float h) {
        fillGradientRect(skinColor1.getColor(), skinColor2.getColor(), vertical, x, y, w, h);
    }

    public void fillGradientRect(FSkinColor skinColor1, Color color2, boolean vertical, float x, float y, float w, float h) {
        fillGradientRect(skinColor1.getColor(), color2, vertical, x, y, w, h);
    }

    public void fillGradientRect(Color color1, FSkinColor skinColor2, boolean vertical, float x, float y, float w, float h) {
        fillGradientRect(color1, skinColor2.getColor(), vertical, x, y, w, h);
    }

    public void fillGradientRect(Color color1, Color color2, boolean vertical, float x, float y, float w, float h) {
        if (BATCHED_RECTS && batch.isDrawing()) {
            if (alphaComposite < 1) {
                color1 = fade(color1, fadeA);
                color2 = fade(color2, fadeB);
            }
            final float c1 = color1.toFloatBits(), c2 = color2.toFloatBits();
            // same corner colors as the ShapeRenderer version below: bottom-left, top-left, top-right, bottom-right
            batchQuad(adjustX(x), adjustY(y, h), w, h, vertical ? c2 : c1, c1, vertical ? c1 : c2, c2);
            return;
        }
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color1 = fade(color1, fadeA);
            color2 = fade(color2, fadeB);
        }
        boolean needBlending = (color1.a < 1 || color2.a < 1);
        if (needBlending) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        Color topLeftColor = color1;
        Color topRightColor = vertical ? color1 : color2;
        Color bottomLeftColor = vertical ? color2 : color1;
        Color bottomRightColor = color2;

        startShape(ShapeType.Filled);
        shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h, bottomLeftColor, bottomRightColor, topRightColor, topLeftColor);
        endShape();

        if (needBlending) {
            Gdx.gl.glDisable(GL_BLEND);
        }

        batch.begin();
    }

    private void startShape(ShapeType shapeType) {
        if (transformCount != 0) {
            //must copy matrix before starting shape if transformed
            shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
        }
        shapeRenderer.begin(shapeType);
    }

    private void endShape() {
        shapeRenderer.end();
    }

    public void setColorRGBA(float r, float g, float b, float alphaComposite0) {
        alphaComposite = alphaComposite0;
        batch.setColor(r, g, b, alphaComposite);
    }

    public void resetColorRGBA(float alphaComposite0) {
        alphaComposite = alphaComposite0;
        batch.setColor(Color.WHITE);
    }

    public void setAlphaComposite(float alphaComposite0) {
        alphaComposite = alphaComposite0;
        batch.setColor(1f, 1f, 1f, alphaComposite);
    }

    public void resetAlphaComposite() {
        alphaComposite = 1;
        batch.setColor(Color.WHITE);
    }

    public float getfloatAlphaComposite() {
        return alphaComposite;
    }

    public void drawBorderImage(FImage image, Color borderColor, Color tintColor, float x, float y, float w, float h, boolean tint) {
        float oldalpha = alphaComposite;
        if (tint && !tintColor.equals(borderColor)) {
            batch.end(); //must pause batch while rendering shapes
            drawRoundRectShapes(2f, borderLiningFor(borderColor), x, y, w, h, (h - w) / 12);
            fillRoundRectShapes(tintColor, x, y, w, h, (h - w) / 12);
            batch.begin();
        } else {
            if (image != null) {
                image.draw(this, x, y, w, h);
                fillRoundRect(borderColor, x, y, w, h, (h - w) / 10);//show corners edges
            }
        }
        setAlphaComposite(oldalpha);
    }

    public void drawborderImage(Color borderColor, float x, float y, float w, float h) {
        float oldalpha = alphaComposite;
        fillRoundRect(borderColor, x, y, w, h, (h - w) / 12);
        setAlphaComposite(oldalpha);
    }

    public void drawImage(FImage image, Color borderColor, float x, float y, float w, float h) {
        if (image == null)
            return;
        image.draw(this, x, y, w, h);
        fillRoundRect(borderColor, x + 1, y + 1, w - 1.5f, h - 1.5f, (h - w) / 10);//used by zoom let some edges show...
    }

    public void drawAvatarImage(FImage image, float x, float y, float w, float h, boolean drawGrayscale, float amount) {
        if (image == null)
            return;
        if (amount > 0) {
            batch.flush();
            ShaderUtil.getInstance().getShaderWarp().bind();
            ShaderUtil.getInstance().getShaderWarp().setUniformf("u_amount", 0.2f);
            ShaderUtil.getInstance().getShaderWarp().setUniformf("u_speed", 0.2f);
            ShaderUtil.getInstance().getShaderWarp().setUniformf("u_time", amount);
            batch.setShader(ShaderUtil.getInstance().getShaderWarp());
            //draw
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        } else if (!drawGrayscale) {
            image.draw(this, x, y, w, h);
        } else {
            batch.flush();
            ShaderUtil.getInstance().getShaderGrayscale().bind();
            ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", 1f);
            ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", 1f);
            batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
            //draw gray
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        }
    }

    public void drawCardImage(FImage image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged) {
        if (image == null)
            return;
        if (!drawGrayscale) {
            image.draw(this, x, y, w, h);
            if (damage_overlay != null && damaged)
                batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
        } else {
            batch.flush();
            ShaderUtil.getInstance().getShaderGrayscale().bind();
            ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", 1f);
            ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", 0.8f);
            batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
            //draw gray
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        }
    }

    public void drawCardImage(Texture image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged, int foilIndex) {
        if (image == null)
            return;
        if (image != null) {
            if (!drawGrayscale) {
                if (foilIndex > 0) {
                    batch.flush();
                    ShaderProgram shaderProgram = ShaderUtil.getInstance().getShaderCardRoundedHolo();
                    shaderProgram.bind();
                    shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
                    shaderProgram.setUniformf("edge_radius", 0);
                    shaderProgram.setUniformf("u_time", 0);
                    shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
                    shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
                    batch.setShader(shaderProgram);
                    batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                    batch.setShader(null);
                } else {
                    batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                }
                if (damage_overlay != null && damaged)
                    batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
            } else {
                batch.flush();
                ShaderUtil.getInstance().getShaderGrayscale().bind();
                ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", 1f);
                ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", 0.8f);
                batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
                //draw gray
                batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                batch.setShader(null);
            }
        }
    }

    public void drawCardImage(TextureRegion image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged, int foilIndex) {
        if (image != null) {
            if (!drawGrayscale) {
                if (foilIndex > 0) {
                    batch.flush();
                    ShaderProgram shaderProgram = ShaderUtil.getInstance().getShaderCardRoundedHolo();
                    shaderProgram.bind();
                    shaderProgram.setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
                    shaderProgram.setUniformf("edge_radius", 0);
                    shaderProgram.setUniformf("u_time", 0);
                    shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
                    shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
                    batch.setShader(shaderProgram);
                    batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                    batch.setShader(null);
                } else {
                    batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                }
                if (damage_overlay != null && damaged)
                    batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
            } else {
                batch.flush();
                ShaderUtil.getInstance().getShaderGrayscale().bind();
                ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", 1f);
                ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", 0.8f);
                batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
                //draw gray
                batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                batch.setShader(null);
            }
        }
    }

    public void drawGrayTransitionImage(FImage image, float x, float y, float w, float h, float percentage) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderGrayscale().bind();
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", percentage);
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", 0.6f);
        batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
        //draw gray
        image.draw(this, x, y, w, h);
        batch.setShader(null);
    }

    public void drawGrayTransitionImage(Texture image, float x, float y, float w, float h, boolean withDarkOverlay, float percentage) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderGrayscale().bind();
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", percentage);
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", withDarkOverlay ? 0.5f : 1f);
        batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
        //draw gray
        batch.draw(image, x, y, w, h);
        batch.setShader(null);
    }

    public void drawGrayTransitionImage(TextureRegion image, float x, float y, float w, float h, boolean withDarkOverlay, float percentage) {
        batch.flush();
        ShaderUtil.getInstance().getShaderGrayscale().bind();
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_grayness", percentage);
        ShaderUtil.getInstance().getShaderGrayscale().setUniformf("u_bias", withDarkOverlay ? 0.5f : 1f);
        batch.setShader(ShaderUtil.getInstance().getShaderGrayscale());
        //draw gray
        batch.draw(image, x, y, w, h);
        batch.setShader(null);
    }

    public void drawCardRoundRect(Texture image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGray, boolean damaged, int foilIndex) {
        if (image == null)
            return;
        float radius = ImageCache.getInstance().getRadius(image);
        batch.flush();
        boolean shouldApplyHolo = foilIndex > 0 && !drawGray;
        float edgeRadius = (float)(image.getHeight() / image.getWidth()) * radius;
        ShaderProgram shaderProgram = shouldApplyHolo ? ShaderUtil.getInstance().getShaderCardRoundedHolo() : ShaderUtil.getInstance().getShaderCardRounded();
        if (shouldApplyHolo) {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_time", 0);
            shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
            shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
        } else {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_gray", drawGray ? 0.8f : 0f);
        }
        batch.setShader(shaderProgram);
        //draw
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        batch.setShader(null);
        if (damage_overlay != null && damaged)
            batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
    }

    public void drawCardRoundRect(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation) {
        drawCardRoundRect(image, x, y, w, h, originX, originY, rotation, 1f, 0);
    }

    public void drawCardRoundRect(TextureRegion image, float x, float y, float w, float h, float originX, float originY, float rotation, float modR, int foilIndex) {
        if (image == null)
            return;
        batch.flush();
        boolean shouldApplyHolo = foilIndex > 0;
        float edgeRadius = ((float)(image.getRegionHeight() / image.getRegionWidth()) * (ImageCache.getInstance().getRadius(image.getTexture()) * modR));
        ShaderProgram shaderProgram = shouldApplyHolo ? ShaderUtil.getInstance().getShaderCardRoundedHolo() : ShaderUtil.getInstance().getShaderCardRounded();
        if (shouldApplyHolo) {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_time", 0);
            shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
            shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
        } else {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_gray", 0f);
        }
        batch.setShader(shaderProgram);
        //draw
        drawRotatedImage(image, x, y, w, h, originX, originY, rotation);
        batch.setShader(null);
    }

    public void drawCardRoundRect(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation, float modR, int foilIndex) {
        if (image == null)
            return;
        batch.flush();
        boolean shouldApplyHolo = foilIndex > 0;
        float edgeRadius = ((float)(image.getHeight() / image.getWidth()) * (ImageCache.getInstance().getRadius(image) * modR));
        ShaderProgram shaderProgram = shouldApplyHolo ? ShaderUtil.getInstance().getShaderCardRoundedHolo() : ShaderUtil.getInstance().getShaderCardRounded();
        if (shouldApplyHolo) {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_time", 0);
            shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
            shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
        } else {
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
            shaderProgram.setUniformf("edge_radius", edgeRadius);
            shaderProgram.setUniformf("u_gray", 0f);
        }
        batch.setShader(shaderProgram);
        //draw
        drawRotatedImage(image, x, y, w, h, originX, originY, 0, 0, image.getWidth(), image.getHeight(), rotation);
        batch.setShader(null);
    }

    public void drawNoiseFade(TextureRegion image, float x, float y, float w, float h, Float time) {
        if (image == null)
            return;
        if (time != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderNoiseFade().bind();
            ShaderUtil.getInstance().getShaderNoiseFade().setUniformf("u_time", time);
            batch.setShader(ShaderUtil.getInstance().getShaderNoiseFade());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawPortalFade(TextureRegion image, float x, float y, float w, float h, Float time, boolean opaque) {
        if (image == null)
            return;
        if (time != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderPortal().bind();
            ShaderUtil.getInstance().getShaderPortal().setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
            ShaderUtil.getInstance().getShaderPortal().setUniformf("u_time", time);
            ShaderUtil.getInstance().getShaderPortal().setUniformf("u_opaque", opaque ? 1f : 0f);
            batch.setShader(ShaderUtil.getInstance().getShaderPortal());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawHueShift(Texture image, float x, float y, float w, float h, Float time) {
        if (image == null)
            return;
        if (time != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderHueShift().bind();
            ShaderUtil.getInstance().getShaderHueShift().setUniformf("u_time", time);
            batch.setShader(ShaderUtil.getInstance().getShaderHueShift());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawHueShift(TextureRegion image, float x, float y, float w, float h, Float time) {
        if (image == null)
            return;
        if (time != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderHueShift().bind();
            ShaderUtil.getInstance().getShaderHueShift().setUniformf("u_time", time);
            batch.setShader(ShaderUtil.getInstance().getShaderHueShift());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawChromatic(TextureRegion image, float x, float y, float w, float h, Float time) {
        if (image == null)
            return;
        if (time != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderChromaticAberration().bind();
            ShaderUtil.getInstance().getShaderChromaticAberration().setUniformf("u_time", time);
            batch.setShader(ShaderUtil.getInstance().getShaderChromaticAberration());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawRipple(FImage image, float x, float y, float w, float h, Float amount) {
        if (image == null)
            return;
        if (amount != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderRipple().bind();
            ShaderUtil.getInstance().getShaderRipple().setUniformf("u_time", amount);
            ShaderUtil.getInstance().getShaderRipple().setUniformf("u_bias", 0.7f);
            batch.setShader(ShaderUtil.getInstance().getShaderRipple());
            //draw
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        } else {
            drawImage(image, x, y, w, h);
        }
    }

    public void drawPix(TextureRegion texture, float x, float y, float w, float h) {
        if (texture == null)
            return;
        batch.flush();
        float mul = 3f;
        float pixelSize = w > h ? (w / h) * mul : (h / w) * mul;
        ShaderUtil.getInstance().getShaderPix().bind();
        ShaderUtil.getInstance().getShaderPix().setUniformf("u_resolution", w, h);
        ShaderUtil.getInstance().getShaderPix().setUniformf("u_pixelSize", pixelSize);
        ShaderUtil.getInstance().getShaderPix().setUniformf("u_bias", 0.8f);
        batch.setShader(ShaderUtil.getInstance().getShaderPix());
        batch.draw(texture, x, y, w, h);
        batch.setShader(null);
    }
    public void drawPixelated(FImage image, float x, float y, float w, float h, Float amount, boolean flipY) {
        if (image == null)
            return;
        if (amount != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderPixelate().bind();
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_resolution", Forge.isLandscapeMode() ? w : h, Forge.isLandscapeMode() ? h : w);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_cellSize", amount);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_yflip", flipY ? 1f : 0f);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_bias", 0.7f);
            batch.setShader(ShaderUtil.getInstance().getShaderPixelate());
            //draw
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        } else {
            drawImage(image, x, y, w, h);
        }
    }

    public void drawPixelated(TextureRegion image, float x, float y, float w, float h, Float amount, boolean flipY) {
        if (image == null)
            return;
        if (amount != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderPixelate().bind();
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_resolution", Forge.isLandscapeMode() ? w : h, Forge.isLandscapeMode() ? h : w);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_cellSize", amount);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_yflip", flipY ? 1 : 0);
            ShaderUtil.getInstance().getShaderPixelate().setUniformf("u_bias", 0.6f);
            batch.setShader(ShaderUtil.getInstance().getShaderPixelate());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawPixelatedWarp(TextureRegion image, float x, float y, float w, float h, float amount) {
        if (image == null)
            return;
        if (amount > 0) {
            batch.flush();
            ShaderUtil.getInstance().getShaderPixelateWarp().bind();
            ShaderUtil.getInstance().getShaderPixelateWarp().setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
            ShaderUtil.getInstance().getShaderPixelateWarp().setUniformf("u_cellSize", amount);
            ShaderUtil.getInstance().getShaderPixelateWarp().setUniformf("u_amount", 0.2f * amount);
            ShaderUtil.getInstance().getShaderPixelateWarp().setUniformf("u_speed", 0.5f);
            ShaderUtil.getInstance().getShaderPixelateWarp().setUniformf("u_time", 0.8f);
            batch.setShader(ShaderUtil.getInstance().getShaderPixelateWarp());
            //draw
            batch.draw(image, x, y, w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawWarpImage(Texture image, float x, float y, float w, float h, float time) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderWarp().bind();
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_amount", 0.2f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_speed", 0.5f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_time", time);
        batch.setShader(ShaderUtil.getInstance().getShaderWarp());
        //draw
        batch.draw(image, x, y, w, h);
        batch.setShader(null);
    }

    public void drawWarpImage(TextureRegion image, float x, float y, float w, float h, float time) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderWarp().bind();
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_amount", 0.2f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_speed", 0.6f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_time", time);
        batch.setShader(ShaderUtil.getInstance().getShaderWarp());
        //draw
        batch.draw(image, x, y, w, h);
        batch.setShader(null);
    }

    public void drawWarpImage(FImage image, float x, float y, float w, float h, float time) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderWarp().bind();
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_amount", 0.2f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_speed", 0.6f);
        ShaderUtil.getInstance().getShaderWarp().setUniformf("u_time", time);
        batch.setShader(ShaderUtil.getInstance().getShaderWarp());
        //draw
        image.draw(this, x, y, w, h);
        batch.setShader(null);
    }

    public void drawUnderWaterImage(FImage image, float x, float y, float w, float h, float time) {
        if (image == null)
            return;
        batch.flush();
        ShaderUtil.getInstance().getShaderUnderwater().bind();
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_amount", 10f * time);
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_speed", 0.5f * time);
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_time", time);
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_bias", 0.7f);
        batch.setShader(ShaderUtil.getInstance().getShaderUnderwater());
        //draw
        image.draw(this, x, y, w, h);
        batch.setShader(null);
    }

    public void drawNightDay(FImage image, float x, float y, float w, float h, Float timeOfDay, boolean darkOverlay, float rippleAmount) {
        if (image == null)
            return;
        if (timeOfDay != null) {
            batch.flush();
            ShaderUtil.getInstance().getShaderNightDay().bind();
            ShaderUtil.getInstance().getShaderNightDay().setUniformf("u_timeOfDay", timeOfDay);
            ShaderUtil.getInstance().getShaderNightDay().setUniformf("u_time", rippleAmount);
            ShaderUtil.getInstance().getShaderNightDay().setUniformf("u_bias",  darkOverlay? 0.7f : 1f);
            batch.setShader(ShaderUtil.getInstance().getShaderNightDay());
            //draw
            image.draw(this, x, y, w, h);
            batch.setShader(null);
        } else {
            drawImage(image, x, y, w, h);
        }
    }

    public void drawUnderWaterImage(TextureRegion image, float x, float y, float w, float h, float time) {
        batch.flush();
        ShaderUtil.getInstance().getShaderUnderwater().bind();
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_amount", 10f);
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_speed", 0.5f);
        ShaderUtil.getInstance().getShaderUnderwater().setUniformf("u_time", time);
        batch.setShader(ShaderUtil.getInstance().getShaderUnderwater());
        //draw
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        batch.setShader(null);
    }

    public void drawImage(Texture image, float x, float y, float w, float h) {
        if (image == null)
            return;
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
    }

    public void drawImage(TextureRegion image, float x, float y, float w, float h) {
        if (image == null)
            return;
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
    }
    public void drawImage(TextureRegion image, float x, float y, float w, float h, int foilIndex) {
        if (image == null)
            return;
        if (foilIndex > 0) {
            batch.flush();
            ShaderProgram shaderProgram = ShaderUtil.getInstance().getShaderCardRoundedHolo();
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getRegionWidth(), image.getRegionHeight());
            shaderProgram.setUniformf("edge_radius", 0);
            shaderProgram.setUniformf("u_time", 0);
            shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
            shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
            batch.setShader(shaderProgram);
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }

    }
    public void drawImage(Texture image, float x, float y, float w, float h, int foilIndex) {
        if (image == null)
            return;
        if (foilIndex > 0) {
            batch.flush();
            ShaderProgram shaderProgram = ShaderUtil.getInstance().getShaderCardRoundedHolo();
            shaderProgram.bind();
            shaderProgram.setUniformf("u_resolution", image.getWidth(), image.getHeight());
            shaderProgram.setUniformf("edge_radius", 0);
            shaderProgram.setUniformf("u_time", 0);
            shaderProgram.setUniformf("u_foilTilt", 0, foilIndex);
            shaderProgram.setUniformf("u_cardPosition", foilIndex, 0);
            batch.setShader(shaderProgram);
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
            batch.setShader(null);
        } else {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawImage(FImage image, float x, float y, float w, float h) {
        drawImage(image, x, y, w, h, false);
    }

    public void drawImage(FImage image, float x, float y, float w, float h, boolean withDarkOverlay) {
        if (image == null)
            return;
        image.draw(this, x, y, w, h);
        if (withDarkOverlay) {
            float oldalpha = alphaComposite;
            setAlphaComposite(0.4f);
            fillRect(Color.BLACK, x, y, w, h);
            setAlphaComposite(oldalpha);
        }
    }

    public void drawImage(TextureRegion image, TextureRegion glowImageReference, float x, float y, float w, float h, Color glowColor, boolean selected) {
        if (image == null || glowImageReference == null)
            return;
        //1st image is the image on top of the shader, 2nd image is for the outline reference for the shader glow...
        // if the 1st image don't have transparency in the middle (only on the sides, top and bottom, use the 1st image as outline reference...
        if (!selected) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        } else {
            batch.flush();
            ShaderUtil.getInstance().getShaderOutline().bind();
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_viewportInverse", 1f / w, 1f / h);
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_offset", 3f);
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_step", Math.min(1f, w / 70f));
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_color", glowColor.r, glowColor.g, glowColor.b);
            batch.setShader(ShaderUtil.getInstance().getShaderOutline());
            //glow
            batch.draw(glowImageReference, adjustX(x), adjustY(y, h), w, h);
            batch.setShader(null);
            //img
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }

    public void drawDeckBox(FImage cardArt, float scale, TextureRegion image, TextureRegion glowImageReference, float x, float y, float w, float h, Color glowColor, boolean selected) {
        if (image == null || glowImageReference == null)
            return;
        float yBox = y - (h * 0.25f);
        if (!selected) {
            cardArt.draw(this, x + ((w - w * scale) / 2), y + ((h - h * scale) / 3f), w * scale, h * scale / 1.85f);
            batch.draw(image, adjustX(x), adjustY(yBox, h), w, h);
        } else {
            batch.flush();
            ShaderUtil.getInstance().getShaderOutline().bind();
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_viewportInverse", 1f / w, 1f / h);
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_offset", 3f);
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_step", Math.min(1f, w / 70f));
            ShaderUtil.getInstance().getShaderOutline().setUniformf("u_color", glowColor.r, glowColor.g, glowColor.b);
            batch.setShader(ShaderUtil.getInstance().getShaderOutline());
            //glow
            batch.draw(glowImageReference, adjustX(x), adjustY(yBox, h), w, h);
            batch.setShader(null);
            //cardart
            cardArt.draw(this, x + ((w - w * scale) / 2), y + ((h - h * scale) / 3f), w * scale, h * scale / 1.85f);
            //deckbox
            batch.draw(image, adjustX(x), adjustY(yBox, h), w, h);
        }
    }

    public void drawRepeatingImage(Texture image, float x, float y, float w, float h) {
        if (image == null)
            return;
        if (startClip(x, y, w, h)) { //only render if clip successful, otherwise it will escape bounds
            int tilesW = (int) (w / image.getWidth()) + 1;
            int tilesH = (int) (h / image.getHeight()) + 1;
            batch.draw(image, adjustX(x), adjustY(y, h),
                    image.getWidth() * tilesW,
                    image.getHeight() * tilesH,
                    0, tilesH, tilesW, 0);
        }
        endClip();
    }

    //draw vertically flipped image
    public void drawFlippedImage(Texture image, float x, float y, float w, float h) {
        if (image == null)
            return;
        batch.draw(image, adjustX(x), adjustY(y, h), w, h, 0, 0, image.getWidth(), image.getHeight(), false, true);
    }

    public void drawImageWithTransforms(TextureRegion image, float x, float y, float w, float h, float rotation, boolean flipX, boolean flipY) {
        if (image == null)
            return;
        float originX = x + w / 2;
        float originY = y + h / 2;
        batch.draw(image.getTexture(), adjustX(x), adjustY(y, h), originX - x, h - (originY - y), w, h, 1, 1, rotation, image.getRegionX(), image.getRegionY(), image.getRegionWidth(), image.getRegionHeight(), flipX, flipY);
    }

    public void setProjectionMatrix(Matrix4 matrix) {
        batch.setProjectionMatrix(matrix);
        shapeRenderer.setProjectionMatrix(matrix);
    }

    public void startRotateTransform(float originX, float originY, float rotation) {
        batch.end();
        transformCount++;
        batch.getTransformMatrix().idt().translate(adjustX(originX), adjustY(originY, 0), 0).rotate(Vector3.Z, rotation).translate(-adjustX(originX), -adjustY(originY, 0), 0);
        batch.begin();
    }

    public void endTransform() {
        batch.end();
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix().idt());
        if (transformCount > 0) {
            transformCount--;
        }
        batch.getTransformMatrix().idt(); //reset
        shapeRenderer.getTransformMatrix().idt(); //reset
        batch.begin();
    }

    public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation) {
        drawRotatedImage(image, x, y, w, h, originX, originY, 0, 0, image.getWidth(), image.getHeight(), rotation);
    }

    public void drawRotatedImage(TextureRegion image, float x, float y, float w, float h, float originX, float originY, float rotation) {
        if (image == null)
            return;
        drawRotatedImage(image.getTexture(), x, y, w, h, originX, originY, image.getRegionX(), image.getRegionY(), image.getRegionWidth(), image.getRegionHeight(), rotation);
    }

    public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, int srcX, int srcY, int srcWidth, int srcHeight, float rotation) {
        if (image == null)
            return;
        batch.draw(image, adjustX(x), adjustY(y, h), originX - x, h - (originY - y), w, h, 1, 1, rotation, srcX, srcY, srcWidth, srcHeight, false, false);
    }

    public void drawText(String text, BitmapFont bitmapFont, float x, float y, Color color, float alpha) {
        if (text == null || bitmapFont == null || text.isEmpty())
            return;
        bitmapFont.setColor(color.r, color.g, color.b, alpha);
        bitmapFont.draw(batch, text, x, y);
    }

    public void drawText(BitmapFont bitmapFont, GlyphLayout layout, float x, float y) {
        if (bitmapFont == null || layout == null)
            return;
        bitmapFont.draw(batch, layout, x, y);
    }

    public void drawText(String text, FSkinFont font, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        drawText(text, font, skinColor.getColor(), x, y, w, h, wrap, horzAlignment, centerVertically);
    }

    public void drawText(String text, FSkinFont font, Color color, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        if (text == null)
            return;
        if (fitText(text, font, w, h, wrap)) {
            drawFittedText(text, color, x, y, w, h, wrap, horzAlignment, centerVertically);
        }
    }

    //measures the text and shrinks the font until it fits; result is left in fitFont/fitNeedClip/fitHeight
    private boolean fitText(String text, FSkinFont font, float w, float h, boolean wrap) {
        try {
            if (wrap) {
                font.getWrappedBounds(text, w, textBounds);
            } else {
                font.getMultiLineBounds(text, textBounds);
            }

            boolean needClip = false;

            while (textBounds.width > w || textBounds.height > h) {
                if (font.canShrink()) { // shrink font to fit if possible
                    font = font.shrink();
                    if (wrap) {
                        font.getWrappedBounds(text, w, textBounds);
                    } else {
                        font.getMultiLineBounds(text, textBounds);
                    }
                } else {
                    needClip = true;
                    break;
                }
            }
            fitFont = font;
            fitNeedClip = needClip;
            fitHeight = textBounds.height;
            return true;
        } catch (Exception e) {
            // shouldn't be here but force English on CJK Error
            Forge.setForcedEnglishonCJKMissing();
            return false;
        }
    }

    //draws text previously measured by fitText; blend/clip state is always restored, even on failure
    private void drawFittedText(String text, Color color, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        boolean blendEnabled = false;
        boolean clipStarted = false;
        try {
            if (alphaComposite < 1) {
                color = fade(color, fadeA);
            }
            if (color.a < 1) { // enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL_BLEND);
                blendEnabled = true;
            }

            if (fitNeedClip) { // prevent text flowing outside region if couldn't shrink it to fit
                startClip(x, y, w, h);
                clipStarted = true;
            }

            float drawY = y;
            if (h > fitHeight && centerVertically) {
                drawY += (h - fitHeight) / 2;
            }

            fitFont.draw(batch, text, color, adjustX(x), adjustY(drawY, 0), w, wrap, horzAlignment);
        } catch (Exception e) {
            // shouldn't be here but force English on CJK Error
            Forge.setForcedEnglishonCJKMissing();
        } finally {
            if (clipStarted) {
                endClip();
            }
            if (blendEnabled) {
                Gdx.gl.glDisable(GL_BLEND);
            }
        }
    }

    //use nifty trick with multiple text renders to draw outlined text
    public void drawOutlinedText(String text, FSkinFont skinFont, Color textColor, Color outlineColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        drawOutlinedText(text, skinFont, textColor, outlineColor, x, y, w, h, wrap, horzAlignment, centerVertically, false);
    }

    public void drawOutlinedText(String text, FSkinFont skinFont, Color textColor, Color outlineColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically, boolean shadow) {
        // all passes share the same text/font/size, so measure once instead of once per pass
        if (text == null || !fitText(text, skinFont, w, h, wrap))
            return;
        if (shadow) {
            float oldAlpha = alphaComposite;
            alphaComposite = 0.4f;
            try {
                drawFittedText(text, outlineColor, x - 1.5f, y + 1.5f, w, h, wrap, horzAlignment, centerVertically);
                drawFittedText(text, outlineColor, x + 1.5f, y + 1.5f, w, h, wrap, horzAlignment, centerVertically);
                drawFittedText(text, outlineColor, x + 1.5f, y - 1.5f, w, h, wrap, horzAlignment, centerVertically);
                drawFittedText(text, outlineColor, x - 1.5f, y - 1.5f, w, h, wrap, horzAlignment, centerVertically);
            } finally {
                alphaComposite = oldAlpha;
            }
        }
        drawFittedText(text, outlineColor, x - 1, y, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, outlineColor, x, y - 1, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, outlineColor, x - 1, y - 1, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, outlineColor, x + 1, y, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, outlineColor, x, y + 1, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, outlineColor, x + 1, y + 1, w, h, wrap, horzAlignment, centerVertically);
        drawFittedText(text, textColor, x, y, w, h, wrap, horzAlignment, centerVertically);
    }

    private Color fade(Color c, Color scratch) {
        return scratch.set(c.r, c.g, c.b, c.a * alphaComposite);
    }

    public float adjustX(float x) {
        return x + bounds.x;
    }

    public float adjustY(float y, float height) {
        return regionHeight - y - bounds.y - height; //flip y-axis
    }

    public Color borderLining(String c) {
        if (c == null || "".equals(c))
            return new Color(LINING_LIGHT);
        int c_r = Integer.parseInt(c.substring(0, 2), 16);
        int c_g = Integer.parseInt(c.substring(2, 4), 16);
        int c_b = Integer.parseInt(c.substring(4, 6), 16);
        return new Color(liningFor(c_r, c_g, c_b));
    }

    // same result as borderLining(color.toString()) without the String round trip; result is shared, don't modify
    private static Color borderLiningFor(Color c) {
        return liningFor((int) (255 * c.r), (int) (255 * c.g), (int) (255 * c.b));
    }

    private static Color liningFor(int r, int g, int b) {
        int brightness = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return brightness > 155 ? LINING_DARK : LINING_LIGHT;
    }

    // ---- Arena theme: shapes, glow, tilt and fades -------------------------------------------------------------------
    //
    // Cost model (read this before drawing hundreds of things):
    //  * SpriteBatch flushes (one GL draw call) whenever the texture or the shader changes.
    //  * The Arena shapes below (fills, gradients, rings, glows, soft shadows) are quads drawn through the batch with the
    //    shared 1x1 white texture and the Arena "uber" shader. That shader draws ordinary sprites exactly like the default
    //    one, so once bound it is left bound: shapes, fillRect/fillGradientRect/drawRect quads and multi-stop gradients
    //    all merge into ONE draw call until something else changes the texture (text, an image) or the shader (any of the
    //    older effect helpers such as drawCardRoundRect, which reset it to the default). Draw a layer's shapes together
    //    (e.g. all glows of a row, then all cards) and they cost one call.
    //  * drawTiltedImage uses its own shader with per-card uniforms: one draw call per tilted card, the same as
    //    drawCardRoundRect costs today. Use it for the few cards that are actually tilted or rounded, not for every tile.
    //  * drawImageFaded / drawCrossfade only change the batch colour: no flush.
    //  * Shapes follow the batch transform (startRotateTransform), clips and setAlphaComposite like everything else.
    // Coordinates are Forge's: y grows downwards from the top of the current bounds, in logical pixels.

    /** {@link #drawArenaShape} modes: filled rounded rectangle. */
    public static final int SHAPE_FILL = 0;
    /** Ring (outline) of thickness {@code amount}, drawn inside the shape's edge. */
    public static final int SHAPE_RING = 1;
    /** Soft glow fading out over {@code amount} beyond the shape's edge; nothing is drawn inside the shape. */
    public static final int SHAPE_GLOW = 2;
    /** The shape blurred by {@code amount} (a soft shadow or halo, filled in the middle). */
    public static final int SHAPE_SOFT = 3;

    private ShaderProgram arenaShader, arenaTiltShader;
    private boolean arenaShaderFailed;
    private final float[] shapeVerts = new float[20 * 8];
    private final Color shapeC1 = new Color(), shapeC2 = new Color(), shapeC3 = new Color();
    private final Color[] twoColors = new Color[2];
    private static final float[] TWO_STOPS = {0f, 1f};
    private float[] tiltVerts = new float[0];
    private static final int TILT_GRID = 8;
    private final float[] tiltGx = new float[(TILT_GRID + 1) * (TILT_GRID + 1)], tiltGy = new float[(TILT_GRID + 1) * (TILT_GRID + 1)],
            tiltGc = new float[(TILT_GRID + 1) * (TILT_GRID + 1)];

    /** Whether the Arena shapes can be drawn on this GPU (the shader compiled). Shapes degrade to plain fills otherwise. */
    public boolean isArenaShaderAvailable() {
        return getArenaShader() != null;
    }

    private ShaderProgram getArenaShader() {
        if (arenaShader == null && !arenaShaderFailed) {
            ShaderProgram s = ShaderUtil.getInstance().getShaderArena();
            if (s.isCompiled()) {
                arenaShader = s;
            } else {
                arenaShaderFailed = true;
            }
        }
        return arenaShader;
    }

    /**
     * Binds the Arena uber shader unless it is bound already (a flush only if it was not). Ordinary drawing carries
     * on unchanged under it, so there is no need to unbind; {@link #end()} resets it at the end of the frame.
     */
    public boolean useArenaShader() {
        ShaderProgram s = getArenaShader();
        if (s == null) {
            return false;
        }
        if (batch.getShader() != s) {
            batch.setShader(s);
        }
        return true;
    }

    /**
     * One signed-distance rounded rectangle through the batch, with a two-colour gradient.
     *
     * @param mode    {@link #SHAPE_FILL}, {@link #SHAPE_RING}, {@link #SHAPE_GLOW} or {@link #SHAPE_SOFT}
     * @param x       left of the shape itself (for glow/soft the drawn quad reaches {@code amount} further out)
     * @param radius  corner radius, clamped to half the shorter side (half of it makes a pill or a circle)
     * @param amount  ring thickness, glow spread or blur, in logical pixels (ignored for fills)
     * @param c1      colour at the top (vertical) or left; alpha included
     * @param c2      colour at the bottom or right (pass c1 again for a flat colour)
     */
    public void drawArenaShape(int mode, float x, float y, float w, float h, float radius, float amount, Color c1, Color c2, boolean vertical) {
        twoColors[0] = c1;
        twoColors[1] = c2;
        drawArenaShape(mode, x, y, w, h, radius, amount, twoColors, TWO_STOPS, vertical);
        twoColors[0] = twoColors[1] = null;
    }

    /**
     * {@link #drawArenaShape(int, float, float, float, float, float, float, Color, Color, boolean)} with a multi-stop
     * gradient: {@code colors[i]} at {@code stops[i]} (0..1 across the shape, ascending). It is one shape however many
     * stops it has (the pieces join seamlessly), drawn in one batch run.
     */
    public void drawArenaShape(int mode, float x, float y, float w, float h, float radius, float amount, Color[] colors, float[] stops, boolean vertical) {
        if (w <= 0 || h <= 0 || colors.length == 0 || !batch.isDrawing()) {
            return;
        }
        if (!useArenaShader()) {
            // no shader: plain fills where that is close enough, nothing for the soft effects
            if (mode == SHAPE_FILL) {
                fillRoundRect(colors[0], x, y, w, h, Math.min(radius, Math.min(w, h) / 2f));
            } else if (mode == SHAPE_RING) {
                drawRoundRect(Math.max(1f, amount), colors[0], x, y, w, h, Math.min(radius, Math.min(w, h) / 2f));
            }
            return;
        }
        final float pad = mode == SHAPE_GLOW || mode == SHAPE_SOFT ? Math.max(1f, amount) : 0f;
        final float qx0 = x - pad, qy0 = y - pad, qw = w + 2 * pad, qh = h + 2 * pad;
        final float m = Math.min(qw, qh) / 2f;
        final float r = Math.max(0f, Math.min(radius, Math.min(w, h) / 2f));
        final int a = Math.round(Math.min(1f, r / m) * 1023f);
        final int amt;
        if (mode == SHAPE_FILL) {
            amt = 0;
        } else if (mode == SHAPE_RING) {
            amt = Math.round(Math.min(1f, Math.max(0f, amount) / m) * 255f);
        } else {
            amt = Math.round(Math.min(1f, pad / m) * 255f);
        }
        final int b = (mode & 3) * 256 + amt;
        final float uBase = -4f * (a + 1), vBase = -4f * (b + 1);

        // split the quad along the gradient axis at the stops that fall inside it; every piece keeps the whole quad's
        // normalised coordinates, so the shader still sees one shape
        final float axis0 = vertical ? qy0 : qx0, axisLen = vertical ? qh : qw;
        final float shape0 = vertical ? y : x, shapeLen = vertical ? h : w;
        float prev = 0f; // position along the quad, 0..1
        int n = 0;
        final float[] v = shapeVerts;
        for (int i = 0; i <= stops.length; i++) {
            float next;
            if (i < stops.length) {
                next = (shape0 + stops[i] * shapeLen - axis0) / axisLen;
                if (next <= prev + 1e-4f || next >= 1f - 1e-4f) {
                    continue;
                }
            } else {
                next = 1f;
            }
            if (n * 20 + 20 > v.length) {
                flushShapeVerts(n);
                n = 0;
            }
            putShapePiece(v, n * 20, prev, next, vertical, qx0, qy0, qw, qh, uBase, vBase, colors, stops, shape0, shapeLen, axis0, axisLen);
            n++;
            prev = next;
        }
        flushShapeVerts(n);
    }

    private void flushShapeVerts(int pieces) {
        if (pieces > 0) {
            batch.draw(getWhiteTexture(), shapeVerts, 0, pieces * 20);
        }
    }

    private void putShapePiece(float[] v, int o, float t0, float t1, boolean vertical, float qx0, float qy0, float qw, float qh,
            float uBase, float vBase, Color[] colors, float[] stops, float shape0, float shapeLen, float axis0, float axisLen) {
        // colours at both ends of the piece
        final float c0 = gradientAt(colors, stops, (axis0 + t0 * axisLen - shape0) / shapeLen, shapeC1).toFloatBits();
        final float c1 = gradientAt(colors, stops, (axis0 + t1 * axisLen - shape0) / shapeLen, shapeC1).toFloatBits();
        final float px0, px1, py0, py1; // piece in Forge coordinates (y down)
        final float nx0, nx1, ny0, ny1; // normalised quad coordinates, -1..1
        if (vertical) {
            px0 = qx0; px1 = qx0 + qw; nx0 = -1f; nx1 = 1f;
            py0 = qy0 + t0 * qh; py1 = qy0 + t1 * qh; ny0 = -1f + 2f * t0; ny1 = -1f + 2f * t1;
        } else {
            py0 = qy0; py1 = qy0 + qh; ny0 = -1f; ny1 = 1f;
            px0 = qx0 + t0 * qw; px1 = qx0 + t1 * qw; nx0 = -1f + 2f * t0; nx1 = -1f + 2f * t1;
        }
        final float l = adjustX(px0), r = adjustX(px1);
        final float top = adjustY(py0, 0), bottom = adjustY(py1, 0); // GL y: top > bottom
        // colour at the top / left edge of the piece is c0, at the bottom / right c1
        final float cTL = c0, cTR = vertical ? c0 : c1, cBL = vertical ? c1 : c0, cBR = c1;
        // bottom-left, top-left, top-right, bottom-right
        v[o] = l;       v[o + 1] = bottom; v[o + 2] = cBL; v[o + 3] = uBase + nx0; v[o + 4] = vBase + ny1;
        v[o + 5] = l;   v[o + 6] = top;    v[o + 7] = cTL; v[o + 8] = uBase + nx0; v[o + 9] = vBase + ny0;
        v[o + 10] = r;  v[o + 11] = top;   v[o + 12] = cTR; v[o + 13] = uBase + nx1; v[o + 14] = vBase + ny0;
        v[o + 15] = r;  v[o + 16] = bottom; v[o + 17] = cBR; v[o + 18] = uBase + nx1; v[o + 19] = vBase + ny1;
    }

    // colour of a multi-stop gradient at t (clamped), times the alpha composite, into out
    private Color gradientAt(Color[] colors, float[] stops, float t, Color out) {
        Color c;
        if (colors.length == 1 || t <= stops[0]) {
            out.set(colors[0]);
        } else if (t >= stops[stops.length - 1]) {
            out.set(colors[Math.min(colors.length, stops.length) - 1]);
        } else {
            int i = 1;
            while (i < stops.length - 1 && t > stops[i]) {
                i++;
            }
            final float span = stops[i] - stops[i - 1];
            final float f = span <= 0f ? 1f : (t - stops[i - 1]) / span;
            c = colors[i - 1];
            final Color d = colors[i];
            out.set(c.r + (d.r - c.r) * f, c.g + (d.g - c.g) * f, c.b + (d.b - c.b) * f, c.a + (d.a - c.a) * f);
        }
        if (alphaComposite < 1) {
            out.a *= alphaComposite;
        }
        return out;
    }

    /** Antialiased filled rounded rectangle, batched (the older {@link #fillRoundRect} pauses the batch). */
    public void fillSmoothRoundRect(Color color, float x, float y, float w, float h, float radius) {
        drawArenaShape(SHAPE_FILL, x, y, w, h, radius, 0, color, color, true);
    }

    /** Antialiased rounded rectangle with a vertical (top to bottom) or horizontal (left to right) gradient. */
    public void fillSmoothRoundRectGradient(Color c1, Color c2, boolean vertical, float x, float y, float w, float h, float radius) {
        drawArenaShape(SHAPE_FILL, x, y, w, h, radius, 0, c1, c2, vertical);
    }

    /** Antialiased rounded outline of the given thickness, inside the rectangle's edge. */
    public void drawSmoothRoundRect(float thickness, Color color, float x, float y, float w, float h, float radius) {
        drawArenaShape(SHAPE_RING, x, y, w, h, radius, thickness, color, color, true);
    }

    /** Antialiased disc (centre and radius, like {@link #fillCircle}). */
    public void fillSmoothCircle(Color color, float cx, float cy, float radius) {
        drawArenaShape(SHAPE_FILL, cx - radius, cy - radius, 2 * radius, 2 * radius, radius, 0, color, color, true);
    }

    /** Antialiased ring round a circle, inside its edge. */
    public void drawSmoothCircle(float thickness, Color color, float cx, float cy, float radius) {
        drawArenaShape(SHAPE_RING, cx - radius, cy - radius, 2 * radius, 2 * radius, radius, thickness, color, color, true);
    }

    /**
     * Outer glow round a rounded rectangle (a card, a button): strongest at its edge and gone {@code spread} beyond it,
     * nothing inside, so draw it before or after the thing itself. Intensity is the colour's alpha.
     */
    public void drawGlow(Color color, float x, float y, float w, float h, float radius, float spread) {
        drawArenaShape(SHAPE_GLOW, x, y, w, h, radius, spread, color, color, true);
    }

    /** Soft shadow (or filled halo) of a rounded rectangle, blurred by {@code blur}. Offset x/y yourself for a drop shadow. */
    public void drawSoftShadow(Color color, float x, float y, float w, float h, float radius, float blur) {
        drawArenaShape(SHAPE_SOFT, x, y, w, h, radius, blur, color, color, true);
    }

    /**
     * Plain (square) rectangle with a multi-stop gradient, batched with the default shader: {@code colors[i]} at
     * {@code stops[i]} (0..1, ascending) along the width (horizontal) or height (vertical).
     */
    public void fillGradientStops(Color[] colors, float[] stops, boolean vertical, float x, float y, float w, float h) {
        if (w <= 0 || h <= 0 || colors.length == 0) {
            return;
        }
        if (!batch.isDrawing()) {
            fillGradientRect(colors[0], colors[colors.length - 1], vertical, x, y, w, h);
            return;
        }
        float prev = 0f;
        for (int i = 0; i <= stops.length; i++) {
            float next = i < stops.length ? stops[i] : 1f;
            if (next <= prev + 1e-4f && i < stops.length) {
                continue;
            }
            next = Math.min(next, 1f);
            final float ca = gradientAt(colors, stops, prev, shapeC2).toFloatBits();
            final float cb = gradientAt(colors, stops, next, shapeC3).toFloatBits();
            if (vertical) {
                final float y0 = y + prev * h, y1 = y + next * h;
                // bottom-left, top-left, top-right, bottom-right: top is ca
                batchQuad(adjustX(x), adjustY(y1, 0), w, y1 - y0, cb, ca, ca, cb);
            } else {
                final float x0 = x + prev * w, x1 = x + next * w;
                batchQuad(adjustX(x0), adjustY(y, h), x1 - x0, h, ca, ca, cb, cb);
            }
            prev = next;
            if (prev >= 1f) {
                break;
            }
        }
    }

    /** Draws an image with its alpha multiplied by {@code alpha} (on top of setAlphaComposite). No flush. */
    public void drawImageFaded(TextureRegion image, float x, float y, float w, float h, float alpha) {
        if (image == null || alpha <= 0f) {
            return;
        }
        final float old = batch.getPackedColor();
        final Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, c.a * Math.min(1f, alpha));
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        batch.setPackedColor(old);
    }

    public void drawImageFaded(Texture image, float x, float y, float w, float h, float alpha) {
        if (image == null || alpha <= 0f) {
            return;
        }
        final float old = batch.getPackedColor();
        final Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, c.a * Math.min(1f, alpha));
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        batch.setPackedColor(old);
    }

    /** {@code from} fading into {@code to}: t = 0 shows only {@code from}, 1 only {@code to}. No flush beyond texture changes. */
    public void drawCrossfade(TextureRegion from, TextureRegion to, float x, float y, float w, float h, float t) {
        t = Math.max(0f, Math.min(1f, t));
        if (t < 1f) {
            drawImageFaded(from, x, y, w, h, 1f);
        }
        if (t > 0f) {
            drawImageFaded(to, x, y, w, h, t);
        }
    }

    /**
     * A card (or any image) with an outer glow, then the image itself, with rounded corners when {@code cornerRadius}
     * is above 0 (through {@link #drawTiltedImage}, flat). For many cards prefer drawing all the glows first with
     * {@link #drawGlow}, then the cards: the glows then share one draw call.
     */
    public void drawImageWithGlow(TextureRegion image, float x, float y, float w, float h, float cornerRadius, Color glowColor, float spread) {
        if (image == null) {
            return;
        }
        if (glowColor != null && glowColor.a > 0f && spread > 0f) {
            drawGlow(glowColor, x, y, w, h, cornerRadius, spread);
        }
        if (cornerRadius > 0f) {
            drawTiltedImage(image, x, y, w, h, 0f, 0f, 0f, 1f, cornerRadius, 0f, 0f, 1f);
        } else {
            drawImage(image, x, y, w, h);
        }
    }

    /** {@link #drawTiltedImage(TextureRegion, float, float, float, float, float, float, float, float, float, float, float, float)} for a whole texture. */
    public void drawTiltedImage(Texture image, float x, float y, float w, float h, float tiltX, float tiltY, float rotation,
            float scale, float cornerRadius, float shading, float gray, float alpha) {
        if (image == null) {
            return;
        }
        drawTiltedImage(new TextureRegion(image), x, y, w, h, tiltX, tiltY, rotation, scale, cornerRadius, shading, gray, alpha);
    }

    /**
     * Draws an image as a card in fake 3D: tilted towards the viewer (hover tilt), turned in the plane (tap), lifted
     * (scaled about its centre), with antialiased rounded corners, shading on the side turned away and a soft glare.
     * The card is a perspective-projected 8x8 mesh, so the texture does not shear along the diagonal. One draw call.
     *
     * @param x,y,w,h      the flat, untransformed card (Forge coordinates); it turns about its centre
     * @param tiltX        degrees about the horizontal screen axis; positive tips the top edge away from the viewer
     * @param tiltY        degrees about the vertical screen axis; positive tips the right edge away
     * @param rotation     degrees in the plane, clockwise on screen (90 = tapped), applied before the tilt
     * @param scale        lift: 1 is the flat size
     * @param cornerRadius corner radius of the flat card in logical pixels (0 for square corners)
     * @param shading      0..1: how dark the far side gets and how bright the glare is
     * @param gray         0..1 grey-out (e.g. for something that cannot be chosen)
     * @param alpha        0..1, on top of setAlphaComposite
     */
    public void drawTiltedImage(TextureRegion image, float x, float y, float w, float h, float tiltX, float tiltY, float rotation,
            float scale, float cornerRadius, float shading, float gray, float alpha) {
        if (image == null || w <= 0 || h <= 0 || alpha <= 0f || !batch.isDrawing()) {
            return;
        }
        if (arenaTiltShader == null) {
            ShaderProgram s = ShaderUtil.getInstance().getShaderArenaTilt();
            if (!s.isCompiled()) {
                // no shader: a plain rotated image
                drawRotatedImage(image, x + w * (1 - scale) / 2, y + h * (1 - scale) / 2, w * scale, h * scale,
                        x + w / 2, y + h / 2, -rotation);
                return;
            }
            arenaTiltShader = s;
            if (arenaShader == null) {
                getArenaShader(); // so end() knows both
            }
        }
        final boolean tilted = Math.abs(tiltX) > 0.05f || Math.abs(tiltY) > 0.05f;
        final int n = tilted ? TILT_GRID : 1;
        final int floats = n * n * 20;
        if (tiltVerts.length < floats) {
            tiltVerts = new float[floats];
        }
        final float cx = x + w / 2f, cy = y + h / 2f;
        final double ax = Math.toRadians(tiltX), ay = Math.toRadians(tiltY), ar = Math.toRadians(rotation);
        final float cosA = (float) Math.cos(ax), sinA = (float) Math.sin(ax);
        final float cosB = (float) Math.cos(ay), sinB = (float) Math.sin(ay);
        final float cosR = (float) Math.cos(ar), sinR = (float) Math.sin(ar);
        final float sw = w * scale, sh = h * scale;
        final float depth = 3.2f * Math.max(sw, sh); // viewer distance: a modest perspective
        final float u0 = image.getU(), u1 = image.getU2(), v0 = image.getV(), v1 = image.getV2();
        final Color bc = batch.getColor();
        final float baseA = bc.a * Math.min(1f, alpha);
        final float half = 0.5f * Math.max(sw, sh);
        // project the (n+1)^2 grid once
        final int g1 = n + 1;
        final float[] gx = tiltGx, gy = tiltGy, gc = tiltGc;
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                float lx = ((float) i / n - 0.5f) * sw, ly = ((float) j / n - 0.5f) * sh; // y down
                // turn in the plane (clockwise on a y-down screen)
                final float rx = lx * cosR - ly * sinR, ry = lx * sinR + ly * cosR;
                // tilt about the screen's vertical axis, then its horizontal axis; z grows away from the viewer
                final float x1 = rx * cosB, z1 = rx * sinB;
                final float y2 = ry * cosA + z1 * sinA, z2 = -ry * sinA + z1 * cosA;
                final float f = depth / (depth + z2);
                final int k = j * g1 + i;
                gx[k] = adjustX(cx + x1 * f);
                gy[k] = adjustY(cy + y2 * f, 0);
                final float shade = 1f - shading * 0.45f * Math.max(0f, Math.min(1f, z2 / half));
                gc[k] = Color.toFloatBits(bc.r * shade, bc.g * shade, bc.b * shade, baseA);
            }
        }
        final float[] v = tiltVerts;
        int o = 0;
        for (int j = 0; j < n; j++) {
            final float tv0 = v0 + (v1 - v0) * j / n, tv1 = v0 + (v1 - v0) * (j + 1) / n;
            for (int i = 0; i < n; i++) {
                final float tu0 = u0 + (u1 - u0) * i / n, tu1 = u0 + (u1 - u0) * (i + 1) / n;
                final int tl = j * g1 + i, tr = tl + 1, bl = tl + g1, br = bl + 1;
                // bottom-left, top-left, top-right, bottom-right (in card terms: row j is above row j+1)
                v[o] = gx[bl];  v[o + 1] = gy[bl];  v[o + 2] = gc[bl];  v[o + 3] = tu0;  v[o + 4] = tv1;
                v[o + 5] = gx[tl]; v[o + 6] = gy[tl]; v[o + 7] = gc[tl]; v[o + 8] = tu0; v[o + 9] = tv0;
                v[o + 10] = gx[tr]; v[o + 11] = gy[tr]; v[o + 12] = gc[tr]; v[o + 13] = tu1; v[o + 14] = tv0;
                v[o + 15] = gx[br]; v[o + 16] = gy[br]; v[o + 17] = gc[br]; v[o + 18] = tu1; v[o + 19] = tv1;
                o += 20;
            }
        }
        final ShaderProgram prev = batch.getShader();
        if (prev == arenaTiltShader) {
            batch.flush(); // another tilted card is still waiting with its own uniforms
        } else {
            batch.setShader(arenaTiltShader); // flushes what was batched before, binds the shader
        }
        arenaTiltShader.setUniformf("u_uvRect", u0, v0, u1, v1);
        arenaTiltShader.setUniformf("u_size", sw, sh);
        arenaTiltShader.setUniformf("u_radius", Math.max(0f, Math.min(cornerRadius * scale, Math.min(sw, sh) / 2f)));
        final float tiltAmount = Math.min(1f, (Math.abs(tiltX) + Math.abs(tiltY)) / 15f);
        arenaTiltShader.setUniformf("u_sheen", 0.5f - tiltY / 45f, 0.5f + tiltX / 45f, tilted ? shading * 0.28f * tiltAmount : 0f);
        arenaTiltShader.setUniformf("u_gray", Math.max(0f, Math.min(1f, gray)));
        batch.draw(image.getTexture(), v, 0, floats);
        // the uniforms belong to this card: draw it now and go back to what was bound
        batch.setShader(prev == arenaShader ? arenaShader : null);
    }

    /** Width of a video mode: "1080p" and the like, or "WIDTHxHEIGHT" (desktop window sizes of the monitor's shape). */
    public static int videoModeWidth(String videoMode) {
        int x = videoMode == null ? -1 : videoMode.indexOf('x');
        if (x > 0) {
            try {
                return Integer.parseInt(videoMode.substring(0, x).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        switch (videoMode == null ? "" : videoMode) {
            case "768p": return 1366;
            case "900p": return 1600;
            case "1080p": return 1920;
            case "1440p": return 2560;
            case "2160p": return 3840;
            default: return 1280;
        }
    }

    /** Height of a video mode: "1080p" and the like, or "WIDTHxHEIGHT". */
    public static int videoModeHeight(String videoMode) {
        int x = videoMode == null ? -1 : videoMode.indexOf('x');
        if (x > 0) {
            try {
                return Integer.parseInt(videoMode.substring(x + 1).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        switch (videoMode == null ? "" : videoMode) {
            case "768p": return 768;
            case "900p": return 900;
            case "1080p": return 1080;
            case "1440p": return 1440;
            case "2160p": return 2160;
            default: return 720;
        }
    }

    public static void setVideoMode(String videoMode) {
        if (videoMode == null)
            videoMode = "720p";
        Config.instance().getSettingData().videomode = videoMode;
        if (videoMode.indexOf('x') > 0) { //a size of its own, "WIDTHxHEIGHT"
            Config.instance().getSettingData().width = videoModeWidth(videoMode);
            Config.instance().getSettingData().height = videoModeHeight(videoMode);
            Config.instance().saveSettings();
            return;
        }
        switch (videoMode) {
            case "768p":
                Config.instance().getSettingData().width = 1366;
                Config.instance().getSettingData().height = 768;
                break;
            case "900p":
                Config.instance().getSettingData().width = 1600;
                Config.instance().getSettingData().height = 900;
                break;
            case "1080p":
                Config.instance().getSettingData().width = 1920;
                Config.instance().getSettingData().height = 1080;
                break;
            case "1440p":
                Config.instance().getSettingData().width = 2560;
                Config.instance().getSettingData().height = 1440;
                break;
            case "2160p":
                Config.instance().getSettingData().width = 3840;
                Config.instance().getSettingData().height = 2160;
                break;
            default: // assume 720p
                Config.instance().getSettingData().width = 1280;
                Config.instance().getSettingData().height = 720;
                break;
        }
        Config.instance().saveSettings();
    }
}