package forge.screens.match;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * Renders one tumbling die into an off-screen FrameBuffer.
 *
 * Two ways to make one, both generate their face texture at runtime (no atlas needed):
 *   numbered(sides, fbSize)  d4..d20, numbers drawn with 7-segment digits (6/9/16/19 underlined)
 *   planar(walk, chaos, fbSize)  cube with the two planar symbols from PNGs and four blank faces
 */
public class Dice3D implements Disposable {
    /** d6 value (index 0 = value 1) -> cube face index; opposite faces add up to 7. */
    public static final int[] D6_FACE = { 0, 2, 4, 5, 3, 1 };

    private static final int CELL = 256;      // pixels per face in the generated number texture
    private static final int COLS = 5;

    // 7-segment masks: a=1 top, b=2 top-right, c=4 bottom-right, d=8 bottom, e=16 bottom-left,
    // f=32 top-left, g=64 middle
    private static final int[] SEGMENTS = { 63, 6, 91, 79, 102, 109, 125, 7, 127, 111 };

    private final DiceShape shape;
    private final int[] labels;               // number on each face, null for art dice
    private final Texture ownedTexture;       // disposed with the die, may be null
    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch modelBatch;
    private final Environment env;
    private final PerspectiveCamera cam;
    private final FrameBuffer fb;
    private final TextureRegion region;

    private boolean rolling;
    private float time, duration, hold = 0.6f;
    private final Quaternion finalQ = new Quaternion();
    private final Quaternion spinQ = new Quaternion();
    private final Vector3 spinAxis = new Vector3();
    private float spinDegrees;
    private final Vector3 pos = new Vector3();

    // factories

    /** A die with faces numbered 1..sides (4 <= sides <= 20). A d6 is a cube with opposite faces adding up to 7. */
    public static Dice3D numbered(int sides, int fbSize) {
        DiceShape s = DiceShape.forSides(sides);
        int[] labels = new int[s.faceCount];
        if (sides == 6) {
            for (int v = 1; v <= 6; v++) labels[D6_FACE[v - 1]] = v;
        } else {
            for (int f = 0; f < labels.length; f++) labels[f] = f % sides + 1; // spare faces repeat
        }
        return generate(s, labels, null, fbSize);
    }

    /**
     * The planar die: a cube with the planeswalk symbol on face 0, chaos on face 1 and four blank faces (2..5).
     * The pixmaps are only read, the caller keeps ownership.
     */
    public static Dice3D planar(Pixmap walk, Pixmap chaos, int fbSize) {
        return generate(DiceShape.cube(), null, new Pixmap[] { walk, chaos, null, null, null, null }, fbSize);
    }

    private static Dice3D generate(DiceShape s, int[] labels, Pixmap[] art, int fbSize) {
        int faces = s.faceCount;
        int rows = (faces + COLS - 1) / COLS;
        int w = COLS * CELL, h = rows * CELL;

        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0.96f, 0.96f, 0.96f, 1f);
        pm.fill();

