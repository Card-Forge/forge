package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
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
import forge.localinstance.properties.ForgeConstants;

/**
 * One tumbling die, rendered into an off-screen FrameBuffer.
 *
 * Split in two so several dice of a roll can share the expensive parts:
 *   Skin    shape + generated face texture + model. Create one per roll, share it between the dice, dispose it after.
 *   Dice3D  one die: its own transform and FrameBuffer, but a shared ModelBatch, camera and light.
 *
 * Android-friendly bits: the ModelBatch (shader) is created once, FrameBuffers are pooled, the die is opaque,
 * and the FrameBuffer is only redrawn when the die actually moved. Call releaseShared() on shutdown.
 */
public class Dice3D implements Disposable {
    private static final String DICE_MATERIAL = ForgeConstants.RES_DIR + "skins/default/dice_bone.png";

    // ------------------------------------------------------------------ look & lighting (tweak here)
    // The face turned to the camera gets roughly AMBIENT + 0.73 * KEY_LIGHT; keep that near 1.0-1.1 so the result
    // face shows the texture at its true colour. This light set is shared by all dice (the coin has its own).
    private static final float AMBIENT = 0.55f;
    private static final float KEY_LIGHT = 0.9f;
    private static final float[] KEY_DIR = { -0.5f, -0.8f, -1f };   // direction the light travels (from upper right)
    private static final float FILL_LIGHT = 0.2f;                    // 0 = off; soft light from the other side
    private static final float[] FILL_DIR = { 0.7f, 0.2f, -0.5f };
    private static final float SPECULAR = 0.15f;                     // 0 = fully matte, ~0.5 = polished
    private static final float SHININESS = 12f;                      // low = broad sheen, high = tight glint
    private static final float[] TINT = { 1f, 1f, 1f };              // multiplies the texture, e.g. 0.95, 0.9, 0.8 = warmer

    /** d6 value (index 0 = value 1) -> cube face index; opposite faces add up to 7. */
    public static final int[] D6_FACE = { 0, 2, 4, 5, 3, 1 };

    private static final int COLS = 5;
    private static final int CORNER_STEPS = 12; // grid resolution per face, finer = smoother corner edge
    // 7-segment masks: a=1 top, b=2 top-right, c=4 bottom-right, d=8 bottom, e=16 bottom-left,
    // f=32 top-left, g=64 middle
    private static final int[] SEGMENTS = { 63, 6, 91, 79, 102, 109, 125, 7, 127, 111 };

    // ------------------------------------------------------------------ shared GL objects

    static ModelBatch sharedBatch;
    private static Environment sharedEnv;
    static PerspectiveCamera sharedCam;
    private static final List<FrameBuffer> POOL = new ArrayList<>();
    private static int poolSize;

    static void ensureShared() {
        if (sharedBatch == null) {
            sharedBatch = new ModelBatch();
            sharedEnv = new Environment();
            sharedEnv.set(new ColorAttribute(ColorAttribute.AmbientLight, AMBIENT, AMBIENT, AMBIENT, 1f));
            sharedEnv.add(new DirectionalLight().set(KEY_LIGHT, KEY_LIGHT, KEY_LIGHT, KEY_DIR[0], KEY_DIR[1], KEY_DIR[2]));
            if (FILL_LIGHT > 0f) {
                sharedEnv.add(new DirectionalLight().set(FILL_LIGHT, FILL_LIGHT, FILL_LIGHT * 1.15f,
                        FILL_DIR[0], FILL_DIR[1], FILL_DIR[2]));
            }
            sharedCam = new PerspectiveCamera(40, 1, 1);
            sharedCam.position.set(0, 0, 4.2f);
            sharedCam.lookAt(0, 0, 0);
            sharedCam.near = 0.1f;
            sharedCam.far = 20f;
            sharedCam.update();
        }
    }

    static FrameBuffer acquire(int size) {
        if (size != poolSize) { // size changed (rotation, different dice count): drop the old ones
            for (FrameBuffer f : POOL) f.dispose();
            POOL.clear();
            poolSize = size;
        }
        FrameBuffer fb = POOL.isEmpty() ? new FrameBuffer(Pixmap.Format.RGBA8888, size, size, true)
                : POOL.remove(POOL.size() - 1);
        fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return fb;
    }

