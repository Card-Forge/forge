package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePath;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * The run's path, climbing from the bottom on a grid of equal rows. Walked lines are solid, the way on is dashed,
 * and the rest is faint; nodes off the way are faded. A tap on a node of the current row chooses it.
 */
public class RogueMapView extends FScrollPane {
    static final Color NODE_FACE = new Color(0.93f, 0.87f, 0.73f, 1);
    private static final float ZOOM_SECONDS = 0.35f;
    // The rays are 30 degrees apart, so one 30 degree turn repeats seamlessly; 3 seconds is 10 degrees a second
    private static final float RAY_TURN_SECONDS = 3f;
    private static final Color[] RAY_BANDS = {new Color(0.77f, 0.25f, 0.16f, 0.35f), new Color(0.77f, 0.25f, 0.16f, 0.28f),
        new Color(0.77f, 0.25f, 0.16f, 0.21f), new Color(0.77f, 0.25f, 0.16f, 0.13f), new Color(0.77f, 0.25f, 0.16f, 0.06f)};

    private final IntConsumer onChoose;
    private IntConsumer onOpen = i -> { };
    private IntConsumer onInspect = i -> { };
    /** A node off the current row that is highlighted instead of the chosen one; -1 for none. */
    private int inspected = -1;
    private boolean whole;
    /** 0 is the scrolling map and 1 every row; in between while zooming. */
    private float wholeness;
    private boolean zooming;
    /** Where the scrolling map was, at full size, when zooming out began. */
    private float zoomOutFrom;
    private float zoom = 1;
    private float raySpin;
    private boolean raysTurning;
    private RogueRun run;
    private Set<Integer> open = Set.of();
    private final Set<RoguePathNode> live = new HashSet<>();
    private final List<NodeTile> tiles = new ArrayList<>();
    private RoguePathNode lastCompleted;
    private boolean scrollToCurrent;
    private Float scrollAfterLayout;
    private boolean intro;
    private Runnable afterIntro;

    /** What a double tap on an open plane does, as well as choosing it. */
    void onOpen(IntConsumer onOpen) {
        this.onOpen = onOpen;
    }

    /** What a tap on a node off the current row does, to plan ahead. */
    void onInspect(IntConsumer onInspect) {
        this.onInspect = onInspect;
    }

    /** Highlights a node off the current row instead of the chosen one; -1 goes back to the chosen one. */
    void setInspected(int index) {
        inspected = index;
    }

    public RogueMapView(IntConsumer onChoose) {
        this.onChoose = onChoose;
    }

    boolean isWhole() {
        return whole;
    }

    /** Zooms between every row at once and the scrolling map at the current row. */
    void setWhole(boolean whole) {
        if (this.whole == whole || zooming) {
            return;
        }
        this.whole = whole;
        if (whole) {
            zoomOutFrom = getScrollTop() / zoom;
        }
        float start = wholeness;
        zooming = true;
        RogueMotion.play(ZOOM_SECONDS, t -> {
            wholeness = whole ? start + (1 - start) * t : start * (1 - t);
            revalidate();
        }, () -> {
            zooming = false;
            scrollToCurrent = !whole;
            revalidate();
        });
    }

    @Override
    public void draw(Graphics g) {
        RogueLook.zoomed(zoom, () -> super.draw(g));
    }

    /** The next layout starts at the path's far end and then scrolls down to the current row; then afterIntro runs. */
    public void playIntro(Runnable afterIntro) {
        intro = true;
        this.afterIntro = afterIntro;
    }

