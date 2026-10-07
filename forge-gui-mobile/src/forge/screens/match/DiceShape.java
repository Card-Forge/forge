package forge.screens.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Pure geometry for the dice (no GL, no textures). Every face is a flat convex polygon with:
 *   - poly[f]   corners, counter-clockwise seen from outside
 *   - normal[f] outward normal
 *   - up[f]     the direction the number/art should point
 *   - local[f]  the corners in the face's own 2D frame (x = right, y = up, origin = face centre)
 *
 * Shapes: cube (d6 / planar), octahedron (d8), pentagonal trapezohedron (d10),
 * dodecahedron (d12), icosahedron (d20).
 * The dodecahedron and the trapezohedron are built as polar duals of the icosahedron and a
 * pentagonal antiprism, which guarantees every face is exactly planar.
 */
public final class DiceShape {
    private static final float RADIUS = 0.9f;   // circumradius of the non-cube shapes
    private static final float PHI = (1f + (float) Math.sqrt(5.0)) / 2f;
    private static final float EPS = 1e-3f;

    public final int faceCount;
    public final Vector3[][] poly;
    public final Vector3[] normal;
    public final Vector3[] up;
    public final Vector2[][] local;
    public final float[] halfExtent; // max |x| or |y| of the local corners
    public final float[] inRadius;   // distance from face centre to the nearest edge

    private DiceShape(List<Vector3[]> faces, Vector3[] upHint, float scale) {
        faceCount = faces.size();
        poly = new Vector3[faceCount][];
        normal = new Vector3[faceCount];
        up = new Vector3[faceCount];
        local = new Vector2[faceCount][];
        halfExtent = new float[faceCount];
        inRadius = new float[faceCount];

        for (int f = 0; f < faceCount; f++) {
            Vector3[] src = faces.get(f);
            Vector3[] p = new Vector3[src.length];
            for (int i = 0; i < p.length; i++) {
                p[i] = new Vector3(src[i]).scl(scale);
            }
            Vector3 c = centroid(p);
            Vector3 n = faceNormal(p);
            if (n.dot(c) < 0) { // make sure it is wound outward
                n.scl(-1f);
                reverse(p);
            }

            Vector3 v;
            if (upHint != null) {
                v = new Vector3(upHint[f]);
            } else { // point "up" at the corner farthest from the centre
                Vector3 far = p[0];
                for (Vector3 q : p) {
                    if (q.dst2(c) > far.dst2(c) + 1e-6f) far = q;
                }
                v = new Vector3(far).sub(c).nor();
            }
            Vector3 u = new Vector3(v).crs(n); // right = up x normal

            Vector2[] loc = new Vector2[p.length];
            float ext = 0;
            for (int i = 0; i < p.length; i++) {
                Vector3 d = new Vector3(p[i]).sub(c);
                loc[i] = new Vector2(d.dot(u), d.dot(v));
                ext = Math.max(ext, Math.max(Math.abs(loc[i].x), Math.abs(loc[i].y)));
            }
            float in = Float.MAX_VALUE;
            for (int i = 0; i < loc.length; i++) {
                Vector2 a = loc[i], b = loc[(i + 1) % loc.length];
                float len = a.dst(b);
                in = Math.min(in, Math.abs(a.x * b.y - a.y * b.x) / len);
            }

            poly[f] = p;
            normal[f] = n;
            up[f] = v;
            local[f] = loc;
            halfExtent[f] = ext;
            inRadius[f] = in;
        }
    }

    // ---------------------------------------------------------------- public factories

    public static boolean supportsNumbered(int sides) {
        return sides >= 4 && sides <= 20;
    }

    /**
     * Smallest real die that has at least {@code sides} faces. d7 uses the d8 shape, d9 the d10,
     * d11 the d12 and d13-d19 the d20; the spare faces simply repeat numbers (like a d20 used as a d10).
     */
    public static DiceShape forSides(int sides) {
        if (sides <= 4) return tetrahedron();
        if (sides <= 6) return cube();
        if (sides <= 8) return octahedron();
        if (sides <= 10) return trapezohedron();
        if (sides <= 12) return dodecahedron();
        if (sides <= 20) return icosahedron();
        return null;
    }