    static void release(FrameBuffer fb, int size) {
        if (size == poolSize) {
            POOL.add(fb);
        } else {
            fb.dispose();
        }
    }

    // The bone texture is decoded once and kept (a Pixmap lives in CPU memory, so it survives GL context loss).
    private static Pixmap boneCache;
    private static boolean boneTried;

    private static Pixmap bone() {
        if (!boneTried) {
            boneTried = true;
            try {
                if (Gdx.files.absolute(DICE_MATERIAL).exists()) {
                    boneCache = new Pixmap(Gdx.files.absolute(DICE_MATERIAL));
                }
            } catch (Exception e) {
                Gdx.app.error("Dice3D", "Could not load material texture asset from path: " + DICE_MATERIAL, e);
            }
        }
        return boneCache;
    }

    /** Frees the shared ModelBatch, pooled FrameBuffers and the cached texture (call when the overlay is disposed). */
    public static void releaseShared() {
        if (sharedBatch != null) {
            sharedBatch.dispose();
            sharedBatch = null;
            sharedEnv = null;
            sharedCam = null;
        }
        for (FrameBuffer f : POOL) f.dispose();
        POOL.clear();
        if (boneCache != null) {
            boneCache.dispose();
            boneCache = null;
        }
        boneTried = false;
    }

    // ------------------------------------------------------------------ skin

    public static final class Skin implements Disposable {
        final DiceShape shape;
        final int[] labels;      // number on each face, null for the planar die
        private final Texture texture;
        private final Model model;

        /** Faces numbered 1..sides (4..20). cell = texture pixels per face (128 or 256). */
        public static Skin numbered(int sides, int cell) {
            DiceShape s = DiceShape.forSides(sides);
            int[] labels = new int[s.faceCount];
            if (sides == 6) {
                for (int v = 1; v <= 6; v++) labels[D6_FACE[v - 1]] = v; // opposite faces add up to 7
            } else {
                for (int f = 0; f < labels.length; f++) labels[f] = f % sides + 1; // spare faces repeat
            }
            return new Skin(s, labels, null, cell);
        }

        /** Cube with the planeswalk symbol on face 0, chaos on face 1 and four blank faces. Pixmaps are only read. */
        public static Skin planar(Pixmap walk, Pixmap chaos, int cell) {
            return new Skin(DiceShape.cube(), null, new Pixmap[] { walk, chaos, null, null, null, null }, cell);
        }

        private Skin(DiceShape s, int[] labels, Pixmap[] art, int cell) {
            this.shape = s;
            this.labels = labels;
            int faces = s.faceCount;
            int rows = (faces + COLS - 1) / COLS;
            int w = COLS * cell, h = rows * cell;

            Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            Pixmap boneTexture = bone(); // cached, shared: do not dispose here

            if (boneTexture != null) {
                for (int tx = 0; tx < w; tx += boneTexture.getWidth()) {
                    for (int ty = 0; ty < h; ty += boneTexture.getHeight()) {
                        pm.drawPixmap(boneTexture, tx, ty);
                    }
                }
            } else {
                // Safe fallback color tint if the .png file is accidentally deleted or missing
                pm.setColor(0.96f, 0.96f, 0.96f, 1f);
                pm.fill();
            }

            float[][] uv = new float[faces][];
            int line = Math.max(1, cell / 128);
            for (int f = 0; f < faces; f++) {
                Vector2[] loc = s.local[f];
                float cx = (f % COLS) * cell + cell / 2f;
                float cy = (f / COLS) * cell + cell / 2f;
                float k = 0.48f * cell / s.halfExtent[f]; // pixels per local unit

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
                            Math.round(cx - side / 2f), Math.round(cy - side / 2f),
                            Math.round(side), Math.round(side));
                } else if (labels != null && labels[f] > 0) {
                    float glyphH = s.inRadius[f] * k * (loc.length == 3 ? 0.9f : 1.1f);
                    pm.setColor(Color.BLACK);
                    drawNumber(pm, labels[f], cx, cy, glyphH);
                }

