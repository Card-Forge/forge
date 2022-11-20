package forge;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
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
    private final ShaderProgram shaderOutline = new ShaderProgram(Gdx.files.internal("shaders").child("outline.vert"), Gdx.files.internal("shaders").child("outline.frag"));
    private final ShaderProgram shaderGrayscale = new ShaderProgram(Gdx.files.internal("shaders").child("grayscale.vert"), Gdx.files.internal("shaders").child("grayscale.frag"));
    private final ShaderProgram shaderWarp = new ShaderProgram(Gdx.files.internal("shaders").child("grayscale.vert"), Gdx.files.internal("shaders").child("warp.frag"));
    private final ShaderProgram shaderUnderwater = new ShaderProgram(Gdx.files.internal("shaders").child("grayscale.vert"), Gdx.files.internal("shaders").child("underwater.frag"));
    private static String vertexShaderDayNight = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "uniform float u_timeOfDay;\n"
            + "varying vec4 v_color;\n"
            + "varying vec4 v_tweak;\n"
            + "varying vec2 v_texCoords;\n"
            + "varying float v_lightFix;\n"
            + "const vec3 forward = vec3(1.0 / 3.0);\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "   float st = sin(1.5707963 * sin(0.2617994 * u_timeOfDay)); // Whenever st is very high or low... \n"
            + "   float ct = sin(1.5707963 * cos(0.2617994 * u_timeOfDay)); // ...ct is close to 0, and vice versa. \n"
            + "   float dd = ct * ct; // Positive, small; used for dawn and dusk. \n"
            + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "   v_color.w = v_color.w * (255.0/254.0);\n"
            + "   vec3 oklab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *"
            + "     pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n"
            + "     * (v_color.rgb * v_color.rgb), forward);\n"
            + "   // The next four lines make use of the time-based variables st, ct, and dd. Edit to fit. \n"
            + "   v_color.x = clamp(oklab.x + (0.0625 * st), 0.0, 1.0);\n"
            + "   v_color.yz = clamp(oklab.yz + vec2(0.0625 * dd + 0.03125 * st, 0.1 * st), -1.0, 1.0) * ((dd + 0.25) * 0.5);\n"
            + "   v_tweak = vec4(0.2 * st + 0.5);\n"
            + "   v_tweak.w = pow((1.0 - 0.125 * st), 1.709);\n"
            + "   v_lightFix = 1.0 + pow(v_tweak.w, 1.41421356);\n"
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "}\n";
    private static String fragmentShaderDayNight =
            "#ifdef GL_ES\n" +
                    "#define LOWP lowp\n" +
                    "precision mediump float;\n" +
                    "#else\n" +
                    "#define LOWP \n" +
                    "#endif\n" +
                    "varying vec2 v_texCoords;\n" +
                    "varying LOWP vec4 v_color;\n" +
                    "varying LOWP vec4 v_tweak;\n" +
                    "varying float v_lightFix;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "const vec3 forward = vec3(1.0 / 3.0);\n" +
                    "void main()\n" +
                    "{\n" +
                    "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
                    "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
                    "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
                    "             * (tgt.rgb * tgt.rgb), forward);\n" +
                    "  lab.x = clamp(pow(lab.x, v_tweak.w) * v_lightFix * v_tweak.x + v_color.x - 1.0, 0.0, 1.0);\n" +
                    "  lab.yz = clamp((lab.yz * v_tweak.yz + v_color.yz) * 1.5, -1.0, 1.0);\n" +
                    "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
                    "  gl_FragColor = vec4(sqrt(clamp(" +
                    "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
                    "                 (lab * lab * lab)," +
                    "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
                    "}";
    private final ShaderProgram shaderNightDay = new ShaderProgram(vertexShaderDayNight, fragmentShaderDayNight);

    private Texture dummyTexture = null;

    public Graphics() {
        ShaderProgram.pedantic = false;
    }

    public ShaderProgram getShaderOutline() {
        return shaderOutline;
    }

    public ShaderProgram getShaderGrayscale() {
        return shaderGrayscale;
    }

    public ShaderProgram getShaderWarp() {
        return shaderWarp;
    }

    public ShaderProgram getShaderUnderwater() {
        return shaderUnderwater;
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
        shaderGrayscale.dispose();
        shaderUnderwater.dispose();
        shaderWarp.dispose();
        if(dummyTexture != null) dummyTexture.dispose();
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

    public void drawLineArrow(float arrowThickness, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
        fillCircle(skinColor.getColor(), x2, y2, arrowThickness);
        drawLineArrow(arrowThickness, skinColor.getColor(), x1, y1, x2, y2);
        fillCircle(Color.WHITE, x2, y2, arrowThickness/2);
        drawLine(arrowThickness/3, Color.WHITE, x1, y1, x2, y2);
    }
    public void drawLineArrow(float thickness, Color color, float x1, float y1, float x2, float y2) {
        batch.end(); //must pause batch while rendering shapes

        float angle = new Vector2(x1 - x2, y1 - y2).angleRad();
        float arrowHeadRotation = (float)(Math.PI * 0.8f);
        Vector2 arrowCorner3 = new Vector2(x2 + (thickness/3) * (float)Math.cos(angle + arrowHeadRotation), y2 + (thickness/3) * (float)Math.sin(angle + arrowHeadRotation));
        Vector2 arrowCorner4 = new Vector2(x2 + (thickness/3) * (float)Math.cos(angle - arrowHeadRotation), y2 + (thickness/3) * (float)Math.sin(angle - arrowHeadRotation));

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
            if (image != null) {
                image.draw(this, x, y, w, h);
                fillRoundRect(borderColor, x, y, w, h, (h - w) / 10);//show corners edges
            }
        }
        setAlphaComposite(oldalpha);
    }
    public void drawborderImage(Color borderColor, float x, float y, float w, float h) {
        float oldalpha = alphaComposite;
        fillRoundRect(borderColor, x, y, w, h, (h-w)/12);
        setAlphaComposite(oldalpha);
    }
    public void drawImage(FImage image, Color borderColor, float x, float y, float w, float h) {
        if (image == null)
            return;
        image.draw(this, x, y, w, h);
        fillRoundRect(borderColor, x+1, y+1, w-1.5f, h-1.5f, (h-w)/10);//used by zoom let some edges show...
    }
    public void drawAvatarImage(FImage image, float x, float y, float w, float h, boolean drawGrayscale) {
        if (image == null)
            return;
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
        if (image == null)
            return;
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
        if (image == null)
            return;
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
    public void drawGrayTransitionImage(Texture image, float x, float y, float w, float h, boolean withDarkOverlay, float percentage) {
        batch.end();
        shaderGrayscale.bind();
        shaderGrayscale.setUniformf("u_grayness", percentage);
        batch.setShader(shaderGrayscale);
        batch.begin();
        //draw gray
        batch.draw(image, x, y, w, h);
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
    public void drawGrayTransitionImage(TextureRegion image, float x, float y, float w, float h, boolean withDarkOverlay, float percentage) {
        batch.end();
        shaderGrayscale.bind();
        shaderGrayscale.setUniformf("u_grayness", percentage);
        batch.setShader(shaderGrayscale);
        batch.begin();
        //draw gray
        batch.draw(image, x, y, w, h);
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
    public void drawWarpImage(Texture image, float x, float y, float w, float h, float time) {
        batch.end();
        shaderWarp.bind();
        shaderWarp.setUniformf("u_amount", 0.2f);
        shaderWarp.setUniformf("u_speed", 0.5f);
        shaderWarp.setUniformf("u_time", time);
        batch.setShader(shaderWarp);
        batch.begin();
        //draw
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        //reset
        batch.end();
        batch.setShader(null);
        batch.begin();
    }
    public void drawWarpImage(TextureRegion image, float x, float y, float w, float h, float time) {
        batch.end();
        shaderWarp.bind();
        shaderWarp.setUniformf("u_amount", 0.2f);
        shaderWarp.setUniformf("u_speed", 0.6f);
        shaderWarp.setUniformf("u_time", time);
        batch.setShader(shaderWarp);
        batch.begin();
        //draw
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        //reset
        batch.end();
        batch.setShader(null);
        batch.begin();
    }
    public void drawWarpImage(FImage image, float x, float y, float w, float h, float time) {
        if (image == null)
            return;
        batch.end();
        shaderWarp.bind();
        shaderWarp.setUniformf("u_amount", 0.2f);
        shaderWarp.setUniformf("u_speed", 0.6f);
        shaderWarp.setUniformf("u_time", time);
        batch.setShader(shaderWarp);
        batch.begin();
        //draw
        image.draw(this, x, y, w, h);
        //reset
        batch.end();
        batch.setShader(null);
        batch.begin();
    }
    public void drawUnderWaterImage(FImage image, float x, float y, float w, float h, float time, boolean withDarkOverlay) {
        if (image == null)
            return;
        batch.end();
        shaderUnderwater.bind();
        shaderUnderwater.setUniformf("u_amount", 10f*time);
        shaderUnderwater.setUniformf("u_speed", 0.5f*time);
        shaderUnderwater.setUniformf("u_time", time);
        batch.setShader(shaderUnderwater);
        batch.begin();
        //draw
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
    public void drawNightDay(FImage image, float x, float y, float w, float h, Float time) {
        if (image == null)
            return;
        if (time != null) {
            batch.end();
            shaderNightDay.bind();
            shaderNightDay.setUniformf("u_timeOfDay", time);
            batch.setShader(shaderNightDay);
            batch.begin();
            //draw
            image.draw(this, x, y, w, h);
            //reset
            batch.end();
            batch.setShader(null);
            batch.begin();
        } else {
            drawImage(image, x, y, w, h);
        }
    }
    public void drawUnderWaterImage(TextureRegion image, float x, float y, float w, float h, float time) {
        batch.end();
        shaderUnderwater.bind();
        shaderUnderwater.setUniformf("u_amount", 10f);
        shaderUnderwater.setUniformf("u_speed", 0.5f);
        shaderUnderwater.setUniformf("u_time", time);
        batch.setShader(shaderUnderwater);
        batch.begin();
        //draw
        batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        //reset
        batch.end();
        batch.setShader(null);
        batch.begin();
    }
    public void drawImage(FImage image, float x, float y, float w, float h) {
        drawImage(image, x, y, w, h, false);
    }
    public void drawImage(FImage image, float x, float y, float w, float h, boolean withDarkOverlay) {
        if (image == null)
            return;
        image.draw(this, x, y, w, h);
        if(withDarkOverlay){
            float oldalpha = alphaComposite;
            setAlphaComposite(0.4f);
            fillRect(Color.BLACK, x, y, w, h);
            setAlphaComposite(oldalpha);
        }
    }
    public void drawImage(Texture image, float x, float y, float w, float h) {
        if (image != null)
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
        if (image == null)
            return;
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
        try {
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
        } catch (Exception e) {
            //shouldnt be here but force English on CJK Error
            Forge.setForcedEnglishonCJKMissing();
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

    public Texture getDummyTexture(){
        if (dummyTexture == null){
            Pixmap P = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            P.setColor(1f,1f,1f,1f);
            P.drawPixel(0, 0);
            dummyTexture = new Texture(P);
            P.dispose();
        }
        return dummyTexture;
    }
}
