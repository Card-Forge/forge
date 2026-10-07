package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * A coin with real thickness: a chamfered, reeded rim and two flat faces carrying the heads/tails art.
 * Rendered into a pooled FrameBuffer through the ModelBatch and camera shared with Dice3D.
 *
 * Heads is on the +Z face, tails on the -Z face. The flip spins around the X axis, so a whole number of
 * turns shows heads and "+ half a turn" shows tails. At rest the coin is tilted a little towards the viewer
 * so its edge stays visible.
 *
 * All methods must be called on the GL (render) thread.
 */
final class Coin3D implements Disposable {
    /** Coin radius in world units (the shared camera sees FRAME world units across the FrameBuffer at z=0). */
    static final float R = 0.8f;
    static final float FRAME = (float) (2.0 * 4.2 * Math.tan(Math.toRadians(20.0)));

    private static final float H = 0.055f;         // half thickness
    private static final float INSET = 0.03f;      // how far the flat face is inset from the rim radius
    private static final float CHAMFER = 0.025f;   // height of the bevel between rim and face
    private static final int SEGMENTS = 72;
    private static final int REEDS = 60;           // ridges around the edge
    /** Fraction of the face texture (measured from its centre) that maps onto the face disc. Lower it if your PNGs have a transparent margin. */
    private static final float ART_RADIUS = 1.0f;

    /** Metal colour of the rim, reeding and the backing under the faces. */
    private static final Color METAL = new Color(0.86f, 0.70f, 0.28f, 1f);
    private static final float REST_TILT_X = -20f; // degrees: top leans away
    private static final float REST_TILT_Y = 14f;  // degrees

    private final Model model;
    private final ModelInstance instance;
    private final Environment env;
    private final FrameBuffer fb;
    private final int fbSize;
    private final TextureRegion region;
    private final List<Texture> owned = new ArrayList<>();

    private final Quaternion q = new Quaternion();
    private final Quaternion tmp = new Quaternion();
    private final Quaternion rest = new Quaternion();
    private final Vector3 pos = new Vector3();