    /** Reads the path; call while the flow is idle or asking. */
    public void setRun(RogueRun run, Set<Integer> open) {
        this.run = run;
        this.open = open;
        clear();
        tiles.clear();
        live.clear();
        lastCompleted = null;
        if (run == null || run.getPath() == null) {
            return;
        }
        RoguePath path = run.getPath();
        for (RoguePathNode node : path.getNodes()) {
            if (node.isCompleted()) {
                lastCompleted = node;
            }
        }
        for (int i : open) {
            live.add(path.getNode(i));
        }
        for (int row = run.getCurrentNode().getRowIndex(); row <= path.getMaxRow(); row++) {
            for (RoguePathNode node : path.getNodesInRow(row)) {
                if (live.contains(node)) {
                    live.addAll(path.getReachableNodes(node));
                }
            }
        }
        for (int i = 0; i < path.getNodeCount(); i++) {
            tiles.add(add(new NodeTile(i, path.getNode(i))));
        }
        scrollToCurrent = true;
        revalidate();
        turnRays();
    }

    /** Keeps the light behind the final boss turning while the screen that showed the map is up. */
    private void turnRays() {
        if (raysTurning) {
            return;
        }
        raysTurning = true;
        FScreen screen = Forge.getCurrentScreen();
        RogueMotion.loop(RAY_TURN_SECONDS, t -> raySpin = t * 30, () -> Forge.getCurrentScreen() == screen, () -> raysTurning = false);
    }

    private float rowStep() {
        return (Forge.isLandscapeMode() ? 60 : 76) * RogueLook.unit();
    }

    private float colStep() {
        return (Forge.isLandscapeMode() ? 80 : 67) * RogueLook.unit();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        if (run == null || run.getPath() == null) {
            return new ScrollBounds(visibleWidth, visibleHeight);
        }
        float fit = Math.min(1, visibleWidth / fullWidth());
        float fullHeight = (run.getPath().getMaxRow() + 1) * rowStep() + 30 * RogueLook.unit();
        float all = Math.min(fit, visibleHeight / fullHeight);
        zoom = fit + (all - fit) * Interpolation.smooth.apply(wholeness);
        ScrollBounds[] bounds = new ScrollBounds[1];
        RogueLook.zoomed(zoom, () -> bounds[0] = layout(visibleWidth, visibleHeight));
        return bounds[0];
    }

    /** The widest row at full size, with a margin at each side. */
    private float fullWidth() {
        RoguePath path = run.getPath();
        int widest = 1;
        for (int row = 0; row <= path.getMaxRow(); row++) {
            widest = Math.max(widest, path.getNodesInRow(row).size());
        }
        float u = RogueLook.unit();
        return (widest - 1) * colStep() + (Forge.isLandscapeMode() ? 70 : 55) * u + 8 * u;
    }

    private ScrollBounds layout(float visibleWidth, float visibleHeight) {
        RoguePath path = run.getPath();
        float u = RogueLook.unit();
        float tw = (Forge.isLandscapeMode() ? 70 : 55) * u;
        float th = (Forge.isLandscapeMode() ? 46 : 52) * u;
        float height = (path.getMaxRow() + 1) * rowStep() + 30 * u;
        for (NodeTile tile : tiles) {
            RoguePathNode node = tile.node;
            int size = path.getNodesInRow(node.getRowIndex()).size();
            float cx = visibleWidth / 2 + (node.getColumnIndex() - (size - 1) / 2f) * colStep();
            float cy = height - 15 * u - (node.getRowIndex() + 0.5f) * rowStep();
            boolean boss = node instanceof NodePlanebound plane && plane.getPlaneboundType() == RoguePlaneboundType.BOSS;
            float w = node.isSideNode() ? 36 * u : boss ? tw * 1.2f : tw;
            float h = node.isSideNode() ? 36 * u : boss ? th * 1.2f : th;
            tile.setBounds(cx - w / 2, cy - h / 2, w, h);
        }
        float cy = height - 15 * u - (run.getCurrentNode().getRowIndex() + 0.5f) * rowStep();
        float target = Math.max(0, Math.min(height - visibleHeight, cy - visibleHeight * 0.68f));
        if (zooming) {
            // Out from wherever the map was; back in to the current row
            float rest = 1 - Interpolation.smooth.apply(wholeness);
            scrollAfterLayout = (whole ? zoomOutFrom * zoom : target) * rest;
        } else if (scrollToCurrent && !whole) {
            scrollToCurrent = false;
            if (intro) {
                intro = false;
                scrollAfterLayout = 0f;
                new IntroScroll(target).start();
            } else {
                scrollAfterLayout = target;
            }
        } else if (whole) {
            scrollAfterLayout = 0f;
        }
        return new ScrollBounds(visibleWidth, height);
    }

