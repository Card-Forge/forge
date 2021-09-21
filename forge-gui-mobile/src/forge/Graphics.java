package forge;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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

import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.toolbox.FDisplayObject;
import forge.util.TextBounds;
import forge.util.Utils;

public class Graphics {
    private static final int GL_BLEND = GL20.GL_BLEND;
    private static final int GL_LINE_SMOOTH = 2848; //create constant here since not in GL20

    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Deque<Matrix4> Dtransforms = new ArrayDeque<>();
    private final Vector3 tmp = new Vector3();
    private float regionHeight;
    private Rectangle bounds;
    private Rectangle visibleBounds;
    private int failedClipCount;
    private float alphaComposite = 1;
    private int transformCount = 0;
    private final String sVertex = "uniform mat4 u_projTrans;\n" +
            "\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "attribute vec4 a_color;\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "\n" +
            "uniform vec2 u_viewportInverse;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "    v_texCoord = a_texCoord0;\n" +
            "    v_color = a_color;\n" +
            "}";
    private final String sFragment = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "precision mediump int;\n" +
            "#endif\n" +
            "\n" +
            "uniform sampler2D u_texture;\n" +
            "\n" +
            "// The inverse of the viewport dimensions along X and Y\n" +
            "uniform vec2 u_viewportInverse;\n" +
            "\n" +
            "// Color of the outline\n" +
            "uniform vec3 u_color;\n" +
            "\n" +
            "// Thickness of the outline\n" +
            "uniform float u_offset;\n" +
            "\n" +
            "// Step to check for neighbors\n" +
            "uniform float u_step;\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "\n" +
            "#define ALPHA_VALUE_BORDER 0.5\n" +
            "\n" +
            "void main() {\n" +
            "   vec2 T = v_texCoord.xy;\n" +
            "\n" +
            "   float alpha = 0.0;\n" +
            "   bool allin = true;\n" +
            "   for( float ix = -u_offset; ix < u_offset; ix += u_step )\n" +
            "   {\n" +
            "      for( float iy = -u_offset; iy < u_offset; iy += u_step )\n" +
            "       {\n" +
            "          float newAlpha = texture2D(u_texture, T + vec2(ix, iy) * u_viewportInverse).a;\n" +
            "          allin = allin && newAlpha > ALPHA_VALUE_BORDER;\n" +
            "          if (newAlpha > ALPHA_VALUE_BORDER && newAlpha >= alpha)\n" +
            "          {\n" +
            "             alpha = newAlpha;\n" +
            "          }\n" +
            "      }\n" +
            "   }\n" +
            "   if (allin)\n" +
            "   {\n" +
            "      alpha = 0.0;\n" +
            "   }\n" +
            "\n" +
            "   gl_FragColor = vec4(u_color,alpha);\n" +
            "}";
    private final String vertexShaderGray = "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "\n" +
            "uniform mat4 u_projTrans;\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "\n" +
            "void main() {\n" +
            "    v_color = a_color;\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}";
    private final String fragmentShaderGray = "#ifdef GL_ES\n" +
            "    precision mediump float;\n" +
            "#endif\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform float u_grayness;\n" +
            "\n" +
            "void main() {\n" +
            "  vec4 c = v_color * texture2D(u_texture, v_texCoords);\n" +
            "  float grey = dot( c.rgb, vec3(0.22, 0.707, 0.071) );\n" +
            "  vec3 blendedColor = mix(c.rgb, vec3(grey), u_grayness);\n" +
            "  gl_FragColor = vec4(blendedColor.rgb, c.a);\n" +
            "}";

    private final ShaderProgram shaderOutline = new ShaderProgram(sVertex, sFragment);
    private final ShaderProgram shaderGrayscale = new ShaderProgram(vertexShaderGray, fragmentShaderGray);

    public Graphics() {
        ShaderProgram.pedantic = false;
    }

    public void begin(float regionWidth0, float regionHeight0) {
        batch.begin();
        bounds = new Rectangle(0, 0, regionWidth0, regionHeight0);
        regionHeight = regionHeight0;
        visibleBounds = new Rectangle(bounds);
    }

