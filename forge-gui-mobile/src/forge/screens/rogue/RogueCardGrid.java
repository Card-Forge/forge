package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.toolbox.FDisplayObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Card images in a scrolling grid, dealt in face down and turned over when it first shows. A chosen card has a gold
 * frame and a tick, or is faded and crossed out when choosing takes it away. A tap chooses; a long press zooms.
 */
public class RogueCardGrid extends RogueScrollPane {
    private final List<PaperCard> cards;
    private final IntPredicate chosen;
    private final boolean crossOut;
    private final RogueDeal deal;
    private final Set<Integer> flown = new HashSet<>();
    private final List<Tile> tiles = new ArrayList<>();
    private boolean dealt;
    private int popIndex = -1;
    private float pop = 1;

    public RogueCardGrid(List<PaperCard> cards, IntPredicate chosen, IntConsumer onTap, boolean crossOut) {
        this.cards = cards;
        this.chosen = chosen;
        this.crossOut = crossOut;
        deal = new RogueDeal(cards.size());
        for (int i = 0; i < cards.size(); i++) {
            tiles.add(add(new Tile(i, onTap)));
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float u = RogueLook.unit();
        float gap = 7 * u;
        int cols = Forge.isLandscapeMode() ? 7 : 4;
        float cw = (visibleWidth - 16 * u - (cols - 1) * gap) / cols;
        if (Forge.isLandscapeMode()) {
            // Fewer columns when the cards then come out larger and still fit the height, never smaller than seven across
            for (int c = 6; c >= 3; c--) {
                int rows = (tiles.size() + c - 1) / c;
                float fit = Math.min((visibleWidth - 16 * u - (c - 1) * gap) / c,
                    (visibleHeight - 7 * u - rows * 8 * u) / rows / RogueLook.CARD_RATIO);
                if (fit > cw) {
                    cw = fit;
                    cols = c;
                }
            }
        }
        float ch = cw * RogueLook.CARD_RATIO;
        float left = (visibleWidth - cols * cw - (cols - 1) * gap) / 2;
        float bottom = 0;
        for (Tile tile : tiles) {
            float y = 7 * u + (tile.index / cols) * (ch + 8 * u);
            tile.setBounds(left + (tile.index % cols) * (cw + gap), y, cw, ch);
            bottom = y + ch;
        }
        if (!dealt && visibleHeight > 0) {
            dealt = true;
            deal.start();
        }
        return new ScrollBounds(visibleWidth, bottom + 8 * u);
    }

    /** The chosen cards fly into target, which is on the same screen; then done runs. */
    void flyChosen(RogueScreen screen, FDisplayObject target, Runnable done) {
        List<Tile> going = new ArrayList<>();
        for (Tile tile : tiles) {
            if (chosen.test(tile.index)) {
                going.add(tile);
            }
        }
        if (going.isEmpty() || crossOut) {
            done.run();
            return;
        }
        float cx = target.screenPos.x + target.getWidth() / 2;
        float cy = target.screenPos.y + target.getHeight() / 2;
        for (int i = 0; i < going.size(); i++) {
            Tile tile = going.get(i);
            flown.add(tile.index);
            PaperCard card = cards.get(tile.index);
            float w = tile.getWidth();
            float h = tile.getHeight();
            float[] from = {tile.screenPos.x, tile.screenPos.y, w, h};
            float[] to = {cx - w * 0.06f, cy - h * 0.06f, w * 0.12f, h * 0.12f};
            screen.fly((g, x, y, fw, fh) -> RogueLook.card(g, card, x, y, fw, fh, 1), from, to, i * 0.06f, 0.35f, 0,
                i == going.size() - 1 ? done : null);
        }
    }

    private void popped(int index) {
        popIndex = index;
        RogueMotion.play(0.18f, t -> pop = t, null);
    }

    private class Tile extends FDisplayObject {
        private final int index;
        private final IntConsumer onTap;

        Tile(int index, IntConsumer onTap) {
            this.index = index;
            this.onTap = onTap;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!deal.arrived(index)) {
                return true;
            }
            boolean was = chosen.test(index);
            onTap.accept(index);
            if (!was && chosen.test(index)) {
                popped(index);
            }
            return true;
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            RogueScreen.hover(this, hovered && deal.arrived(index), cards.get(index));
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(cards, index, null);
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (flown.contains(index)) {
                return;
            }
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            boolean on = chosen.test(index);
            boolean rare = PaperCardPredicates.IS_MYTHIC_RARE.test(cards.get(index));
            if (rare) {
                deal.halo(g, index, w, h);
            }
            if (!deal.arrived(index)) {
                float ox = RogueCardGrid.this.getWidth() / 2 - getLeft();
                float oy = getScrollTop() + RogueCardGrid.this.getHeight() - getTop();
                deal.draw(g, index, cards.get(index), w, h, ox, oy, rare);
                return;
            }
            if (on) {
                float grow = index == popIndex ? (1 - pop) * 4 * u : 0;
                g.fillRect(RogueLook.INK, -3.4f * u - grow, -3.4f * u - grow, w + 6.8f * u + 2 * grow, h + 6.8f * u + 2 * grow);
                g.fillRect(RogueLook.GOLD, -2.2f * u - grow, -2.2f * u - grow, w + 4.4f * u + 2 * grow, h + 4.4f * u + 2 * grow);
            }
            RogueLook.card(g, cards.get(index), 0, 0, w, on && crossOut ? 0.45f : 1);
            if (on && crossOut) {
                RogueLook.line(g, 0, 0, w, h, 2, RogueLook.INK);
                RogueLook.line(g, w, 0, 0, h, 2, RogueLook.INK);
            } else if (on) {
                g.fillCircle(RogueLook.PALE, w - 4 * u, 4 * u, 8 * u);
                RogueLook.item(g, "OK", w - 10.5f * u, -2.5f * u, 13 * u);
            }
        }
    }
}