    /** Scrolling has to wait until the new bounds are in place and the old scroll is undone, which doLayout does next. */
    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        if (scrollAfterLayout != null) {
            scrollTop0 = scrollAfterLayout;
            scrollAfterLayout = null;
        }
        super.setScrollPositionsAfterLayout(scrollLeft0, scrollTop0);
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (run == null) {
            return;
        }
        drawBossLight(g);
        RoguePath path = run.getPath();
        for (NodeTile from : tiles) {
            for (RoguePathNode next : path.getReachableNodes(from.node)) {
                NodeTile to = tiles.get(path.getNodes().indexOf(next));
                boolean walked = from.node.isCompleted() && next.isCompleted();
                boolean way = from.node == lastCompleted && open.contains(to.index);
                int style = walked ? 2 : way || live.contains(from.node) && live.contains(next) ? 1 : 0;
                Color ink = walked || way ? RogueLook.INK
                    : style == 1 ? new Color(0.30f, 0.26f, 0.2f, 0.85f) : new Color(0.36f, 0.32f, 0.27f, 0.45f);
                // Edge to edge, so the bend falls between the tiles and every trail meets a tile at the middle of its edge
                RogueLook.trail(g, from.getLeft() + from.getWidth() / 2, from.getTop(),
                    to.getLeft() + to.getWidth() / 2, to.getTop() + to.getHeight(), false, style, ink);
            }
        }
    }

    /** Dark red light turning slowly behind the act's final boss, fading outwards. */
    private void drawBossLight(Graphics g) {
        for (NodeTile tile : tiles) {
            if (!(tile.node instanceof NodePlanebound plane) || plane.getPlaneboundType() != RoguePlaneboundType.BOSS || plane.isCompleted()) {
                continue;
            }
            RogueLook.rays(g, RAY_BANDS, tile.getLeft() + tile.getWidth() / 2, tile.getTop() + tile.getHeight() / 2,
                tile.getWidth() * 1.3f, raySpin);
        }
    }

    /** Plays the first look again if another screen cut it short. */
    void resumeIntro() {
        if (intro) {
            revalidate();
        }
    }

    /**
     * A short pause on the far end, then an eased scroll down to the current row. Cut short by another screen, it waits
     * to start again when the map shows.
     */
    private class IntroScroll extends ForgeAnimation {
        private static final float PAUSE = 0.5f;
        private static final float DURATION = 1.8f;
        private final float target;
        private float time;

        IntroScroll(float target) {
            this.target = target;
        }

        @Override
        protected boolean advance(float dt) {
            time += dt;
            float t = Math.min(1, Math.max(0, (time - PAUSE) / DURATION));
            float eased = t * t * (3 - 2 * t);
            setScrollTop(target * eased);
            return t < 1;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (endingAll) {
                intro = true;
                scrollToCurrent = true;
                setScrollTop(0);
                return;
            }
            setScrollTop(target);
            Runnable after = afterIntro;
            afterIntro = null;
            if (after != null) {
                after.run();
            }
        }
    }

    /** A side node's building, also drawn large for the chosen node. */
    static void building(Graphics g, RoguePathNode node, float cx, float cy, float size, float alpha) {
        switch (node.getType()) {
            case SANCTUM -> RogueLook.building(g, 160, 240, 32, 32, cx - size / 2, cy - size / 2, size, size, alpha);
            case BAZAAR -> RogueLook.building(g, 192, 240, 32, 32, cx - size / 2, cy - size / 2, size, size, alpha);
            case CHEST -> RogueLook.building(g, 432, 272, 16, 16, cx - size * 0.41f, cy - size * 0.41f, size * 0.82f, size * 0.82f, alpha);
            case EVENT, PLANEBOUND -> RogueLook.building(g, 240, 272, 16, 16, cx - size * 0.41f, cy - size * 0.41f, size * 0.82f, size * 0.82f, alpha);
        }
    }

    private static WeakReference<RogueRun> facesRun = new WeakReference<>(null);
    private static final Set<Integer> faceUp = new HashSet<>();
    private static final Map<Integer, Float> turning = new HashMap<>();

    /**
     * How far plane index has turned face up, 0 to 1. A plane the map has not shown face up before in this run turns
     * over the first time it is drawn so; the first look at a run shows its planes as they are.
     */
    private float turned(int index, boolean down) {
        if (facesRun.get() != run) {
            facesRun = new WeakReference<>(run);
            faceUp.clear();
            turning.clear();
            for (int i = 0; i < run.getPath().getNodeCount(); i++) {
                if (run.getPath().getNode(i) instanceof NodePlanebound p && !run.getPath().isFaceDown(run, p)) {
                    faceUp.add(i);
                }
            }
        }
        if (down) {
            return 0;
        }
        if (faceUp.add(index)) {
            float delay = 0.15f * turning.size();
            turning.put(index, 0f);
            RogueMotion.play(delay + 0.5f, t -> turning.put(index, Math.max(0, (t * (delay + 0.5f) - delay) / 0.5f)),
                () -> turning.remove(index));
        }
        return turning.getOrDefault(index, 1f);
    }

    private class NodeTile extends FDisplayObject {
        private final int index;
        private final RoguePathNode node;

        NodeTile(int index, RoguePathNode node) {
            this.index = index;
            this.node = node;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (open.contains(index)) {
                onChoose.accept(index);
                if (count >= 2 && node instanceof NodePlanebound) {
                    onOpen.accept(index);
                }
            } else {
                onInspect.accept(index);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            boolean here = open.contains(index);
            boolean chosen = inspected >= 0 ? index == inspected : node == run.getCurrentNode();
            float alpha = node.isCompleted() ? 0.75f : live.contains(node) ? 1 : 0.38f;
            if (node instanceof NodePlanebound plane) {
                float turn = turned(index, run.getPath().isFaceDown(run, plane));
                if (turn > 0 && turn < 1) {
                    drawTurning(g, plane, turn, alpha);
                    return;
                }
                drawPlane(g, plane, here, chosen, alpha);
            } else {
                drawSide(g, here, chosen, alpha);
            }
            if (node.isCompleted()) {
                float cx = node.isSideNode() ? getWidth() / 2 + 11 * u : getWidth() - 2 * u;
                float cy = node.isSideNode() ? getHeight() / 2 - 11 * u : 2 * u;
                g.fillCircle(RogueLook.PALE, cx, cy, 7 * u);
                RogueLook.item(g, "OK", cx - 6 * u, cy - 6 * u, 12 * u);
            }
        }

        private void drawSide(Graphics g, boolean here, boolean chosen, float alpha) {
            float u = RogueLook.unit();
            float cx = getWidth() / 2;
            float cy = getHeight() / 2;
            if (here || chosen) {
                g.fillCircle(chosen ? RogueLook.INK : new Color(0.12f, 0.1f, 0.08f, 0.35f), cx, cy, 18 * u);
                g.fillCircle(chosen ? RogueLook.GOLD : RogueLook.PALE, cx, cy, 16 * u);
            }
            g.fillCircle(NODE_FACE, cx, cy, 14.5f * u);
            RogueLook.ring(g, cx, cy, 14.5f * u, 1, new Color(0.12f, 0.1f, 0.08f, alpha));
            building(g, node, cx, cy, 22 * u, alpha);
        }

        /** The whole tile turning over like a card: its hidden face narrows away, then its plane face widens in. */
        private void drawTurning(Graphics g, NodePlanebound plane, float turn, float alpha) {
            float u = RogueLook.unit();
            float tw = getWidth();
            float th = getHeight();
            float art = th - 23 * u;
            float kw = Math.max(u, tw * Math.abs(1 - 2 * turn));
            float kx = (tw - kw) / 2;
            g.fillRect(new Color(0.12f, 0.1f, 0.08f, alpha), kx - u, -u, kw + 2 * u, th + 2 * u);
            g.fillRect(NODE_FACE, kx, 0, kw, th);
            if (turn < 0.5f) {
                g.fillRect(new Color(0.45f, 0.39f, 0.31f, alpha * 0.9f), kx, 0, kw, art);
            } else {
                RogueLook.art(g, CodexHelper.getPlaneCard(plane.getRoguePlanebound()), kx, 0, kw, art, alpha);
            }
        }

        private void drawPlane(Graphics g, NodePlanebound plane, boolean here, boolean chosen, float alpha) {
            float u = RogueLook.unit();
            float tw = getWidth();
            float th = getHeight();
            float art = th - 23 * u;
            boolean down = run.getPath().isFaceDown(run, plane);
            Color ink = new Color(0.12f, 0.1f, 0.08f, Math.max(alpha, 0.5f));
            if (here || chosen) {
                g.fillRect(chosen ? RogueLook.INK : new Color(0.12f, 0.1f, 0.08f, 0.45f), -4 * u, -4 * u, tw + 8 * u, th + 8 * u);
                g.fillRect(chosen ? RogueLook.GOLD : RogueLook.PALE, -2.4f * u, -2.4f * u, tw + 4.8f * u, th + 4.8f * u);
            }
            g.fillRect(new Color(0.12f, 0.1f, 0.08f, alpha), -u, -u, tw + 2 * u, th + 2 * u);
            g.fillRect(NODE_FACE, 0, 0, tw, th);
            if (down) {
                g.fillRect(new Color(0.45f, 0.39f, 0.31f, alpha * 0.9f), 0, 0, tw, art);
                RogueLook.text(g, "?", 0, art / 2 - 11 * u, 1.6f, new Color(1, 0.96f, 0.86f, Math.max(alpha, 0.6f)), tw, Align.center);
            } else {
                RogueLook.art(g, CodexHelper.getPlaneCard(plane.getRoguePlanebound()), 0, 0, tw, art, alpha);
            }
            RogueLook.fit(g, down ? "???" : plane.getRoguePlanebound().planeName(), u, art - 0.5f * u, 0.85f, ink, tw - 2 * u, Align.center);
            RogueLook.item(g, "Life", 2 * u, th - 11.5f * u, 10 * u);
            RogueLook.text(g, String.valueOf(plane.getPlaneboundLife(run.getPath().countPlaneboundRowsUpTo(plane.getRowIndex()))),
                13 * u, th - 12.5f * u, 0.85f, ink);
            float ix = tw - 11 * u;
            float iy = th - 12 * u;
            if (plane.getPlaneboundType() == RoguePlaneboundType.ELITE) {
                RogueLook.item(g, "Star", ix, iy, 10 * u);
                ix -= 10 * u;
            }
            if (plane.getPlaneboundType() == RoguePlaneboundType.BOSS) {
                RogueLook.skin(g, FSkinImage.POISON, ix, iy, 10 * u);
                ix -= 11 * u;
            }
            if (plane.getCursedCount() > 0) {
                RogueLook.item(g, "BlackRune", ix, iy, 10 * u);
                ix -= 10 * u;
                if (plane.getCursedCount() > 1) {
                    RogueLook.text(g, String.valueOf(plane.getCursedCount()), ix + 4 * u, iy - 0.5f * u, 0.8f, ink);
                    ix -= 6 * u;
                }
            }
            if (plane.getWrathfulCount() > 0) {
                RogueLook.building(g, 304, 288, 16, 16, ix - u, iy - u, 11 * u, 11 * u, 1);
                ix -= 10 * u;
                if (plane.getWrathfulCount() > 1) {
                    RogueLook.text(g, String.valueOf(plane.getWrathfulCount()), ix + 4 * u, iy - 0.5f * u, 0.8f, ink);
                }
            }
        }
    }
}
