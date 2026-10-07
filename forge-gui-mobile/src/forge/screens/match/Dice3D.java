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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public class Dice3D implements Disposable {
    public static final int SIZE = 1024;
    public static final int[] D6_FACE = { 0, 2, 4, 5, 3, 1 };

    private static final Vector3[] N = {
            new Vector3(0, 0, 1), new Vector3(0, 0, -1),
            new Vector3(1, 0, 0), new Vector3(-1, 0, 0),
            new Vector3(0, 1, 0), new Vector3(0, -1, 0) };
    private static final Vector3[] U = {
            new Vector3(1, 0, 0), new Vector3(1, 0, 0),
            new Vector3(0, 1, 0), new Vector3(0, 0, 1),
            new Vector3(0, 0, 1), new Vector3(1, 0, 0) };
    private static final Vector3[] V = {
            new Vector3(0, 1, 0), new Vector3(0, -1, 0),
            new Vector3(0, 0, 1), new Vector3(0, 1, 0),
            new Vector3(1, 0, 0), new Vector3(0, 0, 1) };

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

    public Dice3D(Texture[] byFace) {
        this(toRegions(byFace));
    }

    private static TextureRegion[] toRegions(Texture[] t) {
        TextureRegion[] r = new TextureRegion[t.length];
        for (int i = 0; i < t.length; i++) r[i] = new TextureRegion(t[i]);
        return r;
    }

    public Dice3D(TextureRegion[] byFace) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        for (int i = 0; i < 6; i++) {
            TextureRegion r = byFace[i];
            Texture tex = r.getTexture();
            TextureAttribute ta = TextureAttribute.createDiffuse(tex);

            ta.offsetU = (r.getRegionX() + 0.5f) / tex.getWidth();
            ta.offsetV = (r.getRegionY() + 0.5f) / tex.getHeight();
            ta.scaleU = (r.getRegionWidth() - 1f) / tex.getWidth();
            ta.scaleV = (r.getRegionHeight() - 1f) / tex.getHeight();

            Material mat = new Material(ta);
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));

            // Create the mesh part wrapper container
            MeshPartBuilder part = mb.part("face" + i, GL20.GL_TRIANGLES,
                    Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);

            float width = 1.0f;
            float height = 1.0f;
            float depth = 1.0f;

            BoxShapeBuilder.build(part,0, 0, 0, width, height, depth);
        }

        model = mb.end();
        instance = new ModelInstance(model);

        modelBatch = new ModelBatch();
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.55f, 0.55f, 1f));
        env.add(new DirectionalLight().set(0.9f, 0.9f, 0.9f, -0.5f, -0.8f, -1f));

        cam = new PerspectiveCamera(40, SIZE, SIZE);
        cam.position.set(0, 0, 4.2f);
        cam.lookAt(0, 0, 0);
        cam.near = 0.1f;
        cam.far = 20f;
        cam.update();

        fb = new FrameBuffer(Pixmap.Format.RGBA8888, SIZE, SIZE, true);
        fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        region = new TextureRegion(fb.getColorBufferTexture());
        region.flip(false, true); // FBO textures are upside-down
    }

    public void roll(int faceIndex, float durationSeconds) {
        duration = durationSeconds;
        time = 0;
        rolling = true;

        Matrix4 m = new Matrix4().set(U[faceIndex], V[faceIndex], N[faceIndex], Vector3.Zero);
        m.getRotation(finalQ);
        finalQ.conjugate();
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

    public static Texture makePipTexture(int value) {
        int s = 256;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pm.setColor(0.7f, 0.7f, 0.7f, 1f);
        pm.drawRectangle(0, 0, s, s);
        pm.setColor(Color.BLACK);
        int a = s / 4, b = s / 2, c = 3 * s / 4, r = s / 10;
        int[][] p;
        switch (value) {
            case 1: p = new int[][] {{b, b}}; break;
            case 2: p = new int[][] {{a, a}, {c, c}}; break;
            case 3: p = new int[][] {{a, a}, {b, b}, {c, c}}; break;
            case 4: p = new int[][] {{a, a}, {c, a}, {a, c}, {c, c}}; break;
            case 5: p = new int[][] {{a, a}, {c, a}, {b, b}, {a, c}, {c, c}}; break;
            default: p = new int[][] {{a, a}, {c, a}, {a, b}, {c, b}, {a, c}, {c, c}}; break;
        }
        for (int[] pt : p) pm.fillCircle(pt[0], pt[1], r);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    @Override
    public void dispose() {
        fb.dispose();
        modelBatch.dispose();
        model.dispose();
    }
}