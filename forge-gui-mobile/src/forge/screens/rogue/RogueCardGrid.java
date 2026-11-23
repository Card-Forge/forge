package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.toolbox.FDisplayObject;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Card images in a scrolling grid. A chosen card has a gold frame and a tick, or is faded and crossed out when
 * choosing takes it away. A tap chooses; a long press zooms.
 */
public class RogueCardGrid extends RogueScrollPane {
    private final List<PaperCard> cards;
    private final IntPredicate chosen;
    private final boolean crossOut;

    public RogueCardGrid(List<PaperCard> cards, IntPredicate chosen, IntConsumer onTap, boolean crossOut) {
        this.cards = cards;
        this.chosen = chosen;
        this.crossOut = crossOut;
        for (int i = 0; i < cards.size(); i++) {
            add(new Tile(i, onTap));
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float u = RogueLook.unit();
        int cols = Forge.isLandscapeMode() ? 7 : 4;
        float gap = 7 * u;
        float cw = (visibleWidth - 16 * u - (cols - 1) * gap) / cols;
        float ch = cw * RogueLook.CARD_RATIO;
        float bottom = 0;
        for (FDisplayObject child : getChildren()) {
            int i = ((Tile) child).index;
            float y = 7 * u + (i / cols) * (ch + 8 * u);
            child.setBounds(8 * u + (i % cols) * (cw + gap), y, cw, ch);
            bottom = y + ch;
        }
        return new ScrollBounds(visibleWidth, bottom + 8 * u);
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
            onTap.accept(index);
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(cards, index, null);
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            boolean on = chosen.test(index);
            if (on) {
                g.fillRect(RogueLook.INK, -3.4f * u, -3.4f * u, w + 6.8f * u, h + 6.8f * u);
                g.fillRect(RogueLook.GOLD, -2.2f * u, -2.2f * u, w + 4.4f * u, h + 4.4f * u);
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