    /** Face order matches Dice3D.D6_FACE: +Z, -Z, +X, -X, +Y, -Y. */
    public static DiceShape cube() {
        Vector3[] n = {
                new Vector3(0, 0, 1), new Vector3(0, 0, -1),
                new Vector3(1, 0, 0), new Vector3(-1, 0, 0),
                new Vector3(0, 1, 0), new Vector3(0, -1, 0) };
        Vector3[] v = {
                new Vector3(0, 1, 0), new Vector3(0, -1, 0),
                new Vector3(0, 0, 1), new Vector3(0, 1, 0),
                new Vector3(1, 0, 0), new Vector3(0, 0, 1) };
        List<Vector3[]> faces = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Vector3 u = new Vector3(v[i]).crs(n[i]);
            Vector3 c = new Vector3(n[i]).scl(0.5f);
            faces.add(new Vector3[] {
                    corner(c, u, -0.5f, v[i], -0.5f), corner(c, u, 0.5f, v[i], -0.5f),
                    corner(c, u, 0.5f, v[i], 0.5f), corner(c, u, -0.5f, v[i], 0.5f) });
        }
        return new DiceShape(faces, v, 1f);
    }

    public static DiceShape octahedron() {
        Vector3[] v = {
                new Vector3(1, 0, 0), new Vector3(-1, 0, 0),
                new Vector3(0, 1, 0), new Vector3(0, -1, 0),
                new Vector3(0, 0, 1), new Vector3(0, 0, -1) };
        return normalized(trianglesByEdge(v));
    }

    public static DiceShape icosahedron() {
        return normalized(icosaFaces());
    }

    public static DiceShape dodecahedron() {
        return normalized(dual(icosaFaces()));
    }

    public static DiceShape trapezohedron() {
        Vector3[] t = new Vector3[5];
        Vector3[] b = new Vector3[5];
        for (int k = 0; k < 5; k++) {
            float a = k * 72f * MathUtils.degreesToRadians;
            t[k] = new Vector3(MathUtils.cos(a), MathUtils.sin(a), 0.5f);
            b[k] = new Vector3(MathUtils.cos(a + 36f * MathUtils.degreesToRadians),
                    MathUtils.sin(a + 36f * MathUtils.degreesToRadians), -0.5f);
        }
        // uniform pentagonal antiprism (circumradius 1, height 1)
        List<Vector3[]> anti = new ArrayList<>();
        anti.add(new Vector3[] { t[0], t[1], t[2], t[3], t[4] });
        anti.add(new Vector3[] { b[0], b[1], b[2], b[3], b[4] });
        for (int k = 0; k < 5; k++) {
            int k1 = (k + 1) % 5;
            anti.add(new Vector3[] { t[k], b[k], t[k1] });
            anti.add(new Vector3[] { t[k1], b[k1], b[k] });
        }
        return normalized(dual(anti));
    }

    // ---------------------------------------------------------------- construction helpers

    public static DiceShape tetrahedron() {
        Vector3[] v = {
                new Vector3(1, 1, 1), new Vector3(1, -1, -1),
                new Vector3(-1, 1, -1), new Vector3(-1, -1, 1) };
        return normalized(trianglesByEdge(v), 1.05f);
    }

    private static DiceShape normalized(List<Vector3[]> faces) {
        return normalized(faces, RADIUS);
    }

    private static DiceShape normalized(List<Vector3[]> faces, float radius) {
        float max = 0;
        for (Vector3[] p : faces) {
            for (Vector3 q : p) max = Math.max(max, q.len());
        }
        return new DiceShape(faces, null, radius / max);
    }

    private static List<Vector3[]> icosaFaces() {
        List<Vector3> v = new ArrayList<>();
        for (int a = -1; a <= 1; a += 2) {
            for (int b = -1; b <= 1; b += 2) {
                v.add(new Vector3(0, a, b * PHI));
                v.add(new Vector3(a, b * PHI, 0));
                v.add(new Vector3(b * PHI, 0, a));
            }
        }
        return trianglesByEdge(v.toArray(new Vector3[0]));
    }

    /** All triples of vertices whose three sides all equal the shortest edge. */
    private static List<Vector3[]> trianglesByEdge(Vector3[] v) {
        float edge = Float.MAX_VALUE;
        for (int i = 0; i < v.length; i++) {
            for (int j = i + 1; j < v.length; j++) edge = Math.min(edge, v[i].dst(v[j]));
        }
        List<Vector3[]> out = new ArrayList<>();
        for (int i = 0; i < v.length; i++) {
            for (int j = i + 1; j < v.length; j++) {
                for (int k = j + 1; k < v.length; k++) {
                    if (Math.abs(v[i].dst(v[j]) - edge) < EPS
                            && Math.abs(v[j].dst(v[k]) - edge) < EPS
                            && Math.abs(v[i].dst(v[k]) - edge) < EPS) {
                        out.add(new Vector3[] { v[i], v[j], v[k] });
                    }
                }
            }
        }
        return out;
    }

    /** Polar dual: one face per original vertex, one vertex per original face (n / d). */
    private static List<Vector3[]> dual(List<Vector3[]> faces) {
        int n = faces.size();
        Vector3[] dv = new Vector3[n];
        for (int f = 0; f < n; f++) {
            Vector3[] p = faces.get(f);
            Vector3 nr = faceNormal(p);
            if (nr.dot(centroid(p)) < 0) nr.scl(-1f);
            dv[f] = new Vector3(nr).scl(1f / nr.dot(p[0]));
        }

        List<Vector3> verts = new ArrayList<>();
        for (Vector3[] p : faces) {
            for (Vector3 q : p) {
                boolean seen = false;
                for (Vector3 w : verts) {
                    if (w.dst2(q) < 1e-6f) { seen = true; break; }
                }
                if (!seen) verts.add(q);
            }
        }

        List<Vector3[]> out = new ArrayList<>();
        for (Vector3 vtx : verts) {
            List<Integer> adj = new ArrayList<>();
            for (int f = 0; f < n; f++) {
                for (Vector3 q : faces.get(f)) {
                    if (q.dst2(vtx) < 1e-6f) { adj.add(f); break; }
                }
            }
            // sort the neighbouring dual vertices counter-clockwise around the outward axis
            Vector3 a = new Vector3(vtx).nor();
            Vector3 ref = dv[adj.get(0)];
            Vector3 e1 = new Vector3(ref).mulAdd(a, -ref.dot(a)).nor();
            Vector3 e2 = new Vector3(a).crs(e1);
            final float[] ang = new float[n];
            for (int f : adj) ang[f] = MathUtils.atan2(dv[f].dot(e2), dv[f].dot(e1));
            Collections.sort(adj, (x, y) -> Float.compare(ang[x], ang[y]));

            Vector3[] poly = new Vector3[adj.size()];
            for (int i = 0; i < poly.length; i++) poly[i] = dv[adj.get(i)];
            out.add(poly);
        }
        return out;
    }

    private static Vector3 corner(Vector3 c, Vector3 u, float su, Vector3 v, float sv) {
        return new Vector3(c).mulAdd(u, su).mulAdd(v, sv);
    }

    private static Vector3 faceNormal(Vector3[] p) {
        return new Vector3(p[1]).sub(p[0]).crs(new Vector3(p[2]).sub(p[0])).nor();
    }

    private static Vector3 centroid(Vector3[] p) {
        Vector3 c = new Vector3();
        for (Vector3 q : p) c.add(q);
        return c.scl(1f / p.length);
    }

    private static void reverse(Vector3[] p) {
        for (int i = 0, j = p.length - 1; i < j; i++, j--) {
            Vector3 t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
    }
}