    /** heads / tails may be null: plain metal faces are generated instead. They are never disposed here. */
    Coin3D(Texture heads, Texture tails, int fbSize) {
        Dice3D.ensureShared();
        this.fbSize = fbSize;

        if (heads == null) {
            heads = faceFallback(new Color(0.95f, 0.80f, 0.25f, 1f));
        }
        if (tails == null) {
            tails = faceFallback(new Color(0.78f, 0.78f, 0.84f, 1f));
        }
        Texture reed = reedTexture();
        owned.add(reed);

        final long attrs = Usage.Position | Usage.Normal | Usage.TextureCoordinates;
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        Material rimMat = new Material(TextureAttribute.createDiffuse(reed),
                ColorAttribute.createSpecular(0.55f, 0.55f, 0.5f, 1f), FloatAttribute.createShininess(24f));
        buildRim(mb.part("rim", GL20.GL_TRIANGLES, attrs, rimMat));

        // solid metal under the faces, so transparent parts of the art never show the background
        Material metal = new Material(ColorAttribute.createDiffuse(METAL),
                ColorAttribute.createSpecular(0.5f, 0.5f, 0.45f, 1f), FloatAttribute.createShininess(24f));
        MeshPartBuilder base = mb.part("base", GL20.GL_TRIANGLES, attrs, metal);
        buildDisc(base, H - 0.004f, true, false);
        buildDisc(base, -(H - 0.004f), false, false);

        Material headsMat = faceMaterial(heads);
        buildDisc(mb.part("heads", GL20.GL_TRIANGLES, attrs, headsMat), H, true, true);
        Material tailsMat = faceMaterial(tails);
        buildDisc(mb.part("tails", GL20.GL_TRIANGLES, attrs, tailsMat), -H, false, true);

        model = mb.end();
        instance = new ModelInstance(model);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.50f, 0.50f, 0.50f, 1f));
        env.add(new DirectionalLight().set(0.85f, 0.85f, 0.85f, -0.4f, -0.7f, -1f));
        env.add(new DirectionalLight().set(0.30f, 0.30f, 0.35f, 0.8f, 0.3f, -0.6f)); // soft rim light

        rest.set(Vector3.Y, REST_TILT_Y).mul(new Quaternion(Vector3.X, REST_TILT_X));

        fb = Dice3D.acquire(fbSize);
        region = new TextureRegion(fb.getColorBufferTexture());
        region.flip(false, true); // FBO textures are upside-down
    }

    private static Material faceMaterial(Texture t) {
        return new Material(TextureAttribute.createDiffuse(t),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                ColorAttribute.createSpecular(0.25f, 0.25f, 0.25f, 1f), FloatAttribute.createShininess(16f));
    }

    // ---------------------------------------------------------------- geometry

    /** Rim: front bevel, straight reeded side, back bevel, as one strip around the coin. */
    private static void buildRim(MeshPartBuilder p) {
        final float capR = R - INSET, zOut = H - CHAMFER;
        // profile points from the front face to the back face: radius, z, normal radial part, normal z part
        float bn = (float) Math.sqrt(CHAMFER * CHAMFER + INSET * INSET);
        float nr = CHAMFER / bn, nz = INSET / bn; // outward normal of the bevel
        float[][] prof = {
                { capR, H, nr, nz },
                { R, zOut, 1f, 0f },
                { R, -zOut, 1f, 0f },
                { capR, -H, nr, -nz } };

        final int n = SEGMENTS;
        short[][] idx = new short[n + 1][prof.length];
        Vector3 pos = new Vector3(), nor = new Vector3();
        com.badlogic.gdx.math.Vector2 uv = new com.badlogic.gdx.math.Vector2();
        for (int i = 0; i <= n; i++) {
            float a = MathUtils.PI2 * i / n;
            float c = MathUtils.cos(a), s = MathUtils.sin(a);
            for (int k = 0; k < prof.length; k++) {
                pos.set(prof[k][0] * c, prof[k][0] * s, prof[k][1]);
                nor.set(prof[k][2] * c, prof[k][2] * s, prof[k][3]).nor();
                uv.set(i * REEDS / (float) n, k / (float) (prof.length - 1));
                idx[i][k] = p.vertex(pos, nor, null, uv);
            }
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < prof.length - 1; k++) {
                short a = idx[i][k], b = idx[i + 1][k], c = idx[i + 1][k + 1], d = idx[i][k + 1];
                p.triangle(a, d, c);
                p.triangle(a, c, b);
            }
        }
    }

    /**
     * Flat disc at height z. front = faces +Z (heads), otherwise -Z (tails).
     * The tails UVs are chosen so the art is upright after the coin has turned 180 degrees around X.
     */
    private static void buildDisc(MeshPartBuilder p, float z, boolean front, boolean textured) {
        final float r = R - INSET;
        final int n = SEGMENTS;
        Vector3 nor = new Vector3(0, 0, front ? 1f : -1f);
        com.badlogic.gdx.math.Vector2 uv = new com.badlogic.gdx.math.Vector2(0.5f, 0.5f);
        short center = p.vertex(new Vector3(0, 0, z), nor, null, uv);
        short[] ring = new short[n + 1];
        for (int i = 0; i <= n; i++) {
            float a = MathUtils.PI2 * i / n;
            float c = MathUtils.cos(a), s = MathUtils.sin(a);
            if (textured) {
                uv.set(0.5f + 0.5f * c * ART_RADIUS, front ? 0.5f - 0.5f * s * ART_RADIUS : 0.5f + 0.5f * s * ART_RADIUS);
            } else {
                uv.set(0f, 0f);
            }
            ring[i] = p.vertex(new Vector3(r * c, r * s, z), nor, null, uv);
        }
        for (int i = 0; i < n; i++) {
            if (front) {
                p.triangle(center, ring[i], ring[i + 1]);
            } else {
                p.triangle(center, ring[i + 1], ring[i]);
            }
        }
    }

    // ---------------------------------------------------------------- generated textures

    /** Tiny repeating texture of light/dark vertical stripes for the reeded edge. */
    private static Texture reedTexture() {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        for (int x = 0; x < 8; x++) {
            float shade = 0.80f + 0.20f * MathUtils.cos(MathUtils.PI2 * x / 8f);
            pm.setColor(METAL.r * shade, METAL.g * shade, METAL.b * shade, 1f);
            pm.drawLine(x, 0, x, 7);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Plain coloured face with a ring, used when the PNGs are missing. */
    private Texture faceFallback(Color c) {
        int s = 256;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        pm.setColor(c);
        pm.fillCircle(s / 2, s / 2, s / 2 - 1);
        pm.setColor(c.r * 0.75f, c.g * 0.75f, c.b * 0.75f, 1f);
        for (int i = 0; i < 4; i++) {
            pm.drawCircle(s / 2, s / 2, s / 2 - 14 - i);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        owned.add(t);
        return t;
    }

    // ---------------------------------------------------------------- animation

    /**
     * @param flight  0..1 progress of the toss (spin and lift)
     * @param settle  seconds since the coin landed (0 while in the air), drives the little rocking at the end
     * @param turns   total turns around the X axis (whole = heads, +0.5 = tails)
     */
    void pose(float flight, float settle, float turns) {
        float t = MathUtils.clamp(flight, 0f, 1f);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);

        // the spin itself
        q.set(Vector3.X, eased * turns * 360f);

        // a little precession while it is in the air, fading out as it lands
        float fade = 1f - t;
        tmp.set(Vector3.Z, MathUtils.sin(t * MathUtils.PI2 * 1.5f) * 9f * fade)
                .mul(new Quaternion(Vector3.Y, MathUtils.cos(t * MathUtils.PI2 * 1.5f) * 7f * fade));
        q.mulLeft(tmp);

        // ease into the resting tilt over the last part of the flight
        float blend = MathUtils.clamp((t - 0.55f) / 0.45f, 0f, 1f);
        blend = blend * blend * (3f - 2f * blend);
        tmp.idt().slerp(rest, blend);
        q.mulLeft(tmp);

        // rocking after landing
        if (settle > 0f) {
            float decay = (float) Math.exp(-5.0 * settle);
            tmp.set(Vector3.X, MathUtils.sin(settle * 20f) * 13f * decay)
                    .mul(new Quaternion(Vector3.Y, MathUtils.sin(settle * 17f + 1f) * 6f * decay));
            q.mulLeft(tmp);
        }

        // thrown towards the camera at the top of the arc
        pos.set(0f, 0f, 4f * t * (1f - t) * 1.1f);
        instance.transform.set(pos, q);
    }

    /** Renders into the FrameBuffer. Never call while a SpriteBatch is open. */
    void render() {
        final boolean scissor = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
        fb.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        Dice3D.sharedBatch.begin(Dice3D.sharedCam);
        Dice3D.sharedBatch.render(instance, env);
        Dice3D.sharedBatch.end();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        fb.end();
        if (scissor) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        }
    }

    TextureRegion getRegion() {
        return region;
    }

    @Override
    public void dispose() {
        Dice3D.release(fb, fbSize);
        model.dispose();
        for (Texture t : owned) {
            t.dispose();
        }
        owned.clear();
    }
}