                pm.setColor(0.65f, 0.65f, 0.65f, 1f);
                for (int i = 0; i < loc.length; i++) {
                    int j = (i + 1) % loc.length;
                    stroke(pm, px[2 * i], px[2 * i + 1], px[2 * j], px[2 * j + 1], line);
                }
            }

            texture = new Texture(pm);
            pm.dispose();
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // opaque material: no blending needed (the overlay fades the finished picture instead)
            ModelBuilder mb = new ModelBuilder();
            mb.begin();
            Material mat = new Material(TextureAttribute.createDiffuse(texture),
                    ColorAttribute.createDiffuse(TINT[0], TINT[1], TINT[2], 1f),
                    ColorAttribute.createSpecular(SPECULAR, SPECULAR, SPECULAR * 0.9f, 1f),
                    FloatAttribute.createShininess(SHININESS));
            MeshPartBuilder part = mb.part("die", GL20.GL_TRIANGLES,
                    Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);
            for (int f = 0; f < faces; f++) {
                if (s.cornerRadius > 0f) {
                    buildBevelFace(part, s, f, uv[f]);
                } else {
                    buildFlatFace(part, s, f, uv[f]);
                }
            }
            model = mb.end();
        }

        @Override
        public void dispose() {
            model.dispose();
            texture.dispose();
        }
    }

    private static void buildFlatFace(MeshPartBuilder part, DiceShape s, int f, float[] uv) {
        Vector3[] p = s.poly[f];
        Vector2 tmp = new Vector2();
        short[] idx = new short[p.length];
        for (int i = 0; i < p.length; i++) {
            tmp.set(uv[2 * i], uv[2 * i + 1]);
            idx[i] = part.vertex(p[i], s.normal[f], null, tmp);
        }
        for (int i = 1; i < p.length - 1; i++) {
            part.triangle(idx[0], idx[i], idx[i + 1]);
        }
    }

    /**
     * Cube face with rounded CORNERS only: the quad is subdivided and every point that lies farther from the
     * centre than the clip radius is pulled back onto that sphere. Faces and edges stay flat and sharp, only the
     * corner tips are trimmed. UVs stay those of the flat quad.
     */
    private static void buildBevelFace(MeshPartBuilder part, DiceShape s, int f, float[] uv) {
        final float clip = s.cornerRadius;
        final int n = CORNER_STEPS;
        Vector3[] c = s.poly[f];
        short[][] grid = new short[n + 1][n + 1];
        Vector3 p = new Vector3();
        Vector2 t = new Vector2();
        for (int i = 0; i <= n; i++) {
            float a = i / (float) n;
            for (int j = 0; j <= n; j++) {
                float b = j / (float) n;
                float w0 = (1 - a) * (1 - b), w1 = a * (1 - b), w2 = a * b, w3 = (1 - a) * b;
                p.set(0, 0, 0).mulAdd(c[0], w0).mulAdd(c[1], w1).mulAdd(c[2], w2).mulAdd(c[3], w3);
                t.set(uv[0] * w0 + uv[2] * w1 + uv[4] * w2 + uv[6] * w3,
                        uv[1] * w0 + uv[3] * w1 + uv[5] * w2 + uv[7] * w3);
                float len = p.len();
                if (len > clip) {
                    Vector3 pos = new Vector3(p).scl(clip / len);
                    grid[i][j] = part.vertex(pos, new Vector3(pos).nor(), null, t); // spherical cap
                } else {
                    grid[i][j] = part.vertex(p, s.normal[f], null, t);              // flat face
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                part.triangle(grid[i][j], grid[i + 1][j], grid[i + 1][j + 1]);
                part.triangle(grid[i][j], grid[i + 1][j + 1], grid[i][j + 1]);
            }
        }
    }

    // ------------------------------------------------------------------ number drawing

    private static void drawNumber(Pixmap pm, int value, float cx, float cy, float h) {
        String s = Integer.toString(value);
        float dw = h * 0.6f, gap = h * 0.22f;
        // a "1" is just a vertical bar: give it a narrow slot so single and double digits stay centred
        float[] widths = new float[s.length()];
        float total = (s.length() - 1) * gap;
        for (int i = 0; i < widths.length; i++) {
            widths[i] = s.charAt(i) == '1' ? dw * 0.25f : dw;
            total += widths[i];
        }
        float x = cx - total / 2f;
        int r = Math.max(1, Math.round(h * 0.06f));
        // 6, 9, 16, 19 read differently upside down, so they get an underline (nudge the number up to make room)
        if (value == 6 || value == 9 || value == 16 || value == 19) {
            cy -= h * 0.07f;
            float uy = cy + h / 2f + h * 0.17f;
            stroke(pm, x, uy, x + total, uy, r);
        }
        for (int i = 0; i < s.length(); i++) {
            int digit = s.charAt(i) - '0';
            float t = cy - h / 2f, m = cy, b = cy + h / 2f;
            if (digit == 1) {
                float mid = x + widths[i] / 2f;
                stroke(pm, mid, t, mid, b, r);
            } else {
                int mask = SEGMENTS[digit];
                float l = x, rt = x + widths[i];
                if ((mask & 1) != 0) stroke(pm, l, t, rt, t, r);
                if ((mask & 2) != 0) stroke(pm, rt, t, rt, m, r);
                if ((mask & 4) != 0) stroke(pm, rt, m, rt, b, r);
                if ((mask & 8) != 0) stroke(pm, l, b, rt, b, r);
                if ((mask & 16) != 0) stroke(pm, l, m, l, b, r);
                if ((mask & 32) != 0) stroke(pm, l, t, l, m, r);
                if ((mask & 64) != 0) stroke(pm, l, m, rt, m, r);
            }
            x += widths[i] + gap;
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

    // ------------------------------------------------------------------ the die

    private final Skin skin;
    private final ModelInstance instance;
    private final FrameBuffer fb;
    private final int fbSize;
    private final TextureRegion region;

    private boolean rolling, settled, dirty = true;
    private float time, duration, hold = 0.6f;
    private final Quaternion finalQ = new Quaternion();
    private final Quaternion spinQ = new Quaternion();
    private final Vector3 spinAxis = new Vector3();
    private float spinDegrees;
    private final Vector3 pos = new Vector3();

    public Dice3D(Skin skin, int fbSize) {
        ensureShared();
        this.skin = skin;
        this.fbSize = fbSize;
        this.instance = new ModelInstance(skin.model);
        this.fb = acquire(fbSize);
        this.region = new TextureRegion(fb.getColorBufferTexture());
        this.region.flip(false, true); // FBO textures are upside-down
    }

    /** Rolls so that the face carrying {@code value} ends up facing the camera. */
    public void rollValue(int value, float durationSeconds) {
        int[] labels = skin.labels;
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
        settled = false;

        Vector3 n = skin.shape.normal[faceIndex];
        Vector3 v = skin.shape.up[faceIndex];

        // 1) turn the face normal towards the camera (+Z)
        if (n.z < -0.9999f) {
            finalQ.set(Vector3.X, 180f);
        } else {
            finalQ.setFromCross(n, Vector3.Z);
        }
        // 2) spin around Z so the face's "up" points to +Y (number upright)
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
        dirty = true;
    }

    public void update(float delta) {
        if (rolling && !settled) {
            time += delta;
            float p = Math.min(time / duration, 1f);
            applyTransform(p);
            if (p >= 1f) settled = true;
        } else if (rolling) {
            time += delta; // holding the result
        }
        if (!dirty) {
            return; // nothing moved: keep last picture, skip all GL work
        }
        dirty = false;

        fb.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        sharedBatch.begin(sharedCam);
        sharedBatch.render(instance, sharedEnv);
        sharedBatch.end();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        fb.end();
    }

    public void skip() {
        time = duration + hold;
        settled = true;
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

    /** Returns the FrameBuffer to the pool. The Skin is owned (and disposed) by the caller. */
    @Override
    public void dispose() {
        release(fb, fbSize);
    }
}