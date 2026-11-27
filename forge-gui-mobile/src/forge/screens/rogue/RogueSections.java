package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.gamemodes.rogue.flow.RogueDecisions.Section;
import forge.item.PaperCard;
import forge.toolbox.FDisplayObject;
import java.util.ArrayList;
import java.util.List;

/** A result's sections: a label, a text and cards, each part only when it has something. */
public class RogueSections extends RogueScrollPane {
    private final List<Section> sections = new ArrayList<>();
    private final List<RogueText> texts = new ArrayList<>();
    private final List<List<CardTile>> cards = new ArrayList<>();
    private final List<Float> labelTops = new ArrayList<>();

    public RogueSections(List<Section> all) {
        for (Section section : all) {
            boolean hasText = section.text() != null && !section.text().isBlank();
            boolean hasCards = section.cards() != null && !section.cards().isEmpty();
            if (!hasText && !hasCards) {
                continue;
            }
            sections.add(section);
            texts.add(hasText ? add(new RogueText(section.text(), null, 1, RogueLook.INK)) : null);
            List<CardTile> tiles = new ArrayList<>();
            if (hasCards) {
                for (int i = 0; i < section.cards().size(); i++) {
                    tiles.add(add(new CardTile(section.cards(), i)));
                }
            }
            cards.add(tiles);
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float u = RogueLook.unit();
        float y = 4 * u;
        float w = visibleWidth - 12 * u;
        int cols = Forge.isLandscapeMode() ? 6 : 4;
        float gap = 7 * u;
        float cw = Math.min(96 * u, (w - gap * (cols - 1)) / cols);
        labelTops.clear();
        for (int s = 0; s < sections.size(); s++) {
            Section section = sections.get(s);
            labelTops.add(y);
            if (section.label() != null && !section.label().isBlank()) {
                y += 19 * u;
            }
            RogueText text = texts.get(s);
            if (text != null) {
                float th = text.getPreferredHeight(w);
                text.setBounds(6 * u, y, w, th);
                y += th + 4 * u;
            }
            List<CardTile> tiles = cards.get(s);
            float off = 6 * u + (w - Math.min(tiles.size(), cols) * (cw + gap) + gap) / 2;
            for (int i = 0; i < tiles.size(); i++) {
                tiles.get(i).setBounds(off + (i % cols) * (cw + gap), y + (i / cols) * (cw * RogueLook.CARD_RATIO + gap), cw,
                    cw * RogueLook.CARD_RATIO);
            }
            if (!tiles.isEmpty()) {
                y += ((tiles.size() + cols - 1) / cols) * (cw * RogueLook.CARD_RATIO + gap);
            }
            y += 8 * u;
        }
        return new ScrollBounds(visibleWidth, y);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float u = RogueLook.unit();
        float shift = -getScrollTop();
        for (int s = 0; s < sections.size(); s++) {
            String label = sections.get(s).label();
            if (label != null && !label.isBlank() && s < labelTops.size()) {
                RogueLook.text(g, label, 6 * u, labelTops.get(s) + shift, 1.1f, RogueLook.DEEP);
            }
        }
    }

    private static class CardTile extends FDisplayObject {
        private final List<PaperCard> list;
        private final int index;

        CardTile(List<PaperCard> list, int index) {
            this.list = list;
            this.index = index;
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            RogueScreen.hover(this, hovered, list.get(index));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            CardZoom.show(list, index, null);
            return true;
        }

        @Override
        public void draw(Graphics g) {
            RogueLook.card(g, list.get(index), 0, 0, getWidth(), 1);
        }
    }
}
