package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
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
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * The run's path, climbing from the bottom on a grid of equal rows. Walked lines are solid, the way on is dashed,
 * and the rest is faint; nodes off the way are faded. A tap on a node of the current row chooses it.
 */
public class RogueMapView extends FScrollPane {
    static final Color NODE_FACE = new Color(0.93f, 0.87f, 0.73f, 1);

    private final IntConsumer onChoose;
    private final boolean whole;
    private float zoom = 1;
    private RogueRun run;
    private Set<Integer> open = Set.of();
    private final Set<RoguePathNode> live = new HashSet<>();
    private final List<NodeTile> tiles = new ArrayList<>();
    private RoguePathNode lastCompleted;
    private boolean scrollToCurrent;
    private boolean intro;
    private Runnable afterIntro;

    /** whole shows every row at once, smaller, without scrolling. */
    public RogueMapView(IntConsumer onChoose, boolean whole) {
        this.onChoose = onChoose;
        this.whole = whole;
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
    }

    private float rowStep() {
        return (Forge.isLandscapeMode() ? 54 : 66) * RogueLook.unit();
    }

    private float colStep() {
        return (Forge.isLandscapeMode() ? 80 : 61) * RogueLook.unit();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        if (run == null || run.getPath() == null) {
            return new ScrollBounds(visibleWidth, visibleHeight);
        }
        if (whole) {
            zoom = 1;
            float fullHeight = (run.getPath().getMaxRow() + 1) * rowStep() + 30 * RogueLook.unit();
            zoom = Math.min(1, visibleHeight / fullHeight);
        }
        ScrollBounds[] bounds = new ScrollBounds[1];
        RogueLook.zoomed(zoom, () -> bounds[0] = layout(visibleWidth, visibleHeight));
        return bounds[0];
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
        if (scrollToCurrent && !whole) {
            scrollToCurrent = false;
            float cy = height - 15 * u - (run.getCurrentNode().getRowIndex() + 0.5f) * rowStep();
            float target = Math.max(0, Math.min(height - visibleHeight, cy - visibleHeight * 0.68f));
            if (intro) {
                intro = false;
                setScrollPositionsAfterLayout(0, 0);
                new IntroScroll(target).start();
            } else {
                setScrollPositionsAfterLayout(0, target);
            }
        }
        return new ScrollBounds(visibleWidth, height);
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (run == null) {
            return;
        }
        RoguePath path = run.getPath();
        for (NodeTile from : tiles) {
            for (RoguePathNode next : path.getReachableNodes(from.node)) {
                NodeTile to = tiles.get(path.getNodes().indexOf(next));
                boolean walked = from.node.isCompleted() && next.isCompleted();
                boolean way = from.node == lastCompleted && open.contains(to.index);
                int style = walked ? 2 : way || live.contains(from.node) && live.contains(next) ? 1 : 0;
                Color ink = walked || way ? RogueLook.INK
                    : style == 1 ? new Color(0.30f, 0.26f, 0.2f, 0.85f) : new Color(0.36f, 0.32f, 0.27f, 0.45f);
                RogueLook.trail(g, from.getLeft() + from.getWidth() / 2, from.getTop() + from.getHeight() / 2,
                    to.getLeft() + to.getWidth() / 2, to.getTop() + to.getHeight() / 2, false, style, ink);
            }
        }
    }

    /** A short pause on the far end, then an eased scroll down to the current row. */
    private class IntroScroll extends ForgeAnimation {
        private static final float PAUSE = 0.5f;
        private static final float DURATION = 1.2f;
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
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            boolean here = open.contains(index);
            boolean chosen = node == run.getCurrentNode();
            float alpha = node.isCompleted() ? 0.75f : live.contains(node) ? 1 : 0.38f;
            if (node instanceof NodePlanebound plane) {
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
            if (here) {
                g.fillCircle(chosen ? RogueLook.INK : new Color(0.12f, 0.1f, 0.08f, 0.35f), cx, cy, 18 * u);
                g.fillCircle(chosen ? RogueLook.GOLD : RogueLook.PALE, cx, cy, 16 * u);
            }
            g.fillCircle(NODE_FACE, cx, cy, 14.5f * u);
            RogueLook.ring(g, cx, cy, 14.5f * u, 1, new Color(0.12f, 0.1f, 0.08f, alpha));
            building(g, node, cx, cy, 22 * u, alpha);
        }

        private void drawPlane(Graphics g, NodePlanebound plane, boolean here, boolean chosen, float alpha) {
            float u = RogueLook.unit();
            float tw = getWidth();
            float th = getHeight();
            float art = th - 23 * u;
            boolean down = run.getPath().isFaceDown(run, plane);
            Color ink = new Color(0.12f, 0.1f, 0.08f, Math.max(alpha, 0.5f));
            if (here) {
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