        float[][] uv = new float[faces][];
        for (int f = 0; f < faces; f++) {
            Vector2[] loc = s.local[f];
            float cx = (f % COLS) * CELL + CELL / 2f;
            float cy = (f / COLS) * CELL + CELL / 2f;
            float k = 0.48f * CELL / s.halfExtent[f]; // pixels per local unit

            float[] px = new float[loc.length * 2];
            uv[f] = new float[loc.length * 2];
            for (int i = 0; i < loc.length; i++) {
                px[2 * i] = cx + loc[i].x * k;
                px[2 * i + 1] = cy - loc[i].y * k; // pixmap y points down
                uv[f][2 * i] = px[2 * i] / w;
                uv[f][2 * i + 1] = px[2 * i + 1] / h;
            }

            if (art != null && art[f] != null) {
                float side = 2f * s.halfExtent[f] * k;
                pm.drawPixmap(art[f], 0, 0, art[f].getWidth(), art[f].getHeight(),
                        Math.round(cx - side / 2f), Math.round(cy - side / 2f), Math.round(side), Math.round(side));
            } else if (labels != null && labels[f] > 0) {
                float glyphH = s.inRadius[f] * k * (loc.length == 3 ? 0.9f : 1.1f);
                pm.setColor(Color.BLACK);
                drawNumber(pm, labels[f], cx, cy, glyphH);
            }

            pm.setColor(0.65f, 0.65f, 0.65f, 1f);
            for (int i = 0; i < loc.length; i++) {
                int j = (i + 1) % loc.length;
                stroke(pm, px[2 * i], px[2 * i + 1], px[2 * j], px[2 * j + 1], 2);
            }
        }

        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new Dice3D(s, t, uv, labels, t, fbSize);
    }

    // construction

    private Dice3D(DiceShape shape, Texture tex, float[][] uv, int[] labels, Texture owned, int fbSize) {
        this.shape = shape;
        this.labels = labels;
        this.ownedTexture = owned;

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material mat = new Material(TextureAttribute.createDiffuse(tex),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        MeshPartBuilder part = mb.part("die", GL20.GL_TRIANGLES,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);

        Vector2 tmp = new Vector2();
        for (int f = 0; f < shape.faceCount; f++) {
            Vector3[] p = shape.poly[f];
            short[] idx = new short[p.length];
            for (int i = 0; i < p.length; i++) {
                tmp.set(uv[f][2 * i], uv[f][2 * i + 1]);
                idx[i] = part.vertex(p[i], shape.normal[f], null, tmp);
            }
            for (int i = 1; i < p.length - 1; i++) {
                part.triangle(idx[0], idx[i], idx[i + 1]);
            }
        }

        model = mb.end();
        instance = new ModelInstance(model);

        modelBatch = new ModelBatch();
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.55f, 0.55f, 1f));
        env.add(new DirectionalLight().set(0.9f, 0.9f, 0.9f, -0.5f, -0.8f, -1f));

        cam = new PerspectiveCamera(40, fbSize, fbSize);
        cam.position.set(0, 0, 4.2f);
        cam.lookAt(0, 0, 0);
        cam.near = 0.1f;
        cam.far = 20f;
        cam.update();

        fb = new FrameBuffer(Pixmap.Format.RGBA8888, fbSize, fbSize, true);
        fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        region = new TextureRegion(fb.getColorBufferTexture());
        region.flip(false, true); // FBO textures are upside-down
    }

    // number drawing

    private static void drawNumber(Pixmap pm, int value, float cx, float cy, float h) {
        String s = Integer.toString(value);
        float dw = h * 0.6f, gap = h * 0.22f;
        float total = s.length() * dw + (s.length() - 1) * gap;
        float x = cx - total / 2f;
        int r = Math.max(2, Math.round(h * 0.06f));
        // 6, 9, 16, 19 read differently upside down, so they get an underline (nudge the number up to make room)
        boolean underline = value == 6 || value == 9 || value == 16 || value == 19;
        if (underline) {
            cy -= h * 0.07f;
            stroke(pm, x, cy + h / 2f + h * 0.17f, x + total, cy + h / 2f + h * 0.17f, r);
        }
        for (int i = 0; i < s.length(); i++) {
            int mask = SEGMENTS[s.charAt(i) - '0'];
            float l = x, rt = x + dw, t = cy - h / 2f, m = cy, b = cy + h / 2f;
            if ((mask & 1) != 0) stroke(pm, l, t, rt, t, r);
            if ((mask & 2) != 0) stroke(pm, rt, t, rt, m, r);
            if ((mask & 4) != 0) stroke(pm, rt, m, rt, b, r);
            if ((mask & 8) != 0) stroke(pm, l, b, rt, b, r);
            if ((mask & 16) != 0) stroke(pm, l, m, l, b, r);
            if ((mask & 32) != 0) stroke(pm, l, t, l, m, r);
            if ((mask & 64) != 0) stroke(pm, l, m, rt, m, r);
            x += dw + gap;
        }
    }

    /** Thick line with round caps, drawn as a trail of filled circles. */
    private static void stroke(Pixmap pm, float x0, float y0, float x1, float y1, int radius) {
        int steps = Math.max(1, (int) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            pm.fillCircle(Math.round(x0 + (x1 - x0) * t), Math.round(y0 + (y1 - y0) * t), radius);
        }
    }

    // rolling

    /** Rolls so that the face carrying {@code value} ends up facing the camera (numbered dice). */
    public void rollValue(int value, float durationSeconds) {
        if (labels == null) {
            roll(D6_FACE[value - 1], durationSeconds);
            return;
        }
        int count = 0;
        for (int l : labels) {
            if (l == value) count++;
        }
        int pick = MathUtils.random(Math.max(count, 1) - 1);
        int face = 0;
        for (int f = 0; f < labels.length; f++) {
            if (labels[f] == value && pick-- == 0) {
                face = f;
                break;
            }
        }
        roll(face, durationSeconds);
    }

    public void roll(int faceIndex, float durationSeconds) {
        duration = durationSeconds;
        time = 0;
        rolling = true;

        Vector3 n = shape.normal[faceIndex];
        Vector3 v = shape.up[faceIndex];

        // 1) turn the face normal towards the camera (+Z)
        if (n.z < -0.9999f) {
            finalQ.set(Vector3.X, 180f);
        } else {
            finalQ.setFromCross(n, Vector3.Z);
        }
        // 2) spin around Z so the face's "up" points to +Y (number/art upright)
        Vector3 vr = new Vector3(v);
        finalQ.transform(vr);
        float fix = 90f - MathUtils.atan2(vr.y, vr.x) * MathUtils.radiansToDegrees;
        finalQ.mulLeft(new Quaternion(Vector3.Z, fix));
        Quaternion tilt = new Quaternion(Vector3.Z, MathUtils.random(-12f, 12f));
        finalQ.mulLeft(tilt);

        spinAxis.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-0.5f, 0.5f));
        if (spinAxis.isZero(0.01f)) spinAxis.set(1, 1, 0);
        spinAxis.nor();
        spinDegrees = MathUtils.random(540f, 900f);
        applyTransform(0f);
    }

    private void applyTransform(float p) {
        float e = Interpolation.pow3Out.apply(p);
        spinQ.set(spinAxis, (1f - e) * spinDegrees).mul(finalQ);
        float y = Math.abs(MathUtils.sin(p * MathUtils.PI * 3f)) * 0.5f * (1f - p);
        pos.set(0, y, 0);
        instance.transform.set(pos, spinQ);
    }

    public void update(float delta) {
        if (rolling) {
            time += delta;
            applyTransform(Math.min(time / duration, 1f));
        }
        fb.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        modelBatch.begin(cam);
        modelBatch.render(instance, env);
        modelBatch.end();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        fb.end();
    }

    public void skip() {
        time = duration + hold;
        applyTransform(1f);
    }

    public boolean isDone() {
        return rolling && time >= duration + hold;
    }

    public float getAlpha() {
        if (!rolling) {
            return 1f;
        }
        float in = Math.min(time / 0.12f, 1f);
        float out = Math.min((duration + hold - time) / 0.25f, 1f);
        return MathUtils.clamp(Math.min(in, out), 0f, 1f);
    }

    public TextureRegion getRegion() {
        return region;
    }

    @Override
    public void dispose() {
        fb.dispose();
        modelBatch.dispose();
        model.dispose();
        if (ownedTexture != null) {
            ownedTexture.dispose();
        }
    }
}