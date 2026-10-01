package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
            batch.flush(); // must flush batch to ensure stuff rendered during clip respects that clip
            ScissorStack.popScissors();

            // Retract depth tracking downward as layout loops exit
            if (clipDepth > 0) {
                clipDepth--;
            }
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
        if (alphaComposite < 1) {
            fillColor = fade(fillColor, fadeA);
            strokeColor = fade(strokeColor, fadeB);
        }
        float lt = thickness / 3;
        boolean needSmoothing = (x1 != x2 && y1 != y2);
        prepCurved(thickness, x1, y1, x2, y2, !drawPointer);

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
        prepCurved(thickness, x1, y1, x2, y2, false);

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
    private void prepCurved(float thickness, float x1, float y1, float x2, float y2, boolean wantHead) {
        caSx1 = adjustX(x1);
        caSy1 = adjustY(y1, 0);
        caSx2 = adjustX(x2);
        caSy2 = adjustY(y2, 0);
        caHead = false;

        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        // Point just before the tip for direction
        float beforeTipX = x1, beforeTipY = y1;

        if (length < 120f) {
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
            float curveStrength = 50f;
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
        shapeRenderer.rect(adjustX(x), adjustY(y + cornerRadius, h - cornerRadius * 2), w, h - cornerRadius * 2);
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

    public void drawRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h) {
        drawRect(thickness, skinColor.getColor(), x, y, w, h);
    }

    public void drawRect(float thickness, Color color, float x, float y, float w, float h) {
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

    public void drawRectLines(float thickness, Color color, float x, float y, float w, float h) {
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

    public static void setVideoMode(String videoMode) {
        if (videoMode == null)
            videoMode = "720p";
        Config.instance().getSettingData().videomode = videoMode;
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