    public void end() {
        if (batch.isDrawing()) {
            batch.end();
        }
        if (shapeRenderer.getCurrentType() != null) {
            shapeRenderer.end();
        }
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        shaderOutline.dispose();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public boolean startClip() {
        return startClip(0, 0, bounds.width, bounds.height);
    }
    public boolean startClip(float x, float y, float w, float h) {
        batch.flush(); //must flush batch to prevent other things not rendering

        Rectangle clip = new Rectangle(adjustX(x), adjustY(y, h), w, h);
        if (!Dtransforms.isEmpty()) { //transform position if needed
            tmp.set(clip.x, clip.y, 0);
            tmp.mul(batch.getTransformMatrix());
            float minX = tmp.x;
            float maxX = minX;
            float minY = tmp.y;
            float maxY = minY;
            tmp.set(clip.x + clip.width, clip.y, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) { minX = tmp.x; }
            else if (tmp.x > maxX) { maxX = tmp.x; }
            if (tmp.y < minY) { minY = tmp.y; }
            else if (tmp.y > maxY) { maxY = tmp.y; }
            tmp.set(clip.x + clip.width, clip.y + clip.height, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) { minX = tmp.x; }
            else if (tmp.x > maxX) { maxX = tmp.x; }
            if (tmp.y < minY) { minY = tmp.y; }
            else if (tmp.y > maxY) { maxY = tmp.y; }
            tmp.set(clip.x, clip.y + clip.height, 0);
            tmp.mul(batch.getTransformMatrix());
            if (tmp.x < minX) { minX = tmp.x; }
            else if (tmp.x > maxX) { maxX = tmp.x; }
            if (tmp.y < minY) { minY = tmp.y; }
            else if (tmp.y > maxY) { maxY = tmp.y; }

            clip.set(minX, minY, maxX - minX, maxY - minY);
        }
        if (!ScissorStack.pushScissors(clip)) {
            failedClipCount++; //tracked failed clips to prevent calling popScissors on endClip
            return false;
        }
        return true;
    }
    public void endClip() {
        if (failedClipCount == 0) {
            batch.flush(); //must flush batch to ensure stuffed rendered during clip respects that clip
            ScissorStack.popScissors();
        }
        else {
            failedClipCount--;
        }
    }

    public void draw(FDisplayObject displayObj) {
        if (displayObj.getWidth() <= 0 || displayObj.getHeight() <= 0) {
            return;
        }

        final Rectangle parentBounds = bounds;
        bounds = new Rectangle(parentBounds.x + displayObj.getLeft(), parentBounds.y + displayObj.getTop(), displayObj.getWidth(), displayObj.getHeight());
        if (!Dtransforms.isEmpty()) { //transform screen position if needed by applying transform matrix to rectangle
            updateScreenPosForRotation(displayObj);
        }
        else {
            displayObj.screenPos.set(bounds);
        }

        Rectangle intersection = Utils.getIntersection(bounds, visibleBounds);
        if (intersection != null) { //avoid drawing object if it's not within visible region
            final Rectangle backup = visibleBounds;
            visibleBounds = intersection;

            if (displayObj.getRotate90()) { //use top-right corner of bounds as pivot point
                startRotateTransform(displayObj.getWidth(), 0, -90);
                updateScreenPosForRotation(displayObj);
            }
            else if (displayObj.getRotate180()) { //use center of bounds as pivot point
                startRotateTransform(displayObj.getWidth() / 2, displayObj.getHeight() / 2, 180);
                //screen position won't change for this object from a 180 degree rotation
            }

            displayObj.draw(this);

            if (displayObj.getRotate90() || displayObj.getRotate180()) {
                    endTransform();
            }

            visibleBounds = backup;
        }

        bounds = parentBounds;
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
        if (tmp.x < minX) { minX = tmp.x; }
        else if (tmp.x > maxX) { maxX = tmp.x; }
        if (tmp.y < minY) { minY = tmp.y; }
        else if (tmp.y > maxY) { maxY = tmp.y; }
        tmp.set(bounds.x + bounds.width, regionHeight - bounds.y - bounds.height, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        if (tmp.x < minX) { minX = tmp.x; }
        else if (tmp.x > maxX) { maxX = tmp.x; }
        if (tmp.y < minY) { minY = tmp.y; }
        else if (tmp.y > maxY) { maxY = tmp.y; }
        tmp.set(bounds.x, regionHeight - bounds.y - bounds.height, 0);
        tmp.mul(batch.getTransformMatrix());
        tmp.y = regionHeight - tmp.y;
        if (tmp.x < minX) { minX = tmp.x; }
        else if (tmp.x > maxX) { maxX = tmp.x; }
        if (tmp.y < minY) { minY = tmp.y; }
        else if (tmp.y > maxY) { maxY = tmp.y; }

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
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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

    public void drawArrow(float borderThickness, float arrowThickness, float arrowSize, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
        drawArrow(borderThickness, arrowThickness, arrowSize, skinColor.getColor(), x1, y1, x2, y2);
    }
    public void drawArrow(float borderThickness, float arrowThickness, float arrowSize, Color color, float x1, float y1, float x2, float y2) {
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
        }
        Gdx.gl.glEnable(GL_BLEND);
        Gdx.gl.glEnable(GL_LINE_SMOOTH);

        float angle = new Vector2(x2 - x1, y2 - y1).angleRad();
        float perpRotation = (float)(Math.PI * 0.5f);
        float arrowHeadRotation = (float)(Math.PI * 0.8f);
        float arrowTipAngle = (float)(Math.PI - arrowHeadRotation);
        float halfThickness = arrowThickness / 2;

        int index = 0;
        float[] vertices = new float[14];
        Vector2 arrowCorner1 = new Vector2(x2 + arrowSize * (float)Math.cos(angle + arrowHeadRotation), y2 + arrowSize * (float)Math.sin(angle + arrowHeadRotation));
        Vector2 arrowCorner2 = new Vector2(x2 + arrowSize * (float)Math.cos(angle - arrowHeadRotation), y2 + arrowSize * (float)Math.sin(angle - arrowHeadRotation));
        float arrowCornerLen = (arrowCorner1.dst(arrowCorner2) - arrowThickness) / 2;
        float arrowHeadLen = arrowSize * (float)Math.cos(arrowTipAngle);
        index = addVertex(arrowCorner1.x, arrowCorner1.y, vertices, index);
        index = addVertex(x2, y2, vertices, index);
        index = addVertex(arrowCorner2.x, arrowCorner2.y, vertices, index);
        index = addVertex(arrowCorner2.x + arrowCornerLen * (float)Math.cos(angle + perpRotation), arrowCorner2.y + arrowCornerLen * (float)Math.sin(angle + perpRotation), vertices, index);
        index = addVertex(x1 + halfThickness * (float)Math.cos(angle - perpRotation), y1 + halfThickness * (float)Math.sin(angle - perpRotation), vertices, index);
        index = addVertex(x1 + halfThickness * (float)Math.cos(angle + perpRotation), y1 + halfThickness * (float)Math.sin(angle + perpRotation), vertices, index);
        index = addVertex(arrowCorner1.x + arrowCornerLen * (float)Math.cos(angle - perpRotation), arrowCorner1.y + arrowCornerLen * (float)Math.sin(angle - perpRotation), vertices, index);

        //draw arrow tail
        startShape(ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.rectLine(adjustX(x1), adjustY(y1, 0),
                adjustX(x2 - arrowHeadLen * (float)Math.cos(angle)), //shorten tail to make room for arrow head
                adjustY(y2 - arrowHeadLen * (float)Math.sin(angle), 0), arrowThickness);

        //draw arrow head
        shapeRenderer.triangle(vertices[0], vertices[1], vertices[2], vertices[3], vertices[4], vertices[5]);
        endShape();

        //draw border around arrow
        if (borderThickness > 1) {
            Gdx.gl.glLineWidth(borderThickness);
        }
        startShape(ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.polygon(vertices);
        endShape();
        if (borderThickness > 1) {
            Gdx.gl.glLineWidth(1);
        }

        Gdx.gl.glDisable(GL_LINE_SMOOTH);
        Gdx.gl.glDisable(GL_BLEND);

        batch.begin();
    }

    private int addVertex(float x, float y, float[] vertices, int index) {
        vertices[index] = adjustX(x);
        vertices[index + 1] = adjustY(y, 0);
        return index + 2;
    }

    public void drawfillBorder(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
        drawRoundRect(thickness, color, x, y, w, h, cornerRadius);
        fillRoundRect(color, x, y, w, h, cornerRadius);
    }

    public void drawRoundRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h, float cornerRadius) {
        drawRoundRect(thickness, skinColor.getColor(), x, y, w, h, cornerRadius);
    }
    public void drawRoundRect(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
        batch.end(); //must pause batch while rendering shapes

        if (thickness > 1) {
            Gdx.gl.glLineWidth(thickness);
        }
        if (alphaComposite < 1) {
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
        shapeRenderer.rect(adjustX(x), adjustY(y+cornerRadius, h-cornerRadius*2), w, h-cornerRadius*2);
        shapeRenderer.rect(adjustX(x+cornerRadius), adjustY(y, h), w-cornerRadius*2, h);

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

        batch.begin();
    }

    public void fillRoundRect(FSkinColor skinColor, float x, float y, float w, float h, float cornerRadius) {
        fillRoundRect(skinColor.getColor(), x, y, w, h, cornerRadius);
    }
    public void fillRoundRect(Color color, float x, float y, float w, float h, float cornerRadius) {
        batch.end(); //must pause batch while rendering shapes
        if (alphaComposite < 1) {
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
        shapeRenderer.rect(adjustX(x), adjustY(y+cornerRadius, h-cornerRadius*2), w, h-cornerRadius*2);
        shapeRenderer.rect(adjustX(x+cornerRadius), adjustY(y, h), w-cornerRadius*2, h);
        endShape();
        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }
        batch.begin();
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
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
        drawLine(thickness, color, x, y, x+w, y);
        drawLine(thickness, color, x+thickness/2f, y+thickness/2f, x+thickness/2f, y+h-thickness/2f);
        drawLine(thickness, color, x, y+h, x+w, y+h);
        drawLine(thickness, color, x+w-thickness/2f, y+thickness/2f, x+w-thickness/2f, y+h-thickness/2f);
    }

    public void fillRect(FSkinColor skinColor, float x, float y, float w, float h) {
        fillRect(skinColor.getColor(), x, y, w, h);
    }
    public void fillRect(Color color, float x, float y, float w, float h) {
        batch.end(); //must pause batch while rendering shapes

        if (alphaComposite < 1) {
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
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
            color1 = FSkinColor.alphaColor(color1, color1.a * alphaComposite);
            color2 = FSkinColor.alphaColor(color2, color2.a * alphaComposite);
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
        if (!Dtransforms.isEmpty()) {
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
        batch.setColor(new Color(r, g, b, alphaComposite));
    }

    public void resetColorRGBA(float alphaComposite0) {
        alphaComposite = alphaComposite0;
        batch.setColor(Color.WHITE);
    }

    public void setAlphaComposite(float alphaComposite0) {
        alphaComposite = alphaComposite0;
        batch.setColor(new Color(1, 1, 1, alphaComposite));
    }
    public void resetAlphaComposite() {
        alphaComposite = 1;
        batch.setColor(Color.WHITE);
    }
    public float getfloatAlphaComposite() { return  alphaComposite; }

    public void drawBorderImage(FImage image, Color borderColor, Color tintColor, float x, float y, float w, float h, boolean tint) {
        float oldalpha = alphaComposite;
        if(tint && !tintColor.equals(borderColor)){
            drawRoundRect(2f, borderLining(borderColor.toString()), x, y, w, h, (h-w)/12);
            fillRoundRect(tintColor, x, y, w, h, (h-w)/12);
        } else {
            image.draw(this, x, y, w, h);
            fillRoundRect(borderColor, x, y, w, h, (h-w)/10);//show corners edges
        }
        setAlphaComposite(oldalpha);
    }
    public void drawborderImage(Color borderColor, float x, float y, float w, float h) {
        float oldalpha = alphaComposite;
        fillRoundRect(borderColor, x, y, w, h, (h-w)/12);
        setAlphaComposite(oldalpha);
    }
    public void drawImage(FImage image, Color borderColor, float x, float y, float w, float h) {
        image.draw(this, x, y, w, h);
        fillRoundRect(borderColor, x+1, y+1, w-1.5f, h-1.5f, (h-w)/10);//used by zoom let some edges show...
    }
    public void drawAvatarImage(FImage image, float x, float y, float w, float h, boolean drawGrayscale) {
        if (!drawGrayscale) {
            image.draw(this, x, y, w, h);
        } else {
            batch.end();
            shaderGrayscale.bind();
            shaderGrayscale.setUniformf("u_grayness", 1f);
            batch.setShader(shaderGrayscale);
            batch.begin();
            //draw gray
            image.draw(this, x, y, w, h);
            //reset
            batch.end();
            batch.setShader(null);
            batch.begin();
        }
    }
    public void drawCardImage(FImage image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged) {
        if (!drawGrayscale) {
            image.draw(this, x, y, w, h);
            if (damage_overlay != null && damaged)
                batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
        } else {
            batch.end();
            shaderGrayscale.bind();
            shaderGrayscale.setUniformf("u_grayness", 1f);
            batch.setShader(shaderGrayscale);
            batch.begin();
            //draw gray
            image.draw(this, x, y, w, h);
            //reset
            batch.end();
            batch.setShader(null);
            batch.begin();
        }
    }
    public void drawCardImage(Texture image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged) {
        if (!drawGrayscale) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
            if (damage_overlay != null && damaged)
                batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
        } else {
            batch.end();
            shaderGrayscale.bind();
            shaderGrayscale.setUniformf("u_grayness", 1f);
            batch.setShader(shaderGrayscale);
            batch.begin();
            //draw gray
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
            //reset
            batch.end();
            batch.setShader(null);
            batch.begin();
        }
    }
    public void drawCardImage(TextureRegion image, TextureRegion damage_overlay, float x, float y, float w, float h, boolean drawGrayscale, boolean damaged) {
        if (image != null) {
            if (!drawGrayscale) {
                batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                if (damage_overlay != null && damaged)
                    batch.draw(damage_overlay, adjustX(x), adjustY(y, h), w, h);
            } else {
                batch.end();
                shaderGrayscale.bind();
                shaderGrayscale.setUniformf("u_grayness", 1f);
                batch.setShader(shaderGrayscale);
                batch.begin();
                //draw gray
                batch.draw(image, adjustX(x), adjustY(y, h), w, h);
                //reset
                batch.end();
                batch.setShader(null);
                batch.begin();
            }
        }
    }
    public void drawGrayTransitionImage(FImage image, float x, float y, float w, float h, boolean withDarkOverlay, float percentage) {
        batch.end();
        shaderGrayscale.bind();
        shaderGrayscale.setUniformf("u_grayness", percentage);
        batch.setShader(shaderGrayscale);
        batch.begin();
        //draw gray
        image.draw(this, x, y, w, h);
        //reset
        batch.end();
        batch.setShader(null);
        batch.begin();
        if(withDarkOverlay){
            float oldalpha = alphaComposite;
            setAlphaComposite(0.4f);
            fillRect(Color.BLACK, x, y, w, h);
            setAlphaComposite(oldalpha);
        }
    }
    public void drawImage(FImage image, float x, float y, float w, float h) {
        drawImage(image, x, y, w, h, false);
    }
    public void drawImage(FImage image, float x, float y, float w, float h, boolean withDarkOverlay) {
        image.draw(this, x, y, w, h);
        if(withDarkOverlay){
            float oldalpha = alphaComposite;
            setAlphaComposite(0.4f);
            fillRect(Color.BLACK, x, y, w, h);
            setAlphaComposite(oldalpha);
        }
    }
    public void drawImage(Texture image, float x, float y, float w, float h) {
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
    }
    public void drawImage(TextureRegion image, float x, float y, float w, float h) {
        if (image != null)
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
    }
    public void drawImage(TextureRegion image, TextureRegion glowImageReference, float x, float y, float w, float h, Color glowColor, boolean selected) {
        if (image == null || glowImageReference == null)
            return;
         //1st image is the image on top of the shader, 2nd image is for the outline reference for the shader glow...
        // if the 1st image don't have transparency in the middle (only on the sides, top and bottom, use the 1st image as outline reference...
        if (!selected) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        } else {
            batch.end();
            shaderOutline.bind();
            shaderOutline.setUniformf("u_viewportInverse", new Vector2(1f / w, 1f / h));
            shaderOutline.setUniformf("u_offset", 3f);
            shaderOutline.setUniformf("u_step", Math.min(1f, w / 70f));
            shaderOutline.setUniformf("u_color", new Vector3(glowColor.r, glowColor.g, glowColor.b));
            batch.setShader(shaderOutline);
            batch.begin();
            //glow
            batch.draw(glowImageReference, adjustX(x), adjustY(y, h), w, h);
            batch.end();
            batch.setShader(null);
            batch.begin();
            //img
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
    }
    public void drawDeckBox(FImage cardArt, float scale, TextureRegion image, TextureRegion glowImageReference, float x, float y, float w, float h, Color glowColor, boolean selected) {
        if (image == null || glowImageReference == null)
            return;
        float yBox = y-(h*0.25f);
        if (!selected) {
            cardArt.draw(this,x+((w-w*scale)/2), y+((h-h*scale)/3f), w*scale, h*scale/1.85f);
            batch.draw(image, adjustX(x), adjustY(yBox, h), w, h);
        } else {
            batch.end();
            shaderOutline.bind();
            shaderOutline.setUniformf("u_viewportInverse", new Vector2(1f / w, 1f / h));
            shaderOutline.setUniformf("u_offset", 3f);
            shaderOutline.setUniformf("u_step", Math.min(1f, w / 70f));
            shaderOutline.setUniformf("u_color", new Vector3(glowColor.r, glowColor.g, glowColor.b));
            batch.setShader(shaderOutline);
            batch.begin();
            //glow
            batch.draw(glowImageReference, adjustX(x), adjustY(yBox, h), w, h);
            batch.end();
            batch.setShader(null);
            batch.begin();
            //cardart
            cardArt.draw(this,x+((w-w*scale)/2), y+((h-h*scale)/3f), w*scale, h*scale/1.85f);
            //deckbox
            batch.draw(image, adjustX(x), adjustY(yBox, h), w, h);
        }
    }

    public void drawRepeatingImage(Texture image, float x, float y, float w, float h) {
        if (startClip(x, y, w, h)) { //only render if clip successful, otherwise it will escape bounds
            int tilesW = (int)(w / image.getWidth()) + 1;
            int tilesH = (int)(h / image.getHeight()) + 1;
            batch.draw(image, adjustX(x), adjustY(y, h),
                    image.getWidth() * tilesW,
                    image.getHeight() * tilesH,
                    0, tilesH, tilesW, 0);
        }
        endClip();
    }

    //draw vertically flipped image
    public void drawFlippedImage(Texture image, float x, float y, float w, float h) {
        batch.draw(image, adjustX(x), adjustY(y, h), w, h, 0, 0, image.getWidth(), image.getHeight(), false, true);
    }

    public void drawImageWithTransforms(TextureRegion image, float x, float y, float w, float h, float rotation, boolean flipX, boolean flipY) {
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
        Dtransforms.addFirst(new Matrix4(batch.getTransformMatrix().idt())); //startshape is using this above as reference
        transformCount++;
        batch.getTransformMatrix().idt().translate(adjustX(originX), adjustY(originY, 0), 0).rotate(Vector3.Z, rotation).translate(-adjustX(originX), -adjustY(originY, 0), 0);
        batch.begin();
    }

    public void endTransform() {
        batch.end();
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix().idt());
        Dtransforms.removeFirst();
        transformCount--;
        if(transformCount != Dtransforms.size()) {
            System.err.println(String.format("Stack count: %d, transformCount: %d", Dtransforms.size(), transformCount));
            transformCount = 0;
            Dtransforms.clear();
        }
        batch.getTransformMatrix().idt(); //reset
        shapeRenderer.getTransformMatrix().idt(); //reset
        batch.begin();
    }

    public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation) {
        drawRotatedImage(image, x, y, w, h, originX, originY, 0, 0, image.getWidth(), image.getHeight(), rotation);
    }
    public void drawRotatedImage(TextureRegion image, float x, float y, float w, float h, float originX, float originY, float rotation) {
        drawRotatedImage(image.getTexture(), x, y, w, h, originX, originY, image.getRegionX(), image.getRegionY(), image.getRegionWidth(), image.getRegionHeight(), rotation);
    }
    public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, int srcX, int srcY, int srcWidth, int srcHeight, float rotation) {
        batch.draw(image, adjustX(x), adjustY(y, h), originX - x, h - (originY - y), w, h, 1, 1, rotation, srcX, srcY, srcWidth, srcHeight, false, false);
    }

    public void drawText(String text, FSkinFont font, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        drawText(text, font, skinColor.getColor(), x, y, w, h, wrap, horzAlignment, centerVertically);
    }
    public void drawText(String text, FSkinFont font, Color color, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        if (text == null)
            return;
        if (alphaComposite < 1) {
            color = FSkinColor.alphaColor(color, color.a * alphaComposite);
        }
        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }

        TextBounds textBounds;
        if (wrap) {
            textBounds = font.getWrappedBounds(text, w);
        }
        else {
            textBounds = font.getMultiLineBounds(text);
        }

        boolean needClip = false;

        while (textBounds.width > w || textBounds.height > h) {
            if (font.canShrink()) { //shrink font to fit if possible
                font = font.shrink();
                if (wrap) {
                    textBounds = font.getWrappedBounds(text, w);
                }
                else {
                    textBounds = font.getMultiLineBounds(text);
                }
            }
            else {
                needClip = true;
                break;
            }
        }

        if (needClip) { //prevent text flowing outside region if couldn't shrink it to fit
            startClip(x, y, w, h);
        }

        float textHeight = textBounds.height;
        if (h > textHeight && centerVertically) {
            y += (h - textHeight) / 2;
        }

        font.draw(batch, text, color, adjustX(x), adjustY(y, 0), w, wrap, horzAlignment);

        if (needClip) {
            endClip();
        }

        if (color.a < 1) {
            Gdx.gl.glDisable(GL_BLEND);
        }
    }

    //use nifty trick with multiple text renders to draw outlined text
    public void drawOutlinedText(String text, FSkinFont skinFont, Color textColor, Color outlineColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically) {
        drawOutlinedText(text, skinFont, textColor, outlineColor, x, y, w, h, wrap, horzAlignment, centerVertically, false);
    }
    public void drawOutlinedText(String text, FSkinFont skinFont, Color textColor, Color outlineColor, float x, float y, float w, float h, boolean wrap, int horzAlignment, boolean centerVertically, boolean shadow) {
        if (shadow) {
            float oldAlpha = alphaComposite;
            alphaComposite = 0.4f;
            drawText(text, skinFont, outlineColor, x - 1.5f, y + 1.5f, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x + 1.5f, y + 1.5f, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x + 1.5f, y - 1.5f, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x - 1.5f, y - 1.5f, w, h, wrap, horzAlignment, centerVertically);
            alphaComposite = oldAlpha;
        }
        drawText(text, skinFont, outlineColor, x - 1, y, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, outlineColor, x, y - 1, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, outlineColor, x - 1, y - 1, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, outlineColor, x + 1, y, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, outlineColor, x, y + 1, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, outlineColor, x + 1, y + 1, w, h, wrap, horzAlignment, centerVertically);
        drawText(text, skinFont, textColor, x, y, w, h, wrap, horzAlignment, centerVertically);
    }

    public float adjustX(float x) {
        return x + bounds.x;
    }

    public float adjustY(float y, float height) {
        return regionHeight - y - bounds.y - height; //flip y-axis
    }
    public Color borderLining(String c){
        if (c == null || c == "")
            return Color.valueOf("#fffffd");
        int c_r = Integer.parseInt(c.substring(0,2),16);
        int c_g = Integer.parseInt(c.substring(2,4),16);
        int c_b = Integer.parseInt(c.substring(4,6),16);
        int brightness = ((c_r * 299) + (c_g * 587) + (c_b * 114)) / 1000;
        return  brightness > 155 ? Color.valueOf("#171717") : Color.valueOf("#fffffd");
    }